package pharma.repository;

import java.sql.SQLException;

import pharma.dto.ManufacturingFeasibilityDTO;

public interface ComplianceRepository {
    boolean validateManufacturingProposal(ManufacturingFeasibilityDTO proposal)
            throws SQLException, ClassNotFoundException;
}
