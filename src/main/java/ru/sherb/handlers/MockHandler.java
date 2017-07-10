package ru.sherb.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockHandler implements IHandler {

    static final Logger log = LoggerFactory.getLogger(MockHandler.class);

    @Override
    public void start() {
        log.info("start mock handler...");
        try {
            Thread.sleep(2000);

        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }

        log.info("end mock handler");
    }

    @Override
    public void init(int port) {
        log.info("init mock handler");
    }
}
