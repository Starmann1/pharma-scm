package pharma.repository.jdbc;

import pharma.model.PurchaseOrder;
import pharma.model.PurchaseOrder.PurchaseOrderItem;
import pharma.model.Supplier;
import pharma.service.DatabaseService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialect-aware JDBC repository for Purchase Order and PurchaseOrder_Item tables.
 * Extracted from DatabaseService as part of the v1.1 PostgreSQL migration (Phase 5).
 *
 * <p>All SQL uses {@link JdbcSqlDialect} for table name resolution so the same
 * code works against both the legacy MySQL (v1) schema and the new PostgreSQL
 * (v1.1) schema.</p>
 */
public class PurchaseOrderJdbcRepository {

    private static final Logger logger = LoggerFactory.getLogger(PurchaseOrderJdbcRepository.class);

    private final DatabaseService databaseService;
    private final JdbcSqlDialect d;

    public PurchaseOrderJdbcRepository(DatabaseService databaseService) {
        this(databaseService, JdbcSqlDialect.from(DatabaseService.getDatabaseConfig()));
    }

    PurchaseOrderJdbcRepository(DatabaseService databaseService, JdbcSqlDialect sqlDialect) {
        this.databaseService = databaseService;
        this.d = sqlDialect;
    }

    // -----------------------------------------------------------------------
    //  READ
    // -----------------------------------------------------------------------

