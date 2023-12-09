import java.io.IOException;

public class HTML {
    public String generateWordlePage(String gameState) { // TODO generateErrorPage(getStatusMessage)
        // Image to base64
        String base64Image = "";
        try { base64Image = ImageEncoder.encodeImageToBase64("logo.png"); } 
        catch (IOException e) { e.printStackTrace(); } 
        
        String title = "Wordle Game";
        String header = "<header><img src=\"data:image/png;base64,"+base64Image+"\" alt=\"WORDLE\"></header>";
        
        String wordleBoard = "";
        boolean isNewGame = true;
        String[] parts = gameState.split(";");
        if (parts[1].equals("0::")) isNewGame = true;
        // for (int i = 1; i < parts.length; i++) { // Start from index 1 to skip the secret word
        //     if (!parts[i].equals(i-1+"::")) {
        //         isNewGame = false;
        //         break;
        //     }
        // }

        if (isNewGame) {
            wordleBoard = generateWordleBoard(); // For a new game
        } else {
            wordleBoard = generateWordleBoardWithState(gameState); // For a returning player
        }

        String keyboard = generateKeyboard();
        String styles = generateStyles();
        String fallbackForm = fallbackForm();

        String modalHtml =                                              // TODO deal with style
                            "<div id='gameModal' class='modal'>" +
                            "   <div class='modal-content'>" +
                            "       <span class='close'>&times;</span>" +
                            "       <p id='modalText'></p>" +
                            "   </div>" +
                            "</div>";

        String removeLastLetterFunction = 
                            "function removeLastLetter() {" +
                            "    if (currentCell > 0) {" +
                            "        currentCell--;" +
                            "        currentGuess = currentGuess.slice(0, -1);" +
                            "        const cell = document.getElementById(`cell-${currentRow}-${currentCell}`);" +
                            "        if (cell) {" +
                            "            cell.textContent = '';" +
                            "            cell.classList.remove('filled');" +
                            "        }" +
                            "    }" +
                            "}";  
                        
        String keydownEventListener =
                            "document.addEventListener('keydown', (event) => {" +
                            "    const keyName = event.key.toUpperCase();" +
                            "    if (keyName.match(/^[A-Z]$/) && currentGuess.length < 5) {" +
                            "        event.preventDefault();" +
                            "        keyPressed(keyName);" +
                            "    } else if (keyName === 'ENTER') {" +
                            "        event.preventDefault();" +
                            "        if (currentGuess.length === 5) {" +
                            "            onSubmitGuess();" +
                            "        } else {" +
                            "            alert('Your word is incomplete. Please enter 5 letters.');" +
                            "        }" +
                            "    } else if (keyName === 'BACKSPACE' && currentGuess.length > 0) {" +
                            "        event.preventDefault();" +
                            "        removeLastLetter();" +
                            "    }" +
                            "});";                      
                                
        String keyPressedFunction =
                            "function keyPressed(key) {" +
                            "    if (!key.match(/^[A-Z]$/)) return; " + 
                            "    if (currentGuess.length < 5) {" +
                            "        currentGuess += key;" +
                            "        fillCell(key);" +
                            "    }" +
                            "}";                      
        
        String onSubmitGuess = 
                            "function onSubmitGuess() {" +
                            "  if (currentGuess.length === 5) {" +
                            "      sendGuess(currentGuess);" +
                            "  } else {" +
                            "      alert('Your word is incomplete. Please enter 5 letters.');" +
                            "  }" +
                            "}";
        
        String onEraseFunction =
                            "function onErase() {" +
                            "    removeLastLetter();" +
                            "}";

        String sendGuess =                             
                            "function sendGuess(guess) {" +
                            "    const isJSEnabled = typeof window.addEventListener === 'function';" +
                            "    const headers = new Headers({"+
                            "        'JS-Enabled': isJSEnabled.toString(),"+
                            "        'Row': currentRow.toString()"+
                            "    });"+
                            "    console.log('Sending guess:', guess, 'Row:', currentRow);" + // Console log for debugging
                            "    fetch(`/play.html/guess?word=${guess}`, {" +
                            "        method: 'GET'," +
                            "        headers: headers" +
                            "    })" +
                            "    .then(response => response.json())" + // Processing text response
                            "    .then(data => {" +
                            "        console.log(data);" +
                            "        return data;" +
                            "    })" +
                            "    .then(processServerResponse)" +
                            "    .catch(error => {" +
                            // "        console.error('Error:', error);" +
                            "        alert('An error occurred while submitting the guess.');" +
                            "    });" +
                            "}";

        String processServerResponse =
                            "function processServerResponse(response) {"+
                            // "   var response = JSON.parse(data);" + // Assuming 'data' is a JSON string from the server
                            "   switch(response.Status) {" +
                            "       case 'Invalid':" +
                            "           alert(response.Message);" +
                            "           break;" +
                            "       case 'Gameover':" +
                            "           userLost(response.Message);" +
                            "           break;" +
                            "       case 'Win':" +
                            "           userWon(response.Message);" +
                            "           break;" +
                            "       case 'Playing':" +
                            "           var parts = response.Message.split(':');" +
                            "           updateBoardWithFeedback(parts[2]);"+ // Assuming the feedback is in the third part
                            "           break;" +
                            "       default:" +
                            "           console.error('Unknown status from server');" +
                            "    }"+
                            "}";

        String updateBoardWithFeedback =
                            "function updateBoardWithFeedback(feedback) {" +
                            "    for (let i = 0; i < feedback.length; i++) {" +
                            "        const cell = document.getElementById(`cell-${currentRow}-${i}`);" +
                            "        const keyLetter = currentGuess[i].toUpperCase();" +
                            "        const key = document.querySelector(`.key[data-letter='${keyLetter}']`);" +
                            "        if (cell) {" +
                            "            cell.className = 'word-cell'; " + // Reset to default class
                            "            switch (feedback.charAt(i)) {" +
                            "                case 'G':" +
                            "                    cell.classList.add('green');" +
                            "                    if (key) key.classList.add('green');" +
                            "                    break;" +
                            "                case 'Y':" +
                            "                    cell.classList.add('yellow');" +
                            "                    if (key) key.classList.add('yellow');" +
                            "                    break;" +
                            "                case 'B':" +
                            "                    cell.classList.add('darkened');" +
                            "                    if (key) key.classList.add('darkened');" +
                            "                    break;" +
                            "            }" +
                            "        }" +
                            "    }" +
                            "    currentRow++;" + // Prepare for the next guess
                            "}";
        
        String showModalFunction =
                            "function showModal(message) {" +
                            "  var gameModal = document.getElementById('gameModal');" +
                            "  var modalText = document.getElementById('modalText');" +
                            "  modalText.textContent = message;" +
                            "  gameModal.style.display = 'block';" +
                            "}";
                        
        String userWonFunction =
                            "function userWon() {" +
                            "  showModal('YOU WON!');" +
                            "}";
                        
        String userLostFunction =
                            "function userLost(secretWord) {" +
                            "  showModal('GAME OVER. Secret word = ' + secretWord);" +
                            "}";
                        
        String closeModalFunction =
                            "var span = document.getElementsByClassName('close')[0];" +
                            "span.onclick = function() {" +
                            "  var gameModal = document.getElementById('gameModal');" +
                            "  gameModal.style.display = 'none';" +
                            "};";
                        
        // String restartGameFunction =
        //                     "function restartGame() {" +
        //                     "  // Code to reset the game goes here" +
        //                     "}";
                        

        String script = "<script>" +
                        "function fillCell(key) {" +
                        "    if (currentCell < 5) {" +
                        "        const cell = document.getElementById(`cell-${currentRow}-${currentCell}`);" +
                        "        if (cell) {" +
                        "            cell.textContent = key;" +
                        "            cell.classList.add('filled');" +
                        "            cell.style.animation = 'none';" + 
                        "            setTimeout(() => {" +
                        "            cell.style.animation = ''; " +
                        "            }, 10);" +
                        "            currentCell++;" +
                        "        }" +
                        "    }" +
                        "}" +
                        removeLastLetterFunction +
                        onEraseFunction + 
                        keyPressedFunction +
                        showModalFunction+
                        userWonFunction +
                        userLostFunction +
                        processServerResponse +
                        sendGuess +
                        onSubmitGuess + 
                        updateBoardWithFeedback +
                        closeModalFunction + 
                        //restartGameFunction +
                        keydownEventListener +
                        "document.addEventListener('DOMContentLoaded', (event) => {" + // Fallback form
                        "  var fallbackForm = document.getElementById('fallbackForm');" +
                        "  if (fallbackForm) {" +
                        "    fallbackForm.style.display = 'none';" +
                        "  }" +
                        "});" +
                        "</script>";
            

        return "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "<meta charset=\"UTF-8\">\n" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "<link rel=\"icon\" type=\"image/x-icon\" href=\"data:image/x-icon;,\">\n" + // Empty favicon
            "<title>" + title + "</title>\n" +
            "<style>" + styles + "</style>\n" +
            "</head>\n" +
            "<body>\n" +
            header +
            "<div id=\"wordle-board\">" + wordleBoard + "</div>\n" +
            "<div id=\"keyboard\">" + keyboard + "</div>\n" +
            fallbackForm +
            modalHtml +
            script +
            "</body>\n" +
            "</html>";
    }

