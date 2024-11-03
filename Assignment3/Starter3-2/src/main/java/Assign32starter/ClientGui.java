package Assign32starter;

import org.json.*;
import java.util.Base64;
import javax.imageio.ImageIO;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.ByteArrayInputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import javax.swing.JDialog;
import javax.swing.WindowConstants;

public class ClientGui implements OutputPanel.EventHandlers {
	JDialog frame;
	PicturePanel picPanel;
	OutputPanel outputPanel;
	String currentMess;
	Socket sock;
	PrintWriter out;
	BufferedReader bufferedReader;
	String host;
	int port;

	public ClientGui(String host, int port) throws IOException {
		this.host = host;
		this.port = port;

		// Setup GUI frame
		frame = new JDialog();
		frame.setLayout(new GridBagLayout());
		frame.setMinimumSize(new Dimension(500, 500));
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		// Setup PicturePanel
		picPanel = new PicturePanel();
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 0.25;
		frame.add(picPanel, c);

		// Setup OutputPanel
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.weighty = 0.75;
		c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;
		outputPanel = new OutputPanel();
		outputPanel.addEventHandlers(this);
		frame.add(outputPanel, c);

		picPanel.newGame(1);

		connectAndStart();
	}

	private void connectAndStart() throws IOException {;
		open(); // Open server connection once

		// Send initial start message to server
		currentMess = "{\"type\": \"start\"}";
		System.out.println("Sending start message to server: " + currentMess);
		out.println(currentMess);
		out.flush();

		// Read response from server
		String string = bufferedReader.readLine();
		if (string.contains("image")) {
			System.out.println("Received response from server: Sent encoded image.");
		} else {
			System.out.println("Received response from server: " + string);
		}
		JSONObject json = new JSONObject(string);
		outputPanel.appendOutput(json.getString("value"));

		// Update current message immediately after receiving the response
		currentMess = json.getString("value");
		System.out.println("Updated currentMess after initial response: " + currentMess);

		// Handle image if present
		if (json.has("image") && !json.getString("image").isEmpty()) {
			String encodedImage = json.getString("image");
			byte[] imageBytes = Base64.getDecoder().decode(encodedImage);
			BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
			try {
				picPanel.setImage(img, 0, 0);
				System.out.println("Displaying received image.");
			} catch (PicturePanel.InvalidCoordinateException e) {
				e.printStackTrace();
				outputPanel.appendOutput("Error: Invalid coordinates for image placement.");
			}
		} else {
			System.out.println("No image received in the response.");
			outputPanel.appendOutput("No image found in server response.");
		}
	}

	public void open() throws UnknownHostException, IOException {
		System.out.println("Opening connection to server at " + host + ":" + port);
		this.sock = new Socket(host, port);
		this.out = new PrintWriter(sock.getOutputStream(), true);
		this.bufferedReader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
	}

	public void show(boolean makeModal) {
		System.out.println("Showing GUI frame...");
		frame.pack();
		frame.setModal(makeModal);
		frame.setVisible(true);
	}

	@Override // Hot wire
	public void inputUpdated(String input) {
		if (input.equals("surprise")) {
			outputPanel.appendOutput("You found me!");
		}
	}

