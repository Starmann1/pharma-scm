package pharma.config;

import com.zaxxer.hikari.HikariConfig;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.Locale;
import java.util.Map;

public final class DatabaseConfig {
    public enum Dialect {
        MYSQL("mysql", "com.mysql.cj.jdbc.Driver", "jdbc:mysql:"),
        POSTGRESQL("postgresql", "org.postgresql.Driver", "jdbc:postgresql:");

        private final String profileName;
        private final String driverClassName;
        private final String jdbcUrlPrefix;

        Dialect(String profileName, String driverClassName, String jdbcUrlPrefix) {
            this.profileName = profileName;
            this.driverClassName = driverClassName;
            this.jdbcUrlPrefix = jdbcUrlPrefix;
        }

        public String getProfileName() {
            return profileName;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public String getJdbcUrlPrefix() {
            return jdbcUrlPrefix;
        }
    }

    public static final String PROFILE_KEY = "PHARMA_DB_PROFILE";
    public static final String DEFAULT_PROFILE = "mysql";

    private static final String DB_URL = "DB_URL";
    private static final String DB_USER = "DB_USER";
    private static final String DB_PASS = "DB_PASS";
    private static final String POSTGRES_DB_URL = "POSTGRES_DB_URL";
    private static final String POSTGRES_DB_USER = "POSTGRES_DB_USER";
    private static final String POSTGRES_DB_PASS = "POSTGRES_DB_PASS";

    private final String profile;
    private final Dialect dialect;
    private final String jdbcUrl;
    private final String username;
    private final String password;

    private DatabaseConfig(String profile, Dialect dialect, String jdbcUrl, String username, String password) {
        this.profile = requireText(profile, PROFILE_KEY);
        this.dialect = dialect;
        this.jdbcUrl = requireText(jdbcUrl, urlKeyFor(dialect));
        this.username = requireText(username, userKeyFor(dialect));
        this.password = requireConfigured(password, passKeyFor(dialect));
        validateJdbcUrl();
    }

    public static DatabaseConfig fromEnvironment() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        return fromSource(key -> {
            String environmentValue = System.getenv(key);
            if (hasText(environmentValue)) {
                return environmentValue;
            }
            return dotenv.get(key);
        });
    }

    static DatabaseConfig fromSettings(Map<String, String> settings) {
        return fromSource(settings::get);
    }

    private static DatabaseConfig fromSource(SettingSource source) {
        String profile = valueOrDefault(source.get(PROFILE_KEY), DEFAULT_PROFILE);
        Dialect dialect = dialectForProfile(profile);

        if (dialect == Dialect.POSTGRESQL) {
            return new DatabaseConfig(
                    profile,
                    dialect,
                    firstText(source.get(POSTGRES_DB_URL), source.get(DB_URL)),
                    firstText(source.get(POSTGRES_DB_USER), source.get(DB_USER)),
                    firstText(source.get(POSTGRES_DB_PASS), source.get(DB_PASS)));
        }

        return new DatabaseConfig(
                profile,
                dialect,
                source.get(DB_URL),
                source.get(DB_USER),
                source.get(DB_PASS));
    }

    public HikariConfig toHikariConfig() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(dialect.getDriverClassName());
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setMaxLifetime(1800000);
        config.setConnectionTimeout(30000);
        return config;
    }

    public String getProfile() {
        return profile;
    }

    public Dialect getDialect() {
        return dialect;
    }

    public boolean isMysql() {
        return dialect == Dialect.MYSQL;
    }

    public boolean isPostgresql() {
        return dialect == Dialect.POSTGRESQL;
    }

    public String getRedactedJdbcUrl() {
        int queryStart = jdbcUrl.indexOf('?');
        if (queryStart < 0) {
            return jdbcUrl;
        }
        return jdbcUrl.substring(0, queryStart) + "?...";
    }

    private void validateJdbcUrl() {
        if (!jdbcUrl.toLowerCase(Locale.ROOT).startsWith(dialect.getJdbcUrlPrefix())) {
            throw new IllegalArgumentException(
                    urlKeyFor(dialect) + " must start with " + dialect.getJdbcUrlPrefix()
                            + " when " + PROFILE_KEY + "=" + profile);
        }
    }

    private static Dialect dialectForProfile(String profile) {
        String normalizedProfile = valueOrDefault(profile, DEFAULT_PROFILE)
                .trim()
                .toLowerCase(Locale.ROOT);

        return switch (normalizedProfile) {
            case "mysql", "v1" -> Dialect.MYSQL;
            case "postgres", "postgresql", "pg", "v1.1", "v11" -> Dialect.POSTGRESQL;
            default -> throw new IllegalArgumentException(
                    PROFILE_KEY + " must be one of mysql, v1, postgres, postgresql, pg, v1.1, or v11");
        };
    }

    private static String urlKeyFor(Dialect dialect) {
        return dialect == Dialect.POSTGRESQL ? POSTGRES_DB_URL : DB_URL;
    }

    private static String userKeyFor(Dialect dialect) {
        return dialect == Dialect.POSTGRESQL ? POSTGRES_DB_USER : DB_USER;
    }

    private static String passKeyFor(Dialect dialect) {
        return dialect == Dialect.POSTGRESQL ? POSTGRES_DB_PASS : DB_PASS;
    }

    private static String requireText(String value, String key) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(key + " must be configured");
        }
        return value.trim();
    }

    private static String requireConfigured(String value, String key) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(key + " must be configured");
        }
        return value;
    }

    private static String valueOrDefault(String value, String defaultValue) {
        return hasText(value) ? value.trim() : defaultValue;
    }

    private static String firstText(String preferred, String fallback) {
        return hasText(preferred) ? preferred : fallback;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    @FunctionalInterface
    private interface SettingSource {
        String get(String key);
    }
}
