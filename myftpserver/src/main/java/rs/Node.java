package rs;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ftpserver.FtpServer;

import com.slr207.commons.FinishedMessage;
import com.slr207.commons.FinishedMessage.FinishedPhase;
import com.slr207.commons.GroupsMessage;
import com.slr207.commons.Message;
import com.slr207.commons.MessageProcessor;
import com.slr207.commons.MyFTPClient;
import com.slr207.commons.Receiver;
import com.slr207.commons.ReduceFinishedMessage;
import com.slr207.commons.ReduceMessage;
import com.slr207.commons.SecondReduceMessage;
import com.slr207.commons.StartMessage;

public class Node {
    public static void main(String[] args) {
        int ftpPort = 2505;
        int receiverPort = 5524;

        String initialStorageFilePath = "toto/initial-storage.txt";

        MyFTPServer myFTPServer = new MyFTPServer(ftpPort);
        MyFTPClient myFTPClient = new MyFTPClient(ftpPort, "toto", "tata");

        FtpServer ftpServer = myFTPServer.createServer();
        
        try {
            Receiver receiver = new Receiver(receiverPort, createMessageProcessor(myFTPClient, initialStorageFilePath));
            Thread receiverThread = new Thread(receiver);
            receiverThread.start();
            ftpServer.start();
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

    public static MessageProcessor createMessageProcessor(MyFTPClient myFTPClient, String initialStorageFilePath) {
       return new MessageProcessor() {
            private int totalNodes;
            private List<String> nodeServerList;
            private String myServer;

            @Override
            public void process(Message message, ObjectOutputStream out) {
                if (message instanceof StartMessage) {
                    System.out.println("Received a StartMessage from the master.");
                    StartMessage startMessage = (StartMessage) message;
                    
                    totalNodes = startMessage.getTotalNodes();
                    nodeServerList = startMessage.getNodeServerList();
                    myServer = startMessage.getYourOwnServer();
                    
                    System.out.println("Total number of nodes: " + totalNodes);
                    System.out.println("IP addresses of all nodes: " + nodeServerList);
                    System.out.println("My own IP address: " + myServer);
                    
                    Map<String, Integer> mappedContent = map();
                    List<Map<String, Integer>> contentToBeSentList = distributeMap(mappedContent);
                    for (int i = 0; i < totalNodes; i++) {
                        String content = mapToString(contentToBeSentList.get(i));
                        System.out.println("Map " + i + ": " + content);
                        myFTPClient.sendDocuments("shuffled_"+ myServer +".txt", content, nodeServerList.get(i), true);
                    }

                    try {
                        out.writeObject(new FinishedMessage(FinishedPhase.FIRST_SHUFFLE));
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                } else if (message instanceof ReduceMessage) {
                    System.out.println("Received a ReduceMessage from the master.");

                    Map<String, Integer> reducedMap = reduce();

                    System.out.println("Reduced map: " + reducedMap);

                    int minValue = getMinValue(reducedMap);
                    int maxValue = getMaxValue(reducedMap);

                    System.out.println("Min value: " + minValue);
                    System.out.println("Max value: " + maxValue);

                    try {
                        out.writeObject(new ReduceFinishedMessage(minValue, maxValue));
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                } else if (message instanceof GroupsMessage) {
                    System.out.println("Received a GroupsMessage from the master.");
                    try {
                        out.writeObject(new FinishedMessage(FinishedPhase.SECOND_SHUFFLE));
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                } else if (message instanceof SecondReduceMessage) {
                    System.out.println("Received a SecondReduceMessage from the master.");
                    try {
                        out.writeObject(new ReduceFinishedMessage(0, 5));
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Received a message from the master.");
                    System.out.println("Message: " + message);
                }
            }

            private Map<String, Integer> map() {
                Map<String, Integer> wordCount = new HashMap<>();
        
                try (BufferedReader reader = new BufferedReader(new FileReader(initialStorageFilePath))) {
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
    
            private List<Map<String, Integer>> distributeMap(Map<String, Integer> originalMap) {
                List<Map<String, Integer>> mapList = new ArrayList<>(totalNodes);
                for (int i = 0; i < totalNodes; i++) {
                    mapList.add(new HashMap<>());
                }
        
                originalMap.forEach((key, value) -> {
                    int index = Math.abs(key.hashCode()) % totalNodes;
                    mapList.get(index).put(key, value);
                });
        
                return mapList;
            }
        
            private String mapToString(Map<String, Integer> map) {
                StringBuilder mapAsString = new StringBuilder();
                for (String key : map.keySet()) {
                    mapAsString.append(key).append(":").append(map.get(key)).append("\n");
                }
                // Remove the last comma if there is any
                if (mapAsString.length() > 0) {
                    mapAsString.deleteCharAt(mapAsString.length() - 1);
                }
                return mapAsString.toString();
            }

            private Map<String, Integer> reduce() {
                Map<String, Integer> reducedMap = new HashMap<>();
                
                for (int i = 0; i < totalNodes; i++) {
                    String contentFileName = "toto/shuffled_" + nodeServerList.get(i) + ".txt";

                    try (BufferedReader reader = new BufferedReader(new FileReader(contentFileName))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String[] parts = line.split(":");
                            if (parts.length == 2) {
                                String key = parts[0].trim();
                                Integer value = Integer.parseInt(parts[1].trim());
                                reducedMap.put(key, reducedMap.getOrDefault(key, 0) + value);
                            } else {
                                System.out.println("Invalid line: " + line);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    
                }

                return reducedMap;
            }
            
            private int getMinValue(Map<String, Integer> map) {
                return map.values().stream().min(Integer::compare).orElse(0);
            }

            private int getMaxValue(Map<String, Integer> map) {
                return map.values().stream().max(Integer::compare).orElse(0);
            }
        };
    }
            

}
