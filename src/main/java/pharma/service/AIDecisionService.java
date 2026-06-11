package pharma.service;

import java.sql.SQLException;
import java.util.List;

import pharma.dto.AIReasoningResultDTO;
import pharma.repository.AIDecisionRepository;

public class AIDecisionService {
    private final AIDecisionRepository aiDecisionRepository;

    public AIDecisionService(AIDecisionRepository aiDecisionRepository) {
        this.aiDecisionRepository = aiDecisionRepository;
    }

    public void save(AIReasoningResultDTO result, String transactionId)
            throws SQLException, ClassNotFoundException {
        aiDecisionRepository.save(
                transactionId,
                "AI_REASONING",
                result.getPromptSummary(),
                result.getConfidenceScore(),
                String.valueOf(result.getExtractedData()),
                result.isRequiresHumanReview());
    }

    public List<AIReasoningResultDTO> findPendingReview()
            throws SQLException, ClassNotFoundException {
        return aiDecisionRepository.findPendingReview();
    }

    public List<AIReasoningResultDTO> findAll()
            throws SQLException, ClassNotFoundException {
        return aiDecisionRepository.findAll();
    }

    public AIReasoningResultDTO findByTransactionId(String transactionId)
            throws SQLException, ClassNotFoundException {
        return aiDecisionRepository.findByTransactionId(transactionId);
    }

    public void approve(String transactionId, int userId)
            throws SQLException, ClassNotFoundException {
        aiDecisionRepository.updateStatus(transactionId, "APPROVED", userId, null);
    }

    public void reject(String transactionId, int userId, String reason)
            throws SQLException, ClassNotFoundException {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required.");
        }
        aiDecisionRepository.updateStatus(transactionId, "REJECTED", userId, reason);
    }
}
