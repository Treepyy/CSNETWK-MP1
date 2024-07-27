import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.channels.FileChannel;

public class Server {
    private ServerSocket serverSocket;
    private Map<String, ClientHandler> clients = new HashMap<>();
    private final String fileStoragePath = "server_files";

    public Server(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        new File(fileStoragePath).mkdir();
        System.out.println("Server started on port " + port);
    }

    public void start() {
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                new Thread(clientHandler).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
                // Handle client disconnection
                if (handle != null) {
                    System.out.println(handle + " has disconnected from the server.");
                } else {
                    System.out.println("A client has disconnected from the server.");
                }
            } finally {
                try {
                    if (handle != null) {
                        clients.remove(handle);
                    }
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
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
                e.printStackTrace();
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
                }
            } else {
                out.println("Error: Command parameters do not match or is not allowed.");
            }
        }

        private void handleStore(String[] parts) {
            if (parts.length == 2) {
                String filename = parts[1];
                File sourceFile = new File("client_files/" + filename);
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
                File destinationFile = new File("client_files/" + filename);

                if (handle == null) {
                    out.println("Error: You must register before using this command.");
                    return;
                }

                if (sourceFile.exists()) {
                    try {
                        copyFile(sourceFile, destinationFile);
                        out.println("File received from Server: " + filename);
                    } catch (IOException e) {
                        out.println("Error: Unable to recieve file. " + e.getMessage());
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

    public static void main(String[] args) {
        try {
            Server server = new Server(12345);
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
