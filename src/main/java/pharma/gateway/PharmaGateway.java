package pharma.gateway;

import java.util.concurrent.CompletableFuture;

import pharma.dto.AgentRequestEnvelope;
import pharma.dto.AgentResponseEnvelope;

/**
 * Unified gateway interface for agent communication.
 * <p>
 * This is the ONLY boundary between the UI/application layer and the
 * agent orchestration layer. All 3 versions implement this interface:
 * <ul>
 *   <li>V1: JadeGateway (JADE + LangChain4j)</li>
 *   <li>V2: LangChainGateway (Pure LangChain4j)</li>
 *   <li>V3: AdkGateway (Pure Google ADK)</li>
 * </ul>
 */
public interface PharmaGateway {

    /**
     * Submit a request to the agent system.
     * Routes to the correct agent/worker based on the action in the envelope.
     *
     * @param request the agent request envelope
     * @return a future that completes with the agent response
     */
    CompletableFuture<AgentResponseEnvelope<?>> submit(AgentRequestEnvelope<?> request);

    /**
     * Returns the number of in-flight requests (for UI status indicators).
     */
    int pendingRequestCount();

    /**
     * Graceful shutdown of the agent platform.
     */
    void shutdown();
}
