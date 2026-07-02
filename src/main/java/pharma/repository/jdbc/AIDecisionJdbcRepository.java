package pharma.repository.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import pharma.dto.AIReasoningResultDTO;
import pharma.repository.AIDecisionRepository;
import pharma.service.DatabaseService;

public class AIDecisionJdbcRepository implements AIDecisionRepository {
    private final DatabaseService databaseService;
    private final JdbcSqlDialect sqlDialect;

    public AIDecisionJdbcRepository(DatabaseService databaseService) {
        this(databaseService, JdbcSqlDialect.from(DatabaseService.getDatabaseConfig()));
    }

    AIDecisionJdbcRepository(DatabaseService databaseService, JdbcSqlDialect sqlDialect) {
        this.databaseService = databaseService;
        this.sqlDialect = sqlDialect;
    }

    @Override
    public void save(String transactionId, String taskType, String promptSummary,
                     double confidenceScore, String rawOutput, boolean requiresHumanReview)
            throws SQLException, ClassNotFoundException {
        String sql = "INSERT INTO " + sqlDialect.table(JdbcSqlDialect.Table.AI_DECISIONS)
                + " (transaction_id, task_type, prompt_summary, "
                + "confidence_score, raw_output, requires_human_review) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, transactionId);
            pstmt.setString(2, taskType);
            pstmt.setString(3, promptSummary);
            pstmt.setDouble(4, confidenceScore);
            pstmt.setString(5, rawOutput);
            pstmt.setBoolean(6, requiresHumanReview);
            pstmt.executeUpdate();
        }
    }

    @Override
    public AIReasoningResultDTO findByTransactionId(String transactionId)
            throws SQLException, ClassNotFoundException {
        String sql = "SELECT * FROM " + sqlDialect.table(JdbcSqlDialect.Table.AI_DECISIONS)
                + " WHERE transaction_id = ?";
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, transactionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    @Override
    public List<AIReasoningResultDTO> findPendingReview()
            throws SQLException, ClassNotFoundException {
        String sql = "SELECT * FROM " + sqlDialect.table(JdbcSqlDialect.Table.AI_DECISIONS)
                + " WHERE status = 'PENDING' AND requires_human_review = " + sqlDialect.trueLiteral();
        List<AIReasoningResultDTO> results = new ArrayList<>();
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                results.add(mapResultSet(rs));
            }
        }
        return results;
    }

    @Override
    public List<AIReasoningResultDTO> findAll()
            throws SQLException, ClassNotFoundException {
        String sql = "SELECT * FROM " + sqlDialect.table(JdbcSqlDialect.Table.AI_DECISIONS)
                + " ORDER BY created_at DESC";
        List<AIReasoningResultDTO> results = new ArrayList<>();
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                results.add(mapResultSet(rs));
            }
        }
        return results;
    }

    @Override
    public void updateStatus(String transactionId, String status, int reviewedBy, String reason)
            throws SQLException, ClassNotFoundException {
        String sql = "UPDATE " + sqlDialect.table(JdbcSqlDialect.Table.AI_DECISIONS)
                + " SET status = ?, reviewed_by = ?, review_reason = ?, "
                + "reviewed_at = NOW() WHERE transaction_id = ?";
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, reviewedBy);
            pstmt.setString(3, reason);
            pstmt.setString(4, transactionId);
            pstmt.executeUpdate();
        }
    }

    private AIReasoningResultDTO mapResultSet(ResultSet rs) throws SQLException {
        AIReasoningResultDTO dto = new AIReasoningResultDTO();
        dto.setTransactionId(rs.getString("transaction_id"));
        dto.setTaskType(rs.getString("task_type"));
        dto.setPromptSummary(rs.getString("prompt_summary"));
        dto.setConfidenceScore(rs.getDouble("confidence_score"));
        dto.setExtractedData(rs.getString("raw_output"));
        dto.setRequiresHumanReview(rs.getBoolean("requires_human_review"));
        dto.setStatus(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            dto.setCreatedAt(ts.toLocalDateTime());
        }
        return dto;
    }
}
