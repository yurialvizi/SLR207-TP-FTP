package rs;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.slr207.commons.FinishedMessage;
import com.slr207.commons.GroupsMessage;
import com.slr207.commons.Message;
import com.slr207.commons.MyFTPClient;
import com.slr207.commons.ReduceFinishedMessage;
import com.slr207.commons.ReduceMessage;
import com.slr207.commons.SecondReduceMessage;
import com.slr207.commons.Sender;
import com.slr207.commons.StartMessage;

public class Master {
    
    public static void main(String[] args) {
        String nodesFileName = "machines.txt";
        String initialRemoteFileName = "initial-storage.txt";

        List<String> nodes = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(nodesFileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                nodes.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        int totalNodes = nodes.size();
        String storageFileName = "storage.csv";

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
        
        for (int i = 0; i < totalNodes; i++) {
            myFTPClient.sendDocuments(initialRemoteFileName, distributedContentList.get(i), nodes.get(i));
        }
        
        // Shuffle phase
        
        List<Future<?>> futures = new ArrayList<>();
        List<Sender> senders = new ArrayList<>();
        
        for (String node : nodes) {
            StartMessage startMessage = new StartMessage(totalNodes, nodes, node);
            Sender sender = new Sender(node, senderPort, startMessage);
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

        boolean allFinishedShuffle = true;
        for (Sender sender : senders) {
            Message responseMsg = sender.getResponse();
            if (!(responseMsg instanceof FinishedMessage)) {
                allFinishedShuffle = false;
                break;
            }
        }
        
        // TODO: tratar se algum nao terminou o shuffle

        // Reduce phase

        futures.clear();
        senders.clear();

        for (String node : nodes) {
            Sender sender = new Sender(node, senderPort, new ReduceMessage());
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

        List<Integer> minList = new ArrayList<>();
        List<Integer> maxList = new ArrayList<>();
        boolean allFinishedReduce = true;
        for (Sender sender : senders) {
            Message responseMsg = sender.getResponse();
            if (!(responseMsg instanceof ReduceFinishedMessage)) {
                allFinishedReduce = false;
                break;
            } else {
                ReduceFinishedMessage reduceFinishedMessage = (ReduceFinishedMessage) responseMsg;
                minList.add(reduceFinishedMessage.getMin());
                maxList.add(reduceFinishedMessage.getMax());
            }
        }

        // TODO: tratar se algum nao terminou o reduce
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

        
        // Group/second shuffle phase
        futures.clear();
        senders.clear();

        for (String node : nodes) {
            Sender sender = new Sender(node, senderPort, new GroupsMessage(groups));
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

        boolean allFinishedSecondShuffle = true;
        for (Sender sender : senders) {
            Message responseMsg = sender.getResponse();
            if (!(responseMsg instanceof FinishedMessage)) { // TODO: corrigir isso
                allFinishedSecondShuffle = false;
                break;
            }
        }

        // TODO: tratar se algum nao terminou o second shuffle

        // Second reduce phase
        futures.clear();
        senders.clear();

        for (String node : nodes) {
            Sender sender = new Sender(node, senderPort, new SecondReduceMessage());
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

        boolean allFinishedSecondReduce = true;
        for (Sender sender : senders) {
            Message responseMsg = sender.getResponse();
            if (!(responseMsg instanceof ReduceFinishedMessage)) {
                allFinishedSecondReduce = false;
                break;
            }
        }

        // TODO: tratar se algum nao terminou o second reduce

        executor.shutdown();
    }

    private static List<String> readStorageFile(String contentFileName, MyFTPClient myFTPClient, int totalNodes) {
        List<String> readContent = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(contentFileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                readContent.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return readContent;
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
