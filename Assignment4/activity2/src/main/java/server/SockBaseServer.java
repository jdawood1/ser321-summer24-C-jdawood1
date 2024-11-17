package server;

import buffers.RequestProtos.*;
import buffers.ResponseProtos.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SockBaseServer is a server-side class that handles multiple client connections for a multiplayer Sudoku game.
 * It uses protocol buffers to receive requests and send responses, managing different game operations such as
 * starting a game, updating the board, clearing cells, and handling leaderboard requests. Each client connection
 * is managed in a separate thread to allow concurrent gameplay.
 */
class SockBaseServer implements Runnable {
    static String logFilename = "logs.txt";
    static String menuOptions = "\nWhat would you like to do? \n 1 - to see the leader board \n 2 - to enter a game \n 3 - quit the game";
    static String gameOptions = "\nChoose an action: \n (1-9) - Enter an int to specify the row you want to update \n c - Clear number \n r - New Board";

    private static final String LEADERBOARD_FILE = "leaderboard.txt";
    private static final Map<String, Integer> leaderboard = new ConcurrentHashMap<>();
    private static final Map<String, Integer> loginCounts = new ConcurrentHashMap<>();

    private Socket clientSocket;
    private InputStream in;
    private OutputStream out;
    private final int id; // Client ID
    private Game game;
    private boolean inGame = false;
    private String name;
    private int currentState = 1;
    private static boolean grading = true;

    public SockBaseServer(Socket sock, Game game, int id) {
        this.clientSocket = sock;
        this.game = game;
        this.id = id;
        try {
            in = clientSocket.getInputStream();
            out = clientSocket.getOutputStream();
        } catch (Exception e) {
            System.out.println("Error in constructor: " + e);
        }
    }

    // Run method for handling client connections in separate threads
    public void run() {
        try {
            startGame();
        } catch (IOException e) {
            System.out.println("Error handling client " + id + ": " + e.getMessage());
        }
    }

    /**
     * Main loop to handle client requests and respond appropriately
     */
    public void startGame() throws IOException {
        try {
            while (true) {
                // read the proto object and put into new object
                Request op = Request.parseDelimitedFrom(in);
                System.out.println("Got request: " + op.toString());
                Response response;

                boolean quit = false;

                // Handling different types of requests
                switch (op.getOperationType()) {
                    case NAME:
                        response = op.getName().isBlank() ? error(1, "name") : nameRequest(op);
                        break;
                    case LEADERBOARD:
                        response = leaderboardRequest(op);
                        break;
                    case START:
                        response = startGame(op);
                        break;
                    case UPDATE:
                        if (!op.hasRow() || !op.hasColumn() || !op.hasValue()) {
                            response = error(2, "Required Fields Missing: row, column, value for UPDATE request");
                        } else {
                            response = updateRequest(op);
                        }
                        break;
                    case CLEAR:
                        if (!op.hasRow() || !op.hasColumn() || !op.hasValue()) {
                            response = error(2, "Required Fields Missing: row, column, value for CLEAR request");
                        } else {
                            response = clearRequest(op);
                        }
                        break;
                    case QUIT:
                        response = quit();
                        quit = true;
                        break;
                    default:
                        response = error(2, op.getOperationType().name());
                        break;
                }
                response.writeDelimitedTo(out);

                if (quit) {
                    return;
                }
            }
        } catch (SocketException se) {
            System.out.println("Client disconnected");
        } catch (Exception ex) {
            Response error = error(0, "Unexpected server error: " + ex.getMessage());
            error.writeDelimitedTo(out);
        }
        finally {
            System.out.println("Client ID " + id + " disconnected");
            this.inGame = false;
            exitAndClose(in, out, clientSocket);
        }
    }

    // Close input, output, and socket connections
    void exitAndClose(InputStream in, OutputStream out, Socket serverSock) throws IOException {
        if (in != null)   in.close();
        if (out != null)  out.close();
        if (serverSock != null) serverSock.close();
    }

