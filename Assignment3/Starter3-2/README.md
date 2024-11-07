=====================================================
WONDERS GUESSING GAME PROJECT
=====================================================

-----------------------------------------
PROJECT DESCRIPTION
-----------------------------------------
This project is a client-server based guessing game. Players try to identify world-famous
wonders using progressively revealed hints. The client provides a GUI for players to make
guesses, view their score, and see the leaderboard, while the server manages the game logic,
hints, scoring, and leaderboard updates.

-----------------------------------------
FEATURES:
-----------------------------------------
- **Scoring**: Earn more points by guessing with fewer hints. 
-     "With 4 guesses, each round offers a maximum of 25 points (4 * 5 + 5). 
-      Since the total score is doubled, the maximum achievable score is 50 points per round."
- **Hints**: Receive up to 4 hints (images + text) per wonder.
- **Leaderboard**: Tracks top 10 scores for players.
- **Easy Menu Navigation**: Choose "leaderboard", "play", or "quit" from the main menu.

-----------------------------------------
REQUIREMENTS CHECKLIST
-----------------------------------------

[X] Project Description
[X] Requirements Checklist
[X] Protocol Description with Requests & Responses
[ ] Demo Video Link
[X] Robustness Explanation
[X] UDP Protocol Adaptation Explanation

-----------------------------------------
PROTOCOL DESCRIPTION
-----------------------------------------

1. **Connection**:
    - Client initiates with a `{"type": "start"}` message.
    - Server replies with instructions to enter `name` and `age`.

2. **Request Types & Responses**:

   a) Name & Age Submission
    - **Client**: `{"input": "name, age"}`
    - **Server**:
    - *Success*: Main menu message + `question.jpg`
    - *Error*: Invalid input format response

   b) Main Menu
    - **Options**: `"leaderboard"`, `"play"`, `"quit"`
    - **Server Responses**:
    - *Leaderboard*: Shows top scores
    - *Play*: Begins game setup (number of rounds)
    - *Quit*: Ends session
    -        **Note**: *The player can type `"quit"` at any time to exit the game.*

   c) Round Setup
    - **Client**: Specifies rounds (e.g., `{"input": "3"}`)
    - **Server**:
    - *Valid Rounds*: Starts round with 1st hint
    - *Invalid Input*: Error message for invalid number

   d) Gameplay Commands
    - **Commands**: `guess`, `next` (next hint), `skip` (skip wonder)
    - **Server Responses**:
    - *Correct Guess*: Moves to next round
    - *Incorrect Guess*: Updates hints/guesses remaining
    - *Next Hint*: Shows next hint if available
    - *Skip*: Moves to next wonder, resets hints

3. **Error Handling**
    - Invalid actions result in error messages guiding the player.

-----------------------------------------
DEMO VIDEO LINK
-----------------------------------------
[Game Demo Video Link Here]

-----------------------------------------
ROBUSTNESS DESIGN
-----------------------------------------

This game was designed with robustness in mind, through:

- **State Management**: Each client follows a well-defined flow, minimizing unexpected behavior.
- **Error Messages**: Invalid actions prompt error messages, guiding players.
- **Resource Handling**: Each client runs on its own thread, with connections closed cleanly after disconnection.

-----------------------------------------
UDP PROTOCOL ADAPTATION
-----------------------------------------

If we were to switch from TCP to UDP, here’s what would change:

1. **Unreliable Transmission**: UDP does not ensure message order or delivery. We would implement acknowledgment
                                messages and timeouts to handle message loss.
2. **Connectionless Protocol**: UDP is stateless, requiring manual client tracking by IP/port.
3. **Error Checking**: Adding checksums to verify data integrity would help overcome UDP’s lack of error correction.
4. **Message Size Limit**: UDP has a limit of 65,535 bytes; large messages like images would require splitting across multiple packets.


### Terminal

```
	gradle runServer -Pport=1234
```

```
	gradle runClient -Pport=1234 -Phost=192.168.1.10
```

### Code

### ClientGui.java
#### Summary

> The main GUI for the client, displaying an image grid and text output panel.
> This class also handles communication with the server, sending commands, and
> processing responses. 

#### Methods
- ClientGui(String host, int port): Constructor that initializes the GUI frame, establishes a connection
                                    to the server, and starts the listener for server responses.
- show(boolean makeModal): Displays the GUI frame with the current state.
-      Note: makeModal controls whether the GUI suspends background processes. Setting it to true makes the
-            GUI modal, meaning other processes are paused until the window is closed.
- submitClicked(): Handles the submit button click in the output panel. Sends user input to the server and clears the input field.
- Additional Behavior: If "quit" is entered, closes the client connection.
- inputUpdated(String input): Event handler for real-time input updates (implemented as a placeholder with a special "surprise" response).
- connectAndStart(): Opens a server connection and sends an initial start message to the server. Starts a new thread to listen for server responses.
- open(): Establishes a connection to the server using the specified host and port. Initializes the socket, output, and input streams.
- sendToServer(String message): Sends a JSON-formatted message to the server if the connection is active.
- listenToServer(): Continuously listens for responses from the server in a separate thread. Parses JSON responses
                    and calls handleResponse to process them.
