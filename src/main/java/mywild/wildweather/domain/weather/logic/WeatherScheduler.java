package mywild.wildweather.domain.weather.logic;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
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
public class WeatherScheduler {

    private final int SCHEDULE_DELAY = 5 * 1000; // 5 seconds
    private final int SCHEDULE_RATE = 60 * 60 * 1000; // 1 hour
    private final int EXPECTED_RECORDS_PER_DAY = 24 * (60 / 5); // Every 5 minutes

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
        processedCsvFiles.clear();
    }

    @Async
    public void processCsvFiles() {
        if (!isRunning.compareAndSet(false, true)) {
            log.warn("Already busy processing files... The new request will be ignored.");
            return;
        }
        try (Stream<Path> paths = Files.walk(Paths.get(csvRootFolder))) {
            log.info("*************************");
            log.info("Looking for CSV files in: {}", csvRootFolder);
            log.info("*************************");
            List<Path> csvFiles = paths
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().toLowerCase().endsWith(".csv"))
                .filter(path -> !processedCsvFiles.contains(getCsvName(path)))
                .toList();
            List<Path> fineScaleCsvFiles = new ArrayList<>();
            csvFiles.forEach(csvFile -> {
                var isSummaryFile = processSummaryCsv(csvFile);
                if (!isSummaryFile) {
                    fineScaleCsvFiles.add(csvFile);
                }
            });
            processFineScaleFile(fineScaleCsvFiles);
        }
        catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        finally {
            log.info("***************************");
            log.info("Processed all CSV files in: {}", csvRootFolder);
            log.info("***************************");
            isRunning.set(false);
        }
    }

    private boolean processSummaryCsv(Path csvFile) {
        var fileName = csvFile.getParent().getParent().relativize(csvFile).toString();
        log.info("----------------");
        log.info("Processing File: {}", fileName);
        var newRecords = 0;
        var duplicates = 0;
        var warnings = 0;
        var errors = 0;
        try (var reader = Files.newBufferedReader(csvFile)) {
            String[] headers = getHeaders(reader);
            var isSummaryCsv = headers[0].equals("col0");
            if (isSummaryCsv) {
                List<CSVRecord> records = getRecords(reader, headers);
                for (CSVRecord record : records) {
                    try {
                        var categoryRecord = record.get("col0");
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
                                    log.trace("Ignore Duplicate: {} - {} - {}", station, date, category);
                                    duplicates++;
                                }
                                else {
                                    log.warn("Inconsistent Duplicate!");
                                    log.warn("   Entity: {}", entity);
                                    log.warn("   Record: {}", record);
                                    warnings++;
                                }
                            }
                        }
                    }
                    catch (NumberFormatException ex) {
                        log.trace("Could not process record due to number format error.");
                        log.trace("   CSV File : {}", fileName);
                        log.trace("   Headers  : {}", Arrays.toString(headers));
                        log.trace("   Record   : {}", record.toString());
                        log.trace(ex.getMessage());
                        log.trace(ex.getMessage(), ex);
                        warnings++;
                    }
                    catch (Exception ex) {
                        log.error("Could not process record!");
                        log.error("   CSV File : {}", fileName);
                        log.error("   Headers  : {}", Arrays.toString(headers));
                        log.error("   Record   : {}", record.toString());
                        log.error(ex.getMessage(), ex);
                        errors++;
                    }
                }
            }
            else {
                log.info(" > Delaying fine scale file until all summary files have been processed...");
                return false;
            }
        }
        catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            errors++;
        }
        log.info("   New Records: {}", newRecords);
        log.info("   Duplicates : {}", duplicates);
        log.info("   Warnings   : {}", warnings);
        log.info("   Errors     : {}", errors);
        processedCsvFiles.add(fileName);
        return true;
    }

    private void processFineScaleFile(List<Path> csvFiles) {
        List<String> detectDuplicateRecords = new ArrayList<>();
        Map<RecordsPerDateKey, Integer> recordsPerDate = new HashMap<>();
        for (var csvFile : csvFiles) {
            var fileName = getCsvName(csvFile);
            try (var reader = Files.newBufferedReader(csvFile)) {
                log.info("----------------");
                log.info("Processing Delayed Fine Scale File: {}", fileName);

                String[] headers = getHeaders(reader);
                
                ZonedDateTime prevDateTime = null;

                List<CSVRecord> records = getRecords(reader, headers);
                for (CSVRecord record : records) {
                    var dateTime = ZonedDateTime.parse(record.get("Date"));
                    var date = dateTime.toLocalDate();
                    var station = csvFile.getParent().getFileName().toString();
                    var duplicateKey = station + "_" + record.get("Date");
                    if (!detectDuplicateRecords.contains(duplicateKey)) {
                        // The CSV file's dates should be in descending order (every 5 mins), thus it is possible to easily detect gaps
                        if (prevDateTime == null || dateTime.plusMinutes(9).isAfter(prevDateTime)) {
                            recordsPerDate.compute(new RecordsPerDateKey(station, date), (k, v) -> v == null ? 1 : v + 1);
                            detectDuplicateRecords.add(duplicateKey);
                            log.trace("Date Time Counted: Prev {} vs Current {}", prevDateTime, dateTime);
                        }
                        else {
                            log.trace("Date Time Gap: Prev {} vs Current {}", prevDateTime, dateTime);
                        }
                    }
                    else {
                        // TODO: else compare to make sure values match, otherwise log warning
                        log.trace("Duplicate: {} - {}", station, date);
                    }
                    prevDateTime = dateTime;
                }
            }
            catch (Exception ex) {
                log.error(ex.getMessage(), ex);
            }
            // Mark as processed
            processedCsvFiles.add(fileName);
        }
        log.info("----------------");
        log.info("Updating database records to flag dates with missing CSV entries...");
        for (var entry : recordsPerDate.entrySet()) {
            try {
                var entities = repo.findAllByDateAndStation(entry.getKey().date, entry.getKey().station);
                entities.forEach(temp -> {
                    if (temp.getDate().equals(entry.getKey().date)) {
                        temp.setMissing(EXPECTED_RECORDS_PER_DAY - entry.getValue());
                    }
                });
                repo.saveAll(entities);
            }
            catch (Exception ex) {
                log.error(ex.getMessage(), ex);
            }
        }
    }

    private String[] getHeaders(BufferedReader reader) throws IOException {
        String headerLine = reader.readLine();
        String[] headers = headerLine.split(",");
        for (int i = 0; i < headers.length; i++) {
            var header = headers[i].replace("\"", "");
            if (header == null || header.isBlank()) {
                // Replace empty headers with the column index instead
                header = "col" + i;
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
        return path.getParent().getParent().relativize(path).toString();
    }

    private record RecordsPerDateKey(
        String station,
        LocalDate date
    ) {
       // Record automatically generates: equals, hashCode and toString
    };

}

