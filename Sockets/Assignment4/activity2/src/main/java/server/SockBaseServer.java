package server;

import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.*;

import proto.RequestProtos.*;
import proto.ResponseProtos.*;

class SockBaseServer {
    static String logFilename = "logs.txt";

    ServerSocket socket = null;
    InputStream in = null;
    OutputStream out = null;
    Socket clientSocket = null;
    int port = 9099; // default port
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
        int correctGuesses = 0;
        int maxWrongGuesses = 6;
        int wrongGuesses = 0;

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
                        writeToLog(name, Message.CONNECT);
                        responseBuilder.setResponseType(Response.ResponseType.WELCOME)
                                .setHello("Hello " + name + ", welcome to the guessing game!");
                        break;

                    case LEADERBOARD:
                        responseBuilder.setResponseType(Response.ResponseType.LEADERBOARD);
                        Logs logs = readLogFile().build();
                        Map<String, Integer> wins = new HashMap<>();
                        Map<String, Integer> logins = new HashMap<>();

                        for (String line : logs.getLogList()) {
                            String[] parts = line.split(" - ");
                            String player = parts[0].split(": ")[1].split(" ")[0];
                            String action = parts[1];

                            if (action.equals("CONNECT")) {
                                logins.put(player, logins.getOrDefault(player, 0) + 1);
                            } else if (action.equals("WIN")) {
                                wins.put(player, wins.getOrDefault(player, 0) + 1);
                            }
                        }

                        for (String player : logins.keySet()) {
                            Leader l = Leader.newBuilder()
                                    .setName(player)
                                    .setLogins(logins.get(player))
                                    .setWins(wins.getOrDefault(player, 0))
                                    .build();
                            responseBuilder.addLeaderboard(l);
                        }

                        break;

                    case START:
                        inGame = true;
                        correctGuesses = 0;
                        wrongGuesses = 0;

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
                        if (!correct) wrongGuesses++;

                        if (!newPhrase.contains("_")) {
                            writeScoreToLog(name, game.getPoints());
                            responseBuilder.setResponseType(Response.ResponseType.WON)
                                    .setPhrase(newPhrase)
                                    .setMessage("Congratulations! You won with " + game.getPoints() + " points!");
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

    /**
     * Writing a new entry to our log
     * @param name - Name of the person logging in
     * @param message - type Message from Protobuf which is the message to be written in the log (e.g. Connect) 
     * @return String of the new hidden image
     */
    public static synchronized void writeToLog(String name, Message message) {
        try {
            // read old log file
            Logs.Builder logs = readLogFile();

            // get current time and data
            Date date = java.util.Calendar.getInstance().getTime();

            // we are writing a new log entry to our log
            // add a new log entry to the log list of the Protobuf object
            logs.addLog(date.toString() + ": " + name + " - " + message);

            // open log file
            FileOutputStream output = new FileOutputStream(logFilename);
            Logs logsObj = logs.build();

            // This is only to show how you can iterate through a Logs object which is a protobuf object
            // which has a repeated field "log"
            for (String log : logsObj.getLogList()) {
                System.out.println(log);
            }

            // write to log file
            logsObj.writeTo(output);
        } catch (Exception e) {
            System.out.println("Issue while trying to save");
        }
    }

    public static synchronized void writeScoreToLog(String name, int score) {
        try {
            Logs.Builder logs = readLogFile();
            List<String> updatedLogs = new ArrayList<>();

            // Remove lower scores for this player
            for (String entry : logs.getLogList()) {
                if (entry.contains(" - WIN")) {
                    String[] split = entry.split(" - ");
                    String entryName = split[0].split(": ")[1].split(" ")[0];
                    int oldScore = Integer.parseInt(split[1].split(" ")[2]);

                    if (entryName.equals(name) && oldScore >= score) {
                        return; // Don't update
                    }
                }
            }

            Date date = Calendar.getInstance().getTime();
            logs.addLog(date.toString() + ": " + name + " - WIN " + score);

            FileOutputStream output = new FileOutputStream(logFilename);
            logs.build().writeTo(output);
        } catch (Exception e) {
            System.out.println("Issue writing score to log");
        }
    }

    /**
     * Reading the current log file
     * @return Logs.Builder a builder of a logs entry from protobuf
     */
    public static Logs.Builder readLogFile() throws Exception{
        Logs.Builder logs = Logs.newBuilder();

        try {
            // just read the file and put what is in it into the logs object
            return logs.mergeFrom(new FileInputStream(logFilename));
        } catch (FileNotFoundException e) {
            System.out.println(logFilename + ": File not found.  Creating a new file.");
            return logs;
        }
    }

    public static void main (String args[]) throws Exception {
        if (args.length != 2) {
            System.out.println("Expected arguments: <port(int)> <delay(int)>");
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

        while (true) {
            try{
                System.out.println("Accepting a Client...");
                clientSocket = socket.accept();
                SockBaseServer server = new SockBaseServer(clientSocket, new Game());
                server.handleRequests();
            }
            catch(Exception e){
                System.out.println("Server encountered an error while connecting to a client.");
            }
            
        }

    }

}