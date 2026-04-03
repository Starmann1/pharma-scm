package pharma.model;

import java.time.LocalDate;

public class Stock {
    private int stockId;
    private String materialCode;
    private String brandName;
    private String genericName;
    private String manufacturer;
    private String locationCode;
    private String batchNumber;
    private double quantity;
    private double reservedQuantity;
    private double availableQuantity;
    private double unitCost;
    private LocalDate mfgDate;
    private LocalDate expDate;
    private String qcStatus;
    private String parentBatchId;
    private int productionOrderId;

    public Stock() {
    }

    public int getStockId() {
        return stockId;
    }

    public void setStockId(int stockId) {
        this.stockId = stockId;
    }

    public String getMaterialCode() {
        return materialCode;
    }

    public void setMaterialCode(String materialCode) {
        this.materialCode = materialCode;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getGenericName() {
        return genericName;
    }

    public void setGenericName(String genericName) {
        this.genericName = genericName;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getLocationCode() {
        return locationCode;
    }

    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(double reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public double getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(double availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public double getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(double unitCost) {
        this.unitCost = unitCost;
    }

    public LocalDate getMfgDate() {
        return mfgDate;
    }

    public void setMfgDate(LocalDate mfgDate) {
        this.mfgDate = mfgDate;
    }

    public LocalDate getExpDate() {
        return expDate;
    }

    public void setExpDate(LocalDate expDate) {
        this.expDate = expDate;
    }

    public String getQcStatus() {
        return qcStatus;
    }

    public void setQcStatus(String qcStatus) {
        this.qcStatus = qcStatus;
    }

    public String getParentBatchId() {
        return parentBatchId;
    }

    public void setParentBatchId(String parentBatchId) {
        this.parentBatchId = parentBatchId;
    }

    public int getProductionOrderId() {
        return productionOrderId;
    }

    public void setProductionOrderId(int productionOrderId) {
        this.productionOrderId = productionOrderId;
    }
}
