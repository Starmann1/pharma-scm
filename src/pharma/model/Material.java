package pharma.model;

/**
 * Model class representing a Material in the manufacturing system.
 * Replaces the Drug.java class to support both raw materials and finished
 * goods.
 * Materials can be classified as RAW_MATERIAL, EXCIPIENT, PACKAGING, or
 * FINISHED_GOOD.
 */
public class Material {

    // Enum for material classification
    public enum MaterialType {
        RAW_MATERIAL("Raw Material"),
        EXCIPIENT("Excipient"),
        PACKAGING("Packaging"),
        FINISHED_GOOD("Finished Good");

        private final String displayName;

        MaterialType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static MaterialType fromString(String type) {
            for (MaterialType mt : MaterialType.values()) {
                if (mt.name().equalsIgnoreCase(type) || mt.displayName.equalsIgnoreCase(type)) {
                    return mt;
                }
            }
            return FINISHED_GOOD; // Default
        }
    }

    // Enum for unit of measure
    public enum UnitOfMeasure {
        MG("mg"),
        G("g"),
        KG("kg"),
        ML("ml"),
        L("L"),
        UNIT("Unit");

        private final String displayName;

        UnitOfMeasure(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static UnitOfMeasure fromString(String uom) {
            for (UnitOfMeasure u : UnitOfMeasure.values()) {
                if (u.name().equalsIgnoreCase(uom) || u.displayName.equalsIgnoreCase(uom)) {
                    return u;
                }
            }
            return UNIT; // Default
        }
    }

    // Fields corresponding to the Drug_Master table
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
    private MaterialType materialType;
    private UnitOfMeasure unitOfMeasure;

    // Constructors
    public Material() {
    }

    public Material(String materialCode, String brandName, String genericName, String manufacturer,
            String formulation, String strength, String scheduleCategory,
            String storageConditions, Integer reorderLevel, boolean isActive,
            Integer preferredSupplierId, MaterialType materialType, UnitOfMeasure unitOfMeasure) {
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
        this.materialType = materialType;
        this.unitOfMeasure = unitOfMeasure;
    }

    // Getters and Setters
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

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getPreferredSupplierId() {
        return preferredSupplierId;
    }

    public void setPreferredSupplierId(Integer preferredSupplierId) {
        this.preferredSupplierId = preferredSupplierId;
    }

    public MaterialType getMaterialType() {
        return materialType;
    }

    public void setMaterialType(MaterialType materialType) {
        this.materialType = materialType;
    }

    public UnitOfMeasure getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(UnitOfMeasure unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    @Override
    public String toString() {
        return "Material{" +
                "materialCode='" + materialCode + '\'' +
                ", brandName='" + brandName + '\'' +
                ", genericName='" + genericName + '\'' +
                ", materialType=" + materialType +
                ", unitOfMeasure=" + unitOfMeasure +
                ", isActive=" + isActive +
                '}';
    }
}
