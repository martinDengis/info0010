import java.io.PrintWriter;

public class post {
    // ... within the handleRequest method ...
    if (method.equals("POST")) {
        String body = getBody(reader); // Extract the body from the POST request
        String guess = extractGuessFromBody(body); // Extract the guess word from the body
        String feedback = processGuess(guess); // Process the guess and get feedback
        sendPostResponse(writer, feedback); // Send an appropriate response
        return true;
    }
    private String extractGuessFromBody(String body) {
        // Assuming the body is URL-encoded and the guess parameter is named 'guess'
        // Simple parsing logic to extract the guess word
        String[] pairs = body.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue[0].equals("guess")) {
                return keyValue[1]; // Return the guessed word
            }
        }
        return ""; // Or handle error appropriately
    }
    private void sendPostResponse(PrintWriter writer, String feedback) {
        String responseContent = buildPostResponseContent(feedback); // Build the HTML response
        sendHttpResponse(writer, 200, "text/html", responseContent);
    }

    private String buildPostResponseContent(String feedback) {
        // Build an HTML response incorporating the feedback
        // This might involve showing the game board with the updated state
        return "<html>...</html>"; // HTML content with the game's updated state
    }

    // WE NEED TO STORE THE GAME STATE

    private void sendPostResponse(PrintWriter writer, String feedback, SessionData sessionData) {
        HTML htmlGenerator = new HTML();
        String responseContent = htmlGenerator.buildUpdatedHtml(sessionData);
        sendHttpResponse(writer, 200, "text/html", responseContent);
    }
    
}
