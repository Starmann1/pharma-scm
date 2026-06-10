package pharma.service;

import java.sql.SQLException;
import java.util.List;

import pharma.dto.SupplierScoreDTO;
import pharma.repository.SupplierRepository;

public class SupplierService {
    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    public List<SupplierScoreDTO> rankApprovedSuppliersForMaterial(String materialCode)
            throws SQLException, ClassNotFoundException {
        if (materialCode == null || materialCode.isBlank()) {
            throw new IllegalArgumentException("materialCode is required.");
        }
        return supplierRepository.rankApprovedSuppliersForMaterial(materialCode);
    }

    public double getSupplierCapacity(int supplierId, String materialCode)
            throws SQLException, ClassNotFoundException {
        if (materialCode == null || materialCode.isBlank()) {
            throw new IllegalArgumentException("materialCode is required.");
        }
        return supplierRepository.getSupplierCapacity(supplierId, materialCode);
    }
}
