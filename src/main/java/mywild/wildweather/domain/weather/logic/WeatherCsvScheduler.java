package mywild.wildweather.domain.weather.logic;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import mywild.wildweather.domain.weather.data.WeatherCategory;
import mywild.wildweather.domain.weather.data.WeatherEntity;
import mywild.wildweather.domain.weather.data.WeatherRepository;

@Slf4j
@Service
public class WeatherCsvScheduler {

    private final int SCHEDULE_DELAY = 5 * 1000; // 5 seconds
    private final int SCHEDULE_RATE = 60 * 60 * 1000; // 1 hour
    private final int EXPECTED_RECORDS_PER_DAY = 24 * (60 / 5); // Every 5 minutes

    private final Object lock = new Object();

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private final List<String> processedCsvFiles = new ArrayList<>();

    @Value("${mywild.csv.folder}")
    private String csvRootFolder;

    @Autowired
    private WeatherRepository repo;
    
    @Scheduled(initialDelay = SCHEDULE_DELAY, fixedRate = SCHEDULE_RATE)
    void scheduledCsvFilesProcessing() {
        processCsvFiles();
    }

    public void resetProcessedCsvFiles() {
        repo.deleteAll();
        processedCsvFiles.clear();
    }

    @Async
    public void processCsvFiles() {
        if (!isRunning.compareAndSet(false, true)) {
            log.warn("Already busy processing files... The new request will be ignored.");
            return;
        }
        try (Stream<Path> paths = Files.walk(Paths.get(csvRootFolder))) {
            log.info("**************************");
            log.info("Looking for CSV files in : {}", csvRootFolder);
            log.info("**************************");
            List<Path> fineScaleCsvFiles = processAllSummaryFiles(paths);
            processAllFineScaleFiles(fineScaleCsvFiles);
        }
        catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        finally {
            log.info("****************************");
            log.info("Processed all CSV files in : {}", csvRootFolder);
            log.info("****************************");
            isRunning.set(false);
        }
    }

