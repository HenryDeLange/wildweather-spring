package mywild.wildweather.domain.weather.schedulers.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import mywild.ambientweather.openapi.client.api.AmbientWeatherApi;
import mywild.weatherunderground.openapi.client.api.WeatherUndergroundApi;
import mywild.weatherunderground.openapi.client.model.FormatEnum;
import mywild.weatherunderground.openapi.client.model.NumericPrecisionEnum;
import mywild.weatherunderground.openapi.client.model.UnitsEnum;
import mywild.wildweather.domain.weather.data.WeatherRepository;
import mywild.wildweather.domain.weather.schedulers.Utils;

/**
 * https://www.wunderground.com/member/api-keys
 * https://docs.google.com/document/d/1eKCnKXI9xnoMGRRzOL1xPCBihNV2rOet08qpE_gArAY
 * https://docs.google.com/document/d/13HTLgJDpsb39deFzk_YCQ5GoGoZCO_cRYzIxbwvgJLI
 * https://docs.google.com/document/d/1w8jbqfAk0tfZS5P7hYnar1JiitM0gQZB-clxDfG3aD0
 */

@Slf4j
@Service
public class WeatherUndergroundApiScheduler {

    private static final int SCHEDULE_DELAY = 5 * 60 * 1000; // 5 minutes
    private static final int SCHEDULE_RATE = 1 * 60 * 60 * 1000; // 1 hours
    private static final int STOP_AT_EMPTY_DAYS = 5;

    private static final DateTimeFormatter DATE_FORMAT =  DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final AtomicBoolean IS_RUNNING = new AtomicBoolean(false);

    @Value("${mywild.csv.folder}")
    private String csvRootFolder;

    @Autowired
    private WeatherUndergroundApi api;

    @Autowired
    private WeatherRepository repo;

    // @Scheduled(initialDelay = SCHEDULE_DELAY, fixedRate = SCHEDULE_RATE)
    void scheduledApiProcessing() {
        processApiData();
    }

    public boolean isRunning() {
        return IS_RUNNING.get();
    }

