package pharma.agent.behaviour;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import pharma.config.ApplicationServices;
import pharma.dto.PODraftDTO;
import pharma.model.PurchaseOrder;

import java.time.LocalDate;
import java.util.ArrayList;

/**
 * One-shot behaviour that creates a Purchase Order from a {@link PODraftDTO}.
 *
 * <p>This behaviour is typically added after a successful Contract-Net negotiation
 * or when a PO needs to be created programmatically. It delegates persistence
 * to {@code DatabaseService.createPurchaseOrder()} and records an audit entry.
 *
 * <p>Architecture rule: no direct JDBC — all access via {@code services.*}.
 */
public class POCreationBehaviour extends OneShotBehaviour {

    private static final Logger log = LoggerFactory.getLogger(POCreationBehaviour.class);

    private final PODraftDTO draft;
    private final ApplicationServices services;

    /**
     * Creates a PO creation behaviour.
     *
     * @param agent    the owning JADE agent
     * @param draft    the purchase order draft to persist
     * @param services application service composition root
     */
    public POCreationBehaviour(Agent agent, PODraftDTO draft, ApplicationServices services) {
        super(agent);
        this.draft = draft;
        this.services = services;
    }

    /**
     * Executes exactly once — builds a {@link PurchaseOrder} from the draft,
     * persists it, and logs an audit trail entry.
     */
    @Override
    public void action() {
        try {
            log.info("[POCreation] Creating PO: material='{}' supplier={} qty={} unitPrice={} delivery={}",
                    draft.getMaterialCode(), draft.getSupplierId(),
                    draft.getQuantity(), draft.getUnitPrice(), draft.getExpectedDelivery());

            // Build the PurchaseOrder model
            double totalAmount = draft.getQuantity() * draft.getUnitPrice();

            PurchaseOrder.PurchaseOrderItem lineItem = new PurchaseOrder.PurchaseOrderItem(
                    draft.getMaterialCode(),
                    (int) draft.getQuantity(),
                    draft.getUnitPrice());

            ArrayList<PurchaseOrder.PurchaseOrderItem> items = new ArrayList<>();
            items.add(lineItem);

            PurchaseOrder po = new PurchaseOrder(
                    draft.getSupplierId(),
                    "Supplier-" + draft.getSupplierId(),  // supplierName placeholder
                    LocalDate.now(),                       // orderDate
                    draft.getExpectedDelivery(),            // expectedDate
                    totalAmount,
                    "DRAFT",
                    items);

            boolean success = services.getDatabaseService().createPurchaseOrder(po);

            if (success) {
                log.info("[POCreation] PO created successfully for material='{}' total={}",
                        draft.getMaterialCode(), totalAmount);

                // Audit trail
                services.getAuditService().logAgentDecision(
                        0, // system user
                        "PO_CREATED",
                        "PurchaseOrder",
                        draft.getMaterialCode(),
                        "PO created: supplier=" + draft.getSupplierId()
                                + " qty=" + draft.getQuantity()
                                + " total=" + totalAmount
                                + " triggeredBy=" + draft.getTriggeredBy());
            } else {
                log.error("[POCreation] DatabaseService.createPurchaseOrder returned false for material='{}'.",
                        draft.getMaterialCode());
            }

        } catch (Exception e) {
            log.error("[POCreation] Failed to create PO for material='{}': {}",
                    draft.getMaterialCode(), e.getMessage(), e);
        }
    }
}
