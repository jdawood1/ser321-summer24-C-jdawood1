/**
  File: ThreadPoolServer.java
 Author: Student in Fall 2020B
  Description: Server class in package taskone.
*/

package taskone;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;


/**
 * Class: ThreadPoolServer
 * Description: Server with a fixed-size thread pool to manage client connections concurrently.
 */
public class ThreadPoolServer {

    static Performer performer;

    public static void main(String[] args) throws Exception {
        int port;
        int maxThreads = 4;  // Default number of threads in the pool
        StringList strings = new StringList();
        performer = new Performer(strings);

        if (args.length < 1) {
            // gradle runTask3 -Pport=9099 -Pthreads=4 -q --console=plain
            System.out.println("Usage: gradle runTask3 -Pport=9099 -Pthreads=4 -q --console=plain");
            System.exit(1);
        }

        try {
            port = Integer.parseInt(args[0]);
            if (args.length == 2) {
                maxThreads = Integer.parseInt(args[1]);
            }
        } catch (NumberFormatException nfe) {
            System.out.println("[Port] and [Threads] must be integers");
            System.exit(2);
            return;
        }

        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Thread-Pool Server Started...");
        ExecutorService threadPool = Executors.newFixedThreadPool(maxThreads);

        while (true) {
            System.out.println("Accepting a Request...");
            Socket clientSocket = serverSocket.accept();
            threadPool.execute(new ClientHandler(clientSocket));
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