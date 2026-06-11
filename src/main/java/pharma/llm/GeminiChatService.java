package pharma.llm;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.agent.tool.ToolExecutionRequest;

/**
 * Service wrapper around Google Gemini via LangChain4j.
 *
 * <p>Provides two interaction modes:
 * <ul>
 *   <li>{@link #chat(String, String)} — simple prompt/response without tools</li>
 *   <li>{@link #chatWithTools(String, String, List)} — prompt/response with
 *       automatic tool-calling loop that invokes {@code @Tool}-annotated methods</li>
 * </ul>
 *
 * <p>The model is configured once at construction time with API key, model name,
 * temperature, timeout, and retry settings.
 */
public class GeminiChatService {

    private static final Logger log = LoggerFactory.getLogger(GeminiChatService.class);

    private final ChatModel chatModel;
    private final String modelName;

    /**
     * Constructs the Gemini chat service.
     *
     * @param apiKey      Google AI API key
     * @param modelName   Gemini model name (e.g. "gemini-2.0-flash")
     * @param temperature sampling temperature (0.0 = deterministic, 1.0 = creative)
     */
    public GeminiChatService(String apiKey, String modelName, double temperature) {
        this.modelName = modelName;
        this.chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(30))
                .maxRetries(3)
                .logRequestsAndResponses(false)
                .build();
        log.info("[GeminiChatService] Initialized with model='{}' temperature={}", modelName, temperature);
    }

    /**
     * Sends a simple chat prompt without tool support.
     *
     * @param systemPrompt the system-level instructions for the model
     * @param userPrompt   the user's input text
     * @return the model's response text
     */
    public String chat(String systemPrompt, String userPrompt) {
        log.debug("[GeminiChatService] chat() systemPrompt length={} userPrompt length={}",
                systemPrompt.length(), userPrompt.length());

        List<ChatMessage> messages = List.of(
                SystemMessage.from(systemPrompt),
                UserMessage.from(userPrompt)
        );

        ChatRequest request = ChatRequest.builder()
                .messages(messages)
                .build();

        ChatResponse response = chatModel.chat(request);
        String text = response.aiMessage().text();

        log.debug("[GeminiChatService] chat() response length={}", text != null ? text.length() : 0);
        return text;
    }

    /**
     * Sends a chat prompt with tool-calling support.
     *
     * <p>This method implements the tool-calling loop:
     * <ol>
     *   <li>Sends the initial messages with tool specifications to the model</li>
     *   <li>If the model requests tool calls, executes them against the provided tool objects</li>
     *   <li>Feeds tool results back to the model</li>
     *   <li>Repeats until the model produces a final text response (max 10 iterations)</li>
     * </ol>
     *
     * @param systemPrompt the system-level instructions
     * @param userPrompt   the user's input text
     * @param toolObjects  list of objects containing {@code @Tool}-annotated methods
     * @return the model's final text response after all tool calls are resolved
     */
    public String chatWithTools(String systemPrompt, String userPrompt, List<Object> toolObjects) {
        log.debug("[GeminiChatService] chatWithTools() with {} tool objects", toolObjects.size());

        List<ToolSpecification> toolSpecs = new java.util.ArrayList<>();
        java.util.Map<String, Object> toolObjectMap = new java.util.HashMap<>();

        for (Object toolObj : toolObjects) {
            List<ToolSpecification> specs = dev.langchain4j.agent.tool.ToolSpecifications.toolSpecificationsFrom(toolObj);
            for (ToolSpecification spec : specs) {
                toolSpecs.add(spec);
                toolObjectMap.put(spec.name(), toolObj);
            }
        }

        log.debug("[GeminiChatService] Discovered {} tool specifications", toolSpecs.size());

        // Build initial message list (mutable for the tool-calling loop)
        java.util.List<ChatMessage> messages = new java.util.ArrayList<>();
        messages.add(SystemMessage.from(systemPrompt));
        messages.add(UserMessage.from(userPrompt));

        // Tool-calling loop with safety limit
        int maxIterations = 10;
        for (int i = 0; i < maxIterations; i++) {
            ChatRequest request = ChatRequest.builder()
                    .messages(messages)
                    .toolSpecifications(toolSpecs)
                    .build();

            ChatResponse response = chatModel.chat(request);
            AiMessage aiMessage = response.aiMessage();
            messages.add(aiMessage);

            // If no tool calls requested, return the final text
            if (!aiMessage.hasToolExecutionRequests()) {
                String text = aiMessage.text();
                log.debug("[GeminiChatService] chatWithTools() final response after {} iteration(s), length={}",
                        i + 1, text != null ? text.length() : 0);
                return text;
            }

            // Execute each requested tool call
            for (ToolExecutionRequest toolRequest : aiMessage.toolExecutionRequests()) {
                String toolName = toolRequest.name();
                log.info("[GeminiChatService] Executing tool '{}' with args: {}", toolName, toolRequest.arguments());

                Object toolObj = toolObjectMap.get(toolName);
                if (toolObj == null) {
                    String errorMsg = "Unknown tool: " + toolName;
                    log.warn("[GeminiChatService] {}", errorMsg);
                    messages.add(ToolExecutionResultMessage.from(toolRequest, errorMsg));
                    continue;
                }
                DefaultToolExecutor executor = new DefaultToolExecutor(toolObj, toolRequest);

                try {
                    String result = executor.execute(toolRequest, null);
                    messages.add(ToolExecutionResultMessage.from(toolRequest, result));
                    log.debug("[GeminiChatService] Tool '{}' returned result length={}", toolName,
                            result != null ? result.length() : 0);
                } catch (Exception e) {
                    String errorResult = "Tool execution error: " + e.getMessage();
                    log.error("[GeminiChatService] Tool '{}' threw exception: {}", toolName, e.getMessage(), e);
                    messages.add(ToolExecutionResultMessage.from(toolRequest, errorResult));
                }
            }
        }

        log.warn("[GeminiChatService] Tool-calling loop hit max iterations ({})", maxIterations);
        return "Error: tool-calling loop exceeded maximum iterations (" + maxIterations + ")";
    }

    /**
     * Returns the configured model name.
     *
     * @return the Gemini model name
     */
    public String getModelName() {
        return modelName;
    }
}
