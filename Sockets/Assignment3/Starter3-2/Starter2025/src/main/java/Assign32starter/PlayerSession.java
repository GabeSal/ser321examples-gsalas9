package Assign32starter;

public class PlayerSession {
    private final String name;
    private int score;
    private Riddle currentRiddle;

    public PlayerSession(String name, int score) {
        this.name = name;
        this.score = score;
        this.currentRiddle = null;
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }

    public void addPoints(int delta) {
        score += delta;
    }

    public void resetScore() {
        score = 0;
    }

    public void setScore(int newScore) {
        score = newScore;
    }

    public void setCurrentRiddle(Riddle r) {
        currentRiddle = r;
    }

    public Riddle getCurrentRiddle() {
        return currentRiddle;
    }
}
