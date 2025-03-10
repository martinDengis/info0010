import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * The WordleServer class represents the server component of the Wordle game.
 * It handles incoming client connections, manages game sessions, and provides
 * methods to access and manipulate session data.
 */
public class WordleServer {
    private static final int SERVER_ID = new Random().nextInt(9999);
    private static final int PORT = 8008;
    private static final int MAX_CHUNCK_SIZE = 128;
    private static final int MAX_ATTEMPTS = 5;
    private static final Map<String, SessionData> SESSIONS = new ConcurrentHashMap<>(); // ConcurrentHashMap ensures thread safety

    public static void main(String[] args) {
        // Create a thread pool with X threads
        int maxThreads = Integer.parseInt(args[0]);
        ExecutorService executorService = Executors.newFixedThreadPool(maxThreads);
    
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("-- Wordle HTTP Server is listening on port " + PORT + ".");

            while (true) {
                // Accepting new connections
                try {
                    Socket clientSocket = serverSocket.accept();
                    // Handle HTTP request in a separate thread
                    HttpHandler httpHandler = new HttpHandler(getServerID(), clientSocket);
                    executorService.execute(httpHandler);
                } catch (IOException ioe2) {
                    System.err.println("-- Error accepting client connection.");
                    ioe2.printStackTrace();
                }
            }
        } catch (IOException ioe1) {
            System.err.println("-- Could not bind to port " + PORT);
            ioe1.printStackTrace();
            System.exit(1);
        }
    }

    // Getters
    public static int getServerID() { return SERVER_ID; }
    public static int getPort() { return PORT; }
    public static int getMaxChunckSize() { return MAX_CHUNCK_SIZE; }
    public static int getMaxAttempts() { return MAX_ATTEMPTS; }

    // Methods to manage SESSIONS mapping
    public static void addSession(String id, SessionData session) {
        if (id == null || session == null)
            throw new IllegalArgumentException("Neither id nor session can be null");

        SESSIONS.put(id, session);
    }
    public static SessionData getSessionData(String id) { return SESSIONS.get(id); }
    public static String getFullGameState(String id) { return SESSIONS.get(id).getFullGameState(); }
    public static String getCurrGameState(String id, int currGS) { return SESSIONS.get(id).getCurrGameState(currGS); }
    public static String getSecretWord(String id) { return SESSIONS.get(id).getSecretWord(); }
    public static void removeSession(String id) { SESSIONS.remove(id); }
    public static void addGameState(String id, String guess, String color) { SESSIONS.get(id).addGameState(guess, color); }
    public static boolean hasSession(String id) { return SESSIONS.containsKey(id); }
    public static void printSESSION(String id) {
        System.out.println("SESSION INFO:");
        System.out.println("  id: " + id);
        System.out.println("  Secret Word:" + SESSIONS.get(id).getSecretWord());
    }
}
