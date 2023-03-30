import java.io.*;
import java.net.*;

public class Server {
    public static final int DEFAULT_PORT = 7777;
    public static void main(String[] args) {
        try{
            ServerSocket server = new ServerSocket(DEFAULT_PORT);
            Socket connection;
            PrintWriter out;
            BufferedReader in;

            while(true){
                String lineOut = "";
                connection = server.accept();
                in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String lineIn = in.readLine();

                System.out.println("new message: " + lineIn);
                lineOut = "OK";

                out = new PrintWriter(connection.getOutputStream());
                out.println(lineOut); //Antwort an Client zurückschicken
                out.flush();
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
