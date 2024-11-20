package taskledger.leader;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Leader {
    private static final int PORT = 8000;
    private static final int NODE_CONNECTION_TIMEOUT = 90000; // Timeout (90 seconds)
    private static final int MIN_NODES_REQUIRED = 3;
    private static final List<Socket> nodeSockets = new ArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Leader is waiting for client connection on port " + PORT + "...");

            Socket clientSocket = null;
            try {
                // 1. Accept Client Connection
                clientSocket = serverSocket.accept();
                System.out.println("Client connected.");

                BufferedReader clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter clientOut = new PrintWriter(clientSocket.getOutputStream(), true);

                // 2. Handle Client Greeting
                String clientMessage = clientIn.readLine();
                if (clientMessage == null || !clientMessage.startsWith("GREETING:")) {
                    clientOut.println("Error: Invalid greeting from client.");
                    return;
                }
                System.out.println("Received from client: " + clientMessage);
                clientOut.println("Acknowledged: " + clientMessage);

                // 3. Receive Numbers and Delay from Client
                clientMessage = clientIn.readLine();
                if (clientMessage == null || !clientMessage.startsWith("NUMBERS:")) {
                    clientOut.println("Error: Invalid numbers input from client.");
                    return;
                }

                String[] parts = clientMessage.split(", DELAY:");
                if (parts.length < 2) {
                    clientOut.println("Error: Missing delay value.");
                    return;
                }

                String[] numbers = parts[0].substring(8).trim().split(",");
                int[] numberList = new int[numbers.length];
                try {
                    for (int i = 0; i < numbers.length; i++) {
                        numberList[i] = Integer.parseInt(numbers[i].trim());
                    }
                } catch (NumberFormatException e) {
                    clientOut.println("Error: Invalid number format.");
                    return;
                }

                int delay;
                try {
                    delay = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    clientOut.println("Error: Invalid delay value.");
                    return;
                }

                clientOut.println("Numbers received and acknowledged.");

                // 4. Perform Single-Sum Calculation
                System.out.println("Performing single-sum calculation for comparison...");
                long singleStartTime = System.currentTimeMillis();
                int singleSum = 0;
                try {
                    for (int num : numberList) {
                        singleSum += num;
                        Thread.sleep(delay);
                    }
                } catch (InterruptedException e) {
                    System.out.println("Error during single-sum calculation: " + e.getMessage());
                }
                long singleTimeTaken = System.currentTimeMillis() - singleStartTime;
                System.out.printf("Single-sum calculation completed. Result: %d, Time Taken: %d ms%n", singleSum, singleTimeTaken);
                clientOut.printf("SINGLE_RESULT: Single sum calculation result: %d, Time Taken: %d ms%n", singleSum, singleTimeTaken);

                // 5. Wait for Minimum Number of Node Connections
                System.out.println("Leader is now waiting for at least " + MIN_NODES_REQUIRED + " node connections...");
                long startTime = System.currentTimeMillis();
                while (nodeSockets.size() < MIN_NODES_REQUIRED && (System.currentTimeMillis() - startTime) < NODE_CONNECTION_TIMEOUT) {
                    serverSocket.setSoTimeout(1000);
                    try {
                        Socket nodeSocket = serverSocket.accept();
                        nodeSockets.add(nodeSocket);
                        System.out.println("Node connected. Total connected nodes: " + nodeSockets.size());
                    } catch (IOException e) {
                        // Continue waiting for more nodes
                    }
                }

                if (nodeSockets.size() < MIN_NODES_REQUIRED) {
                    clientOut.println("Error: Not enough nodes connected. Need at least " + MIN_NODES_REQUIRED + ".");
                    return;
                }

                // 6. Send READY Signal to Nodes
                System.out.println("All required nodes connected. Sending 'READY' to nodes...");
                for (Socket nodeSocket : nodeSockets) {
                    try {
                        PrintWriter nodeOut = new PrintWriter(nodeSocket.getOutputStream(), true);
                        nodeOut.println("READY");
                    } catch (IOException e) {
                        System.out.println("Error sending 'READY' to node: " + e.getMessage());
                    }
                }

                // 7. Distribute Task Among Nodes
                int splitSize = numberList.length / nodeSockets.size();
                List<NodeHandler> handlers = new ArrayList<>();
                for (int i = 0; i < nodeSockets.size(); i++) {
                    Socket nodeSocket = nodeSockets.get(i);
                    int start = i * splitSize;
                    int end = (i == nodeSockets.size() - 1) ? numberList.length : start + splitSize;
                    StringBuilder task = new StringBuilder();
                    for (int j = start; j < end; j++) {
                        task.append(numberList[j]).append(",");
                    }

                    NodeHandler handler = new NodeHandler(nodeSocket, task.toString().trim(), (i + 1));
                    handler.start();
                    handlers.add(handler);
                }

                // 8. Collect Partial Sums from Nodes
                List<Integer> partialSums = new ArrayList<>();
                for (NodeHandler handler : handlers) {
                    try {
                        handler.join();
                        if (handler.hasResponded()) {
                            partialSums.add(handler.getPartialSum());
                        } else {
                            System.out.println("Node " + handler.getId() + " did not respond.");
                            clientOut.println("ERROR: Not all nodes responded. Ending process.");
                            return;
                        }
                    } catch (InterruptedException e) {
                        System.out.println("Error waiting for NodeHandler: " + e.getMessage());
                        clientOut.println("ERROR: Interrupted while waiting for node responses.");
                        return;
                    }
                }

                // 9. Perform Consensus Check
                int nodesAgreed = 0;
                for (int i = 0; i < handlers.size(); i++) {
                    NodeHandler handler = handlers.get(i);
                    if (!handler.hasResponded()) {
                        continue;
                    }

                    Socket nodeSocket = handler.getNodeSocket();
                    if (nodeSocket == null || nodeSocket.isClosed()) {
                        continue;
                    }

                    int nextIndex = (i + 1) % handlers.size();
                    NodeHandler targetHandler = handlers.get(nextIndex);
                    int sumToVerify = targetHandler.getPartialSum();
                    String taskToVerify = targetHandler.getTask();

                    try {
                        PrintWriter out = new PrintWriter(nodeSocket.getOutputStream(), true);
                        out.println("VERIFY_SUM: " + sumToVerify + ", TASK: " + taskToVerify);
                        System.out.printf("Sent VERIFY message to Node %d with sum: %d and task: %s%n", handler.getId(), sumToVerify, taskToVerify);

                        BufferedReader in = new BufferedReader(new InputStreamReader(nodeSocket.getInputStream()));
                        String response = in.readLine();
                        if ("YES".equals(response)) {
                            nodesAgreed++;
                            System.out.printf("Node %d agrees with the verification of sum: %d from Node %d%n", handler.getId(), sumToVerify, targetHandler.getId());
                        } else {
                            System.out.printf("Node %d disagrees with the sum: %d from Node %d and responded with: %s%n", handler.getId(), sumToVerify, targetHandler.getId(), response);
                        }
                    } catch (IOException e) {
                        System.out.println("Timeout or error while waiting for node " + handler.getId() + " during verification: " + e.getMessage());
                    }
                }

                // 10. Determine Final Result Based on Consensus
                if (nodesAgreed > (handlers.size() / 2)) { // by majority vote
                    System.out.println("Consensus achieved. " + nodesAgreed + " out of " + handlers.size() + " nodes agreed.");
                    int totalSum = partialSums.stream().mapToInt(Integer::intValue).sum();
                    clientOut.printf("RESULT: %d%n", totalSum);
                    System.out.printf("Total sum calculated by Leader: %d%n", totalSum);
                } else {
                    System.out.println("Consensus failed. Only " + nodesAgreed + " out of " + handlers.size() + " nodes agreed.");
                    clientOut.println("ERROR: Consensus failed. Results from nodes are inconsistent.");
                }

                // 11. Close Node Connections
                for (Socket nodeSocket : nodeSockets) {
                    try {
                        nodeSocket.close();
                    } catch (IOException e) {
                        System.out.println("Error closing node socket: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                System.out.println("Client communication error: " + e.getMessage());
            } finally {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    try {
                        clientSocket.close();
                    } catch (IOException e) {
                        System.out.println("Error closing client socket: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Server socket error: " + e.getMessage());
        }
    }
}
