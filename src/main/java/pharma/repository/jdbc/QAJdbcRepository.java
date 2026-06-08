package pharma.repository.jdbc;

import java.sql.SQLException;

import pharma.dto.QAResultDTO;
import pharma.model.Stock;
import pharma.repository.QARepository;
import pharma.service.DatabaseService;

public class QAJdbcRepository implements QARepository {
    private final DatabaseService databaseService;

    public QAJdbcRepository(DatabaseService databaseService) {
        this.databaseService = databaseService;
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
}
