package Assign32starter;
import java.net.*;
import java.nio.file.Files;
import java.util.*;
import java.io.*;
import org.json.*;


/**
 * A class to demonstrate a simple client-server connection using sockets.
 * Ser321 Foundations of Distributed Software Systems
 */
public class SockServer {
	static List<Riddle> riddles = new ArrayList<>(); // stores all riddles
	static Queue<Riddle> pendingRiddles = new LinkedList<>() {
	}; // riddles pending submission to the game
	static final String RIDDLE_DIR = "riddles/";
	static final String RIDDLE_FILE = RIDDLE_DIR + "riddles.json";
	static final String PENDING_FILE = RIDDLE_DIR + "pending_riddles.json";

	public static void main (String args[]) {
		if (args.length != 1) {
			System.out.println("Expected arguments: <port(int)>");
			System.exit(1);
		}

		ensureDirectoryExists();
		loadRiddles();
		loadPendingRiddles();

		Socket client;
		try {

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
						String newRiddle = request.optString("riddle", "");
						String newAnswer = request.optString("answer", "");

						if (!newRiddle.isEmpty() && !newAnswer.isEmpty()) {
							pendingRiddles.add(new Riddle(newRiddle, newAnswer));
							savePendingRiddles();
							response.put("type", "addRiddle");
							response.put("message", "New riddle added to pending list!");
							//sendImg("img/add.png", response);
						} else {
							response.put("type", "error");
							response.put("message", "Riddle or answer was missing.");
						}
						break;

					case "getVoteRiddle":
						if (!pendingRiddles.isEmpty()) {
							Riddle candidate = pendingRiddles.peek();
							response.put("type", "vote");
							response.put("riddle", candidate.getRiddle());
							response.put("answer", candidate.getAnswer());
							//sendImg("img/vote.png", response);
						} else {
							response.put("message", "No riddles available for voting.");
						}
						break;

					case "vote":
						String vote = request.optString("vote", "").toLowerCase();
						if (!pendingRiddles.isEmpty()) {
							Riddle candidate = pendingRiddles.poll();
							if (vote.equals("yes")) {
								riddles.add(candidate);
								saveRiddles();
								response.put("message", "Riddle approved and added to the game!");
							} else {
								response.put("message", "Riddle rejected and removed.");
							}
							savePendingRiddles();
							response.put("type", "vote-result");
							//sendImg("img/vote.png", response);
						} else {
							response.put("type", "vote-result");
							response.put("message", "No riddle to vote on.");
						}
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
		Riddle current = riddles.get(rand.nextInt(riddles.size()));

		JSONObject obj = new JSONObject();
		obj.put("riddle", current.getRiddle());
		obj.put("answer", current.getAnswer());  // Optional: useful for validation later
		return obj;
	}

	public static JSONObject sendImg(String filename, JSONObject obj) throws Exception {
		File file = new File(filename);

		if (file.exists()) {
			obj.put("image", "Pretend I am this image: " + filename);
		}
		return obj;
	}

	private static void ensureDirectoryExists() {
		File dir = new File(RIDDLE_DIR);
		if (!dir.exists()) {
			dir.mkdirs();
		}
	}

	private static void initializeDefaultRiddles() {
		riddles.clear();  // in case called more than once

		riddles.add(new Riddle("I dry as I get wetter.", "Towel"));
		riddles.add(new Riddle("The building that has the most stories.", "Library"));
		riddles.add(new Riddle("The pot called me black. I said “look who’s talking?!” Then, I made some tea.", "Kettle"));
		riddles.add(new Riddle("Seeing double? Check me to spot your doppelganger.", "Mirror"));
		riddles.add(new Riddle("I have eyes but cannot see.", "Potato"));
	}

	private static void loadRiddles() {
		ensureDirectoryExists();
		File file = new File(RIDDLE_FILE);

		if (!file.exists()) {
			System.out.println("No riddles file found. Initializing with default riddles.");
			initializeDefaultRiddles();
			saveRiddles();
			return;
		}

		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String content = reader.lines().reduce("", (acc, line) -> acc + line);
			JSONArray arr = new JSONArray(content);

			riddles.clear();
			for (int i = 0; i < arr.length(); i++) {
				JSONObject obj = arr.getJSONObject(i);
				riddles.add(new Riddle(obj.getString("riddle"), obj.getString("answer")));
			}
		} catch (IOException | JSONException e) {
			System.err.println("Failed to load riddles.json. Falling back to default riddles.");
			e.printStackTrace();
			initializeDefaultRiddles();
		}
	}

	private static void loadPendingRiddles() {
		ensureDirectoryExists();
		File file = new File(PENDING_FILE);

		if (!file.exists()) {
			pendingRiddles.clear();
			return;
		}

		try {
			String content = new String(Files.readAllBytes(file.toPath()));
			JSONArray arr = new JSONArray(content);
			for (int i = 0; i < arr.length(); i++) {
				JSONObject obj = arr.getJSONObject(i);
				pendingRiddles.add(new Riddle(obj.getString("riddle"), obj.getString("answer")));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void saveRiddles() {
		ensureDirectoryExists();
		try (PrintWriter writer = new PrintWriter(RIDDLE_FILE)) {
			JSONArray arr = new JSONArray();
			for (Riddle r : riddles) {
				arr.put(new JSONObject().put("riddle", r.getRiddle()).put("answer", r.getAnswer()));
			}
			writer.println(arr.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void savePendingRiddles() {
		ensureDirectoryExists();
		try (PrintWriter writer = new PrintWriter(PENDING_FILE)) {
			JSONArray arr = new JSONArray();
			for (Riddle r : pendingRiddles) {
				arr.put(new JSONObject().put("riddle", r.getRiddle()).put("answer", r.getAnswer()));
			}
			writer.println(arr.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
