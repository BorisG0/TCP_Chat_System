import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) {
        BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in));
        Socket socket;
        BufferedReader networkIn;
        PrintWriter networkOut;

        while(true){
            try{
                String userLine = userIn.readLine(); //auf Tastatureingabe warten

                socket = new Socket("localhost", Server.DEFAULT_PORT); //Verbindung zum Server
                networkIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                networkOut = new PrintWriter(socket.getOutputStream());

                networkOut.println(userLine); //Befehl an Server schicken
                networkOut.flush();

                System.out.println(networkIn.readLine()); //Antwort vom Server anzeigen
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
