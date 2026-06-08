package pharma.repository.jdbc;

import java.sql.SQLException;

import pharma.dto.ManufacturingFeasibilityDTO;
import pharma.repository.ComplianceRepository;

public class ComplianceJdbcRepository implements ComplianceRepository {
    @Override
    public boolean validateManufacturingProposal(ManufacturingFeasibilityDTO proposal)
            throws SQLException, ClassNotFoundException {
        if (proposal == null) {
            return false;
        }
        return proposal.getBlockers() == null || proposal.getBlockers().isEmpty();
    }
}
