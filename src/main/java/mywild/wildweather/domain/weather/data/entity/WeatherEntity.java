package mywild.wildweather.domain.weather.data.entity;

import java.time.LocalDate;
import org.springframework.data.relational.core.mapping.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import mywild.wildweather.framework.data.BaseEntity;

@ToString(callSuper = true)
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("weather")
public class WeatherEntity extends BaseEntity {

    @NotNull
    @NotBlank
    private String station;

    @NotNull
    private LocalDate date;

    @NotNull
    private WeatherCategory category;

    private double temperature;

    private double windSpeed;

    private double windMax;

    @NotNull
    private String windDirection;

    private double rainRate;

    private double rainDaily;

    private double pressure;

    private double humidity;

    private double uvRadiationIndex;

    private double missing;

}
