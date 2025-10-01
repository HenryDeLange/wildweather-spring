package mywild.wildweather.domain.weather.web;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import mywild.wildweather.domain.weather.data.WeatherCategory;

@ToString(callSuper = true)
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WeatherDto {

    @NotNull
    @NotBlank
    private String station;

    @NotNull
    private LocalDate date;

    @NotNull
    private WeatherCategory category;

    @NotNull
    private double temperature;

    @NotNull
    private double windSpeed;

    @NotNull
    private double windMax;

    @NotNull
    private String windDirection;

    @NotNull
    private double rainRate;

    @NotNull
    private double rainDaily;

    @NotNull
    private double pressure;

    @NotNull
    private double humidity;

    @NotNull
    private double uvRadiationIndex;

    @NotNull
    private double missing;

}
