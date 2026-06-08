package pharma.repository;

import java.sql.SQLException;
import java.util.List;

import pharma.dto.RiskReportDTO;

public interface RiskRepository {
    List<RiskReportDTO> generateRuleBasedRiskReports() throws SQLException, ClassNotFoundException;
}
