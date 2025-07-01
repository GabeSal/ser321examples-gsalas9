package distributed;

import distributed.protocol.NodeHello;
import distributed.protocol.SubtaskRequest;
import distributed.protocol.SubtaskResult;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import java.util.UUID;

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
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // Send handshake
            NodeHello hello = NodeHello.newBuilder()
                    .setNodeId("Node-" + UUID.randomUUID())
                    .build();
            hello.writeDelimitedTo(out);
            System.out.println("[NODE] Sent handshake to Leader.");

            while (true) {
                SubtaskRequest task = SubtaskRequest.parseDelimitedFrom(in);
                if (task == null) {
                    System.out.println("[NODE] No more tasks. Closing connection.");
                    break;
                }

                List<Integer> nums = task.getListList();
                int delay = task.getDelayMs();

                System.out.printf("[NODE] Received task: %s | Delay: %dms%n", nums, delay);

                int result = simulateFault
                        ? computeFaulty(nums, delay)
                        : computeWithDelay(nums, delay);

                SubtaskResult response = SubtaskResult.newBuilder()
                        .setSum(result)
                        .build();
                response.writeDelimitedTo(out);
            }
        } catch (IOException e) {
            System.err.println("[NODE] Error occurred during handshake: " + e.getMessage());
        }
    }

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

    public static int computeFaulty(List<Integer> list, int delayMs) {
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