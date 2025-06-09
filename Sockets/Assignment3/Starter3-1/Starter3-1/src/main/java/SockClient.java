import org.json.JSONArray;
import org.json.JSONObject;
import java.net.*;
import java.io.*;
import java.util.Scanner;

/**
 */
class SockClient {
  static Socket sock = null;
  static String host = "localhost";
  static int port = 8888;
  static OutputStream out;
  // Using and Object Stream here and a Data Stream as return. Could both be the same type I just wanted
  // to show the difference. Do not change these types.
  static ObjectOutputStream os;
  static DataInputStream in;

  public static void main (String args[]) {

    if (args.length != 2) {
      System.out.println("Expected arguments: <host(String)> <port(int)>");
      System.exit(1);
    }

    try {
      host = args[0];
      port = Integer.parseInt(args[1]);
    } catch (NumberFormatException nfe) {
      System.out.println("[Port|sleepDelay] must be an integer");
      System.exit(2);
    }

    try {
      connect(host, port); // connecting to server
      System.out.println("Client connected to server.");
      boolean requesting = true;
      while (requesting) {
        System.out.println("What would you like to do: 1 - echo, 2 - add, 3 - addmany, 4 - dad jokes, 5 - temp convert (0 to quit)");
        Scanner scanner = new Scanner(System.in);
        int choice = Integer.parseInt(scanner.nextLine());
        // You can assume the user put in a correct input, you do not need to handle errors here
        // You can assume the user inputs a String when asked and an int when asked. So you do not have to handle user input checking
        JSONObject json = new JSONObject(); // request object
        switch(choice) {
          case 0:
            System.out.println("Choose quit. Thank you for using our services. Goodbye!");
            requesting = false;
            break;
          case 1:
            System.out.println("Choose echo, which String do you want to send?");
            String message = scanner.nextLine();
            json.put("type", "echo");
            json.put("data", message);
            break;
          case 2:
            System.out.println("Choose add, enter first number:");
            String num1 = scanner.nextLine();
            json.put("type", "add");
            json.put("num1", num1);

            System.out.println("Enter second number:");
            String num2 = scanner.nextLine();
            json.put("num2", num2);
            break;
          case 3:
            System.out.println("Choose addmany, enter as many numbers as you like, when done choose 0:");
            JSONArray array = new JSONArray();
            String num = "1";
            while (!num.equals("0")) {
              num = scanner.nextLine();
              array.put(num);
              System.out.println("Got your " + num);
            }
            json.put("type", "addmany");
            json.put("nums", array);
            break;
          case 4:
            System.out.println("Choose dad joke action: 1 - add, 2 - get, 3 - rate");
            int jokeAction = Integer.parseInt(scanner.nextLine());
            json.put("type", "dadjoke");

            switch (jokeAction) {
              case 1:
                json.put("action", "add");
                System.out.println("Enter the joke text:");
                String joke = scanner.nextLine();
                json.put("joke", joke);
                break;
              case 2:
                json.put("action", "get");
                break;
              case 3:
                json.put("action", "rate");
                System.out.println("Enter the jokeID to rate:");
                int jokeID = Integer.parseInt(scanner.nextLine());
                System.out.println("Enter your rating (0-5):");
                String rating = scanner.nextLine();
                json.put("jokeID", jokeID);
                json.put("rating", rating);
                break;
              default:
                System.out.println("Invalid dad joke action.");
                continue;
            }
            break;
          case 5:
            json.put("type", "tempconvert");
            System.out.println("Enter temperature value:");
            double value = Double.parseDouble(scanner.nextLine());
            System.out.println("Convert from unit (C, F, K):");
            String from = scanner.nextLine().toUpperCase();
            System.out.println("Convert to unit (C, F, K):");
            String to = scanner.nextLine().toUpperCase();
            json.put("value", value);
            json.put("fromUnit", from);
            json.put("toUnit", to);
            break;

          default:
            System.out.println("Invalid choice.");
            continue;
        }
        if(!requesting) {
          continue;
        }

        // write the whole message
        os.writeObject(json.toString());
        // make sure it wrote and doesn't get cached in a buffer
        os.flush();

        // handle the response
        // - not doing anything other than printing payload
        String i = (String) in.readUTF();
        JSONObject res = new JSONObject(i);
        System.out.println("Got response: " + res);
        if (res.getBoolean("ok")) {
          String type = res.getString("type");

          switch (type) {
            case "echo":
              System.out.println(res.getString("echo"));
              break;

            case "add":
            case "addmany":
            case "tempconvert":
              System.out.println("Result: " + res.getDouble("result"));
              break;

            case "dadjoke":
              if (res.has("message")) {
                System.out.println(res.getString("message"));
              } else {
                System.out.println("Joke ID: " + res.getInt("jokeID"));
                System.out.println("Joke: " + res.getString("joke"));
                System.out.println("Rating: " + res.getDouble("rating"));
              }
              break;

            default:
              System.out.println("Response received, but type handler is missing: " + res);
          }

        } else {
          System.out.println("Error: " + res.getString("message"));
        }
      }
      // want to keep requesting services so don't close connection
      //overandout();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void overandout() throws IOException {
    //closing things, could
    in.close();
    os.close();
    sock.close(); // close socked after sending
  }

  public static void connect(String host, int port) throws IOException {
    // open the connection
    sock = new Socket(host, port); // connect to host and socket on port 8888

    // get output channel
    out = sock.getOutputStream();

    // create an object output writer (Java only)
    os = new ObjectOutputStream(out);

    in = new DataInputStream(sock.getInputStream());
  }
}