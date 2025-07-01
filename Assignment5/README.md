# Distributed Sum System
*by Gabriel Salas*

## Description
This project implements a distributed summation system using **Java**, **Gradle**, and **Protocol Buffers** over **TCP sockets**.

It simulates a distributed computing model where:
- A **Client** submitting integer lists and delay time
- A **Leader** distributing sub-tasks and computing results both locally and across Nodes
- Several **Worker Nodes** processing subtasks with optional fault simulation
- A basic **consensus mechanism** to detect inconsistent Node results

Features:
- Dynamic node registration via `NodeHello` messages.
- Fault injection support via simulated incorrect node behavior (`-Pwrong=1`).
- Graceful input loop with continuous client interaction.
- Protobuf-based structured messaging and streaming.

---

## Prerequisites

- Java 21
- Gradle 8.4+ (tested with 8.14.1)
- No manual installation of `protoc` required — handled by the Gradle Protobuf plugin

---

## Build Instructions

To clean, build, and generate protocol classes:

``` bash
./gradlew clean build
```

To regenerate Protobuf classes only:
``` bash
./gradlew generateProto --rerun-tasks
```

Generated classes are located in:
```
build/generated/source/proto/main/java/distributed/protocol/
```

## Running the System
| Property     | Description                                    | Default     |
|--------------|------------------------------------------------|-------------|
| `host`       | Host address (for Client/Node to connect)      | `localhost` |
| `clientPort` | Port Leader listens for Client connections     | `8500`      |
| `nodePort`   | Port Leader listens for Node connections       | `8600`      |
| `delayMs`    | Delay per item in milliseconds for computation | `50`        |
| `wrong`      | Set to `1` to simulate faulty node logic       | `0`         |

## Example Execution (5 terminals)

1. **Start Leader**
``` bash
./gradlew runLeader -PclientPort=8500 -PnodePort=8600
```
2. **Start Nodes**
``` bash
./gradlew runNode -Phost=localhost -Pport=8600
./gradlew runNode -Phost=localhost -Pport=8600
./gradlew runNode -Phost=localhost -Pport=8600
```
To simulate a faulty node:
``` bash
./gradlew runNode -Phost=localhost -Pport=8600 -Pwrong=1
```

3. **Start Client**
``` bash
./gradlew runClient -Phost=localhost -PclientPort=8500 -PdelayMs=100
```

Client will be prompted:
`[CLIENT] Enter comma-separated list of integers (ex: 1,2,3): `

## Communication Summary
The system uses Protocol Buffers for efficient, schema-driven communication over TCP sockets. All streams use:
``` java
writeDelimitedTo(outputStream);
parseDelimitedFrom(inputStream);
```
Refer to `PROTOCOL.md` for all protobuf message definitions.

## Fault Tolerance and Consensus
- Nodes may simulate faulty computation (product instead of sum) using -Pwrong=1.
- Leader performs a consensus check by verifying all partial sums are unique.
- On mismatch, the Leader returns:
``` protobuf
ErrorResponse {
  message: "Consensus check failed"
  errorCode: 3
}
```

## Running Tests
Unit tests cover:

| Class        | Tests                                             |
|--------------|---------------------------------------------------|
| `LeaderTest` | Partitioning logic, local computation, stream I/O |
| `NodeTest`   | Faulty vs correct logic, protobuf serialization   |
| `ClientTest` | Input parsing and input validation                |

Run tests using:
``` bash
./gradlew test
```

Expected output:
```bash
BUILD SUCCESSFUL
```

## Authors and Acknowledgements
- Created for SER321 - Distributed Systems Assignment
- Developed in Java using Gradle & Protobuf