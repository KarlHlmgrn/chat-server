package server;

public class MessageSender implements Runnable{

    private String accountName;
    private String message;

    public MessageSender(String accountName, String message) {
        this.accountName = accountName;
        this.message = message;
    }

    @Override
    public void run() {
        for(int i = 0; i<ChatServer.outs.size(); i++) {
            ChatServer.outs.get(i).println(accountName);
            ChatServer.outs.get(i).println(message);
        }
    }
    
}
