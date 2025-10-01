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
import java.util.stream.Stream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import mywild.wildweather.domain.weather.data.WeatherCategory;
import mywild.wildweather.domain.weather.data.WeatherEntity;
import mywild.wildweather.domain.weather.data.WeatherRepository;

@Slf4j
@Service
public class WeatherScheduler {

    private final int DELAY = 5 * 1000; // 5 seconds
    private final int RATE = 60 * 60 * 1000; // 1 hour
    private final int EXPECTED_RECORDS_PER_DAY = 24 * (60 / 5); // Every 5 minutes

    @Value("${mywild.csv.folder}")
    private String csvRootFolder;

    @Autowired
    private WeatherRepository repo;

    private List<String> processedCsvFiles = new ArrayList<>();
    
    @Scheduled(initialDelay = DELAY, fixedRate = RATE)
    public void processCsvFiles() {
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
                if (!processSummaryCsv(csvFile)) {
                    fineScaleCsvFiles.add(csvFile);
                }
            });
            processFineScaleFile(fineScaleCsvFiles);
        }
        catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        log.info("***************************");
        log.info("Processed all CSV files in: {}", csvRootFolder);
        log.info("***************************");
    }

    private boolean processSummaryCsv(Path csvFile) {
        var fileName = csvFile.getParent().getParent().relativize(csvFile).toString();
        try (var reader = Files.newBufferedReader(csvFile)) {
            log.info("----------------");
            log.info("Processing File: {}", fileName);
            
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
                            var entity = repo.findByDateAndStationAndCategory(date, station, category);
                            if (entity == null) {
                                repo.save(
                                    WeatherEntity.builder()
                                        .station(station)
                                        .date(date)
                                        .category(category)
                                        .temperature(Double.parseDouble(record.get("Outdoor Temperature")))
                                        .windSpeed(Double.parseDouble(record.get("Wind Speed")))
                                        .windMax(Double.parseDouble(record.get("Max Daily Gust")))
                                        .windDirection(record.get("Wind Direction"))
                                        .rainRate(Double.parseDouble(record.get("Rain Rate")))
                                        .rainDaily(Double.parseDouble(record.get("Daily Rain")))
                                        .pressure(Double.parseDouble(record.get("Relative Pressure")))
                                        .humidity(Double.parseDouble(record.get("Humidity")))
                                        .uvRadiationIndex(Double.parseDouble(record.get("Ultra-Violet Radiation Index")))
                                        .missing(0)
                                    .build()
                                );
                            }
                            else {
                                // TODO: else compare to make sure values match, otherwise log warning
                                log.trace("Duplicate: {} - {} - {}", station, date, category);
                            }
                        }
                    }
                    catch (Exception ex) {
                        log.error("Could not process record!");
                        log.error(Arrays.toString(headers));
                        log.error(record.toString());
                        log.error(ex.getMessage(), ex);
                    }
                }
            }
            else {
                log.info("Delay fine scale file until all summary files have been processed: {}", fileName);
                return false;
            }
        }
        catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        // Mark as processed
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

