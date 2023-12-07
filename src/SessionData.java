import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SessionData {
    private int maxAge;
    private int attempt;
    private final String secretWord;
    private final Map<Integer, List<String>> gameState;

    public SessionData(int maxAge, String secretWord) {
        this.maxAge = maxAge;
        this.attempt = 0;
        this.secretWord = secretWord;
        this.gameState = new HashMap<Integer, List<String>>();
        for (int i = 0; i < 6; i++) {
            List<String> data = new ArrayList<String>();
            data.add(""); // initial guess
            data.add(""); // initial color code
            this.gameState.put(i, data);
        }
    }

    public int getMaxAge() { return maxAge; }
    public int getAttempts() { return attempt; }
    public String getSecretWord() { return secretWord; }

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

    public void setMaxAge(int maxAge) {this.maxAge = maxAge; }
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