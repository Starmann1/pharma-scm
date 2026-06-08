package pharma.service;

import java.sql.SQLException;

import pharma.dto.QAResultDTO;
import pharma.repository.QARepository;

public class QAService {
    private final QARepository qaRepository;

    public QAService(QARepository qaRepository) {
        this.qaRepository = qaRepository;
    }

    public QAResultDTO reviewBatch(String batchNumber) throws SQLException, ClassNotFoundException {
        if (batchNumber == null || batchNumber.isBlank()) {
            throw new IllegalArgumentException("batchNumber is required.");
        }
        return qaRepository.reviewBatch(batchNumber);
    }
}
