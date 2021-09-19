package server;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;


public class ChatServer {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    public static Map<String, String> accounts = new HashMap<String, String>();
    public static ArrayList<String> onlineAccounts = new ArrayList<>();
    public static ArrayList<String> messageAccountNames = new ArrayList<>();
    public static ArrayList<String> messages = new ArrayList<>();
    public static ArrayList<ClientHandler> clients = new ArrayList<>();
    public static ArrayList<PrintWriter> outs = new ArrayList<>();
    public static ExecutorService clientThreads = Executors.newCachedThreadPool();

    public void startServer() throws IOException {
        serverSocket = new ServerSocket(4444);
        System.out.println("Started server on port 4444");
        while(true) {
            Socket clientSocket = serverSocket.accept();
            ClientHandler clientThread = new ClientHandler(clientSocket);
            clients.add(clientThread);
            clientThreads.execute(clientThread);
        }
    }

    public static void print(String message) {
        System.out.println(message);
    }

    public void stop() throws IOException {
        in.close();
        out.close();
        clientSocket.close();
        serverSocket.close();
    }

    public static void main(String[] args) throws Exception {
        ChatServer server = new ChatServer();
        server.startServer();
    }
}
