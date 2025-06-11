package Assign32starter;
import java.net.*;
import java.nio.file.Files;
import java.util.*;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.json.*;

/**
 * A class to demonstrate a simple client-server connection using sockets.
 * Ser321 Foundations of Distributed Software Systems
 */
public class SockServer {
	static final String RIDDLE_DIR = "riddles/";
	static final String RIDDLE_FILE = RIDDLE_DIR + "riddles.json";
	static final String PENDING_FILE = RIDDLE_DIR + "pending_riddles.json";
	static final String SESSION_FILE = RIDDLE_DIR + "sessions.json";
	static final String LEADERBOARD_FILE = RIDDLE_DIR + "leaderboard.json";

	static Map<String, PlayerSession> activeSessions = new ConcurrentHashMap<>();
	static Map<String, Integer> leaderboard = new ConcurrentHashMap<>();
	static List<Riddle> riddles = Collections.synchronizedList(new ArrayList<>()); // stores all riddles
	static Queue<Riddle> pendingRiddles = new ConcurrentLinkedQueue<>(); // riddles pending submission to the game

	public static void main (String args[]) {
		if (args.length != 1) {
			System.out.println("Expected arguments: <port(int)>");
			System.exit(1);
		}

		int port = Integer.parseInt(args[0]);
		ensureDirectoryExists();
		loadRiddles();
		loadPendingRiddles();
		loadLeaderboard();
		loadSessions();

		try (ServerSocket serverSocket = new ServerSocket(port)) {
			System.out.println("Listening on port " + port);
			while (true) {
				Socket client = serverSocket.accept();
				new Thread(() -> handleClient(client)).start(); // 🔄 Spawn thread
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void handleClient(Socket client) {
		try (
				BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
				PrintWriter out = new PrintWriter(client.getOutputStream(), true)
		) {
			String line = in.readLine();
			if (line == null) return;

			JSONObject request = new JSONObject(line);
			JSONObject response = handleRequest(request); // Extract your switch logic here

			out.println(response.toString());

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				client.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static JSONObject handleRequest(JSONObject request) throws Exception {
		JSONObject response = new JSONObject();
		String type = request.optString("type", "");
		String name = request.optString("name", "Player");

		switch (type) {
			case "start":
				return handleStart(name);
			case "play":
				return handlePlay(name);
			case "exit":
				return handleExit(name);
			case "guess":
				return handleGuess(name, request.optString("guess", ""));
			case "next":
				return handleNext(name);
			case "leaderboard":
				return handleLeaderboard();
			case "addRiddle":
				return handleAddRiddle(request);
			case "getVoteRiddle":
				return handleGetVoteRiddle();
			case "vote":
				return handleVote(request);
			case "quit":
				return handleQuit(name);
			default:
				error("Unknown request type.");
				return sendImg("img/error.png", response);
		}
	}

	private static JSONObject handleStart(String name) {
		PlayerSession session = activeSessions.computeIfAbsent(name,
				n -> new PlayerSession(n, leaderboard.getOrDefault(n, 0))
		);

		JSONObject response = new JSONObject();
		response.put("type", "hello");
		response.put("value", "Hello " + name + ", welcome to the game!");
		System.out.println(name + " has entered the server.");
		try {
			return sendImg("img/welcome.png", response);
		} catch (Exception e) {
			return response.put("error", "Image not found.");
		}
	}

	private static JSONObject handlePlay(String name) {
		PlayerSession session = activeSessions.get(name);
		if (session == null) return error("No session found for player: " + name);

		Riddle r = serveRandomRiddleObject();
		session.setCurrentRiddle(r);

		JSONObject response = new JSONObject();
		response.put("type", "play");
		response.put("riddle", r.getRiddle());
		response.put("answer", r.getAnswer());
		response.put("score", session.getScore());

		try {
			updateLeaderboard(name, session.getScore());
			saveLeaderboard();
			return sendImg("img/play.jpg", response);
		} catch (Exception e) {
			return response.put("error", "Image load failed.");
		}
	}

	private static JSONObject handleExit(String name) {
		PlayerSession session = activeSessions.get(name);
		JSONObject response = new JSONObject();

		if (session != null) {
			updateLeaderboard(name, session.getScore());
			saveLeaderboard();
			saveSessions();
		}

		response.put("type", "end");
		response.put("message", "Thanks for playing! Final score: " +
				(session != null ? session.getScore() : 0));
		return response;
	}

	private static JSONObject handleGuess(String name, String guess) {
		PlayerSession session = activeSessions.get(name);
		JSONObject response = new JSONObject();

		if (session == null || session.getCurrentRiddle() == null) {
			return error("No active game session.");
		}

		String correct = session.getCurrentRiddle().getAnswer();
		if (guess.equalsIgnoreCase("exit")) {
			response.put("type", "end");
			response.put("message", "Thanks for playing! Final score: " + session.getScore());
			return response;
		}
		else if (guess.equalsIgnoreCase(correct)) {
			session.addPoints(10);
			Riddle next = serveRandomRiddleObject();
			session.setCurrentRiddle(next);

			response.put("type", "correct");
			response.put("message", "Correct! +10 points.");
			response.put("riddle", next.getRiddle());
			response.put("answer", next.getAnswer());
			response.put("points", session.getScore());

			try {
				return sendImg("img/play.jpg", response);
			} catch (Exception e) {
				return response.put("error", "Image error.");
			}

		} else {
			session.addPoints(-1);
			response.put("type", "incorrect");
			response.put("message", "Incorrect! -1 point.");
			response.put("points", session.getScore());

			return response;
		}
	}

	private static JSONObject handleNext(String name) {
		PlayerSession session = activeSessions.get(name);
		JSONObject response = new JSONObject();

		if (session == null) {
			return error("No session in progress.");
		}

		session.addPoints(-5);
		Riddle nextRiddle = serveRandomRiddleObject();
		session.setCurrentRiddle(nextRiddle);

		response.put("type", "next");
		response.put("riddle", nextRiddle.getRiddle());
		response.put("answer", nextRiddle.getAnswer());
		response.put("points", session.getScore());
		response.put("result", "Skipped! -5 points.");

		updateLeaderboard(name, session.getScore());
		saveLeaderboard();
		saveSessions();

		try {
			return sendImg("img/play.jpg", response);
		} catch (Exception e) {
			return response.put("error", "Image error.");
		}
	}

	private static JSONObject handleLeaderboard() {
		JSONObject response = new JSONObject();
		response.put("type", "leaderboard");
		response.put("entries", getLeaderboardJSON());

		try {
			return sendImg("img/leaderboard.png", response);
		} catch (Exception e) {
			return response.put("error", "Leaderboard image error.");
		}
	}

	private static JSONObject handleAddRiddle(JSONObject request) {
		JSONObject response = new JSONObject();

		String newRiddle = request.optString("riddle", "").trim();
		String newAnswer = request.optString("answer", "").trim();

		if (newRiddle.isEmpty() || newAnswer.isEmpty()) {
			response.put("type", "error");
			response.put("message", "Riddle or answer was missing.");
			return response;
		}

		pendingRiddles.add(new Riddle(newRiddle, newAnswer));
		savePendingRiddles();

		response.put("type", "addRiddle");
		response.put("message", "New riddle added to pending list!");

		try {
			return sendImg("img/add.png", response);
		} catch (Exception e) {
			return response.put("error", "Add image error.");
		}
	}

	private static JSONObject handleGetVoteRiddle() {
		JSONObject response = new JSONObject();

		Riddle candidate = pendingRiddles.peek();
		if (candidate == null) {
			response.put("type", "vote");
			response.put("message", "No riddles available for voting.");
			return response;
		}

		response.put("type", "vote");
		response.put("riddle", candidate.getRiddle());
		response.put("answer", candidate.getAnswer());

		try {
			return sendImg("img/vote.png", response);
		} catch (Exception e) {
			return response.put("error", "Vote image error.");
		}
	}

	private static JSONObject handleVote(JSONObject request) {
		JSONObject response = new JSONObject();

		String vote = request.optString("vote", "").toLowerCase();
		Riddle candidate = pendingRiddles.poll();

		if (candidate == null) {
			response.put("type", "vote-result");
			response.put("message", "No riddle to vote on.");
			return response;
		}

		if (vote.equals("yes")) {
			riddles.add(candidate);
			saveRiddles();
			response.put("message", "Riddle approved and added to the game!");
		} else {
			response.put("message", "Riddle rejected and removed.");
		}

		savePendingRiddles();
		response.put("type", "vote-result");

		try {
			return sendImg("img/vote.png", response);
		} catch (Exception e) {
			return response.put("error", "Vote image error.");
		}
	}

	private static JSONObject handleQuit(String name) {
		JSONObject response = new JSONObject();
		response.put("type", "quit");
		response.put("message", "Thanks for playing!");

		PlayerSession session = activeSessions.get(name);
		if (session != null) {
			updateLeaderboard(name, session.getScore());
			saveLeaderboard();
			saveSessions();
		}

		System.out.println(name + " has exited.");

		return response;
	}

	private static JSONObject error(String message) {
		JSONObject obj = new JSONObject();
		obj.put("type", "error");
		obj.put("message", message);
		return obj;
	}

	private static Riddle serveRandomRiddleObject() {
		if (riddles.isEmpty()) {
			initializeDefaultRiddles();  // fallback if needed
		}
		Random rand = new Random();
		return riddles.get(rand.nextInt(riddles.size()));
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

	private static void loadSessions() {
		File file = new File(SESSION_FILE);
		if (!file.exists()) return;

		try {
			String content = Files.readString(file.toPath());
			JSONArray array = new JSONArray(content);
			for (int i = 0; i < array.length(); i++) {
				JSONObject obj = array.getJSONObject(i);
				String name = obj.getString("name");
				int score = obj.getInt("score");
				activeSessions.put(name, new PlayerSession(name, score));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void saveSessions() {
		ensureDirectoryExists();
		JSONArray array = new JSONArray();
		for (PlayerSession session : activeSessions.values()) {
			JSONObject obj = new JSONObject();
			obj.put("name", session.getName());
			obj.put("score", session.getScore());
			array.put(obj);
		}

		try (PrintWriter writer = new PrintWriter(SESSION_FILE)) {
			writer.println(array.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
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

			if (arr.isEmpty()) {
				System.out.println("Empty riddle file. Loading defaults.");
				initializeDefaultRiddles();
				saveRiddles();
			}

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

	private static void updateLeaderboard(String player, int score) {
		leaderboard.put(player, Math.max(score, leaderboard.getOrDefault(player, 0)));
	}

	private static JSONArray getLeaderboardJSON() {
		JSONArray array = new JSONArray();
		leaderboard.entrySet().stream()
			.sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
			.forEach(entry -> {
				array.put(new JSONObject().put("name", entry.getKey()).put("score", entry.getValue()));
			});
		return array;
	}

	private static void saveLeaderboard() {
		try (PrintWriter writer = new PrintWriter(LEADERBOARD_FILE)) {
			JSONArray array = getLeaderboardJSON();
			writer.println(array.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void loadLeaderboard() {
		File file = new File(LEADERBOARD_FILE);
		if (!file.exists()) return;

		try {
			String content = Files.readString(file.toPath());
			JSONArray array = new JSONArray(content);
			for (int i = 0; i < array.length(); i++) {
				JSONObject obj = array.getJSONObject(i);
				leaderboard.put(obj.getString("name"), obj.getInt("score"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static JSONObject sendImg(String filename, JSONObject obj) throws Exception {
		try {
			File file = new File(filename);
			if (file.exists()) {
				byte[] imageBytes = Files.readAllBytes(file.toPath());
				String base64Image = Base64.getEncoder().encodeToString(imageBytes);
				obj.put("imageData", base64Image);
				obj.put("imageName", file.getName());  // optional, for logging
				//System.out.println("Encoded image " + filename + " with " + imageBytes.length + " bytes");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return obj;
	}
}
