package pharma.dto;

import java.util.ArrayList;
import java.util.List;

public class MaterialAvailabilityDTO {
    private String materialCode;
    private double requiredQuantity;
    private double availableQuantity;
    private double reservedQuantity;
    private boolean available;
    private boolean belowSafetyStock;
    private List<String> eligibleBatches = new ArrayList<>();

    public MaterialAvailabilityDTO() {
    }

    public MaterialAvailabilityDTO(String materialCode, double requiredQuantity, double availableQuantity,
            double reservedQuantity, boolean belowSafetyStock) {
        this.materialCode = materialCode;
        this.requiredQuantity = requiredQuantity;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = reservedQuantity;
        this.available = availableQuantity >= requiredQuantity;
        this.belowSafetyStock = belowSafetyStock;
    }

    public String getMaterialCode() {
        return materialCode;
    }

    public void setMaterialCode(String materialCode) {
        this.materialCode = materialCode;
    }

    public double getRequiredQuantity() {
        return requiredQuantity;
    }

    public void setRequiredQuantity(double requiredQuantity) {
        this.requiredQuantity = requiredQuantity;
    }

    public double getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(double availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public double getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(double reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isBelowSafetyStock() {
        return belowSafetyStock;
    }

    public void setBelowSafetyStock(boolean belowSafetyStock) {
        this.belowSafetyStock = belowSafetyStock;
    }

    public List<String> getEligibleBatches() {
        return eligibleBatches;
    }

    public void setEligibleBatches(List<String> eligibleBatches) {
        this.eligibleBatches = eligibleBatches;
    }
}
