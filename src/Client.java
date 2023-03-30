import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) {
        // Verbindung zum Server herstellen
        try {
            Socket socket = new Socket("localhost", Server.DEFAULT_PORT);

            // Thread 1: Tastatureingabe lesen und an Server senden
            Thread sendThread = new Thread(() -> {
                try {
                    BufferedReader networkIn  = new BufferedReader(new InputStreamReader(System.in));
                    PrintWriter networkOut = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
                    String userLine;
                    while ((userLine = networkIn.readLine()) != null) {
                        networkOut.println(userLine);
                        networkOut.flush();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            sendThread.start();

            // Thread 2: Antwort des Servers lesen und auf Konsole ausgeben
            Thread receiveThread = new Thread(() -> {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("Received message from server: " + line);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            receiveThread.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
