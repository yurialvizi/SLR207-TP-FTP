package com.slr207.commons;

import java.io.ObjectOutputStream;
import java.net.Socket;
import java.io.ObjectInputStream;
import java.io.IOException;

public class Sender {
    private String masterAddress;
    private int port;

    public Sender(String host, int port) {
        this.masterAddress = host;
        this.port = port;
    }

    public void sendMessage(Message message) {
        try (Socket socket = new Socket(masterAddress, port);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
             
            out.writeObject(message);  // Envie a mensagem para o servidor
            System.out.println("Message sent to the server.");

            // Esperando uma resposta do servidor
            Message response = (Message) in.readObject();
            if (response instanceof ResponseMessage) {
                System.out.println("Response from server: " + ((ResponseMessage) response).getResponseContent());
            } else {
                System.out.println("Received an unexpected message type from server.");
            }

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error while sending message to the server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
