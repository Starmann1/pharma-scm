package pharma.repository.jdbc;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import pharma.dto.MaterialAvailabilityDTO;
import pharma.dto.ProductionCapacityDTO;
import pharma.model.BOMDetail;
import pharma.model.BOMHeader;
import pharma.repository.InventoryRepository;
import pharma.repository.ProductionRepository;
import pharma.service.DatabaseService;

public class ProductionJdbcRepository implements ProductionRepository {
    private final DatabaseService databaseService;
    private final InventoryRepository inventoryRepository;

    public ProductionJdbcRepository(DatabaseService databaseService, InventoryRepository inventoryRepository) {
        this.databaseService = databaseService;
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public List<MaterialAvailabilityDTO> checkBomMaterialAvailability(int bomId, double plannedQuantity)
            throws SQLException, ClassNotFoundException {
        List<MaterialAvailabilityDTO> results = new ArrayList<>();
        for (BOMDetail detail : databaseService.getBOMIngredients(bomId)) {
            double required = detail.getRequiredQty() * plannedQuantity;
            results.add(inventoryRepository.checkAvailability(detail.getIngredientMaterialCode(), required));
        }
        return results;
    }

    @Override
    public ProductionCapacityDTO checkCapacity(int bomId, double plannedQuantity, LocalDate requestedDate)
            throws SQLException, ClassNotFoundException {
        BOMHeader bom = databaseService.getBOMById(bomId);
        ProductionCapacityDTO dto = new ProductionCapacityDTO();
        dto.setBomId(bomId);
        dto.setMaterialCode(bom != null ? bom.getMaterialCode() : null);
        dto.setPlannedQuantity(plannedQuantity);
        dto.setRequestedDate(requestedDate);
        dto.setCapacityAvailable(bom != null && plannedQuantity > 0);
        if (bom == null) {
            dto.getConstraints().add("BOM not found.");
        }
        if (plannedQuantity <= 0) {
            dto.getConstraints().add("Planned quantity must be greater than zero.");
        }
        return dto;
    }
}
