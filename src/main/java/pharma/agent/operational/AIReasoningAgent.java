package pharma.agent.operational;

import pharma.agent.behaviour.AIReasoningBehaviour;
import pharma.agent.core.BasePharmaAgent;
import pharma.llm.GeminiChatService;
import pharma.llm.tools.LlmToolRegistry;

/**
 * AI Reasoning Agent — the LLM-powered JADE agent for Phase 10.
 *
 * <p>This agent integrates Google Gemini (via LangChain4j) with the existing
 * JADE multi-agent system, enabling structured AI reasoning over pharmaceutical
 * supply-chain data.
 *
 * <p>On startup it:
 * <ol>
 *   <li>Reads configuration from environment variables
 *       ({@code GEMINI_API_KEY}, {@code GEMINI_MODEL}, {@code GEMINI_TEMPERATURE})</li>
 *   <li>Creates a {@link GeminiChatService} wrapping the Gemini model</li>
 *   <li>Creates a {@link LlmToolRegistry} exposing all domain services as LLM tools</li>
 *   <li>Registers an {@link AIReasoningBehaviour} to handle incoming AI_REASONING requests</li>
 * </ol>
 *
 * <p>If {@code GEMINI_API_KEY} is not configured, the agent starts in <b>degraded mode</b>:
 * it remains alive on the JADE platform but will return errors for any AI reasoning
 * requests. This allows the rest of the multi-agent system to function normally.
 *
 * <p>Environment variables:
 * <ul>
 *   <li>{@code GEMINI_API_KEY} — <b>required</b> — Google AI API key</li>
 *   <li>{@code GEMINI_MODEL} — optional, defaults to {@code gemini-2.0-flash}</li>
 *   <li>{@code GEMINI_TEMPERATURE} — optional, defaults to {@code 0.1}</li>
 * </ul>
 */
public class AIReasoningAgent extends BasePharmaAgent {

    private static final String DEFAULT_MODEL = "gemini-2.0-flash";
    private static final double DEFAULT_TEMPERATURE = 0.1;

    private GeminiChatService geminiService;
    private LlmToolRegistry toolRegistry;
    private boolean degradedMode = false;

    @Override
    protected void setup() {
        super.setup();

        // Read configuration from environment
        String apiKey = System.getenv("GEMINI_API_KEY");
        String modelName = System.getenv("GEMINI_MODEL");
        String temperatureStr = System.getenv("GEMINI_TEMPERATURE");

        if (apiKey == null || apiKey.isBlank()) {
            try {
                io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure().ignoreIfMissing().load();
                apiKey = dotenv.get("GEMINI_API_KEY");
                if (modelName == null || modelName.isBlank()) modelName = dotenv.get("GEMINI_MODEL");
                if (temperatureStr == null || temperatureStr.isBlank()) temperatureStr = dotenv.get("GEMINI_TEMPERATURE");
            } catch (Exception e) {
                // Ignore .env loading errors silently
            }
        }

        if (modelName == null || modelName.isBlank()) {
            modelName = DEFAULT_MODEL;
        }

        double temperature = DEFAULT_TEMPERATURE;
        if (temperatureStr != null && !temperatureStr.isBlank()) {
            try {
                temperature = Double.parseDouble(temperatureStr);
            } catch (NumberFormatException e) {
                logger.warn("[AIReasoningAgent] Invalid GEMINI_TEMPERATURE '{}', using default {}",
                        temperatureStr, DEFAULT_TEMPERATURE);
            }
        }

        // Validate API key
        if (apiKey == null || apiKey.isBlank()) {
            logger.warn("[AIReasoningAgent] GEMINI_API_KEY is not set — starting in DEGRADED MODE. " +
                    "AI reasoning requests will fail until a valid API key is configured.");
            degradedMode = true;
        }

        // Create LLM tool registry (always — it only wraps existing services)
        if (services != null) {
            toolRegistry = new LlmToolRegistry(services);
        } else {
            logger.warn("[AIReasoningAgent] ApplicationServices not available — tools will not be registered.");
        }

        // Create Gemini service if API key is available
        if (!degradedMode) {
            try {
                geminiService = new GeminiChatService(apiKey, modelName, temperature);
                logger.info("[AIReasoningAgent] Gemini service initialized: model='{}' temperature={}",
                        modelName, temperature);
            } catch (Exception e) {
                logger.error("[AIReasoningAgent] Failed to initialize Gemini service: {}", e.getMessage(), e);
                degradedMode = true;
            }
        }

        // Register behaviour
        if (!degradedMode && geminiService != null && toolRegistry != null) {
            addBehaviour(new AIReasoningBehaviour(this, geminiService, toolRegistry));
            logger.info("[AIReasoningAgent] Ready to handle AI reasoning requests.");
        } else {
            logger.warn("[AIReasoningAgent] Running in degraded mode — no behaviour registered.");
        }
    }


    /**
     * Whether the agent is running in degraded mode (no Gemini API key).
     *
     * @return {@code true} if degraded
     */
    public boolean isDegradedMode() {
        return degradedMode;
    }


    @Override
    protected void takeDown() {
        logger.info("[AIReasoningAgent] Shutting down. Degraded mode was: {}", degradedMode);
        super.takeDown();
    }
}
