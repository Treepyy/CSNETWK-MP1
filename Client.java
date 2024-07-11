import java.io.*;
import java.net.*;

public class Client {
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private BufferedReader consoleReader;

    public void connectToServer(String ip, int port) throws IOException {
        clientSocket = new Socket(ip, port);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        consoleReader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Connected to the server at " + ip + ":" + port);

        new Thread(new ServerListener()).start();

        String userInput;
        while ((userInput = consoleReader.readLine()) != null) {
            out.println(userInput);
            // System.out.print("> "); // Display the user input prompt
        }
    }

    private class ServerListener implements Runnable {
        @Override
        public void run() {
            try {
                String serverResponse;
                while ((serverResponse = in.readLine()) != null) {
                    System.out.println("> " + serverResponse);
                    System.out.print(">>> ");  // Display the user input prompt again after server response
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void disconnect() throws IOException {
        clientSocket.close();
        System.out.println("Disconnected from the server.");
    }

    public static void main(String[] args) {
        Client client = new Client();
        try {
            client.connectToServer("127.0.0.1", 12345);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
