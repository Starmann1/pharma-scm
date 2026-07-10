package pharma.service;

import pharma.model.User;
import pharma.repository.jdbc.UserJdbcRepository;

public class AuthService {

    private final DatabaseService dbService;
    private final UserJdbcRepository userRepo;

    public AuthService(DatabaseService dbService) {
        this.dbService = dbService;
        this.userRepo = dbService.getUserRepository();
    }

    /**
     * Attempts to authenticate a user against the database.
     * 
     * @param username The username provided.
     * @param password The raw password provided.
     * @return User object if successful, null otherwise.
     */
    public User authenticate(String username, String password) {
        return userRepo.authenticateUser(username, password);
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
        return userRepo.getUsernamesByRole(roleName);
    }

    /**
     * Fetches all User objects associated with a given role.
     * 
     * @param roleName The name of the role to filter by
     * @return List of User objects containing basic info (id, username, full name)
     */
    public java.util.List<User> getUsersByRole(String roleName) {
        return userRepo.getUsersByRole(roleName);
    }
}
