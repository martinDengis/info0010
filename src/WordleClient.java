import java.io.*;
import java.net.*;
import java.util.Scanner;

public class WordleClient {
    public static void main(String[] args) throws Exception {
        String serverAddress = "localhost"; 
        int serverPort = 2348;

        try {
            // Initialise socket, reading from socket, writing to socket
            Socket s = new Socket(serverAddress, serverPort);
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
            PrintWriter writer = new PrintWriter(s.getOutputStream(), true);
            System.out.println("Successful connection with server " + serverAddress + " on port " + serverPort);

            // Initialise user input scanner
            Scanner scanner = new Scanner(System.in);
            System.out.println("1) Cheat\n2) Propose a word\n3) Quit\n");

            // Wordle game loop
            gameLoop(reader, writer, scanner);

            // Closing game
            scanner.close();
            writer.close();
            reader.close();
            s.close();
        } catch (IOException e) { e.printStackTrace(); }
   
    }

    private static void gameLoop(BufferedReader reader, PrintWriter writer, Scanner scanner) throws IOException {
        int tryCounter = 0;

         while(tryCounter < 5) {
                // Prompting user for input and checking input
                String input = userInputPrompt(scanner);
                int inputNumber = userInputCheck(input);

                // Valid entry
                if (inputNumber != 0) {
                    // CHEAT
                    if (inputNumber == 1) {
                        writer.println("CHEAT\r\n");
                        writer.flush();
                        getServerResponse(reader);
                    }

                    // GUESS
                    else if (inputNumber == 2) {
                        String guess = scanner.nextLine();

                        if (guess.length() == 5) {
                            String message = guess.toUpperCase();
                        
                            writer.println("TRY " + message + "\r\n"); // Send the message
                            writer.flush();
                            String response = getServerResponse(reader); // Read the response

                            // If response is gameover (or BBBBB), stop the game
                        } else {
                            System.out.println("Incorrect entry. Please try a five-letter word.");
                            continue;
                        } 
                    }

                    // QUIT
                    else {
                        writer.println("QUIT\r\n");
                        writer.flush(); 
                        String response = getServerResponse(reader);
                        // If (response is stop the game) { print "YOU WON" + System.exit(0); }
                        
                    } 
                } 

                // Invalid entry
                else { 
                    System.out.println("Incorrect entry. Please choose number 1, 2 or 3.");
                    continue;
                }
                
                tryCounter++;
        }

        System.out.println("YOU LOOSE!"); // + Print the word
            
    }

    private static String userInputPrompt(Scanner scanner) {
        System.out.print("Your choice: ");
        return scanner.nextLine();
    }
    
    private static int userInputCheck(String input) {
        try{ 
            int inputNumber = Integer.parseInt(input); 
            if(inputNumber == 1 || inputNumber == 2 || inputNumber == 3) return inputNumber;
            else return 0;
        } catch (NumberFormatException e) { return 0; }
    }
    
    private static String getServerResponse(BufferedReader reader) throws IOException {
        String response = reader.readLine(); // Read the server's response
        System.out.println(response + "\n");
        return response;
    }
}
