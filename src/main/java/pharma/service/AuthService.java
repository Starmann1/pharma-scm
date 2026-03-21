package pharma.service;

import pharma.model.Role;
import pharma.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

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
        String authSql = "SELECT u.user_id, u.username, u.full_name, u.role_id, r.role_name, r.description FROM User_Master u "
                +
                "JOIN Role_Master r ON u.role_id = r.role_id " +
                "WHERE u.username = ? AND u.password_hash = ?";

        String permSql = "SELECT p.permission_name FROM Role_Permission rp " +
                "JOIN Permission_Master p ON rp.permission_id = p.permission_id " +
                "WHERE rp.role_id = ?";

        try (Connection conn = dbService.getConnection();
                PreparedStatement authStmt = conn.prepareStatement(authSql)) {

            authStmt.setString(1, username);
            authStmt.setString(2, password);

            try (ResultSet rs = authStmt.executeQuery()) {
                if (rs.next()) {
                    int userId = rs.getInt("user_id");
                    String dbUsername = rs.getString("username");
                    String fullName = rs.getString("full_name");
                    int roleId = rs.getInt("role_id");
                    String roleName = rs.getString("role_name");
                    String roleDesc = rs.getString("description");

                    Role role = new Role(roleId, roleName, roleDesc);
                    Set<String> permissions = new HashSet<>();

                    // Fetch permissions for this role
                    try (PreparedStatement permStmt = conn.prepareStatement(permSql)) {
                        permStmt.setInt(1, roleId);
                        try (ResultSet permRs = permStmt.executeQuery()) {
                            while (permRs.next()) {
                                permissions.add(permRs.getString("permission_name"));
                            }
                        }
                    }

                    try {
                        dbService.logAuditTrail(userId, "LOGIN_ATTEMPT", "User_Master", String.valueOf(userId), null,
                                "Login Success");
                    } catch (Exception e) {
                    }

                    return new User(userId, dbUsername, fullName, role, permissions);
                } else {
                    try {
                        dbService.logAuditTrail(0, "LOGIN_ATTEMPT", "User_Master", username, null,
                                "Invalid credentials for username: " + username);
                    } catch (Exception e) {
                    }
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Authentication Error: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Checks if a user has permission to perform a specific action.
     * 
     * @param user   The user to check permissions for
     * @param action The action to check (e.g., "UPDATE_QC_STATUS")
     * @return true if user has permission, false otherwise
     */
    public boolean hasPermission(User user, String action) {
        if (user == null || action == null) {
            return false;
        }
        boolean granted = user.hasPermission(action);

        if (!granted) {
            try {
                dbService.logAuditTrail(user.getUserId(), "UNAUTHORIZED_ACCESS", "System", action, null,
                        "Access Denied");
            } catch (Exception e) {
            }
        }

        return granted;
    }

    /**
     * Checks if a user has any of the specified roles.
     * 
     * @param user  The user to check
     * @param roles Array of role names to check against
     * @return true if user has any of the specified roles
     */
    public boolean hasAnyRole(User user, String... roles) {
        if (user == null || user.getRole() == null || user.getRole().getRoleName() == null) {
            return false;
        }

        for (String role : roles) {
            if (user.getRole().getRoleName().equalsIgnoreCase(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Fetches all usernames associated with a given role.
     * 
     * @param roleName The name of the role to filter by
     * @return List of usernames
     */
    public java.util.List<String> getUsernamesByRole(String roleName) {
        java.util.List<String> usernames = new java.util.ArrayList<>();
        String sql = "SELECT u.username FROM User_Master u JOIN Role_Master r ON u.role_id = r.role_id WHERE r.role_name = ? AND u.is_active = TRUE ORDER BY u.username";

        try (Connection conn = dbService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, roleName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    usernames.add(rs.getString("username"));
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error fetching users by role: " + e.getMessage());
            e.printStackTrace();
        }
        return usernames;
    }

    /**
     * Fetches all User objects associated with a given role.
     * 
     * @param roleName The name of the role to filter by
     * @return List of User objects containing basic info (id, username, full name)
     */
    public java.util.List<User> getUsersByRole(String roleName) {
        java.util.List<User> users = new java.util.ArrayList<>();
        String sql = "SELECT u.user_id, u.username, u.full_name FROM User_Master u JOIN Role_Master r ON u.role_id = r.role_id WHERE r.role_name = ? AND u.is_active = TRUE ORDER BY u.full_name";

        try (Connection conn = dbService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, roleName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    User user = new User();
                    user.setUserId(rs.getInt("user_id"));
                    user.setUsername(rs.getString("username"));
                    user.setFullName(rs.getString("full_name"));
                    users.add(user);
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error fetching User objects by role: " + e.getMessage());
            e.printStackTrace();
        }
        return users;
    }
}
