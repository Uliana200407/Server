import clientInfo.Client;
import org.junit.jupiter.api.*;

import java.io.*;
import java.util.List;
import java.util.concurrent.*;

class UnitTestingClient {
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
    void testConnectToServer_SuccessfulLinking() {
        String expectedOutput = "Successful linking";
        BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
        CountDownLatch latch = new CountDownLatch(1);

        Client client = new Client();
        client.setSocket(new TestSocket(messageQueue));

        Thread receivingThread = new Thread(() -> {
            client.connectToServer();
            latch.countDown();
        });
        receivingThread.start();

        try {
            latch.await();

            CompletableFuture<Void> receivedMessageFuture = CompletableFuture.runAsync(() -> {
                try {
                    String message = messageQueue.poll(5, TimeUnit.SECONDS);
                    if (message != null) {
                        System.out.println("Received Message: " + message);
                        // Assert
                        Assertions.assertEquals(expectedOutput, message);
                    } else {
                        Assertions.fail("No message received from the server within the timeout.");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

            receivedMessageFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCaptureScreenshot() {
        String expectedOutput = "[ACTION]Screenshot saved as: screenshot.png";

        Client client = new Client();
        client.captureScreenshot();

        Assertions.assertTrue(outContent.toString().contains(expectedOutput));
    }

    private static class TestSocket extends java.net.Socket {
        private final BlockingQueue<String> messageQueue;

        public TestSocket(BlockingQueue<String> messageQueue) {
            this.messageQueue = messageQueue;
        }

        @Override
        public java.io.InputStream getInputStream() {
            return new ByteArrayInputStream("Server response".getBytes());
        }

        @Override
        public java.io.OutputStream getOutputStream() {
            return new ByteArrayOutputStream();
        }

        @Override
        public void close() throws IOException {
            super.close();
            try {
                messageQueue.put("[ACTION]Disconnected from the server.");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
