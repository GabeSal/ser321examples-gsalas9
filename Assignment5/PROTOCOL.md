# Protocol Specification – Distributed Sum System

This file defines the communication protocol between the `Client`, `Leader`, and `Nodes` for the distributed summation task. Messages can be serialized using **Protocol Buffers** (default) or **JSON** (for debugging or human readability).

---

## General Roles

- **Client**: Sends a list of integers and a delay value.
- **Leader**: Accepts tasks, performs local computation, delegates parts to nodes, and validates results via consensus.
- **Node**: Computes partial sums, may simulate faults.

---

## Client → Leader

### TASK_REQUEST
```
{
  "type": "TASK_REQUEST",
  "list": [1, 2, 3, ..., 15],
  "delayMs": 50
}
```

## Client → Leader

### RESULT
```
{
  "type": "RESULT",
  "sum": 120,
  "singleThreadTimeMs": 850,
  "distributedTimeMs": 350
}
```

### ERROR
```
{
  "type": "ERROR",
  "message": "Consensus failed. Node disagreement detected.",
  "errorCode": 1
}
```

## Leader → Node

### SUBTASK
```
{
  "type": "SUBTASK",
  "list": [1, 2, 3, 4, 5],
  "delayMs": 50
}
```

### CONSENSUS_VALIDATE
```
{
  "type": "CONSENSUS_VALIDATE",
  "list": [6, 7, 8, 9, 10],
  "expectedSum": 40
}
```

## Node → Leader

### PARTIAL_RESULT
```
{
  "type": "PARTIAL_RESULT",
  "sum": 65,
  "nodeId": "Node-3"
}
```

### CONSENSUS_RESPONSE
```
{
  "type": "CONSENSUS_RESPONSE",
  "nodeId": "Node-3",
  "agrees": true
}
```

## Error Codes (Leader → Client)
| Code | Meaning                        |
|------|--------------------------------|
| 1    | Not enough nodes (must be ≥ 3) |
| 2    | Task computation failed        |
| 3    | Consensus failure              |
| 4    | Node communication failed      |
| 0    | Other/internal error           |