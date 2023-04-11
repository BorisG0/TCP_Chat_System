import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) {
        BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in));
        BufferedReader networkIn;
        PrintWriter networkOut;

        String serverAddress = "localhost"; // or "127.0.0.1"
        int serverPort = Server.DEFAULT_PORT;

        int clientPort = 1234;
        if(args.length > 0)
            clientPort = Integer.parseInt(args[0]);

        System.out.println("Client started on port " + clientPort);

        Socket socket;

        while(true){
            try{
                String userLine = userIn.readLine(); //auf Tastatureingabe warten

                socket = new Socket();
                socket.bind(new InetSocketAddress(clientPort));
                socket.connect(new InetSocketAddress(serverAddress, serverPort)); //Verbindung zum Server

                networkIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                networkOut = new PrintWriter(socket.getOutputStream());

                networkOut.println(userLine); //Befehl an Server schicken
                networkOut.flush();

                String answer = networkIn.readLine(); //Antwort vom Server lesen

                String[] answerSplit = answer.split(";");

                for(String s : answerSplit)//Antwort vom Server anzeigen
                System.out.println(s);

                socket.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
