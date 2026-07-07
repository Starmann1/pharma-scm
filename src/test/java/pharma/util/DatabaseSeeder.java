package pharma.util;

import pharma.model.*;
import pharma.service.DatabaseService;
import java.util.List;

public class DatabaseSeeder {
    public static void main(String[] args) {
        System.out.println("Starting database seeder...");
        DatabaseService dbService = DatabaseService.getInstance();
        
        if (!dbService.connect()) {
            System.err.println("Failed to connect to database.");
            return;
        }

        try {
            // 1. Insert Location
            System.out.println("Checking locations...");
            // (Assuming Locations are mostly seed data, but we can add one if location repo exists, else skip)

            // 2. Insert Supplier
            System.out.println("Inserting Supplier...");
            Supplier supplier = new Supplier();
            supplier.setSupplierName("Global Pharma Supplies Inc.");
            supplier.setContactPerson("John Doe");
            supplier.setAddress("123 Pharma Lane, NY");
            supplier.setEmail("contact@globalpharma.com");
            supplier.setPhoneNumber("+1234567890");
            supplier.setGstin("GSTIN12345");
            supplier.setDrugLicenseNumber("DLN98765");
            supplier.setPaymentTerms("Net 30");
            int supplierId = dbService.addSupplier(supplier);
            System.out.println("Inserted Supplier with ID: " + supplierId);
            
            // 3. Insert Material
            System.out.println("Inserting Material...");
            Material material = new Material();
            material.setMaterialCode("RM-1001");
            material.setBrandName("Paracetamol Powder");
            material.setGenericName("Paracetamol");
            material.setManufacturer("Global Pharma Supplies Inc.");
            material.setMaterialType(Material.MaterialType.RAW_MATERIAL);
            material.setUnitOfMeasure(Material.UnitOfMeasure.KG);
            material.setActive(true);
            // using the repository directly
            new pharma.repository.jdbc.MaterialJdbcRepository(dbService).addMaterial(material);
            System.out.println("Inserted Material: RM-1001");

            System.out.println("Database connection and insertion successful!");
            
            // Fetch back to verify
            List<Supplier> suppliers = dbService.getAllSuppliers();
            System.out.println("Total Suppliers: " + suppliers.size());
            
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
