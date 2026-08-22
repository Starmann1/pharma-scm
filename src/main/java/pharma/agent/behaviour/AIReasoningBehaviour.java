package pharma.agent.behaviour;

import java.util.ArrayList;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;

import pharma.agent.core.BasePharmaAgent;
import pharma.agent.ontology.AgentActions;
import pharma.config.ApplicationServices;
import pharma.dto.AIReasoningRequestDTO;
import pharma.dto.AIReasoningResultDTO;
import pharma.dto.AgentRequestEnvelope;
import pharma.dto.AgentResponseEnvelope;
import pharma.llm.GeminiChatService;
import pharma.llm.StructuredExtractionPrompts;
import pharma.llm.tools.LlmToolRegistry;

/**
 * JADE behaviour that handles {@link AgentActions#AI_REASONING} requests.
 *
 * <p>Processing pipeline:
 * <ol>
 *   <li>Deserialises the payload into {@link AIReasoningRequestDTO}</li>
 *   <li>Selects a structured-extraction prompt template based on {@code taskType}</li>
 *   <li>Invokes Gemini via {@link GeminiChatService#chatWithTools} with all registered tools</li>
 *   <li>Parses the LLM's confidence score from the JSON response</li>
 *   <li>Flags results with confidence &lt; 0.75 for mandatory human review</li>
 *   <li>Persists the decision via {@link pharma.service.AIDecisionService}</li>
 *   <li>Returns a success envelope with the {@link AIReasoningResultDTO}</li>
 * </ol>
 */
public class AIReasoningBehaviour extends RequestHandlerBehaviour {

    private final BasePharmaAgent agent;
    private final GeminiChatService geminiService;
    private final LlmToolRegistry toolRegistry;
    private final ApplicationServices services;

    /**
     * Constructs the behaviour.
     *
     * @param agent         the owning agent (for logging and services)
     * @param geminiService the Gemini chat service for LLM calls
     * @param toolRegistry  registry of LLM-callable tool objects
     */
    public AIReasoningBehaviour(BasePharmaAgent agent,
                                 GeminiChatService geminiService,
                                 LlmToolRegistry toolRegistry) {
        this.agent = agent;
        this.geminiService = geminiService;
        this.toolRegistry = toolRegistry;
        this.services = agent.getServices();
    }

