package pharma.model;

import java.util.HashSet;
import java.util.Set;

/**
 * POJO for the User_Master table, tracking logged-in user details.
 */
public class User {

    private int userId;
    private String username;
    private String fullName;
    private Role role; // Uses the Role object instead of a string
    private Set<String> permissions; // Represents all permissions assigned to the role

    // Constructor used by AuthService upon successful login
    public User(int userId, String username, String fullName, Role role, Set<String> permissions) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.permissions = permissions != null ? permissions : new HashSet<>();
    }

    // Default Constructor
    public User() {
        this.permissions = new HashSet<>();
    }

    // Getters
    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    // Setters
    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions != null ? permissions : new HashSet<>();
    }

    /**
     * Helper method to check if user has a specific role
     */
    public boolean hasRole(String roleName) {
        return this.role != null && this.role.getRoleName() != null
                && this.role.getRoleName().equalsIgnoreCase(roleName);
    }

    /**
     * Helper method to check if user has a specific permission
     */
    public boolean hasPermission(String permissionName) {
        return permissions.contains(permissionName);
    }
}
