package mywild.wildweather.domain.weather.schedulers.api;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@ToString(callSuper = true)
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class FetchedWeatherRecord {

    private LocalDate date;

    private FetchedWeatherField temperature;

    private FetchedWeatherField windDirection;

    private FetchedWeatherField wind;
    
    private FetchedWeatherField gust;

    private FetchedWeatherField rainRate;

    private FetchedWeatherField rain;

    private FetchedWeatherField pressure;

    private FetchedWeatherField humidity;

    private FetchedWeatherField uvRadiation;

}
