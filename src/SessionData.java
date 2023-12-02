public class SessionData {
    private String sessionID;
    private int maxAge;
    private int attempts;
    private final String secretWord;

    public SessionData(String sessionID, int maxAge, String secretWord) {
        this.sessionID = sessionID;
        this.maxAge = maxAge;
        this.attempts = 0;
        this.secretWord = secretWord;
    }

    public String getName() { return sessionID; }
    public int getMaxAge() { return maxAge; }
    public int getAttempts() { return attempts; }
    public String getSecretWord() { return secretWord; }
    public void setName(String sessionID) { this.sessionID = sessionID; }
    public void setMaxAge(int maxAge) {this.maxAge = maxAge; }
    public void incrementAttempts() { this.attempts++; }
    public void resetAttempts() { this.attempts = 0; }
}