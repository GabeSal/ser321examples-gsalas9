package distributed;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class Node {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("[NODE] Usage: java Node <host> <port> [-Pwrong=1]");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        boolean simulateFault = args.length >= 3 && args[2].equals("1");

        try (Socket socket = new Socket(host, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println("[NODE] Connected to Leader at " + host + ":" + port);

            String json = in.readLine(); // blocking wait for task
            SubtaskRequest task = mapper.readValue(json, SubtaskRequest.class);

            System.out.printf("[NODE] Received task: %s | Delay: %dms | Faulty: %s%n",
                    task.list, task.delayMs, simulateFault);

            int result = simulateFault
                    ? computeFaulty(task.list, task.delayMs)
                    : computeWithDelay(task.list, task.delayMs);

            SubtaskResult response = new SubtaskResult(result);
            out.println(mapper.writeValueAsString(response));

            System.out.printf("[NODE] Computation done. Sent result: %d%n", result);

        } catch (IOException e) {
            System.err.println("[NODE] Connection error: " + e.getMessage());
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

    private static int computeFaulty(List<Integer> list, int delayMs) {
        int product = 1;
        for (int num : list) {
            product *= num;
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ignored) {}
        }
        return product;
    }

    // Temporary inner models
    static class SubtaskRequest {
        public List<Integer> list;
        public int delayMs;
    }

    static class SubtaskResult {
        public String type = "PARTIAL_RESULT";
        public int sum;

        public SubtaskResult() {}
        public SubtaskResult(int sum) {
            this.sum = sum;
        }
    }
}