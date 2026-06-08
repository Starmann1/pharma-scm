package pharma.repository;

import java.sql.SQLException;
import java.util.Optional;

import pharma.model.Material;

public interface MaterialRepository {
    Optional<Material> findByCode(String materialCode) throws SQLException, ClassNotFoundException;
}
