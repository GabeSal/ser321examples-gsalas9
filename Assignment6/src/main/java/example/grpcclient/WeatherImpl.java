package example.grpcclient;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.json.JSONArray;
import org.json.JSONObject;
import service.WeatherCoordinateRequest;
import service.WeatherCityRequest;
import service.WeatherGrpc;
import service.WeatherResponse;
import service.CitiesResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * Implements the Weather service against OpenWeatherMap’s One-Call API.
 */
public class WeatherImpl extends WeatherGrpc.WeatherImplBase {

    // Map a few city names to lat/lon
    private static final Map<String,double[]> CITY_COORDS = Map.of(
            "Tempe",    new double[]{33.4255, -111.9400},
            "Seattle",  new double[]{47.6062, -122.3321},
            "NewYork",  new double[]{40.7128,  -74.0060}
    );

    private static final String API_KEY = System.getenv("OPENWEATHER_API_KEY");
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public void listCities(Empty _void, StreamObserver<CitiesResponse> obs) {
        CitiesResponse.Builder b = CitiesResponse.newBuilder()
                .setIsSuccess(true);
        CITY_COORDS.keySet().forEach(b::addCityName);
        obs.onNext(b.build());
        obs.onCompleted();
    }

    @Override
    public void inCity(WeatherCityRequest req, StreamObserver<WeatherResponse> obs) {
        String city = req.getCityName();
        double[] coords = CITY_COORDS.get(city);
        if (coords == null) {
            obs.onNext(WeatherResponse.newBuilder()
                    .setIsSuccess(false)
                    .setError("Unknown city: " + city)
                    .build());
            obs.onCompleted();
            return;
        }
        // delegate
        atCoordinates(
                WeatherCoordinateRequest.newBuilder()
                        .setLatitude(coords[0])
                        .setLongitude(coords[1])
                        .build(),
                obs
        );
    }

    @Override
    public void atCoordinates(WeatherCoordinateRequest req,
                              StreamObserver<WeatherResponse> obs) {
        if (API_KEY == null || API_KEY.isEmpty()) {
            obs.onNext(WeatherResponse.newBuilder()
                    .setIsSuccess(false)
                    .setError("OPENWEATHER_API_KEY not set")
                    .build());
            obs.onCompleted();
            return;
        }

        double lat = req.getLatitude(), lon = req.getLongitude();

        try {
            // 1) Current weather
            String currentUrl = String.format(
                    "https://api.openweathermap.org/data/2.5/weather" +
                            "?lat=%.6f&lon=%.6f&units=imperial&appid=%s",
                    lat, lon, API_KEY);

            HttpRequest curReq = HttpRequest.newBuilder()
                    .uri(URI.create(currentUrl))
                    .timeout(Duration.ofSeconds(5))
                    .GET().build();

            HttpResponse<String> curRes =
                    HTTP.send(curReq, HttpResponse.BodyHandlers.ofString());

            if (curRes.statusCode() != 200) {
                obs.onNext(WeatherResponse.newBuilder()
                        .setIsSuccess(false)
                        .setError("Current-weather HTTP " + curRes.statusCode())
                        .build());
                obs.onCompleted();
                return;
            }

            JSONObject curJson = new JSONObject(curRes.body());
            double tempNow = curJson.getJSONObject("main").getDouble("temp");
            String cond    = curJson
                    .getJSONArray("weather")
                    .getJSONObject(0)
                    .getString("description");
            double todayHigh = curJson.getJSONObject("main").getDouble("temp_max");

            // 2) 5-day / 3-hour forecast
            String forecastUrl = String.format(
                    "https://api.openweathermap.org/data/2.5/forecast" +
                            "?lat=%.6f&lon=%.6f&units=imperial&appid=%s",
                    lat, lon, API_KEY);

            HttpRequest fcReq = HttpRequest.newBuilder()
                    .uri(URI.create(forecastUrl))
                    .timeout(Duration.ofSeconds(5))
                    .GET().build();

            HttpResponse<String> fcRes =
                    HTTP.send(fcReq, HttpResponse.BodyHandlers.ofString());

            if (fcRes.statusCode() != 200) {
                obs.onNext(WeatherResponse.newBuilder()
                        .setIsSuccess(false)
                        .setError("Forecast HTTP " + fcRes.statusCode())
                        .build());
                obs.onCompleted();
                return;
            }

            JSONObject fcJson = new JSONObject(fcRes.body());
            JSONArray list    = fcJson.getJSONArray("list");

            // Group by date (YYYY-MM-DD) → max temp
            // Use LinkedHashMap to preserve insertion order (so days are in sequence)
            Map<String,Double> dailyMax = new LinkedHashMap<>();
            for (int i = 0; i < list.length(); i++) {
                JSONObject item = list.getJSONObject(i);
                String dtTxt    = item.getString("dt_txt");       // e.g. "2025-07-09 12:00:00"
                String date     = dtTxt.substring(0, 10);
                double tmax     = item.getJSONObject("main")
                        .getDouble("temp_max");
                dailyMax.merge(date, tmax, Math::max);
            }

            // 3) Build the proto response
            WeatherResponse.Builder resp = WeatherResponse.newBuilder()
                    .setIsSuccess(true)
                    .setCurrentTemp(tempNow)
                    .setCurrentConditions(cond)
                    // first day’s high from the /weather call
                    .addDailyHighs(todayHigh);

            // now append highs for the *next* days
            // skip the first date (today) if present in dailyMax
            boolean skipFirst = true;
            for (double high : dailyMax.values()) {
                if (skipFirst) { skipFirst = false; continue; }
                resp.addDailyHighs(high);
            }

            obs.onNext(resp.build());
            obs.onCompleted();
        }
        catch (IOException | InterruptedException e) {
            obs.onNext(WeatherResponse.newBuilder()
                    .setIsSuccess(false)
                    .setError("Weather lookup failed: " + e.getMessage())
                    .build());
            obs.onCompleted();
        }
    }
}