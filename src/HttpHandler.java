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
    private String guess;
    private boolean isJavaScriptEnabled;
    private int rowID;

    public HttpHandler(int serverID, Socket clientSocket) {
        this.serverID = serverID;
        this.clientSocket = clientSocket;
        this.newSession = false;
        this.sessionID = "";
        this.buffer = null;
        this.isChunked = false;
        this.headers = new HashMap<String, String>();
        this.isRequestGuess = false;
        this.guess = "";
        this.isJavaScriptEnabled = true;
        this.rowID = -1; // -1 means no rowID (initial state)
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
            handleRequest(requestLine, reader, writer);
            // if (!success) sendErrorResponse(writer, 501);

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
    private void handleRequest(String requestLine, BufferedReader reader, PrintWriter writer) {
        // Validate the HTTP request format
        boolean success = formatCheck(requestLine, reader, writer);
        if (!success) return;

        // At this point, if no session ID was found, we generate a new one
        if (this.sessionID.isEmpty()) {
            this.newSession = true;
            this.sessionID = UUID.randomUUID().toString();

            // Create a new entry in the sessions mapping
            SessionData sessionData = new SessionData(generateSecretWord());
            WordleServer.addSession(this.sessionID, sessionData);
        }

        int currAttempt = WordleServer.getSessionData(this.sessionID).getAttempt();
        if (currAttempt > 5) {
            sendHttpResponse(writer, 200, "application/json", "{\"Status\": \"Gameover\", \"Message\":" + WordleServer.getSecretWord(this.sessionID) +"}");
        };

        // Read the HTTP request body
        // String body = getBody(reader); // what to do with body ? what is body ?

        // Process the HTTP request
        HTML htmlGenerator = new HTML();
        String response;

        // Check if JavaScript is enabled && if the request is a guess
        if (isJavaScriptEnabled && isRequestGuess) {
            String colorPattern = responseBuilder(this.guess);
            WordleServer.addGamestate(this.sessionID, this.guess, colorPattern);
            String currGameState = WordleServer.getCurrGameState(this.sessionID);   // 1:guess:color

            // Check if winning state
            if (colorPattern.equals("GGGGG")) {
                response = "{\"Status\": \"Win\", \"Message\":" + WordleServer.getSecretWord(this.sessionID) +"}";
                sendHttpResponse(writer, 200, "application/json", response);
                return;
            }

            // Check if the current attempt is the last attempt
            if (currAttempt == 5) {
                response = "{\"Status\": \"Gameover\", \"Message\":" + WordleServer.getSecretWord(this.sessionID) +"}";
                sendHttpResponse(writer, 200, "application/json", response);
                return;
            }

            response = "{\"Status\": \"Playing\", \"Message\":" + currGameState +"}";
            sendHttpResponse(writer, 200, "application/json", response);
        }
        // Else it is either a page reload (even with JS enabled) or JS is disabled (POST request)
        else {
            String fullGameState = WordleServer.getFullGameState(this.sessionID);   
            // 0:guess:color;1:guess:color;2:guess:color;3:guess:color;4:guess:color;5:guess:color;
            response = htmlGenerator.generateWordlePage(fullGameState);

            // Send the HTTP response
            sendHttpResponse(writer, 200, "text/html", response);
        }
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
                    headers.put(header[0], header[1]);
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
            if (!isURIValid(uri, writer)) return false;

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
        if(uri.matches("^/$")) {
            sendErrorResponse(writer, 303); // Redirect to /play.html
            return true;
        }
        else if (uri.matches("^/play\\.html$")) return true;
        else if (uri.matches("^/play\\.html/guess\\?word=[a-z]{5}$")) {
            this.isRequestGuess = true;
            this.guess = uri.split("=")[1].toLowerCase();

            if (!isGuessValid(this.guess)) {
                String response = "{\"Status\": \"Invalid\", \"Message\": \"error\"}";
                sendHttpResponse(writer, 200, "application/json", response);
                return false;
            }
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
        if (!headers.containsKey("Content-Length")) {
            // Content-Length header not found
            sendErrorResponse(writer, 411);
            return false;
        } else {
            int length = Integer.parseInt(headers.get("Content-Length"));
            this.buffer = new char[length];
        }

        if (headers.get("Transfer-Encoding") != null && headers.get("Transfer-Encoding").contains("chunked"))
            this.isChunked = true;

        // Process headers in search of existing session
        if (headers.containsKey("Cookie")) {
            // Extract the session ID from the Cookie header
            String[] session = headers.get("Cookie").split("=", 2);
            this.sessionID = session[1];

            // Check if the session ID is valid
            if (!sessionID.matches("^[0-9a-f\\-]+$") || !WordleServer.hasSession(this.sessionID)) {
                // Invalid session ID
                sendErrorResponse(writer, 400);
                return false;
            }

            // Check that session has not expired
            WordleServer.getSessionData(this.sessionID).updateLastActivityTime();
            if(WordleServer.getSessionData(this.sessionID).isExpired()) {
                WordleServer.removeSession(this.sessionID);
                this.sessionID = "";
            }
        }

        // Check if JavaScript is enabled
        if (headers.containsKey("JS-Enabled") && headers.get("JS-Enabled").equals("false"))
            this.isJavaScriptEnabled = false;

        // Check if the request is an AJAX request
        if (headers.containsKey("X-Requested-With") && !headers.get("X-Requested-With").equals("XMLHttpRequest")) {
            // Invalid request format
            sendErrorResponse(writer, 400);
            return false;
        }

        if (headers.containsKey("Row")) {
            String row = headers.get("Row");
            try {
                this.rowID = Integer.parseInt(row);
                if (rowID != -1 && !sessionID.isEmpty()) {
                    // Check that the rowID match current attempt
                    if (rowID != WordleServer.getSessionData(this.sessionID).getAttempt()) {
                        sendErrorResponse(writer, 400);
                        return false;
                    }
                } 
            } 
            catch (NumberFormatException e) {
                sendErrorResponse(writer, 400);
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if a given word is a valid 5-letter word that exists in the WordleWordSet.
     * 
     * @param guess the word to be checked
     * @return true if the word is valid and exists, false otherwise
     */
    private boolean isGuessValid(String guess) {
        // Check if the word is a valid 5-letter word and exists
        return guess.length() == 5 && WordleWordSet.WORD_SET.contains(guess);
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
        if (this.newSession) responseHeaders.put("Set-Cookie", "SESSID=" + this.sessionID + "; path=/; Max-Age=600");

        String statusMessage = getStatusMessage(statusCode);

        writer.println("HTTP/1.1 " + statusCode + " " + statusMessage);
        writer.println("Content-Type: " + contentType);
        writer.println("Content-Length: " + content.length());
        // Send additional headers : namely Date and Server ID
        for (Map.Entry<String, String> header : responseHeaders.entrySet())
            writer.println(header.getKey() + ": " + header.getValue());

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
        if (statusCode == 303) writer.println("Location: http://localhost:8008/play.html");
        writer.println();
        writer.println(error);
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

        String _secret = WordleServer.getSecretWord(this.sessionID).toLowerCase();
        String _guess = guess.toLowerCase();
    
        // GREEN: Mark well-placed letters
        for (int i = 0; i < 5; i++) {
            if (_guess.charAt(i) == _secret.charAt(i)) {
                pattern[i] = 'G';
                usedInGuess[i] = usedInSecret[i] = true;
            }
        }
    
        // YELLOW: Mark misplaced letters
        for (int i = 0; i < 5; i++) {
            if (!usedInGuess[i]) {
                for (int j = 0; j < 5; j++) {
                    if (!usedInSecret[j] && _guess.charAt(i) == _secret.charAt(j)) {
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
