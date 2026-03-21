package pharma.model;

import java.time.LocalDateTime;

/**
 * Model class representing an audit trail entry.
 * Provides immutable logging of all critical system operations for compliance.
 */
public class AuditTrail {

    private int auditId;
    private int userId;
    private String actionType; // e.g., QC_STATUS_UPDATE, STOCK_ADJUSTMENT, PRODUCTION_RUN
    private String tableName;
    private String recordId;
    private String oldValue;
    private String newValue;
    private LocalDateTime timestamp;
    private String ipAddress;
    private String notes;

    // Constructors
    public AuditTrail() {
    }

    public AuditTrail(int auditId, int userId, String actionType, String tableName,
            String recordId, String oldValue, String newValue,
            LocalDateTime timestamp, String ipAddress, String notes) {
        this.auditId = auditId;
        this.userId = userId;
        this.actionType = actionType;
        this.tableName = tableName;
        this.recordId = recordId;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.timestamp = timestamp;
        this.ipAddress = ipAddress;
        this.notes = notes;
    }

    // Getters and Setters
    public int getAuditId() {
        return auditId;
    }

    public void setAuditId(int auditId) {
        this.auditId = auditId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "AuditTrail{" +
                "auditId=" + auditId +
                ", userId=" + userId +
                ", actionType='" + actionType + '\'' +
                ", tableName='" + tableName + '\'' +
                ", recordId='" + recordId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
