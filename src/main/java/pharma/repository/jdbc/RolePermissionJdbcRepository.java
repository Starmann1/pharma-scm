package pharma.repository.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

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
}
