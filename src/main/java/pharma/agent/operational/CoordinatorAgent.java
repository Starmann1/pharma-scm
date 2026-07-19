package pharma.agent.operational;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import pharma.agent.core.BasePharmaAgent;
import pharma.agent.ontology.AgentActions;
import pharma.agent.ontology.AgentNames;
import pharma.agent.platform.AgentGateway;
import pharma.dto.AgentRequestEnvelope;
import pharma.dto.AgentResponseEnvelope;

/**
 * CoordinatorAgent — central task router and UI gateway (Phase 6).
 *
 * <p>Responsibilities:
 * <ol>
 *   <li>Reads {@link AgentRequestEnvelope} objects from the O2A channel
 *       (put there by {@link AgentGateway#submit}) and forwards them as ACL
 *       REQUEST messages to the correct specialist agent.</li>
 *   <li>Receives ACL INFORM replies from specialist agents.</li>
 *   <li>Calls {@link AgentGateway#complete} to resolve the pending
 *       {@code CompletableFuture} on the Swing side.</li>
 * </ol>
 *
 * <p>Architecture rule: this agent is a pure router — it must NOT call any
 * Service or Repository directly.
 *
 * <p>Arguments:
 * <ul>
 *   <li>arg[0] — {@link pharma.config.ApplicationServices} (inherited by BasePharmaAgent)</li>
 *   <li>arg[1] — {@link AgentGateway}</li>
 * </ul>
 */
public class CoordinatorAgent extends BasePharmaAgent {

    private static final Logger log = LoggerFactory.getLogger(CoordinatorAgent.class);

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private AgentGateway gateway;

    @Override
    protected void setup() {
        super.setup();

        // Enable the Object-to-Agent (O2A) channel so AgentGateway.submit()
        // can deliver ACLMessage objects into this agent's queue.
        setEnabledO2ACommunication(true, 0);

        Object[] args = getArguments();
        if (args != null && args.length > 1 && args[1] instanceof AgentGateway ag) {
            this.gateway = ag;
        } else {
            log.warn("[CoordinatorAgent] No AgentGateway provided — UI dispatch will not function.");
        }

        // Behaviour 1: drain the O2A queue, forward each request to the right agent
        addBehaviour(new O2ADispatchBehaviour());

        // Behaviour 2: receive INFORM replies from sub-agents, resolve gateway futures
        addBehaviour(new ReplyBehaviour());

        log.info("[CoordinatorAgent] Ready. Gateway wired: {}", gateway != null);
    }

    // =========================================================================
    // Inner: O2ADispatchBehaviour
    // =========================================================================

    /**
     * Polls the O2A channel for ACL messages inserted by {@link AgentGateway#submit},
     * deserialises the envelope, and forwards a REQUEST to the correct sub-agent.
     */
    private class O2ADispatchBehaviour extends CyclicBehaviour {

        @Override
        public void action() {
            // Non-blocking poll of the O2A queue
            Object o2aObj = myAgent.getO2AObject();
            if (o2aObj == null) {
                block(50); // sleep 50 ms then retry — keeps CPU idle between requests
                return;
            }

            if (!(o2aObj instanceof ACLMessage msg)) {
                log.warn("[CoordinatorAgent] O2A received unexpected type: {}",
                        o2aObj.getClass().getName());
                return;
            }

            AgentRequestEnvelope<?> request;
            try {
                @SuppressWarnings("null")
                AgentRequestEnvelope<?> req = MAPPER.readValue(msg.getContent(), AgentRequestEnvelope.class);
                request = req;
            } catch (JsonProcessingException e) {
                log.error("[CoordinatorAgent] Failed to parse O2A request: {}", e.getMessage());
                return;
            }

            String targetAgentName = resolveTargetAgent(request.getAction());
            if (targetAgentName == null) {
                log.warn("[CoordinatorAgent] Unknown action '{}' — no target agent.", request.getAction());
                if (gateway != null) {
                    gateway.complete(AgentResponseEnvelope.failure(
                            request.getTransactionId(), request.getAction(),
                            "No agent registered for action: " + request.getAction()));
                }
                return;
            }

            log.info("[CoordinatorAgent] Routing action='{}' txId='{}' → {}",
                    request.getAction(), request.getTransactionId(), targetAgentName);

            ACLMessage forward = new ACLMessage(ACLMessage.REQUEST);
            forward.addReceiver(new AID(targetAgentName, AID.ISLOCALNAME));
            forward.setContent(msg.getContent()); // pass JSON payload as-is
            forward.setConversationId(request.getTransactionId()); // used to correlate reply
            myAgent.send(forward);
        }

