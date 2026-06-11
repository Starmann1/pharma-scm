package pharma.llm.tools;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import pharma.dto.MaterialAvailabilityDTO;
import pharma.service.InventoryService;

/**
 * LangChain4j tool wrapper that exposes {@link InventoryService} methods
 * as LLM-callable tools.
 *
 * <p>Each {@code @Tool} method delegates directly to the service layer,
 * converting checked exceptions into unchecked ones for LC4j compatibility.
 */
public class InventoryLlmTools {

    private static final Logger log = LoggerFactory.getLogger(InventoryLlmTools.class);

    private final InventoryService inventoryService;

    /**
     * Constructs a new tool wrapper backed by the given service.
     *
     * @param inventoryService the inventory service instance
     */
    public InventoryLlmTools(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /**
     * Checks stock availability for a specific material and required quantity.
     *
     * @param materialCode  the material code to look up
     * @param requiredQty   the quantity required
     * @return availability details including current stock, reservations, and eligibility
     */
    @Tool("Check inventory stock availability for a specific material code and required quantity. " +
          "Returns current available quantity, reserved quantity, whether stock is sufficient, " +
          "and whether stock is below safety threshold.")
    public MaterialAvailabilityDTO checkStock(
            @P("The material code to check, e.g. 'RM-001'") String materialCode,
            @P("The required quantity to check against available stock") double requiredQty) {
        log.info("[InventoryLlmTools] checkStock materialCode='{}' requiredQty={}", materialCode, requiredQty);
        try {
            return inventoryService.checkMaterialAvailability(materialCode, requiredQty);
        } catch (Exception e) {
            log.error("[InventoryLlmTools] checkStock failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to check stock for " + materialCode + ": " + e.getMessage(), e);
        }
    }

    /**
     * Finds all materials currently below their reorder threshold.
     *
     * @return list of materials that are low on stock
     */
    @Tool("Find all materials currently below their reorder or safety-stock threshold. " +
          "Returns a list of materials with their current stock levels and shortfall details.")
    public List<MaterialAvailabilityDTO> findLowStockMaterials() {
        log.info("[InventoryLlmTools] findLowStockMaterials");
        try {
            return inventoryService.findLowStockMaterials();
        } catch (Exception e) {
            log.error("[InventoryLlmTools] findLowStockMaterials failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to find low-stock materials: " + e.getMessage(), e);
        }
    }
}
