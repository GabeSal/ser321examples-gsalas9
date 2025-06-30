# Protocol Specification – Distributed Sum System

This document defines the communication protocol used in the distributed summation system.

All message serialization uses **Protocol Buffers** (`.proto` definitions compiled via Gradle). See `src/main/proto/messages.proto`.

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

### `ErrorResponse` – Leader → Client
``` proto
message ErrorResponse {
  string message = 1;
  int32 errorCode = 2;
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
}
```

## Communication Details
- All sockets use Protobuf's writeDelimitedTo() / parseDelimitedFrom() methods for safe streaming and framing.
- Nodes and the leader use persistent TCP socket connections (one request per connection).
- Fault tolerance can be tested via the -Pwrong=1 argument, simulating incorrect computation (e.g., product instead of sum).

## Error Codes – Leader to Client
| Code | Meaning                        |
|------|--------------------------------|
| 1    | Not enough nodes (must be ≥ 3) |
| 2    | Task computation failed        |
| 3    | Consensus failure              |
| 4    | Node communication failure     |
| 0    | Internal or unknown error      |

### *Notes*
- JSON-based examples (previously used) have been replaced with Protobuf for compact, efficient messaging.
- JSON may still be used for debugging if needed, but the system expects Protobuf-encoded streams.