/**
 * To run open CMD and input    javac Server.java ServerGUI.java
 * 
 * Afterwards, input            java  ServerGUI.java
 */


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JTextArea;

public class Server {
    private ServerSocket serverSocket;
    private Map<String, ClientHandler> clients = new HashMap<>();
    private final String fileStoragePath = "server_files";
    private JTextArea logArea;
    private volatile boolean running = true;

    public Server(int port, JTextArea logArea) throws IOException {
        serverSocket = new ServerSocket(port);
        this.logArea = logArea;
        new File(fileStoragePath).mkdir();
        appendLog("Server started on port " + port);
    }

    public void start() {
        new Thread(() -> {
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler clientHandler = new ClientHandler(clientSocket);
                    new Thread(clientHandler).start();
                } catch (IOException e) {
                    if (running) {
                        appendLog("Error accepting client connection: " + e.getMessage());
                    }
                }
            }
        }).start();
    }

    public void stop() {
        running = false;
        try {
            serverSocket.close();
            appendLog("Server stopped.");
        } catch (IOException e) {
            appendLog("Error closing server socket: " + e.getMessage());
        }
    }

    private void appendLog(String message) {
        if (logArea != null) {
            logArea.append(message + "\n");
        }
    }

    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private BufferedReader in;
        private PrintWriter out;
        private String handle;

        public ClientHandler(Socket socket) throws IOException {
            this.clientSocket = socket;
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);
        }

        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    processCommand(message);
                }
            } catch (IOException e) {
                if (handle != null) {
                    appendLog(handle + " has disconnected from the server.");
                } else {
                    appendLog("A client has disconnected from the server.");
                }
            } finally {
                try {
                    if (handle != null) {
                        clients.remove(handle);
                    }
                    clientSocket.close();
                } catch (IOException e) {
                    appendLog("Error closing client socket: " + e.getMessage());
                }
            }
        }

        private void processCommand(String message) {
            String[] parts = message.split(" ");
            String command = parts[0];
            switch (command) {
                case "/join":
                    handleJoin(parts);
                    break;
                case "/leave":
                    handleLeave();
                    break;
                case "/register":
                    handleRegister(parts);
                    break;
                case "/store":
                    handleStore(parts);
                    break;
                case "/dir":
                    handleDir();
                    break;
                case "/get":
                    handleGet(parts);
                    break;
                case "/broadcast":
                    handleBroadcast(parts);
                    break;
                case "/unicast":
                    handleUnicast(parts);
                    break;
                case "/?":
                    handleHelp();
                    break;
                default:
                    out.println("Error: Command not found.");
                    break;
            }
        }

        private void handleJoin(String[] parts) {
            if (parts.length == 3) {
                out.println("Connection to the File Exchange Server is successful!");
            } else {
                out.println("Error: Connection to the Server has failed! Please check IP Address and Port Number.");
            }
        }

        private void handleLeave() {
            out.println("Connection closed. Thank you!");
            try {
                clientSocket.close();
            } catch (IOException e) {
                appendLog("Error closing client socket: " + e.getMessage());
            }
        }

        private void handleRegister(String[] parts) {
            if (parts.length == 2) {
                handle = parts[1];
                if (clients.containsKey(handle)) {
                    out.println("Error: Registration failed. Handle or alias already exists.");
                } else {
                    clients.put(handle, this);
                    out.println("Welcome " + handle + "!");
                    File folder = new File(handle + "_files");
                    folder.mkdirs();
                }
            } else {
                out.println("Error: Command parameters do not match or is not allowed.");
            }
        }

        private void handleStore(String[] parts) {
            if (parts.length == 2) {
                String filename = parts[1];
                File sourceFile = new File(handle + "_files/" + filename);
                File destinationFile = new File(fileStoragePath + "/" + filename);

                if (handle == null) {
                    out.println("Error: You must register before using this command.");
                    return;
                }

                if (!sourceFile.exists()) {
                    out.println("Error: Source file does not exist.");
                    return;
                }

                try {
                    if (destinationFile.exists()) {
                        out.println("Error: File already exists on the server.");
                    } else {
                        copyFile(sourceFile, destinationFile);
                        out.println(handle + "<" + new Date() + ">: Uploaded " + filename);
                    }
                } catch (IOException e) {
                    out.println("Error: Unable to copy file. " + e.getMessage());
                }
            } else {
                out.println("Error: Command parameters do not match or is not allowed.");
            }
        }

        private void handleDir() {
            File folder = new File(fileStoragePath);
            File[] listOfFiles = folder.listFiles();
            if (handle == null) {
                out.println("Error: You must register before using this command.");
                return;
            }

            if (listOfFiles != null && listOfFiles.length > 0) {
                out.println("Server Directory");
                for (File file : listOfFiles) {
                    out.println(file.getName());
                }
            } else {
                out.println("Error: No files found.");
            }
        }

        private void handleGet(String[] parts) {
            if (parts.length == 2) {
                String filename = parts[1];
                File sourceFile = new File(fileStoragePath + "/" + filename);
                File destinationFile = new File(handle + "_files/" + filename);

                if (handle == null) {
                    out.println("Error: You must register before using this command.");
                    return;
                }

                if (sourceFile.exists()) {
                    try {
                        copyFile(sourceFile, destinationFile);
                        out.println("File received from Server: " + filename);
                    } catch (IOException e) {
                        out.println("Error: Unable to receive file. " + e.getMessage());
                    }
                } else {
                    out.println("Error: File not found in the server.");
                }
            } else {
                out.println("Error: Command parameters do not match or is not allowed.");
            }
        }

        private void copyFile(File sourceFile, File destinationFile) throws IOException {
            if (!destinationFile.getParentFile().exists()) {
                destinationFile.getParentFile().mkdirs();
            }

            try (FileChannel sourceChannel = new FileInputStream(sourceFile).getChannel();
                 FileChannel destChannel = new FileOutputStream(destinationFile).getChannel()) {
                destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
            }
        }

        private void handleBroadcast(String[] parts) {
            if (handle == null) {
                out.println("Error: You must register before using this command.");
                return;
            }
        
            if (parts.length < 2) {
                out.println("Error: Command parameters do not match or is not allowed.");
                return;
            }
        
            StringBuilder messageBuilder = new StringBuilder();
            for (int i = 1; i < parts.length; i++) {
                messageBuilder.append(parts[i]);
                if (i < parts.length - 1) {
                    messageBuilder.append(" ");
                }
            }
            String message = messageBuilder.toString();
        
            for (ClientHandler client : clients.values()) {
                client.out.println("Broadcast from " + handle + ": " + message);
            }
            appendLog("Broadcast from " + handle + ": " + message);
        }
        
        

        private void handleUnicast(String[] parts) {
            if (handle == null) {
                out.println("Error: You must register before using this command.");
                return;
            }
        
            if (parts.length < 3) {
                out.println("Error: Command parameters do not match or is not allowed.");
                return;
            }
        
            String targetHandle = parts[1];
        
            StringBuilder messageBuilder = new StringBuilder();
            for (int i = 2; i < parts.length; i++) {
                messageBuilder.append(parts[i]);
                if (i < parts.length - 1) {
                    messageBuilder.append(" ");
                }
            }
            String message = messageBuilder.toString();
        
        
            ClientHandler targetClient = clients.get(targetHandle);
        
            if (targetClient != null) {
                targetClient.out.println("Message from " + handle + ": " + message);
                out.println("Message sent.");
                appendLog("Message sent to " + targetHandle + ": " + message);
            } else {
                out.println("Error: Target handle not found.");
            }
        }
        

        private void handleHelp() {
            out.println("/join <server_ip> <port>");
            out.println("/leave");
            out.println("/register <handle>");
            out.println("/store <filename>");
            out.println("/dir");
            out.println("/get <filename>");
            out.println("/broadcast <message>");
            out.println("/unicast <handle> <message>");
            out.println("/?");
        }
    }
}
