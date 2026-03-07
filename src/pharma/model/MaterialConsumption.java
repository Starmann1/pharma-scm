package pharma.model;

import java.time.LocalDateTime;

public class MaterialConsumption {
    private int consumptionId;
    private int productionOrderId;
    private String materialCode;
    private String batchNumber;
    private double requiredQty;
    private double consumedQty;
    private String uom;
    private LocalDateTime createdAt;

    public MaterialConsumption() {
    }

    public MaterialConsumption(int consumptionId, int productionOrderId, String materialCode, String batchNumber,
            double requiredQty, double consumedQty, String uom, LocalDateTime createdAt) {
        this.consumptionId = consumptionId;
        this.productionOrderId = productionOrderId;
        this.materialCode = materialCode;
        this.batchNumber = batchNumber;
        this.requiredQty = requiredQty;
        this.consumedQty = consumedQty;
        this.uom = uom;
        this.createdAt = createdAt;
    }

    public int getConsumptionId() {
        return consumptionId;
    }

    public void setConsumptionId(int consumptionId) {
        this.consumptionId = consumptionId;
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

    public double getRequiredQty() {
        return requiredQty;
    }

    public void setRequiredQty(double requiredQty) {
        this.requiredQty = requiredQty;
    }

    public double getConsumedQty() {
        return consumedQty;
    }

    public void setConsumedQty(double consumedQty) {
        this.consumedQty = consumedQty;
    }

    public String getUom() {
        return uom;
    }

    public void setUom(String uom) {
        this.uom = uom;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
