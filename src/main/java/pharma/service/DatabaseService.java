package pharma.service;

import pharma.model.*;
import pharma.model.GRN.GRNItem;
import pharma.model.PurchaseOrder.PurchaseOrderItem;
import pharma.config.DatabaseConfig;
import pharma.repository.jdbc.GRNJdbcRepository;
import pharma.repository.jdbc.PurchaseOrderJdbcRepository;
import pharma.repository.jdbc.StockJdbcRepository;

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
    // Phase 6: Stock/inventory persistence
    private final StockJdbcRepository stockRepository;
    // Phase 7: Production and QA persistence
    private final pharma.repository.jdbc.ProductionOrderJdbcRepository productionOrderRepository;
    private final pharma.repository.jdbc.QAJdbcRepository qaRepository;
    // Phase 8: Auth, Audit, Material, Reports
    private final pharma.repository.jdbc.UserJdbcRepository userRepo;
    private final pharma.repository.jdbc.RolePermissionJdbcRepository roleRepo;
    private final pharma.repository.jdbc.MaterialJdbcRepository materialRepo;
    private final pharma.repository.jdbc.AuditJdbcRepository auditRepo;

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
        this.stockRepository = new StockJdbcRepository(this);
        this.productionOrderRepository = new pharma.repository.jdbc.ProductionOrderJdbcRepository(this);
        this.qaRepository = new pharma.repository.jdbc.QAJdbcRepository(this);
        this.roleRepo = new pharma.repository.jdbc.RolePermissionJdbcRepository(this);
        this.userRepo = new pharma.repository.jdbc.UserJdbcRepository(this);
        this.materialRepo = new pharma.repository.jdbc.MaterialJdbcRepository(this);
        this.auditRepo = new pharma.repository.jdbc.AuditJdbcRepository(this);
        ensureOptionalSchema();
    }

    public pharma.repository.jdbc.UserJdbcRepository getUserRepository() {
        return userRepo;
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
        stockRepository.ensureStatusLocationConsistency(conn);

        try (Statement stmt = conn.createStatement()) {
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
    // --- MATERIAL CRUD OPERATIONS (DELEGATED TO MaterialJdbcRepository) ---
    // =======================================================

    public List<Material> getAllDrugs() {
        try {
            return materialRepo.getAllMaterials();
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public boolean addDrug(Material newDrug) throws SQLException {
        try {
            materialRepo.addMaterial(newDrug);
            return true;
        } catch (ClassNotFoundException e) {
            throw new SQLException(e);
        }
    }

    /**
     * Retrieves a single Material record using its primary key (materialCode).
     */
    public Material getDrugByMaterialCode(String materialCode) {
        try {
            return materialRepo.findByCode(materialCode).orElse(null);
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Updates an existing Material record based on its materialCode.
     */
    public boolean updateDrug(Material material) {
        String sql = "UPDATE Material_Master SET brand_name=?, generic_name=?, manufacturer=?, formulation=?, strength=?, schedule_category=?, storage_conditions=?, reorder_level=?, is_active=?, preferred_supplier_id=?, material_type=?, unit_of_measure=? WHERE material_code=?";
        try (Connection conn = getConnection();
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
            Integer supplierId = material.getPreferredSupplierId();
            if (supplierId != null) {
                pstmt.setInt(10, supplierId);
            } else {
                pstmt.setNull(10, java.sql.Types.INTEGER);
            }
            pstmt.setString(11, material.getMaterialType() != null ? material.getMaterialType().name() : null);
            pstmt.setString(12, material.getUnitOfMeasure() != null ? material.getUnitOfMeasure().name() : null);
            pstmt.setString(13, material.getMaterialCode());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteDrug(String materialCode) {
        String sql = "DELETE FROM Material_Master WHERE material_code = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, materialCode);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Material> getDrugs() {
        return getAllDrugs();
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
            double available = stockRepository.getAvailableStock(ingredient.getIngredientMaterialCode(), null);
            double shortage = Math.max(0, requiredQty - available);
            shortages.put(ingredient.getIngredientMaterialCode(), shortage);
        }

        return shortages;
    }

    // --- Production Order Management ---

    public int createProductionOrder(ProductionOrder order) throws SQLException, ClassNotFoundException {
        return productionOrderRepository.createProductionOrder(order);
    }

    public List<ProductionOrder> getAllProductionOrders() throws SQLException, ClassNotFoundException {
        return productionOrderRepository.getAllProductionOrders();
    }

    public ProductionOrder getProductionOrderById(int orderId) throws SQLException, ClassNotFoundException {
        return productionOrderRepository.getProductionOrderById(orderId);
    }

    public void updateProductionOrderStatus(int orderId, String newStatus) throws SQLException, ClassNotFoundException {
        productionOrderRepository.updateProductionOrderStatus(orderId, newStatus);
    }

    // --- Manufacturing Execution (CRITICAL ATOMIC TRANSACTION) ---

    public void executeProductionRun(int orderId, int userId) throws SQLException, ClassNotFoundException {
        productionOrderRepository.executeProductionRun(orderId, userId);
    }

    // --- QC Status Management ---

    public void updateQCStatus(String batchNumber, String newStatus, int userId) throws SQLException, ClassNotFoundException {
        qaRepository.updateQCStatus(batchNumber, newStatus, userId);
    }

    public void takeSampleForQC(String batchNumber, int userId) throws SQLException, ClassNotFoundException {
        qaRepository.takeSampleForQC(batchNumber, userId);
    }

    // =====================================================================
    // STOCK / INVENTORY METHODS - delegated to StockJdbcRepository (Phase 6)
    // =====================================================================

    public List<Stock> getQCBatches(String statusFilter) {
        return stockRepository.getAllStockByQcStatus(statusFilter);
    }

    public Stock getStockByBatchNumber(String batchNumber) {
        return stockRepository.getStockByBatch(batchNumber);
    }

    public List<String> getBatchGenealogy(String childBatchId) throws SQLException, ClassNotFoundException {
        return stockRepository.getParentBatch(childBatchId);
    }

    public List<Map<String, Object>> getRecallReport(String rawMaterialBatchId)
            throws SQLException, ClassNotFoundException {
        return stockRepository.getChildBatches(rawMaterialBatchId);
    }

    public List<Stock> getDetailedInventoryReport() {
        return stockRepository.getAllStock();
    }

    // --- Audit Trail Methods ---

    public void logAuditTrail(Connection conn, int userId, String actionType, String tableName, String recordId,
            String oldValue, String newValue) throws SQLException {
        auditRepo.logAuditTrail(conn, userId, actionType, tableName, recordId, oldValue, newValue);
    }

    public void logAuditTrail(int userId, String actionType, String tableName, String recordId, String oldValue,
            String newValue) throws SQLException, ClassNotFoundException {
        auditRepo.logAuditTrail(userId, actionType, tableName, recordId, oldValue, newValue);
    }

    public List<AuditTrail> getAuditTrail(String actionType, LocalDate startDate, LocalDate endDate)
            throws SQLException, ClassNotFoundException {
        return auditRepo.getAuditTrail(actionType, startDate, endDate);
    }

    // --- Material Methods (replacing Material methods) ---

    public List<Material> getAllMaterials() throws SQLException, ClassNotFoundException {
        return materialRepo.getAllMaterials();
    }

    public void addMaterial(Material material) throws SQLException, ClassNotFoundException {
        materialRepo.addMaterial(material);
    }

    public List<Material> getMaterialsByType(Material.MaterialType type) throws SQLException, ClassNotFoundException {
        return materialRepo.getMaterialsByType(type);
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
