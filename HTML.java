import java.io.FileWriter;
import java.io.IOException;

public class HTML {
   public String generateWordlePage() {
    String title = "Wordle Game";
    String header = "<header><img src=\"logo.png\" alt=\"WORDLE\"></header>";
    
    String wordleBoard = generateWordleBoard();
    String keyboard = generateKeyboard();
    String styles = generateStyles();
    String fallbackForm = fallbackForm();

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
                        "    fetch(`/submit-guess?word=${guess}`, {" + 
                        "        method: 'GET'," + // Or 'POST' if applicable
                        "        credentials: 'include' " +// To include cookies for session management
                        "    })" +
                        "    .then(response => response.json())" +
                        "    .then(data => {" +
                        "        if (data.error) {" +
                        "            alert('The guessed word does not exist.');" +
                                    // Additional logic to allow user to correct the guess
                        "        } else {" +
                        "            updateBoardWithFeedback(data.feedback);" +
                        "        }" +
                        "    })" +
                        "    .catch(error => {" +
                        "        console.error('Error:', error);" +
                        "        alert('An error occurred while submitting the guess.');" +
                        "    });" +
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
    
    String script = "<script>" +
                    "let currentGuess = '';" +
                    "let currentRow = 0;" +
                    "let currentCell = 0;" +
                    keydownEventListener + 
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
                    onSubmitGuess + 
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
           "<title>" + title + "</title>\n" +
           "<style>" + styles + "</style>\n" +
           "</head>\n" +
           "<body>\n" +
           header +
           "<div id=\"wordle-board\">" + wordleBoard + "</div>\n" +
           "<div id=\"keyboard\">" + keyboard + "</div>\n" +
           fallbackForm +
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
        return boardBuilder.toString();
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
                "<input type=\"text\" name=\"guess\" required pattern=\"[A-Za-z]{5}\" maxlength=\"5\" />" +
                "<input type=\"submit\" value=\"Submit Guess\" />" +
                "</form>" +
                "</noscript>"; 
    }

    private String generateStyles() {
        return "body { font-family: Arial, sans-serif; background-color: #121213; color: white; }" +
               "header { text-align: center; padding: 20px; }" +
               "#wordle-board { margin-bottom: 20px; }" +
               ".word-row { display: flex; justify-content: center; margin-bottom: 5px; }" +
               ".word-cell {" +
               "  width: 50px;" +
               "  height: 50px;" + 
               "  background-color: #3a3a3c;" +
               "  margin: 2px;" +
               "  animation: popIn 0.3s;" +
               "}" +
               "@keyframes popIn {" +
               "  0% { transform: scale(0); }" +
               "  50% { transform: scale(1.2); }" +
               "  100% { transform: scale(1); }" +
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
               "  width: 50px;" +
               "  height: 50px;" +
               "  background-color: #3a3a3c;" +
               "  margin: 2px;" +
               "  display: flex;" +
               "  justify-content: center;" +
               "  align-items: center;" +
               "  font-size: 24px;" + 
               "  color: white;" + 
               "  font-weight: bold;" + 
               "}" + 
               ".word-cell.green { background-color: #6aaa64; }" +
               ".word-cell.yellow { background-color: #c9b458; }" +
               ".word-cell.darkened { background-color: #787c7e; }";
    }


    // FOR POST

    // public String buildUpdatedHtml(SessionData sessionData) {
    //     StringBuilder htmlBuilder = new StringBuilder();
    
    //     htmlBuilder.append("<!DOCTYPE html>\n")
    //                .append("<html lang=\"en\">\n")
    //                .append("<head>\n")
    //                .append("<title>Wordle Game</title>\n")
    //                .append("<style>\n")
    //                .append(generateStyles()) // Reuse existing styles
    //                .append("</style>\n")
    //                .append("</head>\n")
    //                .append("<body>\n")
    //                .append("<header><img src=\"logo.png\" alt=\"WORDLE\"></header>\n")
    //                // Dynamically build the game board
    //                .append("<div id=\"wordle-board\">");
    //     for (int i = 0; i < sessionData.getGuesses().size(); i++) {
    //         String guess = sessionData.getGuesses().get(i);
    //         String feedback = sessionData.getFeedback().get(i);
    //         htmlBuilder.append(buildRowHtml(guess, feedback, i));
    //     }
    //     htmlBuilder.append("</div>\n")
    //            // Add the keyboard and fallback form
    //            .append(generateKeyboard())
    //            .append(fallbackForm())
    //            .append("</body>\n")
    //            .append("</html>");
    
    //     return htmlBuilder.toString();
    // }
    
    
    // private String buildRowHtml(String guess, String feedback, int rowIndex) {
    //     StringBuilder rowBuilder = new StringBuilder();
    //     rowBuilder.append("<div class=\"word-row\" id=\"row-").append(rowIndex).append("\">");
    
    //     for (int j = 0; j < guess.length(); j++) {
    //         char letter = guess.charAt(j);
    //         char colorCode = feedback.charAt(j);
    //         String colorClass = getColorClass(colorCode);
    //         rowBuilder.append("<div class=\"word-cell ")
    //                  .append(colorClass)
    //                  .append("\" id=\"cell-")
    //                  .append(rowIndex)
    //                  .append("-")
    //                  .append(j)
    //                  .append("\">")
    //                  .append(letter)
    //                  .append("</div>");
    //     }
    
    //     rowBuilder.append("</div>");
    //     return rowBuilder.toString();
    // }
    
    // private String getColorClass(char colorCode) {
    //     switch (colorCode) {
    //         case 'G': return "green";
    //         case 'Y': return "yellow";
    //         case 'B': return "darkened";
    //         default: return "";
    //     }
    // }
   
    public static void main(String[] args) {
        HTML htmlGenerator = new HTML();
        String content = htmlGenerator.generateWordlePage();

        try (FileWriter fileWriter = new FileWriter("wordle.html")) {
            fileWriter.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

