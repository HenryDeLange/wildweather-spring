package mywild.wildweather.domain.weather.logic;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import mywild.wildweather.domain.weather.data.WeatherCategory;
import mywild.wildweather.domain.weather.data.WeatherEntity;
import mywild.wildweather.domain.weather.data.WeatherRepository;
import mywild.wildweather.domain.weather.web.WeatherDataDto;
import mywild.wildweather.domain.weather.web.WeatherGrouping;

@Slf4j
@Validated
@Service
public class WeatherService {

    private static final Map<String, Function<WeatherEntity, Double>> FIELD_EXTRACTORS;
    static {
        Map<String, Function<WeatherEntity, Double>> fieldMappings = new LinkedHashMap<>();
        fieldMappings.put("tmp", WeatherEntity::getTemperature);
        fieldMappings.put("wSp", WeatherEntity::getWindSpeed);
        fieldMappings.put("wMx", WeatherEntity::getWindMax);
        fieldMappings.put("wDr", e -> mapDirection(e.getWindDirection()));
        fieldMappings.put("rRt", WeatherEntity::getRainRate);
        fieldMappings.put("rDy", WeatherEntity::getRainDaily);
        fieldMappings.put("prs", WeatherEntity::getPressure);
        fieldMappings.put("hmd", WeatherEntity::getHumidity);
        fieldMappings.put("uvI", WeatherEntity::getUvRadiationIndex);
        fieldMappings.put("mis", WeatherEntity::getMissing);
        FIELD_EXTRACTORS = Collections.unmodifiableMap(fieldMappings);
    }

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
            WeatherCategory category,
            WeatherGrouping grouping,
            String station,
            LocalDate startDate, LocalDate endDate,
            Integer startMonth, Integer endMonth) {
        return mapEntitiesToDto(grouping, 
            repo.searchWeather(category, station, startDate, endDate, startMonth, endMonth));
    }

    private WeatherDataDto mapEntitiesToDto(WeatherGrouping grouping, List<WeatherEntity> entities) {
        var weatherData = new WeatherDataDto();
        for (var entity : entities) {
            var station = entity.getStation();
            var group = grouping == WeatherGrouping.DAY_AVERAGE ? entity.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
                : grouping == WeatherGrouping.WEEK_AVERAGE ? String.format("%02d", entity.getDate().get(WeekFields.ISO.weekOfYear()))
                : grouping == WeatherGrouping.WEEK_TOTAL ? String.format("%02d", entity.getDate().get(WeekFields.ISO.weekOfYear()))
                : grouping == WeatherGrouping.MONTH_AVERAGE ? String.format("%02d", entity.getDate().getMonthValue())
                : grouping == WeatherGrouping.MONTH_TOTAL ? String.format("%02d", entity.getDate().getMonthValue())
                : grouping == WeatherGrouping.YEAR_AVERAGE ? String.valueOf(entity.getDate().getYear())
                : grouping == WeatherGrouping.YEAR_TOTAL ? String.valueOf(entity.getDate().getYear())
                    : entity.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
            var category = entity.getCategory();
            FIELD_EXTRACTORS.forEach((fieldName, extractor) -> {
                var value = extractor.apply(entity);
                var groupEntry = weatherData.getWeather().computeIfAbsent(station, _ -> new LinkedHashMap<>())
                    .computeIfAbsent(entity.getDate().getYear(), _ -> new LinkedHashMap<>())
                        .computeIfAbsent(group, _ -> new LinkedHashMap<>())
                            .computeIfAbsent(fieldName, _ -> new LinkedHashMap<>());
                groupEntry.computeIfAbsent(category, k -> {
                    if (grouping.name().contains("AVERAGE")) {
                        System.out.println("TODO: Keep track of record count, then afterwards (after all have been summed) divide by count?");
                    }
                    return groupEntry.get(k) + value;
                });
            });
        }
        return weatherData;
    }

}
