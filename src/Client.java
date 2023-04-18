import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Client {
    int clientPort;
    ArrayList<Integer> serverPorts = new ArrayList<Integer>();

    Client(int clientPort){
        this.clientPort = clientPort;
        serverPorts.add(7777);
        serverPorts.add(8888);
    }

    int getRandomServerPort(){
        return serverPorts.get((int)(Math.random() * serverPorts.size()));
    }

    public void start(){
        BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in));
        BufferedReader networkIn;
        PrintWriter networkOut;

        String serverAddress = "localhost"; // or "127.0.0.1"

        System.out.println("Client started on port " + clientPort);

        Socket socket;

        while(true){
            try{
                String userLine = userIn.readLine(); //auf Tastatureingabe warten

                socket = new Socket();
                socket.bind(new InetSocketAddress(clientPort));

                //TODO: prüfen ob Server online
                socket.connect(new InetSocketAddress(serverAddress, getRandomServerPort())); //Verbindung zum Server

                networkIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                networkOut = new PrintWriter(socket.getOutputStream());

                //TODO: Timestamp mitverschicken
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

    public static void main(String[] args) {
        int clientPort = 1234;
        if(args.length > 0)
            clientPort = Integer.parseInt(args[0]);

        new Client(clientPort).start();
    }
}
