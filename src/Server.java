import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Server {
    ArrayList<UserData> userData = new ArrayList<>(); // Speichern aller Nutzer mit Passwörtern
    ArrayList<Message> messages = new ArrayList<>(); // Alle verschickten Nachrichten

    HashMap<String, UserData> userDataByName = new HashMap<>(); // Hilfsstruktur zum Bekommen der Nutzer zum Namen
    HashMap<String, String> loggedInUsers = new HashMap<>(); // Speichern welcher Client auf welchen Nutzer angemeldet ist

    int port, port2; //eigener Port und Port vom zweiten Server

    Server(int port, int port2){
        this.port = port;
        this.port2 = port2;

        //drei Anfangsnutzer initialisieren
        userData.add(new UserData("Tom", "111"));
        userDataByName.put("Tom", userData.get(0));

        userData.add(new UserData("Peter", "222"));
        userDataByName.put("Peter", userData.get(1));

        userData.add(new UserData("Heinz", "333"));
        userDataByName.put("Heinz", userData.get(2));
    }

    public void start(){
        this.messages = DataToFileWriter.readMessagesFromFile(String.valueOf(port)); //Nachrichten aus Datei laden

        try {
            ServerSocket server = new ServerSocket(port); // ServerSocket mit eingegebenem Port starten
            Socket connection;
            PrintWriter out;
            BufferedReader in;

            System.out.println("Server started on port " + port + ", with second server on port " + port2);
            requestMessageSync(); //Nachrichten vom zweiten Server anfordern
            requestLoginSync(); //Angemeldete Nutzer vom zweiten Server anfordern
            syncMessagesToSecondServer(); //Nachrichten an zweiten Server senden, falls lokale Datei aktueller ist

            while (true) {
                String lineOut = "ERROR"; //Antwort vom Server vorbereiten
                connection = server.accept(); //auf neue Verbindung warten
                in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                String lineIn = in.readLine(); //Anfrage lesen
                System.out.println("new request: '" + lineIn + "'");

                String [] requestSplit = lineIn.split("/", 3); //Aufteilen in ID, Zeitstempel und Befehl
                String senderId = requestSplit[0];
                String timestamp = requestSplit[1];

                String[] commandSplit = requestSplit[2].split(" ", 2); //Aufteilen in Befehl und Parameter
                String command = commandSplit[0];

                String parameter = "";
                if(commandSplit.length > 1)
                    parameter = commandSplit[1];


                //entsprechende Methode zum Befehl aufrufen
                switch (command) {
                    case "LOGIN":
                        lineOut = handleLogin(parameter, senderId);
                        break;
                    case "MSG":
                        lineOut = handleMessage(parameter, senderId, timestamp);
                        break;
                    case "CONV":
                        lineOut = handleGetConversation(parameter, senderId);
                        break;
                    case "SYNCMSG":
                        lineOut = handleMessageSync(parameter);
                        break;
                    case "SYNCLOGIN":
                        lineOut = handleLoginSync(parameter);
                        break;
                    case "RQSTMSG":
                        lineOut = handleRequestMessageSync();
                        break;
                    case "RQSTLOGIN":
                        lineOut = handleRequestLoginSync();
                        break;
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

    void requestMessageSync(){ //Anfrage an zweiten Server für einen Sync der Messages senden
        String serializedMessages = sendSyncCommand("RQSTMSG");

        if(serializedMessages.equals("sync failed"))
            return;

        handleMessageSync(serializedMessages);
    }

    void requestLoginSync(){ //Anfrage an zweiten Server für einen Sync der Logins senden
        String serializedLogins = sendSyncCommand("RQSTLOGIN");

        if(serializedLogins.equals("sync failed"))
            return;

        handleLoginSync(serializedLogins);
    }

    String handleRequestMessageSync(){ //Anfrage vom zweiten Server für einen Sync der Messages bearbeiten
        String data = "";
        for(Message m : messages){ // alle gespeicherten Nachrichten
            data += m.serialize() + ";";
        }
        return data;
    }

    String handleRequestLoginSync(){ //Anfrage vom zweiten Server für einen Sync der Logins bearbeiten
        String data = "";
        for(String address : loggedInUsers.keySet()){ //alle angemeldeten Nutzer und ihre ClientIDs
            data += address + "-" + loggedInUsers.get(address) + ";";
        }
        return data;
    }

    String sendSyncCommand(String command){ //verschiedene Sync Befehle an den zweiten Server senden
        String timestamp = String.valueOf(System.currentTimeMillis());
        String syncRequest = port + "/" + timestamp + "/" + command; //Details zum Befehl hinzufügen
        System.out.println("created sync command: " + syncRequest);

        try {
            Socket connection = new Socket("localhost", port2); //verbinden mit zweitem Server
            PrintWriter out = new PrintWriter(connection.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            out.println(syncRequest);
            out.flush(); //Befehl absenden

            String answer = in.readLine(); //Antwort lesen
            System.out.println("sync answer: " + answer);

            connection.close();
            return answer; //Antwort zurückgeben, falls von anderen Methoden gebraucht
        } catch (Exception e) {
            System.out.println("sync failed");
        }
        return "sync failed";
    }

    void syncMessagesToSecondServer(){ //alle Nachrichten an zweiten Server senden
        String data = "";
        for(Message m : messages){ // alle gespeicherten Nachrichten
            data += m.serialize() + ";";
        }

        sendSyncCommand("SYNCMSG " + data);
    }

    String handleMessageSync(String data){ //Nachrichten vom zweiten Server empfangen und speichern
        if(data.length() == 0)
            return "message sync not necessary";

        String[] messages = data.split(";");

        //nicht syncen, wenn Anzahl der Nachrichten kleiner oder gleich der eigenen ist
        if(messages.length <= this.messages.size())
            return "message sync not necessary";

        //alle eigenen Nachrichten mit den geschickten überschreiben
        this.messages.clear();
        for(String m : messages){
            this.messages.add(new Message(m));
        }
        System.out.println("synced messages from second server");
        DataToFileWriter.writeMessagesToFile(this.messages, String.valueOf(port)); //Nachrichten in Datei speichern
        return "message sync successful";
    }


    void syncLoginToSecondServer(){ //alle angemeldeten Nutzer an zweiten Server senden
        String data = "";

        for(String address : loggedInUsers.keySet()){
            data += address + "-" + loggedInUsers.get(address) + ";";
        }

        sendSyncCommand("SYNCLOGIN " + data);
    }

    String handleLoginSync(String data){ //angemeldete Nutzer vom zweiten Server empfangen und speichern
        if (data.length() == 0)
            return "login sync not necessary";

        String[] logins = data.split(";");

        for(String login : logins){
            String[] loginData = login.split("-");
            loggedInUsers.put(loginData[0], loginData[1]);
        }

        return "login sync successful";
    }

    String handleLogin(String data, String id){ //Nutzer auf ClientId anmelden
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

    String handleMessage(String data, String id, String timestamp){ //Nachricht von Client empfangen und speichern
        String[] messageData = data.split(" ", 2); //Teilen in Empfänger und Nachricht
        String message = messageData[1];
        String receiver = messageData[0];

        if(loggedInUsers.containsKey(id)){ //Prüfen ob Client angemeldet ist
            String sender = loggedInUsers.get(id);
            messages.add(new Message(sender, receiver, message, timestamp)); //Nachricht abspeichern

            syncMessagesToSecondServer();
            DataToFileWriter.writeMessagesToFile(messages, String.valueOf(port));

            return "Message from '" + sender + "' to '" + receiver + "': " + message;
        }else {
            return "ERROR: You are not logged in!";
        }
    }


    String handleGetConversation(String data, String id){ //Unterhaltung mit einer Person zurückgeben
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

    class UserData { //Klasse für Nutzerdaten
        UserData(String name, String password){
            this.name = name;
            this.password = password;
        }
        public String name;
        public String password;
    }

    public static void main(String[] args) {
        //Standartports für den ersten und zweiten Server
        int port = 7777;
        int port2 = 8888;

        //Ports können als Argumente übergeben werden
        if(args.length > 0){
            port = Integer.parseInt(args[0]);
        }
        if(args.length > 1){
            port2 = Integer.parseInt(args[1]);
        }
        new Server(port, port2).start();
    }
}