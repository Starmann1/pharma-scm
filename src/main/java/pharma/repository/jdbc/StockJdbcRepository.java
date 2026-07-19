package pharma.repository.jdbc;

import pharma.model.Stock;
import pharma.service.DatabaseService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dialect-aware JDBC repository for {@code stock_inventory} persistence.
 * Extracted from {@link DatabaseService} as part of the v1.1 PostgreSQL migration (Phase 6).
 */
public class StockJdbcRepository {

    private static final Logger logger = LoggerFactory.getLogger(StockJdbcRepository.class);

    private static final String STOCK_WITH_MATERIAL_SELECT =
            "SELECT s.stock_id, s.material_code, d.brand_name, d.generic_name, d.manufacturer, "
                    + "s.location_code, s.batch_number, s.quantity, s.reserved_quantity, s.available_quantity, "
                    + "s.unit_cost, s.mfg_date, s.exp_date, s.qc_status, s.parent_batch_id";

    private final DatabaseService databaseService;
    private final JdbcSqlDialect d;

    public StockJdbcRepository(DatabaseService databaseService) {
        this(databaseService, JdbcSqlDialect.from(DatabaseService.getDatabaseConfig()));
    }

    StockJdbcRepository(DatabaseService databaseService, JdbcSqlDialect sqlDialect) {
        this.databaseService = databaseService;
        this.d = sqlDialect;
    }

    // -----------------------------------------------------------------------
    //  READ
    // -----------------------------------------------------------------------

    /**
     * Returns the net available stock (physical quantity minus reserved quantity) for
     * a material. This is the true uncommitted stock available for new production orders.
     * <p>
     * FIX: Previously used raw SUM(quantity) which over-counted already-reserved stock,
     * causing phantom availability and allowing over-commitment of materials.
     */
    public double getAvailableStock(String materialCode, String locationCode)
            throws SQLException, ClassNotFoundException {
        StringBuilder sql = new StringBuilder(
                "SELECT COALESCE(SUM(quantity - reserved_quantity), 0) AS available FROM "
                        + d.table(JdbcSqlDialect.Table.STOCK_INVENTORY)
                        + " WHERE material_code = ? AND qc_status = 'APPROVED'"
                        + " AND location_code != 'REJECTED_AREA'"
                        + " AND (exp_date IS NULL OR exp_date >= CURRENT_DATE)");
        if (locationCode != null && !locationCode.isBlank()) {
            sql.append(" AND location_code = ?");
        }

        try (Connection conn = databaseService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            pstmt.setString(1, materialCode);
            if (locationCode != null && !locationCode.isBlank()) {
                pstmt.setString(2, locationCode);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? Math.max(0.0, rs.getDouble("available")) : 0.0;
            }
        }
    }

    public Stock getStockByBatch(String batchNumber) {
        String sql = STOCK_WITH_MATERIAL_SELECT + ", s.production_order_id "
                + "FROM " + d.table(JdbcSqlDialect.Table.STOCK_INVENTORY) + " s "
                + "JOIN " + d.table(JdbcSqlDialect.Table.MATERIAL_MASTER) + " d "
                + "ON s.material_code = d.material_code "
                + "WHERE s.batch_number = ?";

        try (Connection conn = databaseService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, batchNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Stock stock = mapResultSetToStock(rs);
                    stock.setProductionOrderId(rs.getInt("production_order_id"));
                    return stock;
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error fetching stock for batch {}: {}", batchNumber, e.getMessage(), e);
        }
        return null;
    }

