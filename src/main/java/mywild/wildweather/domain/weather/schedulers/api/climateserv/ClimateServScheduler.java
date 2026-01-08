package mywild.wildweather.domain.weather.schedulers.api.climateserv;

import java.time.LocalDate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import mywild.wildweather.domain.weather.schedulers.api.AbstractScheduler;

@Slf4j
@Service
public class ClimateServScheduler extends AbstractScheduler {

    private static final String NAME = "ClimateServ";

    public static final String CSV_PREFIX = "api-climateserv-chirps";

    private static final LocalDate START_YEAR = LocalDate.of(1980, 1, 1);

    public ClimateServScheduler(ClimateServFetcher api) {
        super(NAME, CSV_PREFIX, api);
    }

    // Run at 4AM
    @Scheduled(cron = "0 0 4 * * *")
    void scheduledApiProcessing() {
        processApiData(false);
    }

    @Async
    public void processApiData(boolean fetchAllData) {
        // TODO: Implement this
        // super.processApiData(fetchAllData);
    }

    @Override
    protected LocalDate nextApiStartDate(LocalDate currentStartDate) {
        // TODO: Implement this
        return null;
    }

    @Override
    protected LocalDate nextApiEndDate(LocalDate currentStartDate) {
        // TODO: Implement this
        return null;
    }

    @Override
    protected LocalDate csvEndDate(LocalDate currentEndDate) {
        // TODO: Implement this
        return null;
    }

    @Override
    public boolean shouldFetchMoreRecords(LocalDate mostRecentDatabaseDate, LocalDate apiStarDate) {
        // TODO: Implement this
        return false;
    }

}
