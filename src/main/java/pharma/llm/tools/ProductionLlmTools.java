package pharma.llm.tools;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import pharma.dto.MaterialAvailabilityDTO;
import pharma.dto.ProductionCapacityDTO;
import pharma.service.ProductionService;

/**
 * LangChain4j tool wrapper that exposes {@link ProductionService} methods
 * as LLM-callable tools.
 *
 * <p>The underlying service uses integer BOM IDs and a {@link LocalDate} parameter.
 * This wrapper accepts BOM IDs as integers and defaults the requested date to today
 * when the LLM invokes the capacity check.
 */
public class ProductionLlmTools {

    private static final Logger log = LoggerFactory.getLogger(ProductionLlmTools.class);

    private final ProductionService productionService;

    /**
     * Constructs a new tool wrapper backed by the given service.
     *
     * @param productionService the production service instance
     */
    public ProductionLlmTools(ProductionService productionService) {
        this.productionService = productionService;
    }

    /**
     * Checks whether production capacity is available for a given BOM and quantity.
     *
     * @param bomId    the Bill of Materials ID
     * @param quantity the planned production quantity
     * @return capacity details including availability and constraints
     */
    @Tool("Check if production capacity is available for a given BOM (Bill of Materials) ID " +
          "and planned quantity. Returns whether capacity is available and any constraints.")
    public ProductionCapacityDTO checkProductionCapacity(
            @P("The Bill of Materials ID (integer)") int bomId,
            @P("The planned production quantity") double quantity) {
        log.info("[ProductionLlmTools] checkProductionCapacity bomId={} quantity={}", bomId, quantity);
        try {
            return productionService.checkCapacity(bomId, quantity, LocalDate.now());
        } catch (Exception e) {
            log.error("[ProductionLlmTools] checkProductionCapacity failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to check production capacity for BOM " + bomId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Checks BOM material availability for a planned production run.
     *
     * @param bomId    the Bill of Materials ID
     * @param quantity the planned production quantity
     * @return list of material availability statuses for each BOM component
     */
    @Tool("Check whether all raw materials in a BOM (Bill of Materials) are available " +
          "for a planned production quantity. Returns availability status for each material component.")
    public List<MaterialAvailabilityDTO> checkBomAvailability(
            @P("The Bill of Materials ID (integer)") int bomId,
            @P("The planned production quantity") double quantity) {
        log.info("[ProductionLlmTools] checkBomAvailability bomId={} quantity={}", bomId, quantity);
        try {
            return productionService.checkBomMaterialAvailability(bomId, quantity);
        } catch (Exception e) {
            log.error("[ProductionLlmTools] checkBomAvailability failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to check BOM availability for BOM " + bomId + ": " + e.getMessage(), e);
        }
    }
}
