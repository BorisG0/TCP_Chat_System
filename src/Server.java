import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Server {
    public static final int DEFAULT_PORT = 7777;

    ArrayList<User> users = new ArrayList<>();
    ArrayList<Message> messages = new ArrayList<>();
    HashMap<String, User> loggedInUsers = new HashMap<>();

    Server(){
        users.add(new User("Tom", "111"));
        users.add(new User("Tim", "222"));
        users.add(new User("Henning", "333"));
    }

    public void start(){
        try {
            ServerSocket server = new ServerSocket(DEFAULT_PORT);
            Socket connection;
            PrintWriter out;
            BufferedReader in;

            System.out.println("Server started on port " + DEFAULT_PORT);

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
                }

                System.out.println("sending answer: '" + lineOut + "'");
                out = new PrintWriter(connection.getOutputStream());
                out.println(lineOut); //Antwort an Client zurückschicken
                out.flush();

                connection.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
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
        new Server().start();
    }
}