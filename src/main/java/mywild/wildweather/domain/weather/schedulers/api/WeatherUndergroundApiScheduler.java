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
    private static final int EXPECTED_RECORDS_PER_DAY = 24 * (60 / 5); // 288 (Every 5 minutes)

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
            log.info("*************************");
            log.info("Fetching Weather Underground API data");
            log.info("*************************");
            List<Path> stationIdFiles = paths
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith("WeatherUnderground-StationId.txt"))
                .toList();
            for (var stationIdPath : stationIdFiles) {
                var station = Utils.getStationName(stationIdPath);
                LocalDate mostRecentDatabaseDate = repo.findTopDateByStation(station);
                var readRecords = 0;
                var processedDays = 0;
                try (var reader = Files.newBufferedReader(stationIdPath)) {
                    var stationId = reader.readLine();
                    if (stationId.equalsIgnoreCase("SKIP")) {
                        log.info("Skipping Weather Underground API fetching for : {}", station);
                        continue;
                    }
                    log.info("----------------");
                    log.info("Processing Weather Underground API : {}", station);
                    LocalDate apiDate = LocalDate.now().minusDays(1); // Yesterday midnight
                    do {
                        var summaryCsvPath = CsvWriter.getCsvPath(stationIdPath.getParent(), LocalDateTime.of(apiDate, LocalTime.MIDNIGHT));
                        if (summaryCsvPath != null && !Files.exists(summaryCsvPath)) {
                            // Fetch the API data
                            log.info("   Fetching data for : {}", apiDate);
                            var data = api.getDaily(stationId, apiDate.format(DATE_FORMAT), 
                                FormatEnum.JSON, UnitsEnum.METRIC, null, null, NumericPrecisionEnum.DECIMAL);
                            // Calculate the daily low/ave/high values
                            Map<Integer, Double> low = new LinkedHashMap<>();
                            Map<Integer, Double> high = new LinkedHashMap<>();
                            Map<Integer, List<Double>> average = new LinkedHashMap<>();
                            System.out.println(data);
                            // for (var dataRecord : data) {
                            //     if (dataRecord.getDate().toLocalDate().equals(apiEndDate.toLocalDate())) {
                            //         processValue(low, high, average, 0, Conversions.fahToCel(dataRecord.getTempf()));
                            //         processValue(low, high, average, 1, Conversions.mphToKmh(dataRecord.getWindspeedmph()));
                            //         processValue(low, high, average, 2, Conversions.mphToKmh(dataRecord.getWindgustmph()));
                            //         processValue(low, high, average, 3, dataRecord.getWinddir());
                            //         processValue(low, high, average, 4, Conversions.inToMm(dataRecord.getHourlyrainin()));
                            //         processValue(low, high, average, 5, Conversions.inToMm(dataRecord.getDailyrainin()));
                            //         processValue(low, high, average, 6, Conversions.inHgToHpa(dataRecord.getBaromrelin()));
                            //         processValue(low, high, average, 7, dataRecord.getHumidity());
                            //         processValue(low, high, average, 8, dataRecord.getUv());
                            //         readRecords++;
                            //     }
                            //     else {
                            //         log.debug("   Not processing records for date {} while busy processing {}",
                            //             dataRecord.getDate().toLocalDate(), apiEndDate.toLocalDate());
                            //         break;
                            //     }
                            // }
                            // Save the record to a CSV file
                            Map<Integer, Double> calculatedAverage = getCalculatedAverage(average);
                            CsvWriter.writeCsvFile(
                                summaryCsvPath, 
                                apiDate,
                                new ArrayList<>(calculatedAverage.values()),
                                new ArrayList<>(high.values()),
                                new ArrayList<>(low.values()));
                            processedDays++;
                            // Sleep for 2 seconds to comply with API guidelines (of 1 request per second)
                            Thread.sleep(Duration.ofSeconds(2));
                        }
                        else {
                            log.info("   Skip {} - Found CSV file : {}",
                                apiDate, 
                                summaryCsvPath.getParent().getParent().relativize(summaryCsvPath).toString());
                        }
                        apiDate = apiDate.minusDays(1);
                    }
                    while (mostRecentDatabaseDate.isEqual(apiDate)
                        || mostRecentDatabaseDate.isBefore(apiDate));
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
            log.info("****************************");
            log.info("Processed all Weather Underground API data");
            log.info("****************************");
            IS_RUNNING.set(false);
        }
    }

    private void processValue(
            Map<Integer, Double> low,
            Map<Integer, Double> high,
            Map<Integer, List<Double>> average,
            int headerIndex,
            double value
    ) {
        low.merge(headerIndex, value, Math::min);
        high.merge(headerIndex, value, Math::max);
        average.computeIfAbsent(headerIndex, k -> new ArrayList<>()).add(value);
    }

    private Map<Integer, Double> getCalculatedAverage(Map<Integer, List<Double>> average) {
        Map<Integer, Double> calculatedAverage = average.entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> {
                List<Double> vals = e.getValue();
                if (vals == null || vals.isEmpty()) {
                    return 0.0;
                }
                double avg = vals.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                return Conversions.roundToOneDecimal(avg);
            },
            (a, b) -> a,
            LinkedHashMap::new
        ));
        return calculatedAverage;
    }

}
