package pharma.service;

import java.sql.SQLException;

import pharma.repository.AuditRepository;

public class AuditService {
    private final AuditRepository auditRepository;

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    public void logAgentDecision(int userId, String actionType, String entityType, String entityId, String result)
            throws SQLException, ClassNotFoundException {
        auditRepository.logAgentDecision(userId, actionType, entityType, entityId, result);
    }
}
