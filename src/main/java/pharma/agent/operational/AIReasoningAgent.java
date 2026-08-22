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

        // Read configuration from environment (.env fallback)
        String groqKey = System.getenv("GROQ_API_KEY");
        String groqModel = System.getenv("GROQ_MODEL");
        String groqBaseUrl = System.getenv("GROQ_BASE_URL");

        String geminiKey = System.getenv("GEMINI_API_KEY");
        String geminiModel = System.getenv("GEMINI_MODEL");
        String temperatureStr = System.getenv("AI_TEMPERATURE");
        if (temperatureStr == null) temperatureStr = System.getenv("GEMINI_TEMPERATURE");

        try {
            io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure().ignoreIfMissing().load();
            if (groqKey == null || groqKey.isBlank()) groqKey = dotenv.get("GROQ_API_KEY");
            if (groqModel == null || groqModel.isBlank()) groqModel = dotenv.get("GROQ_MODEL");
            if (groqBaseUrl == null || groqBaseUrl.isBlank()) groqBaseUrl = dotenv.get("GROQ_BASE_URL");

            if (geminiKey == null || geminiKey.isBlank()) geminiKey = dotenv.get("GEMINI_API_KEY");
            if (geminiModel == null || geminiModel.isBlank()) geminiModel = dotenv.get("GEMINI_MODEL");
            if (temperatureStr == null || temperatureStr.isBlank()) temperatureStr = dotenv.get("AI_TEMPERATURE");
            if (temperatureStr == null || temperatureStr.isBlank()) temperatureStr = dotenv.get("GEMINI_TEMPERATURE");
        } catch (Exception e) {
            // Ignore .env loading errors silently
        }

        double temperature = DEFAULT_TEMPERATURE;
        if (temperatureStr != null && !temperatureStr.isBlank()) {
            try {
                temperature = Double.parseDouble(temperatureStr);
            } catch (NumberFormatException e) {
                logger.warn("[AIReasoningAgent] Invalid temperature '{}', using default {}",
                        temperatureStr, DEFAULT_TEMPERATURE);
            }
        }

        // Initialize tool registry
        if (services != null) {
            toolRegistry = new LlmToolRegistry(services);
        } else {
            logger.warn("[AIReasoningAgent] ApplicationServices not available — tools will not be registered.");
        }

        // Determine LLM provider: Groq takes priority if GROQ_API_KEY is configured
        if (groqKey != null && !groqKey.isBlank()) {
            try {
                String model = (groqModel != null && !groqModel.isBlank()) ? groqModel : "llama-3.3-70b-versatile";
                geminiService = GeminiChatService.forGroq(groqKey, model, temperature, groqBaseUrl);
                logger.info("[AIReasoningAgent] Groq service initialized: model='{}' temperature={}", model, temperature);
            } catch (Exception e) {
                logger.error("[AIReasoningAgent] Failed to initialize Groq service: {}", e.getMessage(), e);
                degradedMode = true;
            }
        } else if (geminiKey != null && !geminiKey.isBlank()) {
            try {
                String model = (geminiModel != null && !geminiModel.isBlank()) ? geminiModel : DEFAULT_MODEL;
                geminiService = new GeminiChatService(geminiKey, model, temperature);
                logger.info("[AIReasoningAgent] Gemini service initialized: model='{}' temperature={}", model, temperature);
            } catch (Exception e) {
                logger.error("[AIReasoningAgent] Failed to initialize Gemini service: {}", e.getMessage(), e);
                degradedMode = true;
            }
        } else {
            logger.warn("[AIReasoningAgent] Neither GROQ_API_KEY nor GEMINI_API_KEY is set — starting in DEGRADED MODE.");
            degradedMode = true;
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
