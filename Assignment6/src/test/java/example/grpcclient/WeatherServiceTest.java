package example.grpcclient;

import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.*;
import service.*;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

public class WeatherServiceTest {
    private static io.grpc.Server server;
    private static WeatherGrpc.WeatherBlockingStub stub;
    private static String apiKey;

    @BeforeClass
    public static void startServer() throws Exception {
        // build & start an in-process or on-port server
        server = io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
                .forPort(9002)
                .addService(new example.grpcclient.WeatherImpl())
                .build()
                .start();

        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", 9002)
                .usePlaintext()
                .build();
        stub = WeatherGrpc.newBlockingStub(channel);

        // grab the key from the environment
        apiKey = System.getenv("OPENWEATHER_API_KEY");
        System.out.println("DEBUG: OPENWEATHER_API_KEY=" + apiKey);
    }

    @AfterClass
    public static void stopServer() throws Exception {
        server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    public void listCities_success() {
        CitiesResponse res = stub.listCities(Empty.getDefaultInstance());
        assertTrue(res.getIsSuccess());
        // matches your CITY_COORDS map keys
        assertTrue(res.getCityNameList().contains("Seattle"));
        assertTrue(res.getCityNameList().contains("Tempe"));
    }

    @Test
    public void inCity_unknownCity() {
        WeatherResponse res = stub.inCity(
                WeatherCityRequest.newBuilder()
                        .setCityName("Atlantis")
                        .build());
        assertFalse(res.getIsSuccess());
        assertTrue(res.getError().contains("Unknown city"));
    }

    @Test
    public void inCity_knownCity_success() {
        // only run if the API key is set
        assumeTrue(apiKey != null && !apiKey.isEmpty());

        WeatherResponse res = stub.inCity(
                WeatherCityRequest.newBuilder()
                        .setCityName("Seattle")
                        .build());
        assertTrue(res.getIsSuccess());
        assertNotNull(res.getCurrentConditions());
    }

    @Test
    public void atCoordinates_knownCoords_success() {
        assumeTrue(apiKey != null && !apiKey.isEmpty());

        WeatherResponse res = stub.atCoordinates(
                WeatherCoordinateRequest.newBuilder()
                        .setLatitude(47.6062)
                        .setLongitude(-122.3321)
                        .build());
        assertTrue(res.getIsSuccess());
        // your impl always returns at least one dailyHigh
        assertTrue(res.getDailyHighsCount() >= 1);
    }
}