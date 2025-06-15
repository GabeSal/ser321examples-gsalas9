package taskone;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.InputMismatchException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

/**
 * ThreadPoolServer - limits number of concurrent clients via fixed thread pool.
 */
public class ThreadPoolServer {
    private static Performer performer;

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Usage: gradle runThreadPoolServer -Pport=9099 -PmaxClients=5 -q --console=plain");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        int maxClients = Integer.parseInt(args[1]);

        ServerSocket server = new ServerSocket(port);
        ExecutorService pool = Executors.newFixedThreadPool(maxClients);
        System.out.println("ThreadPoolServer started on port " + port + " with max clients: " + maxClients);

        StringList sharedStrings = new StringList(); // thread-safe shared state
        performer = new Performer(sharedStrings);

        AtomicInteger activeClients = new AtomicInteger(0);

        while (true) {
            Socket clientSocket = server.accept();
            System.out.println("Accepted client: " + clientSocket.getRemoteSocketAddress());

            // Check if we can serve more clients
            if (activeClients.get() >= maxClients) {
                System.out.println("Connection rejected (max clients reached): " + clientSocket.getRemoteSocketAddress());
                sendRejectMessage(clientSocket);
                continue; // Do not submit to pool
            }
            activeClients.incrementAndGet();
            pool.execute(() -> {
                handleClient(clientSocket);
                activeClients.decrementAndGet(); // cleanup
            });
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (
            OutputStream out = clientSocket.getOutputStream();
            InputStream in = clientSocket.getInputStream()
        ) {
            // Send a welcome message immediately
            JSONObject welcome = new JSONObject();
            welcome.put("type", "info");
            welcome.put("message", "Connection accepted. Welcome!");
            NetworkUtils.send(out, JsonUtils.toByteArray(welcome));

            boolean quit = false;
            while (!quit) {
                byte[] messageBytes = NetworkUtils.receive(in);
                JSONObject message = JsonUtils.fromByteArray(messageBytes);

                int choice = message.getInt("selected");
                Object data = message.opt("data");
                JSONObject request = new JSONObject();

                switch (choice) {
                    case 1:
                        request.put("operation", "add");
                        request.put("data", data);
                        break;
                    case 2:
                        request.put("operation", "display");
                        request.put("data", JSONObject.NULL);
                        break;
                    case 3:
                        request.put("operation", "search");
                        request.put("data", data);
                        break;
                    case 4:
                        request.put("operation", "reverse");
                        request.put("data", data);
                        break;
                    case 0:
                        request.put("operation", "quit");
                        request.put("data", JSONObject.NULL);
                        quit = true;
                        break;
                    default:
                        JSONObject error = new JSONObject();
                        error.put("type", "error");
                        error.put("message", "Invalid selection: " + choice);
                        NetworkUtils.send(out, JsonUtils.toByteArray(error));
                        continue;
                }

                String result = performer.handleRequest(request.toMap());
                JSONObject response = new JSONObject(result);
                NetworkUtils.send(out, JsonUtils.toByteArray(response));
            }

            System.out.println("Client disconnected: " + clientSocket.getRemoteSocketAddress());
            clientSocket.close();
        } catch (IOException | InputMismatchException e) {
            System.out.println("Error handling client: " + e.getMessage());
        }
    }

    // Method to send rejection JSON and close connection
    private static void sendRejectMessage(Socket socket) {
        try (
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream()
        ) {
            JSONObject error = new JSONObject();
            error.put("type", "error");
            error.put("message", "Connection rejected: Max clients reached. Try again later.");
            NetworkUtils.send(out, JsonUtils.toByteArray(error));
            socket.close();
        } catch (IOException e) {
            System.out.println("Failed to send rejection message.");
        }
    }
}
