package pharma.agent.behaviour;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import pharma.agent.ontology.AgentActions;
import pharma.agent.ontology.AgentNames;
import pharma.config.ApplicationServices;
import pharma.dto.AgentRequestEnvelope;
import pharma.dto.MaterialAvailabilityDTO;
import pharma.dto.ProcurementRequestDTO;

/**
 * Periodic behaviour that scans for materials below their reorder threshold.
 *
 * <p>Every tick (default 30 minutes) the behaviour calls
 * {@link pharma.service.InventoryService#findLowStockMaterials()} and,
 * for each material that is below safety stock, creates a
 * {@link ProcurementRequestDTO} wrapped in an {@link AgentRequestEnvelope}
 * and sends it as an ACL REQUEST to the {@code ProcurementWorkflowAgent}.
 *
 * <p>This behaviour is added by the {@code ProcurementWorkflowAgent} at startup.
 */
public class LowStockMonitorBehaviour extends TickerBehaviour {

    private static final Logger log = LoggerFactory.getLogger(LowStockMonitorBehaviour.class);

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final ApplicationServices services;

    /**
     * Creates a new low-stock monitor.
     *
     * @param agent    the owning JADE agent
     * @param periodMs tick interval in milliseconds (recommended: 1_800_000 = 30 min)
     * @param services the application service composition root
     */
    public LowStockMonitorBehaviour(Agent agent, long periodMs, ApplicationServices services) {
        super(agent, periodMs);
        this.services = services;
    }

    /**
     * Executed on every tick — scans inventory for low-stock materials and
     * dispatches procurement requests for each shortfall.
     */
    @Override
    protected void onTick() {
        try {
            List<MaterialAvailabilityDTO> lowStockItems =
                    services.getInventoryService().findLowStockMaterials();

            if (lowStockItems.isEmpty()) {
                log.info("[LowStockMonitor] No low-stock materials detected.");
                return;
            }

            log.warn("[LowStockMonitor] Detected {} low-stock material(s) — dispatching procurement requests.",
                    lowStockItems.size());

            for (MaterialAvailabilityDTO item : lowStockItems) {
                double shortfall = item.getRequiredQuantity() - item.getAvailableQuantity();
                if (shortfall <= 0) {
                    shortfall = item.getRequiredQuantity(); // fallback: order full reorder qty
                }

                String urgency = item.getAvailableQuantity() <= 0 ? "HIGH"
                        : item.isBelowSafetyStock() ? "MEDIUM" : "LOW";

                ProcurementRequestDTO procReq = new ProcurementRequestDTO(
                        item.getMaterialCode(), shortfall, urgency);

                AgentRequestEnvelope<ProcurementRequestDTO> envelope =
                        new AgentRequestEnvelope<>(
                                AgentActions.PROCUREMENT_WORKFLOW,
                                0,          // system-triggered (no user)
                                30_000L,    // 30-second deadline
                                procReq);

                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                msg.addReceiver(new AID(AgentNames.PROCUREMENT, AID.ISLOCALNAME));
                msg.setConversationId(envelope.getTransactionId());
                msg.setContent(MAPPER.writeValueAsString(envelope));

                myAgent.send(msg);

                log.info("[LowStockMonitor] Sent PROCUREMENT_WORKFLOW for material='{}' shortfall={} urgency={}",
                        item.getMaterialCode(), shortfall, urgency);
            }

        } catch (Exception e) {
            log.error("[LowStockMonitor] Error during low-stock scan: {}", e.getMessage(), e);
        }
    }
}
