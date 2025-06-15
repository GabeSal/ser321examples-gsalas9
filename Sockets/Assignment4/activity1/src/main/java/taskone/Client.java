/**
  File: Client.java
  Author: Student in Fall 2020B
  Description: Client class in package taskone.
*/

package taskone;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Base64;
import java.util.InputMismatchException;
import java.util.Scanner;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.json.JSONObject;

/**
 * Class: Client
 * Description: Client tasks.
 */
public class Client {
    private static BufferedReader stdin;

    /**
     * Function JSONObject add().
     */
    public static JSONObject add() {
        String strToSend = null;
        JSONObject request = new JSONObject();
        request.put("selected", 1);
        try {
            System.out.print("Please input the string: ");
            strToSend = stdin.readLine();
            request.put("data", strToSend);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return request;
    }

    /**
     * Function JSONObject display().
     */
    public static JSONObject display() {
        JSONObject request = new JSONObject();
        request.put("selected", 2);
        request.put("data", JSONObject.NULL);
        return request;
    }

    /**
     * Function JSONObject count().
     */
    public static JSONObject count() {
        JSONObject request = new JSONObject();
        request.put("selected", 3);
        try {
            System.out.print("Enter string to search: ");
            String input = stdin.readLine();
            request.put("data", input);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return request;
    }

    public static JSONObject reverse() {
        JSONObject request = new JSONObject();
        request.put("selected", 4);
        try {
            System.out.print("Enter index to reverse: ");
            int idx = Integer.parseInt(stdin.readLine());
            request.put("data", idx);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return request;
    }

    /**
     * Function JSONObject quit().
     */
    public static JSONObject quit() {
        JSONObject request = new JSONObject();
        request.put("selected", 0);
        request.put("data", ".");
        return request;
    }

    /**
     * Function main().
     */
    public static void main(String[] args) throws IOException {
        String host;
        int port;
        Socket sock;
        stdin = new BufferedReader(new InputStreamReader(System.in));

        try {
            if (args.length != 2) {
                // gradle runClient -Phost=localhost -Pport=9099 -q --console=plain
                System.out.println("Usage: gradle Client -Phost=localhost -Pport=9099");
                System.exit(0);
            }

            host = args[0];
            port = -1;
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException nfe) {
                System.out.println("[Port] must be an integer");
                System.exit(2);
            }

            sock = new Socket(host, port);
            OutputStream out = sock.getOutputStream();
            InputStream in = sock.getInputStream();
            Scanner input = new Scanner(System.in);

            // Receive initial response from server
            byte[] initialBytes = NetworkUtils.receive(in);
            JSONObject initialResponse = JsonUtils.fromByteArray(initialBytes);

            // Check type and act accordingly
            String type = initialResponse.optString("type", "");
            if ("error".equals(type)) {
                System.out.println("Server error:\n" + initialResponse.optString("message", "Unknown error."));
                sock.close();
                System.exit(0);
            } else if ("info".equals(type)) {
                System.out.println("Server:\n" + initialResponse.optString("message", "Connected."));
            }

            int choice;
            do {
                System.out.println();
                System.out.println("Client Menu");
                System.out.println("Please select a valid option (1-5). 0 to disconnect the client");
                System.out.println("1. add <string> - adds a string to the list and display it");
                System.out.println("2. display - display the list");
                System.out.println("3. search <string> - returns index of element");
                System.out.println("4. reverse <index> - reverse string at index");
                System.out.println("0. quit");
                System.out.println();
                choice = input.nextInt();

                JSONObject request = null;
                switch (choice) {
                    case 1:
                        request = add();
                        break;
                    case 2:
                        request = display();
                        break;
                    case 3:
                        request = count(); // search functionality
                        break;
                    case 4:
                        request = reverse();
                        break;
                    case 0:
                        request = quit();
                        break;
                    default:
                        System.out.println("Invalid option.");
                        break;
                }

                if (request != null) {
                    System.out.println(request);
                    NetworkUtils.send(out, JsonUtils.toByteArray(request));
                    byte[] responseBytes = NetworkUtils.receive(in);
                    JSONObject response = JsonUtils.fromByteArray(responseBytes);

                    if (response.has("type") && response.getString("type").equals("error")) {
                        System.out.println("Error: " + response.getString("message"));
                    } else {
                        System.out.println();
                        System.out.println("Response from server:");
                        System.out.println("Operation: " + response.getString("operation"));
                        System.out.println("Data: " + response.get("data"));
                    }

                    if (choice == 0) {
                        sock.close();
                        out.close();
                        in.close();
                        System.exit(0);
                    }
                }
            } while (true);
        } catch (IOException | InputMismatchException e) {
            e.printStackTrace();
        }
    }
}