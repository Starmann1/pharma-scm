package pharma.repository.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import pharma.model.Location;
import pharma.repository.LocationRepository;
import pharma.service.DatabaseService;

public class LocationJdbcRepository implements LocationRepository {
    private final DatabaseService databaseService;
    private final JdbcSqlDialect sqlDialect;

    public LocationJdbcRepository(DatabaseService databaseService) {
        this(databaseService, JdbcSqlDialect.from(DatabaseService.getDatabaseConfig()));
    }

    public LocationJdbcRepository(DatabaseService databaseService, JdbcSqlDialect sqlDialect) {
        this.databaseService = databaseService;
        this.sqlDialect = sqlDialect;
    }

    @Override
    public List<Location> getLocations() throws SQLException, ClassNotFoundException {
        List<Location> locations = new ArrayList<>();
        String sql = "SELECT location_code, location_name, description, capacity FROM "
                + sqlDialect.table(JdbcSqlDialect.Table.LOCATION_MASTER);
        try (Connection conn = databaseService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Location location = new Location(
                        rs.getString("location_code"),
                        rs.getString("location_name"),
                        rs.getString("description"),
                        rs.getInt("capacity"));
                locations.add(location);
            }
        }
        return locations;
    }

    @Override
    public Location getLocationById(String locationCode) throws SQLException, ClassNotFoundException {
        String sql = "SELECT location_code, location_name, description, capacity FROM "
                + sqlDialect.table(JdbcSqlDialect.Table.LOCATION_MASTER) + " WHERE location_code = ?";

        try (Connection conn = databaseService.getConnection();
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
        }
        return null;
    }

    @Override
    public boolean addLocation(String code, String name, String description, int capacity)
            throws SQLException, ClassNotFoundException {
        String sql = "INSERT INTO " + sqlDialect.table(JdbcSqlDialect.Table.LOCATION_MASTER)
                + " (location_code, location_name, description, capacity) VALUES (?, ?, ?, ?)";

        if (code == null || code.trim().isEmpty() || name == null || name.trim().isEmpty()) {
            System.err.println("Cannot add location: Code or Name is empty.");
            return false;
        }

        try (Connection conn = databaseService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, code.trim());
            pstmt.setString(2, name.trim());
            pstmt.setString(3, description != null ? description.trim() : "");
            pstmt.setInt(4, capacity);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows == 1;
        }
    }

    @Override
    public boolean updateLocation(String code, String name, String description, int capacity)
            throws SQLException, ClassNotFoundException {
        String sql = "UPDATE " + sqlDialect.table(JdbcSqlDialect.Table.LOCATION_MASTER)
                + " SET location_name = ?, description = ?, capacity = ? WHERE location_code = ?";

        if (code == null || code.trim().isEmpty()) {
            System.err.println("Cannot update location: Code is empty.");
            return false;
        }

        try (Connection conn = databaseService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name != null ? name.trim() : "");
            pstmt.setString(2, description != null ? description.trim() : "");
            pstmt.setInt(3, capacity);
            pstmt.setString(4, code.trim());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows == 1;
        }
    }

    @Override
    public boolean deleteLocation(String locationCode) throws SQLException, ClassNotFoundException {
        String sql = "DELETE FROM " + sqlDialect.table(JdbcSqlDialect.Table.LOCATION_MASTER)
                + " WHERE location_code = ?";

        try (Connection conn = databaseService.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, locationCode);
            return pstmt.executeUpdate() > 0;
        }
    }
}
