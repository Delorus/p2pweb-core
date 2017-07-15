package ru.sherb.nat;


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
            long lifetime = updateInstance.getMappedPort().getLifetime() / 2;
            if (lifetime <= 0) {
                lifetime = updateInstance.getMappedPort().getLifetime();
            }
            updateInstance.setMappedPort(updateInstance.getMapper().refreshPort(updateInstance.getMappedPort(), lifetime));

        } catch (InterruptedException e) {
            log.warn(e.getMessage(), e);
        }

        log.info("Port mapping refreshed: {} new lifetime: {} sec", updateInstance.getMappedPort(), updateInstance.getMappedPort().getLifetime());
    }
}
