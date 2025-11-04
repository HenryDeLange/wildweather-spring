package mywild.wildweather.domain.weather.schedulers.api;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import mywild.ambientweather.openapi.client.api.AmbientWeatherApi;
import mywild.wildweather.domain.weather.data.WeatherRepository;
import mywild.wildweather.domain.weather.schedulers.Utils;

@Slf4j
@Service
public class WeatherApiScheduler {

    private static final int SCHEDULE_DELAY = 5 * 60 * 1000; // 5 minutes
    private static final int SCHEDULE_RATE = 1 * 60 * 60 * 1000; // 1 hours

    private static final AtomicBoolean isRunning = new AtomicBoolean(false);

    private static final String[] CSV_HEADERS = {
        "",
        "Date",
        "Outdoor Temperature (°C)",
        "Feels Like (°C)",
        "Dew Point (°C)",
        "Wind Speed (km/hr)",
        "Wind Gust (km/hr)",
        "Max Daily Gust (km/hr)",
        "Wind Direction (°)",
        "Rain Rate (mm/hr)",
        "Daily Rain (mm)",
        "Relative Pressure (hPa)",
        "Humidity (%)",
        "Ultra-Violet Radiation Index",
        "Absolute Pressure (hPa)"
    };

    private static final String SUMMARY_CSV_PREFIX = "api-ambient-weather-high-lows-details-";

    private static final DateTimeFormatter CSV_NAME_DATE_FORMAT =  DateTimeFormatter.ofPattern("yyyyMMdd");

    @Value("${mywild.csv.folder}")
    private String csvRootFolder;

    @Autowired
    private AmbientWeatherApi api;

    @Autowired
    private WeatherRepository repo;

