import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SessionData {
    private String sessionID;
    private int maxAge;
    private int attempts;
    private final String secretWord;
    private final Map<Integer, List<String>> gameState;

    public SessionData(String sessionID, int maxAge, String secretWord) {
        this.sessionID = sessionID;
        this.maxAge = maxAge;
        this.attempts = 0;
        this.secretWord = secretWord;
        this.gameState = new HashMap<Integer, List<String>>();
        for (int i = 0; i < 6; i++) {
            List<String> data = new ArrayList<String>();
            data.add(""); // initial guess
            data.add(""); // initial color code
            this.gameState.put(i, data);
        }
    }

    public String getSessionID() { return sessionID; }
    public int getMaxAge() { return maxAge; }
    public int getAttempts() { return attempts; }
    public String getSecretWord() { return secretWord; }

    public List<String> getGuessandColor(int attempt) { return gameState.get(attempt); }
    public String getGuess(int attempt) { return gameState.get(attempt).get(0); }
    public String getColor(int attempt) { return gameState.get(attempt).get(1); }

    public void setName(String sessionID) { this.sessionID = sessionID; }
    public void setMaxAge(int maxAge) {this.maxAge = maxAge; }
    public void incrementAttempts() { this.attempts++; }
    public void resetAttempts() { this.attempts = 0; }
    public void setGamestate(int attempt, String guess, String color) {
        List<String> data = new ArrayList<String>();
        data.add(guess);
        data.add(color);
        gameState.put(attempt, data);
    }
}