import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;

public class DataToFileWriter {
    public static void writeMessagesToFile(ArrayList<Message> messages, String filename){
        try {
            FileWriter fileWriter = new FileWriter(filename + ".txt");
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            for(Message m : messages){
                bufferedWriter.write(m.serialize());
                bufferedWriter.newLine();
            }
            bufferedWriter.close();
        }catch (Exception e){
            System.out.println("Error writing messages to file");
        }
    }
}
