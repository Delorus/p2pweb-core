package ru.sherb.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class UdpMessageHandler implements IHandler {

    private final static Logger log = LoggerFactory.getLogger(UdpMessageHandler.class);

    private final LinkedBlockingQueue<String> msgFromKeyboard = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<String> msgFromNode = new LinkedBlockingQueue<>(); //TODO изменить на String
    private final List<InetAddress> receivers = new ArrayList<>();

    private DatagramSocket socket;


    class KeyHandler implements Runnable {

        @Override
        public void run() {
            log.info("start key handler");
            try (Scanner scanner = new Scanner(System.in)) {
                while (scanner.hasNext()) {
                    final String message = scanner.nextLine();
                    msgFromKeyboard.put(message);
                }

            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    class MessageHandler implements Runnable {

        @Override
        public void run() {
            log.info("start message handler");
            if (socket.isClosed()) {
                log.error("socket is closed");
                return;
            }

            try {
                while (true) {

                    final DatagramPacket packet = new DatagramPacket(new byte[256], 256);

                    socket.receive(packet);
                    log.info("packet received from {}", packet.getAddress().getHostAddress());
                    msgFromNode.put(new String(packet.getData(), Charset.forName("UTF-8")));

                    if (!receivers.contains(packet.getAddress())) { //TODO изменить List на Set
                        receivers.add(packet.getAddress());
                    }

                }

            } catch (IOException | InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    class MessageSender implements Runnable {

        @Override
        public void run() {
            log.info("start message sender");
            if (socket.isClosed()) {
                log.error("socket is closed");
                log.error("{} stop", MessageSender.class);
                return;
            }

            if (receivers.isEmpty()) {
                log.warn("No recipients");
            }

            try {
                while (true) {
                    final String msg = msgFromKeyboard.take();

                    final DatagramPacket packet = new DatagramPacket(msg.getBytes("UTF-8"), msg.length());

                    if (receivers.isEmpty()) {
                        log.warn("No recipients");

                    } else {
                        receivers.forEach(inetAddress -> {
                            packet.setSocketAddress(new InetSocketAddress(inetAddress, 8021)); //TODO заменить порт
                            try {
                                log.info("send to remote address = {}", packet.getAddress());
                                socket.send(packet);

                            } catch (IOException e) {
                                log.error(e.getMessage(), e);
                            }
                        });
                    }
                }

            } catch (UnsupportedEncodingException | InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    class MessageDisplay implements Runnable {

        @Override
        public void run() {
            log.info("start message display");

            try {
                while(true) {
                    final String msg = msgFromNode.take();
                    System.out.println(">>> " + msg);
                }

            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void start() {

        final Thread msgHandler = new Thread(new MessageHandler());
        msgHandler.start();

        final Thread msgDisplay = new Thread(new MessageDisplay());
        msgDisplay.start();

        final Thread msgKeyboard = new Thread(new KeyHandler());
        msgKeyboard.start();

        final Thread msgSender = new Thread(new MessageSender());
        msgSender.start();

        try {
            msgHandler.join();
            msgDisplay.join();
            msgSender.join();
            msgKeyboard.join();

        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }

    }

    @Override
    public void init(int port) {
        try {
            this.socket = new DatagramSocket(port);

        } catch (SocketException e) {
            log.error(e.getMessage(), e);
        }

        receivers.addAll(load(null));
    }

    private List<InetAddress> load(String path) {

        final List<InetAddress> addresses = new ArrayList<>();

        if (path == null || path.isEmpty()) {
            path = "config";
        }


        log.info("load addresses from file '{}'", path);
        final File config = new File(path);
        if (config.exists() && config.canRead()) {
            try (Scanner scanner = new Scanner(config)) {
                while (scanner.hasNext()) {
                    final InetAddress address = InetAddress.getByName(scanner.nextLine());
                    log.info("load address - {}", address);
                    addresses.add(address);
                }

            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }

            if (addresses.isEmpty()) {
                log.warn("nothing to load");
            }

        } else {
            log.warn("config not found");
        }

        return addresses;
    }
}
