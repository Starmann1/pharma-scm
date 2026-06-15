package pharma.agent.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import pharma.agent.ontology.AgentNames;
import pharma.agent.operational.CoordinatorAgent;
import pharma.agent.operational.ComplianceAgent;
import pharma.agent.operational.InventoryAgent;
import pharma.agent.operational.ProductionAgent;
import pharma.agent.operational.QAAgent;
import pharma.agent.operational.SupplierAgent;
import pharma.agent.operational.ProcurementWorkflowAgent;
import pharma.agent.operational.RiskAnalysisAgent;
import pharma.agent.operational.AIReasoningAgent;
import pharma.agent.operational.KnowledgeAgent;
import pharma.config.ApplicationServices;

/**
 * AgentPlatformManager — lifecycle manager for the JADE agent platform.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Start and stop the JADE main container in headless mode.</li>
 *   <li>Create and start all operational agents (Phase 6 + V1 additions),
 *       injecting {@link ApplicationServices} as arg[0].</li>
 *   <li>Wire the {@link AgentGateway} to the started {@code CoordinatorAgent}.</li>
 * </ul>
 *
 * <p>Threading: all JADE lifecycle operations are {@code synchronized} to
 * prevent double-start races during application startup.
 */
public class AgentPlatformManager {

    private static final Logger log = LoggerFactory.getLogger(AgentPlatformManager.class);

    private final ApplicationServices appServices;

    private Runtime runtime;
    private ContainerController mainContainer;

    public AgentPlatformManager(ApplicationServices appServices) {
        this.appServices = appServices;
    }

    // -------------------------------------------------------------------------
    // Container lifecycle
    // -------------------------------------------------------------------------

