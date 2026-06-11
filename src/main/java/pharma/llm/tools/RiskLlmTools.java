package pharma.llm.tools;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import pharma.dto.RiskReportDTO;
import pharma.service.RiskService;

/**
 * LangChain4j tool wrapper that exposes {@link RiskService} methods
 * as LLM-callable tools.
 *
 * <p>Provides supplier risk scoring, material stockout risk scoring,
 * and retrieval of all current risk reports.
 */
public class RiskLlmTools {

    private static final Logger log = LoggerFactory.getLogger(RiskLlmTools.class);

    private final RiskService riskService;

    /**
     * Constructs a new tool wrapper backed by the given service.
     *
     * @param riskService the risk service instance
     */
    public RiskLlmTools(RiskService riskService) {
        this.riskService = riskService;
    }

    /**
     * Scores the supply-chain risk for a specific supplier.
     *
     * @param supplierId the supplier's database ID
     * @return risk report with score, severity, drivers, and recommended action
     */
    @Tool("Score the supply-chain risk for a specific supplier. Returns a risk report with " +
          "risk score (0-1), severity level, contributing factors, and recommended mitigation action.")
    public RiskReportDTO scoreSupplierRisk(
            @P("The supplier ID (integer)") int supplierId) {
        log.info("[RiskLlmTools] scoreSupplierRisk supplierId={}", supplierId);
        try {
            return riskService.scoreSupplierRisk(supplierId);
        } catch (Exception e) {
            log.error("[RiskLlmTools] scoreSupplierRisk failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to score supplier risk for supplier " + supplierId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Scores the stockout risk for a specific material.
     *
     * @param materialCode the material code
     * @return risk report with score, severity, and mitigation advice
     */
    @Tool("Score the stockout risk for a specific material code. Returns a risk report with " +
          "risk score, severity, contributing factors like demand trends and lead times, " +
          "and recommended procurement actions.")
    public RiskReportDTO scoreMaterialRisk(
            @P("The material code, e.g. 'RM-001'") String materialCode) {
        log.info("[RiskLlmTools] scoreMaterialRisk materialCode='{}'", materialCode);
        try {
            return riskService.scoreMaterialStockoutRisk(materialCode);
        } catch (Exception e) {
            log.error("[RiskLlmTools] scoreMaterialRisk failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to score material risk for " + materialCode + ": " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves all current risk reports across the supply chain.
     *
     * @return list of all active risk reports
     */
    @Tool("Retrieve all current risk reports across the entire pharmaceutical supply chain. " +
          "Returns supplier risks, material stockout risks, and other risk assessments.")
    public List<RiskReportDTO> getAllRisks() {
        log.info("[RiskLlmTools] getAllRisks");
        try {
            return riskService.getAllRiskReports();
        } catch (Exception e) {
            log.error("[RiskLlmTools] getAllRisks failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve all risk reports: " + e.getMessage(), e);
        }
    }
}
