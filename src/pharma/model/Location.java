package pharma.model;

// A POJO for the Location_Master table
public class Location {
    
    private String locationCode; // Primary Key
    private String locationName;
    private String description;
    private int capacity;

    // Constructor
    public Location(String locationCode, String locationName, String description, int capacity) {
        this.locationCode = locationCode;
        this.locationName = locationName;
        this.description = description;
        this.capacity = capacity;
    }

    // Default Constructor
    public Location() {}

    // --- Getters and Setters ---

    public String getLocationCode() {
        return locationCode;
    }

    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
}
