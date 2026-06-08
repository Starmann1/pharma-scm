package pharma.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import pharma.dto.MaterialAvailabilityDTO;
import pharma.dto.ProductionCapacityDTO;
import pharma.repository.ProductionRepository;

public class ProductionService {
    private final ProductionRepository productionRepository;

    public ProductionService(ProductionRepository productionRepository) {
        this.productionRepository = productionRepository;
    }

    public List<MaterialAvailabilityDTO> checkBomMaterialAvailability(int bomId, double plannedQuantity)
            throws SQLException, ClassNotFoundException {
        if (bomId <= 0) {
            throw new IllegalArgumentException("bomId must be greater than zero.");
        }
        if (plannedQuantity <= 0) {
            throw new IllegalArgumentException("plannedQuantity must be greater than zero.");
        }
        return productionRepository.checkBomMaterialAvailability(bomId, plannedQuantity);
    }

    public ProductionCapacityDTO checkCapacity(int bomId, double plannedQuantity, LocalDate requestedDate)
            throws SQLException, ClassNotFoundException {
        if (requestedDate == null) {
            requestedDate = LocalDate.now();
        }
        return productionRepository.checkCapacity(bomId, plannedQuantity, requestedDate);
    }
}
