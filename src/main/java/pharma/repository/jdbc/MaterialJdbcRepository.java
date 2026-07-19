package pharma.repository.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pharma.model.Material;
import pharma.repository.MaterialRepository;
import pharma.service.DatabaseService;

public class MaterialJdbcRepository implements MaterialRepository {
    private final DatabaseService databaseService;
    private final JdbcSqlDialect sqlDialect;

    public MaterialJdbcRepository(DatabaseService databaseService) {
        this(databaseService, JdbcSqlDialect.from(DatabaseService.getDatabaseConfig()));
    }

    MaterialJdbcRepository(DatabaseService databaseService, JdbcSqlDialect sqlDialect) {
        this.databaseService = databaseService;
        this.sqlDialect = sqlDialect;
    }

    private Material mapResultSetToMaterial(ResultSet rs) throws SQLException {
        Material material = new Material();
        material.setMaterialCode(rs.getString("material_code"));
        material.setBrandName(rs.getString("brand_name"));
        material.setGenericName(rs.getString("generic_name"));
        material.setManufacturer(rs.getString("manufacturer"));
        material.setFormulation(rs.getString("formulation"));
        material.setStrength(rs.getString("strength"));
        material.setScheduleCategory(rs.getString("schedule_category"));
        material.setStorageConditions(rs.getString("storage_conditions"));
        material.setReorderLevel(rs.getInt("reorder_level"));
        material.setActive(rs.getBoolean("is_active"));
        int supplierId = rs.getInt("preferred_supplier_id");
        material.setPreferredSupplierId(rs.wasNull() ? null : supplierId);
        material.setMaterialType(Material.MaterialType.fromString(rs.getString("material_type")));
        material.setUnitOfMeasure(Material.UnitOfMeasure.fromString(rs.getString("unit_of_measure")));
        
        // Retrieve shelf_life_months dynamically with fallback
        try {
            int shelfLife = rs.getInt("shelf_life_months");
            material.setShelfLifeMonths(rs.wasNull() ? 24 : shelfLife);
        } catch (SQLException ex) {
            material.setShelfLifeMonths(24); // Fallback to 24 if column not fetched
        }
        return material;
    }

    @Override
    public Optional<Material> findByCode(String materialCode) throws SQLException, ClassNotFoundException {
        String sql = "SELECT material_code, brand_name, generic_name, manufacturer, formulation, strength, "
                + "schedule_category, storage_conditions, reorder_level, is_active, preferred_supplier_id, "
                + "material_type, unit_of_measure, shelf_life_months FROM "
                + sqlDialect.table(JdbcSqlDialect.Table.MATERIAL_MASTER)
                + " WHERE material_code = ?";
        try (Connection conn = databaseService.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, materialCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapResultSetToMaterial(rs));
            }
        }
    }

    public List<Material> getAllMaterials() throws SQLException, ClassNotFoundException {
        List<Material> materials = new ArrayList<>();
        String sql = "SELECT * FROM " + sqlDialect.table(JdbcSqlDialect.Table.MATERIAL_MASTER) + " ORDER BY material_code";

        try (Connection conn = databaseService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                materials.add(mapResultSetToMaterial(rs));
            }
        }
        return materials;
    }

    public void addMaterial(Material material) throws SQLException, ClassNotFoundException {
        String sql = "INSERT INTO " + sqlDialect.table(JdbcSqlDialect.Table.MATERIAL_MASTER) + " (material_code, brand_name, generic_name, manufacturer, formulation, strength, schedule_category, storage_conditions, reorder_level, is_active, preferred_supplier_id, material_type, unit_of_measure, shelf_life_months) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = databaseService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, material.getMaterialCode());
            pstmt.setString(2, material.getBrandName());
            pstmt.setString(3, material.getGenericName());
            pstmt.setString(4, material.getManufacturer());
            pstmt.setString(5, material.getFormulation());
            pstmt.setString(6, material.getStrength());
            pstmt.setString(7, material.getScheduleCategory());
            pstmt.setString(8, material.getStorageConditions());
            pstmt.setInt(9, material.getReorderLevel());
            pstmt.setBoolean(10, material.isActive());

            if (material.getPreferredSupplierId() != null) {
                pstmt.setInt(11, material.getPreferredSupplierId());
            } else {
                pstmt.setNull(11, java.sql.Types.INTEGER);
            }

            pstmt.setString(12, material.getMaterialType() != null ? material.getMaterialType().name() : null);
            pstmt.setString(13, material.getUnitOfMeasure() != null ? material.getUnitOfMeasure().name() : null);
            pstmt.setInt(14, material.getShelfLifeMonths());

            pstmt.executeUpdate();
        }
    }

    public List<Material> getMaterialsByType(Material.MaterialType type) throws SQLException, ClassNotFoundException {
        List<Material> materials = new ArrayList<>();
        String sql = "SELECT * FROM " + sqlDialect.table(JdbcSqlDialect.Table.MATERIAL_MASTER) + " WHERE material_type = ? ORDER BY material_code";

        try (Connection conn = databaseService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, type.name());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    materials.add(mapResultSetToMaterial(rs));
                }
            }
        }
        return materials;
    }

    public boolean updateMaterial(Material material) throws SQLException, ClassNotFoundException {
        String sql = "UPDATE " + sqlDialect.table(JdbcSqlDialect.Table.MATERIAL_MASTER)
                + " SET brand_name=?, generic_name=?, manufacturer=?, formulation=?, strength=?,"
                + " schedule_category=?, storage_conditions=?, reorder_level=?, is_active=?,"
                + " preferred_supplier_id=?, material_type=?, unit_of_measure=?, shelf_life_months=?"
                + " WHERE material_code=?";
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, material.getBrandName());
            pstmt.setString(2, material.getGenericName());
            pstmt.setString(3, material.getManufacturer());
            pstmt.setString(4, material.getFormulation());
            pstmt.setString(5, material.getStrength());
            pstmt.setString(6, material.getScheduleCategory());
            pstmt.setString(7, material.getStorageConditions());
            pstmt.setInt(8, material.getReorderLevel());
            pstmt.setBoolean(9, material.isActive());
            if (material.getPreferredSupplierId() != null) {
                pstmt.setInt(10, material.getPreferredSupplierId());
            } else {
                pstmt.setNull(10, java.sql.Types.INTEGER);
            }
            pstmt.setString(11, material.getMaterialType() != null ? material.getMaterialType().name() : null);
            pstmt.setString(12, material.getUnitOfMeasure() != null ? material.getUnitOfMeasure().name() : null);
            pstmt.setInt(13, material.getShelfLifeMonths());
            pstmt.setString(14, material.getMaterialCode());
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean deleteMaterial(String materialCode) throws SQLException, ClassNotFoundException {
        String sql = "DELETE FROM " + sqlDialect.table(JdbcSqlDialect.Table.MATERIAL_MASTER)
                + " WHERE material_code = ?";
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, materialCode);
            return pstmt.executeUpdate() > 0;
        }
    }
}
