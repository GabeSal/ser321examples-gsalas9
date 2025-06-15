package taskone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.InputMismatchException;
import java.util.List;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import org.json.JSONObject;

/**
 * ThreadedServer - allows unbounded concurrent clients.
 */
public class ThreadedServer {
    private static Performer performer;

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Usage: gradle runThreadedServer -Pport=9099 -q --console=plain");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        ServerSocket server = new ServerSocket(port);
        System.out.println("ThreadedServer started on port " + port);

        StringList sharedStrings = new StringList(); // Shared thread-safe instance
        performer = new Performer(sharedStrings);

        while (true) {
            Socket clientSocket = server.accept();
            System.out.println("Accepted client: " + clientSocket.getRemoteSocketAddress());
            new Thread(() -> handleClient(clientSocket)).start();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (
                OutputStream out = clientSocket.getOutputStream();
                InputStream in = clientSocket.getInputStream()
        ) {
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
}
