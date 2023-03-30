import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Server {
    public static final int DEFAULT_PORT = 7777;

    ArrayList<User> users = new ArrayList<User>();

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

            while (true) {
                String lineOut = "";
                connection = server.accept();
                in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                String lineIn = in.readLine();
                System.out.println("new request: '" + lineIn + "' from Address: " + connection.getInetAddress() + ":" + connection.getPort());

                String[] lineInSplit = lineIn.split(" ", 2); //Aufteilen in Befehl und Inhalt
                String command = lineInSplit[0];

                lineOut = "ERROR";
                if (command.equals("LOGIN")) {
                    String[] loginData = lineInSplit[1].split(" ", 2);
                    String name = loginData[0];
                    String password = loginData[1];

                    for (User user : users) {
                        if (user.name.equals(name) && user.password.equals(password)) {
                            lineOut = "OK";
                            break;
                        }
                    }
                }


                out = new PrintWriter(connection.getOutputStream());
                out.println(lineOut); //Antwort an Client zurückschicken
                out.flush();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    static class User{
        User(String name, String password){
            this.name = name;
            this.password = password;
        }
        public String name;
        public String password;
    }

    public static void main(String[] args) {
        new Server().start();
    }
}