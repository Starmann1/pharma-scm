package pharma.repository.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import pharma.dto.SupplierScoreDTO;
import pharma.model.Supplier;
import pharma.repository.SupplierRepository;
import pharma.service.DatabaseService;

public class SupplierJdbcRepository implements SupplierRepository {
    private final DatabaseService databaseService;
    private final JdbcSqlDialect sqlDialect;

    public SupplierJdbcRepository(DatabaseService databaseService) {
        this(databaseService, JdbcSqlDialect.from(DatabaseService.getDatabaseConfig()));
    }

    SupplierJdbcRepository(DatabaseService databaseService, JdbcSqlDialect sqlDialect) {
        this.databaseService = databaseService;
        this.sqlDialect = sqlDialect;
    }

    // =========================================================================
    // READ
    // =========================================================================

    @Override
    public List<Supplier> getAllSuppliers() throws SQLException, ClassNotFoundException {
        List<Supplier> suppliers = new ArrayList<>();
        String sql = "SELECT * FROM " + sqlDialect.table(JdbcSqlDialect.Table.SUPPLIER_MASTER)
                + " ORDER BY supplier_name";
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                suppliers.add(mapResultSetToSupplier(rs));
            }
        }
        return suppliers;
    }

    public Supplier getSupplierById(int supplierId) throws SQLException, ClassNotFoundException {
        String sql = "SELECT * FROM " + sqlDialect.table(JdbcSqlDialect.Table.SUPPLIER_MASTER)
                + " WHERE supplier_id = ?";
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, supplierId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapResultSetToSupplier(rs);
            }
        }
        return null;
    }

    public int getSupplierIdByName(String supplierName) throws SQLException, ClassNotFoundException {
        String sql = "SELECT supplier_id FROM " + sqlDialect.table(JdbcSqlDialect.Table.SUPPLIER_MASTER)
                + " WHERE supplier_name = ?";
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, supplierName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt("supplier_id");
            }
        }
        return -1;
    }

    public List<String> getSupplierNames() throws SQLException, ClassNotFoundException {
        List<String> names = new ArrayList<>();
        String sql = "SELECT supplier_name FROM " + sqlDialect.table(JdbcSqlDialect.Table.SUPPLIER_MASTER)
                + " ORDER BY supplier_name";
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) names.add(rs.getString("supplier_name"));
        }
        return names;
    }

    public boolean isSupplierApproved(int supplierId) throws SQLException, ClassNotFoundException {
        String sql = "SELECT supplier_status FROM " + sqlDialect.table(JdbcSqlDialect.Table.SUPPLIER_MASTER)
                + " WHERE supplier_id = ?";
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, supplierId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) throw new SQLException("Supplier not found: " + supplierId);
                return Supplier.STATUS_APPROVED.equalsIgnoreCase(normalizeStatus(rs.getString("supplier_status")));
            }
        }
    }

    // =========================================================================
    // WRITE
    // =========================================================================

    @Override
    public int addSupplier(Supplier supplier) throws SQLException, ClassNotFoundException {
        String sql = "INSERT INTO " + sqlDialect.table(JdbcSqlDialect.Table.SUPPLIER_MASTER)
                + " (supplier_name, contact_person, address, email, phone_number, gstin,"
                + " drug_license_number, payment_terms) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, supplier.getSupplierName());
            pstmt.setString(2, supplier.getContactPerson());
            pstmt.setString(3, supplier.getAddress());
            pstmt.setString(4, supplier.getEmail());
            pstmt.setString(5, supplier.getPhoneNumber());
            pstmt.setString(6, supplier.getGstin());
            pstmt.setString(7, supplier.getDrugLicenseNumber());
            pstmt.setString(8, supplier.getPaymentTerms());
            pstmt.executeUpdate();
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int newId = generatedKeys.getInt(1);
                    supplier.setSupplierId(newId);
                    supplier.setSupplierStatus(Supplier.STATUS_PENDING);
                    return newId;
                }
            }
        }
        return -1;
    }

    public boolean updateSupplier(Supplier supplier) throws SQLException, ClassNotFoundException {
        String sql = "UPDATE " + sqlDialect.table(JdbcSqlDialect.Table.SUPPLIER_MASTER)
                + " SET supplier_name=?, contact_person=?, address=?, email=?, phone_number=?,"
                + " gstin=?, drug_license_number=?, payment_terms=? WHERE supplier_id=?";
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, supplier.getSupplierName());
            pstmt.setString(2, supplier.getContactPerson());
            pstmt.setString(3, supplier.getAddress());
            pstmt.setString(4, supplier.getEmail());
            pstmt.setString(5, supplier.getPhoneNumber());
            pstmt.setString(6, supplier.getGstin());
            pstmt.setString(7, supplier.getDrugLicenseNumber());
            pstmt.setString(8, supplier.getPaymentTerms());
            pstmt.setInt(9, supplier.getSupplierId());
            return pstmt.executeUpdate() > 0;
        }
    }

    /** Deletion blocked by the supplier approval workflow — always returns false. */
    public boolean deleteSupplier(int supplierId) {
        return false;
    }

    public boolean approveSupplier(int supplierId, String remarks, String performedBy)
            throws SQLException, ClassNotFoundException {
        String selectSql = "SELECT supplier_name, gstin, drug_license_number, supplier_status FROM "
                + sqlDialect.table(JdbcSqlDialect.Table.SUPPLIER_MASTER) + " WHERE supplier_id = ?";
        String updateSql = "UPDATE " + sqlDialect.table(JdbcSqlDialect.Table.SUPPLIER_MASTER)
                + " SET supplier_status = 'APPROVED', approved_at = CURRENT_TIMESTAMP,"
                + " rejected_at = NULL, remarks = ? WHERE supplier_id = ?";
        try (Connection conn = databaseService.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Supplier supplier = null;
                try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
                    pstmt.setInt(1, supplierId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            supplier = new Supplier();
                            supplier.setSupplierId(supplierId);
                            supplier.setSupplierName(rs.getString("supplier_name"));
                            supplier.setGstin(rs.getString("gstin"));
                            supplier.setDrugLicenseNumber(rs.getString("drug_license_number"));
                            supplier.setSupplierStatus(normalizeStatus(rs.getString("supplier_status")));
                        }
                    }
                }
                if (supplier == null) throw new SQLException("Supplier not found.");
                if (Supplier.STATUS_REJECTED.equalsIgnoreCase(supplier.getSupplierStatus()))
                    throw new SQLException("Rejected suppliers cannot be approved again.");
                if (Supplier.STATUS_APPROVED.equalsIgnoreCase(supplier.getSupplierStatus()))
                    throw new SQLException("Supplier is already approved.");
                if (isBlank(supplier.getDrugLicenseNumber()))
                    throw new SQLException("License number required for approval.");
                if (isBlank(supplier.getGstin()))
                    throw new SQLException("GSTIN required for approval.");

                try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                    pstmt.setString(1, normalizeRemarks(remarks));
                    pstmt.setInt(2, supplierId);
                    pstmt.executeUpdate();
                }
                insertSupplierAuditLog(conn, supplierId, "APPROVED", remarks, performedBy);
                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public boolean rejectSupplier(int supplierId, String remarks, String performedBy)
            throws SQLException, ClassNotFoundException {
        String selectSql = "SELECT supplier_status FROM "
                + sqlDialect.table(JdbcSqlDialect.Table.SUPPLIER_MASTER) + " WHERE supplier_id = ?";
        String updateSql = "UPDATE " + sqlDialect.table(JdbcSqlDialect.Table.SUPPLIER_MASTER)
                + " SET supplier_status = 'REJECTED', rejected_at = CURRENT_TIMESTAMP,"
                + " remarks = ? WHERE supplier_id = ?";
        try (Connection conn = databaseService.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String oldStatus = null;
                try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
                    pstmt.setInt(1, supplierId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) oldStatus = normalizeStatus(rs.getString("supplier_status"));
                    }
                }
                if (oldStatus == null) throw new SQLException("Supplier not found.");
                if (Supplier.STATUS_REJECTED.equalsIgnoreCase(oldStatus))
                    throw new SQLException("Supplier is already rejected.");

                try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                    pstmt.setString(1, normalizeRemarks(remarks));
                    pstmt.setInt(2, supplierId);
                    pstmt.executeUpdate();
                }
                insertSupplierAuditLog(conn, supplierId, "REJECTED", remarks, performedBy);
                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // =========================================================================
    // AGENT-FACING (SupplierRepository interface)
    // =========================================================================

    @Override
    public List<SupplierScoreDTO> rankApprovedSuppliersForMaterial(String materialCode)
            throws SQLException, ClassNotFoundException {
        String sql = "SELECT s.supplier_id, s.supplier_name, s.supplier_status, "
                + "CASE WHEN mm.preferred_supplier_id = s.supplier_id THEN 100 ELSE 60 END AS score "
                + "FROM " + sqlDialect.table(JdbcSqlDialect.Table.SUPPLIER_MASTER) + " s "
                + "LEFT JOIN " + sqlDialect.table(JdbcSqlDialect.Table.MATERIAL_MASTER)
                + " mm ON mm.material_code = ? "
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
        String sql = "SELECT COALESCE(AVG(poi.quantity), 0) AS avg_capacity "
                + "FROM " + sqlDialect.table(JdbcSqlDialect.Table.PURCHASE_ORDER_ITEM) + " poi "
                + "JOIN " + sqlDialect.table(JdbcSqlDialect.Table.PURCHASE_ORDER)
                + " po ON po.po_id = poi.po_id "
                + "WHERE po.supplier_id = ? AND poi.material_code = ? "
                + "AND po.status NOT IN ('CANCELLED', 'REJECTED')";
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

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private void insertSupplierAuditLog(Connection conn, int supplierId, String action,
            String remarks, String performedBy) throws SQLException {
        String sql = "INSERT INTO supplier_audit_log (supplier_id, action, remarks, performed_by)"
                + " VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, supplierId);
            pstmt.setString(2, action);
            pstmt.setString(3, normalizeRemarks(remarks));
            pstmt.setString(4, isBlank(performedBy) ? "system" : performedBy);
            pstmt.executeUpdate();
        }
    }

    private Supplier mapResultSetToSupplier(ResultSet rs) throws SQLException {
        Supplier supplier = new Supplier();
        supplier.setSupplierId(rs.getInt("supplier_id"));
        supplier.setSupplierName(rs.getString("supplier_name"));
        supplier.setContactPerson(rs.getString("contact_person"));
        supplier.setAddress(rs.getString("address"));
        supplier.setEmail(rs.getString("email"));
        supplier.setPhoneNumber(rs.getString("phone_number"));
        supplier.setGstin(rs.getString("gstin"));
        supplier.setDrugLicenseNumber(rs.getString("drug_license_number"));
        supplier.setPaymentTerms(rs.getString("payment_terms"));
        supplier.setSupplierStatus(normalizeStatus(readOptionalString(rs, "supplier_status")));
        supplier.setApprovedAt(readOptionalTimestamp(rs, "approved_at"));
        supplier.setRejectedAt(readOptionalTimestamp(rs, "rejected_at"));
        supplier.setRemarks(readOptionalString(rs, "remarks"));
        return supplier;
    }

    private String normalizeStatus(String status) {
        return isBlank(status) ? Supplier.STATUS_APPROVED : status.trim().toUpperCase();
    }

    private String normalizeRemarks(String remarks) {
        return isBlank(remarks) ? null : remarks.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String readOptionalString(ResultSet rs, String columnName) {
        try { return rs.getString(columnName); } catch (SQLException ex) { return null; }
    }

    private LocalDateTime readOptionalTimestamp(ResultSet rs, String columnName) {
        try {
            Timestamp ts = rs.getTimestamp(columnName);
            return ts != null ? ts.toLocalDateTime() : null;
        } catch (SQLException ex) { return null; }
    }
}
