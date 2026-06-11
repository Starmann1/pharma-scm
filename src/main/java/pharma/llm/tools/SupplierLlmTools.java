package pharma.llm.tools;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import pharma.dto.SupplierScoreDTO;
import pharma.service.SupplierService;

/**
 * LangChain4j tool wrapper that exposes {@link SupplierService} methods
 * as LLM-callable tools.
 *
 * <p>Provides supplier ranking by material and individual supplier capacity queries.
 */
public class SupplierLlmTools {

    private static final Logger log = LoggerFactory.getLogger(SupplierLlmTools.class);

    private final SupplierService supplierService;

    /**
     * Constructs a new tool wrapper backed by the given service.
     *
     * @param supplierService the supplier service instance
     */
    public SupplierLlmTools(SupplierService supplierService) {
        this.supplierService = supplierService;
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
