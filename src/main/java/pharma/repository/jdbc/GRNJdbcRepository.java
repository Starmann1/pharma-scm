package pharma.repository.jdbc;

import pharma.model.GRN;
import pharma.model.GRN.GRNItem;
import pharma.model.PurchaseOrder;
import pharma.model.PurchaseOrder.PurchaseOrderItem;
import pharma.model.Supplier;
import pharma.service.DatabaseService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialect-aware JDBC repository for Goods Received Notes (GRN) and GRN_Item tables.
 * Extracted from DatabaseService as part of the v1.1 PostgreSQL migration (Phase 5).
 *
 * <p>All SQL uses {@link JdbcSqlDialect} for table name resolution and dialect-specific
 * syntax (e.g. ON DUPLICATE KEY UPDATE vs ON CONFLICT) so the same code works against
 * both the legacy MySQL (v1) schema and the new PostgreSQL (v1.1) schema.</p>
 */
public class GRNJdbcRepository {

    private static final Logger logger = LoggerFactory.getLogger(GRNJdbcRepository.class);

    private final DatabaseService databaseService;
    private final JdbcSqlDialect d;
    private final StockJdbcRepository stockRepository;

    public GRNJdbcRepository(DatabaseService databaseService) {
        this(databaseService, JdbcSqlDialect.from(DatabaseService.getDatabaseConfig()));
    }

    GRNJdbcRepository(DatabaseService databaseService, JdbcSqlDialect sqlDialect) {
        this.databaseService = databaseService;
        this.d = sqlDialect;
        this.stockRepository = new StockJdbcRepository(databaseService, sqlDialect);
    }

    // -----------------------------------------------------------------------
    //  READ
    // -----------------------------------------------------------------------

