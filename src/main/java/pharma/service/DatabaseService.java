package pharma.service;

import pharma.model.*;
import pharma.model.GRN.GRNItem;
import pharma.model.PurchaseOrder.PurchaseOrderItem;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import java.util.Date;

public class DatabaseService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);
    private static DatabaseService instance = null;
    private static HikariDataSource ds;

    static {
        Dotenv env = Dotenv.load();
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(env.get("DB_URL"));
        config.setUsername(env.get("DB_USER"));
        config.setPassword(env.get("DB_PASS"));

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setMaxLifetime(1800000);
        config.setConnectionTimeout(30000);

        ds = new HikariDataSource(config);
    }

    // *** FIX: Removed 'private Connection connection = null;' - connections should
    // not be long-lived fields.

    // Helper method to establish a fresh, single-use connection
    Connection getConnection() throws SQLException, ClassNotFoundException {
        return ds.getConnection();
    }

    public boolean connect() {
        try {
            getConnection().close();
            System.out.println("Database connection established successfully.");
            return true;
        } catch (ClassNotFoundException e) {
            System.err.println("JDBC Driver not found. Check your classpath.");
            e.printStackTrace();
            return false;
        } catch (SQLException e) {
            System.err.println("SQL Connection failed. Ensure MySQL is running and database exists.");
            e.printStackTrace();
            return false;
        }
    }

    public void disconnect() {
        System.out.println("Note: DatabaseService now manages connection lifecycle per operation.");
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

    public static void closePool() {
        if (ds != null && !ds.isClosed()) {
            ds.close();
        }
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
                Supplier supplier = new Supplier(
                        rs.getInt("supplier_id"),
                        rs.getString("supplier_name"),
                        rs.getString("contact_person"),
                        rs.getString("address"),
                        rs.getString("email"),
                        rs.getString("phone_number"),
                        rs.getString("gstin"),
                        rs.getString("drug_license_number"),
                        rs.getString("payment_terms"));
                suppliers.add(supplier);
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
        String sql = "DELETE FROM Supplier_Master WHERE supplier_id = ?";
        // *** FIX: Use try-with-resources on the Connection (conn) and
        // PreparedStatement.
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, supplierId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                logAuditTrail(conn, 0, "DELETE_SUPPLIER", "Supplier_Master", String.valueOf(supplierId), null,
                        "DELETED");
                return true;
            }
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

    public List<PurchaseOrder> getPurchaseOrders() {
        List<PurchaseOrder> orders = new ArrayList<>();
        String sql = "SELECT po.po_id, po.supplier_id, s.supplier_name, po.order_date, po.expected_date, po.total_amount, po.status "
                +
                "FROM Purchase_Order po JOIN Supplier_Master s ON po.supplier_id = s.supplier_id ORDER BY po.po_id DESC";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                orders.add(mapResultSetToPurchaseOrder(rs));
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error fetching all Purchase Orders: " + e.getMessage());
            e.printStackTrace();
        }
        return orders;
    }

    public String generateNextPoNumber() {
        // A simple UUID or sequence generation could replace this for a real
        // application.
        return "PO-" + LocalDate.now().getYear() + "-" + System.currentTimeMillis() % 10000;
    }

    public boolean createPurchaseOrder(PurchaseOrder po) throws ClassNotFoundException {
        // SQL to insert the PO Header and retrieve the auto-generated ID
        String sqlHeader = "INSERT INTO Purchase_Order (supplier_id, order_date, expected_date, total_amount, status) VALUES (?, ?, ?, ?, ?)";
        int newPoId = -1;

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // Start Transaction
            try (PreparedStatement pstmt = conn.prepareStatement(sqlHeader, Statement.RETURN_GENERATED_KEYS)) {

                // 1. Insert PO Header
                pstmt.setInt(1, po.getSupplierId());
                pstmt.setDate(2, java.sql.Date.valueOf(po.getOrderDate()));
                pstmt.setDate(3, java.sql.Date.valueOf(po.getExpectedDate()));
                pstmt.setDouble(4, po.getTotalAmount());
                pstmt.setString(5, po.getStatus());

                if (pstmt.executeUpdate() == 0) {
                    throw new SQLException("Creating PO header failed, no rows affected.");
                }

                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        newPoId = generatedKeys.getInt(1);
                        po.setId(newPoId); // Update the PO object with the new ID
                    } else {
                        throw new SQLException("Creating PO header failed, no ID obtained.");
                    }
                }

                // 2. Insert PO Items (Line Items)
                if (po.getItems() != null && !po.getItems().isEmpty()) {
                    // SQL uses 'drug_id' which references Material_Master.material_code (VARCHAR)
                    String sqlItems = "INSERT INTO PurchaseOrder_Item (po_id, drug_id, quantity, unit_price) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement pstmtItems = conn.prepareStatement(sqlItems)) {
                        for (PurchaseOrder.PurchaseOrderItem item : po.getItems()) {
                            pstmtItems.setInt(1, newPoId);

                            // 💡 CRITICAL FIX: Use setString() and the corrected getter
                            pstmtItems.setString(2, item.getMaterialCode()); // <-- Now uses String materialCode

                            pstmtItems.setInt(3, item.getQuantity());
                            pstmtItems.setDouble(4, item.getUnitPrice());
                            pstmtItems.addBatch();
                        }
                        pstmtItems.executeBatch();
                    }
                }

                // 3. Log Audit Trail (Inside Transaction)
                logAuditTrail(conn, 0, "CREATE_PO", "Purchase_Order", String.valueOf(newPoId), null,
                        "Total: " + po.getTotalAmount());

                conn.commit(); // Commit Transaction
                System.out.println("Purchase Order created successfully with ID: " + newPoId);
                return true;

            } catch (SQLException e) {
                conn.rollback(); // Rollback on error
                logger.error("Transaction Rollback due to SQL Error: {}", e.getMessage(), e);
                // Re-throw if a serious unrecoverable error occurred (like invalid SQL or
                // constraint violation)
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Database Error establishing connection for PO creation: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Corrected signature: returns PurchaseOrder, accepts String poId
    public PurchaseOrder getPurchaseOrderById(String poId) throws ClassNotFoundException {
        String sql = "SELECT po.po_id, po.supplier_id, po.order_date, po.expected_date, po.total_amount, po.status, s.supplier_name "
                +
                "FROM Purchase_Order po JOIN Supplier_Master s ON po.supplier_id = s.supplier_id WHERE po.po_id = ?";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, Integer.parseInt(poId));

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPurchaseOrder(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving Purchase Order " + poId + ": " + e.getMessage());
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.err.println("Invalid PO ID format: " + poId);
        }
        return null;
    }

    private GRNItem mapResultSetToGRNItem(ResultSet rs) throws SQLException {
        return new GRNItem(
                rs.getString("drug_id"), // Changed from getInt to getString
                rs.getString("batch_number"),
                rs.getInt("quantity_received"),
                rs.getDate("expiry_date").toLocalDate());
    }

    private List<GRNItem> getGRNItems(int grn_Id) throws SQLException, ClassNotFoundException {
        List<GRNItem> items = new ArrayList<>();
        String sql = "SELECT drug_id, batch_number, quantity_received, expiry_date FROM GRN_Item WHERE grn_id = ?";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, grn_Id);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToGRNItem(rs));
                }
            }
        }
        return items;
    }

    /**
     * Public wrapper method to get GRN items with exception handling
     */
    private List<GRNItem> getGRNItemsByGrnId(int grnId) {
        try {
            return getGRNItems(grnId);
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error fetching GRN items for GRN ID " + grnId + ": " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<GRN> getGRNs() {
        List<GRN> grns = new ArrayList<>();
        // CHANGE: Replaced INNER JOINs with LEFT JOINs to ensure all GRNs are returned.
        String sql = "SELECT g.grn_id, g.po_id, g.received_date, g.received_by, g.status, s.supplier_name " +
                "FROM Goods_Received_Note g " +
                "LEFT JOIN Purchase_Order po ON g.po_id = po.po_id " +
                "LEFT JOIN Supplier_Master s ON po.supplier_id = s.supplier_id";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("grn_id");
                int poId = rs.getInt("po_id");
                java.sql.Timestamp receivedTs = rs.getTimestamp("received_date");
                LocalDateTime received_Date = receivedTs != null ? receivedTs.toLocalDateTime() : null;
                String received_By = rs.getString("received_by");
                String status = rs.getString("status");
                String supplierName = rs.getString("supplier_name"); // This will be null if no match is found
                GRN grn = new GRN(id, supplierName, poId, received_Date, received_By, status, List.of());
                grns.add(grn);
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error fetching GRNs: " + e.getMessage());
        }
        return grns;
    }

    public boolean createGRNFromPO(PurchaseOrder po) {
        System.out.println("Creating GRN for PO: " + po.getPoNumber() + " (ID: " + po.getId() + ")");

        // SQL statements
        String insertGrnSql = "INSERT INTO Goods_Received_Note (po_id, received_date, received_by, status) VALUES (?, NOW(), ?, ?)";
        String insertGrnItemSql = "INSERT INTO GRN_Item (grn_id, drug_id, batch_number, quantity_received, expiry_date) VALUES (?, ?, ?, ?, ?)";
        String insertOrUpdateStockSql = "INSERT INTO Stock_Inventory (material_code, location_code, batch_number, quantity, unit_cost, mfg_date, exp_date) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE quantity = quantity + VALUES(quantity)";
        String insertInventoryTransactionSql = "INSERT INTO inventory_transaction (material_code, batch_number, location_code, transaction_type, quantity, reference_type, reference_id, performed_by, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String insertEventLogSql = "INSERT INTO event_log (event_type, entity_type, entity_id, details, status) VALUES (?, ?, ?, ?, ?)";
        String updatePoStatusSql = "UPDATE Purchase_Order SET status = ? WHERE po_id = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // Start transaction

            try {
                // 1. Create GRN Header
                int grnId;
                try (PreparedStatement pstmt = conn.prepareStatement(insertGrnSql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setInt(1, po.getId());
                    pstmt.setString(2, "admin"); // Default user, can be parameterized
                    pstmt.setString(3, "Verified");

                    int rowsAffected = pstmt.executeUpdate();
                    if (rowsAffected == 0) {
                        throw new SQLException("Creating GRN failed, no rows affected.");
                    }

                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            grnId = generatedKeys.getInt(1);
                            System.out.println("Created GRN with ID: " + grnId);
                        } else {
                            throw new SQLException("Creating GRN failed, no ID obtained.");
                        }
                    }
                }

                // 2. Get PO items and create GRN items + update inventory
                List<PurchaseOrderItem> poItems = getPurchaseOrderItems(po.getId());

                if (poItems == null || poItems.isEmpty()) {
                    System.out.println("Warning: No items found for PO " + po.getId());
                    conn.rollback();
                    return false;
                }

                try (PreparedStatement grnItemStmt = conn.prepareStatement(insertGrnItemSql);
                        PreparedStatement stockStmt = conn.prepareStatement(insertOrUpdateStockSql);
                        PreparedStatement txStmt = conn.prepareStatement(insertInventoryTransactionSql);
                        PreparedStatement eventStmt = conn.prepareStatement(insertEventLogSql)) {

                    for (PurchaseOrderItem item : poItems) {
                        // Generate batch number (in real app, this would be from user input)
                        String batchNumber = "BATCH-" + System.currentTimeMillis() + "-" + item.getMaterialCode();
                        LocalDate expiryDate = LocalDate.now().plusYears(2); // Default 2 years expiry
                        LocalDate mfgDate = LocalDate.now();

                        // Insert GRN Item
                        grnItemStmt.setInt(1, grnId);
                        grnItemStmt.setString(2, item.getMaterialCode());
                        grnItemStmt.setString(3, batchNumber);
                        grnItemStmt.setInt(4, item.getQuantity());
                        grnItemStmt.setDate(5, Date.valueOf(expiryDate));
                        grnItemStmt.addBatch();

                        // Insert/Update Stock Inventory (default location: RAW_MATERIAL_WAREHOUSE)
                        stockStmt.setString(1, item.getMaterialCode());
                        stockStmt.setString(2, "RAW_MATERIAL_WAREHOUSE"); // Default location
                        stockStmt.setString(3, batchNumber);
                        stockStmt.setInt(4, item.getQuantity());
                        stockStmt.setDouble(5, item.getUnitPrice());
                        stockStmt.setDate(6, Date.valueOf(mfgDate));
                        stockStmt.setDate(7, Date.valueOf(expiryDate));
                        stockStmt.addBatch();

                        // Add Inventory Transaction
                        txStmt.setString(1, item.getMaterialCode());
                        txStmt.setString(2, batchNumber);
                        txStmt.setString(3, "RAW_MATERIAL_WAREHOUSE");
                        txStmt.setString(4, "GRN_RECEIPT");
                        txStmt.setDouble(5, item.getQuantity());
                        txStmt.setString(6, "GRN");
                        txStmt.setString(7, String.valueOf(grnId));
                        txStmt.setInt(8, 1); // System Admin ID
                        txStmt.setString(9, "Received from PO " + po.getPoNumber());
                        txStmt.addBatch();

                        System.out.println("Added GRN item: Material=" + item.getMaterialCode() +
                                ", Qty=" + item.getQuantity() +
                                ", Batch=" + batchNumber);
                    }

                    // Execute batches
                    grnItemStmt.executeBatch();
                    stockStmt.executeBatch();
                    txStmt.executeBatch();

                    // Generate Event Log
                    eventStmt.setString(1, "MATERIAL_RECEIVED");
                    eventStmt.setString(2, "GRN");
                    eventStmt.setString(3, String.valueOf(grnId));
                    eventStmt.setString(4, "Received materials for PO " + po.getPoNumber());
                    eventStmt.setString(5, "SUCCESS");
                    eventStmt.executeUpdate();
                }

                // 3. Update PO status to "Received"
                try (PreparedStatement pstmt = conn.prepareStatement(updatePoStatusSql)) {
                    pstmt.setString(1, "Received");
                    pstmt.setInt(2, po.getId());
                    pstmt.executeUpdate();
                }

                conn.commit(); // Commit transaction
                System.out.println("GRN creation successful for PO " + po.getId());
                return true;

            } catch (SQLException e) {
                conn.rollback(); // Rollback on error
                System.err.println("Error creating GRN, transaction rolled back: " + e.getMessage());
                e.printStackTrace();
                return false;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Database connection error during GRN creation: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private PurchaseOrder mapResultSetToPurchaseOrder(ResultSet rs) throws SQLException, ClassNotFoundException {
        int id = rs.getInt("po_id");
        int supplierId = rs.getInt("supplier_id");

        // Try to get supplierName from the joined query first
        String supplierName = null;
        try {
            supplierName = rs.getString("supplier_name");
        } catch (SQLException e) {
            // ignore if column not found, handle below
        }

        // Fallback: If supplier_name wasn't included in the result set, fetch it.
        if (supplierName == null) {
            Supplier s = getSupplierById(supplierId);
            supplierName = (s != null) ? s.getSupplierName() : "Unknown Supplier";
        }

        LocalDate orderDate = rs.getDate("order_date").toLocalDate();
        LocalDate expectedDate = rs.getDate("expected_date").toLocalDate();

        double totalAmount = rs.getDouble("total_amount");
        String status = rs.getString("status");

        List<PurchaseOrderItem> items = getPurchaseOrderItems(id);

        return new PurchaseOrder(
                id,
                supplierId,
                supplierName,
                orderDate,
                expectedDate,
                totalAmount,
                status,
                items);
    }

    private List<PurchaseOrderItem> getPurchaseOrderItems(int poId) throws SQLException, ClassNotFoundException {
        List<PurchaseOrderItem> items = new ArrayList<>();
        String sql = "SELECT drug_id, quantity, unit_price FROM PurchaseOrder_Item WHERE po_id = ?";

        System.out.println("DEBUG: Fetching items for PO ID: " + poId);

        // Use the getConnection() helper method established in DatabaseService
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, poId);
            try (ResultSet rs = pstmt.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    String drugId = rs.getString("drug_id");
                    int quantity = rs.getInt("quantity");
                    double unitPrice = rs.getDouble("unit_price");

                    System.out.println(
                            "DEBUG: Found item - Material ID: " + drugId + ", Qty: " + quantity + ", Price: " + unitPrice);

                    items.add(new PurchaseOrderItem(
                            // 💡 FIX 1: Read the material_code (drug_id) as a String
                            drugId,

                            // Read quantity and unit_price as intended
                            quantity,
                            unitPrice));
                    count++;
                }
                System.out.println("DEBUG: Total items found for PO " + poId + ": " + count);
            }
        }
        // Note: No explicit catch block here, allowing calling methods (like
        // mapResultSetToPurchaseOrder) to handle the exception.
        return items;
    }

    public Supplier getSupplierById(int supplierId) throws ClassNotFoundException {
        String sql = "SELECT * FROM Supplier_Master WHERE supplier_id = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, supplierId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Supplier(
                            rs.getInt("supplier_id"),
                            rs.getString("supplier_name"),
                            rs.getString("contact_person"),
                            rs.getString("address"),
                            rs.getString("email"),
                            rs.getString("phone_number"),
                            rs.getString("gstin"),
                            rs.getString("drug_license_number"),
                            rs.getString("payment_terms"));
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

    public boolean createPurchaseOrder(String selectedSupplierName) {
        // 💡 FIX: Implemented the wrapper method
        try {
            int supplierId = getSupplierIdByName(selectedSupplierName);
            if (supplierId == -1)
                return false;

            PurchaseOrder minimalPo = new PurchaseOrder(
                    supplierId,
                    selectedSupplierName,
                    LocalDate.now(),
                    LocalDate.now().plusDays(7),
                    0.00,
                    "Pending",
                    new java.util.ArrayList<>());

            return createPurchaseOrder(minimalPo);

        } catch (Exception e) {
            System.err.println("Error in simple PO creation wrapper: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void deletePurchaseOrder(int orderId) throws SQLException {
        String deleteItemsSql = "DELETE FROM PurchaseOrder_Item WHERE po_id = ?";
        String deleteOrderSql = "DELETE FROM Purchase_Order WHERE po_id = ?";
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (
                    PreparedStatement itemsStmt = conn.prepareStatement(deleteItemsSql);
                    PreparedStatement orderStmt = conn.prepareStatement(deleteOrderSql)) {
                itemsStmt.setInt(1, orderId);
                itemsStmt.executeUpdate();

                orderStmt.setInt(1, orderId);
                orderStmt.executeUpdate();

                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                System.err.println("Delete PurchaseOrder rollback: " + ex.getMessage());
                throw ex; // propagate for upper level error handling if needed
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Delete PurchaseOrder error: " + e.getMessage());
            throw new SQLException("Database driver not found: " + e.getMessage());
        }
    }

    public void receivePurchaseOrderShipment(int orderId) {
        String updateStatusSql = "UPDATE Purchase_Order SET status = ? WHERE po_id = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(updateStatusSql)) {
            pstmt.setString(1, "Received");
            pstmt.setInt(2, orderId);
            pstmt.executeUpdate();
            logAuditTrail(conn, 0, "RECEIVE_PO", "Purchase_Order", String.valueOf(orderId), "Pending", "Received");
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error marking PO as received: " + e.getMessage());
        }
    }

    public boolean updatePurchaseOrder(PurchaseOrder updatedPo) {
        String updateHeaderSql = "UPDATE Purchase_Order SET supplier_id = ?, order_date = ?, expected_date = ?, total_amount = ?, status = ? WHERE po_id = ?";
        String deleteItemsSql = "DELETE FROM PurchaseOrder_Item WHERE po_id = ?";
        String insertItemSql = "INSERT INTO PurchaseOrder_Item (po_id, drug_id, quantity, unit_price) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (
                    PreparedStatement headerStmt = conn.prepareStatement(updateHeaderSql);
                    PreparedStatement deleteItemsStmt = conn.prepareStatement(deleteItemsSql);
                    PreparedStatement insertItemStmt = conn.prepareStatement(insertItemSql)) {
                // Update PO Header
                headerStmt.setInt(1, updatedPo.getSupplierId());
                headerStmt.setDate(2, java.sql.Date.valueOf(updatedPo.getOrderDate()));
                headerStmt.setDate(3, java.sql.Date.valueOf(updatedPo.getExpectedDate()));
                headerStmt.setDouble(4, updatedPo.getTotalAmount());
                headerStmt.setString(5, updatedPo.getStatus());
                headerStmt.setInt(6, updatedPo.getId());
                headerStmt.executeUpdate();

                // Delete old items
                deleteItemsStmt.setInt(1, updatedPo.getId());
                deleteItemsStmt.executeUpdate();

                // Insert new items
                for (PurchaseOrder.PurchaseOrderItem item : updatedPo.getItems()) {
                    insertItemStmt.setInt(1, updatedPo.getId());
                    insertItemStmt.setString(2, item.getMaterialCode());
                    insertItemStmt.setInt(3, item.getQuantity());
                    insertItemStmt.setDouble(4, item.getUnitPrice());
                    insertItemStmt.addBatch();
                }
                insertItemStmt.executeBatch();

                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                System.err.println("Update PO rollback: " + ex.getMessage());
                return false;
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Update PO error: " + e.getMessage());
            return false;
        }
    }

    public GRN getGRNById(int grn_Id) {
        // Fixed SQL: Use correct table name and JOIN to get supplier_name
        String sql = "SELECT g.grn_id, g.po_id, g.received_date, g.received_by, g.status, s.supplier_name " +
                "FROM Goods_Received_Note g " +
                "LEFT JOIN Purchase_Order po ON g.po_id = po.po_id " +
                "LEFT JOIN Supplier_Master s ON po.supplier_id = s.supplier_id " +
                "WHERE g.grn_id = ?";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, grn_Id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int grn_id = rs.getInt("grn_id");
                    int po_Id = rs.getInt("po_id");
                    String supplier_Name = rs.getString("supplier_name");
                    java.sql.Timestamp receivedTs = rs.getTimestamp("received_date");
                    String received_By = rs.getString("received_by");
                    String status = rs.getString("status");

                    LocalDateTime received_Date = receivedTs != null ? receivedTs.toLocalDateTime() : null;

                    // Load GRN items
                    List<GRN.GRNItem> items = getGRNItemsByGrnId(grn_Id);

                    return new GRN(grn_id, supplier_Name, po_Id, received_Date, received_By, status, items);
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error fetching GRN by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // =====================================================================
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

            String sql = "SELECT SUM(quantity) as available FROM Stock_Inventory WHERE material_code = ? AND qc_status = 'RELEASED'";

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

                String selectSql = "SELECT stock_id, batch_number, quantity, exp_date FROM Stock_Inventory WHERE material_code = ? AND qc_status = 'RELEASED' AND quantity > 0 ORDER BY exp_date ASC, stock_id ASC";

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

            String insertStockSql = "INSERT INTO Stock_Inventory (material_code, location_code, batch_number, quantity, unit_cost, mfg_date, exp_date, qc_status, parent_batch_id, production_order_id) VALUES (?, ?, ?, ?, ?, ?, ?, 'QUARANTINE', ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(insertStockSql)) {
                pstmt.setString(1, bom.getMaterialCode());
                pstmt.setString(2, "PRODUCTION_FLOOR");
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
            String insertPbSql = "INSERT INTO production_batch (production_order_id, material_code, batch_number, quantity, mfg_date, expiry_date, qc_status, location_code) VALUES (?, ?, ?, ?, ?, ?, 'QUARANTINE', 'PRODUCTION_FLOOR')";
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

            String updateOrderSql = "UPDATE Production_Order SET status = 'Quality-Testing', actual_qty = ?, completed_date = CURDATE() WHERE order_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateOrderSql)) {
                pstmt.setDouble(1, order.getPlannedQty());
                pstmt.setInt(2, orderId);
                pstmt.executeUpdate();
            }

            logAuditTrail(conn, userId, "PRODUCTION_RUN", "Production_Order", String.valueOf(orderId), "Planned",
                    "Quality-Testing");

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

            String selectSql = "SELECT qc_status FROM Stock_Inventory WHERE batch_number = ?";
            String oldStatus = null;

            try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
                pstmt.setString(1, batchNumber);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        oldStatus = rs.getString("qc_status");
                    }
                }
            }

            String updateSql = "UPDATE Stock_Inventory SET qc_status = ? WHERE batch_number = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setString(1, newStatus);
                pstmt.setString(2, batchNumber);
                pstmt.executeUpdate();
            }

            // Also update production_batch if applicable
            String updatePbSql = "UPDATE production_batch SET qc_status = ? WHERE batch_number = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updatePbSql)) {
                pstmt.setString(1, newStatus);
                pstmt.setString(2, batchNumber);
                pstmt.executeUpdate();
            }

            // Create event log
            String insertEventSql = "INSERT INTO event_log (event_type, entity_type, entity_id, details, status) VALUES (?, 'BATCH', ?, ?, 'SUCCESS')";
            try (PreparedStatement evStmt = conn.prepareStatement(insertEventSql)) {
                evStmt.setString(1, "QC_" + newStatus.toUpperCase());
                evStmt.setString(2, batchNumber);
                evStmt.setString(3, "QC Status updated from " + oldStatus + " to " + newStatus);
                evStmt.executeUpdate();
            }

            logAuditTrail(conn, userId, "QC_STATUS_UPDATE", "Stock_Inventory", batchNumber, oldStatus, newStatus);

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
        String sql = "SELECT s.stock_id, s.material_code, d.brand_name, d.generic_name, s.location_code, s.batch_number, s.quantity, s.reserved_quantity, s.available_quantity, s.unit_cost, s.mfg_date, s.exp_date, s.qc_status, s.parent_batch_id "
                +
                "FROM Stock_Inventory s JOIN Material_Master d ON s.material_code = d.material_code";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Stock stock = new Stock();
                stock.setStockId(rs.getInt("stock_id"));
                stock.setMaterialCode(rs.getString("material_code"));
                // We use the brand_name and generic_name fields directly populated from join
                stock.setBrandName(rs.getString("brand_name"));
                stock.setGenericName(rs.getString("generic_name"));
                stock.setLocationCode(rs.getString("location_code"));
                stock.setBatchNumber(rs.getString("batch_number"));
                stock.setQuantity(rs.getDouble("quantity"));
                stock.setReservedQuantity(rs.getDouble("reserved_quantity"));
                stock.setAvailableQuantity(rs.getDouble("available_quantity"));
                stock.setUnitCost(rs.getDouble("unit_cost"));
                java.sql.Date mfg = rs.getDate("mfg_date");
                if (mfg != null)
                    stock.setMfgDate(mfg.toLocalDate());
                java.sql.Date exp = rs.getDate("exp_date");
                if (exp != null)
                    stock.setExpDate(exp.toLocalDate());
                stock.setQcStatus(rs.getString("qc_status"));
                stock.setParentBatchId(rs.getString("parent_batch_id"));
                stocks.add(stock);
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
