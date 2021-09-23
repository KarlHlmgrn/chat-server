package server;

public class MessageSender implements Runnable{

    private String accountName;
    private String message;
    private String roomID;

    public MessageSender(String accountName, String message, String roomID) {
        this.accountName = accountName;
        this.message = message;
        this.roomID = roomID;
    }

    @Override
    public void run() {
        for(int i = 0; i<ChatServer.roomIDS.get(roomID).size(); i++) {
            ChatServer.roomIDS.get(roomID).get(i).println(accountName);
            ChatServer.roomIDS.get(roomID).get(i).println(message);
        }
    }
    
}
