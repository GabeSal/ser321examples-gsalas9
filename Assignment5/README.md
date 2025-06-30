# Distributed Sum System

## Description
This project implements a distributed number summation system using Java, Gradle, Protocol Buffers, and JSON over TCP sockets. It simulates a distributed computing scenario where a client sends a list of numbers and a delay value to a leader node. The leader performs both a local and distributed sum and evaluates performance differences. A lightweight consensus algorithm ensures result correctness, even in the presence of simulated faulty nodes.

---

## How to Run

> Ensure you're using Java 21 and Gradle 8.14.1 or newer.

### Host and Port Configuration
Use Gradle properties:
- `-Phost=<hostname>` (default: `localhost`)
- `-Pport=<port>` (default: `8500`)
- `-Pwrong=1` to simulate a faulty node

### Step-by-step Execution

Start in **4 terminals** or more:

```bash
# Terminal 1 - Start Leader
gradle runLeader -Pport=8500

# Terminal 2 - Start Node 1
gradle runNode -Phost=localhost -Pport=8500

# Terminal 3 - Start Node 2
gradle runNode -Phost=localhost -Pport=8500

# Terminal 4 - Start Node 3
gradle runNode -Phost=localhost -Pport=8500

# Terminal 5 - Start Client (optional)
gradle runClient -Phost=localhost -Pport=8500
