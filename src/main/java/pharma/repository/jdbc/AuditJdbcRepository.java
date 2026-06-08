package pharma.repository.jdbc;

import java.sql.SQLException;

import pharma.repository.AuditRepository;
import pharma.service.DatabaseService;

public class AuditJdbcRepository implements AuditRepository {
    private final DatabaseService databaseService;

    public AuditJdbcRepository(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @Override
    public void logAgentDecision(int userId, String actionType, String entityType, String entityId, String result)
            throws SQLException, ClassNotFoundException {
        databaseService.logAuditTrail(userId, actionType, entityType, entityId, null, result);
    }
}
