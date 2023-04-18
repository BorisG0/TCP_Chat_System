public class Message{
    public String sender;
    public String receiver;
    public String message;
    public String timestamp;

    Message(String serialized){
        String[] messageData = serialized.split("-", 3);
        this.sender = messageData[0];
        this.receiver = messageData[1];
        this.timestamp = messageData[2];
        this.message = messageData[3];
    }

    Message(String sender, String receiver, String message, String timestamp){
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.timestamp = String.valueOf(System.currentTimeMillis());
    }

    public String serialize(){
        return sender + "-" + receiver + "-" + timestamp + "-" + message;
    }
}