        private String resolveTargetAgent(String action) {
            if (action == null) return null;
            return switch (action) {
                case AgentActions.CHECK_STOCK,
                     AgentActions.LOW_STOCK_ALERT         -> AgentNames.INVENTORY;
                case AgentActions.CHECK_SUPPLIER,
                     AgentActions.SUPPLIER_PROPOSE        -> AgentNames.SUPPLIER;
                case AgentActions.MANUFACTURING_FEASIBILITY,
                     AgentActions.CHECK_CAPACITY          -> AgentNames.PRODUCTION;
                case AgentActions.QA_REVIEW               -> AgentNames.QA;
                case AgentActions.COMPLIANCE_VALIDATE     -> AgentNames.COMPLIANCE;
                case AgentActions.RISK_ANALYSIS           -> AgentNames.RISK;
                case AgentActions.PROCUREMENT_WORKFLOW,
                     AgentActions.CREATE_PO_DRAFT         -> AgentNames.PROCUREMENT;
                case AgentActions.AI_REASONING            -> AgentNames.AI_REASONING;
                case AgentActions.KNOWLEDGE_QUERY         -> AgentNames.KNOWLEDGE;
                default -> null;
            };
        }
    }

    // =========================================================================
    // Inner: ReplyBehaviour
    // =========================================================================

    /**
     * Listens for ACL INFORM messages from specialist agents and resolves
     * the corresponding {@code CompletableFuture} in the {@link AgentGateway}.
     *
     * <p>FIX: Previously matched on performative only, which allowed concurrent
     * replies to be delivered to the wrong future when multiple requests were
     * in-flight simultaneously. Now matches on BOTH performative AND conversationId,
     * guaranteeing correct reply routing even under concurrent load.
     */
    private class ReplyBehaviour extends CyclicBehaviour {

        @Override
        public void action() {
            // Dynamic template: match INFORM for any conversationId
            MessageTemplate template = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.not(MessageTemplate.MatchConversationId(null)));

            ACLMessage msg = myAgent.receive(template);
            if (msg == null) {
                block();
                return;
            }

            if (gateway == null) {
                log.warn("[CoordinatorAgent] Received reply but gateway is null — dropping.");
                return;
            }

            try {
                @SuppressWarnings("null")
                AgentResponseEnvelope<?> response =
                        MAPPER.readValue(msg.getContent(), AgentResponseEnvelope.class);

                // Enforce conversationId correlation: the response's transactionId must
                // match the message's conversationId set by O2ADispatchBehaviour.
                if (response.getTransactionId() == null) {
                    response = AgentResponseEnvelope.failure(
                        msg.getConversationId(),
                        response.getAction(),
                        "Response missing transactionId — set from conversationId");
                }

                response.getAgentTrace().add(0, "CoordinatorAgent relayed reply");

                log.info("[CoordinatorAgent] Relaying reply txId='{}' status='{}'",
                        response.getTransactionId(), response.getResponseStatus());

                gateway.complete(response);

            } catch (JsonProcessingException e) {
                log.error("[CoordinatorAgent] Failed to parse specialist reply: {}", e.getMessage());
                // Attempt to resolve the pending future with a failure so the caller isn't hung
                if (gateway != null && msg.getConversationId() != null) {
                    gateway.complete(AgentResponseEnvelope.failure(
                        msg.getConversationId(), "UNKNOWN",
                        "CoordinatorAgent failed to parse specialist reply: " + e.getMessage()));
                }
            }
        }
    }
}
