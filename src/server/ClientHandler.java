package server;

import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class ClientHandler implements Runnable{
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    public String accountName;
    private ExecutorService receiverThread = Executors.newSingleThreadExecutor();
    public String roomID;

    public ClientHandler(Socket socket) throws IOException{
        this.clientSocket = socket;
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        ChatServer.outs.add(out);
    }

    public boolean accountHandler() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        while(true) {
            String request = in.readLine();
            if(request.equals("logIn")) {
                if(userLogIn()) {
                    ChatServer.onlineAccounts.add(accountName);
                    return true;
                }
            } else if(request.equals("createAccount")) {
                if(userCreateAccount(null, null)) {
                    ChatServer.onlineAccounts.add(accountName);
                    return true;
                }
            }
        }
    }

    public boolean userCreateAccount(String accountName, String password) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        if(accountName == null && password == null) {
            accountName = in.readLine();
            password = in.readLine();
        }
        if(ChatServer.accounts.containsKey(accountName)) {
            out.println("There is already an account associated with this username!");
            return false;
        }
        if(password.length() >= 6) {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            Base64.Encoder enc = Base64.getEncoder();
            ChatServer.accounts.put(accountName, (enc.encodeToString(salt) + "$" + enc.encodeToString(hash)));
            out.println("success");
            this.accountName = accountName;
            return true;
        } else {
            out.println("Your password must contain at least 6 characters.");
            return false;
        }
    }

    public boolean userLogIn() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        String accountName = in.readLine();
        String password = in.readLine();
        if(ChatServer.onlineAccounts.contains(accountName)) {
            out.println("You can't login with an account that's already online");
            return false;
        }
        if(ChatServer.accounts.containsKey(accountName)) {
            Base64.Encoder enc = Base64.getEncoder();
            Base64.Decoder decoder = Base64.getDecoder();
            String salt = ChatServer.accounts.get(accountName).split("\\$")[0];
            String storedHash = ChatServer.accounts.get(accountName).split("\\$")[1];
            KeySpec spec = new PBEKeySpec(password.toCharArray(), decoder.decode(salt), 65536, 128);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            if(enc.encodeToString(hash).equals(storedHash)) {
                out.println("success");
                this.accountName = accountName;
                return true;
            } else {
                out.println("Wrong password!");
                return false;
            }
        } else {
            out.println("No account has the username " + accountName);
            return false;
        }
    }

    public void roomHandler() throws IOException {
        while(true) {
            String request = in.readLine();
            if(request.equals("createRoom")) {
                while(true) {
                    Random random = new Random();
                    int roomIDint = random.nextInt(0xFFFFFF + 1);
                    String formattedRoomID = String.format("%06x", roomIDint);
                    if(!ChatServer.roomIDS.keySet().contains(formattedRoomID)) {
                        ArrayList<PrintWriter> roomOuts = new ArrayList<>();
                        roomOuts.add(out);
                        ChatServer.roomIDS.put(formattedRoomID, roomOuts);
                        roomID = formattedRoomID;
                        ArrayList<String> temp = new ArrayList<>();
                        temp.add(accountName);
                        ChatServer.onlineAccountsInRoom.put(roomID, temp);
                        out.println(formattedRoomID);
                        break;
                    }
                }
                break;
            } else if(request.equals("joinRoom")) {
                roomID = in.readLine();
                if(ChatServer.roomIDS.keySet().contains(roomID)) {
                    ChatServer.roomIDS.get(roomID).add(out);
                    out.println("success");
                    for(String user : ChatServer.onlineAccountsInRoom.get(roomID)) {
                        if(!user.equals(accountName)) {
                            out.println(user);
                        }
                    }
                    out.println("done");
                    ChatServer.onlineAccountsInRoom.get(roomID).add(accountName);
                    break;
                } else {
                    out.println("There isn't a room with this ID");
                }
            }
        }
    }

    public void error(ClientHandler clientHandler) {
        ChatServer.roomIDS.get(roomID).remove(out);
        ChatServer.onlineAccounts.remove(accountName);
        ChatServer.onlineAccountsInRoom.get(roomID).remove(accountName);
        MessageSender messageSender = new MessageSender(accountName, "Left the room", roomID);
        messageSender.run();
        ChatServer.print(accountName + " disconnected from the server");
        receiverThread.shutdown();
        try {
            in.close();
            clientSocket.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        out.close();
        Thread.currentThread().interrupt();
    }

    @Override
    public void run() {
        try {
            accountHandler();
            roomHandler();
            ChatServer.print(accountName + " connected to room " + roomID);
            MessageSender messageSender = new MessageSender(accountName, "Just joined the chat!", roomID);
            messageSender.run();
            MessageReceiver messageReceiver = new MessageReceiver(in, accountName, this);
            receiverThread.execute(messageReceiver);
        } catch (IOException e) {
            ChatServer.outs.remove(out);
            ChatServer.onlineAccounts.remove(accountName);
            receiverThread.shutdown();
            try {
                in.close();
                clientSocket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            out.close();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }
}
