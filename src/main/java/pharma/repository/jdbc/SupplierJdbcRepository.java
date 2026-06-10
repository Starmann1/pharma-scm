package pharma.repository.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import pharma.dto.SupplierScoreDTO;
import pharma.model.Supplier;
import pharma.repository.SupplierRepository;
import pharma.service.DatabaseService;

public class SupplierJdbcRepository implements SupplierRepository {
    private final DatabaseService databaseService;

    public SupplierJdbcRepository(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @Override
    public List<SupplierScoreDTO> rankApprovedSuppliersForMaterial(String materialCode)
            throws SQLException, ClassNotFoundException {
        String sql = "SELECT s.supplier_id, s.supplier_name, s.supplier_status, "
                + "CASE WHEN mm.preferred_supplier_id = s.supplier_id THEN 100 ELSE 60 END AS score "
                + "FROM Supplier_Master s "
                + "LEFT JOIN Material_Master mm ON mm.material_code = ? "
                + "WHERE UPPER(COALESCE(s.supplier_status, 'APPROVED')) = ? "
                + "ORDER BY score DESC, s.supplier_name ASC";
        List<SupplierScoreDTO> suppliers = new ArrayList<>();
        try (Connection conn = databaseService.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, materialCode);
            stmt.setString(2, Supplier.STATUS_APPROVED);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    SupplierScoreDTO dto = new SupplierScoreDTO();
                    dto.setSupplierId(rs.getInt("supplier_id"));
                    dto.setSupplierName(rs.getString("supplier_name"));
                    dto.setSupplierStatus(rs.getString("supplier_status"));
                    dto.setMaterialCode(materialCode);
                    dto.setScore(rs.getDouble("score"));
                    dto.setRationale(dto.getScore() >= 100 ? "Preferred approved supplier" : "Approved supplier");
                    suppliers.add(dto);
                }
            }
        }
        return suppliers;
    }

    @Override
    public double getSupplierCapacity(int supplierId, String materialCode)
            throws SQLException, ClassNotFoundException {
        String sql = "SELECT COALESCE(AVG(pod.quantity), 0) AS avg_capacity "
                + "FROM Purchase_Order_Details pod "
                + "JOIN Purchase_Order po ON po.po_id = pod.po_id "
                + "WHERE po.supplier_id = ? AND pod.material_code = ? "
                + "AND po.po_status NOT IN ('CANCELLED', 'REJECTED')";
        try (Connection conn = databaseService.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, supplierId);
            stmt.setString(2, materialCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double avg = rs.getDouble("avg_capacity");
                    return avg > 0 ? avg * 1.5 : 10000.0;
                }
            }
        }
        return 10000.0;
    }
}
