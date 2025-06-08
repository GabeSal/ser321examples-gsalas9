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
		Socket sock;
		try {
			//setting some riddles here, you can add more, change them, store them in a different way
			riddles[0] = new Riddle("I dry as I get wetter.", "Towel");
			riddles[1] = new Riddle("The building that has the most stories.  ", "Library");
			riddles[2] = new Riddle("The pot called me black. I said “look who’s talking?!” Then, I made some tea.", "Kettle");
			riddles[3] = new Riddle("Seeing double? Check me to spot your doppelganger.", "Mirror");
			riddles[4] = new Riddle("I have eyes but cannot see.", "Potatoe");


			//opening the socket here, just hard coded since this is just a bas example
			ServerSocket serv = new ServerSocket(8888); // TODO, should not be hardcoded
			System.out.println("Server ready for connetion");

			// placeholder for the person who wants to play a game
			String name = "";
			int points = 0;

			// read in one object, the message. we know a string was written only by knowing what the client sent. 
			// must cast the object from Object to desired type to be useful
			while(true) {
				sock = serv.accept(); // blocking wait

				// could totally use other input outpur streams here
				ObjectInputStream in = new ObjectInputStream(sock.getInputStream());
				OutputStream out = sock.getOutputStream();

				String s = (String) in.readObject();
				JSONObject json = new JSONObject(s); // the requests that is received

				JSONObject response = new JSONObject();

				// you should adapt this part, this is jsut to show you how you can send a message
				if (json.getString("type").equals("start")){
					
					System.out.println("- Got a start");
				
					response.put("type","hello" );
					response.put("value","Hello, please tell me your name." );

					sendImg("img/hi.png", response); // calling a method that will manipulate the image and will make it send ready
					
				} else if (json.getString("type").equals("riddle")){
					Random rand = new Random();
					Riddle currentRiddle = riddles[rand.nextInt(4)]; // holds the current riddle object
					response.put("riddle",currentRiddle.getRiddle());
				} else if (json.getString("type").equals("name")){
					response.put("name", json.getString("name"));
				}
				else {
					System.out.println("not sure what you meant");
					response.put("type","error" );
					response.put("message","unknown response" );
				}
				PrintWriter outWrite = new PrintWriter(sock.getOutputStream(), true); // using a PrintWriter here, you could also use and ObjectOutputStream or anything you fancy
				outWrite.println(response.toString());
			}
			
		} catch(Exception e) {e.printStackTrace();}
	}

	/* TODO this is for you to implement, I just put a place holder here */
	public static JSONObject sendImg(String filename, JSONObject obj) throws Exception {
		File file = new File(filename);

		if (file.exists()) {
			// import image
			// I did not use the Advanced Custom protocol
			// I read in the image and translated it into basically into a string and send it back to the client where I then decoded again
			obj.put("image", "Pretend I am this image: " + filename);
		} 
		return obj;
	}
}
