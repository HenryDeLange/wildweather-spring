package mywild.wildweather.domain.weather.schedulers.csv;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import mywild.wildweather.domain.weather.data.WeatherRepository;
import mywild.wildweather.domain.weather.schedulers.api.CsvWriter;
import mywild.wildweather.domain.weather.schedulers.api.WeatherUndergroundApiScheduler;

@Slf4j
@Service
public class WeatherCsvScheduler {

    private static final int SCHEDULE_DELAY = 2 * 1000; // 2 seconds
    private static final int SCHEDULE_RATE = 60 * 60 * 1000; // 1 hour

    private static final AtomicBoolean IS_RUNNING = new AtomicBoolean(false);

    private static final List<String> PROCESSED_CSV_FILES = new ArrayList<>();

    @Value("${mywild.csv.folder}")
    private String csvRootFolder;

    @Autowired
    private WeatherRepository repo;

    @Autowired
    private ProcessSummaryFiles processSummaryFiles;

    @Autowired
    private ProcessFullFiles processFullFiles;

    @EventListener(ApplicationReadyEvent.class)
    void startupCsvFilesProcessing() {
        processCsvFiles();
    }
    
    @Scheduled(cron = "0 0 4 * * *") // Run at 4AM
    void scheduledCsvFilesProcessing() {
        resetLatestWeatherUndergroundProcessedCsvFiles();
        processCsvFiles();
    }

    public boolean isRunning() {
        return IS_RUNNING.get();
    }

    public void resetLatestWeatherUndergroundProcessedCsvFiles() {
        log.info("Clearing latest two months of Weather Underground data...");
        var currentMonth = LocalDate.now().withDayOfMonth(1);
        PROCESSED_CSV_FILES.clear();
        PROCESSED_CSV_FILES.addAll(PROCESSED_CSV_FILES.stream()
            .filter(csv -> !csv.contains(WeatherUndergroundApiScheduler.WU_CSV_PREFIX)
                        || !csv.contains(currentMonth.format(CsvWriter.CSV_NAME_DATE_FORMAT))
                        && !csv.contains(currentMonth.minusMonths(1).format(CsvWriter.CSV_NAME_DATE_FORMAT)))
            .toList());
    }

    public void resetAllProcessedCsvFiles() {
        log.info("Clearing existing weather data...");
        repo.deleteAll();
        PROCESSED_CSV_FILES.clear();
    }

    @Async
    public void processCsvFiles() {
        if (!IS_RUNNING.compareAndSet(false, true)) {
            log.warn("Already busy processing csv files... The new request will be ignored.");
            return;
        }
        try (Stream<Path> paths = Files.walk(Paths.get(csvRootFolder))) {
            log.info("**************************");
            log.info("Looking for CSV files in : {}", csvRootFolder);
            log.info("**************************");
            List<Path> fineScaleCsvFiles = processSummaryFiles.processAllSummaryFiles(paths);
            processFullFiles.processAllFineScaleFiles(fineScaleCsvFiles);
        }
        catch (IOException | InterruptedException ex) {
            log.error(ex.getMessage(), ex);
        }
        finally {
            log.info("****************************");
            log.info("Processed all CSV files in : {}", csvRootFolder);
            log.info("****************************");
            IS_RUNNING.set(false);
        }
    }

    static void clearProcessedFiles() {
        PROCESSED_CSV_FILES.clear();
    }

    static boolean hasFileBeenProcessed(String fileName) {
        return PROCESSED_CSV_FILES.contains(fileName);
    }

    static void markFileAsProcessed(String fileName) {
        PROCESSED_CSV_FILES.add(fileName);
    }

}
