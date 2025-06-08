import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.*;
import java.io.*;
import java.util.*;

/**
 * A class to demonstrate a simple client-server connection using sockets.
 *
 */
public class SockServer {
  static Socket sock;
  static DataOutputStream os;
  static ObjectInputStream in;

  static int port = 8888;

  static Map<Integer, JSONObject> jokes = new HashMap<>();
  static int jokeIdCounter = 1;

  public static void main (String args[]) {

    if (args.length != 1) {
      System.out.println("Expected arguments: <port(int)>");
      System.exit(1);
    }

    String portProp = System.getProperty("port");
    if (portProp != null) {
      try {
        port = Integer.parseInt(portProp);
      } catch (NumberFormatException nfe) {
        System.out.println("[Port|sleepDelay] must be an integer");
        System.exit(1);
      }
    }

    try {
      //open socket
      ServerSocket serv = new ServerSocket(port);
      System.out.println("Server ready for connections");

      /**
       * Simple loop accepting one client and handling one request from that client
       *
       */

      while (true){
        System.out.println("Server waiting for a connection");
        sock = serv.accept(); // blocking wait
        System.out.println("Client connected");

        // setup the object reading channel
        in = new ObjectInputStream(sock.getInputStream());

        // get output channel
        OutputStream out = sock.getOutputStream();

        // create an object output writer (Java only)
        os = new DataOutputStream(out);

        boolean connected = true;
        while (connected) {
          String s = "";
          try {
            s = (String) in.readObject(); // attempt to read string in from client
          } catch (Exception e) { // catch rough disconnect
            System.out.println("Client disconnect");
            connected = false;
            continue;
          }

          // method that checks if the json received is a valid json string
          JSONObject res = isValid(s);

          // if string was not valid we are sending an error message back. 
          if (res.has("ok")) {
            writeOut(res);
            continue;
          }

          // handles invalid JSON structure requests
          JSONObject req;
          try {
            req = new JSONObject(s);
          } catch (JSONException e) {
            JSONObject err = new JSONObject();
            err.put("ok", false);
            err.put("message", "Invalid JSON object structure");
            writeOut(err);
            continue;
          }

          res = testField(req, "type");
          if (!res.getBoolean("ok")) { // no "type" header provided
            res = noType(req);
            writeOut(res);
            continue;
          }

          // route based on type
          try {
            String type = req.getString("type");

            switch (type) {
              case "echo":
                res = echo(req);
                break;
              case "add":
                res = add(req);
                break;
              case "addmany":
                res = addmany(req);
                break;
              case "tempconvert":
                res = temp(req);
                break;
              case "dadjoke":
                res = jokes(req);
                break;
              default:
                res = wrongType(req);
            }
          } catch (JSONException e) {
            res = new JSONObject();
            res.put("ok", false);
            res.put("message", "The 'type' field must be a string.");
          }

          writeOut(res);
        }

        // if we are here - client has disconnected so close connection to socket
        overandout();
      }
    } catch(Exception e) {
      e.printStackTrace();
      overandout(); // close connection to socket upon error
    }
  }

  /**
   * Checks if a specific field exists
   *
   */
  static JSONObject testField(JSONObject req, String key){
    JSONObject res = new JSONObject();

    // field does not exist
    if (!req.has(key)){
      res.put("ok", false);
      res.put("message", "Field " + key + " does not exist in request");
      return res;
    }
    return res.put("ok", true);
  }

  // handles the simple echo request
  static JSONObject echo(JSONObject req){
    System.out.println("Echo request: " + req.toString());
    JSONObject res = testField(req, "data");

    if (!res.getBoolean("ok")) return res;

    if (!req.get("data").getClass().getName().equals("java.lang.String")){
      res.put("ok", false);
      res.put("message", "Field 'data' needs to be of type: String");
      return res;
    }

    res.put("ok", true);
    res.put("type", "echo");
    res.put("echo", "Here is your echo: " + req.getString("data"));
    return res;
  }

  // handles the simple add request with two numbers
  static JSONObject add(JSONObject req){
    System.out.println("Add request: " + req.toString());
    JSONObject res1 = testField(req, "num1");
    if (!res1.getBoolean("ok")) {
      return res1;
    }

    JSONObject res2 = testField(req, "num2");
    if (!res2.getBoolean("ok")) {
      return res2;
    }

    JSONObject res = new JSONObject();
    try {
      int n1 = req.getInt("num1");
      int n2 = req.getInt("num2");
      res.put("ok", true);
      res.put("type", "add");
      res.put("result", n1 + n2);
    } catch (JSONException e) {
      res.put("ok", false);
      res.put("message", "Fields 'num1' and 'num2' must be integers.");
    }

    return res;
  }

  // implement me in assignment 3 you can of course re-name these as well. 
  static JSONObject temp(JSONObject req) {
    JSONObject res = new JSONObject();
    String[] validUnits = {"C", "F", "K"};

    for (String field : new String[]{"value", "fromUnit", "toUnit"}) {
      JSONObject check = testField(req, field);
      if (!check.getBoolean("ok")) return check;
    }

    try {
      double value = req.getDouble("value");
      String from = req.getString("fromUnit");
      String to = req.getString("toUnit");

      if (!Arrays.asList(validUnits).contains(from)
          || !Arrays.asList(validUnits).contains(to)) {
        res.put("ok", false);
        res.put("message", "Field 'fromUnit' and 'toUnit' does not exist in request");
        res.put("type", "tempconvert");
        return res;
      }

      double celsius;
      switch (from) {
        case "C":
          celsius = value;
          break;
        case "F":
          celsius = (value - 32) * 5 / 9;
          break;
        case "K":
          celsius = value - 273.15;
          break;
        default:
          throw new JSONException("Invalid fromUnit: " + from);
      }

      if (celsius < -20 || celsius > 400) {
        res.put("ok", false);
        res.put("message", "Value out of bound. Must be between -20 and 400");
        res.put("type", "tempconvert");
        return res;
      }

      double result;
      switch (to) {
        case "C":
          result = celsius;
          break;
        case "F":
          result = celsius * 9 / 5 + 32;
          break;
        case "K":
          result = celsius + 273.15;
          break;
        default:
          throw new JSONException("Invalid toUnit: " + to);
      }

      res.put("ok", true);
      res.put("type", "tempconvert");
      res.put("result", Math.round(result * 100.0) / 100.0);
      return res;
    } catch (JSONException e) {
      res.put("ok", false);
      res.put("message", "Field 'toUnit' and 'fromUnit' does not exist in request");
      res.put("type", "tempconvert");
      return res;
    }
  }

