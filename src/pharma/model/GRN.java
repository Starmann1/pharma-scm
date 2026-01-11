package pharma.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class GRN {
    private int id;
    private int purchaseOrderId; // Link back to the original Purchase Order
    private LocalDateTime receivedDate;
    private String receivedBy;
    private String status; // e.g., "Verified", "Partial", "Completed"

    // Nested class to track specific received batch information
    public static class GRNItem {
        private String materialCode; // Changed from int drugId to match DB schema
        private String batchNumber;
        private int quantityReceived;
        private LocalDate expiryDate;

        public GRNItem(String materialCode, String batchNumber, int quantityReceived, LocalDate expiryDate) {
            this.materialCode = materialCode;
            this.batchNumber = batchNumber;
            this.quantityReceived = quantityReceived;
            this.expiryDate = expiryDate;
        }

        // Getters
        public String getMaterialCode() {
            return materialCode;
        }

        public int getDrugId() {
            return Integer.parseInt(materialCode);
        } // Deprecated, for backward compatibility

        public String getBatchNumber() {
            return batchNumber;
        }

        public int getQuantityReceived() {
            return quantityReceived;
        }

        public LocalDate getExpiryDate() {
            return expiryDate;
        }
    }

    private List<GRNItem> items;
    private String supplierName;

    // Constructor
    public GRN(int id, String supplierName, int purchaseOrderId, LocalDateTime receivedDate, String receivedBy,
            String status, List<GRNItem> items) {
        this.id = id;
        this.supplierName = supplierName;
        this.purchaseOrderId = purchaseOrderId;
        this.receivedDate = receivedDate;
        this.receivedBy = receivedBy;
        this.status = status;
        this.items = items;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public int getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public LocalDateTime getReceivedDate() {
        return receivedDate;
    }

    public String getReceivedBy() {
        return receivedBy;
    }

    public String getStatus() {
        return status;
    }

    public List<GRNItem> getItems() {
        return items;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSupplierName() {
        return this.supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

}
