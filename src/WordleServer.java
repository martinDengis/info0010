import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class WordleServer {
    private static final int SERVER_ID = new Random().nextInt(9999);
    private static final int PORT = 8008;
    private static final Map<String, SessionData> sessions = new ConcurrentHashMap<>(); // ConcurrentHashMap ensures thread safety

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("-- Wordle HTTP Server is listening on port " + PORT + ".");

            while (true) {
                // Accepting new connections
                try {
                    Socket clientSocket = serverSocket.accept();
                    // Handle HTTP request in a separate thread
                    HttpHandler httpHandler = new HttpHandler(getServerID(), clientSocket);
                    Thread httpThread = new Thread(httpHandler);
                    httpThread.start();
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

    public static int getServerID() { return SERVER_ID; }

    // Methods to manage sessions mapping
    public static void addSession(String id, SessionData session) {
        if (id == null || session == null)
            throw new IllegalArgumentException("Neither id nor session can be null");

        sessions.put(id, session);
    }
    public static SessionData getSessionData(String id) { return sessions.get(id); }
    public static void removeSession(String id) { sessions.remove(id); }
    public static boolean hasSession(String id) { return sessions.containsKey(id); }
    public static String getSecretWord(String id) { return sessions.get(id).getSecretWord(); }
}