    /** Starts the JADE main container in headless (no GUI) mode. */
    public synchronized void startMainContainer() {
        if (mainContainer != null) {
            log.warn("AgentPlatformManager: JADE container already started — skipping.");
            return;
        }
        runtime = Runtime.instance();
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.GUI, "false");
        // Enable O2A channel for all agents (required by AgentGateway.submit)
        profile.setParameter(Profile.MAIN, "true");
        mainContainer = runtime.createMainContainer(profile);
        if (mainContainer == null) {
            log.error("AgentPlatformManager: Failed to start JADE container. Port 1099 might be in use.");
            throw new IllegalStateException("JADE createMainContainer returned null. Port 1099 may be in use.");
        }
        log.info("AgentPlatformManager: JADE main container started in headless mode.");
    }

    /** Returns {@code true} if the JADE container has been started. */
    public synchronized boolean isStarted() {
        return mainContainer != null;
    }

    /** Kills the JADE container and releases all resources. */
    public synchronized void shutdown() {
        if (mainContainer != null) {
            try {
                mainContainer.kill();
                log.info("AgentPlatformManager: JADE container shut down.");
            } catch (StaleProxyException e) {
                log.warn("AgentPlatformManager: Failed to stop JADE container cleanly: {}", e.getMessage());
            } finally {
                mainContainer = null;
                runtime = null;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Agent bootstrap — V1: JADE + LangChain4j
    // -------------------------------------------------------------------------

    /**
     * Starts all operational agents and wires the gateway to the
     * CoordinatorAgent.
     *
     * <p>Agent argument convention:
     * <ul>
     *   <li>All agents: arg[0] = {@link ApplicationServices}</li>
     *   <li>CoordinatorAgent only: arg[1] = {@link AgentGateway}</li>
     * </ul>
     *
     * @param gateway the {@link AgentGateway} to wire to the CoordinatorAgent
     */
    public synchronized void startAllOperationalAgents(AgentGateway gateway) {
        if (!isStarted()) {
            startMainContainer();
        }

        try {
            // --- Phase 6: Core sub-agents (Coordinator routes to them) ---
            startAgent(AgentNames.INVENTORY,   InventoryAgent.class,   new Object[]{appServices});
            startAgent(AgentNames.SUPPLIER,    SupplierAgent.class,    new Object[]{appServices});

            // --- Spawn representative agents for all approved suppliers ---
            try (java.sql.Connection conn = appServices.getDatabaseService().getConnection();
                 java.sql.PreparedStatement stmt = conn.prepareStatement(
                         "SELECT supplier_id FROM Supplier_Master WHERE UPPER(COALESCE(supplier_status, 'APPROVED')) = 'APPROVED'");
                 java.sql.ResultSet rs = stmt.executeQuery()) {
                 while (rs.next()) {
                     int id = rs.getInt("supplier_id");
                     startAgent(AgentNames.SUPPLIER + "-" + id, SupplierAgent.class, new Object[]{appServices, id});
                 }
            } catch (Exception e) {
                 log.error("AgentPlatformManager: Failed to spawn supplier representatives", e);
            }

            startAgent(AgentNames.PRODUCTION,  ProductionAgent.class,  new Object[]{appServices});
            startAgent(AgentNames.QA,          QAAgent.class,          new Object[]{appServices});
            startAgent(AgentNames.COMPLIANCE,  ComplianceAgent.class,  new Object[]{appServices});

            // --- V1 Phase 7: Procurement Workflow Agent ---
            startAgent(AgentNames.PROCUREMENT, ProcurementWorkflowAgent.class, new Object[]{appServices});

            // --- V1 Phase 8: Risk Analysis Agent ---
            startAgent(AgentNames.RISK,        RiskAnalysisAgent.class, new Object[]{appServices});

            // --- V1 Phase 10: AI Reasoning Agent (Gemini + LangChain4j) ---
            startAgent(AgentNames.AI_REASONING, AIReasoningAgent.class, new Object[]{appServices});

            // --- V1 Phase 11: Knowledge Agent (RAG) ---
            startAgent(AgentNames.KNOWLEDGE,   KnowledgeAgent.class,   new Object[]{appServices});

            // --- CoordinatorAgent last (receives gateway reference) ---
            AgentController coordinator = startAgent(
                    AgentNames.COORDINATOR,
                    CoordinatorAgent.class,
                    new Object[]{appServices, gateway});

            // Wire the gateway so submit() can send ACL messages to Coordinator
            gateway.setCoordinatorController(coordinator);
            gateway.setPlatformManager(this);

            log.info("AgentPlatformManager: All V1 operational agents started successfully.");

        } catch (StaleProxyException e) {
            log.error("AgentPlatformManager: Failed to start operational agents: {}", e.getMessage(), e);
            throw new RuntimeException("Could not start JADE operational agents.", e);
        }
    }

    // -------------------------------------------------------------------------
    // Low-level helpers
    // -------------------------------------------------------------------------

    /**
     * Creates, starts, and returns a new JADE agent.
     *
     * @param localName  the agent's local name (from {@link AgentNames})
     * @param agentClass the concrete agent class to instantiate
     * @param args       constructor arguments passed to the agent's {@code setup()}
     * @return the started {@link AgentController}
     */
    public synchronized AgentController startAgent(
            String localName, Class<?> agentClass, Object[] args)
            throws StaleProxyException {

        if (mainContainer == null) {
            startMainContainer();
        }
        AgentController controller = mainContainer.createNewAgent(
                localName,
                agentClass.getName(),
                args == null ? new Object[0] : args);
        controller.start();
        log.info("AgentPlatformManager: Agent '{}' started.", localName);
        return controller;
    }

    /**
     * Low-level factory kept for backward compatibility with any existing callers
     * that use the old {@code createAgent} signature.
     */
    public synchronized AgentController createAgent(String localName, String className, Object[] args)
            throws StaleProxyException {
        if (mainContainer == null) {
            startMainContainer();
        }
        return mainContainer.createNewAgent(localName, className, args == null ? new Object[0] : args);
    }
}
