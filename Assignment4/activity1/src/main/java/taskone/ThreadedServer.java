/**
 File: ThreadedServer.java
 Author: Student in Fall 2020B
 Description: Server class in package taskone.
 */

package taskone;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import org.json.JSONObject;

/**
 * Class: ThreadedServer
 * Description: Multi-threaded server that can handle multiple clients concurrently.
 */
public class ThreadedServer {

    static Performer performer;

    public static void main(String[] args) throws Exception {
        int port;
        StringList strings = new StringList();
        performer = new Performer(strings);

        if (args.length != 1) {
            // gradle runTask2 -Pport=9099 -q --console=plain
            System.out.println("Usage: gradle runTask2 -Pport=9099 -q --console=plain");
            System.exit(1);
        }

        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port] must be an integer");
            System.exit(2);
            return;
        }

        ServerSocket server = new ServerSocket(port);
        System.out.println("Multi-threaded Server Started...");

        while (true) {
            System.out.println("Accepting a Request...");
            Socket clientSocket = server.accept();
            new Thread(new ClientHandler(clientSocket)).start();  // Create and start a new thread for each client
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (OutputStream out = clientSocket.getOutputStream();
                 InputStream in = clientSocket.getInputStream()) {

                boolean quit = false;
                System.out.println("Server connected to client in a new thread.");
                while (!quit) {
                    byte[] messageBytes = NetworkUtils.receive(in);
                    if (messageBytes.length == 0) {
                        continue;
                    }

                    JSONObject message = JsonUtils.fromByteArray(messageBytes);
                    JSONObject returnMessage = new JSONObject();

                    int choice = message.getInt("selected");
                    switch (choice) {
                        case 1:
                            String inStr = message.getString("data");
                            synchronized (performer) {  // Synchronize to make shared state thread-safe
                                returnMessage = performer.add(inStr);
                            }
                            break;
                        case 2:
                            synchronized (performer) {  // Synchronize display to ensure thread safety
                                returnMessage = performer.display();
                            }
                            break;
                        case 3:
                            synchronized (performer) {  // Synchronize count to ensure thread safety
                                returnMessage = performer.count();
                            }
                            break;
                        case 0:
                            returnMessage = performer.quit();
                            quit = true;
                            break;
                        default:
                            returnMessage = performer.error("Invalid selection: " + choice + " is not an option");
                            break;
                    }

                    // Convert JSON object to byte array and send it back to the client
                    byte[] output = JsonUtils.toByteArray(returnMessage);
                    NetworkUtils.send(out, output);
                }

                System.out.println("Client disconnected, closing resources.");
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}