package pharma.repository.jdbc;

import java.sql.*;

import pharma.dto.QAResultDTO;
import pharma.model.ProductionOrder;
import pharma.model.Stock;
import pharma.repository.QARepository;
import pharma.service.DatabaseService;

public class QAJdbcRepository implements QARepository {
    private final DatabaseService databaseService;
    private final JdbcSqlDialect d;

    public QAJdbcRepository(DatabaseService databaseService) {
        this(databaseService, JdbcSqlDialect.from(DatabaseService.getDatabaseConfig()));
    }

    QAJdbcRepository(DatabaseService databaseService, JdbcSqlDialect sqlDialect) {
        this.databaseService = databaseService;
        this.d = sqlDialect;
    }

    private Connection getConnection() throws SQLException, ClassNotFoundException {
        return databaseService.getConnection();
    }

    @Override
    public QAResultDTO reviewBatch(String batchNumber) throws SQLException, ClassNotFoundException {
        Stock stock = databaseService.getStockByBatchNumber(batchNumber);
        QAResultDTO dto = new QAResultDTO();
        dto.setBatchNumber(batchNumber);
        if (stock == null) {
            dto.setDecision("HOLD");
            dto.getFindings().add("Batch not found.");
            return dto;
        }
        dto.setPreviousStatus(stock.getQcStatus());
        if ("REJECTED".equalsIgnoreCase(stock.getQcStatus())) {
            dto.setDecision("FAIL");
            dto.setTargetStatus("REJECTED");
            dto.getFindings().add("Batch is already rejected.");
        } else if ("APPROVED".equalsIgnoreCase(stock.getQcStatus()) || "RELEASED".equalsIgnoreCase(stock.getQcStatus())) {
            dto.setDecision("PASS");
            dto.setTargetStatus(stock.getQcStatus());
        } else {
            dto.setDecision("HOLD");
            dto.setTargetStatus(stock.getQcStatus());
            dto.getFindings().add("Batch requires analyst-entered test result before release.");
        }
        return dto;
    }

