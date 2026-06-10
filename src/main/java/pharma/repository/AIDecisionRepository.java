package pharma.repository;

import java.sql.SQLException;
import java.util.List;

import pharma.dto.AIReasoningResultDTO;

public interface AIDecisionRepository {
    void save(String transactionId, String taskType, String promptSummary,
              double confidenceScore, String rawOutput, boolean requiresHumanReview)
            throws SQLException, ClassNotFoundException;

    AIReasoningResultDTO findByTransactionId(String transactionId)
            throws SQLException, ClassNotFoundException;

    List<AIReasoningResultDTO> findPendingReview()
            throws SQLException, ClassNotFoundException;

    void updateStatus(String transactionId, String status, int reviewedBy, String reason)
            throws SQLException, ClassNotFoundException;
}
