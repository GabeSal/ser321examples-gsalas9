package client;

import java.net.*;
import java.io.*;
import java.util.*;

import proto.RequestProtos.*;
import proto.ResponseProtos.*;

class SockBaseClient {

    private static String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        String pad = " ".repeat(Math.max(0, padding));
        return pad + text + pad + (text.length() % 2 != 0 ? " " : "");
    }

    public static void main (String args[]) throws Exception {
        Socket serverSock = null;
        OutputStream out = null;
        InputStream in = null;
        int port = 9099; // default port
        String clientName = "";

        // Make sure two arguments are given
        if (args.length != 2) {
            System.out.println("Expected arguments: <host(String)> <port(int)>");
            System.exit(1);
        }

        String host = args[0];
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port] must be integer");
            System.exit(2);
        }

        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

        try {
            // ==== CONNECT TO SERVER ====
            serverSock = new Socket(host, port);
            out = serverSock.getOutputStream();
            in = serverSock.getInputStream();

            // ==== NAME / LOGIN PHASE ====
            System.out.println("Please provide your name for the server. :-)");
            
            // Name input + validation loop
            while (true) {
                String inputName = stdin.readLine().trim();

                // Name must match regex: starts with letter, 4+ characters, no special chars
                if (inputName.matches("^[A-Za-z][A-Za-z0-9]{3,}$")) {
                    clientName = inputName;
                    break;
                } else {
                    System.out.println("Invalid name. Names must:");
                    System.out.println("- Start with a letter (A-Z or a-z)");
                    System.out.println("- Be at least 4 characters long");
                    System.out.println("- Only contain letters and numbers (no special characters)");
                    System.out.print("Try again: ");
                }
            }

            Request login = Request.newBuilder()
                    .setOperationType(Request.OperationType.NAME)
                    .setName(clientName)
                    .build();
            login.writeDelimitedTo(out);

            Response welcome = Response.parseDelimitedFrom(in);
            System.out.println(welcome.getHello());

            boolean quit = false;
            while (!quit) {
                System.out.println("What would you like to do?");
                System.out.println("1 - View Leaderboard");
                System.out.println("2 - Start Game");
                System.out.println("3 - Quit");
                System.out.print("> ");
                String choice = stdin.readLine();

                switch (choice) {
                    // Leaderboard
                    case "1":
                        Request leaderboardReq = Request.newBuilder()
                                .setOperationType(Request.OperationType.LEADERBOARD)
                                .build();
                        leaderboardReq.writeDelimitedTo(out);
                        Response leaderboardRes = Response.parseDelimitedFrom(in);
                        if (leaderboardRes.getResponseType() == Response.ResponseType.LEADERBOARD) {
                            List<Leader> leaders = leaderboardRes.getLeaderboardList();
                            int rank = 1;

                            // Determine dynamic max lengths for all columns
                            int maxRankLen = String.valueOf(leaders.size()).length();
                            int maxNameLen = "Name".length();
                            int maxLoginLen = "Logins".length();
                            int maxWinsLen = "Wins".length();
                            int maxScoreLen = "Score".length();
                            for (Leader l : leaders) {
                                maxNameLen = Math.max(maxNameLen, l.getName().length());
                                maxLoginLen = Math.max(maxLoginLen, String.valueOf(l.getLogins()).length());
                                maxWinsLen = Math.max(maxWinsLen, String.valueOf(l.getWins()).length());
                                maxScoreLen = Math.max(maxScoreLen, String.valueOf(l.getPoints()).length());
                            }

                            // Build horizontal line dynamically
                            int tableWidth = 6 + maxRankLen + maxNameLen + maxLoginLen + maxWinsLen + maxScoreLen + 20;
                            String horizontalLine = "=".repeat(tableWidth);

                            System.out.println("\n" + horizontalLine);
                            System.out.println(centerText("LEADERBOARD", tableWidth));
                            System.out.println(horizontalLine);

                            // Table header
                            System.out.printf("| %" + maxRankLen + "s | %-" + maxNameLen + "s | %" + maxLoginLen + "s | %" + maxWinsLen + "s | %" + maxScoreLen + "s |\n",
                                    "#", "Name", "Logins", "Wins", "Score");
                            System.out.println("-".repeat(tableWidth));

                            for (Leader l : leaders) {
                                System.out.printf("| %" + maxRankLen + "d | %-" + maxNameLen + "s | %" + maxLoginLen + "d | %" + maxWinsLen + "d | %" + maxScoreLen + "d |\n",
                                        rank++, l.getName(), l.getLogins(), l.getWins(), l.getPoints());
                            }

                            System.out.println(horizontalLine + "\n");
                        }
                        break;
                    // Game start
                    case "2":
                        Request startGame = Request.newBuilder()
                                .setOperationType(Request.OperationType.START)
                                .build();
                        startGame.writeDelimitedTo(out);
                        Response gameStart = Response.parseDelimitedFrom(in);

                        if (gameStart.getResponseType() == Response.ResponseType.TASK) {
                            boolean playing = true;
                            String phrase = gameStart.getPhrase();
                            System.out.println("\nPhrase: " + phrase);
                            System.out.println(gameStart.getTask());

                            while (playing) {
                                System.out.print("Enter your guess (or 'exit' to quit): ");
                                String guess = stdin.readLine();

                                // Returns to main menu when "exit" is entered
                                if (guess.equalsIgnoreCase("exit")) {
                                    Request quitReq = Request.newBuilder()
                                            .setOperationType(Request.OperationType.MAIN_MENU)
                                            .build();
                                    quitReq.writeDelimitedTo(out);
                                    Response menuRes = Response.parseDelimitedFrom(in);

                                    if (menuRes.getResponseType() == Response.ResponseType.TASK) {
                                        System.out.println("Returned to main menu.");
                                    } else {
                                        System.out.println("Unexpected response when returning to menu.");
                                    }

                                    playing = false;
                                    continue;
                                }

                                Request guessReq = Request.newBuilder()
                                        .setOperationType(Request.OperationType.GUESS)
                                        .setGuess(guess)
                                        .build();
                                guessReq.writeDelimitedTo(out);
                                Response gameRes = Response.parseDelimitedFrom(in);

                                switch (gameRes.getResponseType()) {
                                    case TASK:
                                        System.out.println("Phrase: " + gameRes.getPhrase());
                                        System.out.println(gameRes.getTask());
                                        break;
                                    case WON:
                                        System.out.println("You won!");
                                        System.out.println("Phrase: " + gameRes.getPhrase());
                                        System.out.println(gameRes.getMessage());
                                        playing = false;
                                        break;
                                    case LOST:
                                        System.out.println("You lost.");
                                        System.out.println("Phrase: " + gameRes.getPhrase());
                                        System.out.println(gameRes.getMessage());
                                        playing = false;
                                        break;
                                    case ERROR:
                                        System.out.println("Error (" + gameRes.getErrorCode() + "): " + gameRes.getMessage());
                                        break;
                                    default:
                                        System.out.println("Unexpected response.");
                                        playing = false;
                                        break;
                                }
                            }
                        }
                        break;
                    // Quit app
                    case "3":
                        Request quitReq = Request.newBuilder()
                                .setOperationType(Request.OperationType.QUIT)
                                .build();
                        quitReq.writeDelimitedTo(out);
                        Response quitRes = Response.parseDelimitedFrom(in);
                        System.out.println(quitRes.getMessage());
                        quit = true;
                        break;

                    default:
                        System.out.println("Invalid option. Try again.");
                        break;
                }
            }
        } catch (SocketException se) {
            System.out.println("Server closed the connection.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResources(in, out, serverSock);
        }
    }

    private static void closeResources(InputStream in, OutputStream out, Socket serverSock) throws IOException {
        if (in != null) in.close();
        if (out != null) out.close();
        if (serverSock != null) serverSock.close();
    }
}