    @Async
    public void processApiData() {
        if (!IS_RUNNING.compareAndSet(false, true)) {
            log.warn("Already busy processing Weather Underground data... The new request will be ignored.");
            return;
        }
        try (Stream<Path> paths = Files.walk(Paths.get(csvRootFolder))) {
            log.info("********************************************");
            log.info("Fetching Weather Underground API data");
            log.info("********************************************");
            List<Path> stationIdFiles = paths
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith("WeatherUnderground-StationId.txt"))
                .toList();
            for (var stationIdPath : stationIdFiles) {
                var station = Utils.getStationName(stationIdPath);
                var readRecords = 0;
                var processedDays = 0;
                var consecutiveEmptyDays = 0;
                try (var reader = Files.newBufferedReader(stationIdPath)) {
                    var stationId = reader.readLine();
                    if (stationId.equalsIgnoreCase("SKIP")) {
                        log.info("Skipping Weather Underground API fetching for : {}", station);
                        continue;
                    }
                    log.info("----------------");
                    log.info("Processing Weather Underground API : {} -> {}", stationId, station);
                    LocalDate apiDate = LocalDate.now().minusDays(1); // Yesterday midnight
                    do {
                        var summaryCsvPath = CsvWriter.getCsvPath(stationIdPath.getParent(), LocalDateTime.of(apiDate, LocalTime.MIDNIGHT));
                        if (summaryCsvPath != null && !Files.exists(summaryCsvPath)) {
                            // Fetch the API data
                            log.info("   Fetching data for : {}", apiDate);
// TODO: rather fetch a month at a time
                            var httpData = api.getDailyWithHttpInfo(stationId, apiDate.format(DATE_FORMAT), 
                                FormatEnum.JSON, UnitsEnum.METRIC, null, null, NumericPrecisionEnum.DECIMAL);
                            var data = httpData.getData();
                            if (data != null && data.getObservations() != null && !data.getObservations().isEmpty()) {
                                // Calculate the daily low/ave/high values
                                List<Double> low = new ArrayList<>();
                                List<Double> high = new ArrayList<>();
                                List<Double> average = new ArrayList<>();
                                for (var dataRecord : data.getObservations()) {
                                    low     .add(dataRecord.getMetric().getTempLow());
                                    average .add(dataRecord.getMetric().getTempAvg());
                                    high    .add(dataRecord.getMetric().getTempHigh());
                                    low     .add(dataRecord.getMetric().getWindspeedLow());
                                    average .add(dataRecord.getMetric().getWindspeedAvg());
                                    high    .add(dataRecord.getMetric().getWindspeedHigh());
                                    low     .add(dataRecord.getMetric().getWindgustLow());
                                    average .add(dataRecord.getMetric().getWindgustAvg());
                                    high    .add(dataRecord.getMetric().getWindgustHigh());
                                    low     .add(dataRecord.getWinddirAvg());
                                    average .add(dataRecord.getWinddirAvg());
                                    high    .add(dataRecord.getWinddirAvg());
                                    low     .add(dataRecord.getMetric().getPrecipRate());
                                    average .add(dataRecord.getMetric().getPrecipRate());
                                    high    .add(dataRecord.getMetric().getPrecipRate());
                                    low     .add(dataRecord.getMetric().getPrecipTotal());
                                    average .add(dataRecord.getMetric().getPrecipTotal());
                                    high    .add(dataRecord.getMetric().getPrecipTotal());
                                    low     .add(dataRecord.getMetric().getPressureMin());
                                    average .add(dataRecord.getMetric().getPressureTrend());
                                    high    .add(dataRecord.getMetric().getPressureMax());
                                    low     .add(dataRecord.getHumidityLow());
                                    average .add(dataRecord.getHumidityAvg());
                                    high    .add(dataRecord.getHumidityHigh());
                                    low     .add(0.0);
                                    average .add(dataRecord.getUvHigh() != null ? dataRecord.getUvHigh() / 2.0 : null);
                                    high    .add(dataRecord.getUvHigh());
                                    readRecords++;
                                }
                                // Save the record to a CSV file
                                if (readRecords >= 1) {
                                    CsvWriter.writeCsvFile(
                                        summaryCsvPath, 
                                        apiDate,
                                        new ArrayList<>(average),
                                        new ArrayList<>(high),
                                        new ArrayList<>(low));
                                }
                                consecutiveEmptyDays = 0;
                            }
                            else {
                                log.info("   No data returned : {} - {}", httpData.getStatusCode(), 
                                    httpData.getData() == null ? "Response was null" 
                                        : httpData.getData().getObservations() == null ? "Observations was null"
                                            : "Observations was empty");
                                consecutiveEmptyDays++;
                            }
                            processedDays++;
                            // Sleep for 1 seconds to not spam the api too much
                            Thread.sleep(Duration.ofSeconds(1));
                        }
                        else {
                            log.debug("   Skip {} - Found CSV file : {}",
                                apiDate, 
                                summaryCsvPath.getParent().getParent().relativize(summaryCsvPath).toString());
                        }
                        apiDate = apiDate.minusDays(1);
                    }
                    while (consecutiveEmptyDays < STOP_AT_EMPTY_DAYS
                        // && (mostRecentDatabaseDate != null 
                        //     && (mostRecentDatabaseDate.isEqual(apiDate)
                        //         || mostRecentDatabaseDate.isBefore(apiDate)))
                    );
                }
                catch (InterruptedException ex) {
                    log.warn("Processing interrupted!", ex);
                }
                finally {
                    log.info("   Read Records   : {}", readRecords);
                    log.info("   Processed Days : {}", processedDays);
                }
            }
        }
        catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
        finally {
            log.info("********************************************");
            log.info("Processed all Weather Underground API data");
            log.info("********************************************");
            IS_RUNNING.set(false);
        }
    }

}