    @Override
    protected AgentResponseEnvelope<?> handle(AgentRequestEnvelope<?> request) throws Exception {
        // Only handle AI_REASONING actions
        if (!AgentActions.AI_REASONING.equals(request.getAction())) {
            return AgentResponseEnvelope.failure(
                    request.getTransactionId(), request.getAction(),
                    "AIReasoningAgent: unsupported action '" + request.getAction() + "'");
        }

        // Deserialise the request payload
        @SuppressWarnings("null")
        AIReasoningRequestDTO reasoningRequest = MAPPER.convertValue(
                request.getPayload(), AIReasoningRequestDTO.class);

        String taskType = reasoningRequest.getTaskType();
        String rawText = reasoningRequest.getRawText();
        String contextSummary = reasoningRequest.getContextSummary();

        agent.getLogger().info("[AIReasoningBehaviour] Processing taskType='{}' txId='{}'",
                taskType, request.getTransactionId());

        // Select the prompt template based on task type
        String systemPrompt = StructuredExtractionPrompts.forTaskType(taskType);

        // Build the user prompt with context if available
        String userPrompt = buildUserPrompt(rawText, contextSummary);

        // Call Gemini with tools
        String llmResponse;
        try {
            llmResponse = geminiService.chatWithTools(
                    systemPrompt, userPrompt, toolRegistry.getToolObjects());
        } catch (Exception e) {
            agent.getLogger().error("[AIReasoningBehaviour] LLM call failed: {}", e.getMessage(), e);
            return AgentResponseEnvelope.failure(
                    request.getTransactionId(), request.getAction(),
                    "LLM call failed: " + e.getMessage());
        }

        // Determine extraction handling based on task type
        double confidenceScore;
        boolean requiresHumanReview;
        Object extractedData;

        if ("EXPLAINABILITY_CHAT".equalsIgnoreCase(taskType) || "CHAT".equalsIgnoreCase(taskType)) {
            extractedData = llmResponse;
            confidenceScore = 1.0;
            requiresHumanReview = false;
        } else {
            confidenceScore = extractConfidenceScore(llmResponse);
            requiresHumanReview = confidenceScore < 0.75;
            if (requiresHumanReview) {
                agent.getLogger().warn("[AIReasoningBehaviour] Low confidence ({}) — flagging for human review",
                        confidenceScore);
            }
            extractedData = parseExtractedData(llmResponse);
        }

        // Build the result DTO
        AIReasoningResultDTO result = new AIReasoningResultDTO();
        result.setExtractedData(extractedData);
        result.setConfidenceScore(confidenceScore);
        result.setModelUsed(geminiService.getModelName());
        result.setPromptSummary(taskType + ": " + truncate(rawText, 200));
        result.setRequiresHumanReview(requiresHumanReview);
        result.setCitations(new ArrayList<>());

        // Persist the AI decision for audit trail
        try {
            services.getAiDecisionService().save(result, request.getTransactionId());
            agent.getLogger().info("[AIReasoningBehaviour] Persisted AI decision txId='{}'",
                    request.getTransactionId());
        } catch (Exception e) {
            agent.getLogger().error("[AIReasoningBehaviour] Failed to persist AI decision: {}", e.getMessage(), e);
            // Continue — the LLM result is still valid even if persistence fails
        }

        return AgentResponseEnvelope.success(
                request.getTransactionId(), request.getAction(), result);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Builds the user prompt by combining the raw input text with optional context.
     */
    private String buildUserPrompt(String rawText, String contextSummary) {
        StringBuilder sb = new StringBuilder();
        if (contextSummary != null && !contextSummary.isBlank()) {
            sb.append("=== CONTEXT ===\n")
              .append(contextSummary)
              .append("\n\n");
        }
        sb.append("=== INPUT ===\n");
        sb.append(rawText != null ? rawText : "(no input provided)");
        return sb.toString();
    }

    /**
     * Attempts to extract the {@code confidenceScore} field from a JSON response string.
     * Returns 0.5 as a conservative default if parsing fails.
     */
    private double extractConfidenceScore(String llmResponse) {
        try {
            // Strip potential markdown code fences
            String cleaned = cleanJsonResponse(llmResponse);
            Map<String, Object> parsed = MAPPER.readValue(cleaned, new TypeReference<>() {});
            Object score = parsed.get("confidenceScore");
            if (score instanceof Number number) {
                return Math.max(0.0, Math.min(1.0, number.doubleValue()));
            }
        } catch (Exception e) {
            agent.getLogger().warn("[AIReasoningBehaviour] Could not parse confidenceScore from LLM response, " +
                    "defaulting to 0.5: {}", e.getMessage());
        }
        return 0.5;
    }

    /**
     * Attempts to parse the LLM response as structured JSON data.
     * Falls back to returning the raw string if JSON parsing fails.
     */
    private Object parseExtractedData(String llmResponse) {
        try {
            String cleaned = cleanJsonResponse(llmResponse);
            return MAPPER.readValue(cleaned, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            agent.getLogger().debug("[AIReasoningBehaviour] Response is not valid JSON, storing as raw text");
            return llmResponse;
        }
    }

    /**
     * Strips markdown code fences ({@code ```json ... ```}) that LLMs sometimes wrap around JSON output.
     */
    private String cleanJsonResponse(String response) {
        if (response == null) {
            return "{}";
        }
        String trimmed = response.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }

    /**
     * Truncates a string to the specified maximum length, appending "..." if truncated.
     */
    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "(null)";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
