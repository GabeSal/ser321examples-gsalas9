# Protocol Specification – Distributed Sum System

The system uses **Protocol Buffers (proto3)** for structured message exchange over TCP.
All messages are framed using Protobuf’s `writeDelimitedTo()` and `parseDelimitedFrom()` to ensure message boundaries over streams.

---

## System Roles

| Component  | Responsibilities                                                           |
|------------|----------------------------------------------------------------------------|
| **Client** | Sends task request to the Leader                                           |
| **Leader** | Accepts tasks, computes locally, distributes subtasks, and returns results |
| **Node**   | Computes subtasks, may simulate faults                                     |

---

## Protobuf Message Types

### `TaskRequest` – Client → Leader
``` proto
message TaskRequest {
  repeated int32 list = 1;
  int32 delayMs = 2;
}
```

### `ResultResponse` – Leader → Client
``` proto
message ResultResponse {
  int32 sum = 1;
  int32 singleThreadTimeMs = 2;
  int32 distributedTimeMs = 3;
}
```

### `SubtaskRequest` – Leader → Node
``` proto
message SubtaskRequest {
  repeated int32 list = 1;
  int32 delayMs = 2;
}
```

### `SubtaskResult` – Node → Leader
``` proto
message SubtaskResult {
  int32 sum = 1;
  string node_id = 2;
}
```

### `NodeHello` – Node → Leader (handshake)
``` proto
message NodeHello {
  string nodeId = 1;
}
```

### `ErrorResponse` – Leader → Client
``` proto
message ErrorResponse {
  string message = 1;
  int32 errorCode = 2;
}
```

## Communication Details
- All sockets use Protobuf's writeDelimitedTo() / parseDelimitedFrom() methods for safe streaming and framing.
- Nodes and the leader use persistent TCP socket connections (one request per connection).
- Fault tolerance can be tested via the -Pwrong=1 argument, simulating incorrect computation (e.g., product instead of sum).

## Error Codes – Leader to Client
| Code | Meaning                         |
|------|---------------------------------|
| 1    | Not enough nodes (minimum is 3) |
| 2    | Task computation failed         |
| 3    | Consensus failure               |
| 4    | Node communication failure      |
| 0    | Internal or unknown error       |

### *Additional Notes*
- Nodes can simulate failure or incorrect output using the `-Pwrong=1` flag.
- Message exchange assumes blocking socket communication and one message per connection phase.