package Assign32starter;

import java.awt.*;

import org.json.*;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Base64;

import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * The ClientGui class is a GUI frontend that displays an image grid, an input text box,
 * a button, and a text area for status. 
 * 
 * Methods of Interest
 * ----------------------
 * show(boolean modal) - Shows the GUI frame with the current state
 *     -> modal means that it opens the GUI and suspends background processes. Processing 
 *        still happens in the GUI. If it is desired to continue processing in the 
 *        background, set modal to false.
 * newGame(int dimension) - Start a new game with a grid of dimension x dimension size
 * insertImage(String filename, int row, int col) - Inserts an image into the grid
 * appendOutput(String message) - Appends text to the output panel
 * submitClicked() - Button handler for the submit button in the output panel
 * 
 * Notes
 * -----------
 * > Does not show when created. show() must be called to show he GUI.
 * 
 */
public class ClientGui implements Assign32starter.OutputPanel.EventHandlers {
	JDialog frame;
	PicturePanel picPanel;
	OutputPanel outputPanel;
	String currentMess;
	JSONObject welcomeJson;

	Socket sock;
	BufferedReader reader;
	PrintWriter writer;

	String host = "localhost";
	int port = 9000;

	String playerName;
	String currentRiddle = "";
	String currentAnswer = "";
	int currentScore = 0;

	/**
	 * Construct dialog
	 * @throws IOException 
	 */
	public ClientGui(String host, int port) throws IOException {
		this.host = host;
		this.port = port;

		// Prompt for player name before generating game gui elements
		this.playerName = promptForName();
		buildGameGui();

		// you can move the open and closing of a connection to totally different places if you like
		open(); // opening server connection here

		JSONObject startMsg = new JSONObject()
			.put("type", "start")
			.put("name", playerName);

		writer.println(startMsg.toString());

		String reply = reader.readLine();
		JSONObject response = new JSONObject(reply);

		welcomeJson = response;
		outputPanel.appendOutput(response.getString("value"));

		close(); //closing the connection to server

		showMainMenu();
	}

	/**
	 * Shows the current state in the GUI
	 * @param makeModal - true to make a modal window, false disables modal behavior
	 */
	public void show(boolean makeModal) {
		frame.pack();
		frame.setModal(makeModal);
		frame.setVisible(true);
	}

	/**
	 * Creates a new game and set the size of the grid 
	 * @param dimension - the size of the grid will be dimension x dimension
	 * No changes should be needed here
	 */
	public void newGame(int dimension) {
		picPanel.newGame(1);
		outputPanel.appendOutput("Started new game with a " + dimension + "x" + dimension + " board.");
	}

	/**
	 * Submit button handling
	 */
	@Override
	public void submitClicked() {
		String input = outputPanel.getInputText().trim().toLowerCase();

		if (input.equals("exit")) {
			sendExitRequest();
		} else if (input.equals("next")) {
			sendNextRequest();
		} else {
			sendGuessRequest(input);
		}

		// Empty out the input text after each submission
		outputPanel.setInputText("");
	}

