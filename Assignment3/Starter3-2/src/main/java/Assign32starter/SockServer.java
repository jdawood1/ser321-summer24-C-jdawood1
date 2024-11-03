package Assign32starter;
import java.net.*;
import java.util.Base64;
import java.util.Set;
import java.util.Stack;
import java.util.*;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import java.awt.image.BufferedImage;
import java.io.*;
import org.json.*;


/**
 * A class to demonstrate a simple client-server connection using sockets.
 * Ser321 Foundations of Distributed Software Systems
 */
public class SockServer {
	static Stack<String> imageSource = new Stack<String>();

	public static void main(String args[]) {
		try {
			// default to 8888 if not provided
			int port = Integer.parseInt(System.getProperty("port", "8888"));
			ServerSocket serv = new ServerSocket(port);
			System.out.println("Server ready for connection on port: " + port);

			// Accept connections in a loop
			while (true) {
				Socket sock = serv.accept();
				System.out.println("Client connected: " + sock.getInetAddress().getHostAddress());

				// Create a new thread for each client
				new Thread(new ClientHandler(sock)).start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static JSONObject sendImg(String filename, JSONObject obj) throws Exception {
		File file = new File("img/" + filename);
		System.out.println("Attempting to load image: " + filename);

		if (file.exists()) {
			BufferedImage image = ImageIO.read(file);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(image, "png", baos);
			String encodedImage = Base64.getEncoder().encodeToString(baos.toByteArray());

			obj.put("image", encodedImage);
			System.out.println("Encoded image data for file: " + filename);
		} else {
			obj.put("image", "");
			obj.put("error", "Image not found: " + filename);
			System.out.println("Image not found: " + filename);
		}
		return obj;
	}
}

class ClientHandler implements Runnable {
	private Socket socket;
	private PrintWriter outWrite;
	private BufferedReader in;
	private int rounds;
	private int currentRound = 0;
	private int score = 0;
	private int currentHintIndex = 0;
	private static final int hintsPerWonder = 4;
	private static final List<Wonder> wonders = List.of(
			new Wonder("Colosseum", "Hint: Ancient gladiator arena", "Colosseum1.png"),
			new Wonder("Grand Canyon", "Hint: Famous canyon in the USA", "GrandCanyon1.png")
	);


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
			String name = "";
			System.out.println("Started handling client: " + socket.getInetAddress().getHostAddress());

			while (true) {
				// Read incoming request as a string
				String fromClient = in.readLine();
				if (fromClient == null) {
					System.out.println("Client disconnected");
					break;
				}

				System.out.println("Received from client: " + fromClient);
				JSONObject json = new JSONObject(fromClient);
				JSONObject response = new JSONObject();

				// Handle different request types
				switch (json.getString("type")) {
					case "start":
						response.put("type", "hello");
						response.put("value", "Hello, please tell me your name and age. (e.g. name, age)");
						System.out.println("Sent to client: " + response.toString());
						SockServer.sendImg("hi.png", response);
						outWrite.println(response.toString());
						break;

					case "name_age":
						name = json.optString("name", "");
						String ageString = json.optString("age", "");

						int age = Integer.parseInt(ageString);
						response.put("value", "MAIN-MENU");
						response.put("type", "messages");
						response.put("message", "Hello, " + name + ", age " + age + "! Welcome to the game. "
								+ "\nPlease type 'leaderboard', 'play', or 'quit' to choose an option.");
						System.out.println("Sent to client: " + response.toString());
						outWrite.println(response.toString());
						break;

					case "leaderboard":
						response.put("value", "MAIN-MENU");
						response.put("type", "messages");
						response.put("message", "Leaderboard:\n1. Player1 - 100 points\n2. Player2 - 90 points\n3. Player3 - 85 points\n" +
								"\nPlease type 'leaderboard', 'play', or 'quit' to choose an option.");
						System.out.println("Sent to client: " + response.toString());
						outWrite.println(response.toString());
						break;

					case "play":
						response.put("value", "ROUNDS");
						response.put("type", "messages");
						response.put("message", "How many rounds would you like to play?");
						System.out.println("Sent to client: " + response.toString());
						outWrite.println(response.toString());
						break;

					case "set_rounds":
						rounds = json.getInt("rounds");
						currentRound = 0;
						currentHintIndex = 0;
						score = 0;
						response.put("type", "round_start");
						System.out.println("Sent to client: " + response.toString());
						startRound(response);
						break;

					case "guess":
						handleGuess(json.getString("message"), response);
						break;

					case "skip":
						handleSkip(response);
						break;

					case "next":
						handleNextHint(response);
						break;

					case "remaining":
						handleRemainingHints(response);
						break;

					case "quit":
						response.put("type", "quit_game");
						response.put("value", "Goodbye! Thanks for playing.");
						System.out.println("Sent to client: " + response.toString());
						outWrite.println(response.toString());
						break;

					default:
						response.put("type", "error");
						response.put("message", "Unknown request type");
						response.put("value", "MAIN-MENU");
						System.out.println("Sent to client: " + response.toString());
						outWrite.println(response.toString());
						break;
				}
			}
		} catch (EOFException e) {
			System.out.println("Client disconnected due to EOFException.");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				socket.close();
				System.out.println("Closing client connection: " + socket.getInetAddress().getHostAddress());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void startRound(JSONObject response) {
		Wonder wonder = wonders.get(currentRound % wonders.size());
		System.out.println("Answer for grading: " + wonder.getName());
		response.put("type", "hint");
		response.put("value", "Round " + (currentRound + 1) + ": " + wonder.getHint());
		try {
			SockServer.sendImg(wonder.getImageFile(), response);
			System.out.println("Image successfully loaded and encoded for next hint.");
		} catch (Exception e) {
			System.out.println("Failed to load image: " + wonder.getImageFile());
			response.put("image", "");  // Clear image data in case of failure
			response.put("error", "Image not found: " + wonder.getImageFile());
		}
		outWrite.println(response.toString());
	}

	private void handleGuess(String guess, JSONObject response) {
		Wonder wonder = wonders.get(currentRound % wonders.size());
		if (guess.equalsIgnoreCase(wonder.getName())) {
			score += (hintsPerWonder - currentHintIndex) * 5 + 5;
			currentRound++;
			currentHintIndex = 0;
			response.put("type", "correct_guess");
			response.put("value", "Correct! Moving to the next round.");
			startRound(response);
		} else {
			response.put("type", "incorrect_guess");
			response.put("value", "Incorrect. Try again!");
			outWrite.println(response.toString());
		}
	}

	private void handleSkip(JSONObject response) {
		currentRound++;
		currentHintIndex = 0;
		response.put("type", "skip");
		response.put("value", "You skipped this round. Moving to the next Wonder.");
		startRound(response);
	}

	private void handleNextHint(JSONObject response) {
		Wonder wonder = wonders.get(currentRound % wonders.size());
		currentHintIndex++;
		if (currentHintIndex >= hintsPerWonder) {
			response.put("type", "no_more_hints");
			response.put("value", "No more hints available for this Wonder.");
		} else {
			response.put("type", "next_hint");
			response.put("value", wonder.getHint());
			try {
				SockServer.sendImg(wonder.getImageFile(), response);
				System.out.println("Image successfully loaded and encoded for next hint.");
			} catch (Exception e) {
				System.out.println("Failed to load image: " + wonder.getImageFile());
				response.put("image", "");  // Clear image data in case of failure
				response.put("error", "Image not found: " + wonder.getImageFile());
			}
		}
		outWrite.println(response.toString());
	}

	private void handleRemainingHints(JSONObject response) {
		response.put("type", "remaining_hints");
		response.put("value", "You have " + (hintsPerWonder - currentHintIndex) + " hints remaining.");
		outWrite.println(response.toString());
	}


}

// Wonder class to store details about each wonder
class Wonder {
	private final String name;
	private final String hint;
	private final String imageFile;

	public Wonder(String name, String hint, String imageFile) {
		this.name = name;
		this.hint = hint;
		this.imageFile = imageFile;
	}

	public String getName() {
		return name;
	}

	public String getHint() {
		return hint;
	}

	public String getImageFile() {
		return imageFile;
	}
}