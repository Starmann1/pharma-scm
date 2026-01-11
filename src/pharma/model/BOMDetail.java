package pharma.model;

/**
 * Model class representing a Bill of Materials (BOM) detail/ingredient.
 * Each BOM detail specifies one ingredient required for manufacturing.
 */
public class BOMDetail {

    private int bomDetailId;
    private int bomId; // Foreign key to BOM_Header
    private String ingredientMaterialCode; // The ingredient/raw material
    private double requiredQty;
    private String uom; // Unit of measure
    private int sequenceNumber; // Order of addition in manufacturing
    private String notes;

    // Constructors
    public BOMDetail() {
    }

    public BOMDetail(int bomDetailId, int bomId, String ingredientMaterialCode,
            double requiredQty, String uom, int sequenceNumber, String notes) {
        this.bomDetailId = bomDetailId;
        this.bomId = bomId;
        this.ingredientMaterialCode = ingredientMaterialCode;
        this.requiredQty = requiredQty;
        this.uom = uom;
        this.sequenceNumber = sequenceNumber;
        this.notes = notes;
    }

    // Getters and Setters
    public int getBomDetailId() {
        return bomDetailId;
    }

    public void setBomDetailId(int bomDetailId) {
        this.bomDetailId = bomDetailId;
    }

    public int getBomId() {
        return bomId;
    }

    public void setBomId(int bomId) {
        this.bomId = bomId;
    }

    public String getIngredientMaterialCode() {
        return ingredientMaterialCode;
    }

    public void setIngredientMaterialCode(String ingredientMaterialCode) {
        this.ingredientMaterialCode = ingredientMaterialCode;
    }

    public double getRequiredQty() {
        return requiredQty;
    }

    public void setRequiredQty(double requiredQty) {
        this.requiredQty = requiredQty;
    }

    public String getUom() {
        return uom;
    }

    public void setUom(String uom) {
        this.uom = uom;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "BOMDetail{" +
                "bomDetailId=" + bomDetailId +
                ", bomId=" + bomId +
                ", ingredientMaterialCode='" + ingredientMaterialCode + '\'' +
                ", requiredQty=" + requiredQty +
                ", uom='" + uom + '\'' +
                ", sequenceNumber=" + sequenceNumber +
                '}';
    }
}
