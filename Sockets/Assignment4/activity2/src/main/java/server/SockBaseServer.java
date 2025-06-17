package server;

import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONObject;
import proto.RequestProtos.*;
import proto.ResponseProtos.*;

class SockBaseServer {
    static final int MAX_PLAYERS = 16;
    static final String LEADERBOARD_FILE = "leaderboard.json";

    ServerSocket socket = null;
    InputStream in = null;
    OutputStream out = null;
    Socket clientSocket = null;
    Game game;


    public SockBaseServer(Socket sock, Game game){
        this.clientSocket = sock;
        this.game = game;
        this.game.newGame(); // NEW: start new game per client

        try {
            in = clientSocket.getInputStream();
            out = clientSocket.getOutputStream();
        } catch (Exception e){
            System.out.println("Error in constructor: " + e);
        }
    }

    // Handles the communication right now it just accepts one input and then is done you should make sure the server stays open
    // can handle multiple requests and does not crash when the client crashes
    // you can use this server as based or start a new one if you prefer. 
    public void handleRequests() {
        System.out.println("Ready...");
        String name = "";

        boolean quit = false;
        boolean inGame = false;

        try {
            while (!quit) {
                Request request = Request.parseDelimitedFrom(in);
                if (request == null) continue;

                Response.Builder responseBuilder = Response.newBuilder();

                switch (request.getOperationType()) {
                    case NAME:
                        name = request.getName();
                        responseBuilder.setResponseType(Response.ResponseType.WELCOME)
                                .setHello("Hello " + name + ", welcome to the guessing game!");

                        Map<String, int[]> lb = readLeaderboardFile();
                        // stats order: {logins, wins, score}
                        int[] stats = lb.getOrDefault(name, new int[] {0, 0, 10});
                        stats[0]++; // increment logins
                        lb.put(name, stats);
                        writeLeaderboardFile(lb);
                        break;

                    case LEADERBOARD:
                        responseBuilder.setResponseType(Response.ResponseType.LEADERBOARD);
                        Map<String, int[]> lbMap = readLeaderboardFile();

                        // Convert map to list and sort by wins (descending), then logins (descending)
                        List<Map.Entry<String, int[]>> sortedEntries = new ArrayList<>(lbMap.entrySet());
                        sortedEntries.sort((a, b) -> {
                            int winCompare = Integer.compare(b.getValue()[1], a.getValue()[1]); // wins descending
                            return (winCompare != 0) ? winCompare : Integer.compare(b.getValue()[2], a.getValue()[2]); // points descending
                        });

                        // Cap to top 25
                        int count = 0;
                        for (Map.Entry<String, int[]> entry : sortedEntries) {
                            if (count++ >= 25) break;
                            Leader l = Leader.newBuilder()
                                    .setName(entry.getKey())
                                    .setLogins(entry.getValue()[0])
                                    .setWins(entry.getValue()[1])
                                    .setPoints(entry.getValue()[2])
                                    .build();
                            responseBuilder.addLeaderboard(l);
                        }
                        break;

                    case START:
                        inGame = true;

                        responseBuilder.setResponseType(Response.ResponseType.TASK)
                                .setPhrase(game.getPhrase())
                                .setTask(game.getTask());
                        break;

                    case GUESS:
                        if (!inGame) {
                            sendError(responseBuilder, 2, "You must start a game first.");
                            break;
                        }

                        String guess = request.getGuess();
                        if (guess.length() != 1 || !Character.isLetter(guess.charAt(0))) {
                            sendError(responseBuilder, 4, "Invalid guess. Please guess one letter.");
                            break;
                        }

                        char c = Character.toUpperCase(guess.charAt(0));
                        boolean correct = game.markGuess(c);
                        String newPhrase = game.getPhrase();

                        String taskMsg = "Guess another letter.\n";
                        taskMsg += "Score: " + game.getPoints() + "\n";
                        taskMsg += "Correct guesses: " + game.getCorrectGuesses() + "\n";
                        taskMsg += "Incorrect guesses: " + game.getIncorrectGuesses();

                        if (!newPhrase.contains("_")) {
                            responseBuilder.setResponseType(Response.ResponseType.WON)
                                    .setPhrase(newPhrase)
                                    .setMessage("Congratulations! You won with " + game.getPoints() + " points!");

                            lb = readLeaderboardFile();
                            stats = lb.getOrDefault(name, new int[] {0, 0, 10});
                            stats[1]++; // increment wins
                            stats[2] += game.getPoints(); // increment score
                            lb.put(name, stats);
                            writeLeaderboardFile(lb);

                            inGame = false;
                        } else if (game.getPoints() <= 0) {
                            responseBuilder.setResponseType(Response.ResponseType.LOST)
                                    .setPhrase(newPhrase)
                                    .setMessage("You lost and got 0 points.");
                            inGame = false;
                        } else {
                            responseBuilder.setResponseType(Response.ResponseType.TASK)
                                    .setPhrase(newPhrase)
                                    .setTask(taskMsg)
                                    .setEval(correct);
                        }
                        break;

                    case QUIT:
                        quit = true;
                        responseBuilder.setResponseType(Response.ResponseType.BYE)
                                .setMessage("Good bye " + name + ", thanks for playing.");
                        break;

                    default:
                        sendError(responseBuilder, 2, "Unsupported request.");
                }

                // Send final response
                Response response = responseBuilder.build();
                response.writeDelimitedTo(out);
            }
        } catch (Exception e) {
            System.out.println("Client disconnected or error: " + e.getMessage());
        } finally {
            closeResources();
        }
    }

