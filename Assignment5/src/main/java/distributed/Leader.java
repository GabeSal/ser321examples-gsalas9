package distributed;

import distributed.protocol.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Leader {

    private static final List<NodeConnection> connectedNodes = Collections.synchronizedList(new ArrayList<>());

    /**
     * Entry point of the Leader process.
     * Opens a server socket to accept incoming Client task requests.
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("[LEADER] Usage: java Leader <clientPort> <nodePort>");
            return;
        }

        int clientPort = Integer.parseInt(args[0]);
        int nodePort = Integer.parseInt(args[1]);

        try (
            ServerSocket clientSocket = new ServerSocket(clientPort);
            ServerSocket nodeSocket = new ServerSocket(nodePort)
        ) {
            new Thread(() -> {
                while (true) {
                    Socket ns = null;
                    try {
                        ns = nodeSocket.accept();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    handleNodeConnection(ns);
                }
            }).start();

            while (true) {
                Socket cs = clientSocket.accept();
                new Thread(() -> {
                    try {
                        handleClient(cs);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
            }
        } catch (IOException e) {
            System.err.println("[LEADER] Error encountered while connecting to client/node: " + e.getMessage());
        }
    }

    private static void handleNodeConnection(Socket socket) {
        try {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            NodeHello hello = NodeHello.parseDelimitedFrom(in);
            if (hello == null) {
                System.err.println("[LEADER] Failed to receive handshake from node.");
                return;
            }

            NodeConnection node = new NodeConnection(socket, in, out, hello.getNodeId());
            connectedNodes.add(node);

            System.out.printf("[LEADER] Registered Node %s (%d total)%n",
                hello.getNodeId(), connectedNodes.size());

        } catch (IOException e) {
            System.err.println("[LEADER] Node connection failed: " + e.getMessage());
        }
    }

    /**
     * Handles a single client's task request.
     * Executes both local and distributed versions of the sum and sends back a comparison.
     */
    private static void handleClient(Socket socket) throws InterruptedException {
        try (InputStream in = socket.getInputStream();
             OutputStream out = socket.getOutputStream()) {

            System.out.println("[LEADER] Waiting for TaskRequest...");
            TaskRequest request = TaskRequest.parseDelimitedFrom(in);
            if (request == null) {
                System.err.println("[LEADER] No TaskRequest received. Exiting handler.");
                return;
            }

            System.out.printf("[LEADER] Received TaskRequest: list=%s | delayMs=%d%n",
                request.getListList(), request.getDelayMs());

            // --- Local computation (non-distributed) ---
            long localStart = System.currentTimeMillis();
            int localSum = computeWithDelay(request.getListList(), request.getDelayMs());
            long localEnd = System.currentTimeMillis();
            int localDuration = (int) (localEnd - localStart);
            System.out.printf("[LEADER] Local sum: %d | Time: %dms%n", localSum, localDuration);

            int requiredNodes = Math.min(connectedNodes.size(), 3);
            if (connectedNodes.size() < 3) {
                ErrorResponse error = ErrorResponse.newBuilder()
                        .setMessage("Not enough nodes (min: 3)")
                        .setErrorCode(1)
                        .build();
                error.writeDelimitedTo(out);
                out.flush();
                return;
            }

            List<NodeConnection> activeNodes = connectedNodes.subList(0, requiredNodes);
            List<List<Integer>> partitions = partitionList(request.getListList(), connectedNodes.size());
            System.out.println("[LEADER] Dispatching subtasks to registered nodes...");

            Map<String, Integer> partialResults = new ConcurrentHashMap<>();
            List<Thread> threads = new ArrayList<>();

            long distributedStart = System.currentTimeMillis();

            for (int i = 0; i < activeNodes.size(); i++) {
                NodeConnection node = activeNodes.get(i);
                List<Integer> part = partitions.get(i);
                Thread t = new Thread(() -> {
                    try {
                        SubtaskRequest subtask = SubtaskRequest.newBuilder()
                            .addAllList(part)
                            .setDelayMs(request.getDelayMs())
                            .build();
                        subtask.writeDelimitedTo(node.out);
                        node.out.flush();

                        SubtaskResult result = SubtaskResult.parseDelimitedFrom(node.in);
                        partialResults.put(node.nodeId, result.getSum());
                        System.out.printf("[LEADER] Node %s responded: %d%n", node.nodeId, result.getSum());
                    } catch (IOException e) {
                        System.err.printf("[LEADER] Failed to dispatch to Node %s: %s%n", node.nodeId, e.getMessage());
                        connectedNodes.remove(node);
                    }
                });
                threads.add(t);
                t.start();
            }

            for (Thread t : threads) t.join();

            long distributedEnd = System.currentTimeMillis();
            int distributedTime = (int) (distributedEnd - distributedStart);

            // --- Consensus Check ---
            Set<Integer> uniqueResults = new HashSet<>(partialResults.values());
            if (uniqueResults.size() != partialResults.size()) {
                System.err.println("[LEADER] Consensus failure detected: duplicate or inconsistent partial results.");
                ErrorResponse error = ErrorResponse.newBuilder()
                        .setMessage("Consensus check failed: inconsistent node responses.")
                        .setErrorCode(3)
                        .build();
                error.writeDelimitedTo(out);
                return;
            }

            int totalDistributedSum = partialResults.values().stream().mapToInt(Integer::intValue).sum();
            System.out.printf("[LEADER] Distributed result: %d | Local result: %d%n", totalDistributedSum, localSum);

            // --- Respond to Client with results ---
            ResultResponse result = ResultResponse.newBuilder()
                .setSum(totalDistributedSum)
                .setSingleThreadTimeMs(localDuration)
                .setDistributedTimeMs(distributedTime)
                .build();

            result.writeDelimitedTo(out);
            out.flush();
            System.out.println("[LEADER] Sent result to client.");

        } catch (IOException e) {
            System.err.println("[LEADER] Client handling error: " + e.getMessage());
        }
    }

    /**
     * Performs a simple sum of a list of integers, with an artificial delay between additions.
     */
    public static int computeWithDelay(List<Integer> list, int delayMs) {
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
     * Evenly and smartly partitions a list into up to N parts.
     * Nodes may receive uneven sizes, but all input data is used.
     */
    public static List<List<Integer>> partitionList(List<Integer> input, int nodeCount) {
        int totalSize = input.size();
        int actualPartitions = Math.min(nodeCount, totalSize); // Don't create empty partitions

        List<List<Integer>> result = new ArrayList<>(actualPartitions);
        int baseSize = totalSize / actualPartitions;
        int remainder = totalSize % actualPartitions;

        int index = 0;
        for (int i = 0; i < actualPartitions; i++) {
            int partSize = baseSize + (i < remainder ? 1 : 0); // Spread remainder fairly
            List<Integer> part = input.subList(index, index + partSize);
            result.add(new ArrayList<>(part));
            index += partSize;
        }

        return result;
    }

    static class NodeConnection {
        final Socket socket;
        final OutputStream out;
        final InputStream in;
        final String nodeId;

        NodeConnection(Socket socket, InputStream in, OutputStream out, String nodeId) {
            this.socket = socket;
            this.in = in;
            this.out = out;
            this.nodeId = nodeId;
        }
    }
}