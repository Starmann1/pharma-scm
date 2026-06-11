package pharma;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pharma.agent.platform.AgentGateway;
import pharma.agent.platform.AgentPlatformManager;
import pharma.config.ApplicationServices;
import pharma.gateway.PharmaGateway;
import pharma.gui.LoginGUI;
import pharma.service.DatabaseService;

/**
 * Application entry point — Agentic Pharma SCM (V1: JADE + LangChain4j).
 *
 * <p>Startup sequence:
 * <ol>
 *   <li>Register a JVM shutdown hook to close HikariCP and JADE cleanly.</li>
 *   <li>Test the database connection (fail-fast with a dialog if unreachable).</li>
 *   <li>Build the {@link ApplicationServices} composition root.</li>
 *   <li>Create the {@link AgentGateway} (implements {@link PharmaGateway}).</li>
 *   <li>Start all JADE agents (Phase 6 core + V1 additions).</li>
 *   <li>Launch the Swing {@link LoginGUI} on the EDT.</li>
 * </ol>
 *
 * <p>Architecture rule: {@link ApplicationServices}, {@link PharmaGateway}, and
 * {@link AgentPlatformManager} are created once here and injected down into
 * panels that need them. No static singletons.
 */
public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    /** Shared gateway — the ONLY interface between UI and agent layer. */
    private static PharmaGateway gateway;

    /** The V1 JADE-specific gateway instance (for complete() callback access). */
    private static AgentGateway agentGateway;

    /** Platform manager — exposed so panels can query platform state if needed. */
    private static AgentPlatformManager platformManager;

    /** Application services — the composition root. */
    private static ApplicationServices appServices;

    public static void main(String[] args) {
        // ------------------------------------------------------------------
        // 1. Register clean-shutdown hook (runs when JVM exits)
        // ------------------------------------------------------------------
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown hook triggered — cleaning up resources...");
            if (gateway != null) {
                gateway.shutdown();
                log.info("Agent platform stopped via PharmaGateway.shutdown().");
            }
            DatabaseService.closePool();
            log.info("HikariCP connection pool closed.");
        }, "pharma-shutdown-hook"));

        // ------------------------------------------------------------------
        // 2. Database connection test (done off the EDT — before Swing starts)
        // ------------------------------------------------------------------
        DatabaseService dbService = new DatabaseService();
        if (!dbService.connect()) {
            JOptionPane.showMessageDialog(null,
                    "Failed to connect to the database.\n" +
                    "Check your MySQL server status and the .env configuration.",
                    "Fatal Connection Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        log.info("Database connection established successfully.");

        // ------------------------------------------------------------------
        // 3. Build ApplicationServices (single composition root)
        // ------------------------------------------------------------------
        appServices = new ApplicationServices(dbService);
        log.info("ApplicationServices composition root initialised.");

        // ------------------------------------------------------------------
        // 4. Create the AgentGateway (V1: JADE + LangChain4j)
        // ------------------------------------------------------------------
        agentGateway = new AgentGateway();
        gateway = agentGateway; // PharmaGateway interface reference

        // ------------------------------------------------------------------
        // 5. Start the JADE platform and all V1 operational agents
        // ------------------------------------------------------------------
        platformManager = new AgentPlatformManager(appServices);
        try {
            platformManager.startAllOperationalAgents(agentGateway);
            log.info("All V1 operational agents started successfully.");
        } catch (Exception e) {
            log.error("Failed to start JADE agent platform: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(null,
                    "Failed to start the agent platform:\n" + e.getMessage() +
                    "\n\nThe application will start in degraded mode (no agent orchestration).",
                    "Agent Platform Warning",
                    JOptionPane.WARNING_MESSAGE);
            // Non-fatal — application continues without agents in degraded mode
        }

        // ------------------------------------------------------------------
        // 6. Launch Swing UI on the Event Dispatch Thread
        // ------------------------------------------------------------------
        SwingUtilities.invokeLater(() -> {
            log.info("Launching Swing UI on EDT.");
            LoginGUI login = new LoginGUI(dbService);
            login.setVisible(true);
        });
    }

    // -------------------------------------------------------------------------
    // Static accessors — for panels that need agent interaction
    // -------------------------------------------------------------------------

    /**
     * Returns the shared {@link PharmaGateway} for submitting async agent requests.
     * May return {@code null} if the agent platform failed to start (degraded mode).
     */
    public static PharmaGateway getGateway() {
        return gateway;
    }

    /**
     * Returns the V1-specific {@link AgentGateway} for JADE-specific operations.
     * Prefer using {@link #getGateway()} for version-independent code.
     */
    public static AgentGateway getAgentGateway() {
        return agentGateway;
    }

    /**
     * Returns the {@link AgentPlatformManager} for platform state queries.
     */
    public static AgentPlatformManager getPlatformManager() {
        return platformManager;
    }

    /**
     * Returns the {@link ApplicationServices} composition root.
     * Used by GUI panels that need direct service access (e.g., AI Decision Dashboard).
     */
    public static ApplicationServices getAppServices() {
        return appServices;
    }
}
