import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class Client extends JFrame {
    private JTextArea textArea;
    private JTextField textField;
    private JButton sendButton;
    private JButton disconnectButton;
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean connected = false;
    private String serverIp = "127.0.0.1";
    private int serverPort = 12345;

    public Client() {
        setTitle("Client");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        textArea = new JTextArea();
        textArea.setEditable(false);
        add(new JScrollPane(textArea), BorderLayout.CENTER);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        textField = new JTextField();
        panel.add(textField, BorderLayout.CENTER);

        sendButton = new JButton("Send");
        sendButton.addActionListener(new SendButtonListener());
        panel.add(sendButton, BorderLayout.EAST);

        disconnectButton = new JButton("Disconnect");
        disconnectButton.addActionListener(new DisconnectButtonListener());
        panel.add(disconnectButton, BorderLayout.SOUTH);

        add(panel, BorderLayout.SOUTH);
    }

    private void connectToServer(String ip, int port) {
        new Thread(() -> {
            while (!connected) {
                try {
                    clientSocket = new Socket(ip, port);
                    in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    out = new PrintWriter(clientSocket.getOutputStream(), true);
                    connected = true;
                    textArea.append("Connected to the server at " + ip + ":" + port + "\n");

                    new Thread(new ServerListener()).start();
                } catch (IOException e) {
                    textArea.append("Failed to connect to server, retrying in 10 seconds...\n");
                    try {   
                        Thread.sleep(10000); // Wait for 10 seconds before retrying
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }).start();
    }

    private class SendButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            sendMessage();
        }
    }

    private class DisconnectButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                disconnect();
            } catch (IOException ioException) {
                textArea.append("Error disconnecting from server.\n");
            }
        }
    }

    private class ServerListener implements Runnable {
        @Override
        public void run() {
            try {
                String serverResponse;
                while ((serverResponse = in.readLine()) != null) {
                    textArea.append("> " + serverResponse + "\n");
                }
            } catch (IOException e) {
                textArea.append("Connection to server lost.\n");
                connected = false;
                connectToServer(clientSocket.getInetAddress().getHostAddress(), clientSocket.getPort());
            }
        }
    }

    private void sendMessage() {
        String message = textField.getText();
    
        if (connected) {
            try {
                if (!message.isEmpty()) {
                    out.println(message);
                    textField.setText("");
                }
            } catch (Exception e) {
                textArea.append("Error sending message.\n");
            }            
        } else {
            if (message.equals("/join 127.0.0.1 12345")) {
                String[] parts = message.split(" ");
                if (parts.length == 3) {
                    String ip = parts[1];
                    int port = Integer.parseInt(parts[2]);
                    serverIp = ip; 
                    serverPort = port; 
                    connectToServer(serverIp, serverPort);
                } else {
                    textArea.append("Error: Connection to the Server has failed!\n" + 
                                    "Please check IP Address and Port Number.\n");
                }
            } else if (message.equals("/?")) {
                    textArea.append("/join <server_ip> <port>\n");
                    textArea.append("/leave\n");
                    textArea.append("/register <handle>\n");
                    textArea.append("/store <filename>\n");
                    textArea.append("/dir\n");
                    textArea.append("/get <filename>\n");
                    textArea.append("/broadcast <message>\n");
                    textArea.append("/unicast <handle> <message>\n");
                    textArea.append("/?\n");
            } else {
                textArea.append("Error: Connection to the Server has failed!\n" + 
                                    "Please check IP Address and Port Number.\n");
            }
        }
    }
    

    private void disconnect() throws IOException {
        if (clientSocket != null && !clientSocket.isClosed()) {
            clientSocket.close();
            connected = false;
            textArea.append("Disconnected from the server.\n");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Client client = new Client();
            client.setVisible(true);
        });
    }
}
