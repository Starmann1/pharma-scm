package pharma.service;

import java.sql.SQLException;
import java.util.List;

import pharma.dto.RiskReportDTO;
import pharma.repository.RiskRepository;

public class RiskService {
    private final RiskRepository riskRepository;

    public RiskService(RiskRepository riskRepository) {
        this.riskRepository = riskRepository;
    }

    public List<RiskReportDTO> generateRuleBasedRiskReports() throws SQLException, ClassNotFoundException {
        return riskRepository.generateRuleBasedRiskReports();
    }
}
