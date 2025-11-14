package mywild.wildweather.domain.weather.schedulers.csv;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
public class ProcessFullFiles {

    private static final int EXPECTED_RECORDS_PER_DAY = 24 * (60 / 5); // Every 5 minutes

    @Autowired
    private WeatherRepository repo;
    
    void processAllFineScaleFiles(List<Path> csvFiles) throws InterruptedException {
        Set<String> detectDuplicateRecords = ConcurrentHashMap.newKeySet();
        ConcurrentMap<RecordsPerDateKey, Integer> recordsPerDate = new ConcurrentHashMap<>();
        List<Callable<Void>> tasks = new ArrayList<>();
        for (var csvFile : csvFiles) {
            tasks.add(() -> {
                var csvName = CsvUtils.getCsvName(csvFile);
                var goodRecords = 0;
                var duplicates = 0;
                var gapRecords = 0;
                var errors = 0;
                try (var reader = Files.newBufferedReader(csvFile)) {
                    StringBuilder logBuilder = new StringBuilder();
                    logBuilder.append("----------------").append(System.lineSeparator());
                    logBuilder.append(MessageFormatter.format("Processing Delayed File : {}", csvName).getMessage()).append(System.lineSeparator());
                    try {
                        String[] headers = CsvUtils.getHeaders(reader);
                        ZonedDateTime prevDateTime = null;
                        // TODO: Maybe just use normal file reading, line by line (only first 25 chars are needed), might be faster / less memory?
                        for (CSVRecord record : CsvUtils.getRecords(reader, headers)) {
                            var dateTime = ZonedDateTime.parse(record.get("Date"));
                            var date = dateTime.toLocalDate();
                            var station = Utils.getStationName(csvFile);
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
                    catch (IOException ex) {
                        log.error(ex.getMessage(), ex);
                        errors++;
                    }
                    logBuilder.append(MessageFormatter.format("   Good Records : {}", goodRecords).getMessage()).append(System.lineSeparator());
                    logBuilder.append(MessageFormatter.format("   Gap Records  : {}", gapRecords).getMessage()).append(System.lineSeparator());
                    logBuilder.append(MessageFormatter.format("   Duplicates   : {}", duplicates).getMessage()).append(System.lineSeparator());
                    logBuilder.append(MessageFormatter.format("   Errors       : {}", errors).getMessage()).append(System.lineSeparator());
                    log.info(logBuilder.toString());
                }
                WeatherCsvScheduler.markFileAsProcessed(csvName);
                return null;
            });
        }
        ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(), 
            new SchedulerThreadFactory("f-csv-"));
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
                    var entities = repo.findAllByDateAndStationOrderByDateAscCategoryAsc(entry.getKey().date(), entry.getKey().station());
                    for (var temp : entities) {
                        if (temp.getDate().equals(entry.getKey().date())) {
                            if (entry.getValue() < EXPECTED_RECORDS_PER_DAY) {
                                temp.setMissing((double) Math.round((EXPECTED_RECORDS_PER_DAY - entry.getValue()) / (double) EXPECTED_RECORDS_PER_DAY * 100.0));
                                missing++;
                            }
                            else if (entry.getValue() > EXPECTED_RECORDS_PER_DAY) {
                                log.warn("More days counted ({}) than expected ({}) for : {} on {}", 
                                    entry.getValue(), EXPECTED_RECORDS_PER_DAY, entry.getKey().station(), entry.getKey().date());
                            }
                        }
                    }
                    repo.saveAll(entities);
                    updated = updated + entities.size();
                }
                catch (RuntimeException ex) {
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
                                        .missing(100.0)
                                    .build()
                                );
                            }
                            newDays++;
                        }
                    }
                }
                catch (IllegalArgumentException | OptimisticLockingFailureException ex) {
                    log.error(ex.getMessage(), ex);
                }
                prevDate = weather.getDate();
            }
            log.info("   Missing Days Inserted  : {}", newDays);
        }
    }

}
