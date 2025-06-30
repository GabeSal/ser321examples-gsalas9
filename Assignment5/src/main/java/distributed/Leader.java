package distributed;

import distributed.protocol.TaskRequest;
import distributed.protocol.ResultResponse;
import distributed.protocol.SubtaskRequest;
import distributed.protocol.SubtaskResult;
import distributed.protocol.ErrorResponse;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Leader {

    /**
     * Entry point of the Leader process.
     * Opens a server socket to accept incoming Client task requests.
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("[LEADER] Usage: java Leader <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.printf("[LEADER] Listening on port %d...%n", port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[LEADER] Client connected.");

                new Thread(() -> {
                    try {
                        handleClient(clientSocket);
                    } catch (InterruptedException e) {
                        System.err.println("[LEADER] Interrupted while handling client: " + e.getMessage());
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
        } catch (IOException e) {
            System.err.println("[LEADER] Server error: " + e.getMessage());
        }
    }

    /**
     * Handles a single client's task request.
     * Executes both local and distributed versions of the sum and sends back a comparison.
     */
    private static void handleClient(Socket socket) throws InterruptedException {
        try (InputStream in = socket.getInputStream();
             OutputStream out = socket.getOutputStream()) {

            TaskRequest request = TaskRequest.parseFrom(in);
            System.out.printf("[LEADER] Received task: %s | Delay: %dms%n", request.getListList(), request.getDelayMs());

            // --- Local computation (non-distributed) ---
            long localStart = System.currentTimeMillis();
            int localSum = computeWithDelay(request.getListList(), request.getDelayMs());
            long localEnd = System.currentTimeMillis();
            int localDuration = (int) (localEnd - localStart);
            System.out.printf("[LEADER] Local sum: %d | Time: %dms%n", localSum, localDuration);

            // --- Distributed computation via nodes ---
            int numNodes = 3;
            if (numNodes < 3) {
                ErrorResponse error = ErrorResponse.newBuilder()
                        .setMessage("Not enough nodes (min: 3)")
                        .setErrorCode(1)
                        .build();
                error.writeTo(out);
                return;
            }

            List<List<Integer>> partitions = partitionList(request.getListList(), numNodes);
            System.out.println("[LEADER] Dispatching subtasks to nodes...");

            Map<Integer, Integer> partialResults = Collections.synchronizedMap(new HashMap<>());
            List<Thread> threads = new ArrayList<>();

            int baseNodePort = 8601;
            long distStart = System.currentTimeMillis();
            for (int i = 0; i < numNodes; i++) {
                List<Integer> subList = partitions.get(i);
                int nodePort = baseNodePort + i;
                Thread t = new Thread(new NodeWorker("localhost", nodePort, subList, request.getDelayMs(), i + 1, partialResults));
                threads.add(t);
                t.start();
            }

            for (Thread t : threads) {
                t.join();
            }
            long distEnd = System.currentTimeMillis();
            int distDuration = (int) (distEnd - distStart);

            System.out.println("[LEADER] All node responses received.");
            int totalDistributedSum = partialResults.values().stream().mapToInt(Integer::intValue).sum();
            System.out.println("[LEADER] Total distributed sum: " + totalDistributedSum);

            // --- Respond to Client with results ---
            ResultResponse result = ResultResponse.newBuilder()
                    .setSum(totalDistributedSum)
                    .setSingleThreadTimeMs(localDuration)
                    .setDistributedTimeMs(distDuration)
                    .build();

            result.writeTo(out);
            System.out.println("[LEADER] Sent result to client.");

        } catch (IOException e) {
            System.err.println("[LEADER] Client handling error: " + e.getMessage());
        }
    }

    /**
     * Performs a simple sum of a list of integers, with an artificial delay between additions.
     */
    private static int computeWithDelay(List<Integer> list, int delayMs) {
        int sum = 0;
        for (int num : list) {
            sum += num;
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ignored) {}
        }
        return sum;
    }

    /**
     * Evenly partitions a list into N parts for distribution to nodes.
     */
    private static List<List<Integer>> partitionList(List<Integer> list, int parts) {
        int size = list.size();
        int chunkSize = size / parts;
        int remainder = size % parts;

        List<List<Integer>> result = new ArrayList<>();
        int index = 0;
        for (int i = 0; i < parts; i++) {
            int currentChunkSize = chunkSize + (i < remainder ? 1 : 0);
            result.add(list.subList(index, index + currentChunkSize));
            index += currentChunkSize;
        }
        return result;
    }

    /**
     * Threaded task sender that communicates with a specific Node.
     * Sends a subtask and waits for the partial sum response.
     */
    static class NodeWorker implements Runnable {
        private final String host;
        private final int port;
        private final List<Integer> list;
        private final int delayMs;
        private final int nodeId;
        private final Map<Integer, Integer> results;

        public NodeWorker(String host, int port, List<Integer> list, int delayMs,
                          int nodeId, Map<Integer, Integer> results) {
            this.host = host;
            this.port = port;
            this.list = list;
            this.delayMs = delayMs;
            this.nodeId = nodeId;
            this.results = results;
        }

        @Override
        public void run() {
            try (Socket socket = new Socket(host, port);
                 OutputStream out = socket.getOutputStream();
                 InputStream in = socket.getInputStream()) {

                SubtaskRequest.Builder request = SubtaskRequest.newBuilder()
                        .addAllList(list)
                        .setDelayMs(delayMs);
                request.build().writeTo(out);

                SubtaskResult result = SubtaskResult.parseFrom(in);

                synchronized (results) {
                    results.put(nodeId, result.getSum());
                }

                System.out.printf("[LEADER] Node %d responded: %d%n", nodeId, result.getSum());

            } catch (IOException e) {
                System.err.printf("[LEADER] Failed to reach Node %d on port %d: %s%n", nodeId, port, e.getMessage());
            }
        }
    }
}