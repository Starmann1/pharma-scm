package pharma.agent.operational;

import java.util.List;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import pharma.agent.behaviour.LowStockMonitorBehaviour;
import pharma.agent.behaviour.ProcurementInitiatorBehaviour;
import pharma.agent.behaviour.RequestHandlerBehaviour;
import pharma.agent.core.BasePharmaAgent;
import pharma.agent.ontology.AgentActions;
import pharma.agent.ontology.AgentNames;
import pharma.dto.AgentRequestEnvelope;
import pharma.dto.AgentResponseEnvelope;
import pharma.dto.ProcurementRequestDTO;
import pharma.dto.SupplierScoreDTO;

/**
 * ProcurementWorkflowAgent — Phase 7 operational agent for automated procurement.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Runs a {@link LowStockMonitorBehaviour} every 30 minutes to detect
 *       materials below their reorder threshold.</li>
 *   <li>Receives {@code PROCUREMENT_WORKFLOW} requests (from the monitor or
 *       from other agents/UI) and orchestrates a FIPA Contract-Net negotiation
 *       with supplier agents.</li>
 *   <li>Before launching the negotiation, attempts to reserve the material
 *       via {@code InventoryService.reserveMaterial()} to prevent double-ordering.</li>
 * </ul>
 *
 * <p>Architecture rule: delegates exclusively to {@code services.*}. No JDBC.
 *
 * <p>Arguments: arg[0] = {@link pharma.config.ApplicationServices}
 */
public class ProcurementWorkflowAgent extends BasePharmaAgent {

    /** Low-stock scan interval: 15 seconds (for live testing). */
    private static final long MONITOR_PERIOD_MS = 15_000L;

    @Override
    protected void setup() {
        super.setup();

        // 1. Periodic low-stock monitor
        addBehaviour(new LowStockMonitorBehaviour(this, MONITOR_PERIOD_MS, services));
        logger.info("[ProcurementWorkflowAgent] LowStockMonitorBehaviour added (period={}ms).", MONITOR_PERIOD_MS);

        // 2. Request handler for PROCUREMENT_WORKFLOW action
        addBehaviour(new ProcurementRequestBehaviour());
        logger.info("[ProcurementWorkflowAgent] Ready to handle procurement workflows.");
    }

    // =========================================================================
    // Inner behaviour — handles incoming PROCUREMENT_WORKFLOW requests
    // =========================================================================

    /**
     * Cyclic behaviour that listens for {@code PROCUREMENT_WORKFLOW} REQUEST
     * messages. For each request it:
     * <ol>
     *   <li>Reserves the material to prevent duplicate procurement.</li>
     *   <li>Queries all approved suppliers for the material.</li>
     *   <li>Builds a CFP message targeting those suppliers.</li>
     *   <li>Adds a {@link ProcurementInitiatorBehaviour} to run the Contract-Net.</li>
     * </ol>
     */
    private class ProcurementRequestBehaviour extends RequestHandlerBehaviour {

        @Override
        protected AgentResponseEnvelope<?> handle(AgentRequestEnvelope<?> request) throws Exception {
            if (!AgentActions.PROCUREMENT_WORKFLOW.equals(request.getAction())) {
                return AgentResponseEnvelope.failure(
                        request.getTransactionId(), request.getAction(),
                        "ProcurementWorkflowAgent: unsupported action '" + request.getAction() + "'");
            }

            @SuppressWarnings("null")
            ProcurementRequestDTO procReq = MAPPER.convertValue(
                    request.getPayload(), ProcurementRequestDTO.class);

            logger.info("[ProcurementWorkflowAgent] PROCUREMENT_WORKFLOW: material='{}' shortfall={} urgency='{}'",
                    procReq.getMaterialCode(), procReq.getShortfallQuantity(), procReq.getUrgencyLevel());

            // Step 1 — Attempt material reservation to prevent double-ordering
            boolean reserved = false;
            try {
                reserved = services.getInventoryService().reserveMaterial(
                        procReq.getMaterialCode(), procReq.getShortfallQuantity());
            } catch (Exception e) {
                logger.warn("[ProcurementWorkflowAgent] Material reservation failed (non-fatal): {}",
                        e.getMessage());
            }
            logger.info("[ProcurementWorkflowAgent] Material reservation result: {}", reserved);

            // Step 2 — Get ranked list of approved suppliers
            List<SupplierScoreDTO> suppliers = services.getSupplierService()
                    .rankApprovedSuppliersForMaterial(procReq.getMaterialCode());

            if (suppliers.isEmpty()) {
                logger.warn("[ProcurementWorkflowAgent] No approved suppliers found for material='{}'.",
                        procReq.getMaterialCode());
                return AgentResponseEnvelope.failure(
                        request.getTransactionId(), request.getAction(),
                        "No approved suppliers found for material: " + procReq.getMaterialCode());
            }

            logger.info("[ProcurementWorkflowAgent] Found {} approved supplier(s) — launching Contract-Net.",
                    suppliers.size());

            // Step 3 — Build CFP message for all suppliers
            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            for (SupplierScoreDTO supplier : suppliers) {
                // Each supplier agent should be named "SupplierAgent-<id>"
                cfp.addReceiver(new AID(AgentNames.SUPPLIER + "-" + supplier.getSupplierId(),
                        AID.ISLOCALNAME));
            }
            cfp.setContent(MAPPER.writeValueAsString(procReq));
            cfp.setConversationId("procurement-" + request.getTransactionId());
            cfp.setReplyByDate(new java.util.Date(System.currentTimeMillis() + 30_000L)); // 30s deadline

            // Step 4 — Add the Contract-Net Initiator behaviour
            addBehaviour(new ProcurementInitiatorBehaviour(
                    ProcurementWorkflowAgent.this, cfp, procReq, services));

            // Return immediately — the initiator behaviour handles the rest async
            return AgentResponseEnvelope.success(
                    request.getTransactionId(), request.getAction(),
                    "Procurement Contract-Net initiated for material: " + procReq.getMaterialCode()
                            + " with " + suppliers.size() + " supplier(s).");
        }
    }
}
