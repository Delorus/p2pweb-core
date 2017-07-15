package ru.sherb.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;

public class UdpMessageHandler implements IHandler {

    private final static String STOP_CODE = "stope963c8169a23a54d5f4eee4a7c1f269c95e94d4f";

    private final static Logger log = LoggerFactory.getLogger(UdpMessageHandler.class);

    private final LinkedBlockingQueue<String> msgFromKeyboard = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<String> msgFromNode = new LinkedBlockingQueue<>(); //TODO изменить на String
    private final List<InetSocketAddress> receivers = new ArrayList<>();

    private DatagramSocket socket;

    private volatile boolean stop = false;


    class KeyHandler implements Runnable {

        @Override
        public void run() {
            log.info("start key handler");
            try (Scanner scanner = new Scanner(System.in)) {
                while (!stop && scanner.hasNext()) {
                    final String message = scanner.nextLine();
                    if ("stop".equals(message)) { stop = true; break; }
                    msgFromKeyboard.put(message);
                }



            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);

            } finally {
                log.info("STOP {}", this);
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
                while (!stop) {

                    final DatagramPacket packet = new DatagramPacket(new byte[256], 256); //TODO разобраться с размером

                    socket.receive(packet); //TODO добавить выход, если пришло кодовое значение
                    log.info("packet received from {}:{}", packet.getAddress().getHostAddress(), packet.getPort());

                    final InetSocketAddress address = new InetSocketAddress(packet.getAddress(), packet.getPort());

                    if (!"ok".equals(new String(packet.getData()))) {
                        sendOK(address);
                        continue;
                    }

                    msgFromNode.put(new String(packet.getData(), Charset.forName("UTF-8")));
                    if (!receivers.contains(address)) {
                        receivers.add(address);
                    }
                }

            } catch (IOException | InterruptedException e) {
                log.error(e.getMessage(), e);

            } finally {
                log.info("STOP {}", this);
            }
        }

        private void sendOK(InetSocketAddress address) throws IOException {
            final String okMsg = "ok";
            final DatagramPacket packet = new DatagramPacket(okMsg.getBytes(), okMsg.length(), address);
            log.debug("send OK packet from {}:{}", address.getAddress().getHostAddress(), address.getPort());
            socket.send(packet);
        }
    }

    class MessageSender implements Runnable {

        @Override
        public void run() {
            log.info("start message sender");
            if (socket.isClosed()) {
                log.error("socket is closed");
                log.error("STOP {}", MessageSender.class);
                return;
            }

            if (receivers.isEmpty()) {
                log.warn("No recipients");
            }

            try {
                while (!stop) {
                    final String msg = msgFromKeyboard.take();
                    if (msg.startsWith("stop")) {
                        if (STOP_CODE.equals(msg)) {
                            break;
                        }
                    }

                    final DatagramPacket packet = new DatagramPacket(msg.getBytes("UTF-8"), msg.length());

                    if (receivers.isEmpty()) {
                        log.warn("No recipients");

                    } else {
                        receivers.forEach(address -> {
                            packet.setSocketAddress(address);
                            try {
                                log.info("send to remote address = {}:{}", packet.getAddress(), packet.getPort());
                                socket.send(packet);

                            } catch (IOException e) {
                                log.error(e.getMessage(), e);
                            }
                        });
                    }
                }

            } catch (UnsupportedEncodingException | InterruptedException e) {
                log.error(e.getMessage(), e);

            } finally {
                log.info("STOP {}", this);
            }
        }
    }

    class MessageDisplay implements Runnable {

        @Override
        public void run() {
            log.info("start message display");

            try {
                while(!stop) {
                    final String msg = msgFromNode.take();
                    System.out.println(">>> " + msg);
                }

            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);

            } finally {
                log.info("STOP {}", this);
            }
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

    @Override
    public void start() {

        final Thread msgHandler = new Thread(new MessageHandler(), MessageHandler.class.getName());
        final Thread msgDisplay = new Thread(new MessageDisplay(), MessageDisplay.class.getName());
        final Thread msgKeyboard = new Thread(new KeyHandler(), KeyHandler.class.getName());
        final Thread msgSender = new Thread(new MessageSender(), MessageSender.class.getName());

        msgHandler.start();
        msgDisplay.start();
        msgKeyboard.start();
        msgSender.start();

        try {
            msgKeyboard.join();
            close();

        } catch (InterruptedException | IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void close() throws IOException, InterruptedException {
        msgFromKeyboard.put(STOP_CODE);
        socket.send(new DatagramPacket(
                STOP_CODE.getBytes(),
                STOP_CODE.length(),
                InetAddress.getLoopbackAddress(),
                socket.getLocalPort()
        ));
        msgFromNode.put(STOP_CODE);
    }

    private List<InetSocketAddress> load(String path) {

        final List<InetSocketAddress> addresses = new ArrayList<>();

        if (path == null || path.isEmpty()) {
            path = "nodes";
        }


        log.info("load addresses from file '{}'", path);
        final File nodes = new File(path);
        if (nodes.exists() && nodes.canRead()) {
            try (Scanner scanner = new Scanner(nodes)) {
                while (scanner.hasNext()) {
                    final String[] rawAddress = scanner.nextLine().split(":");
                    final InetSocketAddress address = new InetSocketAddress(rawAddress[0], Integer.valueOf(rawAddress[1]));
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
            log.warn("nodes not found");
        }

        return addresses;
    }
}
