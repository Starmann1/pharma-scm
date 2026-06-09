package pharma.dto;

/**
 * Payload DTO for supplier-check agent requests.
 * Used with AgentActions.CHECK_SUPPLIER.
 *
 * The SupplierAgent will return a ranked list of approved suppliers
 * for the given materialCode, ordered by score descending.
 */
public class SupplierCheckRequestDTO {

    private String materialCode;

    public SupplierCheckRequestDTO() {
    }

    public SupplierCheckRequestDTO(String materialCode) {
        this.materialCode = materialCode;
    }

    public String getMaterialCode() {
        return materialCode;
    }

    public void setMaterialCode(String materialCode) {
        this.materialCode = materialCode;
    }

    @Override
    public String toString() {
        return "SupplierCheckRequestDTO{materialCode='" + materialCode + "'}";
    }
}