- handleResponse(JSONObject response): Processes the server’s JSON response by updating the GUI. It:
-     Displays messages and point updates.
-     Decodes and displays images if provided.
-     Handles errors in JSON parsing, image decoding, and I/O.
- close(): Safely closes the client’s connection to the server, disposes of GUI resources, and marks the client as closed.
- main(String[] args): Entry point to start the ClientGui. Reads host and port from system properties and displays the GUI.


### SockServer.java
#### Summary

> This class represents the server side of the Wonders Guessing Game. It manages client connections, game state, hints,
> scoring, and leaderboard data. Each client connection is handled on a separate thread via the ClientHandler class.

#### Fields

- totalRounds: Tracks the total rounds chosen for each client.
- currentRound: Tracks the current round number for each client.
- currentHint: Tracks the current hint number provided for each client.
- clientNames: Stores the names of connected clients.
- clientStates: Tracks the current game state for each client.
- clientScores: Stores the cumulative scores for each client.
- leaderboard: A global leaderboard of all player scores.
- wondersHints: Stores image hints for each wonder.
- wondersTextHints: Stores text hints for each wonder.
- TOTAL_HINTS: The total number of hints available per wonder.
- wonders: A list of wonder names.

#### Methods

- main(String[] args): Starts the server, loads leaderboard and hint data, and listens for incoming client connections.
                       Each connection is handled in a new ClientHandler thread.
- sendImg(String filename, JSONObject obj): Encodes an image file in Base64 and adds it to the JSON response object.
                                            Sends an error if the file is missing or cannot be read.
- loadWondersHints(): Loads hints and image data for each wonder from wondersHints.json. Populates wondersHints and
                      wondersTextHints with hints for each wonder.
- saveLeaderboard(): Saves the current leaderboard to leaderboard.json in a formatted JSON structure.
- loadLeaderboard(): Loads leaderboard data from leaderboard.json on server startup. Clears and updates the leaderboard map with the loaded data.
- setClientState(Socket socket, int state): Sets the game state for a client, using clientStates to track the state.
- getClientState(Socket socket): Retrieves the current game state of a client.
- removeClientState(Socket socket): Removes a client’s state from the clientStates map upon disconnection.

### SockServer.java
### ClientHandler Class

#### Summary

> This inner class handles communication with a single client, processing requests, managing game rounds,
> and sending responses.

#### Methods

- ClientHandler(Socket socket): Constructor that initializes I/O streams for the client’s socket.
- run(): The main loop that listens for client messages. Parses each message as JSON, processes it, and sends a
         response. Closes the connection if the client disconnects.
- handleClientMessage(JSONObject message): Processes client requests based on the current game state and builds an appropriate response.
- State 0 (Initial): Prompts the client for name and age.
- State 1 (Main Menu): Displays options: leaderboard, play, or quit.
- State 2 (Round Selection): Prompts client to choose the number of rounds.
- State 3 (Gameplay): Processes guesses, hint requests, and moves between wonders.
- sendNextWonderHint(JSONObject response): Sends the next available hint (text + image) for the current wonder.
- sendHintImage(JSONObject response, String wonder, int hintNumber): Adds the image and text hint for a specific wonder and
                                                                     hint number to the response object.
- advanceToNextRound(JSONObject response): Moves to the next round, updating the score and checking for game completion.
- calculatePoints(): Calculates points earned based on the number of hints used.
- updateClientScore(int pointsEarned): Adds the points earned in the current round to the client’s total score.
- addScoreToResponse(JSONObject response, int pointsEarned): Adds the current round’s points and the total score to the response.
- isGameOver(int currentRound, int totalRounds): Checks if the game is complete by comparing the current round with the total rounds.
- endGame(JSONObject response, int totalRounds): Ends the game for the client, displays the final score, and updates the leaderboard.
- updateLeaderboard(): Updates the leaderboard with the client’s final score and saves it to leaderboard.json.
- sendEndGameImage(JSONObject response): Sends an end-game image (win.jpg) to the client.
- moveToNextRound(JSONObject response, int currentRound, int totalRounds): Advances to the next round and sends the first hint of the new round.
- getCurrentWonder(): Retrieves the current wonder based on the round number.
- isCorrectGuess(String userInput): Compares the client’s input with the correct answer for the current wonder.
- isValidName(String name): Validates the client’s name input using a regex for alphabetical characters.
- isValidAge(String age): Checks if the age input is a valid integer within a reasonable range (1-150).
- displayCurrentScore(JSONObject response): Displays the client’s current and last round scores.


### PicturePanel.java

#### Summary

> This is the image grid

#### Methods

- newGame(int dimension) :  Reset the board and set grid size to dimension x dimension
- insertImage(String fname, int row, int col) :  Insert an image at (col, row)
- insertImage(ByteArrayInputStream fname, int row, int col) :  Insert an image at (col, row)

### OutputPanel.java

#### Summary

> This is the input box, submit button, and output text area panel

#### Methods

- getInputText() :  Get the input text box text
- setInputText(String newText) :  Set the input text box text
- addEventHandlers(EventHandlers handlerObj) :  Add event listeners
- appendOutput(String message) :  Add message to output text



=====================================================
END OF README
=====================================================