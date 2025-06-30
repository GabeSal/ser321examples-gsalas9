# Distributed Sum System

## Description
This project implements a distributed summation system using **Java**, **Gradle**, and **Protocol Buffers** over **TCP sockets**. It simulates a distributed computing scenario where:
- A **Client** submits a list of integers and delay time.
- A **Leader** performs both local and distributed computation.
- Several **Nodes** compute partial sums.
- The Leader then compares results for accuracy and performance, with optional simulation of faulty node behavior.

This setup includes a lightweight consensus check to identify inconsistent node results.

---

## Prerequisites

- Java 21
- Gradle 8.14.1 or later
- No manual installation of `protoc` required — handled by the Gradle Protobuf plugin

---

## Building the Project

To build the project and auto-generate Protobuf message classes:

``` bash
./gradlew clean build
```

To only regenerate protocol classes:
``` bash
./gradlew generateProto --rerun-tasks
```

Generated classes are located in:
```
build/generated/source/proto/main/java/distributed/protocol/
```

### How to Run
Use the following Gradle CLI tasks. You can configure host and port using:
- `-Phost=<hostname>` (default: `localhost`)
- `-Pport=<port>` (default: `8500`)
- `-Pwrong=1` to simulate a faulty node

### Step-by-step Execution
Run in 4–5 terminals:

``` bash
# Terminal 1 - Start Leader
gradle runLeader -Pport=8500

# Terminal 2-4 - Start Node2
gradle runNode -Phost=localhost -Pport=8500
gradle runNode -Phost=localhost -Pport=8500
gradle runNode -Phost=localhost -Pport=8500

# Terminal 5 - Start Client (optional)
gradle runClient -Phost=localhost -Pport=8500
```

## Communication Format
All communication uses **Protocol Buffers**.
See `PROTOCOL.md` for full message specifications.