import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Server {
    public static final int DEFAULT_PORT = 7777;

    ArrayList<User> users = new ArrayList<>();
    ArrayList<Message> messages = new ArrayList<>();
    HashMap<String, User> loggedInUsers = new HashMap<>();

    int port, port2;

    Server(){
        users.add(new User("Tom", "111"));
        users.add(new User("Peter", "222"));
        users.add(new User("Heinz", "333"));
    }

    public void start(int port, int port2){
        this.port = port;
        this.port2 = port2;

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
                }else if(command.equals("SYNC")) {
                    lineOut = handleSyncRequest(parameter);
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

    void syncToSecondServer(){
        try {
            Socket connection = new Socket("localhost", port2);
            PrintWriter out = new PrintWriter(connection.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String data = "";
            for(Message m : messages){
                data += m.sender + "-" + m.receiver + "-" + m.message + ";";
            }

            out.println("SYNC " + data);
            out.flush();

            String answer = in.readLine();
            System.out.println("sync answer: " + answer);

            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    String handleSyncRequest(String data){
        String[] messages = data.split(";");

        if(messages.length <= this.messages.size())
            return "sync not necessary";

        for(String m : messages){
            String[] messageData = m.split("-", 3);
            String sender = messageData[0];
            String receiver = messageData[1];
            String message = messageData[2];
            this.messages.add(new Message(sender, receiver, message));
        }
        return "sync successful";
    }

    String handleLogin(String data, String address){
        String[] loginData = data.split(" ", 2);
        String name = loginData[0];
        String password = loginData[1];

        for (User user : users) {
            if (user.name.equals(name) && user.password.equals(password)) {
                loggedInUsers.put(address, user);
                return "Logged in as '" + user.name + "' on address: " + address;
            }
        }
        return "ERROR: wrong username or password";
    }

    String handleMessage(String data, String address){
        String[] messageData = data.split(" ", 2);
        String message = messageData[1];
        String receiver = messageData[0];

        if(loggedInUsers.containsKey(address)){
            User sender = loggedInUsers.get(address);
            messages.add(new Message(sender.name, receiver, message));

            syncToSecondServer();

            return "Message from '" + sender.name + "' to '" + receiver + "': " + message;
        }else {
            return "ERROR: You are not logged in!";
        }
    }

    String handleGetMessages(String data, String address){
        if(loggedInUsers.containsKey(address)){
            String allMessages = "";
            User user = loggedInUsers.get(address);

            for(Message m: messages){
                if(m.receiver.equals(user.name)) {
                    allMessages += m.sender + ": " + m.message + ";";
                }
            }

            return allMessages;
        }else {
            return "ERROR: You are not logged in!";
        }
    }

    String handleGetConversation(String data, String address){
        if(loggedInUsers.containsKey(address)){
            String allMessages = "";
            User user = loggedInUsers.get(address);

            for(Message m: messages){
                if((m.receiver.equals(user.name) && m.sender.equals(data)) || (m.sender.equals(user.name) && m.receiver.equals(data))){
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
            System.out.println("logged in user: " + loggedInUsers.get(key).name + " on address: " + key);
        }
    }

    class User{
        User(String name, String password){
            this.name = name;
            this.password = password;
        }
        public String name;
        public String password;
    }

    class Message{
        Message(String sender, String receiver, String message){
            this.sender = sender;
            this.receiver = receiver;
            this.message = message;
        }
        public String sender;
        public String receiver;
        public String message;
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