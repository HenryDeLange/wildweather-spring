package mywild.wildweather.domain.weather.schedulers.api.weatherunderground;

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
public class WeatherUndergroundScheduler extends AbstractScheduler {

    private static final String NAME = "Weather Underground";

    public static final String CSV_PREFIX = "api-weather-underground";

    private static final int STOP_AT_EMPTY_RESPONSES = 24; // Months without data

    public WeatherUndergroundScheduler(WeatherUndergroundFetcher api) {
        super(NAME, CSV_PREFIX, api);
        setStopAtEmptyResponses(STOP_AT_EMPTY_RESPONSES);
    }

    // Run at 2AM
    @Scheduled(cron = "0 0 2 * * *")
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
            // Start of the current month
            return LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
        }
        return currentStartDate.minusMonths(1);
    }

    @Override
    protected LocalDate nextApiEndDate(LocalDate currentStartDate) {
        if (currentStartDate == null) {
            // Yesterday midnight
            return LocalDate.now(ZoneOffset.UTC).minusDays(1);
        }
        return currentStartDate.plusMonths(1).minusDays(1);
    }

    @Override
    protected LocalDate csvEndDate(LocalDate currentEndDate) {
        // End of the current month
        return currentEndDate.with(TemporalAdjusters.lastDayOfMonth());
    }

    @Override
    public boolean shouldFetchMoreRecords(LocalDate mostRecentDatabaseDate, LocalDate apiStarDate) {
        if (!mostRecentDatabaseDate.withDayOfMonth(1).minusMonths(1).isBefore(apiStarDate)
            || !mostRecentDatabaseDate.withDayOfMonth(1).minusMonths(1).equals(apiStarDate)) {
            return false;
        }
        return true;
    }

}
