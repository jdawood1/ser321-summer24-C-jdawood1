package Assign32starter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import java.net.*;
import java.io.*;
import org.json.*;
import java.util.*;

public class SockServer {
	static Map<Socket, Integer> totalRounds = new HashMap<>();
	static Map<Socket, Integer> currentRound = new HashMap<>();
	static Map<Socket, Integer> currentHint = new HashMap<>();
	static Map<Socket, String> clientNames = new HashMap<>();
	static Map<Socket, Integer> clientStates = new HashMap<>();
	static Map<Socket, Integer> roundPoints = new HashMap<>();
	static Map<Socket, Integer> clientScores = new HashMap<>();
	static Map<String, Integer> leaderboard = new HashMap<>();
	static Map<Socket, String> lastInput = new HashMap<>();
	static final Map<String, List<String>> wondersHints = new HashMap<>();
	static final Map<String, List<String>> wondersTextHints = new HashMap<>();
	private static final String LEADERBOARD_FILE = "leaderboard.json";
	static final List<String> wonders = new ArrayList<>(wondersHints.keySet());
	static final int TOTAL_HINTS = 4;

	public static void main(String[] args) {
		try {
			loadLeaderboard();
			loadWondersHints();

			// Populate wonders list dynamically
			wonders.addAll(wondersHints.keySet());

			int port = Integer.parseInt(System.getProperty("port", "8888"));
			ServerSocket serv = new ServerSocket(port);
			System.out.println("Server ready for connection on port: " + port);

			while (true) {
				Socket sock = serv.accept();
				System.out.println("Client connected: " + sock.getInetAddress().getHostAddress());

				// Set initial state for each client
				setClientState(sock, 0);

				// Create a new thread for each client
				new Thread(new ClientHandler(sock)).start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void sendImg(String filename, JSONObject obj) {
		File file = new File("img/" + filename);
		System.out.println("Attempting to load image: " + filename);

		if (file.exists()) {
			try {
				BufferedImage image = ImageIO.read(file);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(image, "png", baos);
				String encodedImage = Base64.getEncoder().encodeToString(baos.toByteArray());

				obj.put("image", encodedImage);
				System.out.println("Encoded image data for file: " + filename);
			} catch (IOException e) {
				System.err.println("Error reading or encoding image: " + e.getMessage());
				obj.put("error", "Error loading image: " + e.getMessage());
			}
		} else {
			obj.put("image", "");
			obj.put("error", "Image not found: " + filename);
			System.err.println("Image not found: " + filename);
		}
	}

	public static void loadWondersHints() {
		File hintsFile = new File("wondersHints.json");
		if (!hintsFile.exists()) {
			System.err.println("Wonders hints file does not exist. Please ensure wondersHints.json is available.");
			return;
		}

		try (FileReader reader = new FileReader(hintsFile)) {
			JSONObject json = new JSONObject(new JSONTokener(reader));

			for (String wonder : json.keySet()) {
				JSONObject wonderData = json.getJSONObject(wonder);

				// Load images and hints for each wonder
				List<String> imageList = new ArrayList<>();
				for (Object img : wonderData.getJSONArray("images")) {
					imageList.add(img.toString());
				}
				wondersHints.put(wonder, imageList);

				List<String> hintList = new ArrayList<>();
				for (Object hint : wonderData.getJSONArray("hints")) {
					hintList.add(hint.toString());
				}
				wondersTextHints.put(wonder, hintList);
			}

			System.out.println("Loaded wonders hints from JSON file successfully.");
		} catch (FileNotFoundException e) {
			System.err.println("Hints JSON file not found: " + e.getMessage());
		} catch (IOException e) {
			System.err.println("Error reading hints JSON file: " + e.getMessage());
		} catch (JSONException e) {
			System.err.println("Error parsing hints JSON file: " + e.getMessage());
		}
	}

	public static void saveLeaderboard() {
		JSONObject json = new JSONObject(leaderboard);
		try (FileWriter file = new FileWriter(LEADERBOARD_FILE)) {
			file.write(json.toString(4)); // Pretty print with 4-space indentation
			System.out.println("Leaderboard saved to " + LEADERBOARD_FILE);
		} catch (IOException e) {
			System.out.println("Error saving leaderboard: " + e.getMessage());
		}
	}

	public static void loadLeaderboard() {
		File file = new File(LEADERBOARD_FILE);
		if (file.exists()) {
			try (FileReader reader = new FileReader(file)) {
				JSONObject json = new JSONObject(new JSONTokener(reader));
				leaderboard.clear();
				json.toMap().forEach((key, value) -> leaderboard.put(key, (Integer) value));
				System.out.println("Leaderboard loaded from " + LEADERBOARD_FILE);
			} catch (FileNotFoundException e) {
				System.err.println("Leaderboard file not found: " + e.getMessage());
			} catch (JSONException e) {
				System.err.println("Error parsing leaderboard JSON: " + e.getMessage());
			} catch (IOException e) {
				System.err.println("Error reading leaderboard file: " + e.getMessage());
			}
		} else {
			System.out.println("Leaderboard file does not exist, starting with an empty leaderboard.");
		}
	}

	public static void setClientState(Socket socket, int state) {
		clientStates.put(socket, state);
	}
	public static int getClientState(Socket socket) {
		return clientStates.getOrDefault(socket, 0);
	}
	public static void removeClientState(Socket socket) {
		clientStates.remove(socket);
	}
}

class ClientHandler implements Runnable {
	private final Socket socket;
	private PrintWriter outWrite;
	private BufferedReader in;

	public ClientHandler(Socket socket) {
		this.socket = socket;
		try {
			this.outWrite = new PrintWriter(socket.getOutputStream(), true);
			this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		try {
			System.out.println("Started handling client: " + socket.getInetAddress().getHostAddress());

			while (true) {
				String fromClient = in.readLine();
				if (fromClient == null) {
					System.out.println("Client disconnected");
					break;
				}

				System.out.println("Received from client: " + fromClient);
				JSONObject clientMessage = new JSONObject(fromClient);
				JSONObject serverResponse = handleClientMessage(clientMessage);

				outWrite.println(serverResponse.toString());
			}
		} catch (EOFException e) {
			System.out.println("Client disconnected due to EOFException.");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				socket.close();
				SockServer.removeClientState(socket); // Clean up client state
				System.out.println("Closing client connection: " + socket.getInetAddress().getHostAddress());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private JSONObject handleClientMessage(JSONObject message) {
		JSONObject response = new JSONObject();
		int state = SockServer.getClientState(socket);

		switch (state) {
			case 0: // Initial state
				if (!message.has("input")) {
					response.put("type", "instruction");
					response.put("message", "Welcome! Please enter your name and age in the format: name, age");
					try {
						SockServer.sendImg("hi.png", response);
					} catch (Exception e) {
						System.err.println("Error loading image for initial message: " + e.getMessage());
					}
				} else { // Get name and age
					String[] parts = message.optString("input", "").split(",");
					if (parts.length == 2) {
						String part1 = parts[0].trim();
						String part2 = parts[1].trim();
						String name = null;
						int age = -1;

						// Check if the first part is a valid name or age, and assign accordingly
						if (isValidName(part1) && isValidAge(part2)) {
							name = part1;
							age = Integer.parseInt(part2);
						} else if (isValidAge(part1) && isValidName(part2)) {
							age = Integer.parseInt(part1);
							name = part2;
						}

						// If a valid name and age were found, proceed
						if (name != null && age != -1) {
							SockServer.clientNames.put(socket, name);
							response.put("type", "menu");
							response.put("message", "Hello, " + name + "! Choose 'leaderboard', 'play', or 'quit'.");
							SockServer.setClientState(socket, 1);
							SockServer.sendImg("questions.jpg", response);
						} else {
							response.put("type", "error");
							response.put("message", "Invalid input. Please enter your name and age in the format: name, age.");
						}
					} else {
						response.put("type", "error");
						response.put("message", "Invalid input. Please enter your name and age in the format: name, age.");
					}
				}
				break;


			case 1: // Main menu options
				String choice = message.optString("input", "").toLowerCase();
				switch (choice) {
					case "leaderboard":
						response.put("type", "update");

						// Sort leaderboard entries by score in descending order and format them
						StringBuilder leaderboardMessage = new StringBuilder("Leaderboard:\n");
						SockServer.leaderboard.entrySet().stream()
								.sorted((a, b) -> b.getValue().compareTo(a.getValue()))
								.limit(10) // Show top 10 players
								.forEach(entry -> leaderboardMessage.append(entry.getKey()).append(" - ").append(entry.getValue()).append(" points\n"));

						leaderboardMessage.append("Please choose 'leaderboard', 'play', or 'quit'.");
						response.put("message", leaderboardMessage.toString());
						break;
					case "play":
						SockServer.clientScores.put(socket, 0); // Reset score for a new game session
						response.put("type", "instruction");
						response.put("message", "How many rounds would you like to play?");
						SockServer.setClientState(socket, 2); // Move to round selection
						SockServer.sendImg("questions.jpg", response);
						break;
					case "quit":
						response.put("type", "instruction");
						response.put("message", "Goodbye!");
						SockServer.setClientState(socket, 0); // Reset state for next connection
						break;
					default:
						response.put("type", "error");
						response.put("message", "Invalid option. Choose 'leaderboard', 'play', or 'quit'.");
						SockServer.sendImg("questions.jpg", response);
				}
				break;

			case 2: // Round selection
				String roundsInput = message.optString("input", "");
				try {
					int rounds = Integer.parseInt(roundsInput);
					SockServer.totalRounds.put(socket, rounds);
					SockServer.currentRound.put(socket, 1); // Start with round 1
					response.put("message", "Starting game with " + rounds + " rounds. Starting round 1!");
					sendNextWonderHint(response);
					SockServer.setClientState(socket, 3); // Move to the gameplay state
				} catch (NumberFormatException e) {
					response.put("message", "Invalid number. Please enter a valid number for rounds.");
				}
				break;


			case 3: // Gameplay state
				String input = message.optString("input", "").toLowerCase();
				SockServer.lastInput.put(socket, input); // Store the last input

				int currentHint = SockServer.currentHint.getOrDefault(socket, 1);
				System.out.println("Current Input: " + input);
				System.out.println("Current Hint: " + currentHint);
				System.out.println("Current Round: " + SockServer.currentRound.getOrDefault(socket, 1));
				System.out.println("Total Hints Allowed: 4");

				if (input.equals("next")) {
					int nextHint = currentHint + 1;
					if (nextHint <= 4) { // Allow exactly 4 hints/attempts
						SockServer.currentHint.put(socket, nextHint);
						sendHintImage(response, getCurrentWonder(), nextHint); // Send only when "next" is requested
					} else {
						response.put("message", "Out of hints: Please guess or type 'skip' to move to the next wonder.");
					}
				} else if (input.equals("skip")) {
					// Move to next wonder
					response.put("message", "Skipping to the next wonder...");
					advanceToNextRound(response);
				} else {
					// Process as a guess
					if (isCorrectGuess(input)) {
						int pointsEarned = calculatePoints();
						updateClientScore(pointsEarned);
						response.put("message", "Correct! You earned " + pointsEarned + " points.");
						advanceToNextRound(response);
					} else {
						// Incorrect guess handling with exactly 4 attempts
						if (currentHint < 4) {
							SockServer.currentHint.put(socket, currentHint + 1);
							int remainingHints = 4 - SockServer.currentHint.get(socket);

							// Warn the user on the third incorrect attempt
							if (remainingHints == 0) {
								response.put("message", "Incorrect guess. This is your last attempt. Please guess or type 'skip' to move to the next wonder.");
								displayCurrentScore(response);
							} else {
								response.put("message", "Incorrect guess. Try again. You have " + remainingHints + " hint(s) remaining.\n"
								+ "Please guess or type 'skip' to move to the next wonder.");
								displayCurrentScore(response);
							}
						} else {
							// After 4th attempt, move to next wonder
							response.put("message", "Incorrect guess. No hints remaining. Moving to the next wonder.");
							advanceToNextRound(response);
						}
					}
				}
				break;



			default:
				response.put("type", "error");
				response.put("message", "Unknown state. Restarting.");
				SockServer.setClientState(socket, 0); // Reset in case of unexpected state
		}
		return response;
	}

	private void sendNextWonderHint(JSONObject response) {
		String wonder = getCurrentWonder();

		// Reset hint to 1 for the new wonder
		SockServer.currentHint.put(socket, 1);
		response.put("type", "hint");

		// Get the first text hint for the wonder
		String firstHintText = SockServer.wondersTextHints.get(wonder).get(0);
		response.put("message", "Here's your first hint: " + firstHintText);

		// Send the first hint image for the new wonder
		sendHintImage(response, wonder, 1);
	}

	private void sendHintImage(JSONObject response, String wonder, int hintNumber) {
		try {
			// Fetch image filename and text hint based on hint number
			String filename = SockServer.wondersHints.get(wonder).get(hintNumber - 1);
			String textHint = SockServer.wondersTextHints.get(wonder).get(hintNumber - 1);

			SockServer.sendImg(filename, response); // Add image to the response
			response.put("message", "Hint " + hintNumber + ": " + textHint);

			System.out.println("Sending hint " + hintNumber + " for " + wonder + ": " + textHint);
		} catch (Exception e) {
			System.out.println("Error loading hint image or text: " + e.getMessage());
		}
	}

	private void advanceToNextRound(JSONObject response) {
		int current = SockServer.currentRound.getOrDefault(socket, 1);
		int total = SockServer.totalRounds.getOrDefault(socket, 1);

		// Calculate points only if the last input was correct
		int pointsEarned = 0;
		if (isCorrectGuess(SockServer.lastInput.get(socket))) {
			pointsEarned = calculatePoints();
		}

		// Update the last round points, so they can be displayed later
		SockServer.roundPoints.put(socket, pointsEarned);

		updateClientScore(pointsEarned);
		addScoreToResponse(response, pointsEarned);

		if (isGameOver(current, total)) {
			endGame(response, total);
		} else {
			moveToNextRound(response, current, total);
		}
	}

	private int calculatePoints() {
		int hintsUsed = SockServer.currentHint.getOrDefault(socket, 1);
		// Calculate current points based on remaining hints
		int currentPoints = Math.max(0, SockServer.TOTAL_HINTS - hintsUsed + 1);
		return (currentPoints * 5) + 5; // the formula...
	}

	private void updateClientScore(int pointsEarned) {
		int currentScore = SockServer.clientScores.getOrDefault(socket, 0);
		SockServer.clientScores.put(socket, currentScore + pointsEarned);
		System.out.println("Updated Total Score after adding points: " + SockServer.clientScores.get(socket));
	}

	private void addScoreToResponse(JSONObject response, int pointsEarned) {
		int currentScore = SockServer.clientScores.getOrDefault(socket, 0);
		response.put("pointsThisRound", pointsEarned);
		response.put("totalScore", currentScore);
		response.put("message", "You earned " + pointsEarned + " points! Total Score: " + currentScore);
	}

	private boolean isGameOver(int currentRound, int totalRounds) {
		return currentRound >= totalRounds;
	}

	private void endGame(JSONObject response, int totalRounds) {
		System.out.println("Game Over. All rounds completed.");
		response.put("message", response.getString("message") + "\nGame over! You've completed all "
				+ totalRounds + " rounds.\nPlease choose 'leaderboard', 'play', or 'quit'.");
		SockServer.setClientState(socket, 1); // Return to main menu

		updateLeaderboard();
		sendEndGameImage(response);
	}

	private void updateLeaderboard() {
		String playerName = SockServer.clientNames.get(socket);
		int finalScore = SockServer.clientScores.getOrDefault(socket, 0);
		SockServer.leaderboard.put(playerName, finalScore);
		System.out.println("Leaderboard updated: " + playerName + " - " + finalScore + " points");
		SockServer.saveLeaderboard();
	}

	private void sendEndGameImage(JSONObject response) {
		String endImage = "win.jpg";
		try {
			System.out.println("Sending end game image: " + endImage);
			SockServer.sendImg(endImage, response);
		} catch (Exception e) {
			System.err.println("Error loading end game image: " + e.getMessage());
		}
	}

	private void moveToNextRound(JSONObject response, int currentRound, int totalRounds) {
		SockServer.currentRound.put(socket, currentRound + 1);
		System.out.println("Advancing to Round: " + (currentRound + 1));
		response.put("message", response.getString("message") + "\nStarting round " + (currentRound + 1)
				+ " of " + totalRounds + "!");
		sendNextWonderHint(response); // Send the first hint of the new round
	}

	private String getCurrentWonder() {
		int currentRound = SockServer.currentRound.getOrDefault(socket, 1) - 1;
		return SockServer.wonders.get(currentRound % SockServer.wonders.size());
	}

	private boolean isCorrectGuess(String userInput) {
		String correctAnswer = getCurrentWonder();
		return userInput.equalsIgnoreCase(correctAnswer);
	}

	private boolean isValidName(String name) {
		return name.matches("[a-zA-Z]{1,20}"); // Allows letters only, up to 20 characters
	}

	private boolean isValidAge(String age) {
		try {
			int ageNum = Integer.parseInt(age);
			return ageNum > 0 && ageNum < 150; // Age should be a reasonable integer
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private void displayCurrentScore(JSONObject response) {
		int currentScore = SockServer.clientScores.getOrDefault(socket, 0);
		int lastRoundScore = SockServer.roundPoints.getOrDefault(socket, 0);

		response.put("type", "displayScore");
		response.put("totalScore", currentScore);
		response.put("pointsThisRound", lastRoundScore);
	}



}
