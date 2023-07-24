package clientInfo;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

import java.util.ArrayList;
import java.util.List;

public class Client {
    private static final String ADDRESS = "127.0.0.1";
    private static final int PORT = 8081;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private final CountDownLatch connectionLatch = new CountDownLatch(1);
    private final List<String> receivedMessages = new ArrayList<>();


    public void connectToServer() {
        try {
            socket = new Socket(ADDRESS, PORT);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            Thread receivingThread = new Thread(this::receiveSMS);
            receivingThread.start();
            sendCommands();

            connectionLatch.countDown();

        } catch (IOException e) {
            System.err.println("[ERROR] Can't link to the server - your fall is " + e.getMessage());
        } finally {
            disconnectFromServer();
        }
    }

    public CountDownLatch getConnectionLatch() {
        return connectionLatch;
    }

    private void sendCommands() throws IOException {
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
        String command;

        while ((command = consoleReader.readLine()) != null) {
            if (command.equalsIgnoreCase("screenshot")) {
                captureScreenshot();
            } else {
                writer.println(command);
            }
        }
    }

    public void captureScreenshot() {
        try {
            Robot robot = new Robot();
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage screenshot = robot.createScreenCapture(screenRect);
            String fileName = "screenshot.png";
            ImageIO.write(screenshot, "png", new File(fileName));
            System.out.println("[ACTION]Screenshot saved as: " + fileName);
        } catch (AWTException | IOException e) {
            System.err.println("[ERROR]Failing to take a screenshot: " + e.getMessage());
        }
    }

    private void disconnectFromServer() {
        try {
            if (socket != null && !socket.isClosed()) {
                Thread.sleep(100);
                socket.close();
                System.out.println("[ACTION]Disconnected from the server.");
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("[ERROR]Trouble with disconnecting: " + e.getMessage());
        }
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public void setReader(BufferedReader reader) {
        this.reader = reader;
    }

    public void setWriter(PrintWriter writer) {
        this.writer = writer;
    }

    private void receiveSMS() {
        try {
            String message;
            while ((message = reader.readLine()) != null) {
                System.out.println("[ACTION]Feedback from the server: " + message);
                receivedMessages.add(message); // Store received messages
            }
        } catch (IOException e) {
            System.err.println("[ERROR]Trouble receiving the reply: " + e.getMessage());
        }
    }

    public List<String> getReceivedMessages() {
        return receivedMessages;
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.connectToServer();
    }
}
