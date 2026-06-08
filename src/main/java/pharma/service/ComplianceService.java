package pharma.service;

import java.sql.SQLException;

import pharma.dto.ManufacturingFeasibilityDTO;
import pharma.repository.ComplianceRepository;

public class ComplianceService {
    private final ComplianceRepository complianceRepository;

    public ComplianceService(ComplianceRepository complianceRepository) {
        this.complianceRepository = complianceRepository;
    }

    public boolean validateManufacturingProposal(ManufacturingFeasibilityDTO proposal)
            throws SQLException, ClassNotFoundException {
        return complianceRepository.validateManufacturingProposal(proposal);
    }
}
