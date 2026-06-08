package pharma.repository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import pharma.dto.MaterialAvailabilityDTO;
import pharma.dto.ProductionCapacityDTO;

public interface ProductionRepository {
    List<MaterialAvailabilityDTO> checkBomMaterialAvailability(int bomId, double plannedQuantity)
            throws SQLException, ClassNotFoundException;

    ProductionCapacityDTO checkCapacity(int bomId, double plannedQuantity, LocalDate requestedDate)
            throws SQLException, ClassNotFoundException;
}
