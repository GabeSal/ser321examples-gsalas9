package server;

import java.util.*; 
import java.io.*;

/**
 * Class: Game 
 * Description: Game class that can load a phrase
 * Class can be used to hold the persistent state for a game for different threads
 * synchronization is not taken care of .
 * You can change this Class in any way you like or decide to not use it at all
 * I used this class in my SockBaseServer to create a new game and keep track of the current image evenon differnt threads. 
 * My threads each get a reference to this Game
 */

public class Game {
    private static final int DEFAULT_GUESS_FAILURES = 6;
    private int points;
    private int length; // length of phrase
    private char[] originalPhrase; // the original phrase
    private char[] hiddenPhrase; // the hidden phrase
    private List<String> phrases = new ArrayList<String>(); // list of phrases
    private Set<Character> guessedAll = new HashSet<>();
    private Set<Character> guessedCorrect = new HashSet<>();
    private Set<Character> guessedWrong = new HashSet<>();
    private String currentTask;
    private int failedGuesses;


    public Game(){
        currentTask = "";
        length = 0;
        loadPhrases("phrases.txt");
    }

    /**
     * Method loads in a new phrase from the specified file and creates the hidden phrase for it.
     * @return Nothing.
     */
    public void newGame(){
        currentTask = "Guess a letter";
        guessedCorrect.clear();
        guessedWrong.clear();
        guessedAll.clear();
        failedGuesses = 0;
        getRandomPhrase();
    }

    public void loadPhrases(String filename){
        try{
            File file = new File( Game.class.getResource("/"+filename).getFile() );
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                phrases.add(line);
                System.out.println("Added Phrase: " + line);
            }
        }
        catch (Exception e){
            System.out.println(e);
            System.out.println("File load error"); // extremely simple error handling, you can do better if you like. 
        }
    }

    // Simple method to load a random phrase, but might load the same phrase twice
    private void getRandomPhrase(){
        String phrase = "";
        try{
            // loads one random phrase from list
            Random rand = new Random(); 
            int randInt = rand.nextInt(phrases.size());

            phrase = phrases.get(randInt);
            length = phrase.length();

            System.out.println("Starting new game with phrase: " + phrase);

            originalPhrase = new char[length];
            hiddenPhrase = new char[length];

            for (int i = 0; i < length; i++) {
                char curr = phrase.charAt(i);
                originalPhrase[i] = curr;
                if (!Character.isLetter(curr)) {
                    hiddenPhrase[i] = curr; // show punctuation and symbols
                } else {
                    hiddenPhrase[i] = '_';
                }
            }
        }
        catch (Exception e){
            System.out.println("Error generating random phrase"); // extremely simple error handling, you can do better if you like. 
            System.exit(0);
        }

    }

    public boolean markGuess(char guess) {
        if (guessedAll.contains(guess)) {
            points--;  // Penalty for duplicate guess
            return false;
        }

        guessedAll.add(guess);
        boolean found = false;
        int revealed = 0;

        for (int i = 0; i < length; i++) {
            if (originalPhrase[i] == guess && hiddenPhrase[i] == '_') {
                hiddenPhrase[i] = guess;
                found = true;
                revealed++;
            }
        }

        if (found) {
            guessedCorrect.add(guess);
            points += revealed; // Gain points for new letters
        } else {
            guessedWrong.add(guess);
            points--; // Wrong guess penalty
            failedGuesses++; // Increment failures
        }

        return found;
    }

    public int getPoints() { return points; }

    public int getFailures() { return failedGuesses; }

    public int getMaxFailures() { return DEFAULT_GUESS_FAILURES; }

    public boolean hasExceededFailures() { return failedGuesses >= DEFAULT_GUESS_FAILURES; }

    public String getPhrase(){
        return String.valueOf(hiddenPhrase);
    }

    public String getTask(){
        return currentTask;
    }

    public String getCorrectGuesses() {
        return guessedCorrect.stream()
                .map(String::valueOf)
                .sorted()
                .reduce((a, b) -> a + " " + b).orElse("");
    }

    public String getIncorrectGuesses() {
        return guessedWrong.stream()
                .map(String::valueOf)
                .sorted()
                .reduce((a, b) -> a + " " + b).orElse("");
    }
}
