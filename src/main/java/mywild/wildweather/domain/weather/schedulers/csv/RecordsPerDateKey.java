package mywild.wildweather.domain.weather.schedulers.csv;

import java.time.LocalDate;

public record RecordsPerDateKey(
    String station,
    LocalDate date
) {
    // Record automatically generates: equals, hashCode and toString
}
