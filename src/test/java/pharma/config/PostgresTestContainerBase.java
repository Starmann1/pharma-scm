package pharma.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import pharma.service.DatabaseService;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;

public abstract class PostgresTestContainerBase {

    private static HikariDataSource testDataSource;

    @BeforeAll
    static void setUpDatabase() throws Exception {
        // Read from .env if possible, otherwise use defaults
        DatabaseConfig dbConfig = DatabaseConfig.fromEnvironment();
        HikariConfig config = dbConfig.toHikariConfig();
        try {
            testDataSource = new HikariDataSource(config);
            Connection testConn = testDataSource.getConnection();
            testConn.close();
        } catch (Exception e) {
            System.err.println("Could not connect to PostgreSQL. Skipping tests. Reason: " + e.getMessage());
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "PostgreSQL not available or bad credentials.");
            return;
        }

        Field dsField = DatabaseService.class.getDeclaredField("ds");
        dsField.setAccessible(true);
        dsField.set(null, testDataSource);

        try (Connection conn = testDataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            String schemaSql = Files.readString(Paths.get("src/main/resources/schema-postgres.sql"));
            stmt.execute(schemaSql);
            
            String[] seedFiles = {
                "db/postgresql/seed/V010__seed_roles_permissions.sql",
                "db/postgresql/seed/V011__seed_admin_user.sql",
                "db/postgresql/seed/V012__seed_locations.sql",
                "db/postgresql/seed/V013__seed_reference_data.sql"
            };
            
            for (String file : seedFiles) {
                String sql = Files.readString(Paths.get(file));
                stmt.execute(sql);
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not execute seed scripts. Tests may fail if DB is not ready.");
            e.printStackTrace();
        }
    }

    @AfterAll
    static void tearDownDatabase() {
        if (testDataSource != null) {
            testDataSource.close();
        }
    }
}
