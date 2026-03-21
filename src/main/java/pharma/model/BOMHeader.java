package pharma.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Model class representing a Bill of Materials (BOM) header.
 * A BOM defines the recipe/formulation for manufacturing a finished product.
 * Supports versioning to track recipe changes over time.
 */
public class BOMHeader {

    private int bomId;
    private String materialCode; // The finished product material code
    private int versionNumber;
    private boolean isActive;
    private LocalDate effectiveDate;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public BOMHeader() {
    }

    public BOMHeader(int bomId, String materialCode, int versionNumber, boolean isActive,
            LocalDate effectiveDate, String description, LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.bomId = bomId;
        this.materialCode = materialCode;
        this.versionNumber = versionNumber;
        this.isActive = isActive;
        this.effectiveDate = effectiveDate;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
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

    public int getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
        return "BOMHeader{" +
                "bomId=" + bomId +
                ", materialCode='" + materialCode + '\'' +
                ", versionNumber=" + versionNumber +
                ", isActive=" + isActive +
                ", effectiveDate=" + effectiveDate +
                ", description='" + description + '\'' +
                '}';
    }
}
