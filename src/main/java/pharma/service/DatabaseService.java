package pharma.service;

import pharma.model.*;
import pharma.model.GRN.GRNItem;
import pharma.model.PurchaseOrder.PurchaseOrderItem;
import pharma.config.DatabaseConfig;
import pharma.repository.jdbc.GRNJdbcRepository;
import pharma.repository.jdbc.PurchaseOrderJdbcRepository;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import java.util.Date;

public class DatabaseService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);
    private static DatabaseService instance = null;
    private static final DatabaseConfig databaseConfig;
    private static HikariDataSource ds;

    // Phase 5: Dialect-aware repository delegates
    private final PurchaseOrderJdbcRepository poRepository;
    private final GRNJdbcRepository grnRepository;

    static {
        databaseConfig = DatabaseConfig.fromEnvironment();
        ds = new HikariDataSource(databaseConfig.toHikariConfig());
        logger.info("Initialized {} database pool for profile '{}' at {}.",
                databaseConfig.getDialect().getProfileName(),
                databaseConfig.getProfile(),
                databaseConfig.getRedactedJdbcUrl());
    }

    // *** FIX: Removed 'private Connection connection = null;' - connections should
    // not be long-lived fields.

    public DatabaseService() {
        this.poRepository = new PurchaseOrderJdbcRepository(this);
        this.grnRepository = new GRNJdbcRepository(this);
        ensureOptionalSchema();
    }

    // Helper method to establish a fresh, single-use connection
    public Connection getConnection() throws SQLException, ClassNotFoundException {
        return ds.getConnection();
    }

    public boolean connect() {
        try {
            getConnection().close();
            logger.info("{} database connection established successfully.",
                    databaseConfig.getDialect().getProfileName());
            return true;
        } catch (ClassNotFoundException e) {
            logger.error("JDBC driver not found. Check the classpath.", e);
            return false;
        } catch (SQLException e) {
            logger.error("SQL connection failed for {} profile '{}'.",
                    databaseConfig.getDialect().getProfileName(),
                    databaseConfig.getProfile(),
                    e);
            return false;
        }
    }

    public void disconnect() {
        logger.info("DatabaseService manages connection lifecycle per operation.");
    }

    public User getUserByCredentials(String username, String password) {
        return new pharma.service.AuthService(this).authenticate(username, password);
    }

    public static DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }

    private void ensureOptionalSchema() {
        if (!databaseConfig.isMysql()) {
            logger.info("Skipping MySQL-only optional schema updates for {} profile '{}'.",
                    databaseConfig.getDialect().getProfileName(),
                    databaseConfig.getProfile());
            return;
        }

        try (Connection conn = getConnection()) {
            ensureSupplierApprovalSchema(conn);
            ensureInventoryStatusLocationConsistency(conn);
        } catch (SQLException | ClassNotFoundException e) {
            logger.warn("Failed to auto-apply optional schema updates: {}", e.getMessage());
        }
    }

    private void ensureSupplierApprovalSchema(Connection conn) throws SQLException {
        boolean hadSupplierStatusColumn = hasColumn(conn, "Supplier_Master", "supplier_status");

        try (Statement stmt = conn.createStatement()) {
            if (!hadSupplierStatusColumn) {
                stmt.executeUpdate(
                        "ALTER TABLE Supplier_Master ADD COLUMN supplier_status VARCHAR(20) DEFAULT 'PENDING'");
            }
            if (!hasColumn(conn, "Supplier_Master", "approved_at")) {
                stmt.executeUpdate(
                        "ALTER TABLE Supplier_Master ADD COLUMN approved_at TIMESTAMP NULL");
            }
            if (!hasColumn(conn, "Supplier_Master", "rejected_at")) {
                stmt.executeUpdate(
                        "ALTER TABLE Supplier_Master ADD COLUMN rejected_at TIMESTAMP NULL");
            }
            if (!hasColumn(conn, "Supplier_Master", "remarks")) {
                stmt.executeUpdate(
                        "ALTER TABLE Supplier_Master ADD COLUMN remarks TEXT NULL");
            }

            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS supplier_audit_log (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY," +
                            "supplier_id INT NOT NULL," +
                            "action VARCHAR(20)," +
                            "remarks TEXT," +
                            "performed_by VARCHAR(50)," +
                            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                            "FOREIGN KEY (supplier_id) REFERENCES Supplier_Master(supplier_id) " +
                            "ON DELETE RESTRICT ON UPDATE CASCADE" +
                            ")");

            if (!hadSupplierStatusColumn) {
                stmt.executeUpdate(
                        "UPDATE Supplier_Master SET supplier_status = 'APPROVED', " +
                                "approved_at = COALESCE(approved_at, CURRENT_TIMESTAMP) " +
                                "WHERE supplier_status IS NULL OR supplier_status = 'PENDING'");
            }
        }
    }

    private boolean hasColumn(Connection conn, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getColumns(conn.getCatalog(), null, tableName, columnName)) {
            return rs.next();
        }
    }

    private void ensureInventoryStatusLocationConsistency(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                    "UPDATE Stock_Inventory si " +
                            "JOIN Material_Master mm ON mm.material_code = si.material_code " +
                            "SET si.qc_status = CASE " +
                            "        WHEN si.qc_status = 'APPROVED' AND mm.material_type = 'FINISHED_GOOD' THEN 'RELEASED' " +
                            "        ELSE si.qc_status " +
                            "    END, " +
                            "    si.location_code = CASE " +
                            "        WHEN si.qc_status = 'REJECTED' THEN 'REJECTED_AREA' " +
                            "        WHEN si.qc_status = 'IN_PRODUCTION' THEN 'PRODUCTION_FLOOR' " +
                            "        WHEN si.qc_status IN ('QUARANTINE', 'QI', 'IN_PROCESS_SAMPLE', 'UNDER_TEST') THEN 'QC_HOLD' " +
                            "        WHEN si.qc_status = 'RELEASED' THEN 'FINISHED_GOODS_WAREHOUSE' " +
                            "        WHEN si.qc_status = 'APPROVED' AND mm.material_type = 'FINISHED_GOOD' THEN 'FINISHED_GOODS_WAREHOUSE' " +
                            "        WHEN si.qc_status = 'APPROVED' AND mm.material_type = 'PACKAGING' THEN 'PACKAGING_WAREHOUSE' " +
                            "        WHEN si.qc_status = 'APPROVED' AND mm.material_type IN ('RAW_MATERIAL', 'INTERMEDIATE') THEN 'RAW_MATERIAL_WAREHOUSE' " +
                            "        ELSE si.location_code " +
                            "    END");

            stmt.executeUpdate(
                    "UPDATE production_batch pb " +
                            "JOIN Material_Master mm ON mm.material_code = pb.material_code " +
                            "SET pb.qc_status = CASE " +
                            "        WHEN pb.qc_status = 'APPROVED' AND mm.material_type = 'FINISHED_GOOD' THEN 'RELEASED' " +
                            "        ELSE pb.qc_status " +
                            "    END, " +
                            "    pb.location_code = CASE " +
                            "        WHEN pb.qc_status = 'REJECTED' THEN 'REJECTED_AREA' " +
                            "        WHEN pb.qc_status = 'IN_PRODUCTION' THEN 'PRODUCTION_FLOOR' " +
                            "        WHEN pb.qc_status IN ('QUARANTINE', 'QI', 'IN_PROCESS_SAMPLE', 'UNDER_TEST') THEN 'QC_HOLD' " +
                            "        WHEN pb.qc_status = 'RELEASED' THEN 'FINISHED_GOODS_WAREHOUSE' " +
                            "        WHEN pb.qc_status = 'APPROVED' AND mm.material_type = 'FINISHED_GOOD' THEN 'FINISHED_GOODS_WAREHOUSE' " +
                            "        WHEN pb.qc_status = 'APPROVED' AND mm.material_type = 'PACKAGING' THEN 'PACKAGING_WAREHOUSE' " +
                            "        WHEN pb.qc_status = 'APPROVED' AND mm.material_type IN ('RAW_MATERIAL', 'INTERMEDIATE') THEN 'RAW_MATERIAL_WAREHOUSE' " +
                            "        ELSE pb.location_code " +
                            "    END");
        }
    }

    public static void closePool() {
        if (ds != null && !ds.isClosed()) {
            ds.close();
        }
    }

    public static DatabaseConfig getDatabaseConfig() {
        return databaseConfig;
    }

    // =======================================================
    // --- Supplier CRUD OPERATIONS (BASED ON PREVIOUS REQUEST) ---
    // =======================================================
    public List<Supplier> getAllSuppliers() {
        List<Supplier> suppliers = new ArrayList<>();
        String sql = "SELECT * FROM Supplier_Master";
        // *** FIX: Use try-with-resources on the Connection (conn) to guarantee closure
        // of all resources.
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql); // Using PreparedStatement is better practice even
                                                                      // for simple selects
                ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                suppliers.add(mapResultSetToSupplier(rs));
            }
        } catch (SQLException | ClassNotFoundException e) { // Combined catch block
            System.err.println("Error fetching all suppliers: " + e.getMessage());
            e.printStackTrace();
        }
        return suppliers;
    }

    // method to addSupplier.
    public int addSupplier(Supplier supplier) {
        String sql = "INSERT INTO Supplier_Master (supplier_name, contact_person, address, email, phone_number, gstin, drug_license_number, payment_terms) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        int newId = -1;
        // *** FIX: Use try-with-resources on the Connection (conn) and
        // PreparedStatement.
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, supplier.getSupplierName());
            pstmt.setString(2, supplier.getContactPerson());
            pstmt.setString(3, supplier.getAddress());
            pstmt.setString(4, supplier.getEmail());
            pstmt.setString(5, supplier.getPhoneNumber());
            pstmt.setString(6, supplier.getGstin());
            pstmt.setString(7, supplier.getDrugLicenseNumber());
            pstmt.setString(8, supplier.getPaymentTerms());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        newId = generatedKeys.getInt(1);
                        supplier.setSupplierId(newId);
                        supplier.setSupplierStatus(Supplier.STATUS_PENDING);
                        logAuditTrail(conn, 0, "ADD_SUPPLIER", "Supplier_Master", String.valueOf(newId), null,
                                supplier.getSupplierName());
                        System.out.println("New supplier added with ID: " + newId);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error adding supplier: " + e.getMessage());
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.err.println("JDBC Driver not found: " + e.getMessage());
            e.printStackTrace();
        }
        return newId;
    }

    // method to updateSupplier.
    public boolean updateSupplier(Supplier supplier) {
        String sql = "UPDATE Supplier_Master SET supplier_name=?, contact_person=?, address=?, email=?, phone_number=?, gstin=?, drug_license_number=?, payment_terms=? WHERE supplier_id=?";
        // *** FIX: Use try-with-resources on the Connection (conn) and
        // PreparedStatement.
        try (Connection conn = getConnection();
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
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                logAuditTrail(conn, 0, "EDIT_SUPPLIER", "Supplier_Master", String.valueOf(supplier.getSupplierId()),
                        null, supplier.getSupplierName());
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Error updating supplier: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (ClassNotFoundException e) {
            System.err.println("JDBC Driver not found: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // method to deleteSupplier.
    public boolean deleteSupplier(int supplierId) {
        try (Connection conn = getConnection()) {
            logAuditTrail(conn, 0, "DELETE_SUPPLIER_BLOCKED", "Supplier_Master", String.valueOf(supplierId), null,
                    "Deletion blocked by supplier approval workflow");
            return false;
        } catch (SQLException e) {
            System.err.println("Error deleting supplier: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (ClassNotFoundException e) {
            System.err.println("JDBC Driver not found: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public List<String> getSupplierNames() throws ClassNotFoundException {
        String sql = "SELECT supplier_name FROM Supplier_Master ORDER BY supplier_name";
        List<String> supplierNames = new ArrayList<>();

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                supplierNames.add(rs.getString("supplier_name"));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving supplier names: " + e.getMessage());
            e.printStackTrace();
        }
        return supplierNames;
    }

    public boolean approveSupplier(int supplierId, String remarks, String performedBy)
            throws SQLException, ClassNotFoundException {
        String selectSql = "SELECT supplier_name, gstin, drug_license_number, supplier_status FROM Supplier_Master WHERE supplier_id = ?";
        String updateSql = "UPDATE Supplier_Master SET supplier_status = 'APPROVED', approved_at = CURRENT_TIMESTAMP, rejected_at = NULL, remarks = ? WHERE supplier_id = ?";

        try (Connection conn = getConnection()) {
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
                            supplier.setSupplierStatus(
                                    normalizeSupplierStatus(readOptionalString(rs, "supplier_status")));
                        }
                    }
                }

                if (supplier == null) {
                    throw new SQLException("Supplier not found.");
                }
                if (Supplier.STATUS_REJECTED.equalsIgnoreCase(supplier.getSupplierStatus())) {
                    throw new SQLException("Rejected suppliers cannot be approved again.");
                }
                if (Supplier.STATUS_APPROVED.equalsIgnoreCase(supplier.getSupplierStatus())) {
                    throw new SQLException("Supplier is already approved.");
                }
                if (isBlank(supplier.getDrugLicenseNumber())) {
                    throw new SQLException("License number required for approval.");
                }
                if (isBlank(supplier.getGstin())) {
                    throw new SQLException("GSTIN required for approval.");
                }

                try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                    pstmt.setString(1, normalizeRemarks(remarks));
                    pstmt.setInt(2, supplierId);
                    pstmt.executeUpdate();
                }

                insertSupplierAuditLog(conn, supplierId, "APPROVED", remarks, performedBy);
                logAuditTrail(conn, 0, "APPROVE_SUPPLIER", "Supplier_Master", String.valueOf(supplierId),
                        Supplier.STATUS_PENDING, Supplier.STATUS_APPROVED);
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
        String selectSql = "SELECT supplier_status FROM Supplier_Master WHERE supplier_id = ?";
        String updateSql = "UPDATE Supplier_Master SET supplier_status = 'REJECTED', rejected_at = CURRENT_TIMESTAMP, remarks = ? WHERE supplier_id = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                String oldStatus = null;
                try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
                    pstmt.setInt(1, supplierId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            oldStatus = normalizeSupplierStatus(readOptionalString(rs, "supplier_status"));
                        }
                    }
                }

                if (oldStatus == null) {
                    throw new SQLException("Supplier not found.");
                }
                if (Supplier.STATUS_REJECTED.equalsIgnoreCase(oldStatus)) {
                    throw new SQLException("Supplier is already rejected.");
                }

                try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                    pstmt.setString(1, normalizeRemarks(remarks));
                    pstmt.setInt(2, supplierId);
                    pstmt.executeUpdate();
                }

                insertSupplierAuditLog(conn, supplierId, "REJECTED", remarks, performedBy);
                logAuditTrail(conn, 0, "REJECT_SUPPLIER", "Supplier_Master", String.valueOf(supplierId), oldStatus,
                        Supplier.STATUS_REJECTED);
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

    private void insertSupplierAuditLog(Connection conn, int supplierId, String action, String remarks,
            String performedBy)
            throws SQLException {
        String sql = "INSERT INTO supplier_audit_log (supplier_id, action, remarks, performed_by) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, supplierId);
            pstmt.setString(2, action);
            pstmt.setString(3, normalizeRemarks(remarks));
            pstmt.setString(4, isBlank(performedBy) ? "system" : performedBy);
            pstmt.executeUpdate();
        }
    }

    private Supplier mapResultSetToSupplier(ResultSet rs) throws SQLException {
        return new Supplier(
                rs.getInt("supplier_id"),
                rs.getString("supplier_name"),
                rs.getString("contact_person"),
                rs.getString("address"),
                rs.getString("email"),
                rs.getString("phone_number"),
                rs.getString("gstin"),
                rs.getString("drug_license_number"),
                rs.getString("payment_terms"),
                normalizeSupplierStatus(readOptionalString(rs, "supplier_status")),
                readOptionalTimestamp(rs, "approved_at"),
                readOptionalTimestamp(rs, "rejected_at"),
                readOptionalString(rs, "remarks"));
    }

    private String normalizeSupplierStatus(String status) {
        return isBlank(status) ? Supplier.STATUS_APPROVED : status.trim().toUpperCase();
    }

    private boolean isSupplierApproved(Connection conn, int supplierId) throws SQLException {
        String sql = "SELECT supplier_status FROM Supplier_Master WHERE supplier_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, supplierId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Supplier not found.");
                }
                return Supplier.STATUS_APPROVED.equalsIgnoreCase(
                        normalizeSupplierStatus(readOptionalString(rs, "supplier_status")));
            }
        }
    }

    private void validateSupplierApprovedForProcurement(Connection conn, int supplierId, String action)
            throws SQLException {
        if (!isSupplierApproved(conn, supplierId)) {
            throw new SQLException(action);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalizeRemarks(String remarks) {
        return isBlank(remarks) ? null : remarks.trim();
    }

    private String readOptionalString(ResultSet rs, String columnName) throws SQLException {
        try {
            return rs.getString(columnName);
        } catch (SQLException ex) {
            return null;
        }
    }

    private LocalDateTime readOptionalTimestamp(ResultSet rs, String columnName) throws SQLException {
        try {
            Timestamp timestamp = rs.getTimestamp(columnName);
            return timestamp != null ? timestamp.toLocalDateTime() : null;
        } catch (SQLException ex) {
            return null;
        }
    }

    // =======================================================
    // --- MATERIAL CRUD OPERATIONS (BASED ON PREVIOUS REQUEST) ---
    // =======================================================

    private Material mapResultSetToDrug(ResultSet rs) throws SQLException {
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
        material.setMaterialType(Material.MaterialType.fromString(rs.getString("material_type")));
        material.setUnitOfMeasure(Material.UnitOfMeasure.fromString(rs.getString("unit_of_measure")));

        int preferredSupplierId = rs.getInt("preferred_supplier_id");
        if (rs.wasNull()) { // Checks if the last value read was SQL NULL
            material.setPreferredSupplierId(null);
        } else {
            material.setPreferredSupplierId(preferredSupplierId);
        }
        return material;
    }

    public List<Material> getAllDrugs() {
        List<Material> materials = new ArrayList<>();
        String SQL = "SELECT material_code, brand_name, generic_name, manufacturer, formulation, strength, schedule_category, storage_conditions, reorder_level, is_active, preferred_supplier_id, material_type, unit_of_measure FROM Material_Master";

        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(SQL)) {

            while (rs.next()) {
                materials.add(mapResultSetToDrug(rs));
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error fetching all materials: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
        return materials;
    }

    public boolean addDrug(Material newDrug) throws SQLException {
        String SQL = "INSERT INTO Material_Master (material_code, brand_name, generic_name, manufacturer, formulation, strength, schedule_category, storage_conditions, reorder_level, is_active, preferred_supplier_id, material_type, unit_of_measure) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(SQL)) {
            stmt.setString(1, newDrug.getMaterialCode());
            stmt.setString(2, newDrug.getBrandName());
            stmt.setString(3, newDrug.getGenericName());
            stmt.setString(4, newDrug.getManufacturer());
            stmt.setString(5, newDrug.getFormulation());
            stmt.setString(6, newDrug.getStrength());
            stmt.setString(7, newDrug.getScheduleCategory());
            stmt.setString(8, newDrug.getStorageConditions());
            stmt.setInt(9, newDrug.getReorderLevel());
            stmt.setBoolean(10, newDrug.isActive());

            if (newDrug.getPreferredSupplierId() == null) {
                stmt.setNull(11, java.sql.Types.INTEGER);
            } else {
                stmt.setInt(11, newDrug.getPreferredSupplierId());
            }
            stmt.setString(12, newDrug.getMaterialType() != null ? newDrug.getMaterialType().name() : null);
            stmt.setString(13, newDrug.getUnitOfMeasure() != null ? newDrug.getUnitOfMeasure().name() : null);

            int affectedRows = stmt.executeUpdate();
            System.out.println("Material added successfully: " + newDrug.getBrandName());
            return affectedRows > 0; // <-- FIXED to return boolean result

        } catch (SQLException e) {
            System.err.println("Database Error inserting material: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } catch (ClassNotFoundException e) {
            System.err.println("JDBC Driver not found: " + e.getMessage());
            e.printStackTrace();
            throw new SQLException("JDBC Driver not found.", e);
        }
    }

    /**
     * Retrieves a single Material record using its primary key (materialCode).
     */
    public Material getDrugByMaterialCode(String materialCode) {
        String sql = "SELECT material_code, brand_name, generic_name, manufacturer, formulation, strength, schedule_category, storage_conditions, reorder_level, is_active, preferred_supplier_id, material_type, unit_of_measure FROM Material_Master WHERE material_code = ?";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, materialCode);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToDrug(rs); // <-- FIXED: Use helper
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error fetching material by code: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Updates an existing Material record based on its materialCode.
     */
    public boolean updateDrug(Material material) {
        // Updated SQL to include 'preferred_supplier_id', 'material_type', and
        // 'unit_of_measure' in the SET clause
        String sql = "UPDATE Material_Master SET brand_name=?, generic_name=?, manufacturer=?, formulation=?, strength=?, schedule_category=?, storage_conditions=?, reorder_level=?, is_active=?, preferred_supplier_id=?, material_type=?, unit_of_measure=? WHERE material_code=?";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Existing parameters (1-9)
            pstmt.setString(1, material.getBrandName());
            pstmt.setString(2, material.getGenericName());
            pstmt.setString(3, material.getManufacturer());
            pstmt.setString(4, material.getFormulation());
            pstmt.setString(5, material.getStrength());
            pstmt.setString(6, material.getScheduleCategory());
            pstmt.setString(7, material.getStorageConditions());
            pstmt.setInt(8, material.getReorderLevel());
            pstmt.setBoolean(9, material.isActive());

            // FIX: New parameter (10) for the foreign key
            Integer supplierId = material.getPreferredSupplierId();
            if (supplierId != null) {
                pstmt.setInt(10, supplierId);
            } else {
                pstmt.setNull(10, java.sql.Types.INTEGER);
            }

            pstmt.setString(11, material.getMaterialType() != null ? material.getMaterialType().name() : null);
            pstmt.setString(12, material.getUnitOfMeasure() != null ? material.getUnitOfMeasure().name() : null);

            // WHERE clause parameter (13)
            pstmt.setString(13, material.getMaterialCode());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error updating material: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteDrug(String materialCode) { // <--- MODIFIED METHOD SIGNATURE AND LOGIC
        // FIX: Corrected table name to Material_Master
        String sql = "DELETE FROM Material_Master WHERE material_code = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, materialCode);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException | ClassNotFoundException e) { // Combined catch block
            System.err.println("Error deleting material: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public List<Material> getDrugs() {
        List<Material> materials = new ArrayList<>();

        // FIX: Updated SQL query to explicitly select 'preferred_supplier_id',
        // 'material_type', 'unit_of_measure' (13
        // columns total)
        String sql = "SELECT material_code, brand_name, generic_name, manufacturer, formulation, strength, schedule_category, storage_conditions, reorder_level, is_active, preferred_supplier_id, material_type, unit_of_measure FROM Material_Master";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Material material = new Material(
                        rs.getString("material_code"),
                        rs.getString("brand_name"),
                        rs.getString("generic_name"),
                        rs.getString("manufacturer"),
                        rs.getString("formulation"),
                        rs.getString("strength"),
                        rs.getString("schedule_category"),
                        rs.getString("storage_conditions"),
                        rs.getInt("reorder_level"),
                        rs.getBoolean("is_active"),
                        rs.getInt("preferred_supplier_id"),
                        Material.MaterialType.fromString(rs.getString("material_type")),
                        Material.UnitOfMeasure.fromString(rs.getString("unit_of_measure")));
                materials.add(material);
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error fetching all materials: " + e.getMessage());
            e.printStackTrace();
        }
        return materials;
    }

    public boolean editDrug(Material material) {
        return updateDrug(material);
    }

    // =======================================================
    // --- LOCATION CRUD OPERATIONS (MODIFIED FOR Location.java) ---
    // =======================================================
    public List<Location> getLocations() {
        List<Location> locations = new ArrayList<>();
        // FIX: Replaced hardcoded columns with the ones selected in the query
        String sql = "SELECT location_code, location_name, description, capacity FROM Location_Master";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                // FIX: Use the Location constructor that aligns with the fields
                Location location = new Location(
                        rs.getString("location_code"), // FIX: Use correct column name
                        rs.getString("location_name"), // FIX: Use correct column name
                        rs.getString("description"),
                        rs.getInt("capacity"));
                locations.add(location);
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error fetching all locations: " + e.getMessage());
            e.printStackTrace(); // Added stack trace
        }
        return locations;
    }

    /**
     * FIX: Replaces stub. Fetches a single Location record by its locationCode.
     * 
     * @param locationCode The primary key of the location.
     * @return The Location object or null if not found or on error.
     */
    public Location getLocationById(String locationCode) {
        // locationCode is the Primary Key
        String sql = "SELECT location_code, location_name, description, capacity FROM Location_Master WHERE location_code = ?";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, locationCode);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Location(
                            rs.getString("location_code"),
                            rs.getString("location_name"),
                            rs.getString("description"),
                            rs.getInt("capacity"));
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error fetching location by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public boolean addLocation(String code, String name, String description, int capacity)
            throws ClassNotFoundException {
        // SQL statement to insert a new location.
        String sql = "INSERT INTO location_master (location_code, location_name, description, capacity) VALUES (?, ?, ?, ?)";

        try (
                // FIX: Using try-with-resources for automatic resource closing
                Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);) {
            // Validate essential fields
            if (code == null || code.trim().isEmpty() || name == null || name.trim().isEmpty()) {
                System.err.println("Cannot add location: Code or Name is empty.");
                return false;
            }

            // 1. Set parameters for the INSERT statement
            pstmt.setString(1, code.trim());
            pstmt.setString(2, name.trim());
            pstmt.setString(3, description.trim());
            pstmt.setInt(4, capacity);

            int affectedRows = pstmt.executeUpdate();

            return affectedRows == 1;

        } catch (SQLException e) {
            // Log the error (e.g., duplicate PK error, connection failure)
            System.err.println("SQL Error inserting location '" + code + "': " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateLocation(String code, String name, String description, int capacity)
            throws ClassNotFoundException {
        // SQL statement to update existing columns, using location_code as the WHERE
        // clause.
        String sql = "UPDATE location_master SET location_name = ?, description = ?, capacity = ? WHERE location_code = ?";

        try (
                // FIX: Using try-with-resources for automatic resource closing
                Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);) {
            // Validate essential fields
            if (code == null || code.trim().isEmpty()) {
                System.err.println("Cannot update location: Code is empty.");
                return false;
            }

            // 1. Set parameters for the UPDATE clause (SET columns)
            pstmt.setString(1, name.trim());
            pstmt.setString(2, description.trim());
            pstmt.setInt(3, capacity);

            // 2. Set parameter for the WHERE clause (Primary Key)
            pstmt.setString(4, code.trim());

            int affectedRows = pstmt.executeUpdate();

            return affectedRows == 1;

        } catch (SQLException e) {
            // Log the error
            System.err.println("SQL Error updating location '" + code + "': " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteLocation(String locationCode) {
        String sql = "DELETE FROM Location_Master WHERE location_code = ?";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, locationCode);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error deleting location: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // =======================================================
    // --- REPORTING & TRANSACTIONAL METHODS (FIXED FROM STUBS) ---
    // =======================================================
    /**
     * FIX: Implemented to retrieve the full inventory data (Material Master Data).
     * 
     * @return List of Material objects.
     */
    public List<Material> getFullInventoryReport() {
        try {
            System.out.println("Retrieving full inventory (Material Master Data)...");
            return getDrugs();
        } catch (Exception e) {
            System.err.println("Error retrieving data for Full Inventory Report: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // =====================================================================
    // PURCHASE ORDER METHODS - delegated to PurchaseOrderJdbcRepository (Phase 5)
    // =====================================================================

    public List<PurchaseOrder> getPurchaseOrders() {
        return poRepository.findAll();
    }

    public String generateNextPoNumber() {
        return poRepository.generateNextPoNumber();
    }

    public boolean createPurchaseOrder(PurchaseOrder po) throws ClassNotFoundException {
        return poRepository.create(po);
    }

    public boolean createPurchaseOrder(String selectedSupplierName) {
        return poRepository.createForSupplier(selectedSupplierName);
    }

    public PurchaseOrder getPurchaseOrderById(String poId) throws ClassNotFoundException {
        try {
            return poRepository.findById(Integer.parseInt(poId));
        } catch (NumberFormatException e) {
            logger.error("Invalid PO ID format: {}", poId);
            return null;
        }
    }

    public List<PurchaseOrderItem> getPurchaseOrderItems(int poId) throws SQLException, ClassNotFoundException {
        return poRepository.findItemsByPoId(poId);
    }

    public Supplier getSupplierById(int supplierId) throws ClassNotFoundException {
        String sql = "SELECT * FROM Supplier_Master WHERE supplier_id = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, supplierId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToSupplier(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching supplier by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public int getSupplierIdByName(String supplierName) throws ClassNotFoundException {
        String sql = "SELECT supplier_id FROM Supplier_Master WHERE supplier_name = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, supplierName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("supplier_id");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching supplier ID: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    public boolean updatePurchaseOrder(PurchaseOrder updatedPo) {
        return poRepository.update(updatedPo);
    }

    public void deletePurchaseOrder(int orderId) throws SQLException {
        poRepository.delete(orderId);
    }

    public void receivePurchaseOrderShipment(int orderId) {
        poRepository.receiveShipment(orderId);
    }

    // =====================================================================
    // GRN METHODS - delegated to GRNJdbcRepository (Phase 5)
    // =====================================================================

    public List<GRN> getGRNs() {
        return grnRepository.findAll();
    }

    public GRN getGRNById(int grnId) {
        return grnRepository.findById(grnId);
    }

    public boolean createGRNFromPO(PurchaseOrder po) {
        return grnRepository.createFromPO(po);
    }


    // MANUFACTURING ERP METHODS - Phase 3
    // =====================================================================

    // --- BOM (Bill of Materials) Management ---

    public int createBOM(BOMHeader header, List<BOMDetail> details) throws SQLException, ClassNotFoundException {
        Connection conn = null;
        int bomId = -1;

        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            String headerSql = "INSERT INTO BOM_Header (material_code, version_number, is_active, effective_date, description) VALUES (?, ?, ?, ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(headerSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, header.getMaterialCode());
                pstmt.setInt(2, header.getVersionNumber());
                pstmt.setBoolean(3, header.isActive());
                pstmt.setDate(4, java.sql.Date.valueOf(header.getEffectiveDate()));
                pstmt.setString(5, header.getDescription());

                pstmt.executeUpdate();

                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        bomId = rs.getInt(1);
                    }
                }
            }

            if (bomId > 0 && details != null && !details.isEmpty()) {
                String detailSql = "INSERT INTO BOM_Details (bom_id, ingredient_material_code, required_qty, uom, sequence_number, notes) VALUES (?, ?, ?, ?, ?, ?)";

                try (PreparedStatement pstmt = conn.prepareStatement(detailSql)) {
                    for (BOMDetail detail : details) {
                        pstmt.setInt(1, bomId);
                        pstmt.setString(2, detail.getIngredientMaterialCode());
                        pstmt.setDouble(3, detail.getRequiredQty());
                        pstmt.setString(4, detail.getUom());
                        pstmt.setInt(5, detail.getSequenceNumber());
                        pstmt.setString(6, detail.getNotes());
                        pstmt.addBatch();
                    }
                    pstmt.executeBatch();
                }
            }

            conn.commit();
            return bomId;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
            }
        }
    }

    public BOMHeader getBOMById(int bomId) throws SQLException, ClassNotFoundException {
        String sql = "SELECT * FROM BOM_Header WHERE bom_id = ?";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, bomId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new BOMHeader(
                            rs.getInt("bom_id"),
                            rs.getString("material_code"),
                            rs.getInt("version_number"),
                            rs.getBoolean("is_active"),
                            rs.getDate("effective_date").toLocalDate(),
                            rs.getString("description"),
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getTimestamp("updated_at").toLocalDateTime());
                }
            }
        }
        return null;
    }

    public List<BOMDetail> getBOMIngredients(int bomId) throws SQLException, ClassNotFoundException {
        List<BOMDetail> ingredients = new ArrayList<>();
        String sql = "SELECT * FROM BOM_Details WHERE bom_id = ? ORDER BY sequence_number";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, bomId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ingredients.add(new BOMDetail(
                            rs.getInt("bom_detail_id"),
                            rs.getInt("bom_id"),
                            rs.getString("ingredient_material_code"),
                            rs.getDouble("required_qty"),
                            rs.getString("uom"),
                            rs.getInt("sequence_number"),
                            rs.getString("notes")));
                }
            }
        }
        return ingredients;
    }

    public List<BOMHeader> getActiveBOMsForMaterial(String materialCode) throws SQLException, ClassNotFoundException {
        List<BOMHeader> boms = new ArrayList<>();
        String sql = "SELECT * FROM BOM_Header WHERE material_code = ? AND is_active = TRUE ORDER BY version_number DESC";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, materialCode);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    boms.add(new BOMHeader(
                            rs.getInt("bom_id"),
                            rs.getString("material_code"),
                            rs.getInt("version_number"),
                            rs.getBoolean("is_active"),
                            rs.getDate("effective_date").toLocalDate(),
                            rs.getString("description"),
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getTimestamp("updated_at").toLocalDateTime()));
                }
            }
        }
        return boms;
    }

    public Map<String, Double> validateBOMAvailability(int bomId, double plannedQty)
            throws SQLException, ClassNotFoundException {
        Map<String, Double> shortages = new HashMap<>();

        List<BOMDetail> ingredients = getBOMIngredients(bomId);

        for (BOMDetail ingredient : ingredients) {
            double requiredQty = ingredient.getRequiredQty() * plannedQty;

            String sql = "SELECT SUM(quantity) as available FROM Stock_Inventory WHERE material_code = ? AND qc_status = 'APPROVED' AND location_code != 'REJECTED_AREA'";

            try (Connection conn = getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, ingredient.getIngredientMaterialCode());

                try (ResultSet rs = pstmt.executeQuery()) {
                    double available = 0;
                    if (rs.next()) {
                        available = rs.getDouble("available");
                    }

                    double shortage = Math.max(0, requiredQty - available);
                    shortages.put(ingredient.getIngredientMaterialCode(), shortage);
                }
            }
        }

        return shortages;
    }

    // --- Production Order Management ---

    public int createProductionOrder(ProductionOrder order) throws SQLException, ClassNotFoundException {
        String sql = "INSERT INTO Production_Order (batch_number, bom_id, planned_qty, status, production_date, created_by, notes) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, order.getBatchNumber());
            pstmt.setInt(2, order.getBomId());
            pstmt.setDouble(3, order.getPlannedQty());
            pstmt.setString(4, order.getStatus().getDisplayName());
            pstmt.setDate(5, java.sql.Date.valueOf(order.getProductionDate()));
            pstmt.setInt(6, order.getCreatedBy());
            pstmt.setString(7, order.getNotes());

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int orderId = rs.getInt(1);
                    logAuditTrail(conn, order.getCreatedBy(), "CREATE_PRODUCTION_ORDER", "Production_Order",
                            String.valueOf(orderId), null, order.getBatchNumber());
                    return orderId;
                }
            }
        }
        return -1;
    }

    public List<ProductionOrder> getAllProductionOrders() throws SQLException, ClassNotFoundException {
        List<ProductionOrder> orders = new ArrayList<>();
        String sql = "SELECT * FROM Production_Order ORDER BY production_date DESC, order_id DESC";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                orders.add(mapResultSetToProductionOrder(rs));
            }
        }
        return orders;
    }

    public ProductionOrder getProductionOrderById(int orderId) throws SQLException, ClassNotFoundException {
        String sql = "SELECT * FROM Production_Order WHERE order_id = ?";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, orderId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProductionOrder(rs);
                }
            }
        }
        return null;
    }

    private ProductionOrder mapResultSetToProductionOrder(ResultSet rs) throws SQLException {
        return new ProductionOrder(
                rs.getInt("order_id"),
                rs.getString("batch_number"),
                rs.getInt("bom_id"),
                rs.getDouble("planned_qty"),
                rs.getObject("actual_qty") != null ? rs.getDouble("actual_qty") : null,
                ProductionOrder.ProductionStatus.fromString(rs.getString("status")),
                rs.getDate("production_date").toLocalDate(),
                rs.getDate("completed_date") != null ? rs.getDate("completed_date").toLocalDate() : null,
                rs.getInt("created_by"),
                rs.getString("notes"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime());
    }

    public void updateProductionOrderStatus(int orderId, String newStatus) throws SQLException, ClassNotFoundException {
        String sql = "UPDATE Production_Order SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE order_id = ?";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStatus);
            pstmt.setInt(2, orderId);
            pstmt.executeUpdate();
        }
    }

    // --- Manufacturing Execution (CRITICAL ATOMIC TRANSACTION) ---

    public void executeProductionRun(int orderId, int userId) throws SQLException, ClassNotFoundException {
        Connection conn = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            ProductionOrder order = getProductionOrderById(orderId);
            if (order == null) {
                throw new SQLException("Production order not found: " + orderId);
            }

            BOMHeader bom = getBOMById(order.getBomId());
            List<BOMDetail> ingredients = getBOMIngredients(order.getBomId());

            Map<String, Double> shortages = validateBOMAvailability(order.getBomId(), order.getPlannedQty());
            for (Map.Entry<String, Double> entry : shortages.entrySet()) {
                if (entry.getValue() > 0) {
                    throw new SQLException(
                            "Insufficient material: " + entry.getKey() + ", shortage: " + entry.getValue());
                }
            }

            StringBuilder parentBatches = new StringBuilder();

            for (BOMDetail ingredient : ingredients) {
                double qtyNeeded = ingredient.getRequiredQty() * order.getPlannedQty();

                String selectSql = "SELECT stock_id, batch_number, quantity, exp_date FROM Stock_Inventory WHERE material_code = ? AND qc_status = 'APPROVED' AND location_code != 'REJECTED_AREA' AND quantity > 0 ORDER BY exp_date ASC, stock_id ASC";

                try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                    selectStmt.setString(1, ingredient.getIngredientMaterialCode());

                    try (ResultSet rs = selectStmt.executeQuery()) {
                        double remaining = qtyNeeded;

                        while (rs.next() && remaining > 0) {
                            int stockId = rs.getInt("stock_id");
                            String batchNumber = rs.getString("batch_number");
                            double available = rs.getDouble("quantity");

                            double toConsume = Math.min(remaining, available);

                            String updateSql = "UPDATE Stock_Inventory SET quantity = quantity - ? WHERE stock_id = ?";
                            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                                updateStmt.setDouble(1, toConsume);
                                updateStmt.setInt(2, stockId);
                                updateStmt.executeUpdate();
                            }

                            // 1. Material Consumption
                            String insertMcSql = "INSERT INTO production_material_consumption (production_order_id, material_code, batch_number, required_qty, consumed_qty, uom) VALUES (?, ?, ?, ?, ?, ?)";
                            try (PreparedStatement mcStmt = conn.prepareStatement(insertMcSql)) {
                                mcStmt.setInt(1, orderId);
                                mcStmt.setString(2, ingredient.getIngredientMaterialCode());
                                mcStmt.setString(3, batchNumber);
                                mcStmt.setDouble(4, qtyNeeded);
                                mcStmt.setDouble(5, toConsume);
                                mcStmt.setString(6, ingredient.getUom());
                                mcStmt.executeUpdate();
                            }

                            // 2. Inventory Transaction (Consumption)
                            String insertTxSql = "INSERT INTO inventory_transaction (material_code, batch_number, location_code, transaction_type, quantity, reference_type, reference_id, performed_by, notes) VALUES (?, ?, 'PRODUCTION_FLOOR', 'PRODUCTION_CONSUMPTION', ?, 'PRODUCTION_ORDER', ?, ?, ?)";
                            try (PreparedStatement txStmt = conn.prepareStatement(insertTxSql)) {
                                txStmt.setString(1, ingredient.getIngredientMaterialCode());
                                txStmt.setString(2, batchNumber);
                                txStmt.setDouble(3, -toConsume);
                                txStmt.setString(4, String.valueOf(orderId));
                                txStmt.setInt(5, userId);
                                txStmt.setString(6, "Consumed for Order " + orderId);
                                txStmt.executeUpdate();
                            }

                            // 3. Batch Genealogy
                            String insertBgSql = "INSERT INTO batch_genealogy (parent_batch, child_batch, production_order_id, relationship_type) VALUES (?, ?, ?, 'USED_IN')";
                            try (PreparedStatement bgStmt = conn.prepareStatement(insertBgSql)) {
                                bgStmt.setString(1, batchNumber);
                                bgStmt.setString(2, order.getBatchNumber());
                                bgStmt.setInt(3, orderId);
                                bgStmt.executeUpdate();
                            }

                            if (parentBatches.length() > 0) {
                                parentBatches.append(",");
                            }
                            parentBatches.append(batchNumber);

                            remaining -= toConsume;
                        }
                    }
                }
            }

            String insertStockSql = "INSERT INTO Stock_Inventory (material_code, location_code, batch_number, quantity, unit_cost, mfg_date, exp_date, qc_status, parent_batch_id, production_order_id) VALUES (?, ?, ?, ?, ?, ?, ?, 'IN_PRODUCTION', ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(insertStockSql)) {
                pstmt.setString(1, bom.getMaterialCode());
                pstmt.setString(2, "PRODUCTION_FLOOR"); // In-production batches remain on the production floor
                pstmt.setString(3, order.getBatchNumber());
                pstmt.setDouble(4, order.getPlannedQty());
                pstmt.setDouble(5, 0.0);
                pstmt.setDate(6, java.sql.Date.valueOf(LocalDate.now()));
                pstmt.setDate(7, java.sql.Date.valueOf(LocalDate.now().plusYears(2)));
                pstmt.setString(8, parentBatches.toString());
                pstmt.setInt(9, orderId);
                pstmt.executeUpdate();
            }

            // 4. Production Batch Record
            String insertPbSql = "INSERT INTO production_batch (production_order_id, material_code, batch_number, quantity, mfg_date, expiry_date, qc_status, location_code) VALUES (?, ?, ?, ?, ?, ?, 'IN_PRODUCTION', 'PRODUCTION_FLOOR')";
            try (PreparedStatement pbStmt = conn.prepareStatement(insertPbSql)) {
                pbStmt.setInt(1, orderId);
                pbStmt.setString(2, bom.getMaterialCode());
                pbStmt.setString(3, order.getBatchNumber());
                pbStmt.setDouble(4, order.getPlannedQty());
                pbStmt.setDate(5, java.sql.Date.valueOf(LocalDate.now()));
                pbStmt.setDate(6, java.sql.Date.valueOf(LocalDate.now().plusYears(2)));
                pbStmt.executeUpdate();
            }

            // 5. Inventory Transaction (Finished Good Receipt)
            String insertTxFGSql = "INSERT INTO inventory_transaction (material_code, batch_number, location_code, transaction_type, quantity, reference_type, reference_id, performed_by, notes) VALUES (?, ?, 'PRODUCTION_FLOOR', 'PRODUCTION_RECEIPT', ?, 'PRODUCTION_ORDER', ?, ?, ?)";
            try (PreparedStatement txFgStmt = conn.prepareStatement(insertTxFGSql)) {
                txFgStmt.setString(1, bom.getMaterialCode());
                txFgStmt.setString(2, order.getBatchNumber());
                txFgStmt.setDouble(3, order.getPlannedQty());
                txFgStmt.setString(4, String.valueOf(orderId));
                txFgStmt.setInt(5, userId);
                txFgStmt.setString(6, "Received from Production Order " + orderId);
                txFgStmt.executeUpdate();
            }

            // 6. Event Log
            String insertEventSql = "INSERT INTO event_log (event_type, entity_type, entity_id, details, status) VALUES (?, 'PRODUCTION_ORDER', ?, ?, 'SUCCESS')";
            try (PreparedStatement evStmt = conn.prepareStatement(insertEventSql)) {
                evStmt.setString(1, "PRODUCTION_COMPLETED");
                evStmt.setString(2, String.valueOf(orderId));
                evStmt.setString(3, "Production run executed. Batch: " + order.getBatchNumber());
                evStmt.executeUpdate();
            }

            String updateOrderSql = "UPDATE Production_Order SET status = 'In-Production', actual_qty = ?, completed_date = CURDATE() WHERE order_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateOrderSql)) {
                pstmt.setDouble(1, order.getPlannedQty());
                pstmt.setInt(2, orderId);
                pstmt.executeUpdate();
            }

            logAuditTrail(conn, userId, "PRODUCTION_RUN", "Production_Order", String.valueOf(orderId), "Planned",
                    "In-Production");

            conn.commit();
            System.out.println("Production run executed successfully for order: " + orderId);

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    System.err.println("Production run failed, transaction rolled back: " + e.getMessage());
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
            }
        }
    }

    // --- QC Status Management ---

    public void updateQCStatus(String batchNumber, String newStatus, int userId)
            throws SQLException, ClassNotFoundException {
        Connection conn = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            // Fetch old status and material type
            String oldStatus = null;
            String materialType = null;
            Integer productionOrderId = null;
            String typeSql = "SELECT si.qc_status, si.production_order_id, mm.material_type FROM Stock_Inventory si " +
                    "JOIN Material_Master mm ON si.material_code = mm.material_code " +
                    "WHERE si.batch_number = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(typeSql)) {
                pstmt.setString(1, batchNumber);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        oldStatus = rs.getString("qc_status");
                        materialType = rs.getString("material_type");
                        productionOrderId = rs.getObject("production_order_id") != null
                                ? rs.getInt("production_order_id")
                                : null;
                    }
                }
            }

            if (oldStatus == null) {
                throw new SQLException("Batch not found: " + batchNumber);
            }

            if (!"QI".equalsIgnoreCase(oldStatus) && !"UNDER_TEST".equalsIgnoreCase(oldStatus)) {
                throw new SQLException(
                        "Batch " + batchNumber + " must be in QI or UNDER_TEST before it can be " + newStatus + ".");
            }

            if (!"APPROVED".equalsIgnoreCase(newStatus) && !"REJECTED".equalsIgnoreCase(newStatus)) {
                throw new SQLException("Only APPROVED or REJECTED are valid QC decisions.");
            }

            // Determine target location and stored batch status based on the QC decision.
            String finalBatchStatus = newStatus;
            String targetLocation = null;
            if ("REJECTED".equals(newStatus)) {
                targetLocation = "REJECTED_AREA";
            } else if ("APPROVED".equals(newStatus)) {
                if ("FINISHED_GOOD".equals(materialType)) {
                    finalBatchStatus = "RELEASED";
                    targetLocation = "FINISHED_GOODS_WAREHOUSE";
                } else if ("PACKAGING".equals(materialType)) {
                    targetLocation = "PACKAGING_WAREHOUSE";
                } else if ("RAW_MATERIAL".equals(materialType) || "INTERMEDIATE".equals(materialType)) {
                    targetLocation = "RAW_MATERIAL_WAREHOUSE";
                }
            }

            if (targetLocation == null) {
                throw new SQLException("Unable to determine released location for material type: " + materialType);
            }

            // Update Stock_Inventory
            String updateSql = "UPDATE Stock_Inventory SET qc_status = ? " +
                    (targetLocation != null ? ", location_code = ? " : "") +
                    "WHERE batch_number = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setString(1, finalBatchStatus);
                pstmt.setString(2, targetLocation);
                pstmt.setString(3, batchNumber);
                pstmt.executeUpdate();
            }

            // Keep any production_batch mirror row aligned with the stock record.
            String updatePbSql = "UPDATE production_batch SET qc_status = ?, location_code = ? WHERE batch_number = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updatePbSql)) {
                pstmt.setString(1, finalBatchStatus);
                pstmt.setString(2, targetLocation);
                pstmt.setString(3, batchNumber);
                pstmt.executeUpdate();
            }

            if (productionOrderId != null && productionOrderId > 0) {
                String updateOrderSql = "UPDATE Production_Order SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE order_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateOrderSql)) {
                    pstmt.setString(1, ProductionOrder.ProductionStatus.fromString(newStatus).getDisplayName());
                    pstmt.setInt(2, productionOrderId);
                    pstmt.executeUpdate();
                }
            }

            // Create event log
            String insertEventSql = "INSERT INTO event_log (event_type, entity_type, entity_id, details, status) VALUES (?, 'BATCH', ?, ?, 'SUCCESS')";
            try (PreparedStatement evStmt = conn.prepareStatement(insertEventSql)) {
                evStmt.setString(1, "QC_" + finalBatchStatus.toUpperCase());
                evStmt.setString(2, batchNumber);
                evStmt.setString(3, "QC Status updated from " + oldStatus + " to " + finalBatchStatus);
                evStmt.executeUpdate();
            }

            logAuditTrail(conn, userId, "QC_STATUS_UPDATE", "Stock_Inventory", batchNumber, oldStatus, finalBatchStatus);

            conn.commit();

        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
            }
        }
    }

    public void takeSampleForQC(String batchNumber, int userId) throws SQLException, ClassNotFoundException {
        String selectSql = "SELECT qc_status FROM Stock_Inventory WHERE batch_number = ?";
        String updateSql = "UPDATE Stock_Inventory SET qc_status = ?, location_code = 'QC_HOLD' WHERE batch_number = ?";
        String updatePbSql = "UPDATE production_batch SET qc_status = ?, location_code = 'QC_HOLD' WHERE batch_number = ?";
        String insertEventSql = "INSERT INTO event_log (event_type, entity_type, entity_id, details, status) VALUES (?, 'BATCH', ?, ?, 'SUCCESS')";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                String currentStatus = null;
                try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
                    pstmt.setString(1, batchNumber);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            currentStatus = rs.getString("qc_status");
                        }
                    }
                }

                if (currentStatus == null) {
                    throw new SQLException("Batch not found: " + batchNumber);
                }

                String nextStatus;
                String eventType;
                String details;
                String auditAction;

                if ("IN_PRODUCTION".equalsIgnoreCase(currentStatus)) {
                    nextStatus = "IN_PROCESS_SAMPLE";
                    eventType = "IPQC_SAMPLE_TAKEN";
                    details = "IPQC sample taken. Batch moved from IN_PRODUCTION to IN_PROCESS_SAMPLE.";
                    auditAction = "IPQC_SAMPLE_TAKEN";
                } else if ("IN_PROCESS_SAMPLE".equalsIgnoreCase(currentStatus)) {
                    nextStatus = "UNDER_TEST";
                    eventType = "IPQC_TESTING_STARTED";
                    details = "IPQC testing started. Batch moved from IN_PROCESS_SAMPLE to UNDER_TEST.";
                    auditAction = "IPQC_TESTING_STARTED";
                } else if ("QUARANTINE".equalsIgnoreCase(currentStatus)) {
                    nextStatus = "QI";
                    eventType = "QC_SAMPLE_TAKEN";
                    details = "QC sample taken. Batch moved from QUARANTINE to QI.";
                    auditAction = "QC_SAMPLE_TAKEN";
                } else {
                    throw new SQLException(
                            "Batch " + batchNumber
                                    + " must be in IN_PRODUCTION, IN_PROCESS_SAMPLE, or QUARANTINE before sampling/testing can continue.");
                }

                try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                    pstmt.setString(1, nextStatus);
                    pstmt.setString(2, batchNumber);
                    pstmt.executeUpdate();
                }

                try (PreparedStatement pstmt = conn.prepareStatement(updatePbSql)) {
                    pstmt.setString(1, nextStatus);
                    pstmt.setString(2, batchNumber);
                    pstmt.executeUpdate();
                }

                try (PreparedStatement pstmt = conn.prepareStatement(insertEventSql)) {
                    pstmt.setString(1, eventType);
                    pstmt.setString(2, batchNumber);
                    pstmt.setString(3, details);
                    pstmt.executeUpdate();
                }

                logAuditTrail(conn, userId, auditAction, "Stock_Inventory", batchNumber, currentStatus, nextStatus);
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public List<Stock> getQCBatches(String statusFilter) {
        List<Stock> stocks = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT s.stock_id, s.material_code, d.brand_name, d.generic_name, d.manufacturer, " +
                        "s.location_code, s.batch_number, s.quantity, s.reserved_quantity, s.available_quantity, " +
                        "s.unit_cost, s.mfg_date, s.exp_date, s.qc_status, s.parent_batch_id " +
                        "FROM Stock_Inventory s JOIN Material_Master d ON s.material_code = d.material_code");

        if (!"All".equalsIgnoreCase(statusFilter)) {
            sql.append(" WHERE s.qc_status = ?");
        }
        sql.append(" ORDER BY s.mfg_date DESC, s.batch_number DESC");

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            if (!"All".equalsIgnoreCase(statusFilter)) {
                pstmt.setString(1, statusFilter);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    stocks.add(mapResultSetToStock(rs));
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error fetching QC batches: " + e.getMessage());
            e.printStackTrace();
        }
        return stocks;
    }

    public Stock getStockByBatchNumber(String batchNumber) {
        String sql = "SELECT s.stock_id, s.material_code, d.brand_name, d.generic_name, d.manufacturer, " +
                "s.location_code, s.batch_number, s.quantity, s.reserved_quantity, s.available_quantity, " +
                "s.unit_cost, s.mfg_date, s.exp_date, s.qc_status, s.parent_batch_id, s.production_order_id " +
                "FROM Stock_Inventory s JOIN Material_Master d ON s.material_code = d.material_code " +
                "WHERE s.batch_number = ?";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, batchNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Stock stock = mapResultSetToStock(rs);
                    stock.setProductionOrderId(rs.getInt("production_order_id"));
                    return stock;
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error fetching stock details for batch " + batchNumber + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private Stock mapResultSetToStock(ResultSet rs) throws SQLException {
        Stock stock = new Stock();
        stock.setStockId(rs.getInt("stock_id"));
        stock.setMaterialCode(rs.getString("material_code"));
        stock.setBrandName(rs.getString("brand_name"));
        stock.setGenericName(rs.getString("generic_name"));
        stock.setManufacturer(rs.getString("manufacturer"));
        stock.setLocationCode(rs.getString("location_code"));
        stock.setBatchNumber(rs.getString("batch_number"));
        stock.setQuantity(rs.getDouble("quantity"));
        stock.setReservedQuantity(readOptionalDouble(rs, "reserved_quantity"));
        stock.setAvailableQuantity(readOptionalDouble(rs, "available_quantity"));
        stock.setUnitCost(rs.getDouble("unit_cost"));
        java.sql.Date mfg = rs.getDate("mfg_date");
        if (mfg != null) {
            stock.setMfgDate(mfg.toLocalDate());
        }
        java.sql.Date exp = rs.getDate("exp_date");
        if (exp != null) {
            stock.setExpDate(exp.toLocalDate());
        }
        stock.setQcStatus(rs.getString("qc_status"));
        stock.setParentBatchId(rs.getString("parent_batch_id"));
        return stock;
    }

    private double readOptionalDouble(ResultSet rs, String columnName) throws SQLException {
        try {
            double value = rs.getDouble(columnName);
            return rs.wasNull() ? 0.0 : value;
        } catch (SQLException ex) {
            return 0.0;
        }
    }

    // --- Batch Genealogy and Traceability ---

    public List<String> getBatchGenealogy(String childBatchId) throws SQLException, ClassNotFoundException {
        List<String> parentBatches = new ArrayList<>();
        String sql = "SELECT parent_batch_id FROM Stock_Inventory WHERE batch_number = ?";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, childBatchId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String parents = rs.getString("parent_batch_id");
                    if (parents != null && !parents.isEmpty()) {
                        String[] batchArray = parents.split(",");
                        for (String batch : batchArray) {
                            parentBatches.add(batch.trim());
                        }
                    }
                }
            }
        }
        return parentBatches;
    }

    public List<Map<String, Object>> getRecallReport(String rawMaterialBatchId)
            throws SQLException, ClassNotFoundException {
        List<Map<String, Object>> affectedBatches = new ArrayList<>();

        String sql = "SELECT si.batch_number, si.material_code, dm.brand_name, si.quantity, si.qc_status, si.exp_date, si.location_code FROM Stock_Inventory si JOIN Material_Master dm ON si.material_code = dm.material_code WHERE si.parent_batch_id LIKE ?";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + rawMaterialBatchId + "%");

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> batch = new HashMap<>();
                    batch.put("batch_number", rs.getString("batch_number"));
                    batch.put("material_code", rs.getString("material_code"));
                    batch.put("brand_name", rs.getString("brand_name"));
                    batch.put("quantity", rs.getDouble("quantity"));
                    batch.put("qc_status", rs.getString("qc_status"));
                    batch.put("exp_date", rs.getDate("exp_date"));
                    batch.put("location_code", rs.getString("location_code"));
                    affectedBatches.add(batch);
                }
            }
        }
        return affectedBatches;
    }

    // --- Audit Trail Methods ---

    private void logAuditTrail(Connection conn, int userId, String actionType, String tableName, String recordId,
            String oldValue, String newValue) throws SQLException {
        String sql = "INSERT INTO System_Audit_Trail (user_id, action_type, table_name, record_id, old_value, new_value) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (userId <= 0) {
                pstmt.setNull(1, java.sql.Types.INTEGER);
            } else {
                pstmt.setInt(1, userId);
            }
            pstmt.setString(2, actionType);
            pstmt.setString(3, tableName);
            pstmt.setString(4, recordId);
            pstmt.setString(5, oldValue);
            pstmt.setString(6, newValue);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error logging audit trail: {}", e.getMessage(), e);
            throw e; // Re-throw to ensure transaction rollback if within one
        }
    }

    public void logAuditTrail(int userId, String actionType, String tableName, String recordId, String oldValue,
            String newValue) throws SQLException, ClassNotFoundException {
        try (Connection conn = getConnection()) {
            logAuditTrail(conn, userId, actionType, tableName, recordId, oldValue, newValue);
        }
    }

    public List<AuditTrail> getAuditTrail(String actionType, LocalDate startDate, LocalDate endDate)
            throws SQLException, ClassNotFoundException {
        List<AuditTrail> trails = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM System_Audit_Trail WHERE 1=1");

        if (actionType != null && !actionType.isEmpty()) {
            sql.append(" AND action_type = ?");
        }
        if (startDate != null) {
            sql.append(" AND DATE(timestamp) >= ?");
        }
        if (endDate != null) {
            sql.append(" AND DATE(timestamp) <= ?");
        }
        sql.append(" ORDER BY timestamp DESC LIMIT 1000");

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            if (actionType != null && !actionType.isEmpty()) {
                pstmt.setString(paramIndex++, actionType);
            }
            if (startDate != null) {
                pstmt.setDate(paramIndex++, java.sql.Date.valueOf(startDate));
            }
            if (endDate != null) {
                pstmt.setDate(paramIndex++, java.sql.Date.valueOf(endDate));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    trails.add(new AuditTrail(
                            rs.getInt("audit_id"),
                            rs.getInt("user_id"),
                            rs.getString("action_type"),
                            rs.getString("table_name"),
                            rs.getString("record_id"),
                            rs.getString("old_value"),
                            rs.getString("new_value"),
                            rs.getTimestamp("timestamp").toLocalDateTime(),
                            rs.getString("ip_address"),
                            rs.getString("notes")));
                }
            }
        }
        return trails;
    }

    // --- Material Methods (replacing Material methods) ---

    public List<Material> getAllMaterials() throws SQLException, ClassNotFoundException {
        List<Material> materials = new ArrayList<>();
        String sql = "SELECT * FROM Material_Master ORDER BY material_code";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                materials.add(mapResultSetToMaterial(rs));
            }
        }
        return materials;
    }

    private Material mapResultSetToMaterial(ResultSet rs) throws SQLException {
        return new Material(
                rs.getString("material_code"),
                rs.getString("brand_name"),
                rs.getString("generic_name"),
                rs.getString("manufacturer"),
                rs.getString("formulation"),
                rs.getString("strength"),
                rs.getString("schedule_category"),
                rs.getString("storage_conditions"),
                rs.getInt("reorder_level"),
                rs.getBoolean("is_active"),
                rs.getObject("preferred_supplier_id") != null ? rs.getInt("preferred_supplier_id") : null,
                Material.MaterialType.fromString(rs.getString("material_type")),
                Material.UnitOfMeasure.fromString(rs.getString("unit_of_measure")));
    }

    public void addMaterial(Material material) throws SQLException, ClassNotFoundException {
        String sql = "INSERT INTO Material_Master (material_code, brand_name, generic_name, manufacturer, formulation, strength, schedule_category, storage_conditions, reorder_level, is_active, preferred_supplier_id, material_type, unit_of_measure) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
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

            pstmt.setString(12, material.getMaterialType().name());
            pstmt.setString(13, material.getUnitOfMeasure().name());

            pstmt.executeUpdate();
        }
    }

    public List<Material> getMaterialsByType(Material.MaterialType type) throws SQLException, ClassNotFoundException {
        List<Material> materials = new ArrayList<>();
        String sql = "SELECT * FROM Material_Master WHERE material_type = ? ORDER BY material_code";

        try (Connection conn = getConnection();
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

    // =======================================================
    // --- MANUFACTURER-CENTRIC SCM (NEW DAO METHODS) ---
    // =======================================================

    public boolean addInventoryTransaction(InventoryTransaction tx) {
        String sql = "INSERT INTO inventory_transaction (material_code, batch_number, location_code, transaction_type, quantity, reference_type, reference_id, performed_by, notes) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, tx.getMaterialCode());
            pstmt.setString(2, tx.getBatchNumber());
            pstmt.setString(3, tx.getLocationCode());
            pstmt.setString(4, tx.getTransactionType());
            pstmt.setDouble(5, tx.getQuantity());
            pstmt.setString(6, tx.getReferenceType());
            pstmt.setString(7, tx.getReferenceId());
            pstmt.setInt(8, tx.getPerformedBy());
            pstmt.setString(9, tx.getNotes());
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next())
                        tx.setTransactionId(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean addMaterialConsumption(MaterialConsumption mc) {
        String sql = "INSERT INTO production_material_consumption (production_order_id, material_code, batch_number, required_qty, consumed_qty, uom) "
                +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, mc.getProductionOrderId());
            pstmt.setString(2, mc.getMaterialCode());
            pstmt.setString(3, mc.getBatchNumber());
            pstmt.setDouble(4, mc.getRequiredQty());
            pstmt.setDouble(5, mc.getConsumedQty());
            pstmt.setString(6, mc.getUom());
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next())
                        mc.setConsumptionId(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean addProductionBatch(ProductionBatch pb) {
        String sql = "INSERT INTO production_batch (production_order_id, material_code, batch_number, quantity, mfg_date, expiry_date, qc_status, location_code) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, pb.getProductionOrderId());
            pstmt.setString(2, pb.getMaterialCode());
            pstmt.setString(3, pb.getBatchNumber());
            pstmt.setDouble(4, pb.getQuantity());
            pstmt.setDate(5, pb.getMfgDate() != null ? java.sql.Date.valueOf(pb.getMfgDate()) : null);
            pstmt.setDate(6, pb.getExpiryDate() != null ? java.sql.Date.valueOf(pb.getExpiryDate()) : null);
            pstmt.setString(7, pb.getQcStatus());
            pstmt.setString(8, pb.getLocationCode());
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next())
                        pb.setBatchId(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean addBatchGenealogy(BatchGenealogy bg) {
        String sql = "INSERT INTO batch_genealogy (parent_batch, child_batch, production_order_id, relationship_type) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, bg.getParentBatch());
            pstmt.setString(2, bg.getChildBatch());
            pstmt.setInt(3, bg.getProductionOrderId());
            pstmt.setString(4, bg.getRelationshipType());
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next())
                        bg.setGenealogyId(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean addEventLog(EventLog el) {
        String sql = "INSERT INTO event_log (event_type, entity_type, entity_id, details, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, el.getEventType());
            pstmt.setString(2, el.getEntityType());
            pstmt.setString(3, el.getEntityId());
            pstmt.setString(4, el.getDetails());
            pstmt.setString(5, el.getStatus());
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next())
                        el.setEventId(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Stock> getDetailedInventoryReport() {
        List<Stock> stocks = new ArrayList<>();
        String sql = "SELECT s.stock_id, s.material_code, d.brand_name, d.generic_name, d.manufacturer, s.location_code, s.batch_number, s.quantity, s.reserved_quantity, s.available_quantity, s.unit_cost, s.mfg_date, s.exp_date, s.qc_status, s.parent_batch_id "
                +
                "FROM Stock_Inventory s JOIN Material_Master d ON s.material_code = d.material_code";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                stocks.add(mapResultSetToStock(rs));
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error fetching detailed inventory: " + e.getMessage());
            e.printStackTrace();
        }
        return stocks;
    }

    public List<MaterialConsumption> getMaterialConsumptionsForOrder(int orderId) {
        List<MaterialConsumption> consumptions = new ArrayList<>();
        String sql = "SELECT * FROM production_material_consumption WHERE production_order_id = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    MaterialConsumption mc = new MaterialConsumption();
                    mc.setConsumptionId(rs.getInt("consumption_id"));
                    mc.setProductionOrderId(rs.getInt("production_order_id"));
                    mc.setMaterialCode(rs.getString("material_code"));
                    mc.setBatchNumber(rs.getString("batch_number"));
                    mc.setRequiredQty(rs.getDouble("required_qty"));
                    mc.setConsumedQty(rs.getDouble("consumed_qty"));
                    mc.setUom(rs.getString("uom"));
                    java.sql.Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null)
                        mc.setCreatedAt(ts.toLocalDateTime());
                    consumptions.add(mc);
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error fetching consumptions: " + e.getMessage());
            e.printStackTrace();
        }
        return consumptions;
    }

}
