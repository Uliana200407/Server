import org.junit.jupiter.api.Test;
import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.net.ServerSocket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UnitTestingServer extends Server {
    private static final String FILE_FOR_CONNECTIONS = "connections.dat";
    @Test

    public void testStart()  {

        // Prepare test data
        List<Server.ClientConnection> clientConnections = new ArrayList<>();
        Socket mockSocket = mock(Socket.class);
        try {
            when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream("John".getBytes()));
        } catch (IOException e) {
            throw new RuntimeException ( e );
        }
        clientConnections.add(new Server.ClientConnection(mockSocket, "John", LocalDateTime.now()));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));

        Server server = new Server();
        server.setClientConnections(clientConnections);

        ServerSocket mockServerSocket = mock(ServerSocket.class);
        try {
            when(mockServerSocket.accept()).thenReturn(mockSocket);
        } catch (IOException e) {
            e.printStackTrace();
        }

        server.start(8081);
        assertEquals("Server started on port 8081\n", outputStream.toString());

        // Clean up
        System.setOut(System.out);
    }



    @Test
    public void testGetClientName() throws IOException {
        // Create a mock client socket
        PipedInputStream inputStream = new PipedInputStream ();
        final PrintWriter writer = new PrintWriter ( new PipedOutputStream ( inputStream ), true );
        Socket clientSocket = new Socket () {
            @Override
            public InputStream getInputStream() {
                return inputStream;
            }
        };

        String clientName = "John";
        writer.println ( clientName );

        Server server = new Server ();
        String result = server.getClientName ( clientSocket );
        assertEquals ( clientName, result );
    }

    @Test
    public void testBroadcastMessage() throws IOException {
        ServerSocket serverSocket = new ServerSocket ( 0 );
        Socket clientSocket1 = new Socket ( serverSocket.getInetAddress (), serverSocket.getLocalPort () );
        Socket clientSocket2 = new Socket ( serverSocket.getInetAddress (), serverSocket.getLocalPort () );
        Socket clientSocket3 = new Socket ( serverSocket.getInetAddress (), serverSocket.getLocalPort () );
        Server server = new Server ();

        ClientConnection connection1 = new ClientConnection ( clientSocket1, "John", LocalDateTime.now () );
        ClientConnection connection2 = new ClientConnection ( clientSocket2, "Alice", LocalDateTime.now () );
        ClientConnection connection3 = new ClientConnection ( clientSocket3, "Uliana", LocalDateTime.now () );
        server.getClientConnections ().add ( connection1 );
        server.getClientConnections ().add ( connection2 );
        server.getClientConnections ().add ( connection3 );

        String message = "Hello, guys from Ukraine!";
        server.broadcastMessage ( message );

        assertFalse ( connection1.getWriter ().checkError () );
        assertFalse ( connection2.getWriter ().checkError () );
        assertFalse ( connection3.getWriter ().checkError () );

        clientSocket1.close ();
        clientSocket2.close ();
        clientSocket3.close ();
        serverSocket.close ();
    }


}
