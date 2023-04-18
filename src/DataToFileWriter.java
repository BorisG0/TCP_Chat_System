import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

public class DataToFileWriter {
    public static void writeMessagesToFile(ArrayList<Message> messages, String filename){
        try {
            // damit jeder Server seine eigenen Nachrichten speichert muss der Dateiname der Port sein
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

    public static ArrayList<Message> readMessagesFromFile(String filename){
        ArrayList<Message> messages = new ArrayList<>();
        try {
            System.out.println("Reading messages from file: " + filename + ".txt");
            FileReader fileReader = new FileReader(filename + ".txt");
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            while((line = bufferedReader.readLine()) != null){
                messages.add(new Message(line));
                System.out.println("Message read from file: " + line);
            }
            bufferedReader.close();
        }catch (Exception e){
            System.out.println("Error reading messages from file");
            e.printStackTrace();
        }
        return messages;
    }
}
