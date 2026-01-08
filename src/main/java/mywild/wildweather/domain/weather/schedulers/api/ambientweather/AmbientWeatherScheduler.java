package mywild.wildweather.domain.weather.schedulers.api.ambientweather;

import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import mywild.wildweather.domain.weather.schedulers.api.AbstractScheduler;

@Slf4j
@Service
public class AmbientWeatherScheduler extends AbstractScheduler {

    private static final String NAME = "Ambient Weather";

    private static final String CSV_PREFIX = "api-ambient-weather";

    public AmbientWeatherScheduler(AmbientWeatherFetcher api) {
        super(NAME, CSV_PREFIX, api);
    }

    // Run at 1AM
    @Scheduled(cron = "0 0 1 * * *")
    void scheduledApiProcessing() {
        processApiData(false);
    }

    @Async
    public void processApiData(boolean fetchAllData) {
        super.processApiData(fetchAllData);
    }

    @Override
    protected LocalDate nextApiStartDate(LocalDate currentStartDate) {
        // Make startDate same as endDate (the API only uses the end date)
        if (currentStartDate == null) {
            // Yesterday midnight
            return LocalDate.now(ZoneOffset.UTC).minusDays(1);
        }
        return currentStartDate.minusDays(1);
    }

    @Override
    protected LocalDate nextApiEndDate(LocalDate currentStartDate) {
        if (currentStartDate == null) {
            // Yesterday midnight
            return LocalDate.now(ZoneOffset.UTC).minusDays(1);
        }
        return currentStartDate.minusDays(1);
    }

    @Override
    protected LocalDate csvEndDate(LocalDate currentEndDate) {
        // Same day
        return currentEndDate;
    }

    @Override
    public boolean shouldFetchMoreRecords(LocalDate mostRecentDatabaseDate, LocalDate apiStarDate) {
        if (!mostRecentDatabaseDate.isEqual(apiStarDate)
            || !mostRecentDatabaseDate.isBefore(apiStarDate)) {
            return false;
        }
        return true;
    }

}
