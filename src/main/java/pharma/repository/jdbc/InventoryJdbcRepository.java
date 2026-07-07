package pharma.repository.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import pharma.dto.MaterialAvailabilityDTO;
import pharma.repository.InventoryRepository;
import pharma.service.DatabaseService;

public class InventoryJdbcRepository implements InventoryRepository {
    private final DatabaseService databaseService;
    private final JdbcSqlDialect sqlDialect;
    private final StockJdbcRepository stockRepository;

    public InventoryJdbcRepository(DatabaseService databaseService) {
        this(databaseService, JdbcSqlDialect.from(DatabaseService.getDatabaseConfig()));
    }

    InventoryJdbcRepository(DatabaseService databaseService, JdbcSqlDialect sqlDialect) {
        this.databaseService = databaseService;
        this.sqlDialect = sqlDialect;
        this.stockRepository = new StockJdbcRepository(databaseService, sqlDialect);
    }

    @Override
    public MaterialAvailabilityDTO checkAvailability(String materialCode, double requiredQuantity)
            throws SQLException, ClassNotFoundException {
        String sql = "SELECT si.batch_number, si.quantity, si.reserved_quantity, mm.reorder_level "
                + "FROM " + sqlDialect.table(JdbcSqlDialect.Table.STOCK_INVENTORY) + " si "
                + "JOIN " + sqlDialect.table(JdbcSqlDialect.Table.MATERIAL_MASTER)
                + " mm ON mm.material_code = si.material_code "
                + "WHERE si.material_code = ? "
                + "AND si.qc_status = 'APPROVED' "
                + "AND si.location_code != 'REJECTED_AREA' "
                + "AND (si.exp_date IS NULL OR si.exp_date >= ?) "
                + "AND (si.quantity - si.reserved_quantity) > 0";

        double available = 0;
        double reserved = 0;
        int reorderLevel = 0;
        List<String> batches = new ArrayList<>();

        try (Connection conn = databaseService.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, materialCode);
            stmt.setDate(2, java.sql.Date.valueOf(LocalDate.now()));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    double rowAvailable = rs.getDouble("quantity") - rs.getDouble("reserved_quantity");
                    available += rowAvailable;
                    reserved += rs.getDouble("reserved_quantity");
                    reorderLevel = rs.getInt("reorder_level");
                    batches.add(rs.getString("batch_number"));
                }
            }
        }

        MaterialAvailabilityDTO dto = new MaterialAvailabilityDTO(
                materialCode,
                requiredQuantity,
                available,
                reserved,
                available < reorderLevel);
        dto.setEligibleBatches(batches);
        return dto;
    }

    @Override
    public List<MaterialAvailabilityDTO> findLowStockMaterials() throws SQLException, ClassNotFoundException {
        String sql = "SELECT mm.material_code, mm.reorder_level, "
                + "COALESCE(SUM(CASE WHEN si.qc_status = 'APPROVED' "
                + "AND (si.exp_date IS NULL OR si.exp_date >= CURRENT_DATE) "
                + "THEN si.quantity - si.reserved_quantity ELSE 0 END), 0) AS available_qty, "
                + "COALESCE(SUM(si.reserved_quantity), 0) AS reserved_qty "
                + "FROM " + sqlDialect.table(JdbcSqlDialect.Table.MATERIAL_MASTER) + " mm "
                + "LEFT JOIN " + sqlDialect.table(JdbcSqlDialect.Table.STOCK_INVENTORY)
                + " si ON si.material_code = mm.material_code "
                + "WHERE mm.is_active = TRUE AND mm.material_type IN ('RAW_MATERIAL', 'PACKAGING') "
                + "AND mm.material_code NOT IN ("
                + "  SELECT poi.material_code FROM " + sqlDialect.table(JdbcSqlDialect.Table.PURCHASE_ORDER_ITEM)
                + " poi "
                + "  JOIN " + sqlDialect.table(JdbcSqlDialect.Table.PURCHASE_ORDER)
                + " po ON po.po_id = poi.po_id "
                + "  WHERE UPPER(po.status) NOT IN ('RECEIVED', 'CANCELLED', 'REJECTED')"
                + ") "
                + "GROUP BY mm.material_code, mm.reorder_level "
                + "HAVING COALESCE(SUM(CASE WHEN si.qc_status = 'APPROVED' "
                + "AND (si.exp_date IS NULL OR si.exp_date >= CURRENT_DATE) "
                + "THEN si.quantity - si.reserved_quantity ELSE 0 END), 0) < mm.reorder_level";
        List<MaterialAvailabilityDTO> results = new ArrayList<>();
        try (Connection conn = databaseService.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                results.add(new MaterialAvailabilityDTO(
                        rs.getString("material_code"),
                        rs.getInt("reorder_level"),
                        rs.getDouble("available_qty"),
                        rs.getDouble("reserved_qty"),
                        true));
            }
        }
        return results;
    }

    @Override
    public boolean reserveMaterial(String materialCode, double quantity)
            throws SQLException, ClassNotFoundException {
        return stockRepository.reserveStock(materialCode, quantity);
    }
}
