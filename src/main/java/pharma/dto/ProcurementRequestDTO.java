package pharma.dto;

public class ProcurementRequestDTO {
    private String materialCode;
    private double shortfallQuantity;
    private String urgencyLevel; // LOW, MEDIUM, HIGH

    public ProcurementRequestDTO() {
    }

    public ProcurementRequestDTO(String materialCode, double shortfallQuantity, String urgencyLevel) {
        this.materialCode = materialCode;
        this.shortfallQuantity = shortfallQuantity;
        this.urgencyLevel = urgencyLevel;
    }

    public String getMaterialCode() {
        return materialCode;
    }

    public void setMaterialCode(String materialCode) {
        this.materialCode = materialCode;
    }

    public double getShortfallQuantity() {
        return shortfallQuantity;
    }

    public void setShortfallQuantity(double shortfallQuantity) {
        this.shortfallQuantity = shortfallQuantity;
    }

    public String getUrgencyLevel() {
        return urgencyLevel;
    }

    public void setUrgencyLevel(String urgencyLevel) {
        this.urgencyLevel = urgencyLevel;
    }
}
