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

import com.slr207.commons.Logger;
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

        int totalNodes = args.length > 0 ? Integer.parseInt(args[0]) : 3;

        List<String> nodes = readMachinesFromResource(nodesFileName, totalNodes);

        if (nodes.size() < totalNodes) {
            Logger.log("Not enough available nodes in the file");
            return;
        }

        Logger.log("Nodes: " + nodes);

        String storageFileName = "/cal/commoncrawl/CC-MAIN-20230320083513-20230320113513-00019.warc.wet";

        ExecutorService executor = Executors.newFixedThreadPool(totalNodes);
    
        int ftpPort = 2505;
        int senderPort = 5524;
    
        String username = "toto";
        String password = "tata";

        MyFTPClient myFTPClient = new MyFTPClient(ftpPort, username, password);

        Logger.log("Reading storage file");
        List<String> distributedContentList = readStorageFile(storageFileName, myFTPClient, totalNodes);

        for (int i = 0; i < totalNodes; i++) {
            myFTPClient.prepareNode(nodes.get(i));
        }

        Logger.log("Nodes prepared");

        long tempTime = System.nanoTime();
        for (int i = 0; i < totalNodes; i++) {
            myFTPClient.sendDocuments(initialRemoteFileName, distributedContentList.get(i), nodes.get(i));
        }

        Logger.log("Initial storage sent to nodes");

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
        
        Logger.log("Min list: " + minList);
        Logger.log("Max list: " + maxList);
        int min = minList.stream().min(Integer::compare).get();
        int max = maxList.stream().max(Integer::compare).get();
        Logger.log("Min: " + min + " Max: " + max);
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

        Logger.log("Computation time: " + computationTime / 1_000_000 + " ms");
        Logger.log("Communication time: " + communicationTime / 1_000_000 + " ms");
        Logger.log("Synchronization time: " + synchronizationTime / 1_000_000 + " ms");
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
        List<StringBuilder> contentBuilders = new ArrayList<>(totalNodes);

        for (int i = 0; i < totalNodes; i++) {
            contentBuilders.add(new StringBuilder());
        }

        try (BufferedReader br = new BufferedReader(new FileReader(contentFileName))) {
            String line;
            for (int i = 0; (line = br.readLine()) != null; i++) {
                line = line.replaceAll("[^a-zA-Z0-9\\s]", "");
                if (line.trim().isEmpty()) {
                    continue;
                }
                contentBuilders.get(i % totalNodes).append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<String> contentList = new ArrayList<>(totalNodes);
        for (StringBuilder builder : contentBuilders) {
            contentList.add(builder.toString());
        }
        
        return contentList;
    }
}
