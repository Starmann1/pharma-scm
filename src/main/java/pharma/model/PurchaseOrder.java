package pharma.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class PurchaseOrder {
    private int id;
    private int supplierId;
    private String supplierName;
    private LocalDate orderDate;
    private LocalDate expectedDate;
    private double totalAmount;
    private String status; // e.g., "Pending", "Shipped", "Received"

    // Nested class to represent items within the Purchase Order
    public static class PurchaseOrderItem {
        private String materialCode;
        private int quantity;
        private double unitPrice;

        public PurchaseOrderItem() {
        }

        public PurchaseOrderItem(String materialCode, int quantity, double unitPrice) {
            this.materialCode = materialCode;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        public String getMaterialCode() {
            return materialCode;
        }

        public int getQuantity() {
            return quantity;
        }

        public double getUnitPrice() {
            return unitPrice;
        }

        public void setMaterialCode(String materialCode) {
            this.materialCode = materialCode;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public void setUnitPrice(double unitPrice) {
            this.unitPrice = unitPrice;
        }
    }

    private List<PurchaseOrderItem> items;

    // Full Constructor
    public PurchaseOrder(int id, int supplierId, String supplierName, LocalDate orderDate, LocalDate expectedDate,
            double totalAmount, String status, List<PurchaseOrderItem> items) {
        this.id = id;
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.orderDate = orderDate;
        this.expectedDate = expectedDate;
        this.totalAmount = totalAmount;
        this.status = status;
        this.items = items;
    }

    // Minimal Constructor (used by CreatePurchaseOrderDialog)
    public PurchaseOrder(int supplierId, String supplierName, LocalDate orderDate, LocalDate expectedDate,
            double totalAmount, String status, List<PurchaseOrderItem> items) {
        this(-1, supplierId, supplierName, orderDate, expectedDate, totalAmount, status, items);
    }

    public PurchaseOrder(int poId, String supplierName2, int supplierId2, LocalDateTime orderDate2, String status2,
            List<Object> of) {
        this.id = poId;
        this.supplierName = supplierName2;
        this.supplierId = supplierId2;
        this.orderDate = orderDate2.toLocalDate();
        this.expectedDate = null; // Default or null if not provided
        this.totalAmount = 0.0; // Default or 0.0 if not provided
        this.status = status2;
        // Assuming 'of' contains PurchaseOrderItem objects, cast them
        this.items = of.stream()
                .filter(item -> item instanceof PurchaseOrderItem)
                .map(item -> (PurchaseOrderItem) item)
                .collect(() -> new java.util.ArrayList<>(), (list, item) -> list.add(item), (list1, list2) -> list1.addAll(list2));
    }

    // Getters
    public int getId() {
        return id;
    }

    public int getSupplierId() {
        return supplierId;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public LocalDate getExpectedDate() {
        return expectedDate;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public List<PurchaseOrderItem> getItems() {
        return items;
    }

    // Default constructor for Jackson
    public PurchaseOrder() {
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setSupplierId(int supplierId) {
        this.supplierId = supplierId;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public void setExpectedDate(LocalDate expectedDate) {
        this.expectedDate = expectedDate;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setItems(List<PurchaseOrderItem> items) {
        this.items = items;
    }

    public String getPurchaseOrderById() {
        return String.valueOf(this.id);
    }

    public String getPoNumber() {
        return String.valueOf(this.id);
    }

    public String getSupplierName() {
        return this.supplierName;
    }
}
