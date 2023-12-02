import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class HttpHandler implements Runnable {
    private final int serverID;
    private final Socket clientSocket;
    private String sessionID;
    private boolean newSession;
    private char[] buffer;
    private boolean isChunked;

    public HttpHandler(int serverID, Socket clientSocket) {
        this.serverID = serverID;
        this.clientSocket = clientSocket;
        this.sessionID = "";
        this.newSession = false;
        this.buffer = null;
        this.isChunked = false;
    }

    @Override
    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), false);

            // Read the HTTP request
            String requestLine = reader.readLine();

            // Process the request
            boolean success = handleRequest(requestLine, reader, writer);
            if (!success) sendErrorResponse(writer, 501);

            reader.close();
            writer.close();
            clientSocket.close();
        } 
        catch (IOException e) { e.printStackTrace(); }
    }

    private boolean handleRequest(String requestLine, BufferedReader reader, PrintWriter writer) {
        try {
            // Validate the HTTP request format
            boolean success = formatCheck(requestLine, reader, writer);
            if (!success) return false;

            // At this point, if no session ID was found, we generate a new one
            if (this.sessionID.isEmpty()) {
                this.newSession = true;
                this.sessionID = UUID.randomUUID().toString();
                
                // Create a new entry in the sessions mapping
                SessionData sessionData = new SessionData(this.sessionID, 600, generateSecretWord());
                WordleServer.addSession(this.sessionID, sessionData);
            }

            // Read the HTTP request body
            String body = getBody(reader);

            // Process the HTTP request
            String response = responseBuilder(body); // The response must match with AJAX request
            
            // Send the HTTP response
            sendHttpResponse(writer, 200, "text/html", response);
            return true;
        } 
        catch (IOException e) {
            System.err.println("An error occurred while processing an HTTP request for session ID " + sessionID);
            e.printStackTrace();
            return false;
        }
    }

    private boolean formatCheck(String requestLine, BufferedReader reader, PrintWriter writer) {
        // Validate the HTTP request line
        if (requestLine == null || !requestLine.matches("^[A-Z]+ .* HTTP/1\\.1$")) {
            // Invalid request format
            sendErrorResponse(writer, 400);
            return false;
        }

        // Retrieve the HTTP method and check if it is valid
        String method = requestLine.split(" ", 3)[0];
        if (!isMethodAllowed(method)) {
            sendErrorResponse(writer, 405);
            return false;
        }

        // Read and validate the HTTP headers
        String line;
        boolean contentLengthFound = false;

        try {
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                // Invalid header format
                if (!line.matches("^[^:]+: .*$")) {
                    sendErrorResponse(writer, 400);
                    return false;
                }
                
                // Retrieve content length
                if (line.matches("^Content-Length: .*")) {
                    contentLengthFound = true;
                    String[] contentLength = line.split(": ", 2);
                    int length = Integer.parseInt(contentLength[1]);
                    this.buffer = new char[length];
                }
                
                if (line.matches("^Transfer-Encoding: .*")) {
                    if (line.contains("chunked")) {
                        this.isChunked = true;
                    }
                }
                // Process headers in search of existing session
                if (line.matches("^Cookie: .*")) {
                    // Extract the session ID from the Cookie header
                    String[] cookie = line.split(": ", 2);
                    String[] session = cookie[1].split("=", 2);
                    this.sessionID = session[1];

                    // Check if the session ID is valid
                    if (!this.sessionID.matches("^[0-9a-f\\-]+$")) {
                        // Invalid session ID
                        sendErrorResponse(writer, 400);
                        return false;
                    } 
                    // Check if the session ID is known
                    else if (!WordleServer.hasSession(this.sessionID)) {
                        // Session ID not found
                        sendErrorResponse(writer, 400);
                        return false;
                    }
                }
            }

            if (!contentLengthFound) {
                // Content-Length header not found
                sendErrorResponse(writer, 411);
                return false;
            }
        } catch (IOException e) { 
            e.printStackTrace(); 
            return false;
        }

        return true;
    }

    private String getBody(BufferedReader reader) throws IOException {
        // Check if the request is chunked
        if (this.isChunked) { return getChunkedBody(reader); }

        // If not chunked, read the body based on Content-Length
        int bytesRead = 0;
        while (bytesRead < this.buffer.length) {
            int result = reader.read(buffer, bytesRead, this.buffer.length - bytesRead);
            if (result == -1) break;
            bytesRead += result;
        }
        return new String(buffer, 0, bytesRead);
    }

    private String getChunkedBody(BufferedReader reader) throws IOException {
        StringBuilder bodyBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            // Parse the chunk size
            int chunkSize = Integer.parseInt(line.trim(), 16); // Chunk sizes are in hex
            if (chunkSize == 0) break;

            // Read the chunk data
            char[] buffer = new char[chunkSize];
            int bytesRead = 0;
            while (bytesRead < chunkSize) {
                int result = reader.read(buffer, bytesRead, chunkSize - bytesRead);
                if (result == -1) break;
                bytesRead += result;
            }
            bodyBuilder.append(buffer, 0, bytesRead);

            // Skip the newline after the chunk
            reader.readLine();
        }
        return bodyBuilder.toString();
    }

    private void sendHttpResponse(PrintWriter writer, int statusCode, String contentType, String content) {
        // Prepare the HTTP response headers
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Date", new Date().toString());
        headers.put("Server", String.valueOf(this.serverID));
        if (this.newSession) headers.put("Set-Cookie", "SESSID=" + this.sessionID + "; path=/");

        String statusMessage = getStatusMessage(statusCode);

        writer.println("HTTP/1.1 " + statusCode + " " + statusMessage);
        writer.println("Content-Type: " + contentType);
        writer.println("Content-Length: " + content.length());
        // Send additional headers : namely Date and Server ID
        for (Map.Entry<String, String> header : headers.entrySet())
            writer.println(header.getKey() + ": " + header.getValue());

        writer.println();
        writer.println(content);
        writer.flush();
    }

    private void sendErrorResponse(PrintWriter writer, int statusCode) {
        String statusMessage = getStatusMessage(statusCode);

        writer.println("HTTP/1.1 " + statusCode + " " + statusMessage);
        writer.println("Content-Type: text/plain");
        writer.println("Content-Length: 0");
        writer.println();
        writer.flush();
    }

    private String getStatusMessage(int statusCode) {
        switch (statusCode) {
            case 200: return "OK";
            case 303: return "See Other";
            case 400: return "Bad Request";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            case 411: return "Length Required";
            case 501: return "Not Implemented";
            case 505: return "HTTP Version Not Supported";
            default: return "Unknown Status";
        }
    }

    private boolean isMethodAllowed(String method) {
        ArrayList<String> allowedMethods = new ArrayList<>(Arrays.asList("GET", "HEAD", "POST", "PUT", "DELETE", "CONNECT", "OPTIONS"));
        if (!allowedMethods.contains(method)) return false;
        return true;
    }

    private static String generateSecretWord() {
        ArrayList<String> wordList = new ArrayList<String>(WordleWordSet.WORD_SET);
        Random random = new Random();
        int index = random.nextInt(wordList.size());
        return wordList.get(index);
    }
}
