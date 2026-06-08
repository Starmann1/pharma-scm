package pharma.repository.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import pharma.model.Material;
import pharma.repository.MaterialRepository;
import pharma.service.DatabaseService;

public class MaterialJdbcRepository implements MaterialRepository {
    private final DatabaseService databaseService;

    public MaterialJdbcRepository(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @Override
    public Optional<Material> findByCode(String materialCode) throws SQLException, ClassNotFoundException {
        String sql = "SELECT material_code, brand_name, generic_name, manufacturer, formulation, strength, "
                + "schedule_category, storage_conditions, reorder_level, is_active, preferred_supplier_id, "
                + "material_type, unit_of_measure FROM Material_Master WHERE material_code = ?";
        try (Connection conn = databaseService.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, materialCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
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
                return Optional.of(material);
            }
        }
    }
}
