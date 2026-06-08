package pharma.service;

import java.sql.SQLException;
import java.util.Optional;

import pharma.model.Material;
import pharma.repository.MaterialRepository;

public class MaterialService {
    private final MaterialRepository materialRepository;

    public MaterialService(MaterialRepository materialRepository) {
        this.materialRepository = materialRepository;
    }

    public Optional<Material> findByCode(String materialCode) throws SQLException, ClassNotFoundException {
        return materialRepository.findByCode(materialCode);
    }
}
