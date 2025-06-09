package Assign32starter;
import java.net.*;
import java.util.Random;
import java.io.*;
import org.json.*;


/**
 * A class to demonstrate a simple client-server connection using sockets.
 * Ser321 Foundations of Distributed Software Systems
 */
public class SockServer {
	static Riddle[] riddles = new Riddle[5]; // stores all riddles

	public static void main (String args[]) {
		if (args.length != 1) {
			System.out.println("Expected arguments: <port(int)>");
			System.exit(1);
		}

		Socket client;
		try {
			//setting some riddles here, you can add more, change them, store them in a different way
			riddles[0] = new Riddle("I dry as I get wetter.", "Towel");
			riddles[1] = new Riddle("The building that has the most stories.  ", "Library");
			riddles[2] = new Riddle("The pot called me black. I said “look who’s talking?!” Then, I made some tea.", "Kettle");
			riddles[3] = new Riddle("Seeing double? Check me to spot your doppelganger.", "Mirror");
			riddles[4] = new Riddle("I have eyes but cannot see.", "Potato");

			int port = Integer.parseInt(args[0]);
			//opening the socket here, just hard coded since this is just a bas example
			ServerSocket serv = new ServerSocket(port);
			System.out.println("Server ready for connection");

			// placeholder for the person who wants to play a game
			String name = "";
			int points = 0;

			// read in one object, the message. we know a string was written only by knowing what the client sent. 
			// must cast the object from Object to desired type to be useful
			while(true) {
				client = serv.accept(); // blocking wait
				System.out.println("Client connected");

				// Text-based streams
				BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
				PrintWriter out = new PrintWriter(client.getOutputStream(), true);

				String line = in.readLine();
				if (line == null) continue;

				JSONObject request = new JSONObject(line); // the requests that is received
				JSONObject response = new JSONObject();

				String type = request.optString("type", "");

				// Available actions
				switch (type) {
					case "start":
						response.put("type", "hello");
						name = request.optString("name", "Player");
						response.put("value", "Hello " + name + ", welcome to the game!");
						sendImg("img/hi.png", response);
						break;

					case "play":
						JSONObject riddleJson = serveRandomRiddle();
						response.put("type", "play");
						response.put("riddle", riddleJson.getString("riddle"));
						sendImg("img/play.jpg", response);
						break;

					case "leaderboard":
						response.put("type", "leaderboard");
						response.put("entries", new JSONArray()  // placeholder values
								.put(new JSONObject().put("name", "Alice").put("score", 3))
								.put(new JSONObject().put("name", "Bob").put("score", 2)));
						break;

					case "addRiddle":
						String newRiddle = request.optString("riddle");
						String newAnswer = request.optString("answer");
						// Here you'd add to your riddles[] or dynamic list
						response.put("type", "addRiddle");
						response.put("message", "Riddle added: " + newRiddle);
						break;

					case "vote":
						response.put("type", "vote");
						response.put("message", "Vote received!");
						break;

					case "quit":
						response.put("type", "quit");
						response.put("message", "Thanks for playing!");
						break;

					default:
						response.put("type", "error");
						response.put("message", "Unknown request type.");
						break;
				}

				out.println(response.toString());
				in.close();
				out.close();
				client.close();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private static JSONObject serveRandomRiddle() {
		Random rand = new Random();
		Riddle current = riddles[rand.nextInt(riddles.length)];

		JSONObject obj = new JSONObject();
		obj.put("riddle", current.getRiddle());
		obj.put("answer", current.getAnswer());  // Optional: useful for validation later
		return obj;
	}

	public static JSONObject sendImg(String filename, JSONObject obj) throws Exception {
		File file = new File(filename);

		if (file.exists()) {
			// import image
			// I did not use the Advanced Custom protocol
			// I read in the image and translated it into basically into a string and send it back to the client where I then decoded again
			//obj.put("image", "Pretend I am this image: " + filename);
		} 
		return obj;
	}
}
