package taskledger.leader;

import java.io.*;
import java.net.Socket;

public class NodeHandler extends Thread {
    private final Socket nodeSocket;
    private final String task;
    private int partialSum;
    private boolean hasResponded = false;
    private final long id;

    public NodeHandler(Socket socket, String task, int id) {
        this.nodeSocket = socket;
        this.task = task;
        this.id = id;
    }

    @Override
    public void run() {
        try {
            PrintWriter out = new PrintWriter(nodeSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(nodeSocket.getInputStream()));

            // Send the task and node ID
            out.println("TASK: " + task + " NODE_ID: " + id);
            System.out.println("Leader sent task to Node " + id + ": " + task);

            nodeSocket.setSoTimeout(15000); // 15-second timeout

            // Receive the partial sum
            String nodeResponse = in.readLine();
            if (nodeResponse != null && nodeResponse.startsWith("Partial sum:")) {
                partialSum = Integer.parseInt(nodeResponse.substring(13).trim());
                System.out.println("Received partial sum from Node " + id + ": " + partialSum);
                hasResponded = true; // Node has responded
            }

        } catch (IOException e) {
            System.out.println("Error or timeout while waiting for node " + id + " response: " + e.getMessage());
        }
    }

    public int getPartialSum() {
        return partialSum;
    }
    public boolean hasResponded() {
        return hasResponded;
    }
    public Socket getNodeSocket() {
        return nodeSocket;
    }
    public String getTask() {
        return task;
    }
    public long getId() {
        return id;
    }
}

