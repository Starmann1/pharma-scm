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

    public RiskReportDTO scoreSupplierRisk(int supplierId) throws SQLException, ClassNotFoundException {
        return riskRepository.scoreSupplierRisk(supplierId);
    }

    public RiskReportDTO scoreMaterialStockoutRisk(String materialCode) throws SQLException, ClassNotFoundException {
        if (materialCode == null || materialCode.isBlank()) {
            throw new IllegalArgumentException("materialCode is required.");
        }
        return riskRepository.scoreMaterialStockoutRisk(materialCode);
    }

    public List<RiskReportDTO> getAllRiskReports() throws SQLException, ClassNotFoundException {
        return riskRepository.getAllRiskReports();
    }
}
