package distributed;

import distributed.protocol.TaskRequest;
import distributed.protocol.ResultResponse;
import distributed.protocol.ErrorResponse;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Client {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("[CLIENT] Usage: java Client <host> <port> <delayMs>");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        int delayMs = Integer.parseInt(args[2]);

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("[CLIENT] Welcome to the Distributed Sum System.");
            System.out.println("[CLIENT] Type 'q', 'quit', or 'exit' to stop.");

            while (true) {
                System.out.print("[CLIENT] Enter comma-separated list of integers: \n(ex: 1,2,3)");
                if (!scanner.hasNextLine()) {
                    System.out.println("\n[CLIENT] Input stream closed. Exiting.");
                    break;
                }

                String inputList = scanner.nextLine().trim();
                if (inputList.equalsIgnoreCase("q") ||
                    inputList.equalsIgnoreCase("quit") ||
                    inputList.equalsIgnoreCase("exit")) {
                    System.out.println("[CLIENT] Exiting.");
                    break;
                }

                List<Integer> numbers;
                try {
                    numbers = parseList(inputList);
                } catch (IllegalArgumentException e) {
                    System.err.println("[CLIENT] Invalid input: " + e.getMessage());
                    continue;
                }

                try (Socket socket = new Socket(host, port)) {
                    OutputStream out = socket.getOutputStream();
                    InputStream in = socket.getInputStream();

                    TaskRequest request = TaskRequest.newBuilder()
                        .addAllList(numbers)
                        .setDelayMs(delayMs)
                        .build();
                    request.writeDelimitedTo(out);
                    System.out.printf("[CLIENT] Sent task: list=%s, delay=%dms%n", numbers, delayMs);

                    System.out.println("[CLIENT] Awaiting response from Leader...");
                    ResultResponse result = ResultResponse.parseDelimitedFrom(in);

                    if (result != null) {
                        System.out.println("[CLIENT] ====== Result ======");
                        System.out.printf("Sum: %d%n", result.getSum());
                        System.out.printf("Single-threaded time: %d ms%n", result.getSingleThreadTimeMs());
                        System.out.printf("Distributed time: %d ms%n", result.getDistributedTimeMs());
                    } else {
                        ErrorResponse error = ErrorResponse.parseDelimitedFrom(in);
                        if (error != null) {
                            System.out.printf("[CLIENT] ERROR (%d): %s%n", error.getErrorCode(), error.getMessage());
                        } else {
                            System.err.println("[CLIENT] Received unrecognized or empty response.");
                        }
                    }

                } catch (Exception e) {
                    System.err.println("[CLIENT] Communication error: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("[CLIENT] Critical failure: " + e.getMessage());
        }
    }

    public static List<Integer> parseList(String input) {
        if (input.isEmpty()) {
            throw new IllegalArgumentException("List cannot be empty.");
        }

        try {
            return Arrays.stream(input.split(","))
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Input must be comma-separated integers.");
        }
    }
}