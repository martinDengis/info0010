/* TODO:
 * - Check that guess exists
 */

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

/**
 * The HttpHandler class is responsible for handling HTTP requests from clients.
 * It implements the Runnable interface to allow for concurrent handling of requests.
 */
public class HttpHandler implements Runnable {
    private final int serverID;
    private final Socket clientSocket;
    private boolean newSession;
    private String sessionID;
    private Map<String, String> headers;
    private char[] buffer;
    private boolean isChunked;
    private boolean isRequestGuess;
    private String uri;
    private String guess;

    public HttpHandler(int serverID, Socket clientSocket) {
        this.serverID = serverID;
        this.clientSocket = clientSocket;
        this.newSession = false;
        this.sessionID = "";
        this.buffer = null;
        this.isChunked = false;
        this.headers = new HashMap<String, String>();
        this.isRequestGuess = false;
        this.uri = "";
        this.guess = "";
    }

    /**
     * This method is responsible for handling the incoming HTTP request and processing it.
     * It reads the request line, calls the handleRequest method to process the request,
     * and sends an error response if the request is not successfully handled.
     * Finally, it closes the reader, writer, and client socket.
     */
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

    /**
     * Handles an HTTP request.
     *
     * @param requestLine The request line of the HTTP request.
     * @param reader      The BufferedReader used to read the request body.
     * @param writer      The PrintWriter used to send the HTTP response.
     * @return true if the request was successfully handled, false otherwise.
     */
    private boolean handleRequest(String requestLine, BufferedReader reader, PrintWriter writer) {
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

        int currAttempt = WordleServer.getSessionData(this.sessionID).getAttempts();
        if (currAttempt == 5) { return false; }

        // Read the HTTP request body
        String body = getBody(reader); // what to do with body ? what is body ?

        // Process the HTTP request
        String response;
        if (this.isRequestGuess)
            response = responseBuilder(this.guess); // The response must match with AJAX request
        else response = "";
        
        // Send the HTTP response
        sendHttpResponse(writer, 200, "text/html", response);
        return true;
    }

