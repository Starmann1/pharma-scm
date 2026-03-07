package pharma.service;

import pharma.model.Permission;
import pharma.model.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RoleService {

    private final DatabaseService dbService;

    public RoleService(DatabaseService dbService) {
        this.dbService = dbService;
    }

    public List<Role> getAllRoles() {
        List<Role> roles = new ArrayList<>();
        String sql = "SELECT role_id, role_name, description FROM Role_Master ORDER BY role_name";

        try (Connection conn = dbService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                roles.add(new Role(
                        rs.getInt("role_id"),
                        rs.getString("role_name"),
                        rs.getString("description")));
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error fetching roles: " + e.getMessage());
            e.printStackTrace();
        }
        return roles;
    }

    public List<Permission> getAllPermissions() {
        List<Permission> permissions = new ArrayList<>();
        String sql = "SELECT permission_id, permission_name, module, description FROM Permission_Master ORDER BY module, permission_name";

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
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error fetching permissions: " + e.getMessage());
            e.printStackTrace();
        }
        return permissions;
    }

    public Set<Integer> getPermissionIdsForRole(int roleId) {
        Set<Integer> permIds = new HashSet<>();
        String sql = "SELECT permission_id FROM Role_Permission WHERE role_id = ?";

        try (Connection conn = dbService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, roleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    permIds.add(rs.getInt("permission_id"));
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error fetching role permissions: " + e.getMessage());
            e.printStackTrace();
        }
        return permIds;
    }

    public boolean updateRolePermissions(int roleId, Set<Integer> permissionIds, int adminUserId) {
        String deleteSql = "DELETE FROM Role_Permission WHERE role_id = ?";
        String insertSql = "INSERT INTO Role_Permission (role_id, permission_id) VALUES (?, ?)";

        try (Connection conn = dbService.getConnection()) {
            conn.setAutoCommit(false);

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
                dbService.logAuditTrail(adminUserId, "UPDATE_ROLE", "Role_Master", String.valueOf(roleId), null,
                        "Updated role permissions");
            } catch (Exception e) {
            }

            conn.commit();
            return true;
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error updating role permissions: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean createRole(String roleName, String description, int adminUserId) {
        String sql = "INSERT INTO Role_Master (role_name, description) VALUES (?, ?)";

        try (Connection conn = dbService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, roleName);
            pstmt.setString(2, description);
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int newRoleId = rs.getInt(1);
                    try {
                        dbService.logAuditTrail(adminUserId, "CREATE_ROLE", "Role_Master", String.valueOf(newRoleId),
                                null, "Created role: " + roleName);
                    } catch (Exception e) {
                    }
                }
            }
            return true;
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error creating role: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
