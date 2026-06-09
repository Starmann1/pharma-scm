package pharma.dto;

/**
 * Payload DTO for QA batch-review agent requests.
 * Used with AgentActions.QA_REVIEW.
 *
 * The QAAgent will retrieve the full QA disposition for the given batchNumber.
 */
public class QAReviewRequestDTO {

    private String batchNumber;

    public QAReviewRequestDTO() {
    }

    public QAReviewRequestDTO(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    @Override
    public String toString() {
        return "QAReviewRequestDTO{batchNumber='" + batchNumber + "'}";
    }
}