	private void sendExitRequest() {
		try {
			open();
			JSONObject req = new JSONObject()
					.put("type", "exit")
					.put("name", playerName);

			writer.println(req.toString());
			JSONObject res = new JSONObject(reader.readLine());

			String message = res.optString("message", "Session ended.");
			outputPanel.appendOutput(message);
			close();
			frame.dispose();
			showMainMenu();
		} catch (Exception e) {
			outputPanel.appendOutput("Error exiting game: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void sendNextRequest() {
		try {
			open();
			JSONObject req = new JSONObject()
					.put("type", "next")
					.put("name", playerName);

			writer.println(req.toString());
			JSONObject res = new JSONObject(reader.readLine());

			currentScore = res.optInt("points", currentScore);
			currentRiddle = res.optString("riddle", currentRiddle);
			currentAnswer = res.optString("answer", currentAnswer);

			outputPanel.appendOutput("Next riddle: " + currentRiddle);
			outputPanel.setPoints(currentScore);
			displayImageFromBase64(res.optString("imageData", ""), 300, 240);
			close();
		} catch (Exception e) {
			outputPanel.appendOutput("Error: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void sendGuessRequest(String input) {
		try {
			open();
			JSONObject req = new JSONObject()
					.put("type", "guess")
					.put("guess", input)
					.put("answer", currentAnswer)
					.put("name", playerName);

			writer.println(req.toString());
			JSONObject res = new JSONObject(reader.readLine());

			String type = res.optString("type", "");

			switch (type) {
				case "correct":
					outputPanel.appendOutput(res.getString("message"));
					currentScore = res.optInt("points", currentScore);
					currentRiddle = res.optString("riddle", currentRiddle);
					currentAnswer = res.optString("answer", currentAnswer);
					displayImageFromBase64(res.optString("imageData", ""), 300, 240);
					outputPanel.appendOutput("Riddle: " + currentRiddle);
					outputPanel.setPoints(currentScore);
					break;
				case "incorrect":
					outputPanel.appendOutput(res.getString("message"));
					currentScore = res.optInt("points", currentScore);
					outputPanel.setPoints(currentScore);
					break;
				case "end":
					outputPanel.appendOutput(res.getString("message"));
					frame.dispose();
					showMainMenu();
					break;
				default:
					outputPanel.appendOutput("Unexpected response.");
			}

			close();
		} catch (Exception e) {
			outputPanel.appendOutput("Error: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Key listener for the input text box
	 * 
	 * Change the behavior to whatever you need
	 */
	@Override
	public void inputUpdated(String input) {
		if (input.equals("surprise")) {
			outputPanel.appendOutput("You found me!");
		}
	}

	public void open() throws UnknownHostException, IOException {
		this.sock = new Socket(host, port); // connect to host and socket
		this.writer = new PrintWriter(sock.getOutputStream(), true);
		this.reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
	}
	
	public void close() {
        try {
            if (writer != null) writer.close();
            if (reader != null) reader.close();
            if (sock != null) sock.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			System.out.println("Expected arguments: <host(String)> <port(int)>");
			System.exit(1);
		}

		// create the frame
		try {
			String host = args[0];
			int port = Integer.parseInt(args[1]);

			ClientGui main = new ClientGui(host, port);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Prompt the user for their name in a modal dialog.
	 * Blocks until a valid name is entered.
	 *
	 * @return the entered player name
	 */
	private String promptForName() {
		final String[] result = new String[1]; // Used to capture user input from inner class

		// Setup modal dialog
		JDialog nameDialog = new JDialog((JDialog) null, "Enter Your Name", true);
		nameDialog.setLayout(new GridBagLayout());
		nameDialog.setSize(300, 150);

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.HORIZONTAL;

		JLabel label = new JLabel("Please enter your name:");
		nameDialog.add(label, c);

		c.gridy++;
		JTextField nameField = new JTextField();
		nameDialog.add(nameField, c);

		c.gridy++;
		c.gridwidth = 1;
		JButton submit = new JButton("Submit");

		submit.addActionListener(e -> {
			String name = nameField.getText().trim();
			if (!name.isEmpty()) {
				result[0] = name;
				nameDialog.dispose();
			}
		});

		nameDialog.add(submit, c);
		nameDialog.setLocationRelativeTo(null);
		nameDialog.setVisible(true);

		return result[0];
	}

	/**
	 * Displays the main menu after name prompt.
	 * Allows user to select an action (view leaderboard, play game, etc).
	 */
	private void showMainMenu() {
		JDialog menuDialog = new JDialog((JDialog) null, "Main Menu", true);
		menuDialog.setLayout(new GridBagLayout());
		menuDialog.setSize(520, 480);
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.weightx = 1;
		c.gridy = 0;

		// Welcome message
		if (welcomeJson != null && welcomeJson.has("value")) {
			JLabel welcomeLabel = new JLabel(welcomeJson.optString("value", ""));
			welcomeLabel.setHorizontalAlignment(SwingConstants.CENTER);
			welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
			welcomeLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));
			c.gridwidth = 2;
			menuDialog.add(welcomeLabel, c);
			c.gridy++;

			// Handle imageData if present
			String imageData = welcomeJson.optString("imageData", null);
			if (imageData != null) {
				JLabel imgLabel = getScaledImageLabel(imageData, 200, 200);
				imgLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));
				c.gridwidth = 2;
				menuDialog.add(imgLabel, c);
				c.gridy++;
			}
		}

		JButton leaderboardBtn = new JButton("View Leaderboard");
		leaderboardBtn.addActionListener(e -> {
			menuDialog.dispose();
			showLeaderboard();
		});
		menuDialog.add(leaderboardBtn, c);

		c.gridy++;
		JButton playGameBtn = new JButton("Play Game");
		playGameBtn.addActionListener(e -> {
			menuDialog.dispose();
			playGame();
		});
		menuDialog.add(playGameBtn, c);

		c.gridy++;
		JButton addRiddleBtn = new JButton("Add New Riddle");
		addRiddleBtn.addActionListener(e -> {
			menuDialog.dispose();
			showAddRiddleForm();
		});
		menuDialog.add(addRiddleBtn, c);

		c.gridy++;
		JButton voteRiddleBtn = new JButton("Vote on Riddle");
		voteRiddleBtn.addActionListener(e -> {
			menuDialog.dispose();
			showRiddleVoting();
		});
		menuDialog.add(voteRiddleBtn, c);

		c.gridy++;
		JButton quitBtn = new JButton("Quit");
		quitBtn.addActionListener(e -> {
			menuDialog.dispose();
			quitGame();
		});
		menuDialog.add(quitBtn, c);

		menuDialog.setLocationRelativeTo(null);
		menuDialog.setVisible(true);
	}

	private void showLeaderboard() {
		try {
			open();
			JSONObject req = new JSONObject().put("type", "leaderboard");
			writer.println(req.toString());

			JSONObject res = new JSONObject(reader.readLine());
			JSONArray entries = res.getJSONArray("entries");

			JDialog dialog = new JDialog((JDialog) null, "Leaderboard", true);
			dialog.setLayout(new GridBagLayout());
			dialog.setSize(480, 640);
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = 0;

			// Show leaderboard image
			String imgData = res.optString("imageData", null);
			if (imgData != null) {
				JLabel imgLabel = getScaledImageLabel(imgData, 296, 240);
				c.gridwidth = 2;
				dialog.add(imgLabel, c);
				c.gridy++;
			}

			for (int i = 0; i < entries.length(); i++) {
				JSONObject entry = entries.getJSONObject(i);
				String line = entry.getString("name") + ": " + entry.getInt("score") + " pts";
				dialog.add(new JLabel(line), c);
				c.gridy++;
			}

			JButton exitBtn = new JButton("Exit");
			exitBtn.addActionListener(e -> {
				dialog.dispose();
				showMainMenu();
			});
			dialog.add(exitBtn, c);

			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);

			close();
		} catch (Exception e) {
			e.printStackTrace();
			showMainMenu();
		}
	}

	private void playGame() {
		// Dispose existing frame if it exists (important when returning from other views)
		if (frame != null) {
			frame.dispose();
		}

		try {
			// Rebuild GUI for game play
			buildGameGui();
			frame.setVisible(true);  // show the new frame
			startGamePlay();         // contact server, show riddle + image
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void buildGameGui() throws IOException {
		frame = new JDialog();
		frame.setLayout(new GridBagLayout());
		frame.setMinimumSize(new Dimension(560, 640));
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		// Top image grid
		picPanel = new PicturePanel();
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 0.25;
		frame.add(picPanel, c);

		// Output + input area
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.weighty = 0.65;
		c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;
		outputPanel = new OutputPanel();
		outputPanel.addEventHandlers(this);
		frame.add(outputPanel, c);

		// Back to Menu Button
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 2;
		c.weighty = 0.1;
		c.fill = GridBagConstraints.HORIZONTAL;

		picPanel.newGame(1);

		frame.setLocationRelativeTo(null);
	}

	private void startGamePlay() {
		try {
			open();
			JSONObject request = new JSONObject()
				.put("type", "play")
				.put("name", playerName);
			writer.println(request.toString());
			JSONObject response = new JSONObject(reader.readLine());

			currentScore = response.optInt("score", currentScore);
			currentRiddle = response.optString("riddle", currentRiddle);
			currentAnswer = response.optString("answer", currentAnswer);

			outputPanel.setPoints(currentScore);
			outputPanel.appendOutput("Riddle: " + currentRiddle);
			displayImageFromBase64(response.optString("imageData", ""), 300, 240);
			close();
		} catch (Exception e) {
			e.printStackTrace();
			outputPanel.appendOutput("Error starting game: " + e.getMessage());
		}
	}

	private void showAddRiddleForm() {
		try {
			open();

			// Build request to start game
			JSONObject request = new JSONObject().put("type", "play");

			// Send request to server
			writer.println(request.toString());

			// Read server response
			JSONObject response = new JSONObject(reader.readLine());
			JDialog dialog = new JDialog((JDialog) null, "Add Riddle", true);
			dialog.setLayout(new GridBagLayout());
			dialog.setSize(360, 420);

			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = 0;

			// Handle imageData if present
			String imageData = response.optString("imageData", null);
			if (imageData != null) {
				JLabel imgLabel = getScaledImageLabel(imageData, 240, 240);
				imgLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));
				c.gridwidth = 2;
				dialog.add(imgLabel, c);
				c.gridy++;
			}

			dialog.add(new JLabel("Riddle:"), c);
			c.gridy++;
			JTextField riddleField = new JTextField();
			dialog.add(riddleField, c);
			c.gridy++;
			dialog.add(new JLabel("Answer:"), c);
			c.gridy++;
			JTextField answerField = new JTextField();
			dialog.add(answerField, c);
			c.gridy++;

			// Buttons Panel
			JPanel buttonPanel = new JPanel();
			JButton submit = new JButton("Submit");
			JButton cancel = new JButton("Cancel");

			submit.addActionListener(e -> {
				try {
					open();
					JSONObject req = new JSONObject()
						.put("type", "addRiddle")
						.put("riddle", riddleField.getText())
						.put("answer", answerField.getText());
					writer.println(req.toString());

					JSONObject res = new JSONObject(reader.readLine());
					outputPanel.appendOutput("Riddle added: " + res.toString());
					close();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				dialog.dispose();
				showMainMenu();
			});

			cancel.addActionListener(e -> {
				dialog.dispose();
				showMainMenu();
			});

			buttonPanel.add(submit);
			buttonPanel.add(cancel);
			dialog.add(buttonPanel, c);

			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void showRiddleVoting() {
		try {
			open();
			JSONObject request = new JSONObject().put("type", "getVoteRiddle");
			writer.println(request.toString());

			JSONObject response = new JSONObject(reader.readLine());
			close();

			if (response.has("riddle")) {
				String riddleText = response.getString("riddle");
				String answerText = response.getString("answer");

				// Display the riddle to vote on
				JDialog dialog = new JDialog((JDialog) null, "Vote on Riddle", true);
				dialog.setLayout(new GridBagLayout());
				dialog.setSize(480, 360);
				GridBagConstraints c = new GridBagConstraints();
				c.gridx = 0;
				c.gridy = 0;
				c.gridwidth = 2;

				// Handle imageData if present
				String imageData = response.optString("imageData", null);
				if (imageData != null) {
					JLabel imgLabel = getScaledImageLabel(imageData, 240, 240);
					imgLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));
					dialog.add(imgLabel, c);
					c.gridy++;
				}

				JLabel votingLabel = new JLabel("<html><b>Riddle:</b> " + riddleText + "<br><b>Answer:</b> " + answerText + "</html>");
				votingLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
				votingLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));
				dialog.add(votingLabel, c);

				// Voting Buttons
				JButton yesBtn = new JButton("Yes");
				JButton noBtn = new JButton("No");
				JPanel btnPanel = new JPanel();
				btnPanel.add(yesBtn);
				btnPanel.add(noBtn);

				c.gridy++;
				dialog.add(btnPanel, c);

				// Voting actions
				yesBtn.addActionListener(e -> {
					submitVote("yes");
					dialog.dispose();
					showMainMenu();
				});

				noBtn.addActionListener(e -> {
					submitVote("no");
					dialog.dispose();
					showMainMenu();
				});

				dialog.setLocationRelativeTo(null);
				dialog.setVisible(true);

				//insertImage(image, 0, 0);
			} else {
				outputPanel.appendOutput(response.optString("message", "No riddles to vote on."));
				showMainMenu();
			}
		} catch (Exception e) {
			e.printStackTrace();
			showMainMenu();
		}
	}

	private void submitVote(String vote) {
		try {
			open();
			JSONObject request = new JSONObject()
				.put("type", "vote")
				.put("vote", vote);
			writer.println(request.toString());

			JSONObject response = new JSONObject(reader.readLine());
			outputPanel.appendOutput(response.optString("message", "Vote submitted."));
			close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void displayImageFromBase64(String base64Image, int targetWidth, int targetHeight) {
		try {
			byte[] imageBytes = Base64.getDecoder().decode(base64Image);
			ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
			picPanel.insertImage(bais, 0, 0, targetWidth, targetHeight);  // Assuming you have a method to accept BufferedImage
		} catch (Exception e) {
			e.printStackTrace();
			outputPanel.appendOutput("Failed to load image from the server: " + e.getMessage());
		}
	}

	private JLabel getScaledImageLabel(String base64Image, int width, int height) {
		try {
			byte[] bytes = Base64.getDecoder().decode(base64Image);
			ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
			BufferedImage image = ImageIO.read(bais);
			if (image != null) {
				Image scaled = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
				return new JLabel(new ImageIcon(scaled));
			}
		} catch (Exception e) {
			outputPanel.appendOutput("Error decoding image: " + e.getMessage());
		}
		return new JLabel("Image failed to load");
	}

	private void quitGame() {
		System.exit(0);
	}
}
