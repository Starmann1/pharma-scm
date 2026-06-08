package pharma.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ProductionCapacityDTO {
    private int bomId;
    private String materialCode;
    private double plannedQuantity;
    private LocalDate requestedDate;
    private boolean capacityAvailable;
    private List<String> constraints = new ArrayList<>();

    public int getBomId() {
        return bomId;
    }

    public void setBomId(int bomId) {
        this.bomId = bomId;
    }

    public String getMaterialCode() {
        return materialCode;
    }

    public void setMaterialCode(String materialCode) {
        this.materialCode = materialCode;
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

    public boolean isCapacityAvailable() {
        return capacityAvailable;
    }

    public void setCapacityAvailable(boolean capacityAvailable) {
        this.capacityAvailable = capacityAvailable;
    }

    public List<String> getConstraints() {
        return constraints;
    }

    public void setConstraints(List<String> constraints) {
        this.constraints = constraints;
    }
}
