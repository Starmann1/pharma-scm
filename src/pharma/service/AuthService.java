package pharma.service;

import pharma.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthService {

    private final DatabaseService dbService;

    public AuthService(DatabaseService dbService) {
        this.dbService = dbService;
    }

    /**
     * Attempts to authenticate a user against the database.
     * 
     * @param username The username provided.
     * @param password The raw password provided.
     * @return User object if successful, null otherwise.
     */
    public User authenticate(String username, String password) {
        String sql = "SELECT user_id, username, role FROM User_Master WHERE username = ? AND password_hash = ?";

        // NOTE: In a real application, password hashing (like bcrypt) should be used.
        // For this basic JDBC demo, we assume the database stores passwords in
        // plaintext
        // or a simple hash/function that matches the input password directly.
        // For security, never store passwords in plaintext!

        try (Connection conn = dbService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password); // Assuming password is checked directly/via a simple hash match

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Successful authentication - fetch user details
                    return new User(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("role"));
                }
            }
        }

        catch (SQLException | ClassNotFoundException e) {
            System.err.println("Authentication Error: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Checks if a user has permission to perform a specific action.
     * Used for role-based access control in manufacturing workflows.
     * 
     * @param user   The user to check permissions for
     * @param action The action to check (e.g., "UPDATE_QC_STATUS",
     *               "CREATE_PRODUCTION_ORDER")
     * @return true if user has permission, false otherwise
     */
    public boolean hasPermission(User user, String action) {
        if (user == null || user.getRole() == null) {
            return false;
        }

        String role = user.getRole();

        switch (action) {
            case "UPDATE_QC_STATUS":
                // Only QA Analysts can update QC status
                return role.equals(User.ROLE_QA_ANALYST) || role.equals(User.ROLE_ADMIN);

            case "CREATE_PRODUCTION_ORDER":
                // Production Managers and Admins can create production orders
                return role.equals(User.ROLE_PRODUCTION_MANAGER) || role.equals(User.ROLE_ADMIN);

            case "EXECUTE_PRODUCTION_RUN":
                // Production Managers and Admins can execute production runs
                return role.equals(User.ROLE_PRODUCTION_MANAGER) || role.equals(User.ROLE_ADMIN);

            case "MANAGE_INVENTORY":
                // Inventory Heads and Admins can manage inventory
                return role.equals(User.ROLE_INVENTORY_HEAD) || role.equals(User.ROLE_ADMIN);

            case "MANAGE_BOM":
                // Production Managers and Admins can manage BOMs
                return role.equals(User.ROLE_PRODUCTION_MANAGER) || role.equals(User.ROLE_ADMIN);

            case "VIEW_AUDIT_TRAIL":
                // Admins and QA Analysts can view audit trails
                return role.equals(User.ROLE_ADMIN) || role.equals(User.ROLE_QA_ANALYST);

            case "MANAGE_USERS":
                // Only Admins can manage users
                return role.equals(User.ROLE_ADMIN);

            default:
                // Unknown action - deny by default
                return false;
        }
    }

    /**
     * Checks if a user has any of the specified roles.
     * 
     * @param user  The user to check
     * @param roles Array of role names to check against
     * @return true if user has any of the specified roles
     */
    public boolean hasAnyRole(User user, String... roles) {
        if (user == null || user.getRole() == null) {
            return false;
        }

        for (String role : roles) {
            if (user.getRole().equals(role)) {
                return true;
            }
        }
        return false;
    }
}
