package pharma.dto;

/**
 * Payload DTO for stock-check agent requests.
 * Used with AgentActions.CHECK_STOCK and AgentActions.LOW_STOCK_ALERT.
 *
 * For LOW_STOCK_ALERT, both fields may be left at their defaults (null / 0)
 * as the agent will scan all materials against their reorder thresholds.
 */
public class StockCheckRequestDTO {

    private String materialCode;
    private double requiredQuantity;

    public StockCheckRequestDTO() {
    }

    public StockCheckRequestDTO(String materialCode, double requiredQuantity) {
        this.materialCode = materialCode;
        this.requiredQuantity = requiredQuantity;
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

    @Override
    public String toString() {
        return "StockCheckRequestDTO{materialCode='" + materialCode + "', requiredQuantity=" + requiredQuantity + '}';
    }
}