    /**
     * Validates the format of the HTTP request and its headers.
     * 
     * @param requestLine The HTTP request line.
     * @param reader      The BufferedReader to read the HTTP headers.
     * @param writer      The PrintWriter to send error responses.
     * @return True if the format is valid, false otherwise.
     */
    private boolean formatCheck(String requestLine, BufferedReader reader, PrintWriter writer) {
        // Validate the HTTP request line
        if (!requestLineCheck(requestLine, reader, writer)) return false;

        // Read and validate the HTTP headers
        String line;
        try {
            // Retrieve headers
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                // Invalid header format
                if (!line.matches("^[^:]+: .*$")) {
                    sendErrorResponse(writer, 400);
                    return false;
                } else {
                    // Extract the header name and value
                    String[] header = line.split(": ", 2);
                    this.headers.put(header[0], header[1]);
                }
            }

            return headersCheck(writer);

        } catch (IOException e) { 
            e.printStackTrace(); 
            return false;
        }
    }

    /**
     * Validates the format of the HTTP request line.
     * 
     * @param requestLine The HTTP request line.
     * @param reader      The BufferedReader to read the HTTP headers.
     * @param writer      The PrintWriter to send error responses.
     * @return True if the format is valid, false otherwise.
     */
    public boolean requestLineCheck(String requestLine, BufferedReader reader, PrintWriter writer) {
        if (requestLine == null || !requestLine.matches("^[A-Z]+ .* HTTP/1\\.1$")) {
            // Invalid request format
            sendErrorResponse(writer, 400);
            return false;
        } 
        else {
            // Extract the request method, URI and HTTP version
            String[] request = requestLine.split(" ", 3);
            String method = request[0];
            String uri = request[1];
            String version = request[2];

            // Check if the HTTP method is allowed
            if (!isMethodAllowed(method)) {
                sendErrorResponse(writer, 405);
                return false;
            }
            
            // Check if the URI is valid
            if (!isURIValid(uri)) {
                sendErrorResponse(writer, 404);
                return false;
            }

            // Check if the HTTP version is supported
            if (!version.equals("HTTP/1.1")) {
                sendErrorResponse(writer, 505);
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the given HTTP method is allowed.
     *
     * @param method the HTTP method to check
     * @return true if the method is allowed, false otherwise
     */
    private boolean isMethodAllowed(String method) {
        ArrayList<String> allowedMethods = new ArrayList<>(Arrays.asList("GET", "HEAD", "POST", "PUT", "DELETE", "CONNECT", "OPTIONS"));
        if (!allowedMethods.contains(method)) return false;
        return true;
    }

    /**
     * Checks if the given URI is valid.
     *
     * @param uri the URI to check
     * @return true if the URI is valid, false otherwise
     */
    private boolean isURIValid(String uri, PrintWriter writer) {
        if(uri.matches("^/$"))
            sendErrorResponse(writer, 303);
        else if (uri.matches("^/play\\.html$")) {
            this.uri = "/play.html";
            return true;
        }
        else if (uri.matches("^/guess\\?word=[a-z]{5}$")) {
            this.uri = uri;
            this.isRequestGuess = true;
            this.guess = uri.split("=")[1].toLowerCase();
            return true;
        }
        
        return false;
    }
 
    /**
     * Checks the headers of the HTTP request.
     * 
     * @param writer the PrintWriter used to send the response
     * @return true if the headers are valid, false otherwise
     */
    public boolean headersCheck(PrintWriter writer) {
        // Retrieve content length
        if (!this.headers.containsKey("Content-Length")) {
            // Content-Length header not found
            sendErrorResponse(writer, 411);
            return false;
        } else {
            int length = Integer.parseInt(this.headers.get("Content-Length"));
            this.buffer = new char[length];
        }

        if (this.headers.get("Transfer-Encoding") != null && this.headers.get("Transfer-Encoding").contains("chunked"))
            this.isChunked = true;

        // Process headers in search of existing session
        if (this.headers.containsKey("Cookie")) {
            // Extract the session ID from the Cookie header
            String[] session = this.headers.get("Cookie").split("=", 2);
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

        if (this.headers.containsKey("X-Requested-With")) {
            // Check if the request is an AJAX request
            if (!this.headers.get("X-Requested-With").equals("XMLHttpRequest")) {
                // Invalid request format
                sendErrorResponse(writer, 400);
                return false;
            }
        }

        return true;
    }

    /**
     * Reads the body of the HTTP request from the provided BufferedReader and returns it as a String.
     *
     * @param reader the BufferedReader used to read the request body
     * @return the body of the HTTP request as a String
     */
    private String getBody(BufferedReader reader) {
        try {
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
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Reads the chunked body from the provided BufferedReader and returns it as a String.
     *
     * @param reader the BufferedReader to read the chunked body from
     * @return the chunked body as a String
     */
    private String getChunkedBody(BufferedReader reader) {
        try {
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
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Sends an HTTP response with the specified status code, content type, and content.
     *
     * @param writer      the PrintWriter used to write the response
     * @param statusCode  the status code of the HTTP response
     * @param contentType the content type of the response
     * @param content     the content of the response
     */
    private void sendHttpResponse(PrintWriter writer, int statusCode, String contentType, String content) {
        // Prepare the HTTP response headers
        Map<String, String> responseHeaders = new HashMap<String, String>();
        responseHeaders.put("Date", new Date().toString());
        responseHeaders.put("Server", String.valueOf(this.serverID));
        if (this.newSession) responseHeaders.put("Set-Cookie", "SESSID=" + this.sessionID + "; path=/");

        String statusMessage = getStatusMessage(statusCode);

        writer.println("HTTP/1.1 " + statusCode + " " + statusMessage);
        writer.println("Content-Type: " + contentType);
        writer.println("Content-Length: " + content.length());
        // Send additional headers : namely Date and Server ID
        for (Map.Entry<String, String> header : responseHeaders.entrySet())
            writer.println(header.getKey() + ": " + header.getValue());

        writer.println(WordleServer.getSessionData(this.sessionID));

        writer.println();
        writer.println(content);
        writer.flush();
    }

    /**
     * Sends an error response to the client.
     * 
     * @param writer     the PrintWriter object used to write the response
     * @param statusCode the HTTP status code of the error response
     */
    private void sendErrorResponse(PrintWriter writer, int statusCode) {
        String statusMessage = getStatusMessage(statusCode);

        HTML htmlGenerator = new HTML();
        String error = htmlGenerator.generateErrorPage(statusCode);

        writer.println("HTTP/1.1 " + statusCode + " " + statusMessage);
        writer.println("Content-Type: text/plain");
        writer.println("Content-Length: 0");
        if (statusCode == 303) writer.println("Location: http://localhost:8010/play.html"); // à revoir
        writer.println(error);
        writer.println();
        writer.flush();
    }

    /**
     * Returns the status message corresponding to the given status code.
     *
     * @param statusCode the HTTP status code
     * @return the status message
     */
    private static String getStatusMessage(int statusCode) {
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

    /**
     * Generates a secret word by randomly selecting a word from the word list.
     *
     * @return the generated secret word
     */
    private static String generateSecretWord() {
        ArrayList<String> wordList = new ArrayList<String>(WordleWordSet.WORD_SET);
        Random random = new Random();
        int index = random.nextInt(wordList.size());
        return wordList.get(index);
    }

    /**
     * Builds a response string based on the provided guess.
     * The response string consists of characters representing the correctness of each letter in the guess.
     * The characters 'G', 'Y', and 'B' represent well-placed letters, misplaced letters, and incorrect letters respectively.
     *
     * @param guess the guess made by the player
     * @return the response string indicating the correctness of each letter in the guess
     */
    private String responseBuilder(String guess) {
        // Initialise response and tracking arrays
        char[] pattern = new char[5];
        boolean[] usedInGuess = new boolean[5];
        boolean[] usedInSecret = new boolean[5];
    
        // GREEN: Mark well-placed letters
        for (int i = 0; i < 5; i++) {
            if (guess.charAt(i) == WordleServer.getSecretWord(this.sessionID).charAt(i)) {
                pattern[i] = 'G';
                usedInGuess[i] = usedInSecret[i] = true;
            }
        }
    
        // YELLOW: Mark misplaced letters
        for (int i = 0; i < 5; i++) {
            if (!usedInGuess[i]) {
                for (int j = 0; j < 5; j++) {
                    if (!usedInSecret[j] && guess.charAt(i) == WordleServer.getSecretWord(this.sessionID).charAt(j)) {
                        pattern[i] = 'Y';
                        usedInGuess[i] = usedInSecret[j] = true;
                        break;
                    }
                }
            }
        }
    
        // BLACK: Mark incorrect letters
        for (int i = 0; i < 5; i++)
            if (!usedInGuess[i]) { pattern[i] = 'B'; }

        String response = new String(pattern);
        return response;
    }

}
