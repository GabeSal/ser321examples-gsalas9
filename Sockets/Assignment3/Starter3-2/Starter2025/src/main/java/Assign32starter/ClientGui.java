package Assign32starter;

import java.awt.Dimension;

import org.json.*;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

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

	public void buildGameGui() throws IOException {
		frame = new JDialog();
		frame.setLayout(new GridBagLayout());
		frame.setMinimumSize(new Dimension(640, 640));
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
		JButton backButton = new JButton("Back to Main Menu");
		backButton.addActionListener(e -> {
			frame.dispose();      // Close current game UI
			showMainMenu();       // Return to menu
		});
		frame.add(backButton, c);

		picPanel.newGame(1);
		insertImage("img/hi.png", 0, 0);
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
	 * Insert an image into the grid at position (col, row)
	 * 
	 * @param filename - filename relative to the root directory
	 * @param row - the row to insert into
	 * @param col - the column to insert into
	 * @return true if successful, false if an invalid coordinate was provided
	 * @throws IOException An error occured with your image file
	 */
	public boolean insertImage(String filename, int row, int col) throws IOException {
		System.out.println("Image insert");
		String error = "";
		try {
			// insert the image
			if (picPanel.insertImage(filename, row, col)) {
				// put status in output
				return true;
			}
			error = "File(\"" + filename + "\") not found.";
		} catch(PicturePanel.InvalidCoordinateException e) {
			// put error in output
			error = e.toString();
		}
		outputPanel.appendOutput(error);
		return false;
	}

	/**
	 * Submit button handling
	 * 
	 * TODO: This is where your logic will go or where you will call appropriate methods you write. 
	 * Right now this method opens and closes the connection after every interaction, if you want to keep that or not is up to you. 
	 */
	@Override
	public void submitClicked() {
		outputPanel.appendOutput("Submit clicked.");
		return;

//		try {
//			open();
//			String input = outputPanel.getInputText();
//			JSONObject message = new JSONObject();
//
//			if (input.equals("riddle")) {
//				message.put("type", "riddle");
//			} else {
//				message.put("type", "name").put("name", input);
//			}
//
//			JSONObject reply = new JSONObject(reader.readLine());
//			outputPanel.appendOutput(reply.toString());
//			close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
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
		menuDialog.setSize(400, 300);
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.weightx = 1;
		c.gridy = 0;

		// Welcome message
		if (welcomeJson != null && welcomeJson.has("value")) {
			JLabel welcomeLabel = new JLabel(welcomeJson.optString("value", ""));
			welcomeLabel.setHorizontalAlignment(SwingConstants.CENTER);
			c.gridwidth = 2;
			menuDialog.add(welcomeLabel, c);
			c.gridy++;
		}

		// Show 'hi' image sent from server

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
			dialog.setSize(300, 300);
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = 0;

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

	private void startGamePlay() {
		try {
			open();

			// Build request to start game
			JSONObject req = new JSONObject().put("type", "play");

			// Send request to server
			writer.println(req.toString());

			// Read server response
			JSONObject res = new JSONObject(reader.readLine());
			// Display the received riddle
			String riddle = res.getString("riddle");
			outputPanel.appendOutput("Riddle: " + riddle);
			outputPanel.appendOutput(res.optString("image", ""));

			// Insert image with riddle
			insertImage("img/play.jpg",0,0);
			close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void showAddRiddleForm() {
		JDialog dialog = new JDialog((JDialog) null, "Add Riddle", true);
		dialog.setLayout(new GridBagLayout());
		dialog.setSize(300, 200);

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;

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
	}

	private void showRiddleVoting() {
		JDialog dialog = new JDialog((JDialog) null, "Vote on Riddle", true);
		dialog.setLayout(new GridBagLayout());
		dialog.setSize(300, 150);

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;

		JLabel label = new JLabel("Voting is not fully implemented.");
		dialog.add(label, c);

		c.gridy++;
		JPanel buttonPanel = new JPanel();
		JButton voteBtn = new JButton("Vote");
		JButton cancelBtn = new JButton("Cancel");

		voteBtn.addActionListener(e -> {
			try {
				open();
				JSONObject req = new JSONObject()
						.put("type", "vote")
						.put("riddleId", "exampleId")
						.put("vote", "up");
				writer.println(req.toString());

				JSONObject res = new JSONObject(reader.readLine());
				outputPanel.appendOutput(res.optString("message", "No response message"));
				close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			dialog.dispose();
			showMainMenu();
		});

		cancelBtn.addActionListener(e -> {
			dialog.dispose();
			showMainMenu();
		});

		buttonPanel.add(voteBtn);
		buttonPanel.add(cancelBtn);
		dialog.add(buttonPanel, c);

		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
	}

	private void quitGame() {
		System.exit(0);
	}
}
