import java.io.*;
import java.util.*;
import java.net.*;

public class WordleServer {
    private static final int PORT = 2348;
    private static int currentConnectionID = -1; // Keep track of number of connections
    private static List<ConnectionStatusMonitor> runningConnections = new ArrayList<>(); // Keep track of running threads

    public static void main(String[] args) {

        try (ServerSocket ss = new ServerSocket(PORT)) {
            System.out.println("Wordle Server is listening on port " + PORT + ".");

            while (true) {
                // Loop identifying terminated connections
                for(int i = 0; i < runningConnections.size(); i++) {
                    ConnectionStatusMonitor connectionStatus = runningConnections.get(i);
                    if(!connectionStatus.getThread().isAlive()) {
                        System.out.println("Connection with ID " + connectionStatus.getConnectionID() + " has terminated.");
                        runningConnections.remove(i);
                    }
                }

                // Accepting new connections
                try {
                    Socket clientSocket = ss.accept();
                    currentConnectionID++;

                    String secretWord = generateSecretWord();
                    ClientConnection client = new ClientConnection(clientSocket, secretWord, currentConnectionID);

                    Thread clientThread = new Thread(client);
                    runningConnections.add(new ConnectionStatusMonitor(clientThread, currentConnectionID));
                    clientThread.start();
                    
                    System.out.println("Connection with ID "+ currentConnectionID + " was established with server.");
                } catch (IOException  e2) {
                    System.err.println("Error accepting client connection. Failing connection ID : " + (currentConnectionID+1));
                    e2.printStackTrace();
                }
            }
        } catch (IOException e1) { 
            System.err.println("Could not bind to port " + PORT);
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
