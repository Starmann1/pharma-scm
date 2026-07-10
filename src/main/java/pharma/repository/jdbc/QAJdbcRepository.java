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

    public void updateQCStatus(String batchNumber, String newStatus, int userId)
            throws SQLException, ClassNotFoundException {
        Connection conn = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            String oldStatus = null;
            String materialType = null;
            Integer productionOrderId = null;
            String typeSql = "SELECT si.qc_status, si.production_order_id, mm.material_type FROM " + d.table(JdbcSqlDialect.Table.STOCK_INVENTORY) + " si " +
                    "JOIN " + d.table(JdbcSqlDialect.Table.MATERIAL_MASTER) + " mm ON si.material_code = mm.material_code " +
                    "WHERE si.batch_number = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(typeSql)) {
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

            if (oldStatus == null) {
                throw new SQLException("Batch not found: " + batchNumber);
            }

            if (!"QI".equalsIgnoreCase(oldStatus) && !"UNDER_TEST".equalsIgnoreCase(oldStatus)) {
                throw new SQLException(
                        "Batch " + batchNumber + " must be in QI or UNDER_TEST before it can be " + newStatus + ".");
            }

            if (!"APPROVED".equalsIgnoreCase(newStatus) && !"REJECTED".equalsIgnoreCase(newStatus)) {
                throw new SQLException("Only APPROVED or REJECTED are valid QC decisions.");
            }

            String finalBatchStatus = newStatus;
            String targetLocation = null;
            if ("REJECTED".equals(newStatus)) {
                targetLocation = "REJECTED_AREA";
            } else if ("APPROVED".equals(newStatus)) {
                if ("FINISHED_GOOD".equals(materialType)) {
                    finalBatchStatus = "RELEASED";
                    targetLocation = "FINISHED_GOODS_WAREHOUSE";
                } else if ("PACKAGING".equals(materialType)) {
                    targetLocation = "PACKAGING_WAREHOUSE";
                } else if ("RAW_MATERIAL".equals(materialType) || "INTERMEDIATE".equals(materialType)) {
                    targetLocation = "RAW_MATERIAL_WAREHOUSE";
                }
            }

            if (targetLocation == null) {
                throw new SQLException("Unable to determine released location for material type: " + materialType);
            }

            String updateSql = "UPDATE " + d.table(JdbcSqlDialect.Table.STOCK_INVENTORY) + " SET qc_status = ?, location_code = ? WHERE batch_number = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setString(1, finalBatchStatus);
                pstmt.setString(2, targetLocation);
                pstmt.setString(3, batchNumber);
                pstmt.executeUpdate();
            }

            String updatePbSql = "UPDATE " + d.table(JdbcSqlDialect.Table.PRODUCTION_BATCH) + " SET qc_status = ?, location_code = ? WHERE batch_number = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updatePbSql)) {
                pstmt.setString(1, finalBatchStatus);
                pstmt.setString(2, targetLocation);
                pstmt.setString(3, batchNumber);
                pstmt.executeUpdate();
            }

            if (productionOrderId != null && productionOrderId > 0) {
                String updateOrderSql = "UPDATE " + d.table(JdbcSqlDialect.Table.PRODUCTION_ORDER) + " SET status = ?, updated_at = " + d.nowExpression() + " WHERE order_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateOrderSql)) {
                    pstmt.setString(1, ProductionOrder.ProductionStatus.fromString(newStatus).getDisplayName());
                    pstmt.setInt(2, productionOrderId);
                    pstmt.executeUpdate();
                }
            }

            String insertEventSql = "INSERT INTO " + d.table(JdbcSqlDialect.Table.EVENT_LOG) + " (event_type, entity_type, entity_id, details, status) VALUES (?, 'BATCH', ?, ?, 'SUCCESS')";
            try (PreparedStatement evStmt = conn.prepareStatement(insertEventSql)) {
                evStmt.setString(1, "QC_" + finalBatchStatus.toUpperCase());
                evStmt.setString(2, batchNumber);
                evStmt.setString(3, "QC Status updated from " + oldStatus + " to " + finalBatchStatus);
                evStmt.executeUpdate();
            }

            databaseService.logAuditTrail(conn, userId, "QC_STATUS_UPDATE", "Stock_Inventory", batchNumber, oldStatus, finalBatchStatus);

            conn.commit();

        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
            }
        }
    }

    public void takeSampleForQC(String batchNumber, int userId) throws SQLException, ClassNotFoundException {
        String selectSql = "SELECT qc_status FROM " + d.table(JdbcSqlDialect.Table.STOCK_INVENTORY) + " WHERE batch_number = ?";
        String updateSql = "UPDATE " + d.table(JdbcSqlDialect.Table.STOCK_INVENTORY) + " SET qc_status = ?, location_code = 'QC_HOLD' WHERE batch_number = ?";
        String updatePbSql = "UPDATE " + d.table(JdbcSqlDialect.Table.PRODUCTION_BATCH) + " SET qc_status = ?, location_code = 'QC_HOLD' WHERE batch_number = ?";
        String insertEventSql = "INSERT INTO " + d.table(JdbcSqlDialect.Table.EVENT_LOG) + " (event_type, entity_type, entity_id, details, status) VALUES (?, 'BATCH', ?, ?, 'SUCCESS')";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                String currentStatus = null;
                try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
                    pstmt.setString(1, batchNumber);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            currentStatus = rs.getString("qc_status");
                        }
                    }
                }

                if (currentStatus == null) {
                    throw new SQLException("Batch not found: " + batchNumber);
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
                                    + " must be in IN_PRODUCTION, IN_PROCESS_SAMPLE, or QUARANTINE before sampling/testing can continue.");
                }

                try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                    pstmt.setString(1, nextStatus);
                    pstmt.setString(2, batchNumber);
                    pstmt.executeUpdate();
                }

                try (PreparedStatement pstmt = conn.prepareStatement(updatePbSql)) {
                    pstmt.setString(1, nextStatus);
                    pstmt.setString(2, batchNumber);
                    pstmt.executeUpdate();
                }

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
}
