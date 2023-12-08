import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SessionData {
    private int attempt = 0;
    private final String secretWord;
    private final Map<Integer, List<String>> gameState;
    private static final int SESSION_TIMEOUT_SECONDS = 600; // 10 minutes
    private long lastActivityTime; 

    public SessionData(String secretWord) {
        this.secretWord = secretWord;
        this.gameState = new HashMap<Integer, List<String>>();
        this.lastActivityTime = System.currentTimeMillis(); // Set the initial last activity time
        for (int i = 0; i < 6; i++) {
            List<String> data = new ArrayList<String>();
            data.add(""); // initial guess
            data.add(""); // initial color code
            this.gameState.put(i, data);
        }
    }

    public int getAttempt() { return attempt; }
    public String getSecretWord() { return secretWord; }
    public void updateLastActivityTime() { this.lastActivityTime = System.currentTimeMillis(); }
    public boolean isExpired() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastActivityTime;
        return elapsedTime > (SESSION_TIMEOUT_SECONDS * 1000); // Convert seconds to milliseconds
    }

    public List<String> getGuessandColor(int attempt) { return gameState.get(attempt); }
    public String getGuess(int attempt) { return gameState.get(attempt).get(0); }
    public String getColor(int attempt) { return gameState.get(attempt).get(1); }

    public String getFullGameState() {
        String fullGameState = "";
        for (int i = 0; i < 6; i++) {
            List<String> data = gameState.get(i);
            fullGameState += i + ":" + data.get(0) + ":" + data.get(1) + ";";
        }
        return fullGameState;
    }

    public String getCurrGameState() {
        List<String> data = gameState.get(this.attempt);
        return this.attempt + ":" + data.get(0) + ":" + data.get(1);
    }

    public void incrementAttempts() { this.attempt++; }
    public void resetAttempts() { this.attempt = 0; }
    public void addGamestate(String guess, String color) {
        List<String> data = new ArrayList<String>();
        data.add(guess);
        data.add(color);
        gameState.put(this.attempt, data);
        incrementAttempts();
    }
}