package pharma.agent.operational;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import pharma.agent.behaviour.RequestHandlerBehaviour;
import pharma.agent.core.BasePharmaAgent;
import pharma.agent.ontology.AgentActions;
import pharma.dto.AgentRequestEnvelope;
import pharma.dto.AgentResponseEnvelope;
import pharma.dto.ManufacturingFeasibilityDTO;

/**
 * ComplianceAgent — Phase 6 operational agent.
 *
 * <p>Handles:
 * <ul>
 *   <li>{@link AgentActions#COMPLIANCE_VALIDATE} — validates a
 *       {@link ManufacturingFeasibilityDTO} against pharmaceutical compliance
 *       rules and returns a boolean pass/fail result.</li>
 * </ul>
 *
 * <p>Architecture rule: delegates exclusively to
 * {@code this.services.getComplianceService()}.  No JDBC calls.
 *
 * <p>In Phase 6 the validation is deterministic (rule-based). AI-assisted
 * compliance reasoning will be added in Phase 10 (AIReasoningAgent).
 *
 * <p>Arguments: arg[0] = {@link pharma.config.ApplicationServices}
 */
public class ComplianceAgent extends BasePharmaAgent {

    @Override
    protected void setup() {
        super.setup();
        addBehaviour(new ComplianceRequestBehaviour());
        logger.info("[ComplianceAgent] Ready to validate compliance rules.");
    }

    // =========================================================================
    // Inner behaviour
    // =========================================================================

    private class ComplianceRequestBehaviour extends RequestHandlerBehaviour {

        private static final ObjectMapper MAPPER_JT = new ObjectMapper()
                .registerModule(new JavaTimeModule());

        @Override
        protected AgentResponseEnvelope<?> handle(AgentRequestEnvelope<?> request) throws Exception {
            if (!AgentActions.COMPLIANCE_VALIDATE.equals(request.getAction())) {
                return AgentResponseEnvelope.failure(
                        request.getTransactionId(), request.getAction(),
                        "ComplianceAgent: unsupported action '" + request.getAction() + "'");
            }

            logger.info("[ComplianceAgent] COMPLIANCE_VALIDATE txId='{}'",
                    request.getTransactionId());

            @SuppressWarnings("null")
            ManufacturingFeasibilityDTO proposal =
                    MAPPER_JT.convertValue(request.getPayload(), ManufacturingFeasibilityDTO.class);

            // Delegate to ComplianceService — no business logic in the agent
            boolean compliant = services.getComplianceService()
                    .validateManufacturingProposal(proposal);

            logger.info("[ComplianceAgent] Compliance result: {}", compliant ? "PASS" : "FAIL");

            return AgentResponseEnvelope.success(
                    request.getTransactionId(), request.getAction(), compliant);
        }
    }
}
