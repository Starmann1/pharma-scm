package pharma.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import pharma.dto.RiskReportDTO;

public interface RiskRepository {
    List<RiskReportDTO> generateRuleBasedRiskReports() throws SQLException, ClassNotFoundException;
    RiskReportDTO scoreSupplierRisk(int supplierId) throws SQLException, ClassNotFoundException;
    RiskReportDTO scoreMaterialStockoutRisk(String materialCode) throws SQLException, ClassNotFoundException;
    List<RiskReportDTO> getAllRiskReports() throws SQLException, ClassNotFoundException;
    List<Map<String, Object>> getSupplierDeliveryHistory(int supplierId, int days) throws SQLException, ClassNotFoundException;
    List<Map<String, Object>> getStockConsumptionTrend(String materialCode, int days) throws SQLException, ClassNotFoundException;
}
