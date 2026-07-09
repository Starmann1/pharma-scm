package pharma.repository;

import java.sql.SQLException;
import java.util.List;
import pharma.model.Location;

public interface LocationRepository {
    List<Location> getLocations() throws SQLException, ClassNotFoundException;
    Location getLocationById(String locationCode) throws SQLException, ClassNotFoundException;
    boolean addLocation(String code, String name, String description, int capacity) throws SQLException, ClassNotFoundException;
    boolean updateLocation(String code, String name, String description, int capacity) throws SQLException, ClassNotFoundException;
    boolean deleteLocation(String locationCode) throws SQLException, ClassNotFoundException;
}
