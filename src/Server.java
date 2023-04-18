import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Server {
    public static final int DEFAULT_PORT = 7777;

    //TODO: Daten in Textdatei speichern
    ArrayList<UserData> userData = new ArrayList<>(); // Speichern aller Nutzer mit Passwörter
    ArrayList<Message> messages = new ArrayList<>(); // Alle verschickten Nachrichten

    HashMap<String, UserData> userDataByName = new HashMap<>(); // Hilfsstruktur zum Bekommen der Nutzer zum Namen
    HashMap<String, String> loggedInUsers = new HashMap<>(); // Speichern welche Adresse auf welchen Nutzer angemeldet ist

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
                String lineOut = "";
                connection = server.accept();
                in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                String lineIn = in.readLine();
                String address = connection.getInetAddress().toString() + ":" +  connection.getPort();
                System.out.println("new request: '" + lineIn + "' from Address: " + address);

                String[] lineInSplit = lineIn.split(" ", 2); //Aufteilen in Befehl und Inhalt
                String command = lineInSplit[0];

                String parameter = "";
                if(lineInSplit.length > 1)
                    parameter = lineInSplit[1];

                lineOut = "ERROR";

                if (command.equals("LOGIN")) {
                    lineOut = handleLogin(parameter, address);
                }else if(command.equals("MSG")){
                    lineOut = handleMessage(parameter, address);
                }else if(command.equals("GET")){
                    lineOut = handleGetMessages(parameter, address);
                }else if(command.equals("CONV")){
                    lineOut = handleGetConversation(parameter, address);
                }else if(command.equals("SYNCMSG")) {
                    lineOut = handleMessageSyncRequest(parameter);
                }else if(command.equals("SYNCLOGIN")) {
                    lineOut = handleLoginSyncRequest(parameter);
                }

                System.out.println("sending answer: '" + lineOut + "'");
                out = new PrintWriter(connection.getOutputStream());
                out.println(lineOut); //Antwort zurückschicken
                out.flush();

                connection.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void syncMessagesToSecondServer(){
        try {
            Socket connection = new Socket("localhost", port2);
            PrintWriter out = new PrintWriter(connection.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String data = "";
            for(Message m : messages){ // alle gespeicherten Nachrichten
                data += m.serialize() + ";";
            }

            out.println("SYNCMSG " + data);
            out.flush();

            String answer = in.readLine();
            System.out.println("sync answer: " + answer);

            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    String handleMessageSyncRequest(String data){
        String[] messages = data.split(";");

        //nicht syncen wenn Anzahl der Nachrichten kleiner oder gleich der eigenen ist
        if(messages.length <= this.messages.size())
            return "message sync not necessary";

        //alle eigenen Nachrichten mit den geschickten überschreiben
        for(String m : messages){
            this.messages.add(new Message(m));
        }
        return "message sync successful";
    }


    void syncLoginToSecondServer(){
        try {
            Socket connection = new Socket("localhost", port2);
            PrintWriter out = new PrintWriter(connection.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String data = "";

            for(String address : loggedInUsers.keySet()){
                data += address + "-" + loggedInUsers.get(address) + ";";
            }

            out.println("SYNCLOGIN " + data);
            out.flush();

            String answer = in.readLine();
            System.out.println("sync answer: " + answer);

            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    String handleLoginSyncRequest(String data){
        String[] logins = data.split(";");

        for(String login : logins){
            String[] loginData = login.split("-");
            loggedInUsers.put(loginData[0], loginData[1]);
        }

        return "login sync successful";
    }

    String handleLogin(String data, String address){
        String[] loginData = data.split(" ", 2); //Teilen in Name und Passwort

        String name = loginData[0];
        String password = loginData[1];

        for (UserData user : userData) {
            if (user.name.equals(name) && user.password.equals(password)) {
                loggedInUsers.put(address, user.name); //Adresse mit User verknüpfen

                syncLoginToSecondServer();

                return "Logged in as '" + user.name + "' on address: " + address;
            }
        }
        return "ERROR: wrong username or password";
    }

    String handleMessage(String data, String address){
        String[] messageData = data.split(" ", 2); //Teilen in Empfänger und Nachricht
        String message = messageData[1];
        String receiver = messageData[0];

        if(loggedInUsers.containsKey(address)){ //Prüfen ob Adresse angemeldet ist
            String sender = loggedInUsers.get(address);
            messages.add(new Message(sender, receiver, message)); //Nachricht abspeichern

            syncMessagesToSecondServer();

            return "Message from '" + sender + "' to '" + receiver + "': " + message;
        }else {
            return "ERROR: You are not logged in!";
        }
    }

    String handleGetMessages(String data, String address){
        if(loggedInUsers.containsKey(address)){
            String allMessages = "";
            String user = loggedInUsers.get(address);

            for(Message m: messages){
                if(m.receiver.equals(user)) {
                    allMessages += m.sender + ": " + m.message + ";";
                }
            }

            return allMessages;
        }else {
            return "ERROR: You are not logged in!";
        }
    }

    String handleGetConversation(String data, String address){
        if(loggedInUsers.containsKey(address)){ //Prüfen ob Adresse angemeldet ist
            String allMessages = "";
            String user = loggedInUsers.get(address);

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

    class Message{
        public String sender;
        public String receiver;
        public String message;

        Message(String serialized){
            String[] messageData = serialized.split("-", 3);
            String sender = messageData[0];
            String receiver = messageData[1];
            String message = messageData[2];
        }

        Message(String sender, String receiver, String message){
            this.sender = sender;
            this.receiver = receiver;
            this.message = message;
        }

        public String serialize(){
            return sender + "-" + receiver + "-" + message;
        }
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