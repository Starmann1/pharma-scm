package pharma.repository.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pharma.model.User;
import pharma.model.Role;
import pharma.service.DatabaseService;

public class UserJdbcRepository {
    private static final Logger logger = LoggerFactory.getLogger(UserJdbcRepository.class);
    private final DatabaseService dbService;
    private final JdbcSqlDialect sqlDialect;
    private final RolePermissionJdbcRepository roleRepo;

    public UserJdbcRepository(DatabaseService dbService) {
        this(dbService, JdbcSqlDialect.from(DatabaseService.getDatabaseConfig()), new RolePermissionJdbcRepository(dbService));
    }

    UserJdbcRepository(DatabaseService dbService, JdbcSqlDialect sqlDialect, RolePermissionJdbcRepository roleRepo) {
        this.dbService = dbService;
        this.sqlDialect = sqlDialect;
        this.roleRepo = roleRepo;
    }

    public User authenticateUser(String username, String password) {
        String authSql = "SELECT u.user_id, u.username, u.full_name, u.role_id, r.role_name, r.description FROM "
                + sqlDialect.table(JdbcSqlDialect.Table.USER_MASTER) + " u "
                + "JOIN " + sqlDialect.table(JdbcSqlDialect.Table.ROLE_MASTER) + " r ON u.role_id = r.role_id "
                + "WHERE u.username = ? AND u.password_hash = ?";

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
                    java.util.Set<String> permissions = roleRepo.getPermissionsForRole(roleId);

                    try {
                        dbService.logAuditTrail(userId, "LOGIN_ATTEMPT", "User_Master", String.valueOf(userId), null,
                                "Login Success");
                    } catch (Exception e) {}

                    return new User(userId, dbUsername, fullName, role, permissions);
                } else {
                    try {
                        dbService.logAuditTrail(0, "LOGIN_ATTEMPT", "User_Master", username, null,
                                "Invalid credentials for username: " + username);
                    } catch (Exception e) {}
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Authentication Error: {}", e.getMessage(), e);
        }
        return null;
    }

    public User getUserById(int userId) {
        // Implementation for future expansion
        return null;
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT u.user_id, u.username, u.full_name FROM " + sqlDialect.table(JdbcSqlDialect.Table.USER_MASTER) + " u WHERE u.is_active = " + sqlDialect.trueLiteral() + " ORDER BY u.full_name";

        try (Connection conn = dbService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                User user = new User();
                user.setUserId(rs.getInt("user_id"));
                user.setUsername(rs.getString("username"));
                user.setFullName(rs.getString("full_name"));
                users.add(user);
            }
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error fetching all users: {}", e.getMessage(), e);
        }
        return users;
    }

    public List<User> getUsersByRole(String roleName) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT u.user_id, u.username, u.full_name FROM " + sqlDialect.table(JdbcSqlDialect.Table.USER_MASTER) + " u "
                + "JOIN " + sqlDialect.table(JdbcSqlDialect.Table.ROLE_MASTER) + " r ON u.role_id = r.role_id "
                + "WHERE r.role_name = ? AND u.is_active = " + sqlDialect.trueLiteral() + " ORDER BY u.full_name";

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
            logger.error("Error fetching User objects by role: {}", e.getMessage(), e);
        }
        return users;
    }

    public List<String> getUsernamesByRole(String roleName) {
        List<String> usernames = new ArrayList<>();
        String sql = "SELECT u.username FROM " + sqlDialect.table(JdbcSqlDialect.Table.USER_MASTER) + " u "
                + "JOIN " + sqlDialect.table(JdbcSqlDialect.Table.ROLE_MASTER) + " r ON u.role_id = r.role_id "
                + "WHERE r.role_name = ? AND u.is_active = " + sqlDialect.trueLiteral() + " ORDER BY u.username";

        try (Connection conn = dbService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, roleName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    usernames.add(rs.getString("username"));
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error fetching usernames by role: {}", e.getMessage(), e);
        }
        return usernames;
    }
}