    /**
     * Handles the name request and returns the appropriate response
     * @param op The request object containing client data
     * @return Response containing a greeting and next steps for the client
     */
    private Response nameRequest(Request op) throws IOException {
        name = op.getName();
        writeToLog(name, Message.CONNECT);
        currentState = 2;
        System.out.println("Got a connection and a name: " + name);
        return Response.newBuilder()
                .setResponseType(Response.ResponseType.GREETING)
                .setMessage("Hello " + name + " and welcome to a simple game of Sudoku.")
                .setMenuoptions(menuOptions)
                .setNext(currentState)
                .build();
    }

    /**
     * Starts to handle start of a game after START request
     * @param op The request object from client
     * @return Response with the initial game board and menu options
     */
    private Response startGame(Request op) throws IOException {
        System.out.println("Received START request.");

        // Set difficulty (handles defaulting and validation)
        Response difficultyResponse = setDifficulty(op);
        if (difficultyResponse != null) {
            System.out.println("\nError: difficulty is out of range");
            return difficultyResponse; // Return error response if any
        }

        System.out.println("Starting game with difficulty: " + game.getDifficulty());
        game.newGame(grading, game.getDifficulty());
        System.out.println("\nSolved Board:");
        System.out.println(game.getSolvedBoard());

        return Response.newBuilder()
                .setResponseType(Response.ResponseType.START)
                .setBoard(game.getDisplayBoard())
                .setMessage("\nStarting new game.")
                .setMenuoptions(gameOptions)
                .setPoints(game.setPoints(20))
                .setNext(3)
                .build();
    }

    /**
     * Sets the difficulty for the game based on client request
     * @param op The request object from client
     * @return Response if there's an error, otherwise null
     */
    private Response setDifficulty(Request op) {
        if (!op.hasDifficulty()) {
            System.out.println("Difficulty not provided in the request. Defaulting to 4.");
            game.setDifficulty(4);
            return null;
        }

        int requestedDifficulty = op.getDifficulty();
        if (requestedDifficulty < 1 || requestedDifficulty > 20) {
            System.out.println("Invalid difficulty provided: " + requestedDifficulty);
            return Response.newBuilder()
                    .setResponseType(Response.ResponseType.ERROR)
                    .setErrorType(5)
                    .setMessage("\nError: difficulty is out of range")
                    .setMenuoptions(menuOptions)
                    .setNext(2) // Return to main menu
                    .build();
        }

        game.setDifficulty(requestedDifficulty);
        System.out.println("Difficulty set to: " + requestedDifficulty);
        return null; // No error
    }

    /**
     * Updates the leaderboard with the player's current score
     */
    private void updateLeaderboard() {
        leaderboard.putIfAbsent(name, 0);
        leaderboard.put(name, game.getPoints());
        saveLeaderboard();
    }

    /**
     * Saves the leaderboard to a file
     */
    private synchronized void saveLeaderboard() {
        System.out.println("Saving leaderboard to file..");
        Map<String, Integer> loginCounts = countPlayerLogins(); // Get current login counts

        try (FileWriter writer = new FileWriter(LEADERBOARD_FILE)) {
            for (Map.Entry<String, Integer> entry : leaderboard.entrySet()) {
                String playerName = entry.getKey();
                int score = entry.getValue();
                int logins = loginCounts.getOrDefault(playerName, 0);

                // Write player name, score, and login count
                writer.write(playerName + "," + score + "," + logins + "\n");
                System.out.println("Writing entry - Name: " + playerName + ", Points: " + score + ", Logins: " + logins);
            }
        } catch (IOException e) {
            System.out.println("Error saving leaderboard: " + e.getMessage());
        }
        System.out.println("Leaderboard saved.");
    }