    /**
     * Returns all GRNs with supplier name resolved via PO → Supplier join.
     * Items are not loaded at this level (lazy — call {@link #findItemsByGrnId} separately).
     */
    public List<GRN> findAll() {
        List<GRN> grns = new ArrayList<>();
        String sql = "SELECT g.grn_id, g.po_id, g.received_date, g.received_by, g.status, s.supplier_name"
                + " FROM " + d.table(JdbcSqlDialect.Table.GOODS_RECEIVED_NOTE) + " g"
                + " LEFT JOIN " + d.table(JdbcSqlDialect.Table.PURCHASE_ORDER) + " po ON g.po_id = po.po_id"
                + " LEFT JOIN " + d.table(JdbcSqlDialect.Table.SUPPLIER_MASTER) + " s ON po.supplier_id = s.supplier_id";

        try (Connection conn = databaseService.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                grns.add(mapGrnHeader(rs, List.of()));
            }
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error fetching GRNs: {}", e.getMessage(), e);
        }
        return grns;
    }

    /**
     * Returns a single GRN by ID, including its line items.
     */
    public GRN findById(int grnId) {
        String sql = "SELECT g.grn_id, g.po_id, g.received_date, g.received_by, g.status, s.supplier_name"
                + " FROM " + d.table(JdbcSqlDialect.Table.GOODS_RECEIVED_NOTE) + " g"
                + " LEFT JOIN " + d.table(JdbcSqlDialect.Table.PURCHASE_ORDER) + " po ON g.po_id = po.po_id"
                + " LEFT JOIN " + d.table(JdbcSqlDialect.Table.SUPPLIER_MASTER) + " s ON po.supplier_id = s.supplier_id"
                + " WHERE g.grn_id = ?";

        try (Connection conn = databaseService.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, grnId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    List<GRNItem> items = findItemsByGrnId(grnId);
                    return mapGrnHeader(rs, items);
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error fetching GRN by ID {}: {}", grnId, e.getMessage(), e);
        }
        return null;
    }

    /**
     * Returns line items for a specific GRN.
     * Aligned with schema.
     */
    public List<GRNItem> findItemsByGrnId(int grnId) {
        List<GRNItem> items = new ArrayList<>();
        String sql = "SELECT material_code, batch_number, quantity_received, expiry_date"
                + " FROM " + d.table(JdbcSqlDialect.Table.GRN_ITEM)
                + " WHERE grn_id = ?";

        try (Connection conn = databaseService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, grnId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapGrnItem(rs));
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error fetching GRN items for GRN ID {}: {}", grnId, e.getMessage(), e);
        }
        return items;
    }

    // -----------------------------------------------------------------------
    //  WRITE
    // -----------------------------------------------------------------------

    /**
     * Creates a GRN from a fully-populated PurchaseOrder.
     * This is a complex transactional operation that:
     * <ol>
     *   <li>Validates the supplier is approved</li>
     *   <li>Inserts a GRN header</li>
     *   <li>Inserts GRN line items</li>
     *   <li>Upserts stock inventory (dialect-aware: ON DUPLICATE KEY / ON CONFLICT)</li>
     *   <li>Inserts inventory transaction records</li>
     *   <li>Logs an event</li>
     *   <li>Updates the PO status to "Received"</li>
     * </ol>
     *
     * @return {@code true} if the GRN was created and committed successfully
     */
    public boolean createFromPO(PurchaseOrder po) {
        logger.info("Creating GRN for PO: {} (ID: {})", po.getPoNumber(), po.getId());

        String insertGrnSql = "INSERT INTO " + d.table(JdbcSqlDialect.Table.GOODS_RECEIVED_NOTE)
                + " (po_id, received_date, received_by, status) VALUES (?, " + d.nowExpression() + ", ?, ?)";
        String insertGrnItemSql = "INSERT INTO " + d.table(JdbcSqlDialect.Table.GRN_ITEM)
                + " (grn_id, material_code, batch_number, quantity_received, expiry_date) VALUES (?, ?, ?, ?, ?)";
        String upsertStockSql = stockRepository.upsertStockSql();
        String insertTxSql = "INSERT INTO " + d.table(JdbcSqlDialect.Table.INVENTORY_TRANSACTION)
                + " (material_code, batch_number, location_code, transaction_type, quantity,"
                + " reference_type, reference_id, performed_by, notes)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String insertEventSql = "INSERT INTO " + d.table(JdbcSqlDialect.Table.EVENT_LOG)
                + " (event_type, entity_type, entity_id, details, status) VALUES (?, ?, ?, ?, ?)";
        String updatePoStatusSql = "UPDATE " + d.table(JdbcSqlDialect.Table.PURCHASE_ORDER)
                + " SET status = ? WHERE po_id = ?";

        try (Connection conn = databaseService.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Validate supplier approval
                validateSupplierApproved(conn, po.getSupplierId(),
                        "Cannot create GRN for unapproved supplier.");

                // 1. Create GRN header
                int grnId;
                try (PreparedStatement pstmt = conn.prepareStatement(insertGrnSql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setInt(1, po.getId());
                    pstmt.setString(2, "admin");
                    pstmt.setString(3, "Verified");

                    int rowsAffected = pstmt.executeUpdate();
                    if (rowsAffected == 0) {
                        throw new SQLException("Creating GRN failed, no rows affected.");
                    }

                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            grnId = generatedKeys.getInt(1);
                            logger.info("Created GRN with ID: {}", grnId);
                        } else {
                            throw new SQLException("Creating GRN failed, no ID obtained.");
                        }
                    }
                }

                // 2. Fetch PO items and create GRN items + update inventory
                List<PurchaseOrderItem> poItems = loadPoItemsWithinTransaction(conn, po.getId());

                if (poItems == null || poItems.isEmpty()) {
                    logger.warn("No items found for PO {}", po.getId());
                    conn.rollback();
                    return false;
                }

                try (PreparedStatement grnItemStmt = conn.prepareStatement(insertGrnItemSql);
                        PreparedStatement stockStmt = conn.prepareStatement(upsertStockSql);
                        PreparedStatement txStmt = conn.prepareStatement(insertTxSql);
                        PreparedStatement eventStmt = conn.prepareStatement(insertEventSql)) {

                    for (PurchaseOrderItem item : poItems) {
                        String batchNumber = "BATCH-" + System.currentTimeMillis() + "-" + item.getMaterialCode();
                        LocalDate expiryDate = LocalDate.now().plusYears(2);
                        LocalDate mfgDate = LocalDate.now();

                        // GRN item
                        grnItemStmt.setInt(1, grnId);
                        grnItemStmt.setString(2, item.getMaterialCode());
                        grnItemStmt.setString(3, batchNumber);
                        grnItemStmt.setInt(4, item.getQuantity());
                        grnItemStmt.setDate(5, Date.valueOf(expiryDate));
                        grnItemStmt.addBatch();

                        // Stock upsert
                        stockRepository.bindUpsertStock(stockStmt, item.getMaterialCode(), "QC_HOLD", batchNumber,
                                item.getQuantity(), item.getUnitPrice(), mfgDate, expiryDate, "QUARANTINE");
                        stockStmt.addBatch();

                        // Inventory transaction
                        txStmt.setString(1, item.getMaterialCode());
                        txStmt.setString(2, batchNumber);
                        txStmt.setString(3, "QC_HOLD");
                        txStmt.setString(4, "GRN_RECEIPT");
                        txStmt.setDouble(5, item.getQuantity());
                        txStmt.setString(6, "GRN");
                        txStmt.setString(7, String.valueOf(grnId));
                        txStmt.setInt(8, 1); // System Admin ID
                        txStmt.setString(9, "Received from PO " + po.getPoNumber());
                        txStmt.addBatch();

                        logger.debug("Added GRN item: Material={}, Qty={}, Batch={}",
                                item.getMaterialCode(), item.getQuantity(), batchNumber);
                    }

                    grnItemStmt.executeBatch();
                    stockStmt.executeBatch();
                    txStmt.executeBatch();

                    // Event log
                    eventStmt.setString(1, "MATERIAL_RECEIVED");
                    eventStmt.setString(2, "GRN");
                    eventStmt.setString(3, String.valueOf(grnId));
                    eventStmt.setString(4, "Received materials for PO " + po.getPoNumber());
                    eventStmt.setString(5, "SUCCESS");
                    eventStmt.executeUpdate();
                }

                // 3. Update PO status to "Received"
                try (PreparedStatement pstmt = conn.prepareStatement(updatePoStatusSql)) {
                    pstmt.setString(1, "Received");
                    pstmt.setInt(2, po.getId());
                    pstmt.executeUpdate();
                }

                conn.commit();
                logger.info("GRN creation successful for PO {}", po.getId());
                return true;

            } catch (SQLException e) {
                conn.rollback();
                logger.error("Error creating GRN, transaction rolled back: {}", e.getMessage(), e);
                return false;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Database connection error during GRN creation: {}", e.getMessage(), e);
            return false;
        }
    }

    // -----------------------------------------------------------------------
    //  INTERNAL HELPERS
    // -----------------------------------------------------------------------

    private GRN mapGrnHeader(ResultSet rs, List<GRNItem> items) throws SQLException {
        int id = rs.getInt("grn_id");
        int poId = rs.getInt("po_id");
        Timestamp receivedTs = rs.getTimestamp("received_date");
        LocalDateTime receivedDate = receivedTs != null ? receivedTs.toLocalDateTime() : null;
        String receivedBy = rs.getString("received_by");
        String status = rs.getString("status");
        String supplierName = rs.getString("supplier_name");
        return new GRN(id, supplierName, poId, receivedDate, receivedBy, status, items);
    }

    private GRNItem mapGrnItem(ResultSet rs) throws SQLException {
        java.sql.Date expiryDate = rs.getDate("expiry_date");
        return new GRNItem(
                rs.getString("material_code"),
                rs.getString("batch_number"),
                rs.getInt("quantity_received"),
                expiryDate != null ? expiryDate.toLocalDate() : null);
    }

    /**
     * Loads PO items using an existing connection (for use within a transaction).
     */
    private List<PurchaseOrderItem> loadPoItemsWithinTransaction(Connection conn, int poId) throws SQLException {
        List<PurchaseOrderItem> items = new ArrayList<>();
        String sql = "SELECT material_code, quantity, unit_price FROM "
                + d.table(JdbcSqlDialect.Table.PURCHASE_ORDER_ITEM)
                + " WHERE po_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, poId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(new PurchaseOrderItem(
                            rs.getString("material_code"),
                            rs.getInt("quantity"),
                            rs.getDouble("unit_price")));
                }
            }
        }
        return items;
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
}
