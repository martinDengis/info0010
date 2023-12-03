import java.io.FileWriter;
import java.io.IOException;

public class HTML {
   public String generateWordlePage() {
    String title = "Wordle Game";
    String header = "<header><img src=\"logo.png\" alt=\"WORDLE\"></header>";
    
    String wordleBoard = generateWordleBoard();
    String keyboard = generateKeyboard();
    String styles = generateStyles();
    String fallbackForm = "<noscript>" +
                          "<form action=\"/guess\" method=\"post\">" +
                          "<input type=\"text\" name=\"guess\" required pattern=\"[A-Z]{5}\" maxlength=\"5\" />" +
                          "<input type=\"submit\" value=\"Submit Guess\" />" +
                          "</form>" +
                          "</noscript>"; 

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
                          "      fetch(`/guess?word=${currentGuess}`)" +
                          "          .then(response => response.json())" +
                          "          .then(data => updateBoardWithColor(data))" +
                          "          .catch(error => console.error('Error:', error));" +
                          "  } else {" +
                          "      alert('Your word is incomplete. Please enter 5 letters.');" +
                          "  }" +
                        "}";
    
    String onEraseFunction =
                        "function onErase() {" +
                        "    removeLastLetter();" +
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
                if (key.equals("⏎")) {
                    keyboardBuilder.append("<button class=\"key special-key\" onclick=\"onSubmitGuess()\">").append(key).append("</button>");
                } else if (key.equals("⇦")) {
                    keyboardBuilder.append("<button class=\"key special-key\" onclick=\"onErase()\">").append(key).append("</button>");
                } else {
                    keyboardBuilder.append("<button class=\"key\" onclick=\"keyPressed('").append(key).append("')\">").append(key).append("</button>");
                }
            }
            keyboardBuilder.append("</div>\n");
        }
        return keyboardBuilder.toString();
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
               "  font-size: 24px;" + // Adjust font size as needed
               "  color: white;" + // Set the text color
               "  font-weight: bold;" + // Make the letter bold
               "}";
    }
    

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