    /**
     * Handles the leaderboard request and returns the leaderboard data to the client
     * @param op The request object from client
     * @return Response containing the leaderboard data
     */
    private Response leaderboardRequest(Request op) {
        StringBuilder leaderboardDisplay = new StringBuilder("=== Current Leaderboard ===\n");
        Map<String, Integer> loginCounts = countPlayerLogins(); // Get the login counts

        // Include all players with either points or login counts
        loginCounts.forEach((name, logins) -> {
            int score = leaderboard.getOrDefault(name, 0); // Default to 0 points if not in leaderboard
            leaderboardDisplay.append(name)
                    .append(": ")
                    .append(score)
                    .append(" points, ")
                    .append(logins)
                    .append(" logins\n");
            System.out.println("Leaderboard entry - Name: " + name + ", Points: " + score + ", Logins: " + logins);
        });

        return Response.newBuilder()
                .setResponseType(Response.ResponseType.LEADERBOARD)
                .setMessage(leaderboardDisplay.toString())
                .setMenuoptions(menuOptions)
                .setNext(2)
                .build();
    }

    /**
     * Loads the leaderboard from a file
     */
    private static void loadLeaderboard() {
        System.out.println("Loading leaderboard from file..");
        File file = new File(LEADERBOARD_FILE);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length == 3) {
                        try {
                            String name = parts[0];
                            int score = Integer.parseInt(parts[1].trim());
                            int logins = Integer.parseInt(parts[2].trim());

                            leaderboard.put(name, score);
                            loginCounts.put(name, logins);
                            System.out.println("Loaded entry - Name: " + name + ", Points: " + score + ", Logins: " + logins);
                        } catch (NumberFormatException e) {
                            System.out.println("Skipping malformed line: " + line);
                        }
                    } else {
                        System.out.println("Skipping malformed line: " + line);
                    }
                }
            } catch (IOException e) {
                System.out.println("Error loading leaderboard: " + e.getMessage());
            }
        } else {
            System.out.println("Leaderboard file not found. Starting fresh.");
        }
    }


    /**
     * Updates the game board with the provided values from the client
     * @param op The request object containing row, column, and value
     * @return Response indicating success or error of the update operation
     */
    private Response updateRequest(Request op) throws IOException {
        int row = op.hasRow() ? op.getRow() : -1;
        int column = op.hasColumn() ? op.getColumn() : -1;
        int value = op.hasValue() ? op.getValue() : -1;

        // Request missing row input
        if (row < 1 || row > 9) {
            return Response.newBuilder()
                    .setResponseType(Response.ResponseType.ERROR)
                    .setErrorType(3) // Row out of bounds
                    .setMessage("Please provide a valid row number (1-9).")
                    .setMenuoptions(gameOptions)
                    .setNext(3)
                    .build();
        }

        // Request missing column input
        if (column < 1 || column > 9) {
            return Response.newBuilder()
                    .setResponseType(Response.ResponseType.ERROR)
                    .setErrorType(3) // Column out of bounds
                    .setMessage("Please provide a valid column number (1-9).")
                    .setMenuoptions(gameOptions)
                    .setNext(3)
                    .build();
        }

        // Request missing value input
        if (value < 1 || value > 9) {
            return Response.newBuilder()
                    .setResponseType(Response.ResponseType.ERROR)
                    .setErrorType(3) // Value out of bounds
                    .setMessage("Please provide a valid value (1-9).")
                    .setMenuoptions(gameOptions)
                    .setNext(3)
                    .build();
        }

        // Attempt to update the board
        int result = game.updateBoard(row - 1, column - 1, value, 0);

        if (result == 0) { // Valid move
            if (game.getWon()) {
                updateLeaderboard();
                return Response.newBuilder()
                        .setResponseType(Response.ResponseType.WON)
                        .setBoard(game.getDisplayBoard())
                        .setMessage("You solved the current puzzle, good job! " + name + "! \nYou earned " + game.getPoints() + " points.")
                        .setMenuoptions(menuOptions) // Return to main menu options after winning
                        .setNext(2) // Go to main menu
                        .build();
            } else {
                return Response.newBuilder()
                        .setResponseType(Response.ResponseType.PLAY)
                        .setBoard(game.getDisplayBoard())
                        .setMessage(name + ", good move! Your current score: " + game.getPoints() + " points.")
                        .setMenuoptions(gameOptions)
                        .setNext(3) // Stay in game
                        .build();
            }
        } else if (result == 1) { // Attempted to change a preset cell
            return Response.newBuilder()
                    .setResponseType(Response.ResponseType.ERROR)
                    .setErrorType(1) // Invalid move error
                    .setMessage("Cannot change preset value. (-2 points)")
                    .setBoard(game.getDisplayBoard())
                    .setMenuoptions(gameOptions)
                    .setPoints(game.setPoints(-2))
                    .setNext(3)
                    .build();
        } else { // Invalid value or position
            return Response.newBuilder()
                    .setResponseType(Response.ResponseType.ERROR)
                    .setErrorType(2) // Invalid move error
                    .setMessage("Invalid move. Try again. (-2 points)")
                    .setBoard(game.getDisplayBoard())
                    .setMenuoptions(gameOptions)
                    .setPoints(game.setPoints(-2))
                    .setNext(3)
                    .build();
        }
    }

    /**
     * Handles requests to clear cells or sections of the game board
     * @param op The request object containing details of the clear operation
     * @return Response indicating success or error of the clear operation
     */
    private Response clearRequest(Request op) throws IOException {
        System.out.println("Clear request received from client.");
        int row = op.hasRow() ? op.getRow() : -1;
        int column = op.hasColumn() ? op.getColumn() : -1;
        int value = op.hasValue() ? op.getValue() : -1;

        // Validate the `value` field to determine the type of clear request
        if (value < 1 || value > 6) {
            return Response.newBuilder()
                    .setResponseType(Response.ResponseType.ERROR)
                    .setErrorType(3) // Invalid `value`
                    .setMessage("Invalid clear operation. Please provide a value between 1 and 6.")
                    .setBoard(game.getDisplayBoard())
                    .setMenuoptions(SockBaseServer.gameOptions)
                    .setNext(3)
                    .build();
        }

        int result = -1; // Result of the operation
        String message;
        Response.EvalType evalType;

        // Handle different clear options based on the `value` field
        switch (value) {
            case 1: // Clear single cell
                if (row < 1 || row > 9 || column < 1 || column > 9) {
                    return Response.newBuilder()
                            .setResponseType(Response.ResponseType.ERROR)
                            .setErrorType(3) // Row/Column out of bounds
                            .setMessage("Invalid row/column for clearing a single cell. Please enter values between 1 and 9.")
                            .setBoard(game.getDisplayBoard())
                            .setMenuoptions(SockBaseServer.gameOptions)
                            .setPoints(game.setPoints(-5))
                            .setNext(3)
                            .build();
                }
                result = game.updateBoard(row - 1, column - 1, 0, 1); // Adjust indices for 0-based grid
                message = " single cell cleared. (-5 points)";
                evalType = Response.EvalType.CLEAR_VALUE;
                break;

            case 2: // Clear row
                if (row < 1 || row > 9) {
                    return Response.newBuilder()
                            .setResponseType(Response.ResponseType.ERROR)
                            .setErrorType(3) // Row out of bounds
                            .setMessage("Invalid row for clearing. Please enter a value between 1 and 9.")
                            .setBoard(game.getDisplayBoard())
                            .setMenuoptions(SockBaseServer.gameOptions)
                            .setNext(3)
                            .build();
                }
                result = game.updateBoard(row - 1, 0, 0, 2);
                message = " row cleared. (-5 points)";
                evalType = Response.EvalType.CLEAR_ROW;
                break;

            case 3: // Clear column
                if (column < 1 || column > 9) {
                    return Response.newBuilder()
                            .setResponseType(Response.ResponseType.ERROR)
                            .setErrorType(3) // Column out of bounds
                            .setMessage("Invalid column for clearing. Please enter a value between 1 and 9.")
                            .setBoard(game.getDisplayBoard())
                            .setMenuoptions(SockBaseServer.gameOptions)
                            .setNext(3)
                            .build();
                }
                result = game.updateBoard(0, column - 1, 0, 3);
                message = " column cleared. (-5 points)";
                evalType = Response.EvalType.CLEAR_COL;
                break;

            case 4: // Clear grid
                if (row < 1 || row > 3 || column < 1 || column > 3) {
                    return Response.newBuilder()
                            .setResponseType(Response.ResponseType.ERROR)
                            .setErrorType(3) // Grid coordinates out of bounds
                            .setMessage("Invalid grid coordinates for clearing. Please provide grid row/column between 1 and 3.")
                            .setBoard(game.getDisplayBoard())
                            .setMenuoptions(SockBaseServer.gameOptions)
                            .setNext(3)
                            .build();
                }
                result = game.updateBoard(row - 1, column - 1, 0, 4);
                message = " 3x3 grid cleared. (-5 points)";
                evalType = Response.EvalType.CLEAR_GRID;
                break;

            case 5: // Clear entire board
                result = game.updateBoard(0, 0, 0, 5);
                message = " entire board cleared. (-5 points)";
                evalType = Response.EvalType.CLEAR_BOARD;
                break;

            case 6: // Generate a new board
                game = new Game();
                game.newGame(grading, game.getDifficulty()); // Reinitialize with current difficulty
                message = name + ", a new board has been generated. Good luck! " +
                        "\nNote: generating a new board also deducts 5 points.";
                evalType = Response.EvalType.RESET_BOARD;
                return Response.newBuilder()
                        .setResponseType(Response.ResponseType.START)
                        .setBoard(game.getDisplayBoard())
                        .setPoints(game.getPoints())
                        .setMenuoptions(SockBaseServer.gameOptions)
                        .setMessage(message)
                        .setType(evalType)
                        .setNext(3)
                        .build();

            default:
                result = -1; // Unrecognized operation
                message = "Invalid clear operation.";
                evalType = Response.EvalType.RESET_BOARD; // Default type...
                break;
        }

        // Return response after successful operation
        if (result == 0) {
            return Response.newBuilder()
                    .setResponseType(Response.ResponseType.PLAY)
                    .setBoard(game.getDisplayBoard())
                    .setPoints(game.getPoints())
                    .setMenuoptions(SockBaseServer.gameOptions)
                    .setMessage(message)
                    .setPoints(game.setPoints(-5))
                    .setType(evalType)
                    .setNext(3)
                    .build();
        } else {
            return Response.newBuilder()
                    .setResponseType(Response.ResponseType.ERROR)
                    .setErrorType(2) // Invalid operation
                    .setMessage("Failed to execute the clear operation.")
                    .setBoard(game.getDisplayBoard())
                    .setMenuoptions(SockBaseServer.gameOptions)
                    .setNext(3)
                    .build();
        }
    }

    /**
     * Handles the quit request, might need adaptation
     * @return Request.Builder holding the reponse back to Client as specified in Protocol
     */
    private Response quit() throws IOException {
        this.inGame = false;
        return Response.newBuilder()
                .setResponseType(Response.ResponseType.BYE)
                .setMessage("Thank you for playing! goodbye.")
                .build();
    }

    /**
     * Start of handling errors, not fully done
     * @return Request.Builder holding the reponse back to Client as specified in Protocol
     */
    private Response error(int err, String field) throws IOException {
        String message;
        int type = err;
        Response.Builder response = Response.newBuilder();

        switch (err) {
            case 1:
                message = "\nError: required field missing or empty";
                break;
            case 2:
                message = "\nError: request not supported";
                break;
            case 3:
                message = "\nError: row or col out of bounds";
                break;
            case 4:
                message = "\nError: request is not expected at this point";
                break;
            case 5:
                message = "\nError: difficulty is out of range";
                break;
            default:
                message = "\nError: cannot process your request";
                type = 0;
                break;
        }

        response
                .setResponseType(Response.ResponseType.ERROR)
                .setErrorType(type)
                .setMessage(message)
                .setMenuoptions(currentState == 1 ? "" : menuOptions)
                .setNext(currentState)
                .build();

        return response.build();
    }

    /**
     * Writing a new entry to our log
     * @param name - Name of the person logging in
     * @param message - type Message from Protobuf which is the message to be written in the log (e.g. Connect) 
     * @return String of the new hidden image
     */
    public synchronized void writeToLog(String name, Message message) {
        try {
            // read old log file
            Logs.Builder logs = readLogFile();

            Date date = java.util.Calendar.getInstance().getTime();

            // we are writing a new log entry to our log
            // add a new log entry to the log list of the Protobuf object
            logs.addLog(date + ": " +  name + " - " + message);

            // open log file
            FileOutputStream output = new FileOutputStream(logFilename);
            Logs logsObj = logs.build();

            // write to log file
            logsObj.writeTo(output);
        } catch(Exception e) {
            System.out.println("Issue while trying to save");
        }
    }

    /**
     * Counts the number of login events for each player based on the log file.
     * @return A map where the key is the player's name and the value is the count of their login events.
     */
    private Map<String, Integer> countPlayerLogins() {
        Map<String, Integer> loginCounts = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(logFilename))) {
            String line;
            // Read each line from the log file
            while ((line = reader.readLine()) != null) {
                line = line.trim(); // Trim any leading or trailing whitespace

                // Check if the line represents a login event (contains " - CONNECT")
                if (line.contains(" - CONNECT")) {
                    // Extract the player name by splitting at ": " and " -"
                    String[] parts = line.split(": ");
                    if (parts.length > 1) {
                        String playerName = parts[1].split(" -")[0].trim();
                        // Increment the player's login count
                        loginCounts.put(playerName, loginCounts.getOrDefault(playerName, 0) + 1);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading login counts: " + e.getMessage());
        }

        return loginCounts;
    }

    /**
     * Reading the current log file
     * @return Logs.Builder a builder of a logs entry from protobuf
     */
    public Logs.Builder readLogFile() throws Exception {
        Logs.Builder logs = Logs.newBuilder();

        try {
            return logs.mergeFrom(new FileInputStream(logFilename));
        } catch (FileNotFoundException e) {
            System.out.println(logFilename + ": File not found.  Creating a new file.");
            return logs;
        }
    }

    /**
     * Main method to initiate the server and handle incoming client connections.
     * This server accepts multiple client connections and handles them concurrently.
     * Each connected client is assigned a unique game instance.
     *
     * @param args Command line arguments: <port(int)> <delay(int)>
     * @throws Exception If there are any connection or socket issues.
     */
    public static void main (String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Expected arguments: <port(int)> <delay(int)>");
            System.exit(1);
        }

        int port = 8000;
        grading = Boolean.parseBoolean(args[1]);
        Socket clientSocket = null;
        ServerSocket socket = null;

        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port|sleepDelay] must be an integer");
            System.exit(2);
        }

        SockBaseServer.loadLeaderboard();

        try {
            socket = new ServerSocket(port);
            System.out.println("Server started on port " + port);
        } catch(Exception e) {
            e.printStackTrace();
            System.exit(2);
        }

        int id = 1;
        while (true) {
            try {
                clientSocket = socket.accept();
                System.out.println("Attempting to connect to client-" + id);
                Game game = new Game();
                SockBaseServer server = new SockBaseServer(clientSocket, game, id++);
                new Thread(server).start();
            } catch (Exception e) {
                System.out.println("Error in accepting client connection.");
            }
        }
    }
}
