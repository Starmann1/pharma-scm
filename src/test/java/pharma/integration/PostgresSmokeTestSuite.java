package pharma.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pharma.config.PostgresTestContainerBase;
import pharma.model.Material;
import pharma.model.Role;
import pharma.model.Supplier;
import pharma.model.User;
import pharma.repository.jdbc.*;
import pharma.service.DatabaseService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PostgresSmokeTestSuite extends PostgresTestContainerBase {

    private DatabaseService dbService;
    private UserJdbcRepository userRepo;
    private MaterialJdbcRepository materialRepo;
    private SupplierJdbcRepository supplierRepo;

    @BeforeEach
    void setUp() {
        dbService = new DatabaseService(); // Uses the overridden static 'ds' from base class
        userRepo = dbService.getUserRepository();
        // Since material and supplier repo aren't exposed cleanly via getters, we instantiate them directly
        materialRepo = new MaterialJdbcRepository(dbService);
        supplierRepo = new SupplierJdbcRepository(dbService);
    }

    @Test
    void testAdminLogin() throws Exception {
        // V011__seed_admin_user.sql creates admin/admin123
        User user = userRepo.authenticateUser("admin", "admin123");
        assertNotNull(user, "Admin user should successfully authenticate against PostgreSQL");
        assertEquals("System Administrator", user.getFullName());
    }

    @Test
    void testMaterialsListLoads() throws Exception {
        // V013__seed_reference_data.sql should seed materials
        List<Material> materials = materialRepo.getAllMaterials();
        assertFalse(materials.isEmpty(), "Materials list should not be empty");
        
        boolean foundParacetamol = materials.stream()
            .anyMatch(m -> m.getGenericName() != null && m.getGenericName().contains("Paracetamol"));
        assertTrue(foundParacetamol, "Should find Paracetamol in seeded reference data");
    }
}
