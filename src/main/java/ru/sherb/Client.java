package ru.sherb;

import java.net.*;
import java.io.*;

public class Client {
    static final int SERVER_PORT = 8021;
    static final String SERVER_ADDRESS = "145.255.21.161";

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in))) {

            boolean stop = false;
            while (!stop) {
                System.out.println("Please write new message\n");
                String line = keyboard.readLine();
                if (line.equals("q")) {
                    stop = true;

                }
                System.out.println("Sending this line to the server...");

                out.writeUTF(line);
                out.flush();

                System.out.println("Waiting for request from server");
                line = in.readUTF();
                System.out.println("ServerP2P sending: " + line);

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
