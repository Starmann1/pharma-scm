package pharma.agent.platform;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import pharma.agent.ontology.AgentNames;
import pharma.dto.AgentRequestEnvelope;
import pharma.dto.AgentResponseEnvelope;

/**
 * AgentGateway — the bridge between the Swing EDT and the JADE agent platform.
 *
 * <p>Swing panels call {@link #submit} with a typed request envelope.
 * The gateway serialises it to JSON, fires an ACL REQUEST message to the
 * {@code CoordinatorAgent}, and returns a {@link CompletableFuture} that
 * resolves when the coordinator relays the specialist agent's reply back
 * via {@link #complete}.
 *
 * <p>Threading:
 * <ul>
 *   <li>{@link #submit} is called on the Swing EDT — it returns immediately.</li>
 *   <li>{@link #complete} is called from a JADE agent thread — it completes
 *       the future, triggering any registered callbacks.</li>
 *   <li>UI callbacks must use {@code SwingUtilities.invokeLater} to update
 *       Swing components from within the future's {@code thenAccept}.</li>
 * </ul>
 */
public class AgentGateway {

    private static final Logger log = LoggerFactory.getLogger(AgentGateway.class);

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /** Pending requests keyed by transactionId — resolved when coordinator replies. */
    private final Map<String, CompletableFuture<AgentResponseEnvelope<?>>> pendingRequests =
            new ConcurrentHashMap<>();

    /** The running CoordinatorAgent controller used to send ACL messages. */
    private AgentController coordinatorController;

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    /**
     * Called by {@link AgentPlatformManager} after the CoordinatorAgent is started.
     *
     * @param controller the JADE {@link AgentController} for the CoordinatorAgent
     */
    public void setCoordinatorController(AgentController controller) {
        this.coordinatorController = controller;
        log.info("AgentGateway: CoordinatorAgent controller registered.");
    }

    // -------------------------------------------------------------------------
    // Public API (called from Swing EDT)
    // -------------------------------------------------------------------------

    /**
     * Submits a request to the agent platform asynchronously.
     *
     * <ol>
     *   <li>Stores a {@link CompletableFuture} in the pending-request map.</li>
     *   <li>Serialises the envelope to JSON.</li>
     *   <li>Sends an ACL REQUEST message to the CoordinatorAgent via JADE.</li>
     * </ol>
     *
     * @param request the typed request envelope
     * @return a future that will be completed with the agent's response
     */
    public CompletableFuture<AgentResponseEnvelope<?>> submit(AgentRequestEnvelope<?> request) {
        CompletableFuture<AgentResponseEnvelope<?>> future = new CompletableFuture<>();
        pendingRequests.put(request.getTransactionId(), future);

        if (coordinatorController == null) {
            log.error("AgentGateway: CoordinatorAgent not initialised — completing future with failure.");
            AgentResponseEnvelope<?> err = AgentResponseEnvelope.failure(
                    request.getTransactionId(), request.getAction(),
                    "JADE platform not started. CoordinatorAgent unavailable.");
            pendingRequests.remove(request.getTransactionId());
            future.complete(err);
            return future;
        }

        try {
            String json = MAPPER.writeValueAsString(request);

            // Build the ACL message targeting CoordinatorAgent
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.addReceiver(new AID(AgentNames.COORDINATOR, AID.ISLOCALNAME));
            msg.setContent(json);
            msg.setConversationId(request.getTransactionId());

            // Use putO2AObject to pass the ACL message through the agent's O2A channel
            coordinatorController.putO2AObject(msg, AgentController.ASYNC);

            log.info("AgentGateway: dispatched action='{}' txId='{}'",
                    request.getAction(), request.getTransactionId());

        } catch (JsonProcessingException e) {
            log.error("AgentGateway: serialisation failed for action='{}': {}",
                    request.getAction(), e.getMessage());
            pendingRequests.remove(request.getTransactionId());
            future.completeExceptionally(e);
        } catch (StaleProxyException e) {
            log.error("AgentGateway: CoordinatorAgent proxy is stale: {}", e.getMessage());
            pendingRequests.remove(request.getTransactionId());
            AgentResponseEnvelope<?> err = AgentResponseEnvelope.failure(
                    request.getTransactionId(), request.getAction(),
                    "CoordinatorAgent proxy stale: " + e.getMessage());
            future.complete(err);
        }

        return future;
    }

    // -------------------------------------------------------------------------
    // Called from JADE agent thread (CoordinatorAgent.ReplyBehaviour)
    // -------------------------------------------------------------------------

    /**
     * Resolves the pending {@link CompletableFuture} for the given response.
     * Called by {@code CoordinatorAgent.ReplyBehaviour} on a JADE thread.
     *
     * @param response the typed response envelope from the specialist agent
     */
    public void complete(AgentResponseEnvelope<?> response) {
        CompletableFuture<AgentResponseEnvelope<?>> future =
                pendingRequests.remove(response.getTransactionId());
        if (future != null) {
            future.complete(response);
            log.info("AgentGateway: completed txId='{}' status='{}'",
                    response.getTransactionId(), response.getResponseStatus());
        } else {
            log.warn("AgentGateway: received reply for unknown txId='{}'",
                    response.getTransactionId());
        }
    }

    // -------------------------------------------------------------------------
    // Diagnostics
    // -------------------------------------------------------------------------

    /** Returns the number of requests awaiting a reply. */
    public int pendingRequestCount() {
        return pendingRequests.size();
    }
}
