package mywild.wildweather.domain.weather.schedulers.api;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public abstract class AbstractFetcher {

    protected abstract List<FetchedWeatherRecord> fetchRecords(
        String station, LocalDate apiStarDate, LocalDate apiEndDate, List<String> badDays);

    protected abstract void apiLimitCheck() throws InterruptedException;

}