    private String generateWordleBoard() {
        StringBuilder boardBuilder = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            boardBuilder.append("<div class=\"word-row\" id=\"row-").append(i).append("\">");
            for (int j = 0; j < 5; j++) {
                boardBuilder.append("<div class=\"word-cell\" id=\"cell-").append(i).append("-").append(j).append("\"></div>");
            }
            boardBuilder.append("</div>");
        }
    
        // Initialize the game state for a new game
        String scriptUpdate = "<script>let currentRow = 0; let currentGuess = ''; let currentCell = 0;</script>";
    
        return boardBuilder.toString() + scriptUpdate;
    }
    
    private String generateWordleBoardWithState(String gameState) {
        StringBuilder boardBuilder = new StringBuilder();
        String[] tries = gameState.split(";");
        int lastFilledRow = -1;
        boolean gameEnded = false;
        boolean playerWon = false;
        String secretWord = "";
    
        for (int i = 0; i < tries.length; i++) {
            String[] parts = tries[i].split(":");
            String guess = (parts.length > 1) ? parts[1].toUpperCase() : "";
            String color = (parts.length > 2) ? parts[2] : "";
    
            if (i == 0) { // Handling the secret word
                secretWord = parts[2]; // Assuming the secret word is always present
                continue; // Skip further processing for the secret word row
            }

            boardBuilder.append("<div class=\"word-row\" id=\"row-").append(i-1).append("\">");
    
            if (guess.isEmpty()) {
                // Generate empty cells for the row
                for (int j = 0; j < 5; j++) {
                    boardBuilder.append("<div class=\"word-cell\" id=\"cell-").append(i-1).append("-").append(j).append("\"></div>");
                }
            } else {
                lastFilledRow = i-1;
                // Generate cells with guesses and color
                for (int j = 0; j < guess.length(); j++) {
                    char letter = guess.charAt(j);
                    char colorCode = (color.length() > j) ? color.charAt(j) : ' ';
                    String colorClass = getColorClass(colorCode);
                    boardBuilder.append("<div class=\"word-cell ")
                             .append(colorClass)
                             .append("\" id=\"cell-")
                             .append(i-1)
                             .append("-")
                             .append(j)
                             .append("\">")
                             .append(letter)
                             .append("</div>");
                }
            }

            boardBuilder.append("</div>");

            // Check for win condition
            if (color.equals("GGGGG")) {
                gameEnded = true;
                playerWon = true;
            }
        }
    
        if (!gameEnded && lastFilledRow == 5) {
            gameEnded = true;
        }

        if (gameEnded) {
            String modalMessage = playerWon ? "Congratulations, You Won!" : "Game Over. The correct word was: " + secretWord;
            boardBuilder.append("<div class=\"modal\" style=\"display: block;\">")
                        .append("<p>").append(modalMessage).append("</p>")
                        .append("</div>");
        }

        // Update currentRow and currentGuess in the script
        String scriptUpdate = "<script>let currentRow = " + (lastFilledRow + 1) + "; let currentGuess = ''; let currentCell = 0;</script>";
    
        return boardBuilder.toString() + scriptUpdate;
    }    
    
    private String getColorClass(char colorCode) {
        switch (colorCode) {
            case 'G': return "green";
            case 'Y': return "yellow";
            case 'B': return "darkened";
            default: return "";
        }
    }    

    private String generateKeyboard() {
        String[][] keyRows = {
            {"A", "Z", "E", "R", "T", "Y", "U", "I", "O", "P"},
            {"Q", "S", "D", "F", "G", "H", "J", "K", "L", "M"},
            {"⇦", "W", "X", "C", "V", "B", "N", "⏎"}
        };

        StringBuilder keyboardBuilder = new StringBuilder();
            for (String[] keyRow : keyRows) {
                keyboardBuilder.append("<div class=\"keyboard-row\">");
                for (String key : keyRow) {
                    // Add the data-letter attribute to each key except special keys like '⇦' and '⏎'
                    if (!key.equals("⇦") && !key.equals("⏎")) {
                        keyboardBuilder.append("<button class='key' data-letter='").append(key)
                                    .append("' onclick='keyPressed(\"").append(key).append("\")'>")
                                    .append(key).append("</button>");
                    } else {
                        // Handle special keys without the data-letter attribute
                        keyboardBuilder.append("<button class=\"key special-key\" onclick=\"")
                                    .append(key.equals("⇦") ? "onErase()" : "onSubmitGuess()")
                                    .append("\">").append(key).append("</button>");
                    }
                }
                keyboardBuilder.append("</div>\n");
            }
            return keyboardBuilder.toString();
    }

    public String fallbackForm() {
        return "<noscript>" +
                "<form action=\"/guess\" method=\"post\">" +
                "<input type=\"text\" name=\"guess\" required pattern=\"[A-Za-z]{5}\" maxlength=\"5\">" +
                "<input type=\"submit\" value=\"Submit Guess\">" +
                "</form>" +
                "</noscript>"; 
    }

    private String generateStyles() {
        return "body { font-family: Arial, sans-serif; background-color: #121213; color: white; }" +
                "header { text-align: center; padding: 20px; }" +
                "#wordle-board { margin-bottom: 20px; }" +
                ".word-row { display: flex; justify-content: center; margin-bottom: 5px; }" +
                ".word-cell {" +
                "   width: 50px;" +
                "   height: 50px;" + 
                "   background-color: #3a3a3c;" +
                "   margin: 2px;" +
                "   animation: popIn 0.3s;" +
                "}" +
                "@keyframes popIn {" +
                "   0% { transform: scale(0); }" +
                "   50% { transform: scale(1.2); }" +
                "   100% { transform: scale(1); }" +
                "}" +
                "#keyboard { margin-bottom: 20px; }" +
                ".keyboard-row { text-align: center; }" +
                ".key { margin: 5px; width: 40px; height: 40px; }" +
                ".key.green { background-color: #6aaa64; color: white; }" + 
                ".key.yellow { background-color: #c9b458; color: white; }" + 
                ".key.darkened { background-color: #787c7e; color: white; }" + 
                "form { text-align: center; }" + 
                "form input[type='text'] { margin: 0 5px; }" +
                "form input[type='submit'] { margin: 0 5px; }" + 
                ".word-cell {" +
                "   width: 50px;" +
                "   height: 50px;" +
                "   background-color: #3a3a3c;" +
                "   margin: 2px;" +
                "   display: flex;" +
                "   justify-content: center;" +
                "   align-items: center;" +
                "   font-size: 24px;" + 
                "   color: white;" + 
                "   font-weight: bold;" + 
                "}" + 
                ".word-cell.green { background-color: #6aaa64; }" +
                ".word-cell.yellow { background-color: #c9b458; }" +
                ".word-cell.darkened { background-color: #262626; }" +
                ".modal {" +
                "   display: none;" +
                "   position: fixed;" + 
                "   z-index: 1;" + 
                "   left: 0;" +
                "   top: 0;" +
                "   width: 100%;" +
                "   height: 100%;" +
                "   overflow: auto; " +
                "   background-color: rgb(0,0,0);" +
                "   background-color: rgba(0,0,0,0.4);" +
                "   backdrop-filter: blur(8px);" +
                "}" +
                ".modal-content {" +
                "   background-color: #fefefe;" +
                "   margin: 15% auto;" +
                "   padding: 20px;" + 
                "   border: 1px solid #888;" +
                "   width: 50%; " +
                "   text-align: center;" +
                "   box-shadow: 0 4px 8px 0 rgba(0,0,0,0.2);" +
                "   animation: animatetop 0.4s;" +
                "}" +
                "@keyframes animatetop {" +
                "   from {top: -300px; opacity: 0}" +
                "   to {top: 0; opacity: 1}" +
                "}" +
                ".close {" +
                "   color: #aaa;" +
                "   float: right;" +
                "   font-size: 28px;" +
                "   font-weight: bold;" +
                "}" +
                ".close:hover," +
                ".close:focus {" +
                "   color: black;" +
                "   text-decoration: none;" +
                "   cursor: pointer;" +
                "}" ;
    }
   
    // public static void main(String[] args) {
    //     String gameState = "-1:secret:secret;0:loose:BGYGY;1:house:GGGBG;2::;3::;4::;5::";
    //     //String gameState = "-1:secret:secret;0::;1::;2::;3::;4::;5::";

    //     HTML htmlGenerator = new HTML();
    //     String content = htmlGenerator.generateWordlePage(gameState);

    //     try (FileWriter fileWriter = new FileWriter("wordle.html")) {
    //         fileWriter.write(content);
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     }
    // }
}