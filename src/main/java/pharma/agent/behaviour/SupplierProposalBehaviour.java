package pharma.agent.behaviour;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jade.core.Agent;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetResponder;
import pharma.config.ApplicationServices;
import pharma.dto.PODraftDTO;
import pharma.dto.ProcurementRequestDTO;
import pharma.dto.SupplierProposalDTO;
import pharma.dto.SupplierScoreDTO;
import pharma.model.PurchaseOrder;

/**
 * FIPA Contract-Net responder for an individual supplier agent.
 *
 * <p>When a CFP arrives from the {@code ProcurementWorkflowAgent}, this behaviour:
 * <ol>
 *   <li>Deserialises the {@link ProcurementRequestDTO} from the CFP content.</li>
 *   <li>Checks available capacity via {@code SupplierService}.</li>
 *   <li>Calculates a composite score (lead-time 40 %, capacity 30 %, price 30 %).</li>
 *   <li>Returns a PROPOSE containing the serialised {@link SupplierProposalDTO}.</li>
 *   <li>On ACCEPT_PROPOSAL, creates a {@link PODraftDTO} and persists the PO.</li>
 * </ol>
 */
public class SupplierProposalBehaviour extends ContractNetResponder {

    private static final Logger log = LoggerFactory.getLogger(SupplierProposalBehaviour.class);

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // Score weight constants
    private static final double LEAD_TIME_WEIGHT  = 0.4;
    private static final double CAPACITY_WEIGHT   = 0.3;
    private static final double PRICE_WEIGHT      = 0.3;

    // Normalisation baselines
    private static final double MAX_LEAD_TIME_DAYS = 30.0;
    private static final double MAX_UNIT_PRICE     = 1_000.0;

    private final ApplicationServices services;
    private final int supplierId;

    /**
     * Creates a supplier proposal responder.
     *
     * @param agent      the owning JADE agent
     * @param template   message template to match incoming CFPs
     * @param services   application service composition root
     * @param supplierId the database ID of this supplier
     */
    public SupplierProposalBehaviour(Agent agent, MessageTemplate template,
                                     ApplicationServices services, int supplierId) {
        super(agent, template);
        this.services = services;
        this.supplierId = supplierId;
    }

    // -------------------------------------------------------------------------
    // Handle CFP — build and return a proposal
    // -------------------------------------------------------------------------

