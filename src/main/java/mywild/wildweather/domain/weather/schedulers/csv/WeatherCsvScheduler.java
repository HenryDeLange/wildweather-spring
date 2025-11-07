package mywild.wildweather.domain.weather.schedulers.csv;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import mywild.wildweather.domain.weather.data.WeatherRepository;

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
    
    @Scheduled(initialDelay = SCHEDULE_DELAY, fixedRate = SCHEDULE_RATE)
    void scheduledCsvFilesProcessing() {
        processCsvFiles();
    }

    public boolean isRunning() {
        return IS_RUNNING.get();
    }

    public void resetProcessedCsvFiles() {
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
