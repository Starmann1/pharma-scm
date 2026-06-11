package pharma.agent.behaviour;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import pharma.config.ApplicationServices;
import pharma.dto.RiskReportDTO;

/**
 * Periodic behaviour that performs a daily risk scan across the supply chain.
 *
 * <p>Every tick (default 24 hours) the behaviour calls
 * {@link pharma.service.RiskService#getAllRiskReports()} and filters for
 * high-risk entries (score &gt; 0.7). Each high-risk entry is logged as a
 * {@code RISK_ALERT} in the audit trail for compliance and escalation.
 *
 * <p>This behaviour is added by the {@code RiskAnalysisAgent} at startup.
 */
public class PeriodicRiskScanBehaviour extends TickerBehaviour {

    private static final Logger log = LoggerFactory.getLogger(PeriodicRiskScanBehaviour.class);

    /** Risk score threshold above which an alert is raised. */
    private static final double HIGH_RISK_THRESHOLD = 0.7;

    private final ApplicationServices services;

    /**
     * Creates a periodic risk scan behaviour.
     *
     * @param agent    the owning JADE agent
     * @param periodMs tick interval in milliseconds (recommended: 86_400_000 = 24 hours)
     * @param services the application service composition root
     */
    public PeriodicRiskScanBehaviour(Agent agent, long periodMs, ApplicationServices services) {
        super(agent, periodMs);
        this.services = services;
    }

    /**
     * Executed on every tick — fetches all risk reports and logs audit entries
     * for any with a score exceeding the {@link #HIGH_RISK_THRESHOLD}.
     */
    @Override
    protected void onTick() {
        try {
            List<RiskReportDTO> allReports = services.getRiskService().getAllRiskReports();

            if (allReports.isEmpty()) {
                log.info("[PeriodicRiskScan] No risk reports available.");
                return;
            }

            List<RiskReportDTO> highRiskReports = allReports.stream()
                    .filter(r -> r.getRiskScore() > HIGH_RISK_THRESHOLD)
                    .toList();

            log.info("[PeriodicRiskScan] Scanned {} risk report(s) — {} high-risk alert(s) detected.",
                    allReports.size(), highRiskReports.size());

            for (RiskReportDTO risk : highRiskReports) {
                log.warn("[PeriodicRiskScan] HIGH RISK: entity='{}' type='{}' category='{}' score={} severity='{}'",
                        risk.getEntityId(), risk.getEntityType(), risk.getRiskCategory(),
                        String.format("%.3f", risk.getRiskScore()), risk.getSeverity());

                // Log each high-risk entry to the audit trail
                try {
                    services.getAuditService().logAgentDecision(
                            0, // system user
                            "RISK_ALERT",
                            risk.getEntityType() != null ? risk.getEntityType() : "Unknown",
                            risk.getEntityId() != null ? risk.getEntityId() : "N/A",
                            "Risk alert: category=" + risk.getRiskCategory()
                                    + " score=" + String.format("%.3f", risk.getRiskScore())
                                    + " severity=" + risk.getSeverity()
                                    + " action=" + risk.getRecommendedAction());
                } catch (Exception e) {
                    log.error("[PeriodicRiskScan] Failed to log audit for entity='{}': {}",
                            risk.getEntityId(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("[PeriodicRiskScan] Error during periodic risk scan: {}", e.getMessage(), e);
        }
    }
}
