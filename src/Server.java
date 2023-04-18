import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Server {
    public static final int DEFAULT_PORT = 7777;

    //TODO: Daten in Textdatei speichern
    ArrayList<UserData> userData = new ArrayList<>(); // Speichern aller Nutzer mit Passwörtern
    ArrayList<Message> messages = new ArrayList<>(); // Alle verschickten Nachrichten

    HashMap<String, UserData> userDataByName = new HashMap<>(); // Hilfsstruktur zum Bekommen der Nutzer zum Namen
    HashMap<String, String> loggedInUsers = new HashMap<>(); // Speichern welcher Client auf welchen Nutzer angemeldet ist

    int port, port2;

    Server(){
        //3 Anfangsnutzer initialisieren
        userData.add(new UserData("Tom", "111"));
        userDataByName.put("Tom", userData.get(0));
        userData.add(new UserData("Peter", "222"));
        userDataByName.put("Peter", userData.get(1));
        userData.add(new UserData("Heinz", "333"));
        userDataByName.put("Heinz", userData.get(2));
    }

    public void start(int port, int port2){
        this.port = port; //eigener Port
        this.port2 = port2; //Port vom zweiten Server

        try {
            ServerSocket server = new ServerSocket(port);
            Socket connection;
            PrintWriter out;
            BufferedReader in;

            System.out.println("Server started on port " + port + ", with second server on port " + port2);

            while (true) {
                String lineOut = "ERROR"; //Antwort vom Server vorbereiten
                connection = server.accept(); //auf neue Verbindung warten
                in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                String lineIn = in.readLine();
                System.out.println("new request: '" + lineIn + "'");

                String [] requestSplit = lineIn.split("/", 3); //Aufteilen in ID, Zeitstempel und Befehl
                String senderId = requestSplit[0];
                String timestamp = requestSplit[1];

                String[] commandSplit = requestSplit[2].split(" ", 2); //Aufteilen in Befehl und Parameter
                String command = commandSplit[0];

                String parameter = "";
                if(commandSplit.length > 1)
                    parameter = commandSplit[1];


                if (command.equals("LOGIN")) {
                    lineOut = handleLogin(parameter, senderId);
                }else if(command.equals("MSG")){
                    lineOut = handleMessage(parameter, senderId, timestamp);
                }else if(command.equals("CONV")){
                    lineOut = handleGetConversation(parameter, senderId);
                }else if(command.equals("SYNCMSG")) {
                    lineOut = handleMessageSyncRequest(parameter);
                }else if(command.equals("SYNCLOGIN")) {
                    lineOut = handleLoginSyncRequest(parameter);
                }

                System.out.println("sending response: '" + lineOut + "'");
                out = new PrintWriter(connection.getOutputStream());
                out.println(lineOut); //Antwort zurückschicken
                out.flush();

                connection.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void sendSyncCommand(String command){
        String timestamp = String.valueOf(System.currentTimeMillis());
        String syncRequest = port + "/" + timestamp + "/" + command;
        System.out.println("created sync request: " + syncRequest);

        try {
            Socket connection = new Socket("localhost", port2);
            PrintWriter out = new PrintWriter(connection.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            out.println(syncRequest);
            out.flush();

            String answer = in.readLine();
            System.out.println("sync answer: " + answer);

            connection.close();
        } catch (Exception e) {
            System.out.println("sync failed");
        }
    }

    void syncMessagesToSecondServer(){
        String data = "";
        for(Message m : messages){ // alle gespeicherten Nachrichten
            data += m.serialize() + ";";
        }

        sendSyncCommand("SYNCMSG " + data);
    }

    String handleMessageSyncRequest(String data){
        String[] messages = data.split(";");

        //nicht syncen wenn Anzahl der Nachrichten kleiner oder gleich der eigenen ist
        if(messages.length <= this.messages.size())
            return "message sync not necessary";

        //alle eigenen Nachrichten mit den geschickten überschreiben
        this.messages.clear();
        for(String m : messages){
            this.messages.add(new Message(m));
        }
        return "message sync successful";
    }


    void syncLoginToSecondServer(){
        String data = "";

        for(String address : loggedInUsers.keySet()){
            data += address + "-" + loggedInUsers.get(address) + ";";
        }

        sendSyncCommand("SYNCLOGIN " + data);
    }

    String handleLoginSyncRequest(String data){
        String[] logins = data.split(";");

        for(String login : logins){
            String[] loginData = login.split("-");
            loggedInUsers.put(loginData[0], loginData[1]);
        }

        return "login sync successful";
    }

    String handleLogin(String data, String id){
        String[] loginData = data.split(" ", 2); //Teilen in Name und Passwort

        String name = loginData[0];
        String password = loginData[1];

        for (UserData user : userData) {
            if (user.name.equals(name) && user.password.equals(password)) {
                loggedInUsers.put(id, user.name); //ID mit User verknüpfen

                syncLoginToSecondServer();

                return "Logged in as '" + user.name + "' on client with id: " + id;
            }
        }
        return "ERROR: wrong username or password";
    }

    String handleMessage(String data, String id, String timestamp){
        String[] messageData = data.split(" ", 2); //Teilen in Empfänger und Nachricht
        String message = messageData[1];
        String receiver = messageData[0];

        if(loggedInUsers.containsKey(id)){ //Prüfen ob Client angemeldet ist
            String sender = loggedInUsers.get(id);
            messages.add(new Message(sender, receiver, message, timestamp)); //Nachricht abspeichern

            syncMessagesToSecondServer();
            DataToFileWriter.writeMessagesToFile(messages, port + "");

            return "Message from '" + sender + "' to '" + receiver + "': " + message;
        }else {
            return "ERROR: You are not logged in!";
        }
    }


    String handleGetConversation(String data, String id){
        if(loggedInUsers.containsKey(id)){ //Prüfen ob Adresse angemeldet ist
            String allMessages = "";
            String user = loggedInUsers.get(id);

            for(Message m: messages){ //Alle Nachrichten aus der Unterhaltung mit einer Person sammeln
                if((m.receiver.equals(user) && m.sender.equals(data)) || (m.sender.equals(user) && m.receiver.equals(data))){
                    allMessages += m.sender + ": " + m.message + ";";
                }
            }

            return allMessages;
        }else {
            return "ERROR: You are not logged in!";
        }
    }

    void printLoggedInUsers(){
        for(String key : loggedInUsers.keySet()){
            System.out.println("logged in user: " + loggedInUsers.get(key) + " on address: " + key);
        }
    }

    class UserData {
        UserData(String name, String password){
            this.name = name;
            this.password = password;
        }
        public String name;
        public String password;
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        int port2 = 8888;

        if(args.length > 0){
            port = Integer.parseInt(args[0]);
        }
        if(args.length > 1){
            port2 = Integer.parseInt(args[1]);
        }
        new Server().start(port, port2);
    }
}