    /**
     * Evaluates the Call-For-Proposals and returns a PROPOSE with the supplier's
     * quotation and composite score.
     *
     * @param cfp the incoming CFP message
     * @return the PROPOSE reply containing a serialised {@link SupplierProposalDTO}
     * @throws RefuseException        if the supplier cannot fulfil the request
     * @throws NotUnderstoodException if the CFP content is malformed
     */
    @Override
    protected ACLMessage handleCfp(ACLMessage cfp) throws RefuseException, NotUnderstoodException {
        try {
            @SuppressWarnings("null")
            ProcurementRequestDTO request = MAPPER.readValue(
                    cfp.getContent(), ProcurementRequestDTO.class);

            log.info("[SupplierProposal-{}] CFP received for material='{}' qty={}",
                    supplierId, request.getMaterialCode(), request.getShortfallQuantity());

            // Check this supplier's capacity for the requested material
            double capacity = services.getSupplierService()
                    .getSupplierCapacity(supplierId, request.getMaterialCode());

            if (capacity <= 0) {
                log.info("[SupplierProposal-{}] No capacity for material='{}' — refusing.",
                        supplierId, request.getMaterialCode());
                throw new RefuseException("Supplier " + supplierId
                        + " has no capacity for material " + request.getMaterialCode());
            }

            // Retrieve this supplier's score/metadata from the ranking service
            List<SupplierScoreDTO> rankings = services.getSupplierService()
                    .rankApprovedSuppliersForMaterial(request.getMaterialCode());

            SupplierScoreDTO myScore = rankings.stream()
                    .filter(s -> s.getSupplierId() == supplierId)
                    .findFirst()
                    .orElse(null);

            if (myScore == null) {
                log.info("[SupplierProposal-{}] Not an approved supplier for material='{}' — refusing.",
                        supplierId, request.getMaterialCode());
                throw new RefuseException("Supplier " + supplierId
                        + " is not approved for material " + request.getMaterialCode());
            }

            double availableQty = Math.min(capacity, request.getShortfallQuantity());
            double compositeScore = calculateCompositeScore(
                    myScore.getLeadTimeDays(), availableQty,
                    request.getShortfallQuantity(), myScore.getUnitPrice());

            SupplierProposalDTO proposal = new SupplierProposalDTO(
                    supplierId,
                    myScore.getSupplierName(),
                    myScore.getUnitPrice(),
                    myScore.getLeadTimeDays(),
                    availableQty,
                    compositeScore);

            ACLMessage propose = cfp.createReply();
            propose.setPerformative(ACLMessage.PROPOSE);
            propose.setContent(MAPPER.writeValueAsString(proposal));

            log.info("[SupplierProposal-{}] PROPOSE: score={} price={} leadTime={} qty={}",
                    supplierId, String.format("%.3f", compositeScore),
                    myScore.getUnitPrice(), myScore.getLeadTimeDays(), availableQty);

            return propose;

        } catch (RefuseException re) {
            throw re;
        } catch (Exception e) {
            log.error("[SupplierProposal-{}] Error processing CFP: {}", supplierId, e.getMessage(), e);
            throw new NotUnderstoodException("Failed to process CFP: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Handle acceptance — create the Purchase Order
    // -------------------------------------------------------------------------

    /**
     * Called when this supplier's proposal is accepted.
     * Creates and persists a Purchase Order via the database service.
     *
     * @param cfp            the original CFP
     * @param propose        the PROPOSE message that was accepted
     * @param accept         the ACCEPT_PROPOSAL message
     * @return INFORM confirming PO creation, or FAILURE
     */
    @Override
    protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose,
                                               ACLMessage accept) throws FailureException {
        try {
            @SuppressWarnings("null")
            SupplierProposalDTO winningProposal = MAPPER.readValue(
                    propose.getContent(), SupplierProposalDTO.class);

            @SuppressWarnings("null")
            ProcurementRequestDTO originalRequest = MAPPER.readValue(
                    cfp.getContent(), ProcurementRequestDTO.class);

            // Build the PO draft
            PODraftDTO draft = new PODraftDTO(
                    originalRequest.getMaterialCode(),
                    winningProposal.getSupplierId(),
                    winningProposal.getAvailableQuantity(),
                    winningProposal.getQuotedPrice(),
                    LocalDate.now().plusDays(winningProposal.getLeadTimeDays()),
                    "ContractNet-AutoProcurement");

            log.info("[SupplierProposal-{}] ACCEPT received — creating PO: material='{}' qty={} price={}",
                    supplierId, draft.getMaterialCode(), draft.getQuantity(), draft.getUnitPrice());

            // Create the PO via DatabaseService
            PurchaseOrder.PurchaseOrderItem lineItem = new PurchaseOrder.PurchaseOrderItem(
                    draft.getMaterialCode(),
                    (int) draft.getQuantity(),
                    draft.getUnitPrice());

            java.util.List<PurchaseOrder.PurchaseOrderItem> items = new java.util.ArrayList<>();
            items.add(lineItem);

            PurchaseOrder po = new PurchaseOrder(
                    draft.getSupplierId(),
                    winningProposal.getSupplierName(),
                    LocalDate.now(),
                    draft.getExpectedDelivery(),
                    draft.getQuantity() * draft.getUnitPrice(),
                    "DRAFT",
                    items);

            boolean created = services.getDatabaseService().createPurchaseOrder(po);

            if (!created) {
                throw new FailureException("DatabaseService.createPurchaseOrder returned false");
            }

            // Audit log
            services.getAuditService().logAgentDecision(
                    0, "AUTO_PO_CREATED", "PurchaseOrder",
                    draft.getMaterialCode(),
                    "Auto-PO: supplier=" + winningProposal.getSupplierName()
                            + " qty=" + draft.getQuantity()
                            + " price=" + draft.getUnitPrice());

            ACLMessage inform = accept.createReply();
            inform.setPerformative(ACLMessage.INFORM);
            inform.setContent(MAPPER.writeValueAsString(draft));

            log.info("[SupplierProposal-{}] PO created successfully for material='{}'.",
                    supplierId, draft.getMaterialCode());

            return inform;

        } catch (FailureException fe) {
            throw fe;
        } catch (Exception e) {
            log.error("[SupplierProposal-{}] Failed to create PO: {}", supplierId, e.getMessage(), e);
            throw new FailureException("PO creation failed: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Composite score calculation
    // -------------------------------------------------------------------------

    /**
     * Calculates a weighted composite score for this supplier's proposal.
     *
     * <p>Formula: {@code 0.4 * leadTimeScore + 0.3 * capacityScore + 0.3 * priceScore}
     *
     * <p>Each component is normalised to [0, 1]:
     * <ul>
     *   <li><b>leadTimeScore</b>: {@code 1 - (leadTimeDays / MAX_LEAD_TIME_DAYS)}</li>
     *   <li><b>capacityScore</b>: {@code availableQty / requiredQty} (capped at 1.0)</li>
     *   <li><b>priceScore</b>: {@code 1 - (unitPrice / MAX_UNIT_PRICE)}</li>
     * </ul>
     *
     * @param leadTimeDays the supplier's quoted lead time
     * @param availableQty the quantity the supplier can fulfil
     * @param requiredQty  the total quantity requested
     * @param unitPrice    the supplier's quoted unit price
     * @return the composite score in [0, 1]
     */
    private double calculateCompositeScore(int leadTimeDays, double availableQty,
                                           double requiredQty, double unitPrice) {
        double leadTimeScore = Math.max(0, 1.0 - (leadTimeDays / MAX_LEAD_TIME_DAYS));
        double capacityScore = Math.min(1.0, availableQty / Math.max(1, requiredQty));
        double priceScore    = Math.max(0, 1.0 - (unitPrice / MAX_UNIT_PRICE));

        return (LEAD_TIME_WEIGHT * leadTimeScore)
             + (CAPACITY_WEIGHT  * capacityScore)
             + (PRICE_WEIGHT     * priceScore);
    }
}
