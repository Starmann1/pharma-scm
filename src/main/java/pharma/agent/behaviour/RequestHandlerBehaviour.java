package pharma.agent.behaviour;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import pharma.dto.AgentRequestEnvelope;
import pharma.dto.AgentResponseEnvelope;

/**
 * Abstract base for all Phase-6 agent request handlers.
 *
 * <p>Pattern:
 * <ol>
 *   <li>Blocks (non-busy) on incoming ACL REQUEST messages.</li>
 *   <li>Deserialises the JSON content into an {@link AgentRequestEnvelope}.</li>
 *   <li>Calls the concrete {@link #handle} implementation.</li>
 *   <li>Serialises the {@link AgentResponseEnvelope} back as an ACL INFORM to the sender.</li>
 * </ol>
 *
 * <p>Architecture rule enforced here: agents never touch JDBC or repositories directly.
 * All data access is via {@code services.*} in the concrete subclass.
 */
public abstract class RequestHandlerBehaviour extends CyclicBehaviour {

    private static final Logger log = LoggerFactory.getLogger(RequestHandlerBehaviour.class);

    protected static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final MessageTemplate REQUEST_TEMPLATE =
            MessageTemplate.MatchPerformative(ACLMessage.REQUEST);

    @Override
    public final void action() {
        ACLMessage msg = myAgent.receive(REQUEST_TEMPLATE);
        if (msg == null) {
            block();
            return;
        }

        AgentRequestEnvelope<?> request = null;
        try {
            request = MAPPER.readValue(msg.getContent(), AgentRequestEnvelope.class);
        } catch (JsonProcessingException e) {
            log.error("[{}] Failed to parse incoming request: {}", myAgent.getLocalName(), e.getMessage());
            sendFailureReply(msg, "unknown", "PARSE_ERROR: " + e.getMessage());
            return;
        }

        log.info("[{}] Received action='{}' txId='{}'",
                myAgent.getLocalName(), request.getAction(), request.getTransactionId());

        AgentResponseEnvelope<?> response;
        try {
            response = handle(request);
        } catch (Exception e) {
            log.error("[{}] Error handling action='{}': {}",
                    myAgent.getLocalName(), request.getAction(), e.getMessage(), e);
            response = AgentResponseEnvelope.failure(
                    request.getTransactionId(), request.getAction(),
                    e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        // Add agent trace entry for observability
        response.getAgentTrace().add(myAgent.getLocalName() + " processed " + request.getAction());

        sendReply(msg, response);
    }

    // -------------------------------------------------------------------------
    // Abstract contract
    // -------------------------------------------------------------------------

    /**
     * Concrete agents implement this to handle the deserialized request.
     *
     * @param request the inbound envelope (action + typed payload)
     * @return a fully-populated response envelope
     * @throws Exception any service/repository-level checked exception
     */
    protected abstract AgentResponseEnvelope<?> handle(AgentRequestEnvelope<?> request) throws Exception;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void sendReply(ACLMessage originalMsg, AgentResponseEnvelope<?> response) {
        try {
            ACLMessage reply = originalMsg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent(MAPPER.writeValueAsString(response));
            myAgent.send(reply);
        } catch (JsonProcessingException e) {
            log.error("[{}] Failed to serialise response: {}", myAgent.getLocalName(), e.getMessage());
        }
    }

    private void sendFailureReply(ACLMessage originalMsg, String txId, String errorMsg) {
        try {
            AgentResponseEnvelope<?> failureEnv = AgentResponseEnvelope.failure(txId, "UNKNOWN", errorMsg);
            ACLMessage reply = originalMsg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent(MAPPER.writeValueAsString(failureEnv));
            myAgent.send(reply);
        } catch (JsonProcessingException e) {
            log.error("[{}] Could not even send failure reply: {}", myAgent.getLocalName(), e.getMessage());
        }
    }
}
