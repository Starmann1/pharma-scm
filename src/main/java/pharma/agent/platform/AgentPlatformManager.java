package pharma.agent.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

public class AgentPlatformManager {
    private static final Logger logger = LoggerFactory.getLogger(AgentPlatformManager.class);

    private Runtime runtime;
    private ContainerController mainContainer;

    public synchronized void startMainContainer() {
        if (mainContainer != null) {
            return;
        }
        runtime = Runtime.instance();
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.GUI, "false");
        mainContainer = runtime.createMainContainer(profile);
        logger.info("JADE main container initialized in headless mode.");
    }

    public synchronized AgentController createAgent(String localName, String className, Object[] args)
            throws StaleProxyException {
        if (mainContainer == null) {
            startMainContainer();
        }
        return mainContainer.createNewAgent(localName, className, args == null ? new Object[0] : args);
    }

    public synchronized boolean isStarted() {
        return mainContainer != null;
    }

    public synchronized void shutdown() {
        if (mainContainer != null) {
            try {
                mainContainer.kill();
            } catch (StaleProxyException e) {
                logger.warn("Failed to stop JADE container cleanly: {}", e.getMessage());
            } finally {
                mainContainer = null;
                runtime = null;
            }
        }
    }
}
