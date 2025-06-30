package distributed;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Leader {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("[LEADER] Usage: java Leader <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.printf("[LEADER] Listening on port %d...%n", port);

            //noinspection InfiniteLoopStatement
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[LEADER] Client connected.");

                new Thread(() -> {
                    handleClient(clientSocket);
                }).start();
            }
        } catch (IOException e) {
            System.err.println("[LEADER] Error: " + e.getMessage());
        }
    }

    private static void handleClient(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String json = in.readLine();
            TaskRequest request = mapper.readValue(json, TaskRequest.class);
            System.out.printf("[LEADER] Received task: %s | Delay: %dms%n", request.list, request.delayMs);

            // Local computation
            long localStart = System.currentTimeMillis();
            int localSum = computeWithDelay(request.list, request.delayMs);
            long localEnd = System.currentTimeMillis();
            int localDuration = (int) (localEnd - localStart);
            System.out.printf("[LEADER] Local sum: %d | Time: %dms%n", localSum, localDuration);

            // Distribute to Nodes
            int numNodes = 3; // We'll start with 3; support 3-5 later
            if (numNodes < 3) {
                sendError(out, "Not enough nodes (min: 3)", 1);
                return;
            }

            List<List<Integer>> partitions = partitionList(request.list, numNodes);
            System.out.println("[LEADER] Dispatching subtasks to nodes...");

            Map<Integer, Integer> partialResults = Collections.synchronizedMap(new HashMap<>());
            List<Thread> threads = new ArrayList<>();

            int baseNodePort = 8601;
            long distStart = System.currentTimeMillis();
            for (int i = 0; i < numNodes; i++) {
                List<Integer> subList = partitions.get(i);
                int nodePort = baseNodePort + i;
                Thread t = new Thread(new NodeWorker("localhost", nodePort, subList, request.delayMs, i + 1, partialResults));
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

            // Send result to client
            ResultResponse result = new ResultResponse(totalDistributedSum, localDuration, distDuration);
            String response = mapper.writeValueAsString(result);
            out.println(response);
            System.out.println("[LEADER] Sent result to client.");

        } catch (IOException | InterruptedException e) {
            System.err.println("[LEADER] Client handling error: " + e.getMessage());
        }
    }

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

    private static void sendError(PrintWriter out, String message, int code) throws IOException {
        ErrorResponse error = new ErrorResponse(message, code);
        String json = mapper.writeValueAsString(error);
        out.println(json);
    }

    // --- Data Classes (to be moved to protocol package later) ---
    static class TaskRequest {
        public List<Integer> list;
        public int delayMs;
    }

    static class ErrorResponse {
        public String type = "ERROR";
        public String message;
        public int errorCode;

        public ErrorResponse() {}
        public ErrorResponse(String message, int code) {
            this.message = message;
            this.errorCode = code;
        }
    }

    static class ResultResponse {
        public String type = "RESULT";
        public int sum;
        public int singleThreadTimeMs;
        public int distributedTimeMs;

        public ResultResponse() {}
        public ResultResponse(int sum, int singleThreadTimeMs, int distributedTimeMs) {
            this.sum = sum;
            this.singleThreadTimeMs = singleThreadTimeMs;
            this.distributedTimeMs = distributedTimeMs;
        }
    }

    static class SubtaskRequest {
        public String type = "SUBTASK";
        public List<Integer> list;
        public int delayMs;

        public SubtaskRequest() {}
        public SubtaskRequest(List<Integer> list, int delayMs) {
            this.list = list;
            this.delayMs = delayMs;
        }
    }

    static class SubtaskResult {
        public String type;
        public int sum;
    }

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
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                SubtaskRequest request = new SubtaskRequest(list, delayMs);
                String json = mapper.writeValueAsString(request);
                out.println(json);

                String response = in.readLine();
                SubtaskResult result = mapper.readValue(response, SubtaskResult.class);
                synchronized (results) {
                    results.put(nodeId, result.sum);
                }

                System.out.printf("[LEADER] Node %d responded: %d%n", nodeId, result.sum);

            } catch (IOException e) {
                System.err.printf("[LEADER] Failed to reach Node %d on port %d: %s%n", nodeId, port, e.getMessage());
            }
        }
    }
}