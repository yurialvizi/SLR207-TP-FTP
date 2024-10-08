package rs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
    private static final String login = "ydesene-23";
    private static final String absPath = "/dev/shm/" + login + "/";
    private static final List<String> storageFileNames = Arrays.asList(
        "/cal/commoncrawl/CC-MAIN-20230320083513-20230320113513-00019.warc.wet",
        "/cal/commoncrawl/CC-MAIN-20230320083513-20230320113513-00020.warc.wet"
    );
    private static String metricsFileName;
    private static String fileContent;
    private static List<String> machines;
    
    public static void main(String[] args) {
        int maxNodes = args.length > 0 ? Integer.parseInt(args[0]) : 3;
        
        // Create the metrics file with timestamp formatted
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HHmmss");
        String currentTimestamp = sdf.format(new Date());

        metricsFileName = absPath + "metrics/metrics_" + currentTimestamp + ".csv";

        createMetricsDirectory();

        // Read the storage file
        Logger.log("Reading storage file");
        readStorageFile();

        // Read the machines file
        String nodesFileName = "/machines.txt";
        machines = readMachinesFromResource(nodesFileName, maxNodes);

        if (machines.size() < maxNodes) {
            Logger.log("Not enough available machines in the file");
            return;
        }

        for (int i = 0; i < maxNodes; i++) {
            System.out.println();
            Logger.log("-----------Executing for " + (i + 1) + " nodes");
            executeForTotalNodes(i + 1);
        }
    }
    
    public static void executeForTotalNodes(int totalNodes) {
        long computationTime = 0;
        long communicationTime = 0;
        long synchronizationTime = 0;

        Map<String, Long> phaseTimes = new HashMap<>();
        phaseTimes.put("FirstMap", 0L);
        phaseTimes.put("FirstShuffle", 0L);
        phaseTimes.put("FirstReduce", 0L);
        phaseTimes.put("SecondMap", 0L);
        phaseTimes.put("SecondShuffle", 0L);
        phaseTimes.put("SecondReduce", 0L);
        
        String initialRemoteFileName = "initial-storage.txt";

        List<String> nodes = new ArrayList<>(machines.subList(0, totalNodes));
        
        Logger.log("Nodes: " + nodes);
        
        ExecutorService executor = Executors.newFixedThreadPool(totalNodes);
    
        int ftpPort = 2505;
        int senderPort = 5524;
    
        String username = "toto";
        String password = "tata";

        List<MyFTPClient> ftpClients = new ArrayList<>();
        for (int i = 0; i < totalNodes; i++) {
            MyFTPClient ftpClient = new MyFTPClient(nodes.get(i), ftpPort, username, password);
            ftpClient.connect();
            ftpClients.add(ftpClient);
        }

        List<String> distributedContentList = distributeContent(totalNodes);

        for (int i = 0; i < totalNodes; i++) {
            ftpClients.get(i).prepareNode();
        }

        Logger.log("Nodes prepared");

        List<Future<?>> futures = new ArrayList<>();
        List<Sender> senders = new ArrayList<>();

        long tempTime = System.nanoTime();
        for (int i = 0; i < totalNodes; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                ftpClients.get(index).sendDocuments(initialRemoteFileName, distributedContentList.get(index));
            }));
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Logger.log("Initial storage sent to nodes");

        communicationTime += System.nanoTime() - tempTime;

        futures.clear();
        
        // ********** FIRST MAP PHASE **********
        
        tempTime = System.nanoTime();
        long tempPhaseTime = System.nanoTime();
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
        phaseTimes.put("FirstMap", System.nanoTime() - tempPhaseTime);
        
        // ********** FIRST SHUFFLE PHASE **********

        futures.clear();
        senders.clear();
        
        tempTime = System.nanoTime();
        tempPhaseTime = System.nanoTime();
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
        phaseTimes.put("FirstShuffle", System.nanoTime() - tempPhaseTime);

        // ********** FIRST REDUCE PHASE **********

        futures.clear();
        senders.clear();
        
        tempTime = System.nanoTime();
        tempPhaseTime = System.nanoTime();
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
        phaseTimes.put("FirstReduce", System.nanoTime() - tempPhaseTime);
        
        // ********** SECOND MAP PHASE **********

        futures.clear();
        senders.clear();

        tempTime = System.nanoTime();
        tempPhaseTime = System.nanoTime();
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
        phaseTimes.put("SecondMap", System.nanoTime() - tempPhaseTime);

        // ********** SECOND SHUFFLE PHASE **********

        futures.clear();
        senders.clear();

        tempTime = System.nanoTime();
        tempPhaseTime = System.nanoTime();
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
        phaseTimes.put("SecondShuffle", System.nanoTime() - tempPhaseTime);

        // ********** SECOND REDUCE PHASE **********

        futures.clear();
        senders.clear();

        tempTime = System.nanoTime();
        tempPhaseTime = System.nanoTime();
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
        phaseTimes.put("SecondReduce", System.nanoTime() - tempPhaseTime);

        executor.shutdown();

        for (MyFTPClient ftpClient : ftpClients) {
            ftpClient.disconnect();
        }

        writeMetrics(totalNodes, computationTime, communicationTime, synchronizationTime, phaseTimes);
    }

    private static void createMetricsDirectory() {
        File directory = new File("/dev/shm/" + login + "/metrics");
        if (!directory.exists()) {
            if(directory.mkdirs()) {
                Logger.log("Metrics directory created");
            } else {
                Logger.log("Failed to create metrics directory");
            }
        }
    }

    private static void writeMetrics(int totalNodes, long computationTime, long communicationTime,
            long synchronizationTime, Map<String, Long> phaseTimes) {
        boolean fileExists = new File(metricsFileName).exists();

        Logger.log("Writing metrics to file: " + metricsFileName);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(metricsFileName, true))) {
            if (!fileExists) {
                writer.write("Number of Nodes,"
                            + "Computation Time (us),"
                            + "Communication Time (us),"
                            + "Synchronization Time (us),"
                            + "First Map Time (us),"
                            + "First Shuffle Time (us),"
                            + "First Reduce Time (us),"
                            + "Second Map Time (us),"
                            + "Second Shuffle Time (us),"
                            + "Second Reduce Time (us)");
                writer.newLine();
            }
            writer.write(totalNodes + "," 
                        + computationTime / 1_000 + "," 
                        + communicationTime / 1_000 + "," 
                        + synchronizationTime / 1_000 + ","
                        + phaseTimes.get("FirstMap") / 1_000 + ","
                        + phaseTimes.get("FirstShuffle") / 1_000 + ","
                        + phaseTimes.get("FirstReduce") / 1_000 + ","
                        + phaseTimes.get("SecondMap") / 1_000 + ","
                        + phaseTimes.get("SecondShuffle") / 1_000 + ","
                        + phaseTimes.get("SecondReduce") / 1_000);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> readMachinesFromResource(String resourcePath, int totalNodes) {
        List<String> machines = new ArrayList<>();
        try (InputStream inputStream = MyFTPClient.class.getResourceAsStream(resourcePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null && machines.size() <= totalNodes) {
                machines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        machines.remove(0); // Remove the first line (master node)
        return machines;
    }

    private static void readStorageFile() {
        StringBuilder contentBuilder = new StringBuilder();
        for (String fileName : storageFileNames) {
            try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.replaceAll("[^a-zA-Z0-9\\s]", "");
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    contentBuilder.append(line).append("\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        fileContent = contentBuilder.toString();
    }

    private static List<String> distributeContent(int totalNodes) {
        List<StringBuilder> contentBuilders = new ArrayList<>(totalNodes);

        for (int i = 0; i < totalNodes; i++) {
            contentBuilders.add(new StringBuilder());
        }

        String[] lines = fileContent.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            contentBuilders.get(i % totalNodes).append(line).append("\n");
        }

        List<String> contentList = new ArrayList<>(totalNodes);
        for (StringBuilder builder : contentBuilders) {
            contentList.add(builder.toString());
        }
        
        return contentList;
    }
}
