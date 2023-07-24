import org.junit.jupiter.api.*;
import serverInfo.Server;

import java.io.*;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class UnitTestingServer {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final InputStream originalIn = System.in;
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUpStreams() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void restoreStreams() {
        System.setIn(originalIn);
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    void testStart_ServerStartMessage() {
        String expectedOutput = "[ACTION] Server started on port";

        CountDownLatch latch = new CountDownLatch(1);


        String input = "8081\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(inputStream);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        System.setOut(printStream);

        Thread serverThread = new Thread(() -> {
            Server server = new Server();
            int port = Integer.parseInt(input.trim());
            server.start(port);
            latch.countDown();
        });
        serverThread.start();


        String actualOutput = outputStream.toString();
        System.out.println("Actual Output:\n" + actualOutput);

        try {
            inputStream.close();
            printStream.close();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Test
    void testBroadcastMessage() {
        String expectedOutput = "[ACTION]Feedback from the server: Message from server";

        Server server = new Server();
        server.getClientConnections().add(new TestClientConnection()); // Mocking ClientConnection
        server.broadcastMessage("Message from server");

        assertTrue(outContent.toString().contains(expectedOutput));
    }

    private static class TestClientConnection extends Server.ClientConnection {
        public TestClientConnection() {
            super(null, "Test Client", null);
        }

        @Override
        public void sendMessage(String message) {
            System.out.println("[ACTION]Feedback from the server: " + message);
        }
    }
}
