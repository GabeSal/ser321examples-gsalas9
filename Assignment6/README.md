# gRPC Services & Registry (Assignment 6)

This project implements five gRPC services plus a JSON/Protobuf registry:
- **Echo** – simple “parrot” service
- **Joke** – joke‐of‐the‐day service
- **Password** (Caesar) – in-memory encrypted storage
- **Weather** – current & 5-day forecast via OpenWeatherMap
- **Notes** – simple create/list/delete note store

All services speak **gRPC** over HTTP/2, with Protocol Buffers for message definitions (under `src/main/proto/services`).

---

## Prerequisites

- **Java 21** JDK
- **Gradle 8.14.1** (or via the Gradle wrapper)
- **Windows 11 / macOS / Linux**
- **OpenWeatherMap API key** (free tier) for the Weather service

    1. You can utilize my active API key value: *49d0d41aea6d13c9f8e9c05e0c389081* or...
    2. Sign up at https://openweathermap.org/
    3. Copy your key and store it in your local Gradle properties.

       ```properties
       # in ~/.gradle/gradle.properties or project-root/gradle.properties (git-ignored)
       openWeatherApiKey=YOUR_OPENWEATHER_API_KEY
       ```

Gradle will inject this as `OPENWEATHER_API_KEY` into the test & server JVMs. Please note that you must set the 
environment variable, `OPENWEATHER_API_KEY`, in your IDE or as a temporary variable within the command prompt before
running the Node task.

---

## Build

``` bash
# from project root
./gradlew clean build
```
This compiles all Java, generates gRPC/Protobuf stubs, and runs the JUnit suite (if a server is running or embedded).

## Running the Registry

We provide a multiprotocol “registry” on two ports:
- Protobuf registry (gRPC) on port 9000
- JSON registry (plain TCP+JSON) on port 9001

You can launch it with:
``` bash
./gradlew runRegistryServer
```
(The registry will then accept `Register/GetServices/FindServer/FindServers` requests.)

## Running a Service Node
By default, nodes run on gRPC port 9002 (JSON discovery port 10000). You can turn registry registration on/off:
``` bash
# registry OFF (default)
./gradlew runNode

# registry ON
./gradlew runNode -PregOn=true

```

You can override ports/hosts via Gradle properties, e.g.;
``` bash
./gradlew runNode \
  -PregOn=true \
  -PgrpcPort=8080 \
  -PregistryHost=registry.example.com \
  -PregistryJsonPort=9001 \
  -PserviceHost=127.0.0.1 \
  -PservicePort=8000
```

## Running the Client
The same CLI can run in **interactive** or **auto** mode:
``` bash
# interactive REPL (menu-driven)
./gradlew runClient

# “auto” mode exercises all services & error paths, then exits
./gradlew runClient -Pauto=1
```

If you used `-PregOn=true` on the server, pass that to the client as well so it can look up services via the registry:
``` bash
./gradlew runClient -PregOn=true -Pauto=1
```

---

## Gradle Tasks Overview
| **Task**                   | **Description**                                                    |
|----------------------------|--------------------------------------------------------------------|
| `runRegistryServer`        | Start JSON+Protobuf registry on ports 9001 (JSON) & 9000 (gRPC)    |
| `runNode`                  | Start a service node (Echo, Joke, Password, Weather, Notes)        |
| `runClient`                | Start the CLI client (interactive or `-Pauto=1` script mode)       |
| `runDiscovery`             | (Demo) fire up dummy services for registry examples                |
| `testJsonRegistration`     | Register & query services via JSON protocol                        |
| `testProtobufRegistration` | Register & query services via Protobuf                             |
| `test`                     | Run all unit tests (includes `ServerTest`, service‐specific tests) |

## Protocol Buffers & gRPC
- All messages are defined under src/main/proto/services/*.proto
- Gradle’s protobuf plugin generates:
  - `service.*Grpc` stubs
  - `service.*` message classes
- Services communicate over **HTTP/2** using gRPC’s binary framing and Protobuf marshalling.
- Clients use blocking stubs (synchronous RPCs) but you can also use async/future stubs.

---

## Tests

### Embedded-server tests
Our JUnit tests spin up an in-process gRPC server so you don’t need to manually launch runNode:

- ServerTest – Echo & Joke
- PasswordServiceTest – Caesar
- WeatherServiceTest – Weather (skipped if `OPENWEATHER_API_KEY` is unset)
- NoteServiceTest – Notes

Run them with:
``` bash
./gradlew test
```