    // TODO: Enable once this is fully implemented
    // @Scheduled(initialDelay = 10000 /*SCHEDULE_DELAY*/, fixedRate = SCHEDULE_RATE)
    void scheduledApiProcessing() {
        processApiData();
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    @Async
    public void processApiData() {
        if (!isRunning.compareAndSet(false, true)) {
            log.warn("Already busy processing api data... The new request will be ignored.");
            return;
        }
        try (Stream<Path> paths = Files.walk(Paths.get(csvRootFolder))) {
            log.info("*************************");
            log.info("Fetching API data");
            log.info("*************************");
            List<Path> macAddressFiles = paths
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith("macAddress.txt"))
                .toList();
            for (var macAddressPath : macAddressFiles) {
                var station = Utils.getStationName(macAddressPath);
                LocalDate mostRecentDatabaseDate = repo.findTopDateByStation(station);
                var readRecords = 0;
                try (var reader = Files.newBufferedReader(macAddressPath)) {
                    var stationMacAddress = reader.readLine();
                    log.info("----------------");
                    log.info("Processing API : {}", station);
                    OffsetDateTime apiEndDate = OffsetDateTime.now(ZoneOffset.UTC);
                    var csvDateStamp = apiEndDate.format(CSV_NAME_DATE_FORMAT);
                    var summaryCsvPath = macAddressPath.getParent().resolve(SUMMARY_CSV_PREFIX + csvDateStamp + ".csv");
                    if (!Files.exists(summaryCsvPath)) {
                        do {
                            // Fetch the API data
                            log.info("   Fetching data for : {}", apiEndDate);
                            var data = api.getDeviceData(stationMacAddress, apiEndDate, null);
                            // Calculate the daily low/ave/high values
                            Map<String, Double> low = new LinkedHashMap<>();
                            Map<String, Double> high = new LinkedHashMap<>();
                            Map<String, List<Double>> average = new LinkedHashMap<>();
                            for (var dataRecord : data) {
                                if (dataRecord.getDate().toLocalDate().equals(apiEndDate.toLocalDate())) {
                                    processValue(low, high, average, 1, fahrenheitToCelsius(dataRecord.getTempf()));
                                    processValue(low, high, average, 2, mphToKmh(dataRecord.getWindspeedmph()));
                                    processValue(low, high, average, 3, mphToKmh(dataRecord.getWindspeedmph()));
                                    processValue(low, high, average, 4, dataRecord.getWinddir());
                                    processValue(low, high, average, 5, inchesToMillimetres(dataRecord.getHourlyrainin()));
                                    processValue(low, high, average, 6, inchesToMillimetres(dataRecord.getDailyrainin()));
                                    processValue(low, high, average, 7, inHgToHpa(dataRecord.getBaromrelin()));
                                    processValue(low, high, average, 8, dataRecord.getHumidity());
                                    processValue(low, high, average, 9, dataRecord.getUv());
                                    readRecords++;
                                }
                                else {
                                    log.info("   Skipping records for date {} while processing {}",
                                        dataRecord.getDate().toLocalDate(), apiEndDate.toLocalDate());
                                    break;
                                }
                            }
                            // Save the record to a CSV file
                            Map<String, Double> singleAverages = average.entrySet().stream().collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> {
                                    List<Double> vals = e.getValue();
                                    if (vals == null || vals.isEmpty()) {
                                        return 0.0;
                                    }
                                    double avg = vals.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                                    return roundToOneDecimal(avg);
                                },
                                (a, b) -> a,
                                LinkedHashMap::new
                            ));
                            writeCsvFile(summaryCsvPath, low, high, singleAverages);
                            // Prepare for processing the next day
                            apiEndDate = data.get(data.size() - 1).getDate();
                            Thread.sleep(Duration.ofSeconds(2));
                        }
                        while (mostRecentDatabaseDate.isEqual(apiEndDate.toLocalDate())
                            || mostRecentDatabaseDate.isBefore(apiEndDate.toLocalDate()));
                    }
                    else {
                        log.info("   Skipping {} because CSV file already exists : {}", apiEndDate.toLocalDate(), summaryCsvPath);
                    }
                }
                catch (InterruptedException ex) {
                    log.warn("Processing interrupted!", ex);
                }
                finally {
                    log.info("   Read Records : {}", readRecords);
                }
            }
        }
        catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
        finally {
            log.info("****************************");
            log.info("Processed all API data");
            log.info("****************************");
            isRunning.set(false);
        }
    }

    private void processValue(
            Map<String, Double> low, Map<String, Double> high, Map<String, List<Double>> average,
            int headerIndex, double temperature) {
        low.merge(CSV_HEADERS[headerIndex], temperature, Math::min);
        high.merge(CSV_HEADERS[headerIndex], temperature, Math::max);
        average.computeIfAbsent(CSV_HEADERS[headerIndex], k -> new ArrayList<>()).add(temperature);
    }

    private void writeCsvFile(Path path, 
            Map<String, Double> lows, Map<String, Double> highs, Map<String, Double> averages) {
        log.info("Write file: {}", path);
        if (!Files.exists(path)) {
            try (
                FileWriter writer = new FileWriter(path.toFile());
                CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(CSV_HEADERS).get())
            ) {
                for (int i = 0; i < averages.size(); i++) {
                    printer.printRecord(Stream.concat(Stream.of("Average"), averages.values().stream()).toArray());
                    printer.printRecord(Stream.concat(Stream.of("High"), highs.values().stream()).toArray());
                    printer.printRecord(Stream.concat(Stream.of("Low"), lows.values().stream()).toArray());
                }
            }
            catch (IOException ex) {
                log.error(ex.getMessage(), ex);
            }
        }
        else {
            log.warn("   CSV file already exists : {}", path);
        }
    }

    private double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private double fahrenheitToCelsius(double fahrenheit) {
        return roundToOneDecimal((fahrenheit - 32) * 5.0 / 9.0);
    }

    private double mphToKmh(double mph) {
        return roundToOneDecimal(mph * 1.609344);
    }

    private double inchesToMillimetres(double inches) {
        return inches * 25.4;
    }

    private double inHgToHpa(double inHg) {
        return inHg * 33.8639;
    }

}
