package taskledger.client;

import java.io.*;
import java.net.Socket;

public class Client {
    private static final String HOST = "localhost";
    private static final int PORT = 8000;

    public static void main(String[] args) {
        String[] input = parseArguments(args);
        String numbers = input[0];
        int delay = Integer.parseInt(input[1]);

        // Connecting to the Leader
        try (Socket socket = new Socket(HOST, PORT)) {
            System.out.println("Client connected to Leader on port " + PORT + "...");

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // Send a greeting message to the Leader
            out.println("GREETING: Hello Leader!");
            String leaderResponse = in.readLine();
            System.out.println("Leader says: " + leaderResponse);

            // Send the numbers and delay time to the Leader
            out.println("NUMBERS: " + numbers + ", DELAY: " + delay);
            leaderResponse = in.readLine();
            System.out.println("Leader says: " + leaderResponse);

            // Handle response from the Leader
            while ((leaderResponse = in.readLine()) != null) {
                if (leaderResponse.startsWith("SINGLE_RESULT:")) {
                    System.out.println(leaderResponse.substring(14).trim());
                } else if (leaderResponse.startsWith("RESULT:")) {
                    System.out.println("Final Result received from Leader: " + leaderResponse.substring(7).trim());
                } else if (leaderResponse.startsWith("ERROR:")) {
                    System.out.println(leaderResponse.substring(6).trim());
                }else {
                    System.out.println("Unexpected message from Leader: " + leaderResponse);
                }
            }
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        }
    }

    private static String[] parseArguments(String[] args) {
        String numbers = "1,2,3,4,5,6";
        String delay = "50";

        if (args.length >= 2) {
            numbers = args[0];
            try {
                Integer.parseInt(args[1]); // Just for validation
                delay = args[1];
            } catch (NumberFormatException e) {
                System.out.println("Invalid delay value provided. Using default delay of 50 ms.");
            }
        } else {
            System.out.println("No arguments provided. Using default values: numbers = " + numbers + ", delay = " + delay);
        }
        return new String[]{numbers, delay};
    }
}
