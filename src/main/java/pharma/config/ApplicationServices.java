package pharma.config;

import pharma.repository.AIDecisionRepository;
import pharma.repository.AuditRepository;
import pharma.repository.ComplianceRepository;
import pharma.repository.InventoryRepository;
import pharma.repository.MaterialRepository;
import pharma.repository.ProductionRepository;
import pharma.repository.QARepository;
import pharma.repository.RiskRepository;
import pharma.repository.SupplierRepository;
import pharma.repository.jdbc.AIDecisionJdbcRepository;
import pharma.repository.jdbc.AuditJdbcRepository;
import pharma.repository.jdbc.ComplianceJdbcRepository;
import pharma.repository.jdbc.InventoryJdbcRepository;
import pharma.repository.jdbc.MaterialJdbcRepository;
import pharma.repository.jdbc.ProductionJdbcRepository;
import pharma.repository.jdbc.QAJdbcRepository;
import pharma.repository.jdbc.RiskJdbcRepository;
import pharma.repository.jdbc.SupplierJdbcRepository;
import pharma.service.AIDecisionService;
import pharma.service.AuditService;
import pharma.service.ComplianceService;
import pharma.service.DatabaseService;
import pharma.service.InventoryService;
import pharma.service.MaterialService;
import pharma.service.ProductionService;
import pharma.service.QAService;
import pharma.service.RiskService;
import pharma.service.SupplierService;

public class ApplicationServices {
    private final DatabaseService databaseService;
    private final MaterialService materialService;
    private final InventoryService inventoryService;
    private final SupplierService supplierService;
    private final ProductionService productionService;
    private final QAService qaService;
    private final ComplianceService complianceService;
    private final RiskService riskService;
    private final AuditService auditService;
    private final AIDecisionService aiDecisionService;

    public ApplicationServices(DatabaseService databaseService) {
        this.databaseService = databaseService;

        MaterialRepository materialRepository = new MaterialJdbcRepository(databaseService);
        InventoryRepository inventoryRepository = new InventoryJdbcRepository(databaseService);
        SupplierRepository supplierRepository = new SupplierJdbcRepository(databaseService);
        ProductionRepository productionRepository = new ProductionJdbcRepository(databaseService, inventoryRepository);
        QARepository qaRepository = new QAJdbcRepository(databaseService);
        ComplianceRepository complianceRepository = new ComplianceJdbcRepository();
        RiskRepository riskRepository = new RiskJdbcRepository(databaseService);
        AuditRepository auditRepository = new AuditJdbcRepository(databaseService);
        AIDecisionRepository aiDecisionRepository = new AIDecisionJdbcRepository(databaseService);

        this.materialService = new MaterialService(materialRepository);
        this.inventoryService = new InventoryService(inventoryRepository);
        this.supplierService = new SupplierService(supplierRepository);
        this.productionService = new ProductionService(productionRepository);
        this.qaService = new QAService(qaRepository);
        this.complianceService = new ComplianceService(complianceRepository);
        this.riskService = new RiskService(riskRepository);
        this.auditService = new AuditService(auditRepository);
        this.aiDecisionService = new AIDecisionService(aiDecisionRepository);
    }

    public DatabaseService getDatabaseService() {
        return databaseService;
    }

    public MaterialService getMaterialService() {
        return materialService;
    }

    public InventoryService getInventoryService() {
        return inventoryService;
    }

    public SupplierService getSupplierService() {
        return supplierService;
    }

    public ProductionService getProductionService() {
        return productionService;
    }

    public QAService getQaService() {
        return qaService;
    }

    public ComplianceService getComplianceService() {
        return complianceService;
    }

    public RiskService getRiskService() {
        return riskService;
    }

    public AuditService getAuditService() {
        return auditService;
    }

    public AIDecisionService getAiDecisionService() {
        return aiDecisionService;
    }
}
