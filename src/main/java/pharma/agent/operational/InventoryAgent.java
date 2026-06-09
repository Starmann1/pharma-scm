package pharma.agent.operational;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import pharma.agent.behaviour.RequestHandlerBehaviour;
import pharma.agent.core.BasePharmaAgent;
import pharma.agent.ontology.AgentActions;
import pharma.dto.AgentRequestEnvelope;
import pharma.dto.AgentResponseEnvelope;
import pharma.dto.MaterialAvailabilityDTO;
import pharma.dto.StockCheckRequestDTO;

/**
 * InventoryAgent — Phase 6 operational agent.
 *
 * <p>Handles:
 * <ul>
 *   <li>{@link AgentActions#CHECK_STOCK} — checks stock level for a specific material.</li>
 *   <li>{@link AgentActions#LOW_STOCK_ALERT} — returns all materials currently below reorder threshold.</li>
 * </ul>
 *
 * <p>Architecture rule: delegates exclusively to
 * {@code this.services.getInventoryService()}.  No JDBC calls.
 *
 * <p>Arguments: arg[0] = {@link pharma.config.ApplicationServices}
 */
public class InventoryAgent extends BasePharmaAgent {

    @Override
    protected void setup() {
        super.setup();
        addBehaviour(new InventoryRequestBehaviour());
        logger.info("[InventoryAgent] Ready to handle stock queries.");
    }

    // =========================================================================
    // Inner behaviour
    // =========================================================================

    private class InventoryRequestBehaviour extends RequestHandlerBehaviour {

        @Override
        protected AgentResponseEnvelope<?> handle(AgentRequestEnvelope<?> request) throws Exception {
            return switch (request.getAction()) {
                case AgentActions.CHECK_STOCK -> handleCheckStock(request);
                case AgentActions.LOW_STOCK_ALERT -> handleLowStockAlert(request);
                default -> AgentResponseEnvelope.failure(
                        request.getTransactionId(), request.getAction(),
                        "InventoryAgent: unsupported action '" + request.getAction() + "'");
            };
        }

        private AgentResponseEnvelope<MaterialAvailabilityDTO> handleCheckStock(
                AgentRequestEnvelope<?> request) throws Exception {

            StockCheckRequestDTO req = extractPayload(request, StockCheckRequestDTO.class);

            logger.info("[InventoryAgent] CHECK_STOCK materialCode='{}' requiredQty={}",
                    req.getMaterialCode(), req.getRequiredQuantity());

            MaterialAvailabilityDTO result = services.getInventoryService()
                    .checkMaterialAvailability(req.getMaterialCode(), req.getRequiredQuantity());

            return AgentResponseEnvelope.success(
                    request.getTransactionId(), request.getAction(), result);
        }

        private AgentResponseEnvelope<List<MaterialAvailabilityDTO>> handleLowStockAlert(
                AgentRequestEnvelope<?> request) throws Exception {

            logger.info("[InventoryAgent] LOW_STOCK_ALERT — scanning all materials");

            List<MaterialAvailabilityDTO> lowStockItems =
                    services.getInventoryService().findLowStockMaterials();

            logger.info("[InventoryAgent] Found {} low-stock material(s).", lowStockItems.size());

            return AgentResponseEnvelope.success(
                    request.getTransactionId(), request.getAction(), lowStockItems);
        }

        /**
         * Converts the raw Object payload (Jackson deserialises maps) into the target DTO type.
         */
        private <T> T extractPayload(AgentRequestEnvelope<?> request, Class<T> type) {
            ObjectMapper mapper = MAPPER;
            return mapper.convertValue(request.getPayload(), type);
        }
    }
}
