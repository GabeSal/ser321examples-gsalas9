# Riddle Me This: A Client-Server Riddle Game

## Description

**Riddle Me This** is a Java-based client-server riddle game that allows players to:
- Solve riddles for points.
- View a persistent leaderboard.
- Add new riddles to a pending list.
- Vote on submitted riddles to include them in the game.
- See visual feedback via server-sent images for every game state (welcome, playing, voting, etc.).

Players interact through a rich Swing GUI (`ClientGui`) while a multithreaded `SockServer` processes logic, manages player sessions, and maintains state across restarts.

---

##  Requirements Checklist

| Requirement | Description | Fulfilled |
|------------|-------------|-----------|
| **1** | Image updates per game state from server (Base64-encoded) | yes |
| **2** | Client connects to server on startup | yes |
| **3** | Server asks and greets client by name | yes |
| **4** | Main menu with play, leaderboard, add, vote, quit options | yes |
| **5** | All game logic lives on the server (riddles, answers, scores) | yes |
| **6a** | Start game → server sends random riddle + image | yes |
| **6b** | Server logs correct answer | yes |
| **6c** | Client can guess or type `"next"` | yes |
| **6d** | Correct guesses: +10 points, server responds with new riddle | yes |
| **6e** | `"next"` command skips riddle with -5 points | yes |
| **6f** | `"exit"` ends session and shows final score | yes |
| **6g** | Final score shown to player | yes |
| **6h** | Game returns to main menu after round | yes |
| **7a** | Add riddle flow with image and inputs | yes |
| **7b** | Riddle stored in pending list | yes |
| **7c** | Game returns to menu | yes |
| **8a** | Voting shows pending riddle with answer and image | yes |
| **8b** | Votes decide if riddle is added or removed | yes |
| **8c** | Game returns to menu | yes |
| **9a** | Leaderboard maintained during server runtime | yes |
| **9b** | Player added to leaderboard after play session | yes |
| **9c** | Only highest score per player kept | yes |
| **9d** | Scoring logic: +10 (correct), -1 (wrong), -5 (next) | yes |
| **10** | Persistent leaderboard and sessions between server restarts | yes (EXTRA) |

---

## Protocol Specification

### Client Requests

Each request is a JSON object with a `"type"` and other fields as needed:

#### 1. `start`
```json
{ "type": "start", "name": "Alice" }

#### Server Response:
```json
{ "type": "hello", "value": "Hello Alice, welcome to the game!", "imageData": "..." }

#### 2. `play`
```json
{ "type": "start", "name": "Alice" }

#### Server Response:
```json
{ "type": "play", "riddle": "I dry as I get wetter.", "answer": "Towel", "score": 0, "imageData": "..." }

#### 3. `guess`
```json
{ "type": "start", "name": "Alice" }

#### Possible Responses:

#### `Correct`
```json
{ "type": "correct", "message": "Correct! +10 points.", "riddle": "Next riddle", "answer": "Answer", "points": 10, "imageData": "..." }

#### `Incorrect`
```json
{ "type": "incorrect", "message": "Incorrect! -1 point.", "points": 9 }

#### 4. `next`
```json
{ "type": "start", "name": "Alice" }

#### Server Response:
```json
{ "type": "next", "riddle": "Another riddle", "answer": "Answer", "points": 5, "imageData": "...", "result": "Skipped! -5 points." }

#### 5. `exit`
```json
{ "type": "exit", "name": "Alice" }

#### Server Response:
```json
{ "type": "end", "message": "Thanks for playing! Final score: 42" }

#### 6. `leaderboard`
```json
{ "type": "exit", "name": "Alice" }

#### Server Response:
```json
{ "type": "leaderboard", "entries": [{ "name": "Alice", "score": 42 }, ...], "imageData": "..." }


#### 7. `addRiddle`
```json
{ "type": "start", "name": "Alice" }

#### Server Response:
```json
{ "type": "addRiddle", "message": "New riddle added to pending list!", "imageData": "..." }

#### 8. `getVoteRiddle`
```json
{ "type": "start", "name": "Alice" }

#### Server Response:
```json
{ "type": "vote", "riddle": "What’s brown and sticky?", "answer": "A stick", "imageData": "..." }

#### 9. `vote`
```json
{ "type": "vote", "vote": "yes" }

#### Server Response:
```json
{ "type": "vote-result", "message": "Riddle approved and added to the game!", "imageData": "..." }

#### 10. `quit`
```json
{ "type": "quit", "name": "Alice" }

#### Server Response:
```json
{ "type": "quit", "message": "Thanks for playing!" }

---

## What If We Used UDP Instead of TCP?

Using UDP would require several non-trivial changes and trade-offs due to its connectionless, unreliable nature:

### 1. Stateless Communication
- **No session connection**: Unlike TCP, UDP does not support persistent connections. You must track users manually using `InetAddress + port`.
- **Stateful logic must be rebuilt**: All user state would need to be re-sent or looked up by identifier on each message.

### 2. Reliability Concerns
- **No built-in delivery guarantee**: Messages can be dropped, reordered, or duplicated.
- You would need to implement:
  - Acknowledgment messages (ACK/NACK).
  - Sequence numbers to reassemble or reorder packets.
  - Retransmission timeouts and retries.

### 3. Image Transfer Issues
- **Datagram size limits**: Standard UDP payloads are limited to ~64 KB.
- You’d need to:
  - Split images into multiple packets.
  - Track and reassemble fragments in order.
  - Handle potential loss or corruption during reassembly.

### 4. Implementation Modifications
- All `Socket`/`PrintWriter`/`BufferedReader` code would be replaced with `DatagramSocket` and `DatagramPacket`.
- Server would continuously `receive()` packets and parse them to distinguish request types.
- Every response would need to manually encode JSON and send to the correct address/port.

### 5. Security Considerations
- **No inherent connection validation**: Potential for spoofing or abuse unless message authenticity is validated (e.g., signed hashes, tokens).

---

### Summary
While technically possible, **UDP** is not appropriate for this application’s needs due to:
- High reliance on game state and session consistency.
- Frequent bidirectional communication.
- Image data sizes and need for guaranteed delivery.

**TCP** was the correct choice for this project.

---

## Authors

- Developed by SER321 ASU Team and Gabriel Salas
- Java, Swing, Socket Programming
- SER 321 - Distributed Software Systems
