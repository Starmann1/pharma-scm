package pharma.llm.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import pharma.dto.SupplierScoreDTO;
import pharma.model.Supplier;
import pharma.service.DatabaseService;
import pharma.service.SupplierService;

/**
 * LangChain4j tool wrapper that exposes {@link SupplierService} and supplier registry methods
 * as LLM-callable tools.
 */
public class SupplierLlmTools {

    private static final Logger log = LoggerFactory.getLogger(SupplierLlmTools.class);

    private final SupplierService supplierService;
    private final DatabaseService databaseService;

    /**
     * Constructs a new tool wrapper backed by the given services.
     */
    public SupplierLlmTools(SupplierService supplierService, DatabaseService databaseService) {
        this.supplierService = supplierService;
        this.databaseService = databaseService;
    }

    public SupplierLlmTools(SupplierService supplierService) {
        this(supplierService, DatabaseService.getInstance());
    }

    /**
     * Lists all registered pharmaceutical suppliers with compliance statuses.
     */
    @Tool("List all registered vendors and suppliers in the pharma supply chain network, " +
          "including supplier name, ID, status (APPROVED, PENDING, REJECTED), drug license number, phone, and email.")
    public List<Map<String, Object>> getAllSuppliers() {
        log.info("[SupplierLlmTools] getAllSuppliers");
        try {
            List<Supplier> sups = databaseService.getAllSuppliers();
            return sups.stream().map(s -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("supplierId", s.getSupplierId());
                map.put("supplierName", s.getSupplierName());
                map.put("status", s.getSupplierStatus());
                map.put("drugLicenseNo", s.getDrugLicenseNo());
                map.put("phone", s.getPhone());
                map.put("email", s.getEmail());
                return map;
            }).toList();
        } catch (Exception e) {
            log.error("[SupplierLlmTools] getAllSuppliers error: {}", e.getMessage(), e);
            return List.of(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Ranks all approved suppliers for a given material code by composite score.
     *
     * @param materialCode the material code to find suppliers for
     * @return ranked list of suppliers with scores, prices, and lead times
     */
    @Tool("Rank all approved suppliers for a given material code by their composite quality, " +
          "delivery, and price score. Returns supplier name, score, unit price, and lead time.")
    public List<SupplierScoreDTO> rankSuppliers(
            @P("The material code to find suppliers for, e.g. 'RM-001'") String materialCode) {
        log.info("[SupplierLlmTools] rankSuppliers materialCode='{}'", materialCode);
        try {
            return supplierService.rankApprovedSuppliersForMaterial(materialCode);
        } catch (Exception e) {
            log.error("[SupplierLlmTools] rankSuppliers failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to rank suppliers for " + materialCode + ": " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves the production capacity of a specific supplier for a material.
     *
     * @param supplierId   the supplier's database ID
     * @param materialCode the material code
     * @return the supplier's available capacity as a quantity
     */
    @Tool("Get the available production capacity of a specific supplier for a given material. " +
          "Returns the maximum quantity the supplier can deliver.")
    public double getSupplierCapacity(
            @P("The supplier ID (integer)") int supplierId,
            @P("The material code, e.g. 'RM-001'") String materialCode) {
        log.info("[SupplierLlmTools] getSupplierCapacity supplierId={} materialCode='{}'", supplierId, materialCode);
        try {
            return supplierService.getSupplierCapacity(supplierId, materialCode);
        } catch (Exception e) {
            log.error("[SupplierLlmTools] getSupplierCapacity failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get capacity for supplier " + supplierId + ": " + e.getMessage(), e);
        }
    }
}
