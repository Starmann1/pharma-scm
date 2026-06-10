package pharma.dto;

import java.time.LocalDate;

public class PODraftDTO {
    private String materialCode;
    private int supplierId;
    private double quantity;
    private double unitPrice;
    private LocalDate expectedDelivery;
    private String triggeredBy;

    public PODraftDTO() {
    }

    public PODraftDTO(String materialCode, int supplierId, double quantity, double unitPrice,
            LocalDate expectedDelivery, String triggeredBy) {
        this.materialCode = materialCode;
        this.supplierId = supplierId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.expectedDelivery = expectedDelivery;
        this.triggeredBy = triggeredBy;
    }

    public String getMaterialCode() {
        return materialCode;
    }

    public void setMaterialCode(String materialCode) {
        this.materialCode = materialCode;
    }

    public int getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(int supplierId) {
        this.supplierId = supplierId;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public LocalDate getExpectedDelivery() {
        return expectedDelivery;
    }

    public void setExpectedDelivery(LocalDate expectedDelivery) {
        this.expectedDelivery = expectedDelivery;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }

    public void setTriggeredBy(String triggeredBy) {
        this.triggeredBy = triggeredBy;
    }
}
