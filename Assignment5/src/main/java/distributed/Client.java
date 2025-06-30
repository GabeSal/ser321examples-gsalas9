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
        if (args.length < 2) {
            System.out.println("[CLIENT] Usage: java Client <host> <port>");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);

        Scanner scanner = new Scanner(System.in);

        System.out.println("[CLIENT] Welcome to the Distributed Sum System.");
        System.out.print("[CLIENT] Enter comma-separated list of integers: ");
        String inputList = scanner.nextLine();
        List<Integer> numbers = parseList(inputList);

        System.out.print("[CLIENT] Enter delay in milliseconds: ");
        int delayMs = Integer.parseInt(scanner.nextLine());

        try (Socket socket = new Socket(host, port)) {
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // Build and send TaskRequest
            TaskRequest request = TaskRequest.newBuilder()
                    .addAllList(numbers)
                    .setDelayMs(delayMs)
                    .build();
            request.writeDelimitedTo(out);
            System.out.println("[CLIENT] Sent task to Leader.");

            // Read delimited Protobuf response
            ResultResponse result = ResultResponse.parseDelimitedFrom(in);

            if (result != null) {
                System.out.println("[CLIENT] ====== Result ======");
                System.out.printf("Sum: %d%n", result.getSum());
                System.out.printf("Single-threaded time: %d ms%n", result.getSingleThreadTimeMs());
                System.out.printf("Distributed time: %d ms%n", result.getDistributedTimeMs());
            } else {
                // Try to parse as ErrorResponse (fallback)
                ErrorResponse error = ErrorResponse.parseDelimitedFrom(in);
                if (error != null) {
                    System.out.printf("[CLIENT] ERROR (%d): %s%n", error.getErrorCode(), error.getMessage());
                } else {
                    System.err.println("[CLIENT] Received unrecognized or empty response.");
                }
            }

        } catch (Exception e) {
            System.err.println("[CLIENT] Error connecting to Leader: " + e.getMessage());
        }
    }

    private static List<Integer> parseList(String input) {
        return Arrays.stream(input.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }
}