    private void closeResources() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
        } catch (IOException e) {
            System.out.println("Error closing resources: " + e.getMessage());
        }
    }

    private void sendError(Response.Builder builder, int code, String msg) {
        builder.setResponseType(Response.ResponseType.ERROR)
                .setErrorCode(code)
                .setMessage(msg);
    }

    private synchronized static Map<String, int[]> readLeaderboardFile() {
        Map<String, int[]> leaderboard = new HashMap<>();
        File file = new File(LEADERBOARD_FILE);
        if (!file.exists()) return leaderboard;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String json = reader.lines().reduce("", (a, b) -> a + b);
            JSONObject obj = new JSONObject(json);
            for (String name : obj.keySet()) {
                JSONObject entry = obj.getJSONObject(name);
                int logins = entry.getInt("logins");
                int wins = entry.getInt("wins");
                int score = entry.getInt("score");
                leaderboard.put(name, new int[] { logins, wins, score });
            }
        } catch (Exception e) {
            System.out.println("Failed to read leaderboard: " + e.getMessage());
        }
        return leaderboard;
    }

    private synchronized static void writeLeaderboardFile(Map<String, int[]> leaderboard) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LEADERBOARD_FILE))) {
            JSONObject obj = new JSONObject();
            for (Map.Entry<String, int[]> entry : leaderboard.entrySet()) {
                JSONObject playerObj = new JSONObject();
                playerObj.put("logins", entry.getValue()[0]);
                playerObj.put("wins", entry.getValue()[1]);
                playerObj.put("score", entry.getValue()[2]);
                obj.put(entry.getKey(), playerObj);
            }
            writer.write(obj.toString(2)); // pretty print
        } catch (Exception e) {
            System.out.println("Failed to write leaderboard: " + e.getMessage());
        }
    }

    public static void main (String args[]) throws Exception {
        if (args.length != 1) {
            System.out.println("Expected arguments: <port(int)>");
            System.exit(1);
        }
        int port = 9099; // default port
        Socket clientSocket = null;
        ServerSocket socket = null;

        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException nfe) {
            System.exit(2);
        }
        try {
            socket = new ServerSocket(port);
            System.out.println("Server Started...");
        } catch(Exception e) {
            e.printStackTrace();
            System.exit(2);
        }

        ExecutorService executor = Executors.newFixedThreadPool(MAX_PLAYERS);
        while (true) {
            try{
                System.out.println("Accepting a Client...");
                clientSocket = socket.accept();

                Socket socketToServe = clientSocket;
                executor.submit(() -> {
                    SockBaseServer server = new SockBaseServer(socketToServe, new Game());
                    server.handleRequests();
                });
            }
            catch(Exception e){
                System.out.println("Server encountered an error while connecting to a client.");
            }
            
        }

    }

}