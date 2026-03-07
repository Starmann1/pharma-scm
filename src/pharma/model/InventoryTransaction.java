package pharma.model;

import java.time.LocalDateTime;

public class InventoryTransaction {
    private int transactionId;
    private String materialCode;
    private String batchNumber;
    private String locationCode;
    private String transactionType;
    private double quantity;
    private String referenceType;
    private String referenceId;
    private int performedBy;
    private LocalDateTime transactionTimestamp;
    private String notes;

    public InventoryTransaction() {
    }

    public InventoryTransaction(int transactionId, String materialCode, String batchNumber, String locationCode,
            String transactionType, double quantity, String referenceType, String referenceId,
            int performedBy, LocalDateTime transactionTimestamp, String notes) {
        this.transactionId = transactionId;
        this.materialCode = materialCode;
        this.batchNumber = batchNumber;
        this.locationCode = locationCode;
        this.transactionType = transactionType;
        this.quantity = quantity;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.performedBy = performedBy;
        this.transactionTimestamp = transactionTimestamp;
        this.notes = notes;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public String getMaterialCode() {
        return materialCode;
    }

    public void setMaterialCode(String materialCode) {
        this.materialCode = materialCode;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public String getLocationCode() {
        return locationCode;
    }

    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public int getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(int performedBy) {
        this.performedBy = performedBy;
    }

    public LocalDateTime getTransactionTimestamp() {
        return transactionTimestamp;
    }

    public void setTransactionTimestamp(LocalDateTime transactionTimestamp) {
        this.transactionTimestamp = transactionTimestamp;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
