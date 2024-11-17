# Description 
This is a simple Sudoku game where multiple players can play separate games while sharing the same leaderboard.
Players can enter a name, play Sudoku puzzles, and compete for the highest scores. Players can type "exit" at any
point during the game to quit and disconnect from the server. The server handles client requests concurrently and
updates the leaderboard in real-time.

## How to run the program
The proto file can be compiled using
``gradle generateProto``  

This will also be done when building the project.  

You should see the compiled proto file in Java under
> build/generated/source/proto/main/java/buffers  

Now you can run the client and server, please follow these instructions to start:
* Please run `gradle runServer -Pport=port` and `gradle runClient -Phost=hostIP -Pport=port` together.
* There is a separate task `gradle runServerGrading -Pport=port` which has a given board that you can also use for testing
  * Solution as row col val
  * 1 8 8
    1 5 7
    2 6 2
    8 1 9
    8 4 2
    9 2 5
    9 5 1
* Can also be run using `gradle runServer` and `gradle runClient -q --console=plain` for localhost and default port
* Recommended that you include the flag `-q --console=plain` to get the best gaming experience (limited output)
* Programs runs on hostIP
* Port and hostIP specification is optional.
* NOTE: If for some reason the .txt files are causing the server or client to crash please either delete them or clear their contents.
  * Hasn't happened to me but, I wanted to make a note just in case someone comes across this!


## Screencast
> here

# Readme checklist:
- A description of your project and a detailed description of what it does.
- An explanation of how we can run the program.
- Name the requirements that you think you fulfilled.


# Requirements (If checked off then completed and includes debugging)
- The game supports concurrent players, with each having their own game instance.
- The leaderboard updates in real-time and is persistent across sessions.
- The game provides error messages for invalid inputs.
- The server handles client disconnections gracefully.
- Includes a predefined grading board for testing purposes.
- Debugging information included in server and client logs.
- Ensures file consistency for leaderboard storage.