	@Override
	public void submitClicked() {
		try {

			System.out.println("Submit button clicked.");

			// Get input from user and trim whitespace
			String input = outputPanel.getInputText().trim();
			if (input.isEmpty()) {
				System.out.println("No input provided.");
				outputPanel.appendOutput("Please enter a command.");
				return; // Stop processing if input is empty
			} System.out.println("User input: " + input);

			// Prepare the JSON request object based on current context
			JSONObject jsonRequest = handleRequest(input);
			if (jsonRequest == null) {
				System.out.println("Invalid input; no request sent.");
				return; // Stop if request preparation failed
			}

			// Send request to server
			System.out.println("Sending request to server: " + jsonRequest.toString());
			out.println(jsonRequest.toString());
			out.flush();

			// Receive response from server
			String string = bufferedReader.readLine();
			if (string.contains("image")) {
				System.out.println("Received response from server: Sent encoded image.");
			} else {
				System.out.println("Received response from server: " + string);
			}

			// Handle server response
			JSONObject response = new JSONObject(string);
			if (response.has("type")) {
				String type = response.getString("type");

				if (type.equals("error")) {
					// Display error message
					outputPanel.appendOutput("Error: " + response.getString("errorMessage"));
				} else {
					// Handle other types generically by displaying the value
					outputPanel.appendOutput(response.getString("message"));
				}
			} else {
				// Default action if no type is specified
				outputPanel.appendOutput("Unknown response format received.");
			}

			// Update current message for context
			currentMess = response.getString("value");
			System.out.println("Update currentMess: " + currentMess);

			// Clear input text field after submit
			outputPanel.setInputText("");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private JSONObject handleRequest(String input) {
		System.out.println("handling request..");

		JSONObject jsonRequest = new JSONObject();
		if (currentMess.contains("tell me your name and age")) {
			String[] parts = input.split(",");
			if (parts[0].trim().matches("[a-zA-Z]+")
					&& parts[1].trim().matches("\\d+")) {
				jsonRequest.put("type", "name_age");
				jsonRequest.put("name", parts[0].trim());
				jsonRequest.put("age", parts[1].trim());
				outputPanel.clearOutput();
			} else {
				outputPanel.appendOutput("Please provide both name and age.");
				return null;
			}

		} else if (currentMess.contains("MAIN-MENU")) {
			switch (input.toLowerCase()) {
				case "leaderboard":
					jsonRequest.put("type", "leaderboard");
					outputPanel.clearOutput();
					break;
				case "play":
					jsonRequest.put("type", "play");
					outputPanel.clearOutput();
					break;
				case "quit":
					jsonRequest.put("type", "quit");
					close();
					break;
				default:
					outputPanel.appendOutput("Invalid option. Please type 'leaderboard', 'play', or 'quit'.");
					return null;
			}

		} else if (currentMess.contains("ROUNDS")) {
			try {
				int rounds = Integer.parseInt(input);
				jsonRequest.put("type", "set_rounds");
				jsonRequest.put("rounds", rounds);
				outputPanel.clearOutput();
			} catch (NumberFormatException e) {
				outputPanel.appendOutput("Please enter a valid number for rounds.");
				return null;
			}

		} else if (currentMess.contains("Hint")) {
			// Handle gameplay commands: "skip", "next", "remaining", or a guess
			if ("skip".equalsIgnoreCase(input)) {
				jsonRequest.put("type", "skip");
			} else if ("next".equalsIgnoreCase(input)) {
				jsonRequest.put("type", "next");
			} else if ("remaining".equalsIgnoreCase(input)) {
				jsonRequest.put("type", "remaining");
			} else if (!input.isEmpty()) {
				jsonRequest.put("type", "guess");
				jsonRequest.put("message", input);
			} else {
				outputPanel.appendOutput("Please enter a valid guess or command.");
				return null;
			}

		} else {
			jsonRequest.put("type", "input");
			jsonRequest.put("message", input);
		}

		return jsonRequest;
	}



	public boolean insertImage(String filename, int row, int col) throws IOException {
		System.out.println("Attempting to insert image: " + filename + " at position (" + row + ", " + col + ")");
		try {
			if (picPanel.insertImage(filename, row, col)) {
				return true;
			}
			outputPanel.appendOutput("File(\"" + filename + "\") not found.");
		} catch (PicturePanel.InvalidCoordinateException e) {
			outputPanel.appendOutput(e.toString());
		}
		return false;
	}

	public void close() {
		System.out.println("Closing connection to server...");
		try {
			if (out != null) out.close();
			if (bufferedReader != null) bufferedReader.close();
			if (sock != null) sock.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Close the GUI frame
		if (frame != null) {
			frame.dispose();
			System.out.println("Closed GUI window.");
		}
	}

	public static void main(String[] args) throws IOException {
		String host = System.getProperty("host", "localhost");
		int port = Integer.parseInt(System.getProperty("port", "8888"));
		ClientGui main = new ClientGui(host, port);
		main.show(true);
	}
}
