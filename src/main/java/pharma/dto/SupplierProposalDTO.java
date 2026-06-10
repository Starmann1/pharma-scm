package pharma.dto;

public class SupplierProposalDTO {
    private int supplierId;
    private String supplierName;
    private double quotedPrice;
    private int leadTimeDays;
    private double availableQuantity;
    private double compositeScore;

    public SupplierProposalDTO() {
    }

    public SupplierProposalDTO(int supplierId, String supplierName, double quotedPrice,
            int leadTimeDays, double availableQuantity, double compositeScore) {
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.quotedPrice = quotedPrice;
        this.leadTimeDays = leadTimeDays;
        this.availableQuantity = availableQuantity;
        this.compositeScore = compositeScore;
    }

    public int getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(int supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public double getQuotedPrice() {
        return quotedPrice;
    }

    public void setQuotedPrice(double quotedPrice) {
        this.quotedPrice = quotedPrice;
    }

    public int getLeadTimeDays() {
        return leadTimeDays;
    }

    public void setLeadTimeDays(int leadTimeDays) {
        this.leadTimeDays = leadTimeDays;
    }

    public double getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(double availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public double getCompositeScore() {
        return compositeScore;
    }

    public void setCompositeScore(double compositeScore) {
        this.compositeScore = compositeScore;
    }
}
