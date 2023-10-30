import java.io.*;
import java.util.*;
import java.net.*;

/** Wordle project (part I).
 * 
 * @Course INFO0010 - Introduction to computer networking
 * @Instructor Pr. Guy Leduc
 * @author Martin Dengis (s193348)
 * @AcademicYear 2023-2024
 * --------------------------------------------------------
 * The WordleServer class provides a simple implementation of a Wordle Server interface.
 * It manages entering connections by delegating to the ClientHandler class
 * each new client as a separate thread.
 * 
 * The class includes the following methods:
 * @method main : Set up Server Socket and handle entering and exiting client via separate Threads.
 * @method generateSecretWord : From class WordleWordSet, randomly set a secret word for each new client.
 * @method isTerminated : Identifiy terminated client connections in a List of running connections.
 */
public class WordleServer {
    private static final int PORT = 2348;
    
    public static void main(String[] args) {
        int currentConnectionID = -1; // Keep track of number of connections
        List<ConnectionStatusMonitor> runningConnections = new ArrayList<>(); // Keep track of running threads

        try (ServerSocket ss = new ServerSocket(PORT)) {
            System.out.println("--Wordle Server is listening on port " + PORT + ".");

            while (true) {
                // Loop identifying terminated connections
                isTerminated(runningConnections);

                // Accepting new connections
                try {
                    Socket clientSocket = ss.accept();
                    currentConnectionID++;

                    String secretWord = generateSecretWord();
                    ClientHandler client = new ClientHandler(clientSocket, secretWord, currentConnectionID);

                    Thread clientThread = new Thread(client);
                    runningConnections.add(new ConnectionStatusMonitor(clientThread, currentConnectionID));
                    clientThread.start();
                    
                    System.out.println("Connection with ID "+ currentConnectionID + " was established with server.");
                } catch (IOException ioe2) {
                    System.err.println("Error accepting client connection. Failing connection ID : " + (currentConnectionID+1));
                    ioe2.printStackTrace();
                }
            }
        } catch (IOException ioe1) { 
            System.err.println("--Could not bind to port " + PORT);
            ioe1.printStackTrace();
            System.exit(1);
        } 
    }

    private static String generateSecretWord() {
        ArrayList<String> wordList = new ArrayList<String>(WordleWordSet.WORD_SET);
        Random random = new Random();
        int index = random.nextInt(wordList.size());
        return wordList.get(index);
    }

    private static void isTerminated(List<ConnectionStatusMonitor> runningConnections) {
        for(int i = 0; i < runningConnections.size(); i++) {
            ConnectionStatusMonitor connectionStatus = runningConnections.get(i);
            if(!connectionStatus.getThread().isAlive()) {
                System.out.println("Connection with ID " + connectionStatus.getConnectionID() + " has terminated.");
                runningConnections.remove(i);
                i--;
            }
        }
    }
}
