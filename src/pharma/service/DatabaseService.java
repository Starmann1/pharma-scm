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
//import java.util.Date;

public class DatabaseService {

    private static DatabaseService instance = null;

    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/pharma_ims?allowPublicKeyRetrieval=true&useSSL=false";
    // OR, if you use a newer connector and still have issues:
    // "jdbc:mysql://localhost:3306/pharma_ims?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "SiriusBlack@369";
    private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";

    // *** FIX: Removed 'private Connection connection = null;' - connections should
    // not be long-lived fields.

    // Helper method to establish a fresh, single-use connection
    Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName(JDBC_DRIVER);
        return DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);
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
        // NOTE: In a real app, 'password_hash' should be used with hashing, not raw
        // password.
        String sql = "SELECT user_id, username, role FROM User_Master WHERE username = ? AND password_hash = ?";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("role"));
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Authentication Query Error: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public static DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
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
            return affectedRows > 0;
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
            return affectedRows > 0;
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
    // --- DRUG CRUD OPERATIONS (BASED ON PREVIOUS REQUEST) ---
    // =======================================================

    private Drug mapResultSetToDrug(ResultSet rs) throws SQLException {
        Drug drug = new Drug();
        drug.setMaterialCode(rs.getString("material_code"));
        drug.setBrandName(rs.getString("brand_name"));
        drug.setGenericName(rs.getString("generic_name"));
        drug.setManufacturer(rs.getString("manufacturer"));
        drug.setFormulation(rs.getString("formulation"));
        drug.setStrength(rs.getString("strength"));
        drug.setScheduleCategory(rs.getString("schedule_category"));
        drug.setStorageConditions(rs.getString("storage_conditions"));
        drug.setReorderLevel(rs.getInt("reorder_level"));
        drug.setActive(rs.getBoolean("is_active"));

        int preferredSupplierId = rs.getInt("preferred_supplier_id");
        if (rs.wasNull()) { // Checks if the last value read was SQL NULL
            drug.setPreferredSupplierId(null);
        } else {
            drug.setPreferredSupplierId(preferredSupplierId);
        }
        return drug;
    }

    public List<Drug> getAllDrugs() {
        List<Drug> drugs = new ArrayList<>();
        String SQL = "SELECT material_code, brand_name, generic_name, manufacturer, formulation, strength, schedule_category, storage_conditions, reorder_level, is_active, preferred_supplier_id FROM Drug_Master";

        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(SQL)) {

            while (rs.next()) {
                drugs.add(mapResultSetToDrug(rs));
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error fetching all drugs: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
        return drugs;
    }

    public boolean addDrug(Drug newDrug) throws SQLException {
        String SQL = "INSERT INTO Drug_Master (material_code, brand_name, generic_name, manufacturer, formulation, strength, schedule_category, storage_conditions, reorder_level, is_active, preferred_supplier_id) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

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

            int affectedRows = stmt.executeUpdate();
            System.out.println("Drug added successfully: " + newDrug.getBrandName());
            return affectedRows > 0; // <-- FIXED to return boolean result

        } catch (SQLException e) {
            System.err.println("Database Error inserting drug: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } catch (ClassNotFoundException e) {
            System.err.println("JDBC Driver not found: " + e.getMessage());
            e.printStackTrace();
            throw new SQLException("JDBC Driver not found.", e);
        }
    }

    /**
     * Retrieves a single Drug record using its primary key (materialCode).
     */
    public Drug getDrugByMaterialCode(String materialCode) {
        String sql = "SELECT material_code, brand_name, generic_name, manufacturer, formulation, strength, schedule_category, storage_conditions, reorder_level, is_active, preferred_supplier_id FROM Drug_Master WHERE material_code = ?";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, materialCode);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToDrug(rs); // <-- FIXED: Use helper
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error fetching drug by code: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Updates an existing Drug record based on its materialCode.
     */
    public boolean updateDrug(Drug drug) {
        // Updated SQL to include 'preferred_supplier_id' in the SET clause
        String sql = "UPDATE Drug_Master SET brand_name=?, generic_name=?, manufacturer=?, formulation=?, strength=?, schedule_category=?, storage_conditions=?, reorder_level=?, is_active=?, preferred_supplier_id=? WHERE material_code=?";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Existing parameters (1-9)
            pstmt.setString(1, drug.getBrandName());
            pstmt.setString(2, drug.getGenericName());
            pstmt.setString(3, drug.getManufacturer());
            pstmt.setString(4, drug.getFormulation());
            pstmt.setString(5, drug.getStrength());
            pstmt.setString(6, drug.getScheduleCategory());
            pstmt.setString(7, drug.getStorageConditions());
            pstmt.setInt(8, drug.getReorderLevel());
            pstmt.setBoolean(9, drug.isActive());

            // FIX: New parameter (10) for the foreign key
            Integer supplierId = drug.getPreferredSupplierId();
            if (supplierId != null) {
                pstmt.setInt(10, supplierId);
            } else {
                pstmt.setNull(10, java.sql.Types.INTEGER);
            }

            // WHERE clause parameter (11)
            pstmt.setString(11, drug.getMaterialCode());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error updating drug: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteDrug(String materialCode) { // <--- MODIFIED METHOD SIGNATURE AND LOGIC
        // FIX: Corrected table name to Drug_Master
        String sql = "DELETE FROM Drug_Master WHERE material_code = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, materialCode);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException | ClassNotFoundException e) { // Combined catch block
            System.err.println("Error deleting drug: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public List<Drug> getDrugs() {
        List<Drug> drugs = new ArrayList<>();

        // FIX: Updated SQL query to explicitly select 'preferred_supplier_id' (11
        // columns total)
        String sql = "SELECT material_code, brand_name, generic_name, manufacturer, formulation, strength, schedule_category, storage_conditions, reorder_level, is_active, preferred_supplier_id FROM Drug_Master";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Drug drug = new Drug(
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
                        rs.getInt("preferred_supplier_id") // <-- NEW: 11th parameter added
                );
                drugs.add(drug);
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error fetching all drugs: " + e.getMessage());
            e.printStackTrace();
        }
        return drugs;
    }

    public boolean editDrug(Drug drug) {
        return updateDrug(drug);
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
     * FIX: Implemented to retrieve the full inventory data (Drug Master Data).
     * 
     * @return List of Drug objects.
     */
    public List<Drug> getFullInventoryReport() {
        try {
            System.out.println("Retrieving full inventory (Drug Master Data)...");
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
                    // SQL uses 'drug_id' which references Drug_Master.material_code (VARCHAR)
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

                conn.commit(); // Commit Transaction
                System.out.println("Purchase Order created successfully with ID: " + newPoId);
                return true;

            } catch (SQLException e) {
                conn.rollback(); // Rollback on error
                System.err.println("Transaction Rollback due to SQL Error: " + e.getMessage());
                e.printStackTrace();
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
                        PreparedStatement stockStmt = conn.prepareStatement(insertOrUpdateStockSql)) {

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

                        // Insert/Update Stock Inventory (default location: LOC-A1)
                        stockStmt.setString(1, item.getMaterialCode());
                        stockStmt.setString(2, "LOC-A1"); // Default location, can be parameterized
                        stockStmt.setString(3, batchNumber);
                        stockStmt.setInt(4, item.getQuantity());
                        stockStmt.setDouble(5, item.getUnitPrice());
                        stockStmt.setDate(6, Date.valueOf(mfgDate));
                        stockStmt.setDate(7, Date.valueOf(expiryDate));
                        stockStmt.addBatch();

                        System.out.println("Added GRN item: Drug=" + item.getMaterialCode() +
                                ", Qty=" + item.getQuantity() +
                                ", Batch=" + batchNumber);
                    }

                    // Execute batches
                    grnItemStmt.executeBatch();
                    stockStmt.executeBatch();
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
                            "DEBUG: Found item - Drug ID: " + drugId + ", Qty: " + quantity + ", Price: " + unitPrice);

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

    public void deletePurchaseOrder(int orderId) {
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
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Delete PurchaseOrder error: " + e.getMessage());
        }
    }

    public void receivePurchaseOrderShipment(int orderId) {
        String updateStatusSql = "UPDATE Purchase_Order SET status = ? WHERE po_id = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(updateStatusSql)) {
            pstmt.setString(1, "Received");
            pstmt.setInt(2, orderId);
            pstmt.executeUpdate();
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
                    return rs.getInt(1);
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
                pstmt.setString(2, "LOC-A1");
                pstmt.setString(3, order.getBatchNumber());
                pstmt.setDouble(4, order.getPlannedQty());
                pstmt.setDouble(5, 0.0);
                pstmt.setDate(6, java.sql.Date.valueOf(LocalDate.now()));
                pstmt.setDate(7, java.sql.Date.valueOf(LocalDate.now().plusYears(2)));
                pstmt.setString(8, parentBatches.toString());
                pstmt.setInt(9, orderId);
                pstmt.executeUpdate();
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

        String sql = "SELECT si.batch_number, si.material_code, dm.brand_name, si.quantity, si.qc_status, si.exp_date, si.location_code FROM Stock_Inventory si JOIN Drug_Master dm ON si.material_code = dm.material_code WHERE si.parent_batch_id LIKE ?";

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
            pstmt.setInt(1, userId);
            pstmt.setString(2, actionType);
            pstmt.setString(3, tableName);
            pstmt.setString(4, recordId);
            pstmt.setString(5, oldValue);
            pstmt.setString(6, newValue);
            pstmt.executeUpdate();
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

    // --- Material Methods (replacing Drug methods) ---

    public List<Material> getAllMaterials() throws SQLException, ClassNotFoundException {
        List<Material> materials = new ArrayList<>();
        String sql = "SELECT * FROM Drug_Master ORDER BY material_code";

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
        String sql = "INSERT INTO Drug_Master (material_code, brand_name, generic_name, manufacturer, formulation, strength, schedule_category, storage_conditions, reorder_level, is_active, preferred_supplier_id, material_type, unit_of_measure) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

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
        String sql = "SELECT * FROM Drug_Master WHERE material_type = ? ORDER BY material_code";

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

}
