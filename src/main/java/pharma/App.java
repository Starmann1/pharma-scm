package pharma;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pharma.agent.platform.AgentGateway;
import pharma.agent.platform.AgentPlatformManager;
import pharma.config.ApplicationServices;
import pharma.gui.LoginGUI;
import pharma.service.DatabaseService;

/**
 * Application entry point — Agentic Pharma SCM.
 *
 * <p>Startup sequence:
 * <ol>
 *   <li>Register a JVM shutdown hook to close HikariCP and JADE cleanly.</li>
 *   <li>Test the database connection (fail-fast with a dialog if unreachable).</li>
 *   <li>Build the {@link ApplicationServices} composition root.</li>
 *   <li>Create the {@link AgentGateway} and {@link AgentPlatformManager}.</li>
 *   <li>Start all Phase-6 operational JADE agents.</li>
 *   <li>Launch the Swing {@link LoginGUI} on the EDT.</li>
 * </ol>
 *
 * <p>Architecture rule: {@link ApplicationServices}, {@link AgentGateway}, and
 * {@link AgentPlatformManager} are created once here and injected down into
 * panels that need them. No static singletons.
 */
public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    /** Shared gateway — injected into UI panels that need async agent calls. */
    private static AgentGateway agentGateway;

    /** Platform manager — exposed so panels can query platform state if needed. */
    private static AgentPlatformManager platformManager;

    public static void main(String[] args) {
        // ------------------------------------------------------------------
        // 1. Register clean-shutdown hook (runs when JVM exits)
        // ------------------------------------------------------------------
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown hook triggered — cleaning up resources...");
            if (platformManager != null) {
                platformManager.shutdown();
                log.info("JADE agent platform stopped.");
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
        ApplicationServices appServices = new ApplicationServices(dbService);
        log.info("ApplicationServices composition root initialised.");

        // ------------------------------------------------------------------
        // 4. Create the AgentGateway (Swing ↔ JADE bridge)
        // ------------------------------------------------------------------
        agentGateway = new AgentGateway();

        // ------------------------------------------------------------------
        // 5. Start the JADE platform and all Phase-6 operational agents
        // ------------------------------------------------------------------
        platformManager = new AgentPlatformManager(appServices);
        try {
            platformManager.startAllOperationalAgents(agentGateway);
            log.info("All Phase-6 operational agents started successfully.");
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
     * Returns the shared {@link AgentGateway} for submitting async agent requests.
     * May return {@code null} if the agent platform failed to start (degraded mode).
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
}
