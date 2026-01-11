package pharma.model;

/**
 * POJO for the User_Master table, tracking logged-in user details.
 */
public class User {

    // Role constants for the manufacturing ERP system
    public static final String ROLE_ADMIN = "Admin";
    public static final String ROLE_PHARMACIST = "Pharmacist";
    public static final String ROLE_STAFF = "Staff";
    public static final String ROLE_PRODUCTION_MANAGER = "Production Manager";
    public static final String ROLE_QA_ANALYST = "QA Analyst";
    public static final String ROLE_INVENTORY_HEAD = "Inventory Head";

    private int userId;
    private String username;
    private String role; // e.g., "Admin", "Staff", "Production Manager", "QA Analyst"

    // Constructor used by AuthService upon successful login
    public User(int userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    // Default Constructor
    public User() {

    }

    // Getters
    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    // Setters
    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setRole(String role) {
        this.role = role;
    }

    /**
     * Helper method to check if user has a specific role
     */
    public boolean hasRole(String roleName) {
        return this.role != null && this.role.equalsIgnoreCase(roleName);
    }
}
