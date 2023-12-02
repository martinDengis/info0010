import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class WordleServer {
    private static final int PORT = 8008;

    public static void main(String[] args) {
        int currentConnectionID = -1;
        List<ConnectionStatusMonitor> runningConnections = new ArrayList<>();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("-- Wordle HTTP Server is listening on port " + PORT + ".");

            while (true) {
                isTerminated(runningConnections);

                try {
                    Socket clientSocket = serverSocket.accept();
                    currentConnectionID++;

                    // Handle HTTP request in a separate thread
                    HttpHandler httpHandler = new HttpHandler(clientSocket, currentConnectionID);
                    Thread httpThread = new Thread(httpHandler);
                    runningConnections.add(new ConnectionStatusMonitor(httpThread, currentConnectionID));
                    httpThread.start();

                    System.out.println("Connection with ID " + currentConnectionID + " was established with server.");
                } catch (IOException e) {
                    System.err.println("Error accepting client connection. Failing connection ID: " + (currentConnectionID + 1));
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.err.println("-- Could not bind to port " + PORT);
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void isTerminated(List<ConnectionStatusMonitor> runningConnections) {
        for(int i = 0; i < runningConnections.size(); i++) {
            ConnectionStatusMonitor connectionStatus = runningConnections.get(i);
            if(!connectionStatus.getThread().isAlive()) {
                System.out.println("Connection with ID " + connectionStatus.getConnectionID() + " has terminated.");
                runningConnections.remove(i);
                i--; // To avoid skipping next connection when reentering the loop
            }
        }
    }
}
