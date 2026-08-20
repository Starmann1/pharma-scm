package pharma.service;

import pharma.model.Permission;
import pharma.model.Role;
import pharma.repository.jdbc.RolePermissionJdbcRepository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoleService {

    private static final Logger logger = LoggerFactory.getLogger(RoleService.class);
    private final RolePermissionJdbcRepository roleRepo;

    public RoleService(DatabaseService dbService) {
        this.roleRepo = dbService.getRoleRepository();
    }

    public List<Role> getAllRoles() {
        try {
            return roleRepo.getAllRoles();
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error fetching roles: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<Permission> getAllPermissions() {
        try {
            return roleRepo.getAllPermissions();
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error fetching permissions: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public Set<Integer> getPermissionIdsForRole(int roleId) {
        try {
            return roleRepo.getPermissionIdsForRole(roleId);
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error fetching role permissions for role ID {}: {}", roleId, e.getMessage(), e);
            return new HashSet<>();
        }
    }

    public boolean updateRolePermissions(int roleId, Set<Integer> permissionIds, int adminUserId) {
        try {
            return roleRepo.updateRolePermissions(roleId, permissionIds, adminUserId);
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error updating role permissions for role ID {}: {}", roleId, e.getMessage(), e);
            return false;
        }
    }

    public boolean createRole(String roleName, String description, int adminUserId) {
        try {
            return roleRepo.createRole(roleName, description, adminUserId);
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error creating role {}: {}", roleName, e.getMessage(), e);
            return false;
        }
    }
}
