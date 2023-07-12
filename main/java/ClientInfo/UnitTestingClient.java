package ClientInfo;

import ClientInfo.Client;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UnitTestingClient extends Client {
    private Client client;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    @BeforeEach
    void setUp() {
        client = new Client ();
        socket = mock ( Socket.class );
        reader = mock ( BufferedReader.class );
        writer = mock ( PrintWriter.class );
        client.setSocket ( socket );
        client.setReader ( reader );
        client.setWriter ( writer );
    }

    @AfterEach
    void tearDown() {
        client = null;
        socket = null;
        reader = null;
        writer = null;
    }

    @Test
    void testConnectToServer_SuccessfulConnection() throws IOException {
        when ( socket.getInputStream () ).thenReturn ( mock ( InputStream.class ) );
        when ( socket.getOutputStream () ).thenReturn ( mock ( OutputStream.class ) );
        client.connectToServer ();

        assertNotNull ( client.getSocket () );
        assertNotNull ( client.getReader () );
        assertNotNull ( client.getWriter () );
    }

    @Test
    void testConnectToServer_ConnectionRefused() throws IOException {
        client.setSocket ( null );
        client.setReader ( null );
        client.setWriter ( null );
        when ( socket.getInputStream () ).thenThrow ( new IOException ( "Connection refused" ) );
        client.connectToServer ();

        assertNull ( client.getSocket () );
        assertNull ( client.getReader () );
        assertNull ( client.getWriter () );
    }




    @Test
    void testSendCommands_OtherCommands() throws IOException {
        BufferedReader consoleReader = mock ( BufferedReader.class );
        when ( consoleReader.readLine () ).thenReturn ( "Command1", "Command2", null );
        client.sendCommands ();

        verify ( writer ).println ( "Command1" );
        verify ( writer ).println ( "Command2" );
        verifyNoMoreInteractions ( writer );
    }


    @Test
    void testDisconnectFromServer_SuccessfulDisconnection() throws IOException {
        when ( socket.isClosed () ).thenReturn ( false );
        client.disconnectFromServer ();

        verify ( socket ).close ();
    }

    @Test
    void testDisconnectFromServer_SocketAlreadyClosed() throws IOException {
        when ( socket.isClosed () ).thenReturn ( true );
        client.disconnectFromServer ();

        // Verify that the socket is not closed again
        verify ( socket, never () ).close ();
    }

    @Test
    void testReceiveSMS_SuccessfulMessage() throws IOException {
        when(reader.readLine()).thenReturn("Message1", "Message2", null);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        client.receiveSMS();

        String expectedOutput = "[ACTION]Feedback from the server is: Message1" + System.lineSeparator()
                + "[ACTION]Feedback from the server is: Message2" + System.lineSeparator();
        assertEquals(expectedOutput, outputStream.toString());
    }


    @Test
    void testReceiveSMS_FailedMessage() throws IOException {
        when ( reader.readLine () ).thenThrow ( new IOException ( "Failed to read message" ) );
        client.receiveSMS ();

        // Verify that the error message is printed
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream ();
        System.setErr ( new PrintStream ( outputStream ) );
        client.receiveSMS ();
        String errorMessage = "[ERROR]Trouble of getting the reply: Failed to read message";
        assertEquals ( errorMessage, outputStream.toString ().trim () );
    }
}
