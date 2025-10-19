package mywild.wildweather.domain.weather.schedulers.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
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

@Slf4j
@Service
public class WeatherApiScheduler {

    private static final int SCHEDULE_DELAY = 1 * 60 * 1000; // 5 minutes
    private static final int SCHEDULE_RATE = 1 * 60 * 60 * 1000; // 1 hours
    private static final int EXPECTED_RECORDS_PER_DAY = 24 * (60 / 5); // Every 5 minutes

    private static final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Value("${mywild.csv.folder}")
    private String csvRootFolder;

    @Autowired
    private AmbientWeatherApi api;

    @Autowired
    private WeatherRepository repo;

    @Scheduled(initialDelay = 10000 /*SCHEDULE_DELAY*/, fixedRate = SCHEDULE_RATE)
    void scheduledApiProcessing() {
        processApiData(false);
    }

    // TODO: Also have a once off, manually triggered task that will download all available data (I suspect the CSVs aren't 100% complete)
    @Async
    public void processApiData(boolean processAllAvailable) {
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
                    do {
                        log.info("   Fetching data for : {}", apiEndDate);
                        var data = api.getDeviceData(stationMacAddress, apiEndDate, null);
                        for (var dataRecord : data) {

                            // TODO: Process the dataRecord calculate daily A/H/L and convert to metric units

                            readRecords++;
                        }
                        apiEndDate = data.get(data.size() - 1).getDate();
                        Thread.sleep(Duration.ofSeconds(2));
                    }
                    while (mostRecentDatabaseDate.isEqual(apiEndDate.toLocalDate())
                        || mostRecentDatabaseDate.isBefore(apiEndDate.toLocalDate()));
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

}

