import java.io.*;
import java.net.*;

public class Client {
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private BufferedReader consoleReader;
    private boolean connected = false;

    public void connectToServer(String ip, int port) {
        while (!connected) {
            try {
                clientSocket = new Socket(ip, port);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                consoleReader = new BufferedReader(new InputStreamReader(System.in));
                System.out.println("Connected to the server at " + ip + ":" + port);
                System.out.print("> ");
                connected = true;

                new Thread(new ServerListener()).start();

                String userInput;
                while ((userInput = consoleReader.readLine()) != null) {
                    out.println(userInput);
                }
            } catch (IOException e) {
                System.err.println("Failed to connect to server, retrying in 10 seconds...");
                try {
                    Thread.sleep(10000); // Wait for 5 seconds before retrying
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
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
                System.err.println("Connection to server lost.");
                connected = false;
                connectToServer(clientSocket.getInetAddress().getHostAddress(), clientSocket.getPort());
            }
        }
    }

    public void disconnect() throws IOException {
        if (clientSocket != null && !clientSocket.isClosed()) {
            clientSocket.close();
            System.out.println("Disconnected from the server.");
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.connectToServer("127.0.0.1", 12345);
    }
}
