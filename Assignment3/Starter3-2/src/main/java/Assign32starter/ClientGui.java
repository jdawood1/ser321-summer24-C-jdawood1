package Assign32starter;

import org.json.JSONException;
import org.json.JSONObject;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Dimension;

import javax.swing.JDialog;
import javax.swing.WindowConstants;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import java.util.Base64;
import java.net.Socket;
import java.io.*;

public class ClientGui implements OutputPanel.EventHandlers {
	private final JDialog frame;
	private final PicturePanel picPanel;
	private final OutputPanel outputPanel;
	private Socket sock;
	private PrintWriter out;
	private BufferedReader bufferedReader;
	private boolean isClosed = false;
	private final String host;
	private final int port;

	public ClientGui(String host, int port) throws IOException {
		this.host = host;
		this.port = port;

		// Setup GUI frame
		frame = new JDialog();
		frame.setLayout(new GridBagLayout());
		frame.setMinimumSize(new Dimension(1300, 750));
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

		picPanel.newGame(1); // Initialize the grid for displaying images

		connectAndStart();
	}

	private void connectAndStart() throws IOException {
		open(); // Open server connection

		// Send initial start message to server
		sendToServer("{\"type\": \"start\"}");

		// Start listening to server responses in a new thread
		new Thread(this::listenToServer).start();
	}

	private void open() throws IOException {
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

	@Override
	public void submitClicked() {
		if (isClosed) {
			outputPanel.appendOutput("Connection is already closed.");
			return;
		}

		String input = outputPanel.getInputText().trim();
		outputPanel.setInputText(""); // Clear input field
		outputPanel.clearOutput();

		if (input.isEmpty()) {
			outputPanel.appendOutput("Please enter a command.");
			return;
		}

		System.out.println("Sending user input to server: " + input);
		sendToServer("{\"input\": \"" + input + "\"}");

		if ("quit".equalsIgnoreCase(input)) {
			close();
		}
	}

	@Override // plug
	public void inputUpdated(String input) {
		if (input.equals("surprise")) {
			outputPanel.appendOutput("You got me!");
		}
	}

	private void sendToServer(String message) {
		if (out != null) {
			out.println(message);
			out.flush();
		}
	}

	private void listenToServer() {
		try {
			String response;
			while ((response = bufferedReader.readLine()) != null) {
				try {
					if (response.contains("image")) {
						System.out.println("Received response from server: encoded image");
					} else {
						System.out.println("Received response from server: " + response);
					}
					handleResponse(new JSONObject(response));
				} catch (JSONException e) {
					System.err.println("Error parsing server response: " + e.getMessage());
					outputPanel.appendOutput("Received invalid response from server. Please try again.");
				}
			}
		} catch (IOException e) {
			if (!isClosed) {
				System.err.println("Connection to server lost: " + e.getMessage());
				outputPanel.appendOutput("Lost connection to server. Please check your network and try again.");
				close();
			}
		}
	}


	private void handleResponse(JSONObject response) {
		String type = response.optString("type", "unknown");
		String message = response.optString("message", "Unknown response from server.");
		outputPanel.appendOutput(message);

		// Update points display if provided by the server
		int pointsThisRound = response.optInt("pointsThisRound", 0);
		int totalScore = response.optInt("totalScore", 0);
		outputPanel.setPoints(pointsThisRound, totalScore);

		// Display image if provided
		if (response.has("image") && !response.getString("image").isEmpty()) {
			try {
				byte[] imageBytes = Base64.getDecoder().decode(response.getString("image"));
				BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
				if (img != null) {
					picPanel.setImage(img, 0, 0);
					System.out.println("Image successfully loaded and displayed.");
				} else {
					System.err.println("Failed to decode image: BufferedImage is null.");
					outputPanel.appendOutput("Error: Unable to load image.");
				}
			} catch (IllegalArgumentException e) {
				System.err.println("Base64 decoding failed: " + e.getMessage());
				outputPanel.appendOutput("Error: Corrupted image data received from server.");
			} catch (IOException e) {
				System.err.println("Error reading image data: " + e.getMessage());
				outputPanel.appendOutput("Error: Unable to display image from server.");
			} catch (PicturePanel.InvalidCoordinateException e) {
                throw new RuntimeException(e);
            }
        } else if (type.equals("instruction")) {
			System.out.println("No image data in the server response.");
		}
	}


	public void close() {
		if (isClosed) return;

		System.out.println("Closing connection to server...");
		isClosed = true;

		try {
			if (out != null) out.close();
			if (bufferedReader != null) bufferedReader.close();
			if (sock != null) sock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

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
