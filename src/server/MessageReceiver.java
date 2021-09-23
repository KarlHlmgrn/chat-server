package server;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessageReceiver implements Runnable {
    private BufferedReader in;
    private String accountName;
    private ExecutorService senderThreads = Executors.newCachedThreadPool();
    private ClientHandler clientHandler;
    private String roomID;

    public MessageReceiver(BufferedReader in, String accountName, ClientHandler clientHandler) {
        this.in = in;
        this.accountName = accountName;
        this.clientHandler = clientHandler;
        this.roomID = clientHandler.roomID;
    }
    @Override
    public void run() {
        while(true) {
            try {
                String message = in.readLine();
                ChatServer.messageAccountNames.add(accountName);
                ChatServer.messages.add(message);
                MessageSender messageSender = new MessageSender(accountName, message, roomID);
                senderThreads.execute(messageSender);
                // out.println("received");
            } catch (IOException e) {
                clientHandler.error(clientHandler);
                break;
            }
        }
    }
    
}
