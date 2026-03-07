package pharma.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ProductionBatch {
    private int batchId;
    private int productionOrderId;
    private String materialCode;
    private String batchNumber;
    private double quantity;
    private LocalDate mfgDate;
    private LocalDate expiryDate;
    private String qcStatus;
    private String locationCode;
    private LocalDateTime createdAt;

    public ProductionBatch() {
    }

    public ProductionBatch(int batchId, int productionOrderId, String materialCode, String batchNumber, double quantity,
            LocalDate mfgDate, LocalDate expiryDate, String qcStatus, String locationCode, LocalDateTime createdAt) {
        this.batchId = batchId;
        this.productionOrderId = productionOrderId;
        this.materialCode = materialCode;
        this.batchNumber = batchNumber;
        this.quantity = quantity;
        this.mfgDate = mfgDate;
        this.expiryDate = expiryDate;
        this.qcStatus = qcStatus;
        this.locationCode = locationCode;
        this.createdAt = createdAt;
    }

    public int getBatchId() {
        return batchId;
    }

    public void setBatchId(int batchId) {
        this.batchId = batchId;
    }

    public int getProductionOrderId() {
        return productionOrderId;
    }

    public void setProductionOrderId(int productionOrderId) {
        this.productionOrderId = productionOrderId;
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

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public LocalDate getMfgDate() {
        return mfgDate;
    }

    public void setMfgDate(LocalDate mfgDate) {
        this.mfgDate = mfgDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getQcStatus() {
        return qcStatus;
    }

    public void setQcStatus(String qcStatus) {
        this.qcStatus = qcStatus;
    }

    public String getLocationCode() {
        return locationCode;
    }

    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
