package pharma.repository;

import java.sql.SQLException;

public interface AuditRepository {
    void logAgentDecision(int userId, String actionType, String entityType, String entityId, String result)
            throws SQLException, ClassNotFoundException;
}
