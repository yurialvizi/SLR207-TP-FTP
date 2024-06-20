package com.slr207.commons;


import java.net.ServerSocket;
import java.net.Socket;

import com.slr207.commons.messages.Message;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;

public class Receiver implements Runnable {
    private ServerSocket serverSocket;
    private MessageProcessor messageProcessor;

    public Receiver(int port, MessageProcessor processor) throws IOException {
        serverSocket = new ServerSocket(port);
        this.messageProcessor = processor;
        System.out.println("Receiver initialized on port " + port);
    }

    @Override
    public void run() {
        System.out.println("Receiver is running");
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try (Socket clientSocket = serverSocket.accept();
                     ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                     ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {

                    Message message = (Message) in.readObject();
                    if (message != null) {
                        messageProcessor.process(message, out);
                    }
                } catch (ClassNotFoundException e) {
                    System.out.println("Class not found: " + e.getMessage());
                } catch (IOException e) {
                    System.out.println("IO Error: " + e.getMessage());
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }
                }
            }
        } finally {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.out.println("Error closing server socket: " + e.getMessage());
            }
        }
    }
}