    private List<Path> processAllSummaryFiles(Stream<Path> paths) throws InterruptedException {
        List<Path> csvFiles = paths
            .filter(Files::isRegularFile)
            .filter(path -> path.toString().toLowerCase().endsWith(".csv"))
            .filter(path -> !processedCsvFiles.contains(getCsvName(path)))
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
            new CsvThreadFactory("s-csv-"));
        executor.invokeAll(tasks);
        executor.shutdown();
        return fineScaleCsvFiles;
    }

    private boolean processSummaryFile(Path csvFile) {
        StringBuilder logBuilder = new StringBuilder();
        var csvName = getCsvName(csvFile);
        logBuilder.append("----------------").append(System.lineSeparator());
        logBuilder.append(MessageFormatter.format("Processing File : {}", csvName).getMessage()).append(System.lineSeparator());
        var newRecords = 0;
        var duplicates = 0;
        var warnings = 0;
        var errors = 0;
        try (var reader = Files.newBufferedReader(csvFile)) {
            String[] headers = getHeaders(reader);
            var isSummaryCsv = headers[0].equals("COL0");
            if (isSummaryCsv) {
                List<CSVRecord> records = getRecords(reader, headers);
                for (CSVRecord record : records) {
                    try {
                        var categoryRecord = record.get("COL0");
                        if (!categoryRecord.contains("Datetime")) {
                            var category = WeatherCategory.valueOf(categoryRecord.substring(0, 1));
                            var date = LocalDate.parse(record.get("Date"));
                            var station = csvFile.getParent().getFileName().toString();
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
                            synchronized (lock) {
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
                                            .missing(0)
                                        .build()
                                    );
                                    newRecords++;
                                }
                                else {
                                    if (entity.getTemperature() == temperature
                                            || entity.getWindSpeed() == windSpeed
                                            || entity.getWindMax() == windMax
                                            || entity.getWindDirection() == windDirection
                                            || entity.getRainRate() == rainRate
                                            || entity.getRainDaily() == rainDaily
                                            || entity.getPressure() == pressure
                                            || entity.getHumidity() == humidity
                                            || entity.getUvRadiationIndex() == uvRadiationIndex) { 
                                        log.trace("Ignore Duplicate : {} - {} - {}", station, date, category);
                                        duplicates++;
                                    }
                                    else {
                                        logBuilder.append("Inconsistent Duplicate!").append(System.lineSeparator());
                                        logBuilder.append(MessageFormatter.format("   Entity : {}", entity).getMessage()).append(System.lineSeparator());
                                        logBuilder.append(MessageFormatter.format("   Record : {}", record).getMessage()).append(System.lineSeparator());
                                        warnings++;
                                    }
                                }
                            }
                        }
                    }
                    catch (NumberFormatException ex) {
                        log.trace("Could not process record due to number format error.");
                        log.trace("   CSV File : {}", csvName);
                        log.trace("   Headers  : {}", Arrays.toString(headers));
                        log.trace("   Record   : {}", record.toString());
                        log.trace(ex.getMessage());
                        log.trace(ex.getMessage(), ex);
                        warnings++;
                    }
                    catch (Exception ex) {
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
        catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            errors++;
        }
        logBuilder.append(MessageFormatter.format("   New Records : {}", newRecords).getMessage()).append(System.lineSeparator());
        logBuilder.append(MessageFormatter.format("   Duplicates  : {}", duplicates).getMessage()).append(System.lineSeparator());
        logBuilder.append(MessageFormatter.format("   Warnings    : {}", warnings).getMessage()).append(System.lineSeparator());
        logBuilder.append(MessageFormatter.format("   Errors      : {}", errors).getMessage()).append(System.lineSeparator());
        log.info(logBuilder.toString());
        processedCsvFiles.add(csvName);
        return true;
    }

    private void processAllFineScaleFiles(List<Path> csvFiles) throws InterruptedException {
        Set<String> detectDuplicateRecords = ConcurrentHashMap.newKeySet();
        ConcurrentMap<RecordsPerDateKey, Integer> recordsPerDate = new ConcurrentHashMap<>();
        List<Callable<Void>> tasks = new ArrayList<>();
        for (var csvFile : csvFiles) {
            tasks.add(() -> {
                var csvName = getCsvName(csvFile);
                var goodRecords = 0;
                var duplicates = 0;
                var gapRecords = 0;
                var errors = 0;
                try (var reader = Files.newBufferedReader(csvFile)) {
                    StringBuilder logBuilder = new StringBuilder();
                    logBuilder.append("----------------").append(System.lineSeparator());
                    logBuilder.append(MessageFormatter.format("Processing Delayed File : {}", csvName).getMessage()).append(System.lineSeparator());
                    try {
                        String[] headers = getHeaders(reader);
                        ZonedDateTime prevDateTime = null;
                        // TODO: Maybe just use normal file reading, line by line (only first 25 chars are needed), might be faster / less memory?
                        for (CSVRecord record : getRecords(reader, headers)) {
                            var dateTime = ZonedDateTime.parse(record.get("Date"));
                            var date = dateTime.toLocalDate();
                            var station = csvFile.getParent().getFileName().toString();
                            var duplicateKey = station + "_" + record.get("Date");
                            if (detectDuplicateRecords.add(duplicateKey)) {
                                recordsPerDate.compute(new RecordsPerDateKey(station, date), (_, v) -> v == null ? 1 : v + 1);
                                // The CSV file's dates should be in descending order (every 5 mins), thus it is possible to easily detect gaps
                                if (prevDateTime == null || dateTime.plusMinutes(9).isAfter(prevDateTime)) {
                                    goodRecords++;
                                }
                                else {
                                    log.trace("Large time gap : Prev {} vs Current {}", prevDateTime, dateTime);
                                    gapRecords++;
                                }
                            }
                            else {
                                log.trace("Duplicate: {}", duplicateKey);
                                duplicates++;
                            }
                            prevDateTime = dateTime;
                        }
                    }
                    catch (Exception ex) {
                        log.error(ex.getMessage(), ex);
                        errors++;
                    }
                    logBuilder.append(MessageFormatter.format("   Good Records : {}", goodRecords).getMessage()).append(System.lineSeparator());
                    logBuilder.append(MessageFormatter.format("   Gap Records  : {}", gapRecords).getMessage()).append(System.lineSeparator());
                    logBuilder.append(MessageFormatter.format("   Duplicates   : {}", duplicates).getMessage()).append(System.lineSeparator());
                    logBuilder.append(MessageFormatter.format("   Errors       : {}", errors).getMessage()).append(System.lineSeparator());
                    log.info(logBuilder.toString());
                }
                processedCsvFiles.add(csvName);
                return null;
            });
        }
        ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(), 
            new CsvThreadFactory("f-csv-"));
        executor.invokeAll(tasks);
        executor.shutdown();
        // Calculate missing percentage
        if (!recordsPerDate.entrySet().isEmpty()) {
            log.info("----------------");
            log.info("Updating database entities to indicate percentage of missing records per day...");
            var updated = 0;
            var missing = 0;
            for (var entry : recordsPerDate.entrySet()) {
                try {
                    var entities = repo.findAllByDateAndStationOrderByDateAscCategoryAsc(entry.getKey().date, entry.getKey().station);
                    for (var temp : entities) {
                        if (temp.getDate().equals(entry.getKey().date)) {
                            if (entry.getValue() < EXPECTED_RECORDS_PER_DAY) {
                                temp.setMissing(Math.round((EXPECTED_RECORDS_PER_DAY - entry.getValue()) / (double) EXPECTED_RECORDS_PER_DAY * 100.0));
                                missing++;
                            }
                            else if (entry.getValue() > EXPECTED_RECORDS_PER_DAY) {
                                log.warn("More days counted ({}) than expected ({}) for : {} on {}", 
                                    entry.getValue(), EXPECTED_RECORDS_PER_DAY, entry.getKey().station, entry.getKey().date);
                            }
                        }
                    }
                    repo.saveAll(entities);
                    updated = updated + entities.size();
                }
                catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
                }
            }
            log.info("   Days Updated              : {}", updated);
            log.info("   Days with Missing records : {}", missing);
        }
        // Insert missing days
        log.info("----------------");
        log.info("Updating database entities to insert completely missing days...");
        for (var station : repo.findStations()) {
            log.info("Processing station : {}", station);
            var newDays = 0;
            var entities = repo.findAllByStationOrderByDateAscCategoryAsc(station);
            LocalDate prevDate = entities.get(0).getDate();
            for (var weather : entities) {
                try {
                    if (prevDate.plusDays(1).isBefore(weather.getDate())) {
                        for (var i = 1; i < ChronoUnit.DAYS.between(prevDate, weather.getDate()); i++) {
                            for (var category : WeatherCategory.values()) {
                                repo.save(
                                    WeatherEntity.builder()
                                        .station(station)
                                        .date(prevDate.plusDays(i))
                                        .category(category)
                                        .windDirection("")
                                        .missing(100)
                                    .build()
                                );
                            }
                            newDays++;
                        }
                    }
                }
                catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
                }
                prevDate = weather.getDate();
            }
            log.info("   Missing Days Inserted  : {}", newDays);
        }
    }

    private String[] getHeaders(BufferedReader reader) throws IOException {
        String headerLine = reader.readLine();
        String[] headers = headerLine.split(",");
        for (int i = 0; i < headers.length; i++) {
            var header = headers[i].replace("\"", "");
            if (header == null || header.isBlank()) {
                // Replace empty headers with the column index instead
                header = "COL" + i;
            }
            if (header.contains("(")) {
                header = header.substring(0, header.indexOf("(")).trim();
            }
            if (header.equals("Hourly Rain")) {
                header = "Rain Rate";
            }
            headers[i] = header;
        }
        log.trace("Headers: {}", Arrays.toString(headers));
        return headers;
    }

    private List<CSVRecord> getRecords(BufferedReader reader, String[] headers) throws IOException {
        List<CSVRecord> records = CSVFormat.Builder
            .create()
            .setHeader(headers)
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .get()
            .parse(reader).getRecords();
        log.trace("Records: {}", records.size());
        return records;
    }

    private String getCsvName(Path path) {
        return path.getParent().getFileName() + " -> " + path.getFileName();
    }

    private record RecordsPerDateKey(
        String station,
        LocalDate date
    ) {
       // Record automatically generates: equals, hashCode and toString
    };

    private class CsvThreadFactory implements ThreadFactory {
        
        private final AtomicInteger counter = new AtomicInteger(1);

        private final String name;

        CsvThreadFactory(String name) {
            this.name = name;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, String.format(name + "%02d", counter.getAndIncrement()));
        }

    }

}

