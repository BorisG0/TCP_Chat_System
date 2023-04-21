import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

public class DataToFileWriter {
    public static void writeMessagesToFile(ArrayList<Message> messages, String filename){
        try {
            // damit jeder Server seine eigenen Nachrichten speichert, muss der Dateiname der Port sein
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
            FileReader fileReader = new FileReader(filename + ".txt");
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            while((line = bufferedReader.readLine()) != null){
                if(line.length() > 0){
                    messages.add(new Message(line));
                }
            }
            bufferedReader.close();
        }catch (Exception e){
            System.out.println("Error reading messages from file");
        }
        return messages;
    }
}
