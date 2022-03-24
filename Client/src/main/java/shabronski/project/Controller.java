package shabronski.project;


import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Controller {

    @FXML TextField messageField, usernameField;
    @FXML TextArea chatArea;
    @FXML HBox authPanel, msgPanel;
    @FXML ListView<String> clientListView;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    public void setAuthorized(boolean authorized) {
        msgPanel.setVisible(authorized);
        msgPanel.setManaged(authorized);
        authPanel.setVisible(!authorized);
        authPanel.setManaged(!authorized);
        clientListView.setVisible(authorized);
        clientListView.setManaged(authorized);
    }

    public void sendMessage() {
        try {
            out.writeUTF(messageField.getText());
            messageField.clear();
            messageField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendCloseRequest() {
        try {
            if (out != null) {
                out.writeUTF("/exit");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void auth() {
        connect();
        try {
            out.writeUTF("/auth " + usernameField.getText());
            usernameField.clear();
        } catch (IOException e) {
            showError("Не удалось авторизоваться на сервере");
        }
    }

    public void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).showAndWait();

    }

    public void connect() {
        if(socket != null && !socket.isClosed()) {
            return;
        }
        try {
            socket = new Socket("localhost", 8188);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            new Thread(() -> {
                readMessagesFromServer();
            }).start();

        } catch (IOException e) {
            showError("Не удалось подключиться к серверу");
        }
    }

    private void readMessagesFromServer() {
        try {
            while (true) {
                String inputMessage = in.readUTF();
                if(inputMessage.startsWith("/exit")){
                    closeConnection();
                }
                if(inputMessage.startsWith("/authOk")){
                    setAuthorized(true);
                    break;
                }
                chatArea.appendText(inputMessage + "\n");
            }
            while (true) {
                String inputMessage = in.readUTF();
                if (inputMessage.startsWith("/")) {
                    if(inputMessage.equals("/exit")) {
                        break;
                    }
                        Platform.runLater(() -> {
                            if (inputMessage.startsWith("/clients_list ")) {
                                String[] tokens = inputMessage.split("\\s+");
                                clientListView.getItems().clear();
                                for (int i = 1; i < tokens.length; i++) {
                                    clientListView.getItems().add(tokens[i]);
                                }
                            }
                        });
                    continue;
                }
                chatArea.appendText(inputMessage + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
    }

    private void closeConnection() {
        setAuthorized(false);
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

    public void listViewClick(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            String selectUser = clientListView.getSelectionModel().getSelectedItem();
            messageField.setText("/w " + selectUser + " ");
            messageField.requestFocus();
            messageField.selectEnd();
        }
    }
}