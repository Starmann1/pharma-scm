package pharma.repository;

import java.sql.SQLException;

import pharma.dto.QAResultDTO;

public interface QARepository {
    QAResultDTO reviewBatch(String batchNumber) throws SQLException, ClassNotFoundException;
}
