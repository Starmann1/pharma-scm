package pharma.repository.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pharma.model.AuditTrail;
import pharma.repository.AuditRepository;
import pharma.service.DatabaseService;

public class AuditJdbcRepository implements AuditRepository {
    private static final Logger logger = LoggerFactory.getLogger(AuditJdbcRepository.class);
    private final DatabaseService databaseService;
    private final JdbcSqlDialect sqlDialect;

    public AuditJdbcRepository(DatabaseService databaseService) {
        this(databaseService, JdbcSqlDialect.from(DatabaseService.getDatabaseConfig()));
    }

    AuditJdbcRepository(DatabaseService databaseService, JdbcSqlDialect sqlDialect) {
        this.databaseService = databaseService;
        this.sqlDialect = sqlDialect;
    }

    @Override
    public void logAgentDecision(int userId, String actionType, String entityType, String entityId, String result)
            throws SQLException, ClassNotFoundException {
        logAuditTrail(userId, actionType, entityType, entityId, null, result);
    }

    public void logAuditTrail(Connection conn, int userId, String actionType, String tableName, String recordId,
            String oldValue, String newValue) throws SQLException {
        String sql = "INSERT INTO " + sqlDialect.table(JdbcSqlDialect.Table.SYSTEM_AUDIT_TRAIL) + " (user_id, action_type, table_name, record_id, old_value, new_value) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (userId <= 0) {
                pstmt.setNull(1, java.sql.Types.INTEGER);
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
            throw e; // Re-throw to ensure transaction rollback if within one
        }
    }

    public void logAuditTrail(int userId, String actionType, String tableName, String recordId, String oldValue,
            String newValue) throws SQLException, ClassNotFoundException {
        try (Connection conn = databaseService.getConnection()) {
            logAuditTrail(conn, userId, actionType, tableName, recordId, oldValue, newValue);
        }
    }

    public List<AuditTrail> getAuditTrail(String actionType, LocalDate startDate, LocalDate endDate)
            throws SQLException, ClassNotFoundException {
        List<AuditTrail> trails = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM " + sqlDialect.table(JdbcSqlDialect.Table.SYSTEM_AUDIT_TRAIL) + " WHERE 1=1");

        if (actionType != null && !actionType.isEmpty()) {
            sql.append(" AND action_type = ?");
        }
        if (startDate != null) {
            // Use a portable date condition or native Postgres/MySQL.
            if (sqlDialect.isPostgresql()) {
                sql.append(" AND DATE(timestamp) >= ?");
            } else {
                sql.append(" AND DATE(timestamp) >= ?");
            }
        }
        if (endDate != null) {
            sql.append(" AND DATE(timestamp) <= ?");
        }
        sql.append(" ORDER BY timestamp DESC LIMIT 1000");

        try (Connection conn = databaseService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            if (actionType != null && !actionType.isEmpty()) {
                pstmt.setString(paramIndex++, actionType);
            }
            if (startDate != null) {
                pstmt.setDate(paramIndex++, java.sql.Date.valueOf(startDate));
            }
            if (endDate != null) {
                pstmt.setDate(paramIndex++, java.sql.Date.valueOf(endDate));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    trails.add(new AuditTrail(
                            rs.getInt("audit_id"),
                            rs.getInt("user_id"),
                            rs.getString("action_type"),
                            rs.getString("table_name"),
                            rs.getString("record_id"),
                            rs.getString("old_value"),
                            rs.getString("new_value"),
                            rs.getTimestamp("timestamp").toLocalDateTime(),
                            rs.getString("ip_address"),
                            rs.getString("notes")));
                }
            }
        }
        return trails;
    }
}
