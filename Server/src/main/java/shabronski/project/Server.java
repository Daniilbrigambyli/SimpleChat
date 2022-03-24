package shabronski.project;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {

    private List<ClientHandler> clients;

    public Server() {
        try {
            this.clients = new ArrayList<>();
            ServerSocket serverSocket = new ServerSocket(8188);
            System.out.println("Сервер запущен, ожидаем подключение..");
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Подключился новый клиент");
                ClientHandler c = new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void subscribe(ClientHandler c) {
        broadcastMessage("В чат подключился " + c.getUsername());
        clients.add(c);
        broadcastClientList();
    }

    public synchronized void unsubscribe(ClientHandler c) {
        clients.remove(c);
        broadcastMessage("из чата вышел " + c.getUsername());
        broadcastClientList();
    }

    public synchronized void broadcastMessage(String message) {
        for(ClientHandler c : clients) {
            c.sendMessage(message);
        }
    }

    public synchronized void broadcastClientList() {
        StringBuilder builder = new StringBuilder(clients.size() * 10);
        builder.append("/clients_list ");
        for(ClientHandler c : clients) {
            builder.append(c.getUsername()).append(" ");
        }
        String clientsListStr = builder.toString();
        broadcastMessage(clientsListStr);
    }

    public synchronized boolean isUsernameUsed (String username) {
        for(ClientHandler c : clients) {
            if(c.getUsername().equalsIgnoreCase(username)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void sendPersonalMessage(ClientHandler sender, String receiverUsername, String message) {
        if(sender.getUsername().equalsIgnoreCase(receiverUsername)) {
            sender.sendMessage("Нельзя отправлять личные сообщения самому себе");
            return;
        }
        for (ClientHandler c : clients) {
            if(c.getUsername().equals(receiverUsername)) {
                c.sendMessage("От " + sender.getUsername() + ": " + message);
                sender.sendMessage("Пользователю " + receiverUsername + ": " + message);
                return;
            }
        }
        sender.sendMessage("Пользователь " + receiverUsername + ": не в сети");
    }
}
