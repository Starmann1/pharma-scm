package pharma.agent.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jade.core.Agent;
import pharma.config.ApplicationServices;

public abstract class BasePharmaAgent extends Agent {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected ApplicationServices services;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length > 0 && args[0] instanceof ApplicationServices applicationServices) {
            this.services = applicationServices;
        }
        logger.info("{} started.", getLocalName());
    }

    @Override
    protected void takeDown() {
        logger.info("{} stopped.", getLocalName());
    }
}
