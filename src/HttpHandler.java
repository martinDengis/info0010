import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class HttpHandler implements Runnable {
    private final Socket clientSocket;
    private final int connectionID;

    public HttpHandler(Socket clientSocket, int connectionID) {
        this.clientSocket = clientSocket;
        this.connectionID = connectionID;
    }

    @Override
    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);

            // Read the HTTP request
            String requestLine = reader.readLine();
            System.out.println("Received HTTP request from connection ID " + connectionID + ": " + requestLine);

            // Process the request (you need to implement this part)

            // Respond with a simple HTTP response for now
            String httpResponse = "HTTP/1.1 200 OK\r\nContent-Length: 12\r\n\r\nHello, World!";
            writer.print(httpResponse);
            writer.flush();

            reader.close();
            writer.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
