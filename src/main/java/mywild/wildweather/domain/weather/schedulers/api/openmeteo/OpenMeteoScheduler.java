package mywild.wildweather.domain.weather.schedulers.api.openmeteo;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import mywild.wildweather.domain.weather.schedulers.api.AbstractScheduler;

@Slf4j
@Service
public class OpenMeteoScheduler extends AbstractScheduler {

    private static final String NAME = "Open-Meteo";

    public static final String CSV_PREFIX = "api-openmeteo";

    private static final LocalDate START_YEAR = LocalDate.of(1940, 1, 1);

    public OpenMeteoScheduler(OpenMeteoFetcher api) {
        super(NAME, CSV_PREFIX, api);
    }

    // Run at 3AM
    @Scheduled(cron = "0 0 3 * * *")
    void scheduledApiProcessing() {
        processApiData(false);
    }

    @Async
    public void processApiData(boolean fetchAllData) {
        super.processApiData(fetchAllData);
    }

    @Override
    protected LocalDate nextApiStartDate(LocalDate currentStartDate) {
        if (currentStartDate == null) {
            // Start of the current year
            return LocalDate.now(ZoneOffset.UTC).withDayOfYear(1);
        }
        return currentStartDate.minusYears(1);
    }

    @Override
    protected LocalDate nextApiEndDate(LocalDate currentStartDate) {
        if (currentStartDate == null) {
            // Yesterday midnight
            return LocalDate.now(ZoneOffset.UTC).minusDays(1);
        }
        return currentStartDate.with(TemporalAdjusters.lastDayOfYear());
    }

    @Override
    protected LocalDate csvEndDate(LocalDate currentEndDate) {
        // End of the current year
        return currentEndDate.with(TemporalAdjusters.lastDayOfYear());
    }

    @Override
    public boolean shouldFetchMoreRecords(LocalDate mostRecentDatabaseDate, LocalDate apiStarDate) {
        if (apiStarDate.isBefore(START_YEAR)) {
            return false;
        }
        if (!mostRecentDatabaseDate.withDayOfYear(1).isBefore(apiStarDate)) {
            return false;
        }
        return true;
    }

}
