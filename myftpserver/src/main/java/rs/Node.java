package rs;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.ftpserver.FtpServer;

import com.slr207.commons.Message;
import com.slr207.commons.MessageProcessor;
import com.slr207.commons.Receiver;
import com.slr207.commons.StartMessage;

public class Node {
    public static void main(String[] args) {
        int ftpPort = 2505;
        int receiverPort = 5524;

        MyFTPServer myFTPServer = new MyFTPServer(ftpPort);

        FtpServer ftpServer = myFTPServer.createServer();

        
        try {
            Receiver receiver = new Receiver(receiverPort, createMessageProcessor());
            Thread receiverThread = new Thread(receiver);
            receiverThread.start();
            ftpServer.start();
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    public static MessageProcessor createMessageProcessor() {
        return new MessageProcessor() {
            @Override
            public void process(Message message, ObjectOutputStream out) {
                if (message instanceof StartMessage) {
                    Map<String, Integer> mappedContent = map((StartMessage) message, out);
                    Map<String, Integer>[] contentToBeSentList = distributeMap(mappedContent);
                    String stringMap_1 = mapToString(contentToBeSentList[0]);
                    String stringMap_2 = mapToString(contentToBeSentList[1]);
                    String stringMap_3 = mapToString(contentToBeSentList[2]);
                    System.out.println("Map 1: " + stringMap_1);
                    System.out.println("Map 2: " + stringMap_2);
                    System.out.println("Map 3: " + stringMap_3);
                }
            }
        };
    }

    private static Map<String, Integer> map(StartMessage message, ObjectOutputStream out) {
        System.out.println("Received a StartMessage from the master.");
        System.out.println("Total number of nodes: " + message.getTotalNodes());
        System.out.println("IP addresses of all nodes: " + message.getNodeIPList());
        System.out.println("IP address of this node: " + message.getMasterIP());

        return countWordsFromFile("toto/bonjour.txt"); // TODO: Change this to a variable

    }

    private static Map<String, Integer> countWordsFromFile(String filePath) {
        Map<String, Integer> wordCount = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] words = line.split(",");
                for (String word : words) {
                    word = word.trim();
                    if (!word.isEmpty()) {
                        wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return wordCount;
    }

    private static Map<String, Integer>[] distributeMap(Map<String, Integer> originalMap) {
        Map<String, Integer>[] maps = new Map[3];
        for (int i = 0; i < 3; i++) {
            maps[i] = new HashMap<>();
        }

        originalMap.forEach((key, value) -> {
            int index = Math.abs(key.hashCode()) % 3;
            maps[index].put(key, value);
        });

        return maps;
    }

    private static String mapToString(Map<String, Integer> map) {
        StringBuilder mapAsString = new StringBuilder("");
        for (String key : map.keySet()) {
            mapAsString.append(key + ":" + map.get(key) + ",");
        }
        mapAsString.delete(mapAsString.length()-2, mapAsString.length()).append("\n");
        return mapAsString.toString();
    }
}
