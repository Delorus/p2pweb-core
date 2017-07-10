package ru.sherb.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;

public class EchoHandler implements IHandler {

    private final static Logger log = LoggerFactory.getLogger(EchoHandler.class);

    private boolean execute;
    private int port;

    @Override
    public void init(int port) {
        this.port = port;
    }

    public void start() {
        execute = true;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log.info("Waiting for a client...");

            Socket socket = serverSocket.accept();
            log.info("Client connected successful: " + socket.getRemoteSocketAddress());

            try (DataInputStream in = new DataInputStream(socket.getInputStream());
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

                out.write("you connected successful".getBytes(Charset.forName("UTF-8")));


                while (execute) {
                    if (socket.isInputShutdown()) System.out.println("input shutdown!");
                    byte[] buf = new byte[255];
                    int count = -1;
                    try {
                        count = in.read(buf);
                    } catch (SocketException e) {
                        log.warn(e.getMessage(), e);
                        setExecute(false);
                    }
                    log.info("read " + count + " bytes success");
                    log.info("Message from client: " + new String(buf));

                    log.info("Send back...");
                    out.write(buf);
                    out.flush();

                    log.info("Waiting for the next line...\n");
                }
            }

        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public boolean isExecute() {
        return execute;
    }

    public void setExecute(boolean execute) {
        this.execute = execute;
    }
}
