package pharma.agent.operational;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import pharma.agent.behaviour.RequestHandlerBehaviour;
import pharma.agent.core.BasePharmaAgent;
import pharma.agent.ontology.AgentActions;
import pharma.dto.AgentRequestEnvelope;
import pharma.dto.AgentResponseEnvelope;
import pharma.dto.FeasibilityRequestDTO;
import pharma.dto.MaterialAvailabilityDTO;
import pharma.dto.ProductionCapacityDTO;

/**
 * ProductionAgent — Phase 6 operational agent.
 *
 * <p>Handles:
 * <ul>
 *   <li>{@link AgentActions#MANUFACTURING_FEASIBILITY} — checks whether all BOM components
 *       are available in sufficient quantity for a planned production run.</li>
 *   <li>{@link AgentActions#CHECK_CAPACITY} — checks floor capacity for the run on a given date.</li>
 * </ul>
 *
 * <p>Architecture rule: delegates exclusively to
 * {@code this.services.getProductionService()}.  No JDBC calls.
 *
 * <p>Arguments: arg[0] = {@link pharma.config.ApplicationServices}
 */
public class ProductionAgent extends BasePharmaAgent {

    private static final ObjectMapper MAPPER_WITH_TIME = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    protected void setup() {
        super.setup();
        addBehaviour(new ProductionRequestBehaviour());
        logger.info("[ProductionAgent] Ready to evaluate BOM feasibility and capacity.");
    }

    // =========================================================================
    // Inner behaviour
    // =========================================================================

    private class ProductionRequestBehaviour extends RequestHandlerBehaviour {

        @Override
        protected AgentResponseEnvelope<?> handle(AgentRequestEnvelope<?> request) throws Exception {
            return switch (request.getAction()) {
                case AgentActions.MANUFACTURING_FEASIBILITY -> handleFeasibility(request);
                case AgentActions.CHECK_CAPACITY            -> handleCapacity(request);
                default -> AgentResponseEnvelope.failure(
                        request.getTransactionId(), request.getAction(),
                        "ProductionAgent: unsupported action '" + request.getAction() + "'");
            };
        }

        private AgentResponseEnvelope<List<MaterialAvailabilityDTO>> handleFeasibility(
                AgentRequestEnvelope<?> request) throws Exception {

            @SuppressWarnings("null")
            FeasibilityRequestDTO req = MAPPER_WITH_TIME
                    .convertValue(request.getPayload(), FeasibilityRequestDTO.class);

            logger.info("[ProductionAgent] MANUFACTURING_FEASIBILITY bomId={} plannedQty={}",
                    req.getBomId(), req.getPlannedQuantity());

            List<MaterialAvailabilityDTO> bomCheck = services.getProductionService()
                    .checkBomMaterialAvailability(req.getBomId(), req.getPlannedQuantity());

            long shortfalls = bomCheck.stream()
                    .filter(m -> !m.isAvailable()).count();
            logger.info("[ProductionAgent] BOM check: {} component(s), {} shortfall(s).",
                    bomCheck.size(), shortfalls);

            return AgentResponseEnvelope.success(
                    request.getTransactionId(), request.getAction(), bomCheck);
        }

        private AgentResponseEnvelope<ProductionCapacityDTO> handleCapacity(
                AgentRequestEnvelope<?> request) throws Exception {

            @SuppressWarnings("null")
            FeasibilityRequestDTO req = MAPPER_WITH_TIME
                    .convertValue(request.getPayload(), FeasibilityRequestDTO.class);

            logger.info("[ProductionAgent] CHECK_CAPACITY bomId={} plannedQty={} date={}",
                    req.getBomId(), req.getPlannedQuantity(), req.getRequestedDate());

            ProductionCapacityDTO capacity = services.getProductionService()
                    .checkCapacity(req.getBomId(), req.getPlannedQuantity(), req.getRequestedDate());

            logger.info("[ProductionAgent] Capacity check result: capacityAvailable={}",
                    capacity.isCapacityAvailable());

            return AgentResponseEnvelope.success(
                    request.getTransactionId(), request.getAction(), capacity);
        }
    }
}
