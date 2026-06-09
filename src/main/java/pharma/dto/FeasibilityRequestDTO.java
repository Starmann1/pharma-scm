package pharma.dto;

import java.time.LocalDate;

/**
 * Payload DTO for production feasibility and capacity agent requests.
 * Used with AgentActions.MANUFACTURING_FEASIBILITY and AgentActions.CHECK_CAPACITY.
 *
 * - MANUFACTURING_FEASIBILITY: checks whether all BOM materials are available
 *   in sufficient quantity for the planned production run.
 * - CHECK_CAPACITY: checks whether the production floor has the capacity to
 *   accommodate the run on the requested date.
 */
public class FeasibilityRequestDTO {

    private int bomId;
    private double plannedQuantity;
    private LocalDate requestedDate;

    public FeasibilityRequestDTO() {
    }

    public FeasibilityRequestDTO(int bomId, double plannedQuantity, LocalDate requestedDate) {
        this.bomId = bomId;
        this.plannedQuantity = plannedQuantity;
        this.requestedDate = requestedDate;
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

    @Override
    public String toString() {
        return "FeasibilityRequestDTO{bomId=" + bomId
                + ", plannedQuantity=" + plannedQuantity
                + ", requestedDate=" + requestedDate + '}';
    }
}
