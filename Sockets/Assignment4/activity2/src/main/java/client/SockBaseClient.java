package client;

import java.net.*;
import java.io.*;
import java.util.*;

import proto.RequestProtos.*;
import proto.ResponseProtos.*;

class SockBaseClient {

    public static void main (String args[]) throws Exception {
        Socket serverSock = null;
        OutputStream out = null;
        InputStream in = null;
        int i1=0, i2=0;
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
            String strToSend = stdin.readLine();
            clientName = strToSend;

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
                            System.out.println("\n--- Leaderboard ---");
                            for (Leader l : leaderboardRes.getLeaderboardList()) {
                                System.out.println(
                                    l.getName() + " - Wins: " + l.getWins() +
                                    ", Logins: " + l.getLogins() +
                                    ", Points: " + l.getPoints()
                                );
                            }
                            System.out.println("-------------------\n");
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

                                if (guess.equalsIgnoreCase("exit")) {
                                    Request quitReq = Request.newBuilder()
                                            .setOperationType(Request.OperationType.QUIT)
                                            .build();
                                    quitReq.writeDelimitedTo(out);
                                    Response quitRes = Response.parseDelimitedFrom(in);
                                    System.out.println(quitRes.getMessage());
                                    System.exit(0);
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


