package pharma.llm.tools;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pharma.config.ApplicationServices;

/**
 * Central registry for all LangChain4j tool wrapper instances.
 *
 * <p>Creates the six domain-specific tool classes from
 * {@link ApplicationServices} and exposes them as an unmodifiable list
 * that can be passed directly to LangChain4j's tool-enabled chat calls.
 *
 * <p>Usage:
 * <pre>{@code
 *   LlmToolRegistry registry = new LlmToolRegistry(services);
 *   List<Object> tools = registry.getToolObjects();
 *   // pass tools to GeminiChatService.chatWithTools(...)
 * }</pre>
 */
public class LlmToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(LlmToolRegistry.class);

    private final List<Object> toolObjects;

    /**
     * Constructs the registry, instantiating all tool wrappers from the application services.
     *
     * @param services the application services providing access to all service-layer beans
     */
    @SuppressWarnings("null")
    public LlmToolRegistry(ApplicationServices services) {
        this.toolObjects = Collections.unmodifiableList(List.of(
                new InventoryLlmTools(services.getInventoryService(), services.getDatabaseService()),
                new SupplierLlmTools(services.getSupplierService(), services.getDatabaseService()),
                new ProductionLlmTools(services.getProductionService()),
                new QALlmTools(services.getQaService()),
                new ComplianceLlmTools(services.getComplianceService()),
                new RiskLlmTools(services.getRiskService())
        ));
        log.info("[LlmToolRegistry] Registered {} tool classes: Inventory, Supplier, Production, QA, Compliance, Risk",
                toolObjects.size());
    }

    /**
     * Returns an unmodifiable list of all tool objects for LangChain4j.
     *
     * <p>Each element in the list is a Java object whose {@code @Tool}-annotated
     * methods will be discovered by LangChain4j at runtime.
     *
     * @return unmodifiable list of tool wrapper instances
     */
    public List<Object> getToolObjects() {
        return toolObjects;
    }
}