 // implement me in assignment 3 you can of course re-name these as well. 
  static JSONObject jokes(JSONObject req) {
    JSONObject res = new JSONObject();
    String action = req.optString("action", "");

    switch (action) {
      case "add":
        JSONObject jokeCheck = testField(req, "joke");
        if (!jokeCheck.getBoolean("ok")) return jokeCheck;

        String joke = req.getString("joke");
        JSONObject jokeObj = new JSONObject();
        jokeObj.put("joke", joke);
        jokeObj.put("rating", 5.0);
        jokes.put(jokeIdCounter, jokeObj);

        res.put("ok", true);
        res.put("type", "dadjoke");
        res.put("message", "Joke added, this is joke number " + jokeIdCounter + "!");
        jokeIdCounter++;
        return res;

      case "get":
        if (jokes.isEmpty()) {
          res.put("ok", false);
          res.put("type", "dadjoke");
          res.put("message", "No jokes on server");
          return res;
        }
        int nextId = jokes.keySet().stream().min(Integer::compareTo).orElse(1);
        JSONObject jokeEntry = jokes.get(nextId);
        res.put("ok", true);
        res.put("type", "dadjoke");
        res.put("jokeID", nextId);
        res.put("joke", jokeEntry.getString("joke"));
        res.put("rating", jokeEntry.getDouble("rating"));
        return res;

      case "rate":
        JSONObject jokeIdCheck = testField(req, "jokeID");
        if (!jokeIdCheck.getBoolean("ok")) return jokeIdCheck;

        JSONObject ratingCheck = testField(req, "rating");
        if (!ratingCheck.getBoolean("ok")) return ratingCheck;

        try {
          int jokeID = req.getInt("jokeID");
          double newRating = Double.parseDouble(req.getString("rating"));

          if (newRating < 0 || newRating > 5) {
            res.put("ok", false);
            res.put("type", "dadjoke");
            res.put("message", "Rating is out of bounds");
            return res;
          }

          if (!jokes.containsKey(jokeID)) {
            res.put("ok", false);
            res.put("type", "dadjoke");
            res.put("message", "Invalid jokeID");
            return res;
          }

          JSONObject jokeData = jokes.get(jokeID);
          double oldRating = jokeData.getDouble("rating");
          double avgRating = (oldRating + newRating) / 2;

          jokeData.put("rating", avgRating);

          res.put("ok", true);
          res.put("type", "dadjoke");
          res.put("jokeID", jokeID);
          res.put("joke", jokeData.getString("joke"));
          res.put("rating", Math.round(avgRating * 100.0) / 100.0);
          return res;

        } catch (Exception e) {
          res.put("ok", false);
          res.put("type", "dadjoke");
          res.put("message", "Invalid rating or jokeID format");
          return res;
        }

      default:
        res.put("ok", false);
        res.put("type", "dadjoke");
        res.put("message", "Unsupported dadjoke action");
        return res;
    }
  }

  // handles the simple addmany request
  static JSONObject addmany(JSONObject req){
    System.out.println("Add many request: " + req.toString());
    JSONObject res = testField(req, "nums");
    if (!res.getBoolean("ok")) {
      return res;
    }

    int result = 0;
    try {
      JSONArray array = req.getJSONArray("nums");
      for (int i = 0; i < array.length(); i++) {
        result += array.getInt(i);
      }
    } catch (JSONException e) {
      res.put("ok", false);
      res.put("message", "Values in array need to be ints");
      return res;
    }

    res.put("ok", true);
    res.put("type", "addmany");
    res.put("result", result);
    return res;
  }

  // creates the error message for wrong type
  static JSONObject wrongType(JSONObject req){
    System.out.println("Wrong type request: " + req.toString());
    JSONObject res = new JSONObject();
    res.put("ok", false);
    res.put("message", "Type " + req.getString("type") + " is not supported.");
    return res;
  }

  // creates the error message for no given type
  static JSONObject noType(JSONObject req){
    System.out.println("No type request: " + req.toString());
    JSONObject res = new JSONObject();
    res.put("ok", false);
    res.put("message", "No request type was given.");
    return res;
  }

  // From: https://www.baeldung.com/java-validate-json-string
  public static JSONObject isValid(String json) {
    try {
      new JSONObject(json);
    } catch (JSONException e) {
      try {
        new JSONArray(json);
      } catch (JSONException ne) {
        JSONObject res = new JSONObject();
        res.put("ok", false);
        res.put("message", "req not JSON");
        return res;
      }
    }
    return new JSONObject();
  }

  // sends the response and closes the connection between client and server.
  static void overandout() {
    try {
      os.close();
      in.close();
      sock.close();
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  // sends the response and closes the connection between client and server.
  static void writeOut(JSONObject res) {
    try {
      os.writeUTF(res.toString());
      // make sure it wrote and doesn't get cached in a buffer
      os.flush();
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
}