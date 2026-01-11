package pharma.model;

// A simple POJO (Plain Old Java Object) to represent a Drug Master record
public class Drug {

    // Corresponds to the 'material_code' in the Drug_Master table.
    private String materialCode;
    private String brandName;
    private String genericName;
    private String manufacturer;
    private String formulation;
    private String strength;
    private String scheduleCategory;
    private String storageConditions;
    private int reorderLevel;
    private boolean isActive;
    private Integer preferredSupplierId;

    // Constructor
    public Drug(String materialCode, String brandName, String genericName, String manufacturer, 
                String formulation, String strength, String scheduleCategory, 
                String storageConditions, Integer reorderLevel, boolean isActive, Integer preferredSupplierId) {
        this.materialCode = materialCode;
        this.brandName = brandName;
        this.genericName = genericName;
        this.manufacturer = manufacturer;
        this.formulation = formulation;
        this.strength = strength;
        this.scheduleCategory = scheduleCategory;
        this.storageConditions = storageConditions;
        this.reorderLevel = reorderLevel;
        this.isActive = isActive;
        this.preferredSupplierId = preferredSupplierId;
    }

    // Default Constructor (required by some frameworks/libraries)
    //public Drug() {}

    // ----------------------------------------------------------
    // Getters and Setters for accessing and modifying the fields
    // ----------------------------------------------------------

    public Drug() {
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

    public String getFormulation() {
        return formulation;
    }

    public void setFormulation(String formulation) {
        this.formulation = formulation;
    }

    public String getStrength() {
        return strength;
    }

    public void setStrength(String strength) {
        this.strength = strength;
    }

    public String getScheduleCategory() {
        return scheduleCategory;
    }

    public void setScheduleCategory(String scheduleCategory) {
        this.scheduleCategory = scheduleCategory;
    }

    public String getStorageConditions() {
        return storageConditions;
    }

    public void setStorageConditions(String storageConditions) {
        this.storageConditions = storageConditions;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(int reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isactive) {
        isActive = isactive;
    }
    public Integer getPreferredSupplierId() {
        return preferredSupplierId;
    }

    public void setPreferredSupplierId(Integer preferredSupplierId) {
        this.preferredSupplierId = preferredSupplierId;
    }

}
