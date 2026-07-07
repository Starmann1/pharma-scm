package pharma.repository.jdbc;

import pharma.model.BOMDetail;
import pharma.model.BOMHeader;
import pharma.model.ProductionOrder;
import pharma.service.DatabaseService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProductionOrderJdbcRepository {

    private static final Logger logger = LoggerFactory.getLogger(ProductionOrderJdbcRepository.class);

    private final DatabaseService databaseService;
    private final JdbcSqlDialect d;
    private final StockJdbcRepository stockRepository;

    public ProductionOrderJdbcRepository(DatabaseService databaseService) {
        this(databaseService, JdbcSqlDialect.from(DatabaseService.getDatabaseConfig()));
    }

    ProductionOrderJdbcRepository(DatabaseService databaseService, JdbcSqlDialect sqlDialect) {
        this.databaseService = databaseService;
        this.d = sqlDialect;
        this.stockRepository = new StockJdbcRepository(databaseService, sqlDialect);
    }

    private Connection getConnection() throws SQLException, ClassNotFoundException {
        return databaseService.getConnection();
    }

    public int createProductionOrder(ProductionOrder order) throws SQLException, ClassNotFoundException {
        String sql = "INSERT INTO " + d.table(JdbcSqlDialect.Table.PRODUCTION_ORDER) + " (batch_number, bom_id, planned_qty, status, production_date, created_by, notes) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, order.getBatchNumber());
            pstmt.setInt(2, order.getBomId());
            pstmt.setDouble(3, order.getPlannedQty());
            pstmt.setString(4, order.getStatus().getDisplayName());
            pstmt.setDate(5, java.sql.Date.valueOf(order.getProductionDate()));
            pstmt.setInt(6, order.getCreatedBy());
            pstmt.setString(7, order.getNotes());

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int orderId = rs.getInt(1);
                    databaseService.logAuditTrail(conn, order.getCreatedBy(), "CREATE_PRODUCTION_ORDER", "Production_Order",
                            String.valueOf(orderId), null, order.getBatchNumber());
                    return orderId;
                }
            }
        }
        return -1;
    }

    public List<ProductionOrder> getAllProductionOrders() throws SQLException, ClassNotFoundException {
        List<ProductionOrder> orders = new ArrayList<>();
        String sql = "SELECT * FROM " + d.table(JdbcSqlDialect.Table.PRODUCTION_ORDER) + " ORDER BY production_date DESC, order_id DESC";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                orders.add(mapResultSetToProductionOrder(rs));
            }
        }
        return orders;
    }

    public ProductionOrder getProductionOrderById(int orderId) throws SQLException, ClassNotFoundException {
        String sql = "SELECT * FROM " + d.table(JdbcSqlDialect.Table.PRODUCTION_ORDER) + " WHERE order_id = ?";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, orderId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProductionOrder(rs);
                }
            }
        }
        return null;
    }

    private ProductionOrder mapResultSetToProductionOrder(ResultSet rs) throws SQLException {
        return new ProductionOrder(
                rs.getInt("order_id"),
                rs.getString("batch_number"),
                rs.getInt("bom_id"),
                rs.getDouble("planned_qty"),
                rs.getObject("actual_qty") != null ? rs.getDouble("actual_qty") : null,
                ProductionOrder.ProductionStatus.fromString(rs.getString("status")),
                rs.getDate("production_date").toLocalDate(),
                rs.getDate("completed_date") != null ? rs.getDate("completed_date").toLocalDate() : null,
                rs.getInt("created_by"),
                rs.getString("notes"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime());
    }

    public void updateProductionOrderStatus(int orderId, String newStatus) throws SQLException, ClassNotFoundException {
        String sql = "UPDATE " + d.table(JdbcSqlDialect.Table.PRODUCTION_ORDER) + " SET status = ?, updated_at = " + d.nowExpression() + " WHERE order_id = ?";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStatus);
            pstmt.setInt(2, orderId);
            pstmt.executeUpdate();
        }
    }

    public void executeProductionRun(int orderId, int userId) throws SQLException, ClassNotFoundException {
        Connection conn = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            ProductionOrder order = getProductionOrderById(orderId);
            if (order == null) {
                throw new SQLException("Production order not found: " + orderId);
            }

            BOMHeader bom = databaseService.getBOMById(order.getBomId());
            List<BOMDetail> ingredients = databaseService.getBOMIngredients(order.getBomId());

            Map<String, Double> shortages = databaseService.validateBOMAvailability(order.getBomId(), order.getPlannedQty());
            for (Map.Entry<String, Double> entry : shortages.entrySet()) {
                if (entry.getValue() > 0) {
                    throw new SQLException(
                            "Insufficient material: " + entry.getKey() + ", shortage: " + entry.getValue());
                }
            }

            StringBuilder parentBatches = new StringBuilder();

            for (BOMDetail ingredient : ingredients) {
                double qtyNeeded = ingredient.getRequiredQty() * order.getPlannedQty();

                List<StockJdbcRepository.ConsumedStockLine> consumedLines =
                        stockRepository.consumeStock(conn, ingredient.getIngredientMaterialCode(), qtyNeeded);

                for (StockJdbcRepository.ConsumedStockLine line : consumedLines) {
                    String batchNumber = line.batchNumber();
                    double toConsume = line.quantityConsumed();

                    // 1. Material Consumption
                    String insertMcSql = "INSERT INTO " + d.table(JdbcSqlDialect.Table.PRODUCTION_MATERIAL_CONSUMPTION) + " (production_order_id, material_code, batch_number, required_qty, consumed_qty, uom) VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement mcStmt = conn.prepareStatement(insertMcSql)) {
                        mcStmt.setInt(1, orderId);
                        mcStmt.setString(2, ingredient.getIngredientMaterialCode());
                        mcStmt.setString(3, batchNumber);
                        mcStmt.setDouble(4, qtyNeeded);
                        mcStmt.setDouble(5, toConsume);
                        mcStmt.setString(6, ingredient.getUom());
                        mcStmt.executeUpdate();
                    }

                    // 2. Inventory Transaction (Consumption)
                    String insertTxSql = "INSERT INTO " + d.table(JdbcSqlDialect.Table.INVENTORY_TRANSACTION) + " (material_code, batch_number, location_code, transaction_type, quantity, reference_type, reference_id, performed_by, notes) VALUES (?, ?, 'PRODUCTION_FLOOR', 'PRODUCTION_CONSUMPTION', ?, 'PRODUCTION_ORDER', ?, ?, ?)";
                    try (PreparedStatement txStmt = conn.prepareStatement(insertTxSql)) {
                        txStmt.setString(1, ingredient.getIngredientMaterialCode());
                        txStmt.setString(2, batchNumber);
                        txStmt.setDouble(3, -toConsume);
                        txStmt.setString(4, String.valueOf(orderId));
                        txStmt.setInt(5, userId);
                        txStmt.setString(6, "Consumed for Order " + orderId);
                        txStmt.executeUpdate();
                    }

                    // 3. Batch Genealogy
                    String insertBgSql = "INSERT INTO " + d.table(JdbcSqlDialect.Table.BATCH_GENEALOGY) + " (parent_batch, child_batch, production_order_id, relationship_type) VALUES (?, ?, ?, 'USED_IN')";
                    try (PreparedStatement bgStmt = conn.prepareStatement(insertBgSql)) {
                        bgStmt.setString(1, batchNumber);
                        bgStmt.setString(2, order.getBatchNumber());
                        bgStmt.setInt(3, orderId);
                        bgStmt.executeUpdate();
                    }

                    if (parentBatches.length() > 0) {
                        parentBatches.append(",");
                    }
                    parentBatches.append(batchNumber);
                }
            }

            stockRepository.insertProductionStock(conn, bom.getMaterialCode(), "PRODUCTION_FLOOR",
                    order.getBatchNumber(), order.getPlannedQty(), parentBatches.toString(), orderId);

            // 4. Production Batch Record
            String insertPbSql = "INSERT INTO " + d.table(JdbcSqlDialect.Table.PRODUCTION_BATCH) + " (production_order_id, material_code, batch_number, quantity, mfg_date, expiry_date, qc_status, location_code) VALUES (?, ?, ?, ?, ?, ?, 'IN_PRODUCTION', 'PRODUCTION_FLOOR')";
            try (PreparedStatement pbStmt = conn.prepareStatement(insertPbSql)) {
                pbStmt.setInt(1, orderId);
                pbStmt.setString(2, bom.getMaterialCode());
                pbStmt.setString(3, order.getBatchNumber());
                pbStmt.setDouble(4, order.getPlannedQty());
                pbStmt.setDate(5, java.sql.Date.valueOf(LocalDate.now()));
                pbStmt.setDate(6, java.sql.Date.valueOf(LocalDate.now().plusYears(2)));
                pbStmt.executeUpdate();
            }

            // 5. Inventory Transaction (Finished Good Receipt)
            String insertTxFGSql = "INSERT INTO " + d.table(JdbcSqlDialect.Table.INVENTORY_TRANSACTION) + " (material_code, batch_number, location_code, transaction_type, quantity, reference_type, reference_id, performed_by, notes) VALUES (?, ?, 'PRODUCTION_FLOOR', 'PRODUCTION_RECEIPT', ?, 'PRODUCTION_ORDER', ?, ?, ?)";
            try (PreparedStatement txFgStmt = conn.prepareStatement(insertTxFGSql)) {
                txFgStmt.setString(1, bom.getMaterialCode());
                txFgStmt.setString(2, order.getBatchNumber());
                txFgStmt.setDouble(3, order.getPlannedQty());
                txFgStmt.setString(4, String.valueOf(orderId));
                txFgStmt.setInt(5, userId);
                txFgStmt.setString(6, "Received from Production Order " + orderId);
                txFgStmt.executeUpdate();
            }

            // 6. Event Log
            String insertEventSql = "INSERT INTO " + d.table(JdbcSqlDialect.Table.EVENT_LOG) + " (event_type, entity_type, entity_id, details, status) VALUES (?, 'PRODUCTION_ORDER', ?, ?, 'SUCCESS')";
            try (PreparedStatement evStmt = conn.prepareStatement(insertEventSql)) {
                evStmt.setString(1, "PRODUCTION_COMPLETED");
                evStmt.setString(2, String.valueOf(orderId));
                evStmt.setString(3, "Production run executed. Batch: " + order.getBatchNumber());
                evStmt.executeUpdate();
            }

            String curDate = d.isMysql() ? "CURDATE()" : "CURRENT_DATE";
            String updateOrderSql = "UPDATE " + d.table(JdbcSqlDialect.Table.PRODUCTION_ORDER) + " SET status = 'In-Production', actual_qty = ?, completed_date = " + curDate + " WHERE order_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateOrderSql)) {
                pstmt.setDouble(1, order.getPlannedQty());
                pstmt.setInt(2, orderId);
                pstmt.executeUpdate();
            }

            databaseService.logAuditTrail(conn, userId, "PRODUCTION_RUN", "Production_Order", String.valueOf(orderId), "Planned",
                    "In-Production");

            conn.commit();
            System.out.println("Production run executed successfully for order: " + orderId);

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    System.err.println("Production run failed, transaction rolled back: " + e.getMessage());
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
            }
        }
    }
}
