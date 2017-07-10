package ru.sherb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.sherb.handlers.UdpMessageHandler;

public class Launcher {

    final static Logger log = LoggerFactory.getLogger(Launcher.class);

    public static void main(String[] args) throws InstantiationException, IllegalAccessException {

        final int port = args.length == 1 ? Integer.valueOf(args[0]) : 8021;

        ServerP2P server = new ServerP2P(port, UdpMessageHandler.class);

        server.run();
    }
}
