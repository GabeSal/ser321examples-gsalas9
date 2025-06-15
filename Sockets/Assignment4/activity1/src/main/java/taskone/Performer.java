/**
  File: Performer.java
  Author: Student in Fall 2020B
  Description: Performer class in package taskone.
*/

package taskone;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import static java.lang.Thread.sleep;

/**
 * Class: Performer 
 * Description: Threaded Performer for server tasks. Refactored to support 'display',
 * 'search', 'reverse', 'quit' operations in addition to 'add'
 */
class Performer {

    private StringList state;

    public Performer(StringList strings) {
        this.state = strings;
    }

    /**
     * Handles structured requests and generates a valid protocol-compliant JSON response.
     *
     * @param request a Map parsed from incoming JSON
     * @return JSON response as string
     */
    public String handleRequest(Map<String, Object> request) {
        try {
            String operation = (String) request.get("operation");
            Object data = request.get("data");

            if (operation == null) {
                return error("missing operation");
            }

            switch (operation.toLowerCase()) {
                case "add":
                    return handleAdd(data);

                case "display":
                    return handleDisplay();

                case "search":
                    return handleSearch(data);

                case "reverse":
                    return handleReverse(data);

                case "quit":
                    return success("quit", "Goodbye!");

                default:
                    return error("unknown operation: " + operation);
            }
        } catch (Exception e) {
            return error("internal server error: " + e.getMessage());
        }
    }

    private String handleAdd(Object data) {
        if (!(data instanceof String)) {
            return error("wrong data");
        }
        String value = (String) data;
        state.add(value);
        return success("add", state.getAll().toString());
    }

    private String handleDisplay() {
        List<String> all = state.getAll();
        if (all.isEmpty()) {
            return error("list empty");
        }
        return success("display", all.toString());
    }

    private String handleSearch(Object data) {
        if (!(data instanceof String)) {
            return error("wrong data");
        }
        int index = state.indexOf((String) data);
        return success("search", index);
    }

    private String handleReverse(Object data) {
        if (!(data instanceof Number)) {
            return error("wrong data");
        }
        int index = ((Number) data).intValue();
        String reversed = state.reverseAt(index);
        if (reversed == null) {
            return error("index not found");
        }
        return success("reverse", reversed);
    }

    private String success(String operation, Object result) {
        return String.format("{\"operation\": \"%s\", \"data\": %s}", operation, toJson(result));
    }

    private String error(String message) {
        return String.format("{\"type\": \"error\", \"message\": \"%s\"}", message);
    }

    private String toJson(Object obj) {
        if (obj instanceof String) {
            return "\"" + obj + "\"";
        } else if (obj instanceof List) {
            return obj.toString().replaceAll("^\\[|\\]$", "").replace(", ", "\", \"").replaceAll("^", "[\"").replaceAll("$", "\"]");
        }
        return obj.toString();
    }
}
