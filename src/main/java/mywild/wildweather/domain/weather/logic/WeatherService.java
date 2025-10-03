package mywild.wildweather.domain.weather.logic;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import mywild.wildweather.domain.weather.data.WeatherEntity;
import mywild.wildweather.domain.weather.data.WeatherRepository;
import mywild.wildweather.domain.weather.web.WeatherDataDto;

@Slf4j
@Validated
@Service
public class WeatherService {

    private static final Map<String, Function<WeatherEntity, Double>> FIELD_EXTRACTORS = Map.of(
        "tmp", WeatherEntity::getTemperature,
        "wSp", WeatherEntity::getWindSpeed,
        "wMx", WeatherEntity::getWindMax,
        "wDr", entity -> mapDirection(entity.getWindDirection()),
        "rRt", WeatherEntity::getRainRate,
        "rDy", WeatherEntity::getRainDaily,
        "prs", WeatherEntity::getPressure,
        "hmd", WeatherEntity::getHumidity,
        "uvI", WeatherEntity::getUvRadiationIndex,
        "mis", WeatherEntity::getMissing
    );

    private static double mapDirection(String direction) {
        if (direction == null || direction.isBlank())
            return 0.0;
        return Double.parseDouble(direction
            .replace('N', '1')
            .replace('E', '2')
            .replace('S', '3')
            .replace('W', '4'));
    }

    @Autowired
    private WeatherRepository repo;

    public @Valid WeatherDataDto getWeather(
            String station, LocalDate startDate, LocalDate endDate, 
            Integer startMonth, Integer endMonth) {
        return mapEntitiesToDto(repo.searchWeather(station, startDate, endDate, startMonth, endMonth));
    }

    public @Valid WeatherDataDto getStationWeatherOnDay(
            @Valid LocalDate date, @NotBlank String station) {
        return mapEntitiesToDto(repo.findAllByDateAndStationOrderByDateAscCategoryAsc(date, station));
    }

    private WeatherDataDto mapEntitiesToDto(List<WeatherEntity> entities) {
        var weatherData = new WeatherDataDto();
        String prevStation = null;
        LocalDate prevDate = null;
        for (var entity : entities) {
            var station = entity.getStation();
            var date = entity.getDate();
            var category = entity.getCategory();
            if (prevStation == null || !prevStation.equals(station)) {
                prevDate = null;
            }
            if (prevDate != null && prevDate.plusDays(1).isBefore(date)) {
                log.warn("large date gap: {} vs {}", prevDate, date);
            }
            FIELD_EXTRACTORS.forEach((fieldName, extractor) -> {
                var value = extractor.apply(entity);
                weatherData.getWeather().computeIfAbsent(station, _ -> new LinkedHashMap<>())
                    .computeIfAbsent(date.getYear(), _ -> new LinkedHashMap<>())
                        .computeIfAbsent(date, _ -> new LinkedHashMap<>())
                            .computeIfAbsent(fieldName, _ -> new LinkedHashMap<>())
                                .computeIfAbsent(category, _ -> value);
            });
            prevDate = date;
        }
        return weatherData;
    }

}
