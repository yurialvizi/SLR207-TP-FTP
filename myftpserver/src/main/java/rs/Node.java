package rs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ftpserver.FtpServer;

import com.slr207.commons.messages.*;
import com.slr207.commons.Logger;
import com.slr207.commons.MessageProcessor;
import com.slr207.commons.MyFTPClient;
import com.slr207.commons.Receiver;

public class Node {
    public static void main(String[] args) {
        int ftpPort = 2505;
        int receiverPort = 5524;

        String absPath = "/dev/shm/ydesene-23/toto/";

        MyFTPServer myFTPServer = new MyFTPServer(ftpPort);
        MyFTPClient myFTPClient = new MyFTPClient(ftpPort, "toto", "tata");

        FtpServer ftpServer = myFTPServer.createServer();
        
        try {
            Receiver receiver = new Receiver(receiverPort, createMessageProcessor(myFTPClient, absPath));
            Thread receiverThread = new Thread(receiver);
            receiverThread.start();
            ftpServer.start();
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

    public static MessageProcessor createMessageProcessor(MyFTPClient myFTPClient, String absPath) {
       return new MessageProcessor() {
            private int totalNodes;
            private List<String> nodeServerList;
            private String myServer;
            private Map<String, String> secondMap;

            @Override
            public void process(Message message, ObjectOutputStream out) {
                printReceivedMessage(message.getType());
                if (message instanceof StartMessage) {
                    StartMessage startMessage = (StartMessage) message;
                    
                    totalNodes = startMessage.getTotalNodes();
                    nodeServerList = startMessage.getNodeServerList();
                    myServer = startMessage.getYourOwnServer();
                    
                    Logger.log("Total number of nodes: " + totalNodes);
                    Logger.log("IP addresses of all nodes: " + nodeServerList);
                    Logger.log("My own IP address: " + myServer);
                    
                    Map<String, Integer> mappedContent = map();

                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(absPath + "mapped.txt"))) {
                        writer.write(mapToString(mappedContent));
                    } catch (IOException e) {
                        e.printStackTrace();
                    } 

                    sendResponse(out, new FirstShuffleFinishedMessage());
                } else if (message instanceof FirstShuffleMessage) {
                    Map<String, Integer> mappedContent = new HashMap<>();

                    try (BufferedReader reader = new BufferedReader(new FileReader(absPath + "mapped.txt"))) {
                        StringBuilder contentBuilder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            contentBuilder.append(line).append("\n");
                        }
                        for (String keyValuePair : contentBuilder.toString().split("\n")) {
                            String[] parts = keyValuePair.split(":");
                            if (parts.length == 2) {
                                String key = parts[0].trim();
                                Integer value = Integer.parseInt(parts[1].trim());
                                mappedContent.put(key, value);
                            } else {
                                Logger.log("Invalid line: " + keyValuePair);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println("Error reading mapped.txt");
                        System.exit(1);
                    }

                    List<Map<String, Integer>> contentToBeSentList = distributeMap(mappedContent);
                    for (int i = 0; i < totalNodes; i++) {
                        String content = mapToString(contentToBeSentList.get(i));
                        myFTPClient.sendDocuments("shuffled_"+ myServer +".txt", content, nodeServerList.get(i));
                    }

                    sendResponse(out, new FirstShuffleFinishedMessage());
                } else if (message instanceof FirstReduceMessage) {
                    Map<String, Integer> reducedMap = reduce();

                    String reducedMapString = mapToString(reducedMap);

                    // Write the reduced map to a local file
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(absPath + "reduced.txt"))) {
                        writer.write(reducedMapString);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }                    
                    
                    int minValue = getMinValue(reducedMap);
                    int maxValue = getMaxValue(reducedMap);

                    Logger.log("Min value: " + minValue);
                    Logger.log("Max value: " + maxValue);

                    sendResponse(out, new FirstReduceFinishedMessage(minValue, maxValue));
                } else if (message instanceof GroupsMessage) {
                    GroupsMessage groupsMessage = (GroupsMessage) message;
                    Map<String, Map<String, Integer>> groups = groupsMessage.getGroups();

                    String reducedContent;

                    try (BufferedReader reader = new BufferedReader(new FileReader(absPath + "reduced.txt"))) {
                        StringBuilder contentBuilder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            contentBuilder.append(line).append("\n");
                        }
                        reducedContent = contentBuilder.toString();
                    } catch (IOException e) {
                        e.printStackTrace();
                        reducedContent = "";
                    }

                    secondMap = secondMap(groups, reducedContent);

                    sendResponse(out, new SecondMapFinishedMessage());
                } else if (message instanceof SecondShuffleMessage) {
                    sendContentToGroups();

                    secondMap.clear();

                    sendResponse(out, new SecondShuffleFinishedMessage());

                } else if (message instanceof SecondReduceMessage) {
                    Map<String, Integer> groupedMap = new HashMap<>();

                    // Read all grouped files and merge them into a single map
                    for (int i = 0; i < totalNodes; i++) {
                        String contentFileName = absPath + "grouped_" + nodeServerList.get(i) + ".txt";

                        try (BufferedReader reader = new BufferedReader(new FileReader(contentFileName))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                String[] parts = line.split(":");
                                if (parts.length == 2) {
                                    String key = parts[0].trim();
                                    Integer value = Integer.parseInt(parts[1].trim());
                                    groupedMap.put(key, groupedMap.getOrDefault(key, 0) + value);
                                } else {
                                    Logger.log("Invalid line: " + line);
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    // Sort map by value and then by key
                    List<Map.Entry<String, Integer>> sortedMapList = new ArrayList<>(groupedMap.entrySet());
                    sortedMapList.sort((a, b) -> {
                        int valueComparison = b.getValue().compareTo(a.getValue());
                        return valueComparison == 0 ? a.getKey().compareTo(b.getKey()) : valueComparison;
                    });

                    // Parse the sorted map to a string
                    StringBuilder sortedMapStringBuilder = new StringBuilder();
                    for (Map.Entry<String, Integer> entry : sortedMapList) {
                        sortedMapStringBuilder.append(entry.getKey())
                                            .append(":")
                                            .append(entry.getValue())
                                            .append("\n");
                    }
                    String sortedMapString = sortedMapStringBuilder.toString();

                    // Write the grouped map to a local file
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(absPath + "grouped.txt"))) {
                        writer.write(sortedMapString);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    sendResponse(out, new SecondReduceFinishedMessage());
                } else {
                    Logger.log("Received a message from the master.");
                    Logger.log("Message: " + message);
                }
            }

            private Map<String, Integer> map() {
                Map<String, Integer> wordCount = new HashMap<>();
        
                try (BufferedReader reader = new BufferedReader(new FileReader(absPath + "initial-storage.txt"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] words = line.split("\\s+");
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
                    String contentFileName = absPath + "shuffled_" + nodeServerList.get(i) + ".txt";

                    try (BufferedReader reader = new BufferedReader(new FileReader(contentFileName))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String[] parts = line.split(":");
                            if (parts.length == 2) {
                                String key = parts[0].trim();
                                Integer value = Integer.parseInt(parts[1].trim());
                                reducedMap.put(key, reducedMap.getOrDefault(key, 0) + value);
                            } else {
                                Logger.log("Invalid line: " + line);
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
        
            private Map<String, String> secondMap(Map<String, Map<String, Integer>> groups, String reducedContent) {
                Map<String, String> secondMap = new HashMap<>();
                for (String node : groups.keySet()) {
                    Map<String, Integer> group = groups.get(node);
                    int start = group.get("start");
                    int end = group.get("end");
                    // Get all keys which values are between start and end
                    StringBuilder contentBuilder = new StringBuilder();
                    for (String key : reducedContent.split("\n")) {
                        String[] parts = key.split(":");
                        if (parts.length == 2) {
                            int value = Integer.parseInt(parts[1].trim());
                            if (value >= start && value <= end) {
                                contentBuilder.append(key).append("\n");
                            }
                        }
                    }
                    secondMap.put(node, contentBuilder.toString());
                }
                return secondMap;
            }
            
            private void sendContentToGroups() {
                for (String node : secondMap.keySet()) {
                    myFTPClient.sendDocuments("grouped_" + myServer + ".txt", secondMap.get(node), node);
                }
            }

            private void printReceivedMessage(MessageType messageType) {
                Logger.log("Received a " + messageType.getDisplayName() + " message from the master.");
            }

            private void sendResponse(ObjectOutputStream out, Message message) {
                try {
                    out.writeObject(message);
                    Logger.log("Sent a " + message.getType().getDisplayName() + " message to the master.");
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        };
    }
            

}
