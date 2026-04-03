package pharma.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Model class representing a Production Order.
 * Tracks the manufacturing of finished goods from raw materials.
 */
public class ProductionOrder {

    private int orderId;
    private String batchNumber; // Unique batch identifier
    private int bomId; // Foreign key to BOM_Header
    private double plannedQty;
    private Double actualQty; // Nullable - set after production
    private ProductionStatus status;
    private LocalDate productionDate;
    private LocalDate completedDate; // Nullable
    private int createdBy; // User ID who created the order
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Enum representing the status of a production order
     */
    public enum ProductionStatus {
        PLANNED("Planned"),
        IN_PRODUCTION("In-Production"),
        QUALITY_TESTING("Quality-Testing"),
        APPROVED("Approved"),
        REJECTED("Rejected");

        private final String displayName;

        ProductionStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static ProductionStatus fromString(String status) {
            for (ProductionStatus ps : ProductionStatus.values()) {
                if (ps.displayName.equalsIgnoreCase(status) || ps.name().equalsIgnoreCase(status)) {
                    return ps;
                }
            }
            return PLANNED; // Default
        }
    }

    // Constructors
    public ProductionOrder() {
    }

    public ProductionOrder(int orderId, String batchNumber, int bomId, double plannedQty,
            Double actualQty, ProductionStatus status, LocalDate productionDate,
            LocalDate completedDate, int createdBy, String notes,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.orderId = orderId;
        this.batchNumber = batchNumber;
        this.bomId = bomId;
        this.plannedQty = plannedQty;
        this.actualQty = actualQty;
        this.status = status;
        this.productionDate = productionDate;
        this.completedDate = completedDate;
        this.createdBy = createdBy;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public int getBomId() {
        return bomId;
    }

    public void setBomId(int bomId) {
        this.bomId = bomId;
    }

    public double getPlannedQty() {
        return plannedQty;
    }

    public void setPlannedQty(double plannedQty) {
        this.plannedQty = plannedQty;
    }

    public Double getActualQty() {
        return actualQty;
    }

    public void setActualQty(Double actualQty) {
        this.actualQty = actualQty;
    }

    public ProductionStatus getStatus() {
        return status;
    }

    public void setStatus(ProductionStatus status) {
        this.status = status;
    }

    public LocalDate getProductionDate() {
        return productionDate;
    }

    public void setProductionDate(LocalDate productionDate) {
        this.productionDate = productionDate;
    }

    public LocalDate getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(LocalDate completedDate) {
        this.completedDate = completedDate;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "ProductionOrder{" +
                "orderId=" + orderId +
                ", batchNumber='" + batchNumber + '\'' +
                ", bomId=" + bomId +
                ", plannedQty=" + plannedQty +
                ", actualQty=" + actualQty +
                ", status=" + status +
                ", productionDate=" + productionDate +
                ", completedDate=" + completedDate +
                ", createdBy=" + createdBy +
                '}';
    }
}