    /**
     * Returns all purchase orders, most recent first.
     */
    public List<PurchaseOrder> findAll() {
        List<PurchaseOrder> orders = new ArrayList<>();
        String sql = "SELECT po.po_id, po.supplier_id, s.supplier_name, po.order_date,"
                + " po.expected_date, po.total_amount, po.status"
                + " FROM " + d.table(JdbcSqlDialect.Table.PURCHASE_ORDER) + " po"
                + " JOIN " + d.table(JdbcSqlDialect.Table.SUPPLIER_MASTER) + " s"
                + " ON po.supplier_id = s.supplier_id"
                + " ORDER BY po.po_id DESC";

        try (Connection conn = databaseService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                orders.add(mapResultSetToPurchaseOrder(rs));
            }
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error fetching all Purchase Orders: {}", e.getMessage(), e);
        }
        return orders;
    }

    /**
     * Finds a single purchase order by its integer ID.
     */
    public PurchaseOrder findById(int poId) {
        String sql = "SELECT po.po_id, po.supplier_id, po.order_date, po.expected_date,"
                + " po.total_amount, po.status, s.supplier_name"
                + " FROM " + d.table(JdbcSqlDialect.Table.PURCHASE_ORDER) + " po"
                + " JOIN " + d.table(JdbcSqlDialect.Table.SUPPLIER_MASTER) + " s"
                + " ON po.supplier_id = s.supplier_id"
                + " WHERE po.po_id = ?";

        try (Connection conn = databaseService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, poId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPurchaseOrder(rs);
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error retrieving Purchase Order {}: {}", poId, e.getMessage(), e);
        }
        return null;
    }

    /**
     * Returns the line items for a given purchase order.
     */
    public List<PurchaseOrderItem> findItemsByPoId(int poId) throws SQLException, ClassNotFoundException {
        List<PurchaseOrderItem> items = new ArrayList<>();
        String sql = "SELECT drug_id, quantity, unit_price FROM "
                + d.table(JdbcSqlDialect.Table.PURCHASE_ORDER_ITEM)
                + " WHERE po_id = ?";

        try (Connection conn = databaseService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, poId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(new PurchaseOrderItem(
                            rs.getString("drug_id"),
                            rs.getInt("quantity"),
                            rs.getDouble("unit_price")));
                }
            }
        }
        return items;
    }

    // -----------------------------------------------------------------------
    //  WRITE
    // -----------------------------------------------------------------------

    /**
     * Creates a new purchase order with header and line items inside a single
     * transaction. Validates that the supplier is approved before proceeding.
     *
     * @return {@code true} if the PO was created successfully
     */
    public boolean create(PurchaseOrder po) throws ClassNotFoundException {
        String sqlHeader = "INSERT INTO " + d.table(JdbcSqlDialect.Table.PURCHASE_ORDER)
                + " (supplier_id, order_date, expected_date, total_amount, status)"
                + " VALUES (?, ?, ?, ?, ?)";
        int newPoId = -1;

        try (Connection conn = databaseService.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sqlHeader, Statement.RETURN_GENERATED_KEYS)) {
                validateSupplierApproved(conn, po.getSupplierId(), "Supplier is not approved.");

                pstmt.setInt(1, po.getSupplierId());
                pstmt.setDate(2, Date.valueOf(po.getOrderDate()));
                pstmt.setDate(3, Date.valueOf(po.getExpectedDate()));
                pstmt.setDouble(4, po.getTotalAmount());
                pstmt.setString(5, po.getStatus());

                if (pstmt.executeUpdate() == 0) {
                    throw new SQLException("Creating PO header failed, no rows affected.");
                }

                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        newPoId = generatedKeys.getInt(1);
                        po.setId(newPoId);
                    } else {
                        throw new SQLException("Creating PO header failed, no ID obtained.");
                    }
                }

                // Insert line items
                if (po.getItems() != null && !po.getItems().isEmpty()) {
                    insertItems(conn, newPoId, po.getItems());
                }

                // Audit trail
                logAuditTrail(conn, 0, "CREATE_PO",
                        d.table(JdbcSqlDialect.Table.PURCHASE_ORDER),
                        String.valueOf(newPoId), null, "Total: " + po.getTotalAmount());

                conn.commit();
                logger.info("Purchase Order created successfully with ID: {}", newPoId);
                return true;

            } catch (SQLException e) {
                conn.rollback();
                logger.error("Transaction rollback during PO creation: {}", e.getMessage(), e);
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Database connection error during PO creation: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Convenience wrapper: creates a minimal PO for a supplier by name.
     */
    public boolean createForSupplier(String supplierName) {
        try {
            int supplierId = findSupplierIdByName(supplierName);
            if (supplierId == -1) return false;

            PurchaseOrder minimalPo = new PurchaseOrder(
                    supplierId, supplierName,
                    LocalDate.now(), LocalDate.now().plusDays(7),
                    0.00, "Pending", new ArrayList<>());
            return create(minimalPo);
        } catch (Exception e) {
            logger.error("Error in simple PO creation wrapper: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Updates an existing purchase order header and replaces all line items.
     */
    public boolean update(PurchaseOrder updatedPo) {
        String updateHeaderSql = "UPDATE " + d.table(JdbcSqlDialect.Table.PURCHASE_ORDER)
                + " SET supplier_id = ?, order_date = ?, expected_date = ?, total_amount = ?, status = ?"
                + " WHERE po_id = ?";
        String deleteItemsSql = "DELETE FROM " + d.table(JdbcSqlDialect.Table.PURCHASE_ORDER_ITEM)
                + " WHERE po_id = ?";

        try (Connection conn = databaseService.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement headerStmt = conn.prepareStatement(updateHeaderSql);
                    PreparedStatement deleteItemsStmt = conn.prepareStatement(deleteItemsSql)) {

                validateSupplierApproved(conn, updatedPo.getSupplierId(), "Supplier is not approved.");

                headerStmt.setInt(1, updatedPo.getSupplierId());
                headerStmt.setDate(2, Date.valueOf(updatedPo.getOrderDate()));
                headerStmt.setDate(3, Date.valueOf(updatedPo.getExpectedDate()));
                headerStmt.setDouble(4, updatedPo.getTotalAmount());
                headerStmt.setString(5, updatedPo.getStatus());
                headerStmt.setInt(6, updatedPo.getId());
                headerStmt.executeUpdate();

                deleteItemsStmt.setInt(1, updatedPo.getId());
                deleteItemsStmt.executeUpdate();

                if (updatedPo.getItems() != null && !updatedPo.getItems().isEmpty()) {
                    insertItems(conn, updatedPo.getId(), updatedPo.getItems());
                }

                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                logger.error("Update PO rollback: {}", ex.getMessage(), ex);
                return false;
            }
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Update PO error: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Deletes a purchase order and its items. Throws if the PO has GRNs attached.
     */
    public void delete(int orderId) throws SQLException {
        String deleteItemsSql = "DELETE FROM " + d.table(JdbcSqlDialect.Table.PURCHASE_ORDER_ITEM) + " WHERE po_id = ?";
        String deleteOrderSql = "DELETE FROM " + d.table(JdbcSqlDialect.Table.PURCHASE_ORDER) + " WHERE po_id = ?";

        try (Connection conn = databaseService.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (!exists(conn, orderId)) {
                    throw new SQLException("Purchase order " + orderId + " was not found.");
                }
                if (hasGoodsReceivedNotes(conn, orderId)) {
                    throw new SQLIntegrityConstraintViolationException(
                            "Purchase order " + orderId
                                    + " cannot be deleted because goods have already been received for it."
                                    + " Delete the order only before creating a GRN.");
                }

                try (PreparedStatement itemsStmt = conn.prepareStatement(deleteItemsSql);
                        PreparedStatement orderStmt = conn.prepareStatement(deleteOrderSql)) {
                    itemsStmt.setInt(1, orderId);
                    itemsStmt.executeUpdate();

                    orderStmt.setInt(1, orderId);
                    int deletedRows = orderStmt.executeUpdate();
                    if (deletedRows == 0) {
                        throw new SQLException("Purchase order " + orderId + " could not be deleted.");
                    }
                }

                logAuditTrail(conn, 0, "DELETE_PO",
                        d.table(JdbcSqlDialect.Table.PURCHASE_ORDER),
                        String.valueOf(orderId), "Existing", null);
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                logger.error("Delete PurchaseOrder rollback: {}", ex.getMessage());
                throw ex;
            }
        } catch (ClassNotFoundException e) {
            logger.error("Delete PurchaseOrder error: {}", e.getMessage());
            throw new SQLException("Database driver not found: " + e.getMessage());
        }
    }

    /**
     * Marks a purchase order as "Received".
     */
    public void receiveShipment(int orderId) {
        String sql = "UPDATE " + d.table(JdbcSqlDialect.Table.PURCHASE_ORDER)
                + " SET status = ? WHERE po_id = ?";

        try (Connection conn = databaseService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "Received");
            pstmt.setInt(2, orderId);
            pstmt.executeUpdate();
            logAuditTrail(conn, 0, "RECEIVE_PO",
                    d.table(JdbcSqlDialect.Table.PURCHASE_ORDER),
                    String.valueOf(orderId), "Pending", "Received");
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error marking PO as received: {}", e.getMessage(), e);
        }
    }

    /**
     * Generates a human-readable PO number.
     */
    public String generateNextPoNumber() {
        return "PO-" + LocalDate.now().getYear() + "-" + System.currentTimeMillis() % 10000;
    }

    // -----------------------------------------------------------------------
    //  INTERNAL HELPERS
    // -----------------------------------------------------------------------

    private PurchaseOrder mapResultSetToPurchaseOrder(ResultSet rs) throws SQLException {
        int id = rs.getInt("po_id");
        int supplierId = rs.getInt("supplier_id");

        String supplierName = null;
        try {
            supplierName = rs.getString("supplier_name");
        } catch (SQLException e) {
            // column not in result set, will fall back below
        }
        if (supplierName == null) {
            supplierName = lookupSupplierName(supplierId);
        }

        LocalDate orderDate = rs.getDate("order_date").toLocalDate();
        LocalDate expectedDate = rs.getDate("expected_date").toLocalDate();
        double totalAmount = rs.getDouble("total_amount");
        String status = rs.getString("status");

        List<PurchaseOrderItem> items;
        try {
            items = findItemsByPoId(id);
        } catch (ClassNotFoundException e) {
            logger.error("Error loading items for PO {}: {}", id, e.getMessage());
            items = List.of();
        }

        return new PurchaseOrder(id, supplierId, supplierName, orderDate, expectedDate,
                totalAmount, status, items);
    }

    private void insertItems(Connection conn, int poId, List<PurchaseOrderItem> items) throws SQLException {
        String sql = "INSERT INTO " + d.table(JdbcSqlDialect.Table.PURCHASE_ORDER_ITEM)
                + " (po_id, drug_id, quantity, unit_price) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (PurchaseOrderItem item : items) {
                pstmt.setInt(1, poId);
                pstmt.setString(2, item.getMaterialCode());
                pstmt.setInt(3, item.getQuantity());
                pstmt.setDouble(4, item.getUnitPrice());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    private boolean exists(Connection conn, int orderId) throws SQLException {
        String sql = "SELECT 1 FROM " + d.table(JdbcSqlDialect.Table.PURCHASE_ORDER) + " WHERE po_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean hasGoodsReceivedNotes(Connection conn, int orderId) throws SQLException {
        String sql = "SELECT 1 FROM " + d.table(JdbcSqlDialect.Table.GOODS_RECEIVED_NOTE)
                + " WHERE po_id = ? LIMIT 1";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void validateSupplierApproved(Connection conn, int supplierId, String message) throws SQLException {
        String sql = "SELECT supplier_status FROM " + d.table(JdbcSqlDialect.Table.SUPPLIER_MASTER)
                + " WHERE supplier_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, supplierId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Supplier not found.");
                }
                String status = rs.getString("supplier_status");
                if (!Supplier.STATUS_APPROVED.equalsIgnoreCase(
                        status != null ? status.trim() : null)) {
                    throw new SQLException(message);
                }
            }
        }
    }

    private int findSupplierIdByName(String supplierName) {
        String sql = "SELECT supplier_id FROM " + d.table(JdbcSqlDialect.Table.SUPPLIER_MASTER)
                + " WHERE supplier_name = ?";
        try (Connection conn = databaseService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, supplierName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt("supplier_id");
            }
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error fetching supplier ID for '{}': {}", supplierName, e.getMessage());
        }
        return -1;
    }

    private String lookupSupplierName(int supplierId) {
        String sql = "SELECT supplier_name FROM " + d.table(JdbcSqlDialect.Table.SUPPLIER_MASTER)
                + " WHERE supplier_id = ?";
        try (Connection conn = databaseService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, supplierId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getString("supplier_name");
            }
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error looking up supplier name for ID {}: {}", supplierId, e.getMessage());
        }
        return "Unknown Supplier";
    }

    private void logAuditTrail(Connection conn, int userId, String actionType, String tableName,
            String recordId, String oldValue, String newValue) throws SQLException {
        String sql = "INSERT INTO " + d.table(JdbcSqlDialect.Table.SYSTEM_AUDIT_TRAIL)
                + " (user_id, action_type, table_name, record_id, old_value, new_value)"
                + " VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (userId <= 0) {
                pstmt.setNull(1, Types.INTEGER);
            } else {
                pstmt.setInt(1, userId);
            }
            pstmt.setString(2, actionType);
            pstmt.setString(3, tableName);
            pstmt.setString(4, recordId);
            pstmt.setString(5, oldValue);
            pstmt.setString(6, newValue);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error logging audit trail: {}", e.getMessage(), e);
            throw e;
        }
    }
}
