package com.slr207.commons;

import java.io.ObjectOutputStream;
import java.net.Socket;

import com.slr207.commons.messages.Message;

import java.io.ObjectInputStream;
import java.io.IOException;

public class Sender implements Runnable {
    private String host;
    private int port;
    private Message message;
    private Message response;

    public Sender(String host, int port, Message message) {
        this.host = host;
        this.port = port;
        this.message = message;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(host, port);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
             
            out.writeObject(message);  // Envie a mensagem para o servidor
            System.out.println("Message sent to: " + host + " on port: " + port + " of type: " + message.getClass().getName());

            // Esperando uma resposta do servidor
            response = (Message) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error while sending message to the host: " + host + " with error message: " + e.getMessage());
            e.printStackTrace();
        }

    }

    public Message getResponse() {
        return response;
    }
}
