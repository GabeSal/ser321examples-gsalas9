# Protobuf Guessing Game
## Description
This project implements a multiplayer terminal-based letter-guessing game using Google Protocol Buffers (Protobuf) for structured communication between a Java-based client and server over sockets. Each client can view a leaderboard, start a personalized game session, and guess letters of a hidden phrase. The server tracks logins, wins, and persistent logs across restarts.

## Major Classes
### SockBaseClient ```(client/SockBaseClient.java)```
- Handles user input and communicates with the server using Protobuf messages. 
- Presents a text-based menu and controls gameplay.

### SockBaseServer ```(server/SockBaseServer.java)```
- Accepts client connections and spawns a handler per client using a shared Game instance. 
- Manages all protocol-compliant logic including guess processing, state persistence, and leaderboard generation.

### Game ```(server/Game.java)```
- Manages game state including the current phrase, hidden version, guessed letters, score tracking, and validation. 
- Ensures phrase selection and user-specific gameplay logic.

### Player ```(client/Player.java)```
Simple POJO used optionally by the client to manage and sort leaderboard entries (by win count).

## Protocol Buffers
This project uses .proto definitions to structure messages exchanged between clients and the server. The definitions are:

### ```request.proto```
Defines client-side requests with types: 
- NAME
- LEADERBOARD
- START
- GUESS
- QUIT.

### ```response.proto```
Defines server-side responses with types: 
- WELCOME
- LEADERBOARD
- TASK
- WON
- LOST
- ERROR
- BYE 

It also includes a Leader message used for leaderboard responses.

All Protobuf files are compiled into Java using the Gradle Protobuf plugin and placed in ```build/generated/source/proto/main/java.```

## Protocol Overview (see ```PROTOCOL.md``` for full details)
1. Client sends NAME → Server replies with WELCOME 
2. Client sends START → Server begins new Game and responds with TASK 
3. Client sends GUESS (single letter) → Server updates state and responds with updated TASK, WON, or LOST 
4. Client sends LEADERBOARD → Server responds with LEADERBOARD list 
5. Client sends QUIT → Server responds with BYE

## Scoring and Game Rules
- Games start at 10 points. 
- +1 point for each new letter revealed. 
- -1 point for incorrect or duplicate guesses. 
- Game is lost when score reaches 0. 
- Game is won when all letters are revealed.

Winning entries are logged with the highest score per name.

## Persistent Logging
- All client logins and game outcomes (WIN) are logged to logs.txt. 
- Logs persist across server restarts. 
- Log format: Date: Name - MessageType

## Leaderboard
- Built from the logs file. 
- Each player has a win count and login count. 
- Shared across all connected clients.

## How to Run
Ensure ``protobuf`` files are present in the ``build/generated/source/proto/main/java/proto`` directory. "Host" and "port" parameters must be defined.
If connecting to AWS, then ```-Phost="52.14.11.155"``` for the client, and the port should be ```-Pport=8500```.
If connecting locally, then ```-Phost=localhost``` and ```-Pport=[your port # here]``` should work fine.

### To start the server:
```
gradle runServer -Pport=8500
```

### To start the client:
```
gradle runClient -Phost=localhost -Pport=8500
```