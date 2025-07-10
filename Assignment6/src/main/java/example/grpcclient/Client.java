package example.grpcclient;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import service.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import com.google.protobuf.Empty; // needed to use Empty


/**
 * Client that requests `parrot` method from the `EchoServer`.
 */
public class Client {
  private final EchoGrpc.EchoBlockingStub blockingStub;
  private final JokeGrpc.JokeBlockingStub blockingStub2;
  private final RegistryGrpc.RegistryBlockingStub blockingStub3;
  private final RegistryGrpc.RegistryBlockingStub blockingStub4;
  private final CaesarGrpc.CaesarBlockingStub caesarStub;
  private final WeatherGrpc.WeatherBlockingStub weatherStub;
  private final NoteServiceGrpc.NoteServiceBlockingStub noteStub;

  /** Construct client for accessing server using the existing channel. */
  public Client(Channel channel, Channel regChannel) {
    // 'channel' here is a Channel, not a ManagedChannel, so it is not this code's
    // responsibility to
    // shut it down.

    // Passing Channels to code makes code easier to test and makes it easier to
    // reuse Channels.
    blockingStub = EchoGrpc.newBlockingStub(channel);
    blockingStub2 = JokeGrpc.newBlockingStub(channel);
    blockingStub3 = RegistryGrpc.newBlockingStub(regChannel);
    blockingStub4 = RegistryGrpc.newBlockingStub(channel);
    caesarStub = CaesarGrpc.newBlockingStub(channel);
    weatherStub = WeatherGrpc.newBlockingStub(channel);
    noteStub = NoteServiceGrpc.newBlockingStub(channel);
  }

  /** Construct client for accessing server using the existing channel. */
  public Client(Channel channel) {
    // 'channel' here is a Channel, not a ManagedChannel, so it is not this code's
    // responsibility to
    // shut it down.

    // Passing Channels to code makes code easier to test and makes it easier to
    // reuse Channels.
    blockingStub = EchoGrpc.newBlockingStub(channel);
    blockingStub2 = JokeGrpc.newBlockingStub(channel);
    blockingStub3 = null;
    blockingStub4 = null;
    caesarStub = CaesarGrpc.newBlockingStub(channel);
    weatherStub = WeatherGrpc.newBlockingStub(channel);
    noteStub = NoteServiceGrpc.newBlockingStub(channel);
  }

  public void askServerToParrot(String message) {

    ClientRequest request = ClientRequest.newBuilder().setMessage(message).build();
    ServerResponse response;
    try {
      response = blockingStub.parrot(request);
    } catch (Exception e) {
      System.err.println("RPC failed: " + e.getMessage());
      return;
    }
    System.out.println("Received from server: " + response.getMessage());
  }

  public void askForJokes(int num) {
    JokeReq request = JokeReq.newBuilder().setNumber(num).build();
    JokeRes response;

    // just to show how to use the empty in the protobuf protocol
    Empty empt = Empty.newBuilder().build();

    try {
      response = blockingStub2.getJoke(request);
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
    System.out.println("Your jokes: ");
    for (String joke : response.getJokeList()) {
      System.out.println("--- " + joke);
    }
  }

  public void setJoke(String joke) {
    JokeSetReq request = JokeSetReq.newBuilder().setJoke(joke).build();
    JokeSetRes response;

    try {
      response = blockingStub2.setJoke(request);
      System.out.println(response.getOk());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void getNodeServices() {
    GetServicesReq request = GetServicesReq.newBuilder().build();
    ServicesListRes response;
    try {
      response = blockingStub4.getServices(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void getServices() {
    GetServicesReq request = GetServicesReq.newBuilder().build();
    ServicesListRes response;
    try {
      response = blockingStub3.getServices(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void findServer(String name) {
    FindServerReq request = FindServerReq.newBuilder().setServiceName(name).build();
    SingleServerRes response;
    try {
      response = blockingStub3.findServer(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void findServers(String name) {
    FindServersReq request = FindServersReq.newBuilder().setServiceName(name).build();
    ServerListRes response;
    try {
      response = blockingStub3.findServers(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void encryptPassword(String name, String pw) {
    SaveReq req = SaveReq.newBuilder()
            .setName(name)
            .setPassword(pw)
            .build();
    SaveRes res = caesarStub.encrypt(req);
    System.out.printf("Encrypt '%s': ok=%b, err=%s%n",
            name, res.getOk(), res.getError());
  }

  public void decryptPassword(String name) {
    PasswordReq req = PasswordReq.newBuilder()
            .setName(name)
            .build();
    PasswordRes res = caesarStub.decrypt(req);
    if (res.getOk()) {
      System.out.printf("Decrypted '%s' → %s%n", name, res.getPassword());
    } else {
      System.out.printf("Decrypt error for '%s': %s%n", name, res.getError());
    }
  }

  public void listPasswords() {
    PasswordList list =
            caesarStub.listPasswords(com.google.protobuf.Empty.getDefaultInstance());
    System.out.println("Saved names: " + list.getPassListList());
  }

  public void listCities() {
    CitiesResponse resp =
            weatherStub.listCities(com.google.protobuf.Empty.getDefaultInstance());
    if (!resp.getIsSuccess()) {
      System.out.println("Error: " + resp.getError());
    } else {
      System.out.println("Known cities: " + resp.getCityNameList());
    }
  }

  public void weatherInCity(String city) {
    WeatherResponse r = weatherStub.inCity(
            WeatherCityRequest.newBuilder().setCityName(city).build());
    printWeatherResponse(r);
  }

  public void weatherAt(double lat, double lon) {
    WeatherResponse r = weatherStub.atCoordinates(
            WeatherCoordinateRequest.newBuilder()
                    .setLatitude(lat)
                    .setLongitude(lon)
                    .build());
    printWeatherResponse(r);
  }

  private void printWeatherResponse(WeatherResponse r) {
    if (!r.getIsSuccess()) {
      System.out.println("Error: " + r.getError());
      return;
    }
    System.out.printf(
            "Now: %.1f F, %s%nHighs: %s%n",
            r.getCurrentTemp(),
            r.getCurrentConditions(),
            r.getDailyHighsList());
  }

  public void createNote(String text) {
    CreateNoteResponse res = noteStub.createNote(
            CreateNoteRequest.newBuilder().setNote(text).build());
    if (res.getOk()) {
      System.out.println("Created note ID=" + res.getId());
    } else {
      System.out.println("Error: " + res.getError());
    }
  }

  public void listNotes() {
    GetNotesResponse res = noteStub.getNotes(Empty.getDefaultInstance());
    res.getNotesList().forEach(n ->
            System.out.printf("  [%d] %s%n", n.getId(), n.getNote()));
  }

  public void deleteNote(long id) {
    DeleteNoteResponse res = noteStub.deleteNote(
            DeleteNoteRequest.newBuilder().setId(id).build());
    System.out.println(res.getOk()
            ? "Deleted " + id
            : "Error: " + res.getError());
  }

  private static boolean performMainLoop(BufferedReader reader, Client client, boolean exit) throws IOException {
    // ==== MAIN MENU ====
    System.out.println("\n=== Main Menu ===");
    System.out.println("1: Echo");
    System.out.println("2: Tell a Joke");
    System.out.println("3: Password Service");
    System.out.println("4: Weather Forecast");
    System.out.println("5: Notes Service");
    System.out.println("6: Quit");
    System.out.print("Select [1-6]: ");

    int choice;
    try {
      choice = Integer.parseInt(reader.readLine());
    } catch (NumberFormatException e) {
      System.out.println("Invalid input; please enter a number 1-5.");
      return exit;
    }

    switch (choice) {
      case 1:
        // Echo
        System.out.print("Enter message to echo: ");
        String msg = reader.readLine();
        client.askServerToParrot(msg);
        break;

      case 2:
        // Joke
        System.out.print("How many jokes? ");
        try {
          int n = Integer.parseInt(reader.readLine());
          client.askForJokes(n);
        } catch (NumberFormatException e) {
          System.out.println("Please enter a valid integer.");
        }
        break;

      case 3:
        // Password sub-menu
        while (true) {
          System.out.println("\n-- Password Menu --");
          System.out.println("1: Encrypt & Save");
          System.out.println("2: Decrypt");
          System.out.println("3: List Saved Names");
          System.out.println("4: Back to Main");
          System.out.print("Select [1-4]: ");

          int passwordChoice;
          try {
            passwordChoice = Integer.parseInt(reader.readLine());
          } catch (NumberFormatException e) {
            System.out.println("Invalid; enter 1-4.");
            continue;
          }

          if (passwordChoice == 4) break;        // exit sub-menu

          switch (passwordChoice) {
            case 1:
              System.out.print("Name to save under: ");
              String name = reader.readLine();
              System.out.print("Password to store: ");
              String pw    = reader.readLine();
              client.encryptPassword(name, pw);
              break;

            case 2:
              System.out.print("Name to decrypt: ");
              String lookup = reader.readLine();
              client.decryptPassword(lookup);
              break;

            case 3:
              client.listPasswords();
              break;

            default:
              System.out.println("Choose 1-4.");
          }
        }
        break;

      case 4:
        // Weather sub-menu
        while (true) {
          System.out.println("\n-- Weather Menu --");
          System.out.println("1: List Cities");
          System.out.println("2: Weather in City");
          System.out.println("3: Weather at Coordinates");
          System.out.println("4: Back to Main");
          System.out.print("Select [1-4]: ");

          int weatherChoice;
          try {
            weatherChoice = Integer.parseInt(reader.readLine());
          } catch (NumberFormatException e) {
            System.out.println(" Invalid; enter 1-4.");
            continue;
          }
          if (weatherChoice == 4) break;
          switch (weatherChoice) {
            case 1:
              client.listCities();            // your Weather helper
              break;
            case 2:
              System.out.print("Which city? ");
              client.weatherInCity(reader.readLine());
              break;
            case 3:
              try {
                System.out.print("Latitude: ");
                double lat = Double.parseDouble(reader.readLine());
                System.out.print("Longitude: ");
                double lon = Double.parseDouble(reader.readLine());
                client.weatherAt(lat, lon);
              } catch (NumberFormatException e) {
                System.out.println("Enter valid numbers for lat/lon.");
              }
              break;
            default:
              System.out.println("Choose 1-4.");
          }
        }
        break;

      case 5:
        // Notes sub-menu
        while (true) {
          System.out.println("\n-- Notes Service Menu --");
          System.out.println("1: Create Note");
          System.out.println("2: List Notes");
          System.out.println("3: Delete Note");
          System.out.println("4: Back to Main");
          System.out.print("Select [1-4]: ");

          int noteChoice;
          try {
            noteChoice = Integer.parseInt(reader.readLine());
          } catch (NumberFormatException e) {
            System.out.println("Invalid input; please enter 1-4.");
            continue;
          }
          if (noteChoice == 4) break;   // back to main menu

          switch (noteChoice) {
            case 1:
              // Create
              System.out.print("Enter note text: ");
              String text = reader.readLine();
              client.createNote(text);
              break;

            case 2:
              // List
              client.listNotes();
              break;

            case 3:
              // Delete
              System.out.print("Enter note ID to delete: ");
              try {
                long id = Long.parseLong(reader.readLine());
                client.deleteNote(id);
              } catch (NumberFormatException nfe) {
                System.out.println("Please enter a valid numeric ID.");
              }
              break;

            default:
              System.out.println("Choose 1-4.");
          }
        }
        break;

      case 6:
        exit = true;
        break;

      default:
        System.out.println("Please choose a number between 1 and 6.");
    }
    return exit;
  }

  private static void performAutoFunctions(Client client, ManagedChannel channel, boolean useReg, ManagedChannel regChannel) throws InterruptedException {
    System.out.println("=== AUTO MODE START ===");

    // 1) Echo: success + error
    System.out.println("-- Echo --");
    client.askServerToParrot("hello auto");
    client.askServerToParrot("");      // should produce an error

    // 2) Joke: happy + maybe error
    System.out.println("\n-- Joke --");
    client.askForJokes(1);
    client.askForJokes(999);           // out-of-jokes path

    // 3) Caesar (encrypt/decrypt/list)
    System.out.println("\n-- Password (Caesar) --");
    client.encryptPassword("foo", "Bar123!");
    client.listPasswords();
    client.decryptPassword("foo");
    client.decryptPassword("no_such"); // error path

    // 4) Weather: list, city, coords
    System.out.println("\n-- Weather --");
    client.listCities();
    client.weatherInCity("Seattle");
    client.weatherAt(33.4255, -111.94);

    // 5) Notes Service: create, list, delete, delete-error
    System.out.println("\n-- Notes Service --");
    System.out.println("Creating note: \"First auto note\"");
    client.createNote("First auto note");   // will print ID=1

    System.out.println("Listing notes:");
    client.listNotes();

    System.out.println("Deleting note ID=1");
    client.deleteNote(1);

    System.out.println("Attempting to delete non-existent note ID=999");
    client.deleteNote(999);

    System.out.println("=== AUTO MODE END ===");
    // clean shutdown and exit
    channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    if (useReg) regChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
  }

  public static void main(String[] args) throws Exception {
    if (args.length < 6) {
      System.err.println("Usage: <host> <port> <regHost> <regPort> <message> <regOn> [auto]");
      System.exit(1);
    }
    // parse args
    String host    = args[0];
    int    port    = Integer.parseInt(args[1]);
    String regHost = args[2];
    int    regPort = Integer.parseInt(args[3]);
    String message = args[4];
    boolean useReg = Boolean.parseBoolean(args[5]);
    boolean autoMode = (args.length >= 7 && "1".equals(args[6]));

    // build channels
    ManagedChannel channel    = ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext().build();
    ManagedChannel regChannel = ManagedChannelBuilder.forAddress(regHost, regPort)
            .usePlaintext().build();

    Client client = useReg
            ? new Client(channel, regChannel)
            : new Client(channel);

    if (autoMode) {
      performAutoFunctions(client, channel, useReg, regChannel);
      return;
    }

    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    boolean exit = false;

    while (!exit) {
      exit = performMainLoop(reader, client, exit);
    }

    // Clean shutdown
    channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    if (useReg) {
      regChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
    System.out.println("Goodbye!");
  }
}
