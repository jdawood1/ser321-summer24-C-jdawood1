package taskledger.node;

import java.io.*;
import java.net.Socket;
import java.util.Random;

public class Node {
    private static final String HOST = "localhost";
    private static final int PORT = 8000;
    private static boolean isFaulty = false;

    public static void main(String[] args) {
        parseArguments(args);
        connectToLeader();
    }

    private static void parseArguments(String[] args) {
        for (String arg : args) {
            if ("-fault".equals(arg)) {
                isFaulty = true;
                System.out.println("Node is set to faulty mode. It will return incorrect calculations.");
            }
        }
    }

    private static void connectToLeader() {
        try (Socket socket = new Socket(HOST, PORT)) {
            System.out.println("Node connected to Leader on port " + PORT + "...");
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            String leaderMessage;
            while ((leaderMessage = in.readLine()) != null) {
                handleLeaderMessage(leaderMessage, in, out);
            }
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        }
    }

    private static void handleLeaderMessage(String leaderMessage, BufferedReader in, PrintWriter out) throws IOException {
        switch (leaderMessage) {
            case "READY":
                handleTaskMessage(in, out);
                break;
            case "CLOSE":
                System.out.println("Received 'CLOSE' from Leader. Closing connection.");
                break;
            default:
                if (leaderMessage.startsWith("VERIFY_SUM:")) {
                    handleVerificationMessage(leaderMessage, out);
                } else {
                    System.out.println("Unexpected message from Leader: " + leaderMessage);
                }
                break;
        }
    }

    private static void handleTaskMessage(BufferedReader in, PrintWriter out) throws IOException {
        String taskMessage = in.readLine();
        if (taskMessage != null && taskMessage.startsWith("TASK:")) {
            System.out.println("Received task from Leader: " + taskMessage);
            String[] parts = taskMessage.split(" NODE_ID:");
            if (parts.length < 2) {
                System.out.println("Error: Incorrect task message format from Leader.");
                return;
            }

            String currentTask = parts[0].substring(5).trim();
            int sum = calculateSum(currentTask);
            sum = modifySumIfFaulty(sum, "Node is faulty. Sending incorrect sum: ");

            out.println("Partial sum: " + sum);
            System.out.println("Partial sum sent to Leader: " + sum);
        }
    }

    private static void handleVerificationMessage(String leaderMessage, PrintWriter out) {
        String[] parts = leaderMessage.split(", TASK:");
        if (parts.length < 2) {
            System.out.println("Error: Incorrect verification message format from Leader.");
            return;
        }

        int verificationSum = Integer.parseInt(parts[0].substring(11).trim());
        String taskToVerify = parts[1].trim();
        System.out.println("Verification request from Leader. Expected sum: " + verificationSum);

        int recalculatedSum = calculateSum(taskToVerify);
        recalculatedSum = modifySumIfFaulty(recalculatedSum, "Node is faulty during verification. recalculatedSum: ");

        if (recalculatedSum == verificationSum) {
            out.println("YES");
            System.out.println("Verification successful. Sent 'YES' to Leader.");
        } else {
            out.println("NO");
            System.out.println("Verification failed. Sent 'NO' to Leader.");
        }
    }

    private static int modifySumIfFaulty(int sum, String logMessage) {
        if (isFaulty) {
            sum += new Random().nextInt(10) + 1; // Add some random error
            System.out.println(logMessage + sum);
        }
        return sum;
    }

    private static int calculateSum(String numbersString) {
        if (numbersString == null || numbersString.isEmpty()) {
            return 0; // Return 0 if the string is empty or null
        }

        String[] numbers = numbersString.split(",");
        int sum = 0;
        for (String num : numbers) {
            sum += Integer.parseInt(num.trim());
        }
        return sum;
    }
}
