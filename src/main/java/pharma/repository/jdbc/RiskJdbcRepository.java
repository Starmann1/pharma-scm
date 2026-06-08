package pharma.repository.jdbc;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import pharma.dto.MaterialAvailabilityDTO;
import pharma.dto.RiskReportDTO;
import pharma.repository.InventoryRepository;
import pharma.repository.RiskRepository;

public class RiskJdbcRepository implements RiskRepository {
    private final InventoryRepository inventoryRepository;

    public RiskJdbcRepository(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public List<RiskReportDTO> generateRuleBasedRiskReports() throws SQLException, ClassNotFoundException {
        List<RiskReportDTO> reports = new ArrayList<>();
        for (MaterialAvailabilityDTO lowStock : inventoryRepository.findLowStockMaterials()) {
            RiskReportDTO report = new RiskReportDTO();
            report.setRiskType("STOCKOUT");
            report.setRiskScore(0.85);
            report.setSeverity("HIGH");
            report.setGeneratedAt(LocalDateTime.now());
            report.getDrivers().add("Material " + lowStock.getMaterialCode() + " is below reorder level.");
            reports.add(report);
        }
        return reports;
    }
}
