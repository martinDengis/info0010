import java.io.*;
import java.net.*;

public class ConnectionChannel implements Runnable {
    private Socket clientSocket;
    private String secretWord;

    public ConnectionChannel(Socket clientSocket, String secretWord) {
        this.clientSocket = clientSocket;
        this.secretWord = secretWord;
    }

    @Override
    public void run() {
        try {
            // Set a socket timeout (for 30 sec)
            clientSocket.setSoTimeout(30000);
            clientSocket.setTcpNoDelay(true);

            // Initialize reader and writer
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);

            String input;

            // Loop handling client's requests
            while ((input = reader.readLine()) != null) {
                if (input.equals("QUIT")) { break; } 
                else if (input.equals("CHEAT")) { 
                    writer.print(secretWord + "\r\n");
                    writer.flush(); 
                } 
                else if (input.startsWith("TRY")) {
                    String guess = input.substring(4).trim();
                    String response = checkWord(guess, secretWord);
                    writer.print(response + "\r\n");
                    writer.flush();
                } 
                else { 
                    writer.print("WRONG\r\n"); 
                    writer.flush();
                }
            }
        
            reader.close();
            writer.close();
            clientSocket.close();
        } 
        catch (SocketTimeoutException e1) { System.out.println("Client connection timed out."); }
        catch (SocketException e2) { e2.printStackTrace(); }
        catch (IOException e3) { e3.printStackTrace(); }
    }

    private String checkWord(String guess, String secretWord) {
        if (!isValidGuess(guess)) { return "NONEXISTENT"; } 
        else { return responseConstructor(guess, secretWord); }
    }
    
    private boolean isValidGuess(String guess) {
        // Check if the word is a valid 5-letter word
        return guess.length() == 5 && WordleWordSet.WORD_SET.contains(guess);
    }
    
    private String responseConstructor(String guess, String secretWord) {
        
        // Initialise response and tracking arrays
        char[] response = new char[5];
        boolean[] usedInGuess = new boolean[5];
        boolean[] usedInSecret = new boolean[5];
    
        // GREEN: Mark well-placed letters
        for (int i = 0; i < 5; i++) {
            if (guess.charAt(i) == secretWord.charAt(i)) {
                response[i] = 'G';
                usedInGuess[i] = usedInSecret[i] = true;
            }
        }
    
        // YELLOW: Mark misplaced letters
        for (int i = 0; i < 5; i++) {
            if (!usedInGuess[i]) {
                for (int j = 0; j < 5; j++) {
                    if (!usedInSecret[j] && guess.charAt(i) == secretWord.charAt(j)) {
                        response[i] = 'Y';
                        usedInGuess[i] = usedInSecret[j] = true;
                        break;
                    }
                }
            }
        }
    
        // BLACK: Mark incorrect letters
        for (int i = 0; i < 5; i++)
            if (!usedInGuess[i]) { response[i] = 'B'; }
    
        return new String(response);
    }

}
