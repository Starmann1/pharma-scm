package pharma.repository.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pharma.model.Permission;
import pharma.model.Role;
import pharma.service.DatabaseService;

public class RolePermissionJdbcRepository {
    private final DatabaseService dbService;
    private final JdbcSqlDialect sqlDialect;

    public RolePermissionJdbcRepository(DatabaseService dbService) {
        this(dbService, JdbcSqlDialect.from(DatabaseService.getDatabaseConfig()));
    }

    RolePermissionJdbcRepository(DatabaseService dbService, JdbcSqlDialect sqlDialect) {
        this.dbService = dbService;
        this.sqlDialect = sqlDialect;
    }

    public Set<String> getPermissionsForRole(int roleId) throws SQLException, ClassNotFoundException {
        Set<String> permissions = new HashSet<>();
        String permSql = "SELECT p.permission_name FROM " + sqlDialect.table(JdbcSqlDialect.Table.ROLE_PERMISSION) + " rp " +
                "JOIN " + sqlDialect.table(JdbcSqlDialect.Table.PERMISSION_MASTER) + " p ON rp.permission_id = p.permission_id " +
                "WHERE rp.role_id = ?";

        try (Connection conn = dbService.getConnection();
             PreparedStatement permStmt = conn.prepareStatement(permSql)) {
            permStmt.setInt(1, roleId);
            try (ResultSet permRs = permStmt.executeQuery()) {
                while (permRs.next()) {
                    permissions.add(permRs.getString("permission_name"));
                }
            }
        }
        return permissions;
    }

    public List<Role> getAllRoles() throws SQLException, ClassNotFoundException {
        List<Role> roles = new ArrayList<>();
        String sql = "SELECT role_id, role_name, description FROM " 
                + sqlDialect.table(JdbcSqlDialect.Table.ROLE_MASTER) + " ORDER BY role_name";

        try (Connection conn = dbService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                roles.add(new Role(
                        rs.getInt("role_id"),
                        rs.getString("role_name"),
                        rs.getString("description")));
            }
        }
        return roles;
    }

    public List<Permission> getAllPermissions() throws SQLException, ClassNotFoundException {
        List<Permission> permissions = new ArrayList<>();
        String sql = "SELECT permission_id, permission_name, module, description FROM "
                + sqlDialect.table(JdbcSqlDialect.Table.PERMISSION_MASTER) + " ORDER BY module, permission_name";

        try (Connection conn = dbService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                permissions.add(new Permission(
                        rs.getInt("permission_id"),
                        rs.getString("permission_name"),
                        rs.getString("module"),
                        rs.getString("description")));
            }
        }
        return permissions;
    }

    public Set<Integer> getPermissionIdsForRole(int roleId) throws SQLException, ClassNotFoundException {
        Set<Integer> permIds = new HashSet<>();
        String sql = "SELECT permission_id FROM " + sqlDialect.table(JdbcSqlDialect.Table.ROLE_PERMISSION) + " WHERE role_id = ?";

        try (Connection conn = dbService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, roleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    permIds.add(rs.getInt("permission_id"));
                }
            }
        }
        return permIds;
    }

    public boolean updateRolePermissions(int roleId, Set<Integer> permissionIds, int adminUserId) throws SQLException, ClassNotFoundException {
        String deleteSql = "DELETE FROM " + sqlDialect.table(JdbcSqlDialect.Table.ROLE_PERMISSION) + " WHERE role_id = ?";
        String insertSql = "INSERT INTO " + sqlDialect.table(JdbcSqlDialect.Table.ROLE_PERMISSION) + " (role_id, permission_id) VALUES (?, ?)";

        try (Connection conn = dbService.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                    deleteStmt.setInt(1, roleId);
                    deleteStmt.executeUpdate();
                }

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    if (permissionIds != null) {
                        for (Integer permId : permissionIds) {
                            insertStmt.setInt(1, roleId);
                            insertStmt.setInt(2, permId);
                            insertStmt.addBatch();
                        }
                        insertStmt.executeBatch();
                    }
                }

                try {
                    dbService.logAuditTrail(conn, adminUserId, "UPDATE_ROLE", sqlDialect.table(JdbcSqlDialect.Table.ROLE_MASTER), String.valueOf(roleId), null,
                            "Updated role permissions");
                } catch (Exception e) {
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public boolean createRole(String roleName, String description, int adminUserId) throws SQLException, ClassNotFoundException {
        String sql = "INSERT INTO " + sqlDialect.table(JdbcSqlDialect.Table.ROLE_MASTER) + " (role_name, description) VALUES (?, ?)";

        try (Connection conn = dbService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, roleName);
            pstmt.setString(2, description);
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int newRoleId = rs.getInt(1);
                    try {
                        dbService.logAuditTrail(conn, adminUserId, "CREATE_ROLE", sqlDialect.table(JdbcSqlDialect.Table.ROLE_MASTER), String.valueOf(newRoleId),
                                null, "Created role: " + roleName);
                    } catch (Exception e) {
                    }
                }
            }
            return true;
        }
    }
}
