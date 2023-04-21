import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;

public class ServerMCS {
    ArrayList<UserData> userData = new ArrayList<>(); // Speichern aller Nutzer mit Passwörtern
    ArrayList<Message> messages = new ArrayList<>(); // Alle verschickten Nachrichten

    boolean myTurnToVote = false; // Ob der Server als nächster voten darf
    String myVote = "no response"; // aktuelles Votum des Servers
    ArrayList<Message> potentialNewMessages = new ArrayList<>(); // Nachrichten über die noch abgestimmt wird

    HashMap<String, UserData> userDataByName = new HashMap<>(); // Hilfsstruktur zum Bekommen der Nutzer zum Namen
    HashMap<String, String> loggedInUsers = new HashMap<>(); // Speichern welcher Client auf welchen Nutzer angemeldet ist

    int port; //eigener Port
    ArrayList<Integer> serverPorts; //Ports der anderen Server
    int predecessorId = -1; //ID des Vorgängers, -1 wenn es keinen gibt

    ServerMCS(int port, ArrayList<Integer> serverPorts){
        this.port = port;
        this.serverPorts = serverPorts;

        //Server stehen in einer Hierarchie, kleinere IDs voten zuerst
        if((serverPorts.size() > 0) && (serverPorts.get(0) < port))
            this.predecessorId = serverPorts.get(0); //Vorgänger ist der erste Server in der Liste

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

            System.out.println("Server started on port " + port);

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
                    case "SYNCLOGIN":
                        lineOut = handleLoginSync(parameter);
                        break;
                    case "NEWVOTING":
                        lineOut = handleNewVoting(parameter, Integer.parseInt(senderId));
                        break;
                    case "VOTE":
                        lineOut = handleVote(Integer.parseInt(senderId));
                        break;
                }

                System.out.println("sending response: '" + lineOut + "'");
                out = new PrintWriter(connection.getOutputStream());
                out.println(lineOut); //Antwort zurückschicken
                out.flush();

                connection.close();

                if(myTurnToVote) //Wenn gerade abgestimmt wird, dann nach jeder Anfrage die Votes checken
                    collectVotes();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    String serializeMessages(ArrayList<Message> messages){
        String serializedMessages = "";
        for(Message message: messages){
            serializedMessages += message.serialize() + ";";
        }
        return serializedMessages;
    }

    ArrayList<Message> deserializeMessages(String serializedMessages){
        ArrayList<Message> messages = new ArrayList<>();

        if(serializedMessages.length() == 0) return messages;

        String[] serializedMessagesSplit = serializedMessages.split(";");
        for(String serializedMessage: serializedMessagesSplit){
            messages.add(new Message(serializedMessage));
        }

        return messages;
    }

    boolean syncNewMessage(Message message){
        ArrayList<Message> potentialNewMessages = (ArrayList<Message>) messages.clone();
        potentialNewMessages.add(message);

        String serializedMessages = serializeMessages(potentialNewMessages);

        ArrayList<String> votes = new ArrayList<>();
        myVote = "YES";
        votes.add(myVote); //eigener Vote

        for(int i = serverPorts.size() - 1; i >= 0; i--){
            int serverPort = serverPorts.get(i);
            String vote = sendCommand("NEWVOTING " + serializedMessages, serverPort);
            votes.add(vote);
        }

        boolean majority = checkVotes(votes);
        if(majority){
            messages = potentialNewMessages;
            DataToFileWriter.writeMessagesToFile(messages, String.valueOf(port));
        }

        return majority;
    }

    void collectVotes(){
        ArrayList<String> votes = new ArrayList<>();
        votes.add(myVote); //eigener Vote

        for(int serverPort: serverPorts){
            String vote = sendCommand("VOTE", serverPort);
            votes.add(vote);
        }

        boolean majority = checkVotes(votes);

        if(majority){
            messages = potentialNewMessages;
            DataToFileWriter.writeMessagesToFile(messages, String.valueOf(port));
        }

        myTurnToVote = false;
    }

    boolean checkVotes(ArrayList<String> votes){
        int yes = 0;
        int no = 0;

        for(String vote: votes){
            if(vote.equals("YES")) yes++;
            else if(vote.equals("NO")) no++;
            else System.out.println("invalid vote: " + vote);
        }

        return yes > no;
    }

