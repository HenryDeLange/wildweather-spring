package mywild.wildweather.domain.weather.schedulers.csv;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import mywild.wildweather.domain.weather.data.WeatherRepository;
import mywild.wildweather.domain.weather.data.entity.WeatherCategory;
import mywild.wildweather.domain.weather.data.entity.WeatherEntity;
import mywild.wildweather.domain.weather.schedulers.SchedulerThreadFactory;
import mywild.wildweather.domain.weather.schedulers.Utils;

@Slf4j
@Component
public class ProcessSummaryFiles {

    private static final Object DATABASE_LOCK = new Object();

    private static final List<String> KNOWN_BAD_FILES = List.of(
        "Andante -> ambient-weather-high-lows-details-20241003-20251002.csv",
        "Andante -> api-weather-underground-high-lows-details-20241201-20241231.csv",
        "Andante -> api-weather-underground-high-lows-details-20241101-20241130.csv",
        "Corgi Corner -> ambient-weather-high-lows-details-20241003-20251002.csv",
        "Corgi Corner -> api-weather-underground-high-lows-details-20250101-20250131.csv",
        "Corgi Corner -> api-weather-underground-high-lows-details-20250201-20250228.csv"
    );

    @Autowired
    private WeatherRepository repo;
    
    List<Path> processAllSummaryFiles(Stream<Path> paths) throws InterruptedException {
        List<Path> csvFiles = paths
            .filter(Files::isRegularFile)
            .filter(path -> path.toString().toLowerCase(Locale.ROOT).endsWith(".csv"))
            .filter(path -> !WeatherCsvScheduler.hasFileBeenProcessed(CsvUtils.getCsvName(path)))
            .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)))
            .toList();
        List<Path> fineScaleCsvFiles = Collections.synchronizedList(new ArrayList<>());
        List<Callable<Void>> tasks = new ArrayList<>();
        csvFiles.forEach(csvFile -> {
            tasks.add(() -> {
                var isSummaryFile = processSummaryFile(csvFile);
                if (!isSummaryFile) {
                    fineScaleCsvFiles.add(csvFile);
                }
                return null;
            });
        });
        ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            new SchedulerThreadFactory("s-csv-"));
        executor.invokeAll(tasks);
        executor.shutdown();
        return fineScaleCsvFiles;
    }

    private boolean processSummaryFile(Path csvFile) {
        StringBuilder logBuilder = new StringBuilder();
        var csvName = CsvUtils.getCsvName(csvFile);
        logBuilder.append("----------------").append(System.lineSeparator());
        logBuilder.append(MessageFormatter.format("Processing File : {}", csvName).getMessage()).append(System.lineSeparator());
        var newRecords = 0;
        var duplicates = 0;
        var warnings = 0;
        var errors = 0;
        var missing = (csvName.contains("estimates-") || csvName.contains("api-")) ? 99.99 : 0;
        try (var reader = Files.newBufferedReader(csvFile)) {
            String[] headers = CsvUtils.getHeaders(reader);
            var isSummaryCsv = headers[0].equals("COL0");
            if (isSummaryCsv) {
                List<CSVRecord> records = CsvUtils.getRecords(reader, headers);
                for (CSVRecord record : records) {
                    try {
                        var categoryRecord = record.get("COL0");
                        if (!categoryRecord.contains("Datetime")) {
                            var category = WeatherCategory.valueOf(categoryRecord.substring(0, 1));
                            var date = LocalDate.parse(record.get("Date"));
                            var station = Utils.getStationName(csvFile);
                            var temperature = Double.parseDouble(record.get("Outdoor Temperature"));
                            var windSpeed = Double.parseDouble(record.get("Wind Speed"));
                            var windMax = Double.parseDouble(record.get("Max Daily Gust"));
                            var windDirection = record.get("Wind Direction");
                            var rainRate = Double.parseDouble(record.get("Rain Rate"));
                            var rainDaily = Double.parseDouble(record.get("Daily Rain"));
                            var pressure = Double.parseDouble(record.get("Relative Pressure"));
                            var humidity = Double.parseDouble(record.get("Humidity"));
                            var uvRadiationIndex = Double.parseDouble(record.get("Ultra-Violet Radiation Index"));
                            // TODO: Maybe better to build a map in memory and then save the map once-off after all the tasks are done
                            synchronized (DATABASE_LOCK) {
                                var entity = repo.findByDateAndStationAndCategory(date, station, category);
                                if (entity == null) {
                                    repo.save(
                                        WeatherEntity.builder()
                                            .station(station)
                                            .date(date)
                                            .category(category)
                                            .temperature(temperature)
                                            .windSpeed(windSpeed)
                                            .windMax(windMax)
                                            .windDirection(windDirection)
                                            .rainRate(rainRate)
                                            .rainDaily(rainDaily)
                                            .pressure(pressure)
                                            .humidity(humidity)
                                            .uvRadiationIndex(uvRadiationIndex)
                                            .missing(missing)
                                        .build()
                                    );
                                    newRecords++;
                                }
                                else {
                                    if (entity.getTemperature() == temperature
                                            || entity.getWindSpeed() == windSpeed
                                            || entity.getWindMax() == windMax
                                            || entity.getWindDirection().equals(windDirection)
                                            || entity.getRainRate() == rainRate
                                            || entity.getRainDaily() == rainDaily
                                            || entity.getPressure() == pressure
                                            || entity.getHumidity() == humidity
                                            || entity.getUvRadiationIndex() == uvRadiationIndex) { 
                                        log.trace("Ignore Duplicate : {} - {} - {}", station, date, category);
                                        duplicates++;
                                    }
                                    else {
                                        if (!csvName.contains("api-weather-underground")) { // Don't log for weather underground files
                                            logBuilder.append("Inconsistent Duplicate!").append(System.lineSeparator());
                                            logBuilder.append(MessageFormatter.format("   Entity : {}", entity).getMessage()).append(System.lineSeparator());
                                            logBuilder.append(MessageFormatter.format("   Record : {}", record).getMessage()).append(System.lineSeparator());   
                                        }
                                        warnings++;
                                    }
                                }
                            }
                        }
                    }
                    catch (NumberFormatException ex) {
                        if (KNOWN_BAD_FILES.contains(csvName)) {
                            log.debug("Could not process record due to (known) number format error.");
                            log.debug("   CSV File : {}", csvName);
                            log.trace("   Headers  : {}", Arrays.toString(headers));
                            log.debug("   Record   : {}", record.toString());
                            log.debug("   Error    : {}", ex.getMessage());
                        }
                        else {
                            log.warn("Could not process record due to number format error.");
                            log.warn("   CSV File : {}", csvName);
                            log.trace("   Headers  : {}", Arrays.toString(headers));
                            log.warn("   Record   : {}", record.toString());
                            log.warn("   Error    : {}", ex.getMessage());
                        }
                        log.trace(ex.getMessage(), ex);
                        warnings++;
                    }
                    catch (IllegalArgumentException | OptimisticLockingFailureException | IllegalStateException ex) {
                        log.error("Could not process record!");
                        log.error("   CSV File : {}", csvName);
                        log.error("   Headers  : {}", Arrays.toString(headers));
                        log.error("   Record   : {}", record.toString());
                        log.error(ex.getMessage(), ex);
                        errors++;
                    }
                }
            }
            else {
                logBuilder.append("   > Delaying fine scale file until all summary files have been processed...").append(System.lineSeparator());
                return false;
            }
        }
        catch (IOException ex) {
            log.error(ex.getMessage(), ex);
            errors++;
        }
        logBuilder.append(MessageFormatter.format("   New Records : {}", newRecords).getMessage()).append(System.lineSeparator());
        logBuilder.append(MessageFormatter.format("   Duplicates  : {}", duplicates).getMessage()).append(System.lineSeparator());
        logBuilder.append(MessageFormatter.format("   Warnings    : {}", warnings).getMessage()).append(System.lineSeparator());
        logBuilder.append(MessageFormatter.format("   Errors      : {}", errors).getMessage()).append(System.lineSeparator());
        log.info(logBuilder.toString());
        WeatherCsvScheduler.markFileAsProcessed(csvName);
        return true;
    }

}
