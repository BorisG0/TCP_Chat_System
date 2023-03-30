import java.net.ServerSocket;

public class Server {
    public static final int DEFAULT_PORT = 7777;
    public static void main(String[] args) {
        try{
            ServerSocket server = new ServerSocket(DEFAULT_PORT);

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
