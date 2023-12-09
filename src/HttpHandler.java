import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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
    private boolean newSession = false;
    private boolean isChunked = false;
    private boolean isRequestGuess = false;
    private boolean isJavaScriptEnabled = true;
    private int rowID = -1; // -1 means no rowID (initial state)
    private String sessionID = "";
    private String guess = "";
    private char[] buffer = null;
    private final Map<String, String> headers = new HashMap<String, String>();
    private String method;

    public HttpHandler(int serverID, Socket clientSocket) {
        this.serverID = serverID;
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), false);

            // Read the HTTP request
            String requestLine = reader.readLine();

            // Process the request
            handleRequest(requestLine, reader, writer);

            reader.close();
            writer.close();
            clientSocket.close();
        } 
        catch (IOException e) { e.printStackTrace(); }
    }


    // PROCESSORS METHODS ------------------------------------------------------------
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

        // At this point, if no session ID was found, we generate a new session
        if (this.sessionID.isEmpty()) {
            this.newSession = true;
            this.sessionID = UUID.randomUUID().toString();

            // Create a new entry in the sessions mapping
            SessionData sessionData = new SessionData(generateSecretWord());
            WordleServer.addSession(this.sessionID, sessionData);
        }

        // Check if the game is over
        int currAttempt = WordleServer.getSessionData(this.sessionID).getAttempt();
        if (currAttempt > 5) {
            WordleServer.getSessionData(this.sessionID).setStatus("Gameover");
            String response = "{\"Status\": \"Gameover\", \"Message\":\"" + WordleServer.getSecretWord(this.sessionID) +"\"}";
            sendHttpResponse(writer, 200, "application/json", response);
            return;
        };

        // Check if JavaScript is enabled && if the request is a guess
        // Else it is either a page reload (even with JS enabled) or JS is disabled (POST request)
        if (isJavaScriptEnabled && isRequestGuess) { pleaseRespond(writer, currAttempt, true); }
        else { pleaseRespond(writer, currAttempt, false); }    
    }

    /**
     * Validates the format of the HTTP request and its headers.
     * Called by handleRequest method.
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
                    System.err.println("117 formatCheck: " + line + " :: Invalid header format");
                    sendErrorResponse(writer, 400);
                    return false;
                } 
                // Extract the header name and value
                else {
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
     * Called by formatCheck method.
     * 
     * @param requestLine The HTTP request line.
     * @param reader      The BufferedReader to read the HTTP headers.
     * @param writer      The PrintWriter to send error responses.
     * @return True if the format is valid, false otherwise.
     */
    public boolean requestLineCheck(String requestLine, BufferedReader reader, PrintWriter writer) {
        if (requestLine == null || !requestLine.matches("^[A-Z]+ .* HTTP/1\\.1$")) {
            // Invalid request format
            System.err.println("147 requestLineCheck : " + requestLine + " ::Invalid request format");
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
                System.err.println("160 requestLineCheck : " + method + " ::Invalid HTTP method");
                sendErrorResponse(writer, 405);
                return false;
            }
            
            // Check if the URI is valid
            if (!isURIValid(uri, writer)) return false;

            // Check if the HTTP version is supported
            if (!version.equals("HTTP/1.1")) {
                System.err.println("170 requestLineCheck : " + version + " ::Invalid HTTP version");
                sendErrorResponse(writer, 505);
                return false;
            }
        }
        return true;
    }

    /**
     * Checks the headers of the HTTP request.
     * Called by formatCheck method.
     * 
     * @param writer the PrintWriter used to send the response
     * @return true if the headers are valid, false otherwise
     */
    public boolean headersCheck(PrintWriter writer) {
        // Retrieve content length
        if (!headers.containsKey("Content-Length") && !this.method.equals("GET") && !this.method.equals("HEAD")) {
            // Content-Length header not found
            System.err.println("189 headersCheck : " + method + " ::Content-Length header not found");
            sendErrorResponse(writer, 411);
            return false;
        } 
        else if (headers.containsKey("Content-Length")) {
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
            if (!sessionID.matches("^[0-9a-f\\-]+$")) {
                // Invalid session ID
                System.err.println("210 headersCheck : " + sessionID + " ::Invalid session ID");
                sendErrorResponse(writer, 400);
                return false;
            } 
            // If sessionID exists on client but not on server, make as if new session (will override the cookie on browser)
            else if (!WordleServer.hasSession(this.sessionID)) {
                this.newSession = true;

                // Create a new entry in the sessions mapping
                SessionData sessionData = new SessionData(generateSecretWord());
                WordleServer.addSession(this.sessionID, sessionData);
            }

            // Check that session has not expired or is not in a winning/gameover state
            WordleServer.getSessionData(this.sessionID).updateLastActivityTime();
            boolean isExpired = WordleServer.getSessionData(this.sessionID).isExpired();
            String status = WordleServer.getSessionData(this.sessionID).getStatus();
            if(isExpired || status.equals("Gameover") || status.equals("Win")) {
                WordleServer.removeSession(this.sessionID);
                this.sessionID = "";    // Good behaviour ? Will start a new game but should identify this case to display a message
            }
        }

        // Check if JavaScript is enabled
        if (headers.containsKey("JS-Enabled") && headers.get("JS-Enabled").equals("false"))
            this.isJavaScriptEnabled = false;

        // Check if the request is an AJAX request
        if (headers.containsKey("X-Requested-With") && !headers.get("X-Requested-With").equals("XMLHttpRequest")) {
            // Invalid request format
            System.err.println("232 headersCheck : " + headers.get("X-Requested-With") + " ::Invalid request format");
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
                        System.err.println("244 headersCheck : " + rowID + " ::Invalid row ID");
                        sendErrorResponse(writer, 400);
                        return false;
                    }
                } 
            } 
            catch (NumberFormatException e) {
                System.err.println("251 headersCheck : " + row + " ::NumberFormatException");
                sendErrorResponse(writer, 400);
                return false;
            }
        }

        return true;
    }

    /**
     * Handles the HTTP response based on the current game state and request type.
     * If the request type is JavaScript and guess, it updates the game state, checks for a winning state,
     * and sends the appropriate JSON response.
     * If the request type is not JavaScript and guess, it updates the game state, checks for a final state,
     * and sends the appropriate HTML response.
     * Called by handleRequest method.
     *
     * @param writer      the PrintWriter object used to send the HTTP response
     * @param currAttempt the current attempt number
     * @param isJSandGuess true if the request type is JavaScript and guess, false otherwise
     */
    public void pleaseRespond(PrintWriter writer, int currAttempt, boolean isJSandGuess) {
        // Process the request
        HTML htmlGenerator = new HTML();
        String response;
        
        if(isJSandGuess) {
            // Update game state
            String colorPattern = responseBuilder(this.guess);
            WordleServer.addGameState(this.sessionID, this.guess, colorPattern);

            // Retrieve the current game state -> 1:guess:color
            String currGameState = WordleServer.getCurrGameState(this.sessionID, currAttempt);

            // Check if winning state
            if (colorPattern.equals("GGGGG")) {
                WordleServer.getSessionData(this.sessionID).setStatus("Win");
                response = "{\"Status\": \"Win\", \"Message\":\"" + WordleServer.getSecretWord(this.sessionID) + "\"}";
                sendHttpResponse(writer, 200, "application/json", response);
                return;
            }

            // Check if the current attempt is the last attempt
            if (currAttempt == 5) {
                WordleServer.getSessionData(this.sessionID).setStatus("Gameover");
                response = "{\"Status\": \"Gameover\", \"Message\":\"" + WordleServer.getSecretWord(this.sessionID) + "\"}";
                sendHttpResponse(writer, 200, "application/json", response);
                return;
            }

            response = "{\"Status\": \"Playing\", \"Message\":\"" + currGameState + "\"}";
            sendHttpResponse(writer, 200, "application/json", response);
        }
        else {
            // Update game state
            if (this.isRequestGuess) {
                String colorPattern = responseBuilder(this.guess);
                WordleServer.addGameState(this.sessionID, this.guess, colorPattern);
            }

            // Retrieve the full game state
            // -1:secret:secret;0:guess:color;1:guess:color;2:guess:color;3:guess:color;4:guess:color;5:guess:color;
            String fullGameState = WordleServer.getFullGameState(this.sessionID);

            // Check if final state
            if (fullGameState.contains("GGGGG")) WordleServer.getSessionData(this.sessionID).setStatus("Win");
            else if (currAttempt == 5) WordleServer.getSessionData(this.sessionID).setStatus("Gameover");
            
            // Send the HTTP response
            response = htmlGenerator.generateWordlePage(fullGameState);
            sendHttpResponse(writer, 200, "text/html", response);
        }
    }


    // HELPERS METHODS ------------------------------------------------------------
    /**
     * Sends an HTTP response with the specified status code, content type, and content.
     * Supports Chunked Transfer Encoding.
     *
     * @param writer      the PrintWriter used to write the response
     * @param statusCode  the status code of the HTTP response
     * @param contentType the content type of the response
     * @param content     the content of the response
     */
    private void sendHttpResponse(PrintWriter writer, int statusCode, String contentType, String content) {
        // Check if the content should be chunked and get status message
        boolean isChunked = content.length() > WordleServer.getMaxChunckSize();
        String statusMessage = getStatusMessage(statusCode);

        // Get content-length in bytes
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        int contentLength = contentBytes.length;

        // Prepare the HTTP response headers
        Map<String, String> responseHeaders = new HashMap<>();
        responseHeaders.put("Content-Type", contentType);
        if (isChunked) responseHeaders.put("Transfer-Encoding", "chunked");
        else responseHeaders.put("Content-Length", String.valueOf(contentLength));
        if (this.newSession) responseHeaders.put("Set-Cookie", "SESSID=" + this.sessionID + "; path=/; Max-Age=600");
        responseHeaders.put("Date", new Date().toString());
        responseHeaders.put("Server", String.valueOf(this.serverID));

        // Send the HTTP response
        writer.println("HTTP/1.1 " + statusCode + " " + statusMessage);
        System.out.println("HTTP/1.1 " + statusCode + " " + statusMessage);
        for (Map.Entry<String, String> header : responseHeaders.entrySet()) {
            writer.println(header.getKey() + ": " + header.getValue());
            System.out.println(header.getKey() + ": " + header.getValue());
        }

        writer.println();
        System.out.println();

        if (isChunked) sendContentInChunks(writer, content);
        else {
            writer.println(content);
            writer.flush();
        }

    }

    /**
     * Sends the content in chunks using Chunked Transfer Encoding.
     *
     * @param writer  the PrintWriter used to write the chunks
     * @param content the content to be sent in chunks
     */
    private void sendContentInChunks(PrintWriter writer, String content) {
        int chunkSize = WordleServer.getMaxChunckSize();

        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

        for (int i = 0; i < contentBytes.length; i += chunkSize) {
            int end = Math.min(i + chunkSize, contentBytes.length);
            byte[] chunkBytes = Arrays.copyOfRange(contentBytes, i, end);

            // Convert chunkBytes back to a string for printing
            String chunk = new String(chunkBytes, StandardCharsets.UTF_8);

            writer.println(Integer.toHexString(chunkBytes.length));
            System.out.println(Integer.toHexString(chunkBytes.length));
            writer.println(chunk);
            System.out.println(chunk);
            writer.flush();
        }

        // Send a zero-size chunk to indicate the end of the content
        writer.println("0");
        writer.println();
        System.out.println("0");
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

        // HTML htmlGenerator = new HTML();
        // String error = htmlGenerator.generateErrorPage(statusCode);
        // String error = "error " + String.valueOf(statusCode);

        writer.println("HTTP/1.1 " + statusCode + " " + statusMessage);
        writer.println("Content-Type: text/plain");
        writer.println("Content-Length: 0");
        // writer.println("Content-Length: " + error.length());
        if (statusCode == 303) writer.println("Location: http://localhost:8008/play.html");
        // writer.println();
        // writer.println(error);
        writer.flush();
    }

    /**
     * Checks if the given HTTP method is allowed.
     * Called by requestLineCheck method.
     *
     * @param method the HTTP method to check
     * @return true if the method is allowed, false otherwise
     */
    private boolean isMethodAllowed(String method) {
        this.method = method; 
        return HttpMethod.isMethodAllowed(method); 
    }

    /**
     * Checks if the given URI is valid.
     * Called by requestLineCheck method.
     *
     * @param uri the URI to check
     * @return true if the URI is valid, false otherwise
     */
    private boolean isURIValid(String uri, PrintWriter writer) {
        if(uri.matches("^/$")) {
            System.err.println("430 isURIValid : " + uri + " ::Invalid URI");
            sendErrorResponse(writer, 303); // Redirect to /play.html
            return true;
        }
        else if (uri.matches("^/play\\.html$")) return true;
        else if (uri.matches("^/play\\.html/guess\\?word=[A-Z]{5}$")) {
            this.isRequestGuess = true;
            this.guess = uri.split("=")[1].toLowerCase();

            if (!isGuessValid(this.guess)) {
                String response = "{\"Status\": \"Invalid\", \"Message\": \"Word does not exist. Try another.\"}";
                sendHttpResponse(writer, 200, "application/json", response);
                if (!this.sessionID.isEmpty() && WordleServer.hasSession(sessionID))
                    WordleServer.getSessionData(this.sessionID).decrementAttempts();
                return false;
            }
            return true;
        }
        
        return false;
    }
 
    /**
     * Checks if a given word is a valid 5-letter word that exists in the WordleWordSet.
     * Called by isURIValid method.
     * 
     * @param guess the word to be checked
     * @return true if the word is valid and exists, false otherwise
     */
    private boolean isGuessValid(String guess) { return guess.length() == 5 && WordleWordSet.WORD_SET.contains(guess); }

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
        if (guess == null) return null;

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


    // UNUSED METHODS ------------------------------------------------------------
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

}
