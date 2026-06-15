package pharma.repository.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

import pharma.dto.RiskReportDTO;
import pharma.repository.RiskRepository;
import pharma.service.DatabaseService;

public class RiskJdbcRepository implements RiskRepository {
    private final DatabaseService databaseService;

    public RiskJdbcRepository(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @Override
    public List<RiskReportDTO> generateRuleBasedRiskReports() throws SQLException, ClassNotFoundException {
        List<RiskReportDTO> reports = new ArrayList<>();

        // Rule 1: Low stock materials
        String lowStockSql = "SELECT mm.material_code, mm.reorder_level, "
                + "COALESCE(SUM(CASE WHEN si.qc_status = 'APPROVED' "
                + "AND (si.exp_date IS NULL OR si.exp_date >= CURRENT_DATE) "
                + "THEN si.quantity - si.reserved_quantity ELSE 0 END), 0) AS available_qty "
                + "FROM Material_Master mm "
                + "LEFT JOIN Stock_Inventory si ON si.material_code = mm.material_code "
                + "WHERE mm.is_active = TRUE AND mm.material_type IN ('RAW_MATERIAL', 'PACKAGING') "
                + "GROUP BY mm.material_code, mm.reorder_level "
                + "HAVING available_qty < mm.reorder_level";
        try (Connection conn = databaseService.getConnection();
                PreparedStatement stmt = conn.prepareStatement(lowStockSql);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String materialCode = rs.getString("material_code");
                double available = rs.getDouble("available_qty");
                int reorderLevel = rs.getInt("reorder_level");
                double score = 0.6 * ((double) reorderLevel / Math.max(available, 1)) + 0.4 * 0.8;
                score = Math.min(score, 1.0);

                RiskReportDTO report = new RiskReportDTO();
                report.setRiskType("STOCKOUT");
                report.setRiskScore(score);
                report.setSeverity(classifySeverity(score));
                report.setEntityType("MATERIAL");
                report.setEntityId(materialCode);
                report.setRiskCategory("SUPPLY_RISK");
                report.setRecommendedAction("Expedite open orders or seek alternative supplier");
                report.setGeneratedAt(LocalDateTime.now());
                report.getDrivers().add("Material " + materialCode + " is below reorder level. "
                        + "Available: " + available + ", Reorder Level: " + reorderLevel);
                reports.add(report);
            }
        }

        // Rule 2: Suppliers with high late delivery rate (last 90 days)
        String lateDeliverySql = "SELECT po.supplier_id, sm.supplier_name, "
                + "COUNT(*) AS total_deliveries, "
                + "SUM(CASE WHEN po.actual_delivery_date > po.expected_date THEN 1 ELSE 0 END) AS late_deliveries "
                + "FROM Purchase_Order po "
                + "JOIN Supplier_Master sm ON sm.supplier_id = po.supplier_id "
                + "WHERE po.status IN ('DELIVERED', 'COMPLETED', 'CLOSED') "
                + "AND po.order_date >= DATE_SUB(CURRENT_DATE, INTERVAL 90 DAY) "
                + "GROUP BY po.supplier_id, sm.supplier_name "
                + "HAVING late_deliveries > 0";
        try (Connection conn = databaseService.getConnection();
                PreparedStatement stmt = conn.prepareStatement(lateDeliverySql);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                int supplierId = rs.getInt("supplier_id");
                String supplierName = rs.getString("supplier_name");
                int total = rs.getInt("total_deliveries");
                int late = rs.getInt("late_deliveries");
                double lateRate = (double) late / Math.max(total, 1);
                double score = 0.5 * lateRate + 0.3 * 0.0 + 0.2 * 0.5;
                score = Math.min(score, 1.0);

                RiskReportDTO report = new RiskReportDTO();
                report.setRiskType("SUPPLIER_RELIABILITY");
                report.setRiskScore(score);
                report.setSeverity(classifySeverity(score));
                report.setEntityType("SUPPLIER");
                report.setEntityId(String.valueOf(supplierId));
                report.setRiskCategory("PERFORMANCE_RISK");
                report.setRecommendedAction("Review supplier performance or source alternatives");
                report.setGeneratedAt(LocalDateTime.now());
                report.getDrivers().add("Supplier " + supplierName + " (ID: " + supplierId + ") has "
                        + late + "/" + total + " late deliveries in the last 90 days.");
                reports.add(report);
            }
        }

