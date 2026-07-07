package pharma.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import pharma.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DatabaseConnectionCheck {
    public static void main(String[] args) {
        System.out.println("--- Testing PostgreSQL Connection ---");
        try {
            DatabaseConfig dbConfig = DatabaseConfig.fromEnvironment();
            HikariConfig config = dbConfig.toHikariConfig();
            try (HikariDataSource ds = new HikariDataSource(config);
                 Connection conn = ds.getConnection()) {
                 
                System.out.println("✅ Successfully connected to PostgreSQL!");
                
                // 1. Insert Location
                String insertLocation = "INSERT INTO location_master (location_code, description, location_type, capacity, status) " +
                                        "VALUES ('TEST_WH', 'Test Warehouse', 'WAREHOUSE', 5000, 'ACTIVE') " +
                                        "ON CONFLICT (location_code) DO NOTHING";
                try (PreparedStatement stmt = conn.prepareStatement(insertLocation)) {
                    int rows = stmt.executeUpdate();
                    System.out.println("✅ Location TEST_WH inserted or already exists (Rows affected: " + rows + ")");
                }

                // 2. Insert Supplier
                String insertSupplier = "INSERT INTO supplier_master (supplier_name, contact_person, email, phone_number, address, supplier_status) " +
                                        "VALUES ('Test Supplier Inc', 'Jane Doe', 'test@supplier.com', '123-456', '123 Test St', 'APPROVED') " +
                                        "ON CONFLICT DO NOTHING";
                try (PreparedStatement stmt = conn.prepareStatement(insertSupplier)) {
                    int rows = stmt.executeUpdate();
                    System.out.println("✅ Supplier 'Test Supplier Inc' inserted or already exists (Rows affected: " + rows + ")");
                }

                // 3. Insert Inventory for Raw Material
                String insertInventoryRM = "INSERT INTO stock_inventory (material_code, batch_number, location_code, quantity, status, received_date) " +
                                           "VALUES ('RM-001', 'BATCH-TEST-RM', 'TEST_WH', 1000, 'AVAILABLE', CURRENT_DATE) " +
                                           "ON CONFLICT DO NOTHING";
                try (PreparedStatement stmt = conn.prepareStatement(insertInventoryRM)) {
                    int rows = stmt.executeUpdate();
                    System.out.println("✅ Raw Material Inventory inserted or already exists (Rows affected: " + rows + ")");
                }

                // 4. Insert Inventory for Packaging Goods
                String insertInventoryPM = "INSERT INTO stock_inventory (material_code, batch_number, location_code, quantity, status, received_date) " +
                                           "VALUES ('PM-001', 'BATCH-TEST-PM', 'TEST_WH', 5000, 'AVAILABLE', CURRENT_DATE) " +
                                           "ON CONFLICT DO NOTHING";
                try (PreparedStatement stmt = conn.prepareStatement(insertInventoryPM)) {
                    int rows = stmt.executeUpdate();
                    System.out.println("✅ Packaging Material Inventory inserted or already exists (Rows affected: " + rows + ")");
                }

                // 5. Query to verify
                System.out.println("\n--- Verifying Inserted Records ---");
                String verifySql = "SELECT material_code, batch_number, quantity, status FROM stock_inventory WHERE location_code = 'TEST_WH'";
                try (PreparedStatement stmt = conn.prepareStatement(verifySql);
                     ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        System.out.printf("Found Stock: %s | Batch: %s | Qty: %d | Status: %s%n",
                                rs.getString("material_code"), rs.getString("batch_number"),
                                rs.getInt("quantity"), rs.getString("status"));
                    }
                }

            }
        } catch (Exception e) {
            System.err.println("❌ Database connection or insertion failed!");
            e.printStackTrace();
        }
    }
}
