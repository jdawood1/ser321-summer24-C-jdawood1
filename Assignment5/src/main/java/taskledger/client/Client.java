package taskledger.client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8000;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to the leader at " + SERVER_HOST + ":" + SERVER_PORT);

            // 1. Greet the Leader
            System.out.print("Enter a greeting message for the leader: ");
            String greeting = scanner.nextLine();
            out.println("GREETING:" + greeting);
            String response = in.readLine();
            if (response == null || response.startsWith("Error")) {
                System.out.println("Leader rejected the greeting: " + response);
                return;
            }
            System.out.println("Leader responded: " + response);

            // 2. Ask User for Numbers
            System.out.print("Enter a series of numbers separated by commas (e.g., 1,2,3): ");
            String numbersInput = scanner.nextLine();
            if (numbersInput.trim().isEmpty()) {
                System.out.println("Error: Numbers list cannot be empty.");
                return;
            }

            // Validate input
            String[] numberTokens = numbersInput.split(",");
            try {
                for (String token : numberTokens) {
                    Integer.parseInt(token.trim()); // Validate each number
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid number format. Please enter valid integers separated by commas.");
                return;
            }

            // 3. Ask User for Delay
            System.out.print("Enter a delay in milliseconds (e.g., 1000): ");
            String delayInput = scanner.nextLine();
            int delay;
            try {
                delay = Integer.parseInt(delayInput.trim());
                if (delay < 0) {
                    System.out.println("Delay cannot be negative.");
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid delay. Please enter a valid integer.");
                return;
            }

            // 4. Send Data to the Leader
            out.println("NUMBERS:" + numbersInput + ", DELAY:" + delay);
            response = in.readLine();
            if (response == null || response.startsWith("Error")) {
                System.out.println("Leader rejected the numbers input: " + response);
                return;
            }
            System.out.println("Leader acknowledged numbers: " + response);

            // 5. Wait for Results
            while ((response = in.readLine()) != null) {
                System.out.println("Leader says: " + response);
            }

        } catch (IOException e) {
            System.err.println("Communication error: " + e.getMessage());
        }
    }
}
