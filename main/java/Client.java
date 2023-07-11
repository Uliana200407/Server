import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
    private static final String ADDRESS = "192.168.3.197";

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public BufferedReader getReader() {
        return reader;
    }

    public void setReader(BufferedReader reader) {
        this.reader = reader;
    }

    public PrintWriter getWriter() {
        return writer;
    }

    public void setWriter(PrintWriter writer) {
        this.writer = writer;
    }

    private static final int PORT = 8081;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;


    public void connectToServer(){
        try{
            socket = new Socket(ADDRESS,PORT);
            System.out.println("Successful linking");
            reader = new BufferedReader(new InputStreamReader (socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            Thread receivingThread = new Thread(this::receiveSMS);
            receivingThread.start ();
            sendCommands();
        } catch (IOException e) {
            System.err.println ("[ERROR] Can't link to the server - your fall is "+ e.getMessage ());
        }finally {
            disconnectFromServer();
        }

    }
    private void sendCommands() throws IOException {
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
        String command;

        while ((command = consoleReader.readLine()) != null) {
            if (command.equalsIgnoreCase("Client")) {
                captureScreenshot();
            } else {
                writer.println(command);
            }
        }
    }
    private void captureScreenshot() {
        try {
            Robot robot = new Robot();
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage screenshot = robot.createScreenCapture(screenRect);
            String fileName = "Client.png";
            ImageIO.write(screenshot, "png", new File (fileName));
            System.out.println("[ACTION]Screenshot saved as: " + fileName);
        } catch (AWTException | IOException e) {
            System.err.println("[ERROR]Failing doing a screenshot: " + e.getMessage());
        }
    }
    public void disconnectFromServer() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("[ACTION]Refusing connection.");
            }
        } catch (IOException e) {
            System.err.println("[ERROR]Trouble with refusing connection: " + e.getMessage());
        }
    }
    private void receiveSMS() {
        try {
            String message;
            while ((message = reader.readLine()) != null) {
                System.out.println("[ACTION]Feedback from the server is: " + message);
            }
        } catch (IOException e) {
            System.err.println("[ERROR]Trouble of getting the reply: " + e.getMessage());
        }
    }
    public static void main(String[] args) {
        Client client = new Client();
        client.connectToServer();
    }
}
