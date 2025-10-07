package mywild.wildweather.domain.weather.logic;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.Collections;
import java.util.HashMap;
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
import mywild.wildweather.domain.weather.web.WeatherAggregate;
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
            WeatherAggregate aggregate,
            String station,
            LocalDate startDate, LocalDate endDate,
            Integer startMonth, Integer endMonth) {
        return mapEntitiesToDto(grouping, aggregate,
            repo.searchWeather(category, station, startDate, endDate, startMonth, endMonth));
    }

    private WeatherDataDto mapEntitiesToDto(
            WeatherGrouping grouping,
            WeatherAggregate aggregate,
            List<WeatherEntity> entities) {
        Map<String, Integer> daysPerGroup = new HashMap<>();
        var weatherData = new WeatherDataDto();
        for (var entity : entities) {
            var station = entity.getStation();
            var year = entity.getDate().getYear();
            var group = grouping == WeatherGrouping.DAILY ? entity.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
                : grouping == WeatherGrouping.WEEKLY ? String.format("%02d", entity.getDate().get(WeekFields.ISO.weekOfYear()))
                : grouping == WeatherGrouping.MONTHLY ? String.format("%02d", entity.getDate().getMonthValue())
                : grouping == WeatherGrouping.YEARLY ? String.valueOf(entity.getDate().getYear())
                    : entity.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
            var category = entity.getCategory();
            FIELD_EXTRACTORS.forEach((fieldName, extractor) -> {
                var value = extractor.apply(entity);
                weatherData.getWeather().computeIfAbsent(station, _ -> new LinkedHashMap<>())
                    .computeIfAbsent(year, _ -> new LinkedHashMap<>())
                        .computeIfAbsent(group, _ -> new LinkedHashMap<>())
                            .computeIfAbsent(fieldName, _ -> new LinkedHashMap<>())
                                .merge(category, value, (a, b) -> Math.round((a + b) * 10) / 10.0);
            });
            if (grouping != WeatherGrouping.DAILY && (aggregate == null || aggregate == WeatherAggregate.AVERAGE)) {
                if (entity.getCategory() == WeatherCategory.A) {
                    String daysPerGroupKey = station + "-" + year + "-" + group;
                    daysPerGroup.merge(daysPerGroupKey, 1, Integer::sum);
                }
            }
        }
        if (!daysPerGroup.isEmpty()) {
            weatherData.getWeather().forEach((station, yearMap) -> {
                yearMap.forEach((year, groupMap) -> {
                    groupMap.forEach((group, fieldMap) -> {
                        fieldMap.forEach((_, categoryMap) -> {
                            categoryMap.replaceAll((_, total) -> {
                                String daysPerGroupKey = station + "-" + year + "-" + group;
                                Integer days = daysPerGroup.getOrDefault(daysPerGroupKey, 1);
                                return Math.round(total / (double) days * 10.0) / 10.0;
                            });
                        });
                    });
                });
            });
        }
        return weatherData;
    }

    public List<String> getWeatherStations() {
        return repo.findStations();
    }

}