    String sendCommand(String command, int port){ //verschiedene Sync Befehle an bestimmten Server senden
        String timestamp = String.valueOf(System.currentTimeMillis());
        String syncRequest = this.port + "/" + timestamp + "/" + command; //notwendige Details zum Befehl hinzufügen
        System.out.println("created command: " + syncRequest);

        try {
            Socket connection = new Socket("localhost", port); //verbinden mit zweitem Server
            PrintWriter out = new PrintWriter(connection.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            out.println(syncRequest);
            out.flush(); //Befehl absenden

            String answer = in.readLine(); //Antwort lesen
            System.out.println("received response from " + port + ": " + answer);

            connection.close();
            return answer; //Antwort zurückgeben, falls von anderen Methoden gebraucht
        } catch (Exception e) {
            System.out.println("error while sending command to server on port " + port);
        }
        return "no response";
    }

    String handleNewVoting(String serializedMessages, int voteStarterId){ //Sync Befehl verarbeiten
        if(voteStarterId == predecessorId || predecessorId == -1){
            myTurnToVote = true;
        }
        myVote = "NO";

        potentialNewMessages = deserializeMessages(serializedMessages);

        // nur syncen, wenn Anzahl der neuen Nachrichten größer als eigene ist, aber nicht mehr als um eine
        if(potentialNewMessages.size() != messages.size() + 1){
            System.out.println("set my vot to NO because of different message count");
            return myVote;
        }

        // prüfen, ob alle vorherigen Nachrichten gleich sind
        for(int i = 0; i < messages.size(); i++){
            if(!messages.get(i).equals(potentialNewMessages.get(i))){
                System.out.println("set my vot to NO because of different messages");
                return myVote;
            }
        }

        myVote = "YES";
        return myVote;
    }

    String handleVote(int voterId){ //Vote Befehl verarbeiten
        if(voterId == predecessorId) myTurnToVote = true;
        return myVote;
    }

    String handleLogin(String data, String id){ //Nutzer auf ClientId anmelden
        String[] loginData = data.split(" ", 2); //Teilen in Name und Passwort

        String name = loginData[0];
        String password = loginData[1];

        for (UserData user : userData) {
            if (user.name.equals(name) && user.password.equals(password)) {
                loggedInUsers.put(id, user.name); //ID mit User verknüpfen

                syncLogins();
                return "Logged in as '" + user.name + "' on client with id: " + id;
            }
        }
        return "ERROR: wrong username or password";
    }

    void syncLogins(){
        String serializedLogins = "";

        for(String id: loggedInUsers.keySet()){
            serializedLogins += id + "-" + loggedInUsers.get(id) + ";";
        }

        for(int serverPort: serverPorts){
            sendCommand("SYNCLOGIN " + serializedLogins, serverPort);
        }
    }

    String handleMessage(String data, String id, String timestamp){ //Nachricht von Client empfangen und speichern
        if(!loggedInUsers.containsKey(id)) return "ERROR: You are not logged in!"; //Prüfen ob Client angemeldet ist

        String[] messageData = data.split(" ", 2); //Teilen in Empfänger und Inhalt

        String content = messageData[1];
        String receiver = messageData[0];
        String sender = loggedInUsers.get(id);

        Message message = new Message(sender, receiver, content, timestamp);

        boolean success = syncNewMessage(message);

        if(success){
            return "Message from '" + sender + "' to '" + receiver + "': " + content;
        }else{
            return "no consensus";
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

    String handleLoginSync(String data){ //Login Sync funktioniert ohne MCS
        if (data.length() == 0)
            return "login sync not necessary";

        String[] logins = data.split(";");

        for(String login : logins){
            String[] loginData = login.split("-");
            loggedInUsers.put(loginData[0], loginData[1]);
        }

        return "login sync successful";
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
        //Standartports
        int port = 7777;

        ArrayList<Integer> otherPorts = new ArrayList<>();
        otherPorts.add(8888);
        otherPorts.add(7788);

        //Ports können als Argumente übergeben werden
        if(args.length > 0){
            port = Integer.parseInt(args[0]);
        }
        if(args.length > 1){
            otherPorts.clear();
            for(int i = 1; i < args.length; i++){
                otherPorts.add(Integer.parseInt(args[i]));
            }
        }

        new ServerMCS(port, otherPorts).start();
    }
}