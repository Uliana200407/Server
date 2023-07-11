import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.io.FileDescriptor.out;
import static java.lang.System.in;

public class Server {

    public List<ClientConnection> getClientConnections() {
        return clientConnections;
    }

    public void setClientConnections(List<ClientConnection> clientConnections) {
        this.clientConnections = clientConnections;
    }

    private static List<ClientConnection> clientConnections;
    private static final String FILE_FOR_CONNECTIONS = "connections.dat";

    public static void main(String[] args) {
        Server server = new Server();
        server.start(8081);
    }

    public Server() {
        clientConnections = new ArrayList<>();
    }

    String getClientName(Socket clientSocket) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
            return reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public void start(int port) {
        upload();

        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                String clientName = getClientName(clientSocket);
                LocalDateTime time = LocalDateTime.now();
                ClientConnection clientConnection = new ClientConnection(clientSocket, clientName, time);
                clientConnections.add(clientConnection);
                clientConnection.start();

                broadcastMessage("New client connected: " + clientSocket);

                saveConnections();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcastMessage(String message) {
        for (ClientConnection clientConnection : clientConnections) {
            clientConnection.sendMessage(message);
        }
    }

    private void saveConnections() {
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(FILE_FOR_CONNECTIONS))) {
            outputStream.writeObject(clientConnections);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private void upload() {
        File file = new File(FILE_FOR_CONNECTIONS);
        if (file.exists()) {
            try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(FILE_FOR_CONNECTIONS))) {
                clientConnections = (List<ClientConnection>) inputStream.readObject();
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected static class ClientConnection extends Thread implements Serializable {
        private transient Socket clientSocket;
        // private Socket clientSocket;
        private String clientName;
        private LocalDateTime time;
        private PrintWriter writer;

        public ClientConnection(Socket clientSocket, String clientName, LocalDateTime time) {
            this.clientSocket = clientSocket;
            this.clientName = clientName;
            this.time = time;

            try {
                if (clientSocket != null && clientSocket.getOutputStream() != null) {
                    OutputStream outputStream = clientSocket.getOutputStream();
                    writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void writeObject(ObjectOutputStream out) throws IOException {
            out.defaultWriteObject();
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
        }
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));

                String inputLine;
                while (true) {
                    try {
                        inputLine = reader.readLine();
                        if (inputLine == null) {
                            break;
                        }

                        if (inputLine.equals("close")) {
                            clientConnections.remove(this);
                            break;
                        }
                    } catch (SocketException e) {
                        break;
                    }
                }

                reader.close();
                writer.close();
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendMessage(String message) {
            writer.println(message);
        }

        public PrintWriter getWriter() {
            return writer;
        }
    }
}