        return reports;
    }

    @Override
    public RiskReportDTO scoreSupplierRisk(int supplierId) throws SQLException, ClassNotFoundException {
        String sql = "SELECT "
                + "COUNT(*) AS total_deliveries, "
                + "SUM(CASE WHEN po.actual_delivery_date > po.expected_date THEN 1 ELSE 0 END) AS late_deliveries, "
                + "SUM(CASE WHEN po.status = 'REJECTED' THEN 1 ELSE 0 END) AS rejections "
                + "FROM Purchase_Order po "
                + "WHERE po.supplier_id = ? "
                + "AND po.order_date >= DATE_SUB(CURRENT_DATE, INTERVAL 365 DAY)";

        double lateDeliveryRate = 0.0;
        double rejectionRate = 0.0;
        int totalDeliveries = 0;

        try (Connection conn = databaseService.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, supplierId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    totalDeliveries = rs.getInt("total_deliveries");
                    int lateDeliveries = rs.getInt("late_deliveries");
                    int rejections = rs.getInt("rejections");
                    lateDeliveryRate = totalDeliveries > 0 ? (double) lateDeliveries / totalDeliveries : 0.0;
                    rejectionRate = totalDeliveries > 0 ? (double) rejections / totalDeliveries : 0.0;
                }
            }
        }

        // Capacity score: ratio of fulfilled orders to total
        double capacityScore = totalDeliveries > 0 ? 0.8 : 0.5;

        // supplierRisk = 0.5 * lateDeliveryRate + 0.3 * rejectionRate + 0.2 * (1 - capacityScore)
        double riskScore = 0.5 * lateDeliveryRate + 0.3 * rejectionRate + 0.2 * (1 - capacityScore);
        riskScore = Math.min(riskScore, 1.0);

        RiskReportDTO report = new RiskReportDTO();
        report.setRiskType("SUPPLIER_RISK");
        report.setRiskScore(riskScore);
        report.setSeverity(classifySeverity(riskScore));
        report.setEntityType("SUPPLIER");
        report.setEntityId(String.valueOf(supplierId));
        report.setRiskCategory("PERFORMANCE_RISK");
        report.setRecommendedAction(riskScore > 0.7 ? "Find alternative supplier immediately" : "Monitor supplier performance");
        report.setGeneratedAt(LocalDateTime.now());
        report.getDrivers().add("Late delivery rate: " + String.format("%.2f", lateDeliveryRate * 100) + "%");
        report.getDrivers().add("Rejection rate: " + String.format("%.2f", rejectionRate * 100) + "%");
        report.getDrivers().add("Capacity score: " + String.format("%.2f", capacityScore));
        return report;
    }

    @Override
    public RiskReportDTO scoreMaterialStockoutRisk(String materialCode) throws SQLException, ClassNotFoundException {
        String sql = "SELECT mm.reorder_level, "
                + "COALESCE(SUM(CASE WHEN si.qc_status = 'APPROVED' "
                + "AND (si.exp_date IS NULL OR si.exp_date >= CURRENT_DATE) "
                + "THEN si.quantity - si.reserved_quantity ELSE 0 END), 0) AS current_stock "
                + "FROM Material_Master mm "
                + "LEFT JOIN Stock_Inventory si ON si.material_code = mm.material_code "
                + "WHERE mm.material_code = ? "
                + "GROUP BY mm.reorder_level";

        double currentStock = 0;
        int reorderLevel = 0;

        try (Connection conn = databaseService.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, materialCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    reorderLevel = rs.getInt("reorder_level");
                    currentStock = rs.getDouble("current_stock");
                }
            }
        }

        // Estimate days until stockout based on recent consumption
        double avgDailyConsumption = estimateAvgDailyConsumption(materialCode);
        double daysUntilStockout = avgDailyConsumption > 0 ? currentStock / avgDailyConsumption : 999;

        // materialStockoutRisk = 0.6 * (reorderLevel / max(currentStock, 1)) + 0.4 * (daysUntilStockout < 14 ? 0.8 : 0.3)
        double riskScore = 0.6 * ((double) reorderLevel / Math.max(currentStock, 1))
                + 0.4 * (daysUntilStockout < 14 ? 0.8 : 0.3);
        riskScore = Math.min(riskScore, 1.0);

        RiskReportDTO report = new RiskReportDTO();
        report.setRiskType("MATERIAL_STOCKOUT");
        report.setRiskScore(riskScore);
        report.setSeverity(classifySeverity(riskScore));
        report.setEntityType("MATERIAL");
        report.setEntityId(materialCode);
        report.setRiskCategory("SUPPLY_RISK");
        report.setRecommendedAction(riskScore > 0.7 ? "Reorder material immediately" : "Maintain current stock levels");
        report.setGeneratedAt(LocalDateTime.now());
        report.getDrivers().add("Current stock: " + currentStock + ", Reorder level: " + reorderLevel);
        report.getDrivers().add("Estimated days until stockout: " + String.format("%.1f", daysUntilStockout));
        return report;
    }

    @Override
    public List<RiskReportDTO> getAllRiskReports() throws SQLException, ClassNotFoundException {
        List<RiskReportDTO> allReports = new ArrayList<>();

        // Collect supplier risks for all active suppliers
        String supplierSql = "SELECT DISTINCT supplier_id FROM Supplier_Master "
                + "WHERE UPPER(COALESCE(supplier_status, 'APPROVED')) = 'APPROVED'";
        try (Connection conn = databaseService.getConnection();
                PreparedStatement stmt = conn.prepareStatement(supplierSql);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                int supplierId = rs.getInt("supplier_id");
                allReports.add(scoreSupplierRisk(supplierId));
            }
        }

        // Collect material stockout risks for all active materials
        String materialSql = "SELECT material_code FROM Material_Master WHERE is_active = TRUE AND material_type IN ('RAW_MATERIAL', 'PACKAGING')";
        try (Connection conn = databaseService.getConnection();
                PreparedStatement stmt = conn.prepareStatement(materialSql);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String materialCode = rs.getString("material_code");
                allReports.add(scoreMaterialStockoutRisk(materialCode));
            }
        }

        return allReports;
    }

    @Override
    public List<Map<String, Object>> getSupplierDeliveryHistory(int supplierId, int days)
            throws SQLException, ClassNotFoundException {
        String sql = "SELECT po.po_id, po.order_date, po.expected_date AS expected_delivery_date, "
                + "po.actual_delivery_date, po.status AS po_status, "
                + "CASE WHEN po.actual_delivery_date > po.expected_date THEN 'LATE' "
                + "     WHEN po.actual_delivery_date IS NULL THEN 'PENDING' "
                + "     ELSE 'ON_TIME' END AS delivery_status "
                + "FROM Purchase_Order po "
                + "WHERE po.supplier_id = ? "
                + "AND po.order_date >= DATE_SUB(CURRENT_DATE, INTERVAL ? DAY) "
                + "ORDER BY po.order_date DESC";
        List<Map<String, Object>> history = new ArrayList<>();
        try (Connection conn = databaseService.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, supplierId);
            stmt.setInt(2, days);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("poId", rs.getInt("po_id"));
                    row.put("orderDate", rs.getDate("order_date") != null
                            ? rs.getDate("order_date").toLocalDate().toString() : null);
                    row.put("expectedDeliveryDate", rs.getDate("expected_delivery_date") != null
                            ? rs.getDate("expected_delivery_date").toLocalDate().toString() : null);
                    row.put("actualDeliveryDate", rs.getDate("actual_delivery_date") != null
                            ? rs.getDate("actual_delivery_date").toLocalDate().toString() : null);
                    row.put("poStatus", rs.getString("po_status"));
                    row.put("deliveryStatus", rs.getString("delivery_status"));
                    history.add(row);
                }
            }
        }
        return history;
    }

    @Override
    public List<Map<String, Object>> getStockConsumptionTrend(String materialCode, int days)
            throws SQLException, ClassNotFoundException {
        String sql = "SELECT si.batch_number, si.quantity, si.reserved_quantity, "
                + "si.qc_status, si.location_code, si.exp_date, si.created_at AS received_date "
                + "FROM Stock_Inventory si "
                + "WHERE si.material_code = ? "
                + "AND si.created_at >= DATE_SUB(CURRENT_DATE, INTERVAL ? DAY) "
                + "ORDER BY si.created_at DESC";
        List<Map<String, Object>> trend = new ArrayList<>();
        try (Connection conn = databaseService.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, materialCode);
            stmt.setInt(2, days);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("batchNumber", rs.getString("batch_number"));
                    row.put("quantity", rs.getDouble("quantity"));
                    row.put("reservedQuantity", rs.getDouble("reserved_quantity"));
                    row.put("qcStatus", rs.getString("qc_status"));
                    row.put("locationCode", rs.getString("location_code"));
                    row.put("expDate", rs.getDate("exp_date") != null
                            ? rs.getDate("exp_date").toLocalDate().toString() : null);
                    row.put("receivedDate", rs.getDate("received_date") != null
                            ? rs.getDate("received_date").toLocalDate().toString() : null);
                    trend.add(row);
                }
            }
        }
        return trend;
    }

    private double estimateAvgDailyConsumption(String materialCode) throws SQLException, ClassNotFoundException {
        // Estimate consumption from reserved_quantity changes over the last 30 days
        String sql = "SELECT COALESCE(SUM(si.reserved_quantity), 0) / 30.0 AS avg_daily "
                + "FROM Stock_Inventory si "
                + "WHERE si.material_code = ? "
                + "AND si.created_at >= DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY)";
        try (Connection conn = databaseService.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, materialCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("avg_daily");
                }
            }
        }
        return 0.0;
    }

    private String classifySeverity(double score) {
        if (score > 0.7) {
            return "HIGH";
        } else if (score >= 0.4) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
}
