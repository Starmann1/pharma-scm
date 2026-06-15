package pharma.llm.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import pharma.dto.ManufacturingFeasibilityDTO;
import pharma.service.ComplianceService;

/**
 * LangChain4j tool wrapper that exposes {@link ComplianceService} methods
 * as LLM-callable tools.
 *
 * <p>The LLM provides a JSON string representing the manufacturing proposal,
 * which is deserialized into a {@link ManufacturingFeasibilityDTO} before
 * delegating to the service layer.
 */
public class ComplianceLlmTools {

    private static final Logger log = LoggerFactory.getLogger(ComplianceLlmTools.class);

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final ComplianceService complianceService;

    /**
     * Constructs a new tool wrapper backed by the given service.
     *
     * @param complianceService the compliance service instance
     */
    public ComplianceLlmTools(ComplianceService complianceService) {
        this.complianceService = complianceService;
    }

    /**
     * Validates a manufacturing proposal against GMP and regulatory compliance rules.
     *
     * @param proposalJson JSON string representing the manufacturing feasibility proposal
     * @return {@code true} if the proposal passes all compliance checks
     */
    @Tool("Validate a manufacturing proposal against GMP and regulatory compliance rules. " +
          "Accepts a JSON string with fields: materialCode, bomId, plannedQuantity, requestedDate. " +
          "Returns true if the proposal is compliant, false otherwise.")
    public boolean validateProposal(
            @P("JSON string representing the manufacturing proposal, " +
               "e.g. {\"materialCode\":\"FP-001\",\"bomId\":1,\"plannedQuantity\":100.0}") String proposalJson) {
        log.info("[ComplianceLlmTools] validateProposal json='{}'", proposalJson);
        try {
            @SuppressWarnings("null")
            ManufacturingFeasibilityDTO proposal = MAPPER.readValue(proposalJson, ManufacturingFeasibilityDTO.class);
            return complianceService.validateManufacturingProposal(proposal);
        } catch (Exception e) {
            log.error("[ComplianceLlmTools] validateProposal failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to validate manufacturing proposal: " + e.getMessage(), e);
        }
    }
}
