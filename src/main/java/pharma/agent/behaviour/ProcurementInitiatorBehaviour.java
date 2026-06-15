package pharma.agent.behaviour;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;
import pharma.config.ApplicationServices;
import pharma.dto.ProcurementRequestDTO;
import pharma.dto.SupplierProposalDTO;

/**
 * FIPA Contract-Net initiator for multi-agent procurement negotiation.
 *
 * <p>The {@code ProcurementWorkflowAgent} creates an instance of this behaviour
 * after building a CFP (Call-For-Proposals) message targeted at all available
 * {@code SupplierAgent} instances.
 *
 * <p>Flow:
 * <ol>
 *   <li>Sends the CFP containing a serialised {@link ProcurementRequestDTO}.</li>
 *   <li>Collects all PROPOSE replies (each wrapping a {@link SupplierProposalDTO}).</li>
 *   <li>Selects the proposal with the highest {@code compositeScore} as the winner.</li>
 *   <li>Sends ACCEPT_PROPOSAL to the winner and REJECT_PROPOSAL to the rest.</li>
 *   <li>Logs the PO creation result in the audit trail via the result notifications.</li>
 * </ol>
 */
public class ProcurementInitiatorBehaviour extends ContractNetInitiator {

    private static final Logger log = LoggerFactory.getLogger(ProcurementInitiatorBehaviour.class);

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final ProcurementRequestDTO procurementRequest;
    private final ApplicationServices services;

    /**
     * Creates a new Contract-Net initiator for a procurement cycle.
     *
     * @param agent              the owning agent (ProcurementWorkflowAgent)
     * @param cfp                the pre-built CFP message (receivers already set)
     * @param procurementRequest the procurement request driving this negotiation
     * @param services           application service composition root
     */
    public ProcurementInitiatorBehaviour(Agent agent, ACLMessage cfp,
                                         ProcurementRequestDTO procurementRequest,
                                         ApplicationServices services) {
        super(agent, cfp);
        this.procurementRequest = procurementRequest;
        this.services = services;
    }

    // -------------------------------------------------------------------------
    // Phase 1 — Evaluate all PROPOSE responses
    // -------------------------------------------------------------------------

    /**
     * Called when all proposals (or timeouts) have been received.
     * Selects the best supplier based on {@code compositeScore} and sends
     * ACCEPT_PROPOSAL / REJECT_PROPOSAL accordingly.
     *
     * @param responses    the raw ACL responses from supplier agents
     * @param acceptances  the vector to fill with ACCEPT/REJECT messages
     */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void handleAllResponses(Vector responses, Vector acceptances) {
        log.info("[ProcurementInitiator] Received {} response(s) for material='{}'.",
                responses.size(), procurementRequest.getMaterialCode());

        List<ACLMessage> proposals = new ArrayList<>();
        List<SupplierProposalDTO> proposalDtos = new ArrayList<>();

        // Parse all PROPOSE messages
        for (Object obj : responses) {
            ACLMessage response = (ACLMessage) obj;
            if (response.getPerformative() == ACLMessage.PROPOSE) {
                try {
                    @SuppressWarnings("null")
                    SupplierProposalDTO dto = MAPPER.readValue(
                            response.getContent(), SupplierProposalDTO.class);
                    proposals.add(response);
                    proposalDtos.add(dto);
                    log.info("[ProcurementInitiator] PROPOSE from '{}': supplier={} score={:.3f} price={:.2f}",
                            response.getSender().getLocalName(),
                            dto.getSupplierName(), dto.getCompositeScore(), dto.getQuotedPrice());
                } catch (Exception e) {
                    log.warn("[ProcurementInitiator] Failed to parse PROPOSE from '{}': {}",
                            response.getSender().getLocalName(), e.getMessage());
                }
            } else {
                log.info("[ProcurementInitiator] Non-PROPOSE ({}) from '{}'.",
                        ACLMessage.getPerformative(response.getPerformative()),
                        response.getSender().getLocalName());
            }
        }

        if (proposals.isEmpty()) {
            log.warn("[ProcurementInitiator] No valid proposals received for material='{}'.",
                    procurementRequest.getMaterialCode());
            return;
        }

        // Find the best proposal by composite score (descending)
        int bestIndex = 0;
        double bestScore = proposalDtos.get(0).getCompositeScore();
        for (int i = 1; i < proposalDtos.size(); i++) {
            if (proposalDtos.get(i).getCompositeScore() > bestScore) {
                bestScore = proposalDtos.get(i).getCompositeScore();
                bestIndex = i;
            }
        }

        // Send ACCEPT to winner, REJECT to rest
        for (int i = 0; i < proposals.size(); i++) {
            ACLMessage reply = proposals.get(i).createReply();
            if (i == bestIndex) {
                reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                reply.setContent(proposals.get(i).getContent()); // echo the winning proposal back
                log.info("[ProcurementInitiator] ACCEPT_PROPOSAL → supplier='{}' score={:.3f}",
                        proposalDtos.get(i).getSupplierName(), proposalDtos.get(i).getCompositeScore());
            } else {
                reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                log.info("[ProcurementInitiator] REJECT_PROPOSAL → supplier='{}'",
                        proposalDtos.get(i).getSupplierName());
            }
            acceptances.add(reply);
        }
    }

    // -------------------------------------------------------------------------
    // Phase 2 — Handle result notifications (post-acceptance)
    // -------------------------------------------------------------------------

    /**
     * Called after ACCEPT_PROPOSAL has been acknowledged.
     * Logs the successful PO creation in the audit trail.
     *
     * @param resultNotifications the INFORM/FAILURE messages from the winning supplier
     */
    @Override
    @SuppressWarnings("rawtypes")
    protected void handleAllResultNotifications(Vector resultNotifications) {
        for (Object obj : resultNotifications) {
            ACLMessage notification = (ACLMessage) obj;
            if (notification.getPerformative() == ACLMessage.INFORM) {
                log.info("[ProcurementInitiator] PO creation confirmed by '{}' for material='{}'.",
                        notification.getSender().getLocalName(), procurementRequest.getMaterialCode());

                // Audit trail
                try {
                    services.getAuditService().logAgentDecision(
                            0, // system user
                            "AUTO_PO_CREATED",
                            "PurchaseOrder",
                            procurementRequest.getMaterialCode(),
                            "PO auto-created via Contract-Net for shortfall="
                                    + procurementRequest.getShortfallQuantity());
                } catch (Exception e) {
                    log.error("[ProcurementInitiator] Failed to log audit: {}", e.getMessage(), e);
                }
            } else {
                log.warn("[ProcurementInitiator] Unexpected notification performative={} from '{}'.",
                        ACLMessage.getPerformative(notification.getPerformative()),
                        notification.getSender().getLocalName());
            }
        }
    }
}
