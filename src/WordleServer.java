import java.io.*;
import java.util.*;
import java.net.*;

public class WordleServer {
    private static int currentConnectionID = -1; // Keep track of number of connections

    public static void main(String[] args) {
        int port = 2348;

        try (ServerSocket ss = new ServerSocket(port)) {
            System.out.println("Wordle Server is listening on port " + port + ".");

            while (true) {
                try {
                    Socket clientSocket = ss.accept();
                    String secretWord = generateSecretWord();
                    ConnectionChannel client = new ConnectionChannel(clientSocket, secretWord, currentConnectionID++);
                    Thread clientThread = new Thread(client);
                    clientThread.start();
                    System.out.println("Connection with ID "+ currentConnectionID + " was established with server.");
                } catch (IOException  e2) {
                    System.err.println("Error accepting client connection. Failing connection ID : " + currentConnectionID);
                    e2.printStackTrace();
                }
            }
        } catch (IOException e1) { 
            System.err.println("Could not bind to port " + port);
            e1.printStackTrace();
            System.exit(1);
        }
    }

    private synchronized static String generateSecretWord() {
        ArrayList<String> wordList = new ArrayList<String>(WordleWordSet.WORD_SET);
        Random random = new Random();
        int index = random.nextInt(wordList.size());
        return wordList.get(index);
    }
}