    /**
     * Advances a batch to the next intermediate QC status:
     *   IN_PRODUCTION        -> IN_PROCESS_SAMPLE  (TAKE IPQC SAMPLE)
     *   IN_PROCESS_SAMPLE    -> UNDER_TEST         (START TESTING)
     *   QUARANTINE           -> QI                 (TAKE QC SAMPLE)
     *
     * Updates both stock_inventory and production_batch tables.
     * If the batch is not found in stock_inventory, falls back to production_batch only.
     */
    public void takeSampleForQC(String batchNumber, int userId) throws SQLException, ClassNotFoundException {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Try to find the current status from stock_inventory first
                String currentStatus = null;
                boolean inStockInventory = false;

                String selectStockSql = "SELECT qc_status FROM " + d.table(JdbcSqlDialect.Table.STOCK_INVENTORY)
                        + " WHERE batch_number = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(selectStockSql)) {
                    pstmt.setString(1, batchNumber);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            currentStatus = rs.getString("qc_status");
                            inStockInventory = true;
                        }
                    }
                }

                // Fallback: check production_batch table if not in stock_inventory
                if (currentStatus == null) {
                    String selectPbSql = "SELECT qc_status FROM " + d.table(JdbcSqlDialect.Table.PRODUCTION_BATCH)
                            + " WHERE batch_number = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(selectPbSql)) {
                        pstmt.setString(1, batchNumber);
                        try (ResultSet rs = pstmt.executeQuery()) {
                            if (rs.next()) {
                                currentStatus = rs.getString("qc_status");
                            }
                        }
                    }
                }

                if (currentStatus == null) {
                    throw new SQLException("Batch not found in any table: " + batchNumber);
                }

                String nextStatus;
                String eventType;
                String details;
                String auditAction;

                if ("IN_PRODUCTION".equalsIgnoreCase(currentStatus)) {
                    nextStatus = "IN_PROCESS_SAMPLE";
                    eventType = "IPQC_SAMPLE_TAKEN";
                    details = "IPQC sample taken. Batch moved from IN_PRODUCTION to IN_PROCESS_SAMPLE.";
                    auditAction = "IPQC_SAMPLE_TAKEN";
                } else if ("IN_PROCESS_SAMPLE".equalsIgnoreCase(currentStatus)) {
                    nextStatus = "UNDER_TEST";
                    eventType = "IPQC_TESTING_STARTED";
                    details = "IPQC testing started. Batch moved from IN_PROCESS_SAMPLE to UNDER_TEST.";
                    auditAction = "IPQC_TESTING_STARTED";
                } else if ("QUARANTINE".equalsIgnoreCase(currentStatus)) {
                    nextStatus = "QI";
                    eventType = "QC_SAMPLE_TAKEN";
                    details = "QC sample taken. Batch moved from QUARANTINE to QI.";
                    auditAction = "QC_SAMPLE_TAKEN";
                } else {
                    throw new SQLException(
                            "Batch " + batchNumber
                                    + " must be in IN_PRODUCTION, IN_PROCESS_SAMPLE, or QUARANTINE before sampling/testing can continue. Current status: " + currentStatus);
                }

                // Update stock_inventory if the record exists there
                if (inStockInventory) {
                    String updateSql = "UPDATE " + d.table(JdbcSqlDialect.Table.STOCK_INVENTORY)
                            + " SET qc_status = ?, location_code = 'QC_HOLD' WHERE batch_number = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                        pstmt.setString(1, nextStatus);
                        pstmt.setString(2, batchNumber);
                        pstmt.executeUpdate();
                    }
                }

                // Always update production_batch (may or may not exist — ignore 0 rows)
                String updatePbSql = "UPDATE " + d.table(JdbcSqlDialect.Table.PRODUCTION_BATCH)
                        + " SET qc_status = ?, location_code = 'QC_HOLD' WHERE batch_number = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updatePbSql)) {
                    pstmt.setString(1, nextStatus);
                    pstmt.setString(2, batchNumber);
                    pstmt.executeUpdate();
                }

                // Insert event log
                String insertEventSql = "INSERT INTO " + d.table(JdbcSqlDialect.Table.EVENT_LOG)
                        + " (event_type, entity_type, entity_id, details, status) VALUES (?, 'BATCH', ?, ?, 'SUCCESS')";
                try (PreparedStatement pstmt = conn.prepareStatement(insertEventSql)) {
                    pstmt.setString(1, eventType);
                    pstmt.setString(2, batchNumber);
                    pstmt.setString(3, details);
                    pstmt.executeUpdate();
                }

                databaseService.logAuditTrail(conn, userId, auditAction, "Stock_Inventory", batchNumber, currentStatus, nextStatus);
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Applies a final QC decision (APPROVED or REJECTED) to a batch.
     * Accepts batches in any active QC state (IN_PRODUCTION, IN_PROCESS_SAMPLE,
     * UNDER_TEST, QI, QUARANTINE). Updates both stock_inventory and production_batch
     * tables, sets the correct warehouse location, and syncs the production order status.
     *
     * If the batch is not found in stock_inventory, falls back to production_batch only.
     */
    public void updateQCStatus(String batchNumber, String newStatus, int userId)
            throws SQLException, ClassNotFoundException {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {

                String oldStatus = null;
                String materialType = null;
                Integer productionOrderId = null;
                boolean inStockInventory = false;

                // Try stock_inventory first (joins material_master for type)
                String typeSql = "SELECT si.qc_status, si.production_order_id, mm.material_type FROM "
                        + d.table(JdbcSqlDialect.Table.STOCK_INVENTORY) + " si "
                        + "JOIN " + d.table(JdbcSqlDialect.Table.MATERIAL_MASTER)
                        + " mm ON si.material_code = mm.material_code "
                        + "WHERE si.batch_number = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(typeSql)) {
                    pstmt.setString(1, batchNumber);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            oldStatus = rs.getString("qc_status");
                            materialType = rs.getString("material_type");
                            productionOrderId = rs.getObject("production_order_id") != null
                                    ? rs.getInt("production_order_id")
                                    : null;
                            inStockInventory = true;
                        }
                    }
                }

                // Fallback: check production_batch joined with production_order -> material_master
                if (oldStatus == null) {
                    String pbSql = "SELECT pb.qc_status, pb.production_order_id, mm.material_type FROM "
                            + d.table(JdbcSqlDialect.Table.PRODUCTION_BATCH) + " pb "
                            + "JOIN " + d.table(JdbcSqlDialect.Table.PRODUCTION_ORDER)
                            + " po ON pb.production_order_id = po.order_id "
                            + "JOIN " + d.table(JdbcSqlDialect.Table.MATERIAL_MASTER)
                            + " mm ON pb.material_code = mm.material_code "
                            + "WHERE pb.batch_number = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(pbSql)) {
                        pstmt.setString(1, batchNumber);
                        try (ResultSet rs = pstmt.executeQuery()) {
                            if (rs.next()) {
                                oldStatus = rs.getString("qc_status");
                                materialType = rs.getString("material_type");
                                productionOrderId = rs.getObject("production_order_id") != null
                                        ? rs.getInt("production_order_id")
                                        : null;
                            }
                        }
                    }
                }

                if (oldStatus == null) {
                    throw new SQLException("Batch not found in any table: " + batchNumber);
                }

                // Validate: reject if already in a terminal state
                if ("APPROVED".equalsIgnoreCase(oldStatus) || "RELEASED".equalsIgnoreCase(oldStatus)
                        || "REJECTED".equalsIgnoreCase(oldStatus)) {
                    throw new SQLException(
                            "Batch " + batchNumber + " is already in a terminal state (" + oldStatus + ") and cannot be changed.");
                }

                // Validate the requested new status
                if (!"APPROVED".equalsIgnoreCase(newStatus) && !"REJECTED".equalsIgnoreCase(newStatus)) {
                    throw new SQLException("Only APPROVED or REJECTED are valid final QC decisions. Got: " + newStatus);
                }

                // Determine final status and warehouse location
                String finalBatchStatus = newStatus.toUpperCase();
                String targetLocation;
                if ("REJECTED".equalsIgnoreCase(newStatus)) {
                    targetLocation = "REJECTED_AREA";
                } else {
                    // APPROVED path — route to the correct warehouse by material type
                    if ("FINISHED_GOOD".equals(materialType)) {
                        finalBatchStatus = "RELEASED";
                        targetLocation = "FINISHED_GOODS_WAREHOUSE";
                    } else if ("PACKAGING".equals(materialType)) {
                        targetLocation = "PACKAGING_WAREHOUSE";
                    } else if ("RAW_MATERIAL".equals(materialType) || "INTERMEDIATE".equals(materialType)
                            || "EXCIPIENT".equals(materialType)) {
                        targetLocation = "RAW_MATERIAL_WAREHOUSE";
                    } else {
                        // Default fallback for unknown types
                        targetLocation = "FINISHED_GOODS_WAREHOUSE";
                    }
                }

                // Update stock_inventory if it exists there
                if (inStockInventory) {
                    String updateSql = "UPDATE " + d.table(JdbcSqlDialect.Table.STOCK_INVENTORY)
                            + " SET qc_status = ?, location_code = ? WHERE batch_number = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                        pstmt.setString(1, finalBatchStatus);
                        pstmt.setString(2, targetLocation);
                        pstmt.setString(3, batchNumber);
                        pstmt.executeUpdate();
                    }
                }

                // Always sync production_batch (ignore 0-row update if batch not there)
                String updatePbSql = "UPDATE " + d.table(JdbcSqlDialect.Table.PRODUCTION_BATCH)
                        + " SET qc_status = ?, location_code = ? WHERE batch_number = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updatePbSql)) {
                    pstmt.setString(1, finalBatchStatus);
                    pstmt.setString(2, targetLocation);
                    pstmt.setString(3, batchNumber);
                    pstmt.executeUpdate();
                }

                // Sync the parent production order status if linked
                if (productionOrderId != null && productionOrderId > 0) {
                    String poStatus = "REJECTED".equalsIgnoreCase(newStatus) ? "Rejected"
                            : ("FINISHED_GOOD".equals(materialType) ? "Released" : "Approved");
                    String updateOrderSql = "UPDATE " + d.table(JdbcSqlDialect.Table.PRODUCTION_ORDER)
                            + " SET status = ?, updated_at = " + d.nowExpression() + " WHERE order_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(updateOrderSql)) {
                        pstmt.setString(1, poStatus);
                        pstmt.setInt(2, productionOrderId);
                        pstmt.executeUpdate();
                    }
                }

                // Audit event log
                String insertEventSql = "INSERT INTO " + d.table(JdbcSqlDialect.Table.EVENT_LOG)
                        + " (event_type, entity_type, entity_id, details, status) VALUES (?, 'BATCH', ?, ?, 'SUCCESS')";
                try (PreparedStatement evStmt = conn.prepareStatement(insertEventSql)) {
                    evStmt.setString(1, "QC_" + finalBatchStatus.toUpperCase());
                    evStmt.setString(2, batchNumber);
                    evStmt.setString(3, "QC Status updated from " + oldStatus + " to " + finalBatchStatus
                            + ". Location: " + targetLocation);
                    evStmt.executeUpdate();
                }

                databaseService.logAuditTrail(conn, userId, "QC_STATUS_UPDATE", "Stock_Inventory", batchNumber,
                        oldStatus, finalBatchStatus);

                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
