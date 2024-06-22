package rs;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.slr207.commons.MyFTPClient;
import com.slr207.commons.Sender;
import com.slr207.commons.messages.*;

public class Master {
    
    public static void main(String[] args) {
        long computationTime = 0;
        long communicationTime = 0;
        long synchronizationTime = 0;

        String nodesFileName = "/machines.txt";
        String initialRemoteFileName = "initial-storage.txt";

        int totalNodes = 3;

        List<String> nodes = readMachinesFromResource(nodesFileName, totalNodes);

        if (nodes.size() < totalNodes) {
            System.out.println("Not enough available nodes in the file");
            return;
        }

        System.out.println("Nodes: " + nodes);

        String storageFileName = "/cal/commoncrawl/CC-MAIN-20230320083513-20230320113513-00019.warc.wet";

        ExecutorService executor = Executors.newFixedThreadPool(totalNodes);
    
        int ftpPort = 2505;
        int senderPort = 5524;
    
        String username = "toto";
        String password = "tata";

        MyFTPClient myFTPClient = new MyFTPClient(ftpPort, username, password);

        List<String> readContent = readStorageFile(storageFileName, myFTPClient, totalNodes);

        List<String> distributedContentList = distributeContentLines(readContent, totalNodes);

        for (int i = 0; i < totalNodes; i++) {
            myFTPClient.prepareNode(nodes.get(i));
        }
        
        long tempTime = System.nanoTime();
        for (int i = 0; i < totalNodes; i++) {
            myFTPClient.sendDocuments(initialRemoteFileName, distributedContentList.get(i), nodes.get(i));
        }

        communicationTime += System.nanoTime() - tempTime;

        List<Future<?>> futures = new ArrayList<>();
        List<Sender> senders = new ArrayList<>();
        
        // ********** FIRST MAP PHASE **********
        
        tempTime = System.nanoTime();
        for (String node : nodes) {
            StartMessage startMessage = new StartMessage(totalNodes, nodes, node);
            Sender sender = new Sender(node, senderPort, startMessage);
            senders.add(sender);
            futures.add(executor.submit(sender));
        }
        synchronizationTime += System.nanoTime() - tempTime;

        tempTime = System.nanoTime();

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        computationTime += System.nanoTime() - tempTime;
        
        // ********** FIRST SHUFFLE PHASE **********

        futures.clear();
        senders.clear();
        
        tempTime = System.nanoTime();
        FirstShuffleMessage shuffleMessage = new FirstShuffleMessage();
        for (String node : nodes) {
            Sender sender = new Sender(node, senderPort, shuffleMessage);
            senders.add(sender);
            futures.add(executor.submit(sender));
        }
        
        // Wait for all the threads to finish
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        communicationTime += System.nanoTime() - tempTime;
        
        // ********** FIRST REDUCE PHASE **********

        futures.clear();
        senders.clear();
        
        tempTime = System.nanoTime();
        for (String node : nodes) {
            Sender sender = new Sender(node, senderPort, new FirstReduceMessage());
            senders.add(sender);
            futures.add(executor.submit(sender));
        }

        synchronizationTime += System.nanoTime() - tempTime;

        tempTime = System.nanoTime();

        // Wait for all the threads to finish
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        List<Integer> minList = new ArrayList<>();
        List<Integer> maxList = new ArrayList<>();
        for (Sender sender : senders) {
            Message responseMsg = sender.getResponse();
            FirstReduceFinishedMessage reduceFinishedMessage = (FirstReduceFinishedMessage) responseMsg;
            minList.add(reduceFinishedMessage.getMin());
            maxList.add(reduceFinishedMessage.getMax());
        }
        
        System.out.println("Min list: " + minList);
        System.out.println("Max list: " + maxList);
        int min = minList.stream().min(Integer::compare).get();
        int max = maxList.stream().max(Integer::compare).get();
        System.out.println("Min: " + min + " Max: " + max);
        int groupSize = (int) Math.ceil((double) (max - min + 1) / totalNodes);
        Map<String, Map<String, Integer>> groups = new HashMap<>();
        
        for (int i = 0; i < totalNodes; i++) {
            int start = min + i * groupSize;
            int end = Math.min(max, start + groupSize - 1);
            Map<String, Integer> group = new HashMap<>();
            group.put("start", start);
            group.put("end", end);
            groups.put(nodes.get(i), group);
        }
        
        computationTime += System.nanoTime() - tempTime;
        
        // ********** SECOND MAP PHASE **********

        futures.clear();
        senders.clear();

        tempTime = System.nanoTime();
        for (String node : nodes) {
            Sender sender = new Sender(node, senderPort, new GroupsMessage(groups));
            senders.add(sender);
            futures.add(executor.submit(sender));
        }

        synchronizationTime += System.nanoTime() - tempTime;

        tempTime = System.nanoTime();

        // Wait for all the threads to finish
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        computationTime += System.nanoTime() - tempTime;

        // ********** SECOND SHUFFLE PHASE **********

        futures.clear();
        senders.clear();

        tempTime = System.nanoTime();
        for (String node : nodes) {
            Sender sender = new Sender(node, senderPort, new SecondShuffleMessage());
            senders.add(sender);
            futures.add(executor.submit(sender));
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        communicationTime += System.nanoTime() - tempTime;

        // ********** SECOND REDUCE PHASE **********

        futures.clear();
        senders.clear();

        tempTime = System.nanoTime();
        for (String node : nodes) {
            Sender sender = new Sender(node, senderPort, new SecondReduceMessage());
            senders.add(sender);
            futures.add(executor.submit(sender));
        }

        synchronizationTime += System.nanoTime() - tempTime;

        tempTime = System.nanoTime();

        // Wait for all the threads to finish
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        computationTime += System.nanoTime() - tempTime;

        executor.shutdown();

        System.out.println("Computation time: " + computationTime / 1_000_000 + " ms");
        System.out.println("Communication time: " + communicationTime / 1_000_000 + " ms");
        System.out.println("Synchronization time: " + synchronizationTime / 1_000_000 + " ms");
    }

    public static List<String> readMachinesFromResource(String resourcePath, int totalNodes) {
        List<String> machines = new ArrayList<>();
        try (InputStream inputStream = MyFTPClient.class.getResourceAsStream(resourcePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null && machines.size() < totalNodes) {
                machines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return machines;
    }

    private static List<String> readStorageFile(String contentFileName, MyFTPClient myFTPClient, int totalNodes) {
        List<String> contentRead = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(contentFileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.replaceAll("[^a-zA-Z0-9\\s]", "");
                contentRead.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return contentRead;
    }

    private static List<String> distributeContentLines(List<String> content, int totalNodes) {
        List<String> contentList = new ArrayList<>();
        for (int i = 0; i < totalNodes; i++) {
            contentList.add(new String());
        }

        // Distribute content lines separated by a newline character to each node 
        for (int i = 0; i < content.size(); i++) {
            contentList.set(i % totalNodes, contentList.get(i % totalNodes) + content.get(i) + "\n");
        }

        return contentList;
    }
    
}
