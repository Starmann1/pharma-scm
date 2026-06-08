package pharma.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ManufacturingFeasibilityDTO {
    private String materialCode;
    private int bomId;
    private double plannedQuantity;
    private LocalDate requestedDate;
    private boolean feasible;
    private List<MaterialAvailabilityDTO> materialAvailability = new ArrayList<>();
    private ProductionCapacityDTO productionCapacity;
    private List<SupplierScoreDTO> supplierOptions = new ArrayList<>();
    private List<String> blockers = new ArrayList<>();

    public String getMaterialCode() {
        return materialCode;
    }

    public void setMaterialCode(String materialCode) {
        this.materialCode = materialCode;
    }

    public int getBomId() {
        return bomId;
    }

    public void setBomId(int bomId) {
        this.bomId = bomId;
    }

    public double getPlannedQuantity() {
        return plannedQuantity;
    }

    public void setPlannedQuantity(double plannedQuantity) {
        this.plannedQuantity = plannedQuantity;
    }

    public LocalDate getRequestedDate() {
        return requestedDate;
    }

    public void setRequestedDate(LocalDate requestedDate) {
        this.requestedDate = requestedDate;
    }

    public boolean isFeasible() {
        return feasible;
    }

    public void setFeasible(boolean feasible) {
        this.feasible = feasible;
    }

    public List<MaterialAvailabilityDTO> getMaterialAvailability() {
        return materialAvailability;
    }

    public void setMaterialAvailability(List<MaterialAvailabilityDTO> materialAvailability) {
        this.materialAvailability = materialAvailability;
    }

    public ProductionCapacityDTO getProductionCapacity() {
        return productionCapacity;
    }

    public void setProductionCapacity(ProductionCapacityDTO productionCapacity) {
        this.productionCapacity = productionCapacity;
    }

    public List<SupplierScoreDTO> getSupplierOptions() {
        return supplierOptions;
    }

    public void setSupplierOptions(List<SupplierScoreDTO> supplierOptions) {
        this.supplierOptions = supplierOptions;
    }

    public List<String> getBlockers() {
        return blockers;
    }

    public void setBlockers(List<String> blockers) {
        this.blockers = blockers;
    }
}
