package rs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.slr207.commons.Message;
import com.slr207.commons.MessageProcessor;
import com.slr207.commons.Receiver;
import com.slr207.commons.Sender;
import com.slr207.commons.StartMessage;

public class Master {
    
    public static void main(String[] args) {
        List<String> remoteMachines = new ArrayList<String>(
            Arrays.asList("tp-3b07-14", "tp-3b07-15", "tp-3c41-02"));
        String contentFileName = "storage.csv";
        String myIP = "";
    
        int ftpPort = 2505;
        int senderPort = 5524;
        int receiverPort = 1254;
    
        String username = "toto";
        String password = "tata";

        MessageProcessor msgProcessor = createMessageProcessor();

        MyFTPClient myFTPClient = new MyFTPClient(remoteMachines, ftpPort, username, password);
        Sender sender = new Sender(remoteMachines, senderPort);

        Receiver receiver;
        
        try {
            receiver = new Receiver(receiverPort, msgProcessor);
            Thread receiverThread = new Thread(receiver);
            receiverThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        splitStorage(contentFileName, myFTPClient);

        StartMessage startMessage = new StartMessage(remoteMachines.size(), remoteMachines, myIP);
        
        sender.sendMessage(startMessage);
    }

    private static void splitStorage(String contentFileName, MyFTPClient myFTPClient) {
        List<String> content = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(contentFileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                content.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        myFTPClient.sendDocumentsToAll(content);
    }

    private static MessageProcessor createMessageProcessor() {
        return new MessageProcessor() {
            @Override
            public void process(Message message, ObjectOutputStream out) {
                System.out.println("(Unimplemented method) Message received:");
            }
        };
    }
    
}
