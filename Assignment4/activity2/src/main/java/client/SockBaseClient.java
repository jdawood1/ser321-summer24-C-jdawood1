package client;

import buffers.RequestProtos.*;
import buffers.ResponseProtos.*;

import java.io.*;
import java.net.Socket;

/**
 * SockBaseClient is a client-side class that connects to the SockBaseServer and interacts with it using protocol buffers.
 * The client sends various requests such as name registration, starting a game, updating game moves, or viewing the leaderboard.
 * It handles server responses to maintain an interactive user experience, allowing the user to make decisions like choosing actions
 * during the game or requesting the leaderboard. The client facilitates the communication and ensures proper flow of actions in the game.
 */
class SockBaseClient {

    private static final BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

    /**
     * Main method to initiate the client and handle communication with the server.
     * @param args Command line arguments: <host(String)> <port(int)>
     * @throws Exception If there are connection issues or input/output exceptions
     */
    public static void main (String[] args) throws Exception {
        Socket serverSock = null;
        OutputStream out = null;
        InputStream in = null;
        int i1=0, i2=0;
        int port = 8000; // default port

        // Make sure two arguments are given
        if (args.length != 2) {
            System.out.println("Expected arguments: <host(String)> <port(int)>");
            System.exit(1);
        }
        String host = args[0];
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port] must be integer");
            System.exit(2);
        }

        // Build the first request object just including the name
        Request op = nameRequest().build();
        Response response;
        try {
            // connect to the server
            serverSock = new Socket(host, port);

            // write to the server
            out = serverSock.getOutputStream();
            in = serverSock.getInputStream();

            op.writeDelimitedTo(out);

            while (true) {
                // Receive response from the server
                response = Response.parseDelimitedFrom(in);
                System.out.println("Got a response: " + response.toString());
                Request.Builder req = Request.newBuilder();
                System.out.println(response.getMessage());

                switch (response.getResponseType()) {
                    case GREETING:
                        req = chooseMenu(req, response);
                        break;

                    case LEADERBOARD:
                        req = chooseMenu(req, response);
                        break;

                    case START:
                        System.out.println("\nCurrent Board:\n" + response.getBoard());
                        System.out.println(response.getMenuoptions());
                        req = chooseGameAction(req);
                        break;

                    case PLAY:
                        System.out.println("\nCurrent Board:\n" + response.getBoard());
                        System.out.println(response.getMenuoptions());
                        req = chooseGameAction(req);
                        break;

                    case WON:
                        System.out.println("\nCurrent Board:\n" + response.getBoard());
                        req = chooseMenu(req, response);
                        break;

                    case ERROR:
                        System.out.println(response.getMenuoptions());
                        req = errorResponse(req, response);
                        break;

                    case BYE:
                        return;

                }
                // Send the next request to the server
                req.build().writeDelimitedTo(out);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            exitAndClose(in, out, serverSock);
        }
    }

    /**
     * Handles building a simple name request, asks the user for their name and builds the request.
     * @return Request.Builder which holds all the information for the NAME request.
     * @throws IOException If input reading fails.
     */
    static Request.Builder nameRequest() throws IOException {
        System.out.println("Please provide your name for the server.");
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        String strToSend = stdin.readLine();

        return Request.newBuilder()
                .setOperationType(Request.OperationType.NAME)
                .setName(strToSend);
    }

    /**
     * Shows the main menu and lets the user choose a number. It builds the request for the next server call.
     * @param req The current request builder to be updated.
     * @param response The response object from the server.
     * @return Request.Builder which holds the information the server needs for a specific request.
     * @throws IOException If input reading fails.
     */
    static Request.Builder chooseMenu(Request.Builder req, Response response) throws IOException {
        while (true) {
            System.out.println(response.getMenuoptions());
            System.out.print("Enter a number 1-3: ");
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            String menu_select = stdin.readLine();
            System.out.println(menu_select);
            switch (menu_select) {
                case "1":
                    req.setOperationType(Request.OperationType.LEADERBOARD);
                    break;
                case "2":
                    System.out.print("Please enter a difficulty level (1-20): ");
                    int difficulty = Integer.parseInt(stdin.readLine());
                    req.setOperationType(Request.OperationType.START)
                            .setDifficulty(difficulty);
                    break;
                case "3":
                    req.setOperationType(Request.OperationType.QUIT);
                    break;
                default:
                    System.out.println("\nNot a valid choice, please choose again");
                    break;
            }
            return req;
        }
    }

    /**
     * Handles error responses from the server and prompts the user for the next action.
     * @param req The current request builder to be updated.
     * @param response The response object from the server containing the error.
     * @return Request.Builder which holds the information for the next server call.
     * @throws IOException If input reading fails.
     */
    static Request.Builder errorResponse(Request.Builder req, Response response) throws IOException {
        try {
            switch (response.getErrorType()) {
                case 1: case 2: case 3: case 4: case 5:
                    if (response.getNext() == 1) {
                        req = nameRequest();
                    } else if (response.getNext() == 2) {
                        req = chooseMenu(req, response);
                    } else {
                        System.out.println("\nCurrent Board:\n" + response.getBoard());
                        req = chooseGameAction(req);
                    }
                    break;
                default:
                    if (response.getNext() == 1) {
                        req = nameRequest();
                    } else if (response.getNext() == 2) {
                        req = chooseMenu(req, response);
                    } else {
                        System.out.println("\nCurrent Board:\n" + response.getBoard());
                        req = chooseGameAction(req);
                    }
                    break;
            }
        } catch (Exception e) {
            System.out.println("An error occurred during error handling: " + e.getMessage());
            req = nameRequest();
        }

        return req;
    }

    /**
     * Shows the game action menu and lets the user choose a game action.
     * @param req The current request builder to be updated.
     * @return Request.Builder which holds the information for the next server call.
     * @throws Exception If there is an input error.
     */
    static Request.Builder chooseGameAction(Request.Builder req) throws Exception {

        System.out.print("Please enter value or type `exit`: ");
        String action = stdin.readLine().trim().toLowerCase();

        switch (action) {
            case "1": case "2": case "3": case "4": case "5": case "6": case "7": case "8": case "9":
                int row = Integer.parseInt(action);
                int column = getInput("Enter column: ", 1, 9);
                int value = getInput("Enter value: ", 1, 9);

                req.setOperationType(Request.OperationType.UPDATE)
                        .setRow(row)
                        .setColumn(column)
                        .setValue(value);
                break;

            case "c":
                int[] clearCoordinates = boardSelectionClear();
                req.setOperationType(Request.OperationType.CLEAR)
                        .setRow(clearCoordinates[0])
                        .setColumn(clearCoordinates[1])
                        .setValue(clearCoordinates[2]);
                break;

            case "r": // new board request
                req.setOperationType(Request.OperationType.CLEAR)
                        .setRow(-1)
                        .setColumn(-1)
                        .setValue(6);
                break;

            case "exit":
                req.setOperationType(Request.OperationType.QUIT);
                break;

            default:
                System.out.println("Invalid choice. Please try again.");
                return chooseGameAction(req);
        }
        return req;
    }

    /**
     * Helper method to get a valid integer input from the user.
     * @return The integer value entered by the user.
     */
    private static int getInput(String prompt, int min, int max) throws IOException {
        while (true) {
            System.out.print(prompt);
            try {
                int input = Integer.parseInt(stdin.readLine());
                if (input >= min && input <= max) return input;
            } catch (NumberFormatException ignored) {
            }
            System.out.println("Invalid choice. Please enter a number between " + min + " and " + max + ".");
        }
    }

    /**
     * Exits the connection
     */
    static void exitAndClose(InputStream in, OutputStream out, Socket serverSock) throws IOException {
        if (in != null)   in.close();
        if (out != null)  out.close();
        if (serverSock != null) serverSock.close();
        System.exit(0);
    }

    /**
     * Handles the clear menu logic when the user chooses that in the Game menu. It returns the values exactly
     * as needed in the CLEAR request: row (int[0]), column (int[1]), value (int[2]).
     * @return An array of integers representing the clear operation parameters.
     * @throws Exception If there is an input error.
     */
    static int[] boardSelectionClear() throws Exception {
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Choose what kind of clear by entering an integer (1 - 5)");
        System.out.print(" 1 - Clear value \n 2 - Clear row \n 3 - Clear column \n 4 - Clear Grid \n 5 - Clear Board \n");

        String selection = stdin.readLine();

        while (true) {
            if (selection.equalsIgnoreCase("exit")) {
                return new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
            }
            try {
                int temp = Integer.parseInt(selection);

                if (temp < 1 || temp > 5) {
                    System.out.println("Please choose and integer 1-5");
                    throw new NumberFormatException();
                }

                break;
            } catch (NumberFormatException nfe) {
                System.out.println("That's not an integer!");
                System.out.println("Choose what kind of clear by entering an integer (1 - 5)");
                System.out.print("1 - Clear value \n 2 - Clear row \n 3 - Clear column \n 4 - Clear Grid \n 5 - Clear Board \n");
            }
            selection = stdin.readLine();
        }

        int[] coordinates = new int[3];

        switch (selection) {
            case "1":
                // clear value, so array will have {row, col, 1}
                coordinates = boardSelectionClearValue();
                break;
            case "2":
                // clear row, so array will have {row, -1, 2}
                coordinates = boardSelectionClearRow();
                break;
            case "3":
                // clear col, so array will have {-1, col, 3}
                coordinates = boardSelectionClearCol();
                break;
            case "4":
                // clear grid, so array will have {gridNum, -1, 4}
                coordinates = boardSelectionClearGrid();
                break;
            case "5":
                // clear entire board, so array will have {-1, -1, 5}
                coordinates[0] = -1;
                coordinates[1] = -1;
                coordinates[2] = 5;
                break;
            default:
                break;
        }

        return coordinates;
    }

    /**
     * Handles the clear value logic, allowing the user to specify a cell to clear.
     * @return An array of integers representing the clear operation parameters.
     * @throws Exception If there is an input error.
     */
    static int[] boardSelectionClearValue() throws Exception {
        int[] coordinates = new int[3];

        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Choose coordinates of the value you want to clear");
        System.out.print("Enter the row as an integer (1 - 9): ");
        String row = stdin.readLine();

        while (true) {
            if (row.equalsIgnoreCase("exit")) {
                return new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
            }
            try {
                int temp = Integer.parseInt(row);
                if (temp < 1 || temp > 9) {
                    System.out.println("Enter the row as an integer (1 - 9): ");
                    throw new NumberFormatException();
                }
                break;
            } catch (NumberFormatException nfe) {
                System.out.println("That's not an integer!");
                System.out.print("Enter the row as an integer (1 - 9): ");
            }
            row = stdin.readLine();
        }

        coordinates[0] = Integer.parseInt(row);

        System.out.print("Enter the column as an integer (1 - 9): ");
        String col = stdin.readLine();

        while (true) {
            if (col.equalsIgnoreCase("exit")) {
                return new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
            }
            try {
                int temp = Integer.parseInt(col);
                if (temp < 1 || temp > 9) {
                    System.out.println("Enter the column as an integer (1 - 9): ");
                    throw new NumberFormatException();
                }
                break;
            } catch (NumberFormatException nfe) {
                System.out.println("That's not an integer!");
                System.out.print("Enter the column as an integer (1 - 9): ");
            }
            col = stdin.readLine();
        }

        coordinates[1] = Integer.parseInt(col);
        coordinates[2] = 1;

        return coordinates;
    }

    /**
     * Handles the clear row logic, allowing the user to specify a row to clear.
     * @return An array of integers representing the clear operation parameters.
     * @throws Exception If there is an input error.
     */
    static int[] boardSelectionClearRow() throws Exception {
        int[] coordinates = new int[3];

        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Choose the row you want to clear");
        System.out.print("Enter the row as an integer (1 - 9): ");
        String row = stdin.readLine();

        while (true) {
            if (row.equalsIgnoreCase("exit")) {
                return new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
            }
            try {
                int temp = Integer.parseInt(row);
                if (temp < 1 || temp > 9) {
                    System.out.println("Enter the row as an integer (1 - 9): ");
                    throw new NumberFormatException();
                }
                break;
            } catch (NumberFormatException nfe) {
                System.out.println("That's not an integer!");
                System.out.print("Enter the row as an integer (1 - 9): ");
            }
            row = stdin.readLine();
        }

        coordinates[0] = Integer.parseInt(row);
        coordinates[1] = -1;
        coordinates[2] = 2;

        return coordinates;
    }

    /**
     * Handles the clear column logic, allowing the user to specify a column to clear.
     * @return An array of integers representing the clear operation parameters.
     * @throws Exception If there is an input error.
     */
    static int[] boardSelectionClearCol() throws Exception {
        int[] coordinates = new int[3];

        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Choose the column you want to clear");
        System.out.print("Enter the column as an integer (1 - 9): ");
        String col = stdin.readLine();

        while (true) {
            if (col.equalsIgnoreCase("exit")) {
                return new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
            }
            try {
                int temp = Integer.parseInt(col);
                if (temp < 1 || temp > 9) {
                    System.out.println("Enter the column as an integer (1 - 9): ");
                    throw new NumberFormatException();
                }
                break;
            } catch (NumberFormatException nfe) {
                System.out.println("That's not an integer!");
                System.out.print("Enter the column as an integer (1 - 9): ");
            }
            col = stdin.readLine();
        }

        coordinates[0] = -1;
        coordinates[1] = Integer.parseInt(col);
        coordinates[2] = 3;
        return coordinates;
    }

    /**
     * Handles the clear grid logic, allowing the user to specify a 3x3 grid to clear.
     * @return An array of integers representing the clear operation parameters.
     * @throws Exception If there is an input error.
     */
    static int[] boardSelectionClearGrid() throws Exception {
        int[] coordinates = new int[3];

        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Choose area of the grid you want to clear");
        System.out.println(" 1 2 3 \n 4 5 6 \n 7 8 9 \n");
        System.out.print("Enter the grid as an integer (1 - 9): ");
        String grid = stdin.readLine();

        while (true) {
            if (grid.equalsIgnoreCase("exit")) {
                return new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
            }
            try {
                int temp = Integer.parseInt(grid);
                if (temp < 1 || temp > 9) {
                    System.out.println("Enter the grid as an integer (1 - 9): ");
                    throw new NumberFormatException();
                }
                break;
            } catch (NumberFormatException nfe) {
                System.out.println("That's not an integer!");
                System.out.print("Enter the grid as an integer (1 - 9): ");
            }
            grid = stdin.readLine();
        }

        coordinates[0] = Integer.parseInt(grid);
        coordinates[1] = -1;
        coordinates[2] = 4;

        return coordinates;
    }
}
