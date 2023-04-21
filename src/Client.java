import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Client {
    int id; //ID des Clients, damit der Server weiß, wo der Nutzer angemeldet ist
    ArrayList<Integer> serverPorts = new ArrayList<>();

    Client(int id){
        this.id = id;
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
        Socket connection;

        String serverAddress = "localhost"; // or "127.0.0.1"

        System.out.println("Client started with id " + id);


        mainLoop: while(true){
            try{
                String userLine = userIn.readLine(); //auf Tastatureingabe warten
                String response = "";

                boolean connected = false;
                int tries = 0;
                while(!connected){ //solange versuchen bis man sich mit einem Server verbunden hat
                    if(tries >= 100){ //bei 100 Versuchen abbrechen, und auf neue Nutzereingabe warten
                        System.out.println("No server online, try again later");
                        continue mainLoop;
                    }

                    int serverPort = getRandomServerPort();
                    tries++;
                    try{
                        connection = new Socket(serverAddress, serverPort);
                        networkIn = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        networkOut = new PrintWriter(connection.getOutputStream());

                        String currentTime = String.valueOf(System.currentTimeMillis()); //aktuelle Zeit des Clients
                        String request = id + "/" + currentTime + "/" + userLine; //Befehl mit ID versehen
                        networkOut.println(request); //Befehl an Server schicken
                        networkOut.flush();

                        response = networkIn.readLine(); //Antwort vom Server lesen

                        connected = true;
                        System.out.println("Connected to server with port: " + serverPort);
                    }catch (Exception e){
                        System.out.println("Server with port: " + serverPort + " not online, trying another random one");
                    }
                }

                String[] answerSplit = response.split(";");
                System.out.println("Response from server: ");

                for(String s : answerSplit)//Antwort vom Server anzeigen
                    System.out.println(s);

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        int clientId = 0; //StandartId festlegen

        //Id aus Argumenten auslesen
        if(args.length > 0){
            clientId = Integer.parseInt(args[0]);
        }


        new Client(clientId).start();
    }
}
