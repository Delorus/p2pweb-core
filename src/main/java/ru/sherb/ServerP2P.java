package ru.sherb;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.sherb.handlers.IHandler;

public class ServerP2P implements Runnable {

    private final static Logger log = LoggerFactory.getLogger(ServerP2P.class);

    private final int port;
    private final IHandler handler;

    public <T extends IHandler> ServerP2P(int port, Class<T> handler) throws IllegalAccessException, InstantiationException {
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

        //TODO сделать NAT-проброс
//        final NatPmpPortOpener opener = new NatPmpPortOpener();
//        try {
//            opener.open(port);
//
//        } catch (InterruptedException e) {
//            log.error(e.getMessage(), e);
//        }

//        try {
            handler.init(port);

            handler.start();

//        } finally {
//            opener.close();
//        }
        log.info("server shutdown");
    }
}
