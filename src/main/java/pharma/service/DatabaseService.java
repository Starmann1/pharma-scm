package pharma.service;

import pharma.model.*;
import pharma.model.PurchaseOrder.PurchaseOrderItem;
import pharma.config.DatabaseConfig;
import pharma.repository.jdbc.GRNJdbcRepository;
import pharma.repository.jdbc.PurchaseOrderJdbcRepository;
import pharma.repository.jdbc.StockJdbcRepository;

import java.sql.*;
import java.time.LocalDate;
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
    // Phase 8: Auth, Audit, Material, Reports, Supplier
    private final pharma.repository.jdbc.UserJdbcRepository userRepo;
    private final pharma.repository.jdbc.RolePermissionJdbcRepository roleRepo;
    private final pharma.repository.jdbc.MaterialJdbcRepository materialRepo;
    private final pharma.repository.jdbc.AuditJdbcRepository auditRepo;
    private final pharma.repository.jdbc.SupplierJdbcRepository supplierRepo;
    private final pharma.repository.jdbc.LocationJdbcRepository locationRepo;
    private final pharma.repository.jdbc.BOMJdbcRepository bomRepo;

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
        this.supplierRepo = new pharma.repository.jdbc.SupplierJdbcRepository(this);
        this.locationRepo = new pharma.repository.jdbc.LocationJdbcRepository(this);
        this.bomRepo = new pharma.repository.jdbc.BOMJdbcRepository(this);
        ensureOptionalSchema();
    }

    public pharma.repository.jdbc.UserJdbcRepository getUserRepository() {
        return userRepo;
    }

    public pharma.repository.jdbc.RolePermissionJdbcRepository getRoleRepository() {
        return roleRepo;
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

        // PostgreSQL-compatible UPDATE with JOIN via subquery (no MySQL-style UPDATE...JOIN)
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                    "UPDATE production_batch pb " +
                            "SET qc_status = CASE " +
                            "    WHEN pb.qc_status = 'APPROVED' AND mm.material_type = 'FINISHED_GOOD' THEN 'RELEASED' " +
                            "    ELSE pb.qc_status " +
                            "END, " +
                            "location_code = CASE " +
                            "    WHEN pb.qc_status = 'REJECTED' THEN 'REJECTED_AREA' " +
                            "    WHEN pb.qc_status = 'IN_PRODUCTION' THEN 'PRODUCTION_FLOOR' " +
                            "    WHEN pb.qc_status IN ('QUARANTINE', 'QI', 'IN_PROCESS_SAMPLE', 'UNDER_TEST') THEN 'QC_HOLD' " +
                            "    WHEN pb.qc_status = 'RELEASED' THEN 'FINISHED_GOODS_WAREHOUSE' " +
                            "    WHEN pb.qc_status = 'APPROVED' AND mm.material_type = 'FINISHED_GOOD' THEN 'FINISHED_GOODS_WAREHOUSE' " +
                            "    WHEN pb.qc_status = 'APPROVED' AND mm.material_type = 'PACKAGING' THEN 'PACKAGING_WAREHOUSE' " +
                            "    WHEN pb.qc_status = 'APPROVED' AND mm.material_type IN ('RAW_MATERIAL', 'INTERMEDIATE') THEN 'RAW_MATERIAL_WAREHOUSE' " +
                            "    ELSE pb.location_code " +
                            "END " +
                            "FROM material_master mm " +
                            "WHERE mm.material_code = pb.material_code");
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
    // --- Supplier CRUD OPERATIONS — delegated to SupplierJdbcRepository (Phase 8) ---
    // =======================================================
    public List<Supplier> getAllSuppliers() {
        try {
            return supplierRepo.getAllSuppliers();
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error fetching all suppliers: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    // method to addSupplier.
    public int addSupplier(Supplier supplier) {
        try {
            return supplierRepo.addSupplier(supplier);
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error adding supplier: {}", e.getMessage(), e);
            return -1;
        }
    }

    // method to updateSupplier.
    public boolean updateSupplier(Supplier supplier) {
        try {
            return supplierRepo.updateSupplier(supplier);
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error updating supplier: {}", e.getMessage(), e);
            return false;
        }
    }

    // method to deleteSupplier.
    public boolean deleteSupplier(int supplierId) {
        return supplierRepo.deleteSupplier(supplierId);
    }

    public List<String> getSupplierNames() throws ClassNotFoundException {
        try {
            return supplierRepo.getSupplierNames();
        } catch (SQLException e) {
            logger.error("Error retrieving supplier names: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public boolean approveSupplier(int supplierId, String remarks, String performedBy)
            throws SQLException, ClassNotFoundException {
        return supplierRepo.approveSupplier(supplierId, remarks, performedBy);
    }

    public boolean rejectSupplier(int supplierId, String remarks, String performedBy)
            throws SQLException, ClassNotFoundException {
        return supplierRepo.rejectSupplier(supplierId, remarks, performedBy);
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
     * Delegated to MaterialJdbcRepository (Phase 8).
     */
    public boolean updateDrug(Material material) {
        try {
            return materialRepo.updateMaterial(material);
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error updating material: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean deleteDrug(String materialCode) {
        try {
            return materialRepo.deleteMaterial(materialCode);
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error deleting material: {}", e.getMessage(), e);
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
    // --- LOCATION CRUD OPERATIONS (DELEGATED TO REPOSITORY) ---
    // =======================================================
    public List<Location> getLocations() {
        try {
            return locationRepo.getLocations();
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error fetching all locations: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public Location getLocationById(String locationCode) {
        try {
            return locationRepo.getLocationById(locationCode);
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error fetching location by ID: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public boolean addLocation(String code, String name, String description, int capacity)
            throws ClassNotFoundException {
        try {
            return locationRepo.addLocation(code, name, description, capacity);
        } catch (SQLException e) {
            System.err.println("SQL Error inserting location '" + code + "': " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateLocation(String code, String name, String description, int capacity)
            throws ClassNotFoundException {
        try {
            return locationRepo.updateLocation(code, name, description, capacity);
        } catch (SQLException e) {
            System.err.println("SQL Error updating location '" + code + "': " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteLocation(String locationCode) {
        try {
            return locationRepo.deleteLocation(locationCode);
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
        try {
            return supplierRepo.getSupplierById(supplierId);
        } catch (SQLException e) {
            logger.error("Error fetching supplier by ID: {}", e.getMessage(), e);
            return null;
        }
    }

    public int getSupplierIdByName(String supplierName) throws ClassNotFoundException {
        try {
            return supplierRepo.getSupplierIdByName(supplierName);
        } catch (SQLException e) {
            logger.error("Error fetching supplier ID by name: {}", e.getMessage(), e);
            return -1;
        }
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

    // --- BOM (Bill of Materials) Management (DELEGATED TO REPOSITORY) ---

    public int createBOM(BOMHeader header, List<BOMDetail> details) throws SQLException, ClassNotFoundException {
        return bomRepo.createBOM(header, details);
    }

    public BOMHeader getBOMById(int bomId) throws SQLException, ClassNotFoundException {
        return bomRepo.getBOMById(bomId);
    }

    public List<BOMDetail> getBOMIngredients(int bomId) throws SQLException, ClassNotFoundException {
        return bomRepo.getBOMIngredients(bomId);
    }

    public List<BOMHeader> getActiveBOMsForMaterial(String materialCode) throws SQLException, ClassNotFoundException {
        return bomRepo.getActiveBOMsForMaterial(materialCode);
    }

    public List<BOMHeader> getAllBOMs() {
        try {
            return bomRepo.getAllBOMs();
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    public Map<String, Double> validateBOMAvailability(int bomId, double plannedQty)
            throws SQLException, ClassNotFoundException {
        Map<String, Double> shortages = new HashMap<>();

        List<BOMDetail> ingredients = bomRepo.getBOMIngredients(bomId);

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

    public boolean updateQcStatus(String batchNumber, String status) {
        try {
            return stockRepository.updateQcStatus(batchNumber, status);
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
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

    public List<pharma.model.InventoryTransaction> getInventoryTransactions() {
        try {
            return stockRepository.getAllInventoryTransactions();
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
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
        return productionOrderRepository.addInventoryTransaction(tx);
    }

    public boolean addMaterialConsumption(MaterialConsumption mc) {
        return productionOrderRepository.addMaterialConsumption(mc);
    }

    public boolean addProductionBatch(ProductionBatch pb) {
        return productionOrderRepository.addProductionBatch(pb);
    }

    public boolean addBatchGenealogy(BatchGenealogy bg) {
        return productionOrderRepository.addBatchGenealogy(bg);
    }

    public boolean addEventLog(EventLog el) {
        return productionOrderRepository.addEventLog(el);
    }

    public List<EventLog> getLatestEventLogs(int limit) {
        return productionOrderRepository.getLatestEventLogs(limit);
    }

    public List<MaterialConsumption> getMaterialConsumptionsForOrder(int orderId) {
        return productionOrderRepository.getMaterialConsumptionsForOrder(orderId);
    }

}
