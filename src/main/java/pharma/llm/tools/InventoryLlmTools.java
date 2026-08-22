package pharma.llm.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import pharma.dto.MaterialAvailabilityDTO;
import pharma.model.Material;
import pharma.model.Stock;
import pharma.service.DatabaseService;
import pharma.service.InventoryService;

/**
 * LangChain4j tool wrapper that exposes inventory and batch queries
 * as LLM-callable tools.
 */
public class InventoryLlmTools {

    private static final Logger log = LoggerFactory.getLogger(InventoryLlmTools.class);

    private final InventoryService inventoryService;
    private final DatabaseService databaseService;

    /**
     * Constructs a new tool wrapper backed by the given services.
     */
    public InventoryLlmTools(InventoryService inventoryService, DatabaseService databaseService) {
        this.inventoryService = inventoryService;
        this.databaseService = databaseService;
    }

    public InventoryLlmTools(InventoryService inventoryService) {
        this(inventoryService, DatabaseService.getInstance());
    }

    /**
     * Retrieves all details for a specific batch by its batch number.
     */
    @Tool("Get full details for a specific inventory batch by its batch number. " +
          "Returns material code, quantity, reserved quantity, available quantity, unit cost, " +
          "total batch valuation, expiry date, manufacture date, QC status, and warehouse location.")
    public Map<String, Object> getBatchDetails(
            @P("The exact or partial batch number to look up, e.g. 'BATCH-RM-PARA-001' or 'B-AMX-08'") String batchNumber) {
        log.info("[InventoryLlmTools] getBatchDetails batchNumber='{}'", batchNumber);
        Map<String, Object> res = new LinkedHashMap<>();
        if (batchNumber == null || batchNumber.isBlank()) {
            res.put("error", "Batch number cannot be empty");
            return res;
        }

        try {
            // Direct exact match
            Stock stock = databaseService.getStockByBatchNumber(batchNumber.trim());
            if (stock == null) {
                // Try case-insensitive or partial match
                List<Stock> all = databaseService.getDetailedInventoryReport();
                stock = all.stream()
                        .filter(s -> s.getBatchNumber() != null && s.getBatchNumber().equalsIgnoreCase(batchNumber.trim()))
                        .findFirst()
                        .orElse(null);
            }

            if (stock != null) {
                res.put("batchNumber", stock.getBatchNumber());
                res.put("materialCode", stock.getMaterialCode());
                res.put("quantity", stock.getQuantity());
                res.put("reservedQuantity", stock.getReservedQuantity());
                res.put("availableQuantity", stock.getAvailableQuantity());
                res.put("unitCost", "$" + String.format("%.2f", stock.getUnitCost()));
                res.put("totalBatchValuation", "$" + String.format("%.2f", stock.getQuantity() * stock.getUnitCost()));
                res.put("qcStatus", stock.getQcStatus());
                res.put("locationCode", stock.getLocationCode());
                res.put("mfgDate", stock.getMfgDate() != null ? stock.getMfgDate().toString() : "N/A");
                res.put("expDate", stock.getExpDate() != null ? stock.getExpDate().toString() : "N/A");
            } else {
                res.put("found", false);
                res.put("message", "No batch found with identifier '" + batchNumber + "'.");
            }
        } catch (Exception e) {
            log.error("[InventoryLlmTools] getBatchDetails error: {}", e.getMessage(), e);
            res.put("error", e.getMessage());
        }
        return res;
    }

    /**
     * Lists all stock batches in the warehouse.
     */
    @Tool("List all active inventory batches stored across all warehouses with their batch numbers, " +
          "material codes, quantities, unit costs, QC statuses, and expiry dates.")
    public List<Map<String, Object>> getAllStockBatches() {
        log.info("[InventoryLlmTools] getAllStockBatches");
        try {
            List<Stock> all = databaseService.getDetailedInventoryReport();
            return all.stream().map(s -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("batchNumber", s.getBatchNumber());
                map.put("materialCode", s.getMaterialCode());
                map.put("quantity", s.getQuantity());
                map.put("reservedQuantity", s.getReservedQuantity());
                map.put("unitCost", "$" + String.format("%.2f", s.getUnitCost()));
                map.put("qcStatus", s.getQcStatus());
                map.put("locationCode", s.getLocationCode());
                map.put("expDate", s.getExpDate() != null ? s.getExpDate().toString() : "N/A");
                return map;
            }).toList();
        } catch (Exception e) {
            log.error("[InventoryLlmTools] getAllStockBatches error: {}", e.getMessage(), e);
            return List.of(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Retrieves material master details.
     */
    @Tool("Get material master specification for a given material code, including material name, " +
          "type (RAW_MATERIAL, PACKAGING, FINISHED_GOOD), unit of measure, and safety reorder level.")
    public Map<String, Object> getMaterialDetails(
            @P("The material code, e.g. 'RM-PARA-001' or 'MAT-001'") String materialCode) {
        log.info("[InventoryLlmTools] getMaterialDetails materialCode='{}'", materialCode);
        Map<String, Object> res = new LinkedHashMap<>();
        try {
            Material m = databaseService.getDrugByMaterialCode(materialCode);
            if (m != null) {
                res.put("materialCode", m.getMaterialCode());
                res.put("brandName", m.getBrandName());
                res.put("genericName", m.getGenericName());
                res.put("manufacturer", m.getManufacturer());
                res.put("materialType", m.getMaterialType() != null ? m.getMaterialType().name() : "UNKNOWN");
                res.put("unitOfMeasure", m.getUnitOfMeasure() != null ? m.getUnitOfMeasure().getDisplayName() : "N/A");
                res.put("reorderLevel", m.getReorderLevel());
                res.put("formulation", m.getFormulation());
                res.put("strength", m.getStrength());
                res.put("storageConditions", m.getStorageConditions());
            } else {
                res.put("found", false);
                res.put("message", "No material found for code: " + materialCode);
            }
        } catch (Exception e) {
            res.put("error", e.getMessage());
        }
        return res;
    }

    /**
     * Checks stock availability for a specific material and required quantity.
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
