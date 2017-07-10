package ru.sherb;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TimerTask;

public class LifetimePortUpdater extends TimerTask {

    final static Logger log = LoggerFactory.getLogger(LifetimePortUpdater.class);

    final NatPmpPortOpener updateInstance;

    public LifetimePortUpdater(NatPmpPortOpener update) {
        updateInstance = update;
    }

    @Override
    public void run() {
        log.info("refresh the port");
        try {
            updateInstance.setPort(updateInstance.getMapper().refreshPort(updateInstance.getPort(), updateInstance.getPort().getLifetime() / 2));

        } catch (InterruptedException e) {
            log.warn(e.getMessage(), e);
        }

        log.info("Port mapping refreshed: {} new lifetime: {} sec", updateInstance.getPort(), updateInstance.getPort().getLifetime());
    }
}
