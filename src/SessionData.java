import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The SessionData class represents the data associated with a game session.
 * It stores information such as the number of attempts, last activity time,
 * game status, secret word, and game state.
 */
public class SessionData {
    private static final int SESSION_TIMEOUT_SECONDS = 600; // 10 minutes
    private int attempt = 0;
    private long lastActivityTime;
    private String status = "Playing"; // "Playing", "Win", "Gameover"
    private final String secretWord;
    private final Map<Integer, List<String>> gameState = new HashMap<Integer, List<String>>();

    public SessionData(String secretWord) {
        this.secretWord = secretWord;
        this.lastActivityTime = System.currentTimeMillis(); // Set the initial last activity time

        // Initialize the game state
        List<String> initSecret = new ArrayList<String>();
        initSecret.add(secretWord);
        initSecret.add(secretWord);
        this.gameState.put(-1, initSecret); // -1 is the initial state

        // Initialize the game state
        for (int i = 0; i < 6; i++) {
            List<String> data = new ArrayList<String>();
            data.add(""); // initial guess
            data.add(""); // initial color code
            this.gameState.put(i, data);
        }
        
    }

    // Getters
    public int getAttempt() { return this.attempt; }
    public long getLastActivityTime() { return this.lastActivityTime; }
    public String getStatus() { return this.status; }
    public String getSecretWord() { return this.secretWord; }
    public String getFullGameState() {
        String fullGameState = "";
        for (int i = -1; i < 6; i++) {
            List<String> data = gameState.get(i);
            fullGameState += i + ":" + data.get(0) + ":" + data.get(1) + ";";
        }
        return fullGameState;
    }
    public String getCurrGameState(int currGS) {
        List<String> data = gameState.get(currGS);
        return currGS + ":" + data.get(0) + ":" + data.get(1);
    }
    
    // Setters
    public void incrementAttempts() { this.attempt++; }
    public void decrementAttempts() { this.attempt--; }
    public void resetAttempts() { this.attempt = 0; }
    public void updateLastActivityTime() { this.lastActivityTime = System.currentTimeMillis(); }
    public void setStatus(String status) { this.status = status; }
    public void addGameState(String guess, String color) {
        List<String> data = new ArrayList<String>();
        data.add(guess);
        data.add(color);
        gameState.put(this.attempt, data);
        incrementAttempts();
    }

    // Other methods
    public boolean isExpired() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastActivityTime;
        return elapsedTime > (SESSION_TIMEOUT_SECONDS * 1000); // Convert seconds to milliseconds
    }

}
