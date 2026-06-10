package pharma.repository;

import java.sql.SQLException;
import java.util.List;

import pharma.dto.MaterialAvailabilityDTO;

public interface InventoryRepository {
    MaterialAvailabilityDTO checkAvailability(String materialCode, double requiredQuantity)
            throws SQLException, ClassNotFoundException;

    List<MaterialAvailabilityDTO> findLowStockMaterials() throws SQLException, ClassNotFoundException;

    boolean reserveMaterial(String materialCode, double quantity) throws SQLException, ClassNotFoundException;
}
