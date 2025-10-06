package mywild.wildweather.domain.weather.web;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
public class WeatherDataDto {

    @NotNull
    @Builder.Default
    private Map<
        String/*station*/,
            Map<Integer/*year*/,
                Map<String/*grouping*/,
                    Map<String/*field*/,
                        Map<WeatherCategory/*category*/,
                            Double/*value*/
                                >>>>> weather = new LinkedHashMap<>();

}
