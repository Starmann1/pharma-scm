package pharma.agent.operational;

import java.util.List;

import pharma.agent.behaviour.PeriodicRiskScanBehaviour;
import pharma.agent.behaviour.RequestHandlerBehaviour;
import pharma.agent.core.BasePharmaAgent;
import pharma.agent.ontology.AgentActions;
import pharma.dto.AgentRequestEnvelope;
import pharma.dto.AgentResponseEnvelope;
import pharma.dto.RiskReportDTO;

/**
 * RiskAnalysisAgent — Phase 8 operational agent for supply chain risk monitoring.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Runs a {@link PeriodicRiskScanBehaviour} every 24 hours that scans for
 *       high-risk conditions and logs audit alerts for scores above 0.7.</li>
 *   <li>Handles on-demand {@code RISK_ANALYSIS} requests from the UI/gateway,
 *       returning the full list of risk reports for dashboard display.</li>
 * </ul>
 *
 * <p>Architecture rule: delegates exclusively to
 * {@code services.getRiskService()} and {@code services.getAuditService()}.
 * No direct JDBC calls.
 *
 * <p>Arguments: arg[0] = {@link pharma.config.ApplicationServices}
 */
public class RiskAnalysisAgent extends BasePharmaAgent {

    /** Daily risk scan interval: 24 hours. */
    @SuppressWarnings("unused")
    private static final long DAILY_SCAN_PERIOD_MS = 86_400_000L;

    @Override
    protected void setup() {
        super.setup();

        // 1. Daily periodic risk scanner — DISABLED (all auto-processes are off)
        // addBehaviour(new PeriodicRiskScanBehaviour(this, DAILY_SCAN_PERIOD_MS, services));
        // logger.info("[RiskAnalysisAgent] PeriodicRiskScanBehaviour added (period={}ms).", DAILY_SCAN_PERIOD_MS);

        // 2. On-demand risk analysis request handler
        addBehaviour(new RiskAnalysisRequestBehaviour());
        logger.info("[RiskAnalysisAgent] Ready to handle risk analysis requests.");
    }

    // =========================================================================
    // Inner behaviour — handles RISK_ANALYSIS requests
    // =========================================================================

    /**
     * Cyclic behaviour that listens for {@code RISK_ANALYSIS} REQUEST messages.
     * Returns the full list of {@link RiskReportDTO} objects from the risk service.
     */
    private class RiskAnalysisRequestBehaviour extends RequestHandlerBehaviour {

        @Override
        protected AgentResponseEnvelope<?> handle(AgentRequestEnvelope<?> request) throws Exception {
            if (!AgentActions.RISK_ANALYSIS.equals(request.getAction())) {
                return AgentResponseEnvelope.failure(
                        request.getTransactionId(), request.getAction(),
                        "RiskAnalysisAgent: unsupported action '" + request.getAction() + "'");
            }

            logger.info("[RiskAnalysisAgent] RISK_ANALYSIS — generating risk reports...");

            List<RiskReportDTO> reports = services.getRiskService().getAllRiskReports();

            logger.info("[RiskAnalysisAgent] Generated {} risk report(s).", reports.size());

            // Log the on-demand analysis request in audit trail
            try {
                long highRiskCount = reports.stream()
                        .filter(r -> r.getRiskScore() > 0.7)
                        .count();

                services.getAuditService().logAgentDecision(
                        request.getRequestedByUserId(),
                        "RISK_ANALYSIS_REQUESTED",
                        "RiskReport",
                        "ALL",
                        "On-demand risk analysis: total=" + reports.size()
                                + " highRisk=" + highRiskCount);
            } catch (Exception e) {
                logger.warn("[RiskAnalysisAgent] Audit logging failed (non-fatal): {}", e.getMessage());
            }

            return AgentResponseEnvelope.success(
                    request.getTransactionId(), request.getAction(), reports);
        }
    }
}
