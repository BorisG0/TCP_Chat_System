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
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
