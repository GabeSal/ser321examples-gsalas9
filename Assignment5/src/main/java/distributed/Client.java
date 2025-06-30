package distributed;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Client {
    private static final ObjectMapper mapper = new ObjectMapper();

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

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Create request
            TaskRequest request = new TaskRequest(numbers, delayMs);
            String json = mapper.writeValueAsString(request);
            out.println(json);

            // Read response
            String response = in.readLine();
            if (response != null) {
                handleResponse(response);
            }

        } catch (IOException e) {
            System.err.println("[CLIENT] Error connecting to Leader: " + e.getMessage());
        }
    }

    private static List<Integer> parseList(String input) {
        return Arrays.stream(input.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    private static void handleResponse(String json) throws IOException {
        if (json.contains("\"errorCode\"")) {
            ErrorResponse error = mapper.readValue(json, ErrorResponse.class);
            System.out.printf("[ERROR] (%d): %s%n", error.errorCode, error.message);
        } else {
            ResultResponse result = mapper.readValue(json, ResultResponse.class);
            System.out.println("[CLIENT] ====== Result ======");
            System.out.printf("Sum: %d%n", result.sum);
            System.out.printf("Single-threaded time: %d ms%n", result.singleThreadTimeMs);
            System.out.printf("Distributed time: %d ms%n", result.distributedTimeMs);
        }
    }

    // Internal static classes for serialization
    static class TaskRequest {
        public final String type = "TASK_REQUEST";
        public List<Integer> list;
        public int delayMs;

        public TaskRequest() {} // Needed for Jackson
        public TaskRequest(List<Integer> list, int delayMs) {
            this.list = list;
            this.delayMs = delayMs;
        }
    }

    static class ResultResponse {
        public String type;
        public int sum;
        public int singleThreadTimeMs;
        public int distributedTimeMs;
    }

    static class ErrorResponse {
        public String type;
        public String message;
        public int errorCode;
    }
}
