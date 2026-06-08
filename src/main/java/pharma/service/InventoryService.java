package pharma.service;

import java.sql.SQLException;
import java.util.List;

import pharma.dto.MaterialAvailabilityDTO;
import pharma.repository.InventoryRepository;

public class InventoryService {
    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public MaterialAvailabilityDTO checkMaterialAvailability(String materialCode, double requiredQuantity)
            throws SQLException, ClassNotFoundException {
        if (materialCode == null || materialCode.isBlank()) {
            throw new IllegalArgumentException("materialCode is required.");
        }
        if (requiredQuantity < 0) {
            throw new IllegalArgumentException("requiredQuantity cannot be negative.");
        }
        return inventoryRepository.checkAvailability(materialCode, requiredQuantity);
    }

    public List<MaterialAvailabilityDTO> findLowStockMaterials() throws SQLException, ClassNotFoundException {
        return inventoryRepository.findLowStockMaterials();
    }
}
