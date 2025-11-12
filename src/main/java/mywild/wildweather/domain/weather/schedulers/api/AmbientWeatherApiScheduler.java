package mywild.wildweather.domain.weather.schedulers.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
import mywild.wildweather.domain.weather.data.WeatherRepository;
import mywild.wildweather.domain.weather.schedulers.Utils;

/**
 * https://ambientweather.net/account/keys
 * https://ambientweather.docs.apiary.io/
 * https://github.com/ambient-weather/api-docs/wiki/Device-Data-Specs
 */

@Slf4j
@Service
public class AmbientWeatherApiScheduler {

    private static final int SCHEDULE_DELAY = 5 * 60 * 1000; // 5 minutes
    private static final int SCHEDULE_RATE = 1 * 60 * 60 * 1000; // 1 hours
    private static final int EXPECTED_RECORDS_PER_DAY = 24 * (60 / 5); // 288 (Every 5 minutes)

    private static final AtomicBoolean IS_RUNNING = new AtomicBoolean(false);

    @Value("${mywild.csv.folder}")
    private String csvRootFolder;

    @Autowired
    private AmbientWeatherApi api;

    @Autowired
    private WeatherRepository repo;

    @Scheduled(initialDelay = SCHEDULE_DELAY, fixedRate = SCHEDULE_RATE)
    void scheduledApiProcessing() {
        processApiData();
    }

    public boolean isRunning() {
        return IS_RUNNING.get();
    }

    @Async
    public void processApiData() {
        if (!IS_RUNNING.compareAndSet(false, true)) {
            log.warn("Already busy processing Ambient Weather API data... The new request will be ignored.");
            return;
        }
        try (Stream<Path> paths = Files.walk(Paths.get(csvRootFolder))) {
            log.info("****************************************");
            log.info("Fetching Ambient Weather API data");
            log.info("****************************************");
            List<Path> macAddressFiles = paths
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith("macAddress.txt"))
                .toList();
            for (var macAddressPath : macAddressFiles) {
                var station = Utils.getStationName(macAddressPath);
                LocalDate mostRecentDatabaseDate = repo.findTopDateByStation(station);
                var readRecords = 0;
                var processedDays = 0;
                try (var reader = Files.newBufferedReader(macAddressPath)) {
                    var stationMacAddress = reader.readLine();
                    log.info("----------------");
                    log.info("Processing Ambient Weather API : {}", station);
                    OffsetDateTime apiEndDate = LocalDate.now(ZoneOffset.UTC).atStartOfDay().atOffset(ZoneOffset.UTC).minusSeconds(1); // Yesterday midnight
                    do {
                        var summaryCsvPath = CsvWriter.getCsvPath(macAddressPath.getParent(), apiEndDate.toLocalDateTime());
                        if (summaryCsvPath != null && !Files.exists(summaryCsvPath)) {
                            // Fetch the API data
                            log.info("   Fetching data for : {}", apiEndDate.toLocalDate());
                            var data = api.getDeviceData(stationMacAddress, apiEndDate, EXPECTED_RECORDS_PER_DAY);
                            // Calculate the daily low/ave/high values
                            Map<Integer, Double> low = new LinkedHashMap<>();
                            Map<Integer, Double> high = new LinkedHashMap<>();
                            Map<Integer, List<Double>> average = new LinkedHashMap<>();
                            for (var dataRecord : data) {
                                if (dataRecord.getDate().toLocalDate().equals(apiEndDate.toLocalDate())) {
                                    processValue(low, high, average, 0, Conversions.fahToCel(dataRecord.getTempf()));
                                    processValue(low, high, average, 1, Conversions.mphToKmh(dataRecord.getWindspeedmph()));
                                    processValue(low, high, average, 2, Conversions.mphToKmh(dataRecord.getWindgustmph()));
                                    processValue(low, high, average, 3, dataRecord.getWinddir());
                                    processValue(low, high, average, 4, Conversions.inToMm(dataRecord.getHourlyrainin()));
                                    processValue(low, high, average, 5, Conversions.inToMm(dataRecord.getDailyrainin()));
                                    processValue(low, high, average, 6, Conversions.inHgToHpa(dataRecord.getBaromrelin()));
                                    processValue(low, high, average, 7, dataRecord.getHumidity());
                                    processValue(low, high, average, 8, dataRecord.getUv());
                                    readRecords++;
                                }
                                else {
                                    log.debug("   Not processing records for date {} while busy processing {}",
                                        dataRecord.getDate().toLocalDate(), apiEndDate.toLocalDate());
                                    break;
                                }
                            }
                            // Save the record to a CSV file
                            Map<Integer, Double> calculatedAverage = getCalculatedAverage(average);
                            CsvWriter.writeCsvFile(
                                summaryCsvPath, 
                                apiEndDate.toLocalDate(),
                                new ArrayList<>(calculatedAverage.values()),
                                new ArrayList<>(high.values()),
                                new ArrayList<>(low.values()));
                            processedDays++;
                            // Sleep for 2 seconds to comply with API guidelines (of 1 request per second)
                            Thread.sleep(Duration.ofSeconds(2));
                        }
                        else {
                            log.info("   Skip {} - Found CSV file : {}",
                                apiEndDate.toLocalDate(), 
                                summaryCsvPath.getParent().getParent().relativize(summaryCsvPath).toString());
                        }
                        apiEndDate = apiEndDate.minusDays(1);
                    }
                    while (mostRecentDatabaseDate.isEqual(apiEndDate.toLocalDate())
                        || mostRecentDatabaseDate.isBefore(apiEndDate.toLocalDate()));
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
            log.info("****************************************");
            log.info("Processed all Ambient Weather API data");
            log.info("****************************************");
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
