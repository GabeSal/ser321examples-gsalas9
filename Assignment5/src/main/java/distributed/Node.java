package distributed;

import distributed.protocol.SubtaskRequest;
import distributed.protocol.SubtaskResult;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

public class Node {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("[NODE] Usage: java Node <host> <port> [-Pwrong=1]");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        boolean simulateFault = args.length >= 3 && args[2].equals("1");

        try (Socket socket = new Socket(host, port)) {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            System.out.printf("[NODE] Connected to Leader at %s:%d%n", host, port);

            // Receive subtask via Protobuf
            SubtaskRequest request = SubtaskRequest.parseDelimitedFrom(in);

            if (request == null) {
                System.err.println("[NODE] Received empty task.");
                return;
            }

            List<Integer> nums = request.getListList();
            int delay = request.getDelayMs();

            System.out.printf("[NODE] Received task: %s | Delay: %dms | Faulty: %s%n",
                    nums, delay, simulateFault);

            int result = simulateFault
                    ? computeFaulty(nums, delay)
                    : computeWithDelay(nums, delay);

            // Build and send response
            SubtaskResult response = SubtaskResult.newBuilder()
                    .setSum(result)
                    .build();
            response.writeDelimitedTo(out);

            System.out.printf("[NODE] Computation done. Sent result: %d%n", result);

        } catch (Exception e) {
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
}