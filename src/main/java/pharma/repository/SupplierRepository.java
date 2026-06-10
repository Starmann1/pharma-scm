package pharma.repository;

import java.sql.SQLException;
import java.util.List;

import pharma.dto.SupplierScoreDTO;

public interface SupplierRepository {
    List<SupplierScoreDTO> rankApprovedSuppliersForMaterial(String materialCode)
            throws SQLException, ClassNotFoundException;

    double getSupplierCapacity(int supplierId, String materialCode) throws SQLException, ClassNotFoundException;
}
