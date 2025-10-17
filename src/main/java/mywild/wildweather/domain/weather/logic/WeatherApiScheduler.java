package mywild.wildweather.domain.weather.logic;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import mywild.ambientweather.openapi.client.api.AmbientWeatherApi;

@Slf4j
@Service
public class WeatherApiScheduler {

    private static final int SCHEDULE_DELAY = 5 * 60 * 1000; // 5 minutes
    private static final int SCHEDULE_RATE = 6 * 60 * 60 * 1000; // 6 hours

    private static final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Value("${mywild.csv.folder}")
    private String csvRootFolder;

    @Autowired
    private AmbientWeatherApi api;

    @Scheduled(initialDelay = 1500 /*SCHEDULE_DELAY*/, fixedRate = SCHEDULE_RATE)
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
        try {
            log.info("*************************");
            log.info("Looking for API data");
            log.info("*************************");
            // TODO: Download up to date data from the Ambient Weather API and store it in a CSV file myself
            System.out.println(api.devicesGet());
        }
        catch (Exception ex) {
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

