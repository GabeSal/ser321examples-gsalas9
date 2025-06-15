/**
  File: Server.java
  Author: Student in Fall 2020B
  Description: Server class in package taskone.
*/

package taskone;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import org.json.JSONObject;

/**
 * Class: Server
 * Description: Server tasks.
 */
class Server {
    static Socket conn;
    static Performer performer;

    public static void main(String[] args) throws Exception {
        int port;
        StringList strings = new StringList();
        performer = new Performer(strings);

        if (args.length != 1) {
            // gradle runServer -Pport=9099 -q --console=plain
            System.out.println("Usage: gradle runServer -Pport=9099 -q --console=plain");
            System.exit(1);
        }
        port = -1;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port] must be an integer");
            System.exit(2);
        }
        ServerSocket server = new ServerSocket(port);
        System.out.println("Server Started...");

        while (true) {
            System.out.println("Accepting a Request...");
            conn = server.accept();
            doPerform();
        }
    }

    public static void doPerform() {
        boolean quit = false;
        try (
                OutputStream out = conn.getOutputStream();
                InputStream in = conn.getInputStream()
        ) {
            System.out.println("Server connected to client:");
            while (!quit) {
                byte[] messageBytes = NetworkUtils.receive(in);
                JSONObject message = JsonUtils.fromByteArray(messageBytes);

                int choice = message.getInt("selected");
                Object data = message.opt("data");
                JSONObject request = new JSONObject();

                // Translate numeric menu choice to structured Performer-compatible protocol
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
                        error.put("message", "Invalid selection: " + choice + " is not an option");
                        NetworkUtils.send(out, JsonUtils.toByteArray(error));
                        continue;
                }

                // Call Performer logic and send result
                String result = performer.handleRequest(request.toMap());
                JSONObject returnMessage = new JSONObject(result);
                NetworkUtils.send(out, JsonUtils.toByteArray(returnMessage));
            }

            System.out.println("close the resources of client ");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
