package shabronski.project;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {

    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            this.in =  new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            new Thread(() -> {
                logic();
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void logic() {
        try {
            while (!authoRize(in.readUTF()));
            while (sendServerMessage(in.readUTF()));
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Клиент " + username + " отключился");
            server.unsubscribe(this);
            closeConnection();
        }
    }

    private boolean sendServerMessage(String inputMessage) {
        if (inputMessage.startsWith("/")) {
            if (inputMessage.equals("/exit")) {
                sendMessage("/exit");
                return false;
            }
            if (inputMessage.startsWith("/w ")) {
                String[] tokens = inputMessage.split("\\s+", 3);
                server.sendPersonalMessage(this, tokens[1], tokens[2]);
            }
            return true;
        }
        server.broadcastMessage(username + ": " + inputMessage);
        return true;
    }

    private boolean authoRize(String message) {
        if (message.startsWith("/auth ")) {
            String[] tokens = message.split("\\s+");
            if (tokens.length == 1) {
                sendMessage("Укажите имя пользователя");
                return false;
            }
            if (tokens.length > 2) {
                sendMessage("Имя пользователя не может состять из нескольких слов");
                return false;
            }
            String selectedUsername = tokens[1];
            if (server.isUsernameUsed(selectedUsername)) {
                sendMessage("Имя пользователя занято");
                return false;
            }
            username = selectedUsername;
            sendMessage("/authOk");
            server.subscribe(this);
            return true;
        } else {
            System.out.println("Необходимо авторизоваться");
            return false;
        }
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeConnection() {
        try {
            if(in != null) {
                in.close();
            }
        } catch (IOException e) {
                e.printStackTrace();
            }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
                e.printStackTrace();
            }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUsername() {
        return username;
    }

}