    public List<Stock> getStockByLocation(String locationCode) {
        List<Stock> stocks = new ArrayList<>();
        String sql = STOCK_WITH_MATERIAL_SELECT + " "
                + "FROM " + d.table(JdbcSqlDialect.Table.STOCK_INVENTORY) + " s "
                + "JOIN " + d.table(JdbcSqlDialect.Table.MATERIAL_MASTER) + " d "
                + "ON s.material_code = d.material_code "
                + "WHERE s.location_code = ? "
                + "ORDER BY s.mfg_date DESC, s.batch_number DESC";

        try (Connection conn = databaseService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, locationCode);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    stocks.add(mapResultSetToStock(rs));
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error fetching stock for location {}: {}", locationCode, e.getMessage(), e);
        }
        return stocks;
    }

    /**
     * Returns the full inventory list with material master join.
     */
    public List<Stock> getAllStock() {
        List<Stock> stocks = new ArrayList<>();
        String sql = STOCK_WITH_MATERIAL_SELECT + " "
                + "FROM " + d.table(JdbcSqlDialect.Table.STOCK_INVENTORY) + " s "
                + "JOIN " + d.table(JdbcSqlDialect.Table.MATERIAL_MASTER) + " d "
                + "ON s.material_code = d.material_code";

        try (Connection conn = databaseService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                stocks.add(mapResultSetToStock(rs));
            }
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error fetching all stock: {}", e.getMessage(), e);
        }
        return stocks;
    }

    /**
     * Returns stock filtered by QC status (pass {@code "All"} for no filter).
     */
    public List<Stock> getAllStockByQcStatus(String statusFilter) {
        List<Stock> stocks = new ArrayList<>();
        StringBuilder sql = new StringBuilder(STOCK_WITH_MATERIAL_SELECT + " "
                + "FROM " + d.table(JdbcSqlDialect.Table.STOCK_INVENTORY) + " s "
                + "JOIN " + d.table(JdbcSqlDialect.Table.MATERIAL_MASTER) + " d "
                + "ON s.material_code = d.material_code");

        if (!"All".equalsIgnoreCase(statusFilter)) {
            sql.append(" WHERE s.qc_status = ?");
        }
        sql.append(" ORDER BY s.mfg_date DESC, s.batch_number DESC");

        try (Connection conn = databaseService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            if (!"All".equalsIgnoreCase(statusFilter)) {
                pstmt.setString(1, statusFilter);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    stocks.add(mapResultSetToStock(rs));
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error fetching stock by QC status {}: {}", statusFilter, e.getMessage(), e);
        }
        return stocks;
    }

    /**
     * Returns parent batch numbers for a child batch by querying the relational
     * {@code batch_genealogy} table joined with {@code material_master}, falling
     * back to the CSV column in {@code stock_inventory}.
     */
    public List<String> getParentBatch(String batchNumber) throws SQLException, ClassNotFoundException {
        List<String> parentBatches = new ArrayList<>();

        // 1. Primary: query batch_genealogy table
        String bgSql = "SELECT bg.parent_batch, mm.brand_name "
                + "FROM " + d.table(JdbcSqlDialect.Table.BATCH_GENEALOGY) + " bg "
                + "LEFT JOIN " + d.table(JdbcSqlDialect.Table.STOCK_INVENTORY) + " si ON bg.parent_batch = si.batch_number "
                + "LEFT JOIN " + d.table(JdbcSqlDialect.Table.MATERIAL_MASTER) + " mm ON si.material_code = mm.material_code "
                + "WHERE bg.child_batch = ?";

        try (Connection conn = databaseService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(bgSql)) {
            pstmt.setString(1, batchNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String pBatch = rs.getString("parent_batch");
                    String brandName = rs.getString("brand_name");
                    if (brandName != null && !brandName.isBlank()) {
                        parentBatches.add(pBatch + " (" + brandName + ")");
                    } else {
                        parentBatches.add(pBatch);
                    }
                }
            }
        }

        // 2. Fallback: check CSV column in stock_inventory if no genealogy table entries found
        if (parentBatches.isEmpty()) {
            String sql = "SELECT parent_batch_id FROM " + d.table(JdbcSqlDialect.Table.STOCK_INVENTORY)
                    + " WHERE batch_number = ?";

            try (Connection conn = databaseService.getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, batchNumber);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        String parents = rs.getString("parent_batch_id");
                        if (parents != null && !parents.isEmpty()) {
                            for (String batch : parents.split(",")) {
                                parentBatches.add(batch.trim());
                            }
                        }
                    }
                }
            }
        }
        return parentBatches;
    }

    /**
     * Returns child batches whose {@code parent_batch_id} contains the given batch number.
     */
    public List<Map<String, Object>> getChildBatches(String batchNumber)
            throws SQLException, ClassNotFoundException {
        List<Map<String, Object>> affectedBatches = new ArrayList<>();
        String sql = "SELECT si.batch_number, si.material_code, dm.brand_name, si.quantity, "
                + "si.qc_status, si.exp_date, si.location_code "
                + "FROM " + d.table(JdbcSqlDialect.Table.STOCK_INVENTORY) + " si "
                + "JOIN " + d.table(JdbcSqlDialect.Table.MATERIAL_MASTER) + " dm "
                + "ON si.material_code = dm.material_code "
                + "WHERE si.parent_batch_id LIKE ?";

        try (Connection conn = databaseService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + batchNumber + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> batch = new HashMap<>();
                    batch.put("batch_number", rs.getString("batch_number"));
                    batch.put("material_code", rs.getString("material_code"));
                    batch.put("brand_name", rs.getString("brand_name"));
                    batch.put("quantity", rs.getDouble("quantity"));
                    batch.put("qc_status", rs.getString("qc_status"));
                    batch.put("exp_date", rs.getDate("exp_date"));
                    batch.put("location_code", rs.getString("location_code"));
                    affectedBatches.add(batch);
                }
            }
        }
        return affectedBatches;
    }

    // -----------------------------------------------------------------------
    //  WRITE
    // -----------------------------------------------------------------------

    /**
     * Upserts stock on receipt (GRN). Uses dialect-aware ON DUPLICATE KEY / ON CONFLICT.
     */
    public void insertOrUpdateStock(Connection conn, String materialCode, String locationCode,
            String batchNumber, double quantity, double unitCost, LocalDate mfgDate, LocalDate expDate,
            String qcStatus) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(d.upsertStockSql())) {
            bindUpsertStock(stmt, materialCode, locationCode, batchNumber, quantity, unitCost, mfgDate, expDate,
                    qcStatus);
            stmt.executeUpdate();
        }
    }

    /**
     * Binds parameters for batch upsert (used by GRN batch processing).
     */
    public void bindUpsertStock(PreparedStatement stmt, String materialCode, String locationCode,
            String batchNumber, double quantity, double unitCost, LocalDate mfgDate, LocalDate expDate,
            String qcStatus) throws SQLException {
        stmt.setString(1, materialCode);
        stmt.setString(2, locationCode);
        stmt.setString(3, batchNumber);
        stmt.setDouble(4, quantity);
        stmt.setDouble(5, unitCost);
        stmt.setDate(6, java.sql.Date.valueOf(mfgDate));
        stmt.setDate(7, java.sql.Date.valueOf(expDate));
        stmt.setString(8, qcStatus);
    }

    public String upsertStockSql() {
        return d.upsertStockSql();
    }

    /**
     * Inserts a new finished-goods stock record from production (not an upsert).
     */
    public void insertProductionStock(Connection conn, String materialCode, String locationCode,
            String batchNumber, double quantity, String parentBatchIds, int productionOrderId, LocalDate expiryDate)
            throws SQLException {
        String sql = "INSERT INTO " + d.table(JdbcSqlDialect.Table.STOCK_INVENTORY)
                + " (material_code, location_code, batch_number, quantity, unit_cost, mfg_date, exp_date,"
                + " qc_status, parent_batch_id, production_order_id)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, 'IN_PRODUCTION', ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, materialCode);
            pstmt.setString(2, locationCode);
            pstmt.setString(3, batchNumber);
            pstmt.setDouble(4, quantity);
            pstmt.setDouble(5, 0.0);
            pstmt.setDate(6, java.sql.Date.valueOf(LocalDate.now()));
            pstmt.setDate(7, java.sql.Date.valueOf(expiryDate));
            pstmt.setString(8, parentBatchIds);
            pstmt.setInt(9, productionOrderId);
            pstmt.executeUpdate();
        }
    }

    public boolean reserveStock(String materialCode, double quantity)
            throws SQLException, ClassNotFoundException {
        String sql = "UPDATE " + d.table(JdbcSqlDialect.Table.STOCK_INVENTORY)
                + " SET reserved_quantity = reserved_quantity + ? "
                + "WHERE material_code = ? "
                + "AND qc_status = 'APPROVED' "
                + "AND location_code != 'REJECTED_AREA' "
                + "AND (exp_date IS NULL OR exp_date >= CURRENT_DATE) "
                + "AND (quantity - reserved_quantity) >= ?";
        try (Connection conn = databaseService.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, quantity);
            stmt.setString(2, materialCode);
            stmt.setDouble(3, quantity);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean unreserveStock(String materialCode, double quantity)
            throws SQLException, ClassNotFoundException {
        String sql = "UPDATE " + d.table(JdbcSqlDialect.Table.STOCK_INVENTORY)
                + " SET reserved_quantity = reserved_quantity - ? "
                + "WHERE material_code = ? "
                + "AND reserved_quantity >= ?";
        try (Connection conn = databaseService.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, quantity);
            stmt.setString(2, materialCode);
            stmt.setDouble(3, quantity);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Consumes approved stock using FEFO (First-Expired, First-Out) within an existing
     * database transaction.
     *
     * <p>FIX 1 — Row-level lock: Uses {@code SELECT ... FOR UPDATE} to prevent concurrent
     * production runs from reading the same available quantities and double-consuming.
     *
     * <p>FIX 2 — Reservation release: After consuming physical stock, proportionally
     * reduces {@code reserved_quantity} so the DB CHECK constraint
     * ({@code reserved_quantity <= quantity}) is not violated.
     *
     * @param conn          active JDBC connection (caller manages transaction)
     * @param materialCode  the material to consume
     * @param quantityNeeded total quantity to consume
     * @return consumed batch lines in FEFO order
     * @throws SQLException if stock is insufficient or a DB error occurs
     */
    public List<ConsumedStockLine> consumeStock(Connection conn, String materialCode, double quantityNeeded)
            throws SQLException {
        List<ConsumedStockLine> consumed = new ArrayList<>();

        // FOR UPDATE acquires row-level exclusive locks — prevents concurrent reads from
        // seeing the same available quantity and racing to consume it.
        String selectSql = "SELECT stock_id, batch_number, quantity, reserved_quantity, exp_date FROM "
                + d.table(JdbcSqlDialect.Table.STOCK_INVENTORY)
                + " WHERE material_code = ? AND qc_status = 'APPROVED'"
                + " AND location_code != 'REJECTED_AREA'"
                + " AND (exp_date IS NULL OR exp_date >= CURRENT_DATE)"
                + " AND (quantity - reserved_quantity) > 0"
                + d.fefoOrderByClause()
                + " FOR UPDATE";

        // Decrement physical quantity AND proportionally release the reservation
        String updateSql = "UPDATE " + d.table(JdbcSqlDialect.Table.STOCK_INVENTORY)
                + " SET quantity = quantity - ?,"
                + "     reserved_quantity = GREATEST(0, reserved_quantity - ?)"
                + " WHERE stock_id = ?";

        try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
            selectStmt.setString(1, materialCode);
            try (ResultSet rs = selectStmt.executeQuery()) {
                double remaining = quantityNeeded;
                while (rs.next() && remaining > 0) {
                    int stockId = rs.getInt("stock_id");
                    String batchNumber = rs.getString("batch_number");
                    double physicalQty = rs.getDouble("quantity");
                    double reservedQty = rs.getDouble("reserved_quantity");
                    // Only consume the net-available (uncommitted) portion of this row
                    double netAvailable = physicalQty - reservedQty;
                    double toConsume = Math.min(remaining, netAvailable);

                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setDouble(1, toConsume);   // reduce physical qty
                        updateStmt.setDouble(2, toConsume);   // release the matching reservation
                        updateStmt.setInt(3, stockId);
                        updateStmt.executeUpdate();
                    }

                    consumed.add(new ConsumedStockLine(stockId, batchNumber, toConsume));
                    remaining -= toConsume;
                }

                if (remaining > 0.001) { // tolerance for floating-point rounding
                    throw new SQLException(
                            "Insufficient unreserved stock for material '" + materialCode
                            + "'. Shortage: " + String.format("%.3f", remaining));
                }
            }
        }
        return consumed;
    }

    /**
     * Applies dialect-aware qc_status / location_code consistency rules for stock rows.
     */
    public void ensureStatusLocationConsistency(Connection conn) throws SQLException {
        String stock = d.table(JdbcSqlDialect.Table.STOCK_INVENTORY);
        String material = d.table(JdbcSqlDialect.Table.MATERIAL_MASTER);

        String qcCase = "CASE "
                + "WHEN si.qc_status = 'APPROVED' AND mm.material_type = 'FINISHED_GOOD' THEN 'RELEASED' "
                + "ELSE si.qc_status END";
        String locationCase = "CASE "
                + "WHEN si.qc_status = 'REJECTED' THEN 'REJECTED_AREA' "
                + "WHEN si.qc_status = 'IN_PRODUCTION' THEN 'PRODUCTION_FLOOR' "
                + "WHEN si.qc_status IN ('QUARANTINE', 'QI', 'IN_PROCESS_SAMPLE', 'UNDER_TEST') THEN 'QC_HOLD' "
                + "WHEN si.qc_status = 'RELEASED' THEN 'FINISHED_GOODS_WAREHOUSE' "
                + "WHEN si.qc_status = 'APPROVED' AND mm.material_type = 'FINISHED_GOOD' THEN 'FINISHED_GOODS_WAREHOUSE' "
                + "WHEN si.qc_status = 'APPROVED' AND mm.material_type = 'PACKAGING' THEN 'PACKAGING_WAREHOUSE' "
                + "WHEN si.qc_status = 'APPROVED' AND mm.material_type IN ('RAW_MATERIAL', 'INTERMEDIATE') THEN 'RAW_MATERIAL_WAREHOUSE' "
                + "ELSE si.location_code END";

        String sql;
        if (d.isPostgresql()) {
            sql = "UPDATE " + stock + " si SET qc_status = " + qcCase + ", location_code = " + locationCase
                    + " FROM " + material + " mm WHERE mm.material_code = si.material_code";
        } else {
            sql = "UPDATE " + stock + " si JOIN " + material + " mm ON mm.material_code = si.material_code "
                    + "SET si.qc_status = " + qcCase + ", si.location_code = " + locationCase;
        }

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    // -----------------------------------------------------------------------
    //  INTERNAL HELPERS
    // -----------------------------------------------------------------------

    private Stock mapResultSetToStock(ResultSet rs) throws SQLException {
        Stock stock = new Stock();
        stock.setStockId(rs.getInt("stock_id"));
        stock.setMaterialCode(rs.getString("material_code"));
        stock.setBrandName(rs.getString("brand_name"));
        stock.setGenericName(rs.getString("generic_name"));
        stock.setManufacturer(rs.getString("manufacturer"));
        stock.setLocationCode(rs.getString("location_code"));
        stock.setBatchNumber(rs.getString("batch_number"));
        stock.setQuantity(rs.getDouble("quantity"));
        stock.setReservedQuantity(readOptionalDouble(rs, "reserved_quantity"));
        stock.setAvailableQuantity(readOptionalDouble(rs, "available_quantity"));
        stock.setUnitCost(rs.getDouble("unit_cost"));
        java.sql.Date mfg = rs.getDate("mfg_date");
        if (mfg != null) {
            stock.setMfgDate(mfg.toLocalDate());
        }
        java.sql.Date exp = rs.getDate("exp_date");
        if (exp != null) {
            stock.setExpDate(exp.toLocalDate());
        }
        stock.setQcStatus(rs.getString("qc_status"));
        stock.setParentBatchId(rs.getString("parent_batch_id"));
        return stock;
    }

    private double readOptionalDouble(ResultSet rs, String columnName) throws SQLException {
        try {
            double value = rs.getDouble(columnName);
            return rs.wasNull() ? 0.0 : value;
        } catch (SQLException ex) {
            return 0.0;
        }
    }

    public List<pharma.model.InventoryTransaction> getAllInventoryTransactions() throws SQLException, ClassNotFoundException {
        List<pharma.model.InventoryTransaction> list = new ArrayList<>();
        String sql = "SELECT * FROM " + d.table(JdbcSqlDialect.Table.INVENTORY_TRANSACTION) + " ORDER BY transaction_timestamp DESC";
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                pharma.model.InventoryTransaction tx = new pharma.model.InventoryTransaction();
                tx.setTransactionId(rs.getInt("transaction_id"));
                tx.setMaterialCode(rs.getString("material_code"));
                tx.setBatchNumber(rs.getString("batch_number"));
                tx.setLocationCode(rs.getString("location_code"));
                tx.setTransactionType(rs.getString("transaction_type"));
                tx.setQuantity(rs.getDouble("quantity"));
                tx.setReferenceType(rs.getString("reference_type"));
                tx.setReferenceId(rs.getString("reference_id"));
                tx.setPerformedBy(rs.getInt("performed_by"));
                java.sql.Timestamp ts = rs.getTimestamp("transaction_timestamp");
                if (ts != null) {
                    tx.setTransactionTimestamp(ts.toLocalDateTime());
                }
                tx.setNotes(rs.getString("notes"));
                list.add(tx);
            }
        }
        return list;
    }

    public boolean updateQcStatus(String batchNumber, String status) throws SQLException, ClassNotFoundException {
        String sql = "UPDATE " + d.table(JdbcSqlDialect.Table.STOCK_INVENTORY) + " SET qc_status = ?, available_quantity = CASE WHEN ? = 'APPROVED' THEN quantity - reserved_quantity ELSE 0 END WHERE batch_number = ?";
        try (Connection conn = databaseService.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, status);
                stmt.setString(2, status);
                stmt.setString(3, batchNumber);
                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    ensureStatusLocationConsistency(conn);
                    conn.commit();
                    return true;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
        return false;
    }

    public record ConsumedStockLine(int stockId, String batchNumber, double quantityConsumed) {
    }
}
