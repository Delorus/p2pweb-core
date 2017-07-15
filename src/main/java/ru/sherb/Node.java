package ru.sherb;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.sherb.handlers.IHandler;
import ru.sherb.nat.NatPmpPortOpener;

public class Node implements Runnable {

    private final static Logger log = LoggerFactory.getLogger(Node.class);

    private int port;
    private final IHandler handler;

    public <T extends IHandler> Node(int port, Class<T> handler) throws IllegalAccessException, InstantiationException {
        this.port = port;
        try {
            this.handler = handler.newInstance();

        } catch (InstantiationException | IllegalAccessException e) {
            log.error(e.getMessage(), e);
            throw e;
        }

    }

    @Override
    public void run() {

        try (NatPmpPortOpener opener = new NatPmpPortOpener(port)) { //TODO слишком сильная зависимость с пробросом порта

            port = opener.open();

            handler.init(port);
            handler.start();
        }
        log.info("node shutdown");
    }
}
