package pharma.agent.operational;

import com.fasterxml.jackson.databind.ObjectMapper;

import pharma.agent.behaviour.RequestHandlerBehaviour;
import pharma.agent.core.BasePharmaAgent;
import pharma.agent.ontology.AgentActions;
import pharma.dto.AgentRequestEnvelope;
import pharma.dto.AgentResponseEnvelope;
import pharma.dto.QAResultDTO;
import pharma.dto.QAReviewRequestDTO;

/**
 * QAAgent — Phase 6 operational agent.
 *
 * <p>Handles:
 * <ul>
 *   <li>{@link AgentActions#QA_REVIEW} — retrieves the full QA disposition
 *       (APPROVED / REJECTED / QUARANTINED) for a given batch number.</li>
 * </ul>
 *
 * <p>Architecture rule: delegates exclusively to
 * {@code this.services.getQaService()}.  No JDBC calls.
 *
 * <p>Arguments: arg[0] = {@link pharma.config.ApplicationServices}
 */
public class QAAgent extends BasePharmaAgent {

    @Override
    protected void setup() {
        super.setup();
        addBehaviour(new QARequestBehaviour());
        logger.info("[QAAgent] Ready to review batch dispositions.");
    }

    // =========================================================================
    // Inner behaviour
    // =========================================================================

    private class QARequestBehaviour extends RequestHandlerBehaviour {

        @Override
        protected AgentResponseEnvelope<?> handle(AgentRequestEnvelope<?> request) throws Exception {
            if (!AgentActions.QA_REVIEW.equals(request.getAction())) {
                return AgentResponseEnvelope.failure(
                        request.getTransactionId(), request.getAction(),
                        "QAAgent: unsupported action '" + request.getAction() + "'");
            }

            @SuppressWarnings("null")
            QAReviewRequestDTO req = new ObjectMapper()
                    .convertValue(request.getPayload(), QAReviewRequestDTO.class);

            logger.info("[QAAgent] QA_REVIEW batchNumber='{}'", req.getBatchNumber());

            QAResultDTO result = services.getQaService()
                    .reviewBatch(req.getBatchNumber());

            logger.info("[QAAgent] Batch '{}' QA decision: {}",
                    req.getBatchNumber(), result.getDecision());

            return AgentResponseEnvelope.success(
                    request.getTransactionId(), request.getAction(), result);
        }
    }
}
