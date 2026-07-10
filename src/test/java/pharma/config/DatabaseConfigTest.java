package pharma.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

@SuppressWarnings("null")
class DatabaseConfigTest {
    @Test
    void explicitlySetMysqlUsingExistingDbSettings() {
        DatabaseConfig config = DatabaseConfig.fromSettings(Map.of(
                "PHARMA_DB_PROFILE", "mysql",
                "DB_URL", "jdbc:mysql://localhost:3306/pharma_ims?allowPublicKeyRetrieval=true",
                "DB_USER", "root",
                "DB_PASS", "secret"));

        assertEquals(DatabaseConfig.Dialect.MYSQL, config.getDialect());
        assertTrue(config.isMysql());
        assertEquals("jdbc:mysql://localhost:3306/pharma_ims?...", config.getRedactedJdbcUrl());
    }

    @Test
    void defaultsToPostgresqlUsingExistingDbSettings() {
        DatabaseConfig config = DatabaseConfig.fromSettings(Map.of(
                "POSTGRES_DB_URL", "jdbc:postgresql://localhost:5432/pharma_ims_v11",
                "POSTGRES_DB_USER", "pharma_v11",
                "POSTGRES_DB_PASS", "secret"));

        assertEquals(DatabaseConfig.Dialect.POSTGRESQL, config.getDialect());
        assertTrue(config.isPostgresql());
    }

    @Test
    void selectsPostgresqlForV11Profile() {
        DatabaseConfig config = DatabaseConfig.fromSettings(Map.of(
                "PHARMA_DB_PROFILE", "v1.1",
                "POSTGRES_DB_URL", "jdbc:postgresql://localhost:5432/pharma_ims_v11",
                "POSTGRES_DB_USER", "pharma_v11",
                "POSTGRES_DB_PASS", "secret"));

        assertEquals(DatabaseConfig.Dialect.POSTGRESQL, config.getDialect());
        assertTrue(config.isPostgresql());
        assertEquals("v1.1", config.getProfile());
    }

    @Test
    void rejectsPostgresqlProfileWithMysqlUrl() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                DatabaseConfig.fromSettings(Map.of(
                        "PHARMA_DB_PROFILE", "postgresql",
                        "POSTGRES_DB_URL", "jdbc:mysql://localhost:3306/pharma_ims",
                        "POSTGRES_DB_USER", "pharma_v11",
                        "POSTGRES_DB_PASS", "secret")));

        assertTrue(exception.getMessage().contains("jdbc:postgresql:"));
    }

    @Test
    void rejectsUnknownProfile() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                DatabaseConfig.fromSettings(Map.of(
                        "PHARMA_DB_PROFILE", "oracle",
                        "DB_URL", "jdbc:mysql://localhost:3306/pharma_ims",
                        "DB_USER", "root",
                        "DB_PASS", "secret")));

        assertTrue(exception.getMessage().contains("PHARMA_DB_PROFILE"));
    }

    @Test
    void preservesConfiguredPasswordValue() {
        DatabaseConfig config = DatabaseConfig.fromSettings(Map.of(
                "PHARMA_DB_PROFILE", "mysql",
                "DB_URL", "jdbc:mysql://localhost:3306/pharma_ims",
                "DB_USER", "root",
                "DB_PASS", " leading-and-trailing "));

        assertEquals(" leading-and-trailing ", config.toHikariConfig().getPassword());
    }
}
