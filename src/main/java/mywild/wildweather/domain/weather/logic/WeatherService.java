package mywild.wildweather.domain.weather.logic;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import mywild.wildweather.domain.weather.web.WeatherField;
import mywild.wildweather.domain.weather.web.WeatherGrouping;

@Slf4j
@Validated
@Service
public class WeatherService {

    private static final Map<WeatherField, Function<WeatherEntity, Double>> FIELD_EXTRACTORS;
    static {
        Map<WeatherField, Function<WeatherEntity, Double>> fieldMappings = new LinkedHashMap<>();
        fieldMappings.put(WeatherField.TEMPERATURE, WeatherEntity::getTemperature);
        fieldMappings.put(WeatherField.WIND_SPEED, WeatherEntity::getWindSpeed);
        fieldMappings.put(WeatherField.WIND_MAX, WeatherEntity::getWindMax);
        fieldMappings.put(WeatherField.WIND_DIRECTION, e -> mapDirection(e.getWindDirection()));
        fieldMappings.put(WeatherField.RAIN_RATE, WeatherEntity::getRainRate);
        fieldMappings.put(WeatherField.RAIN_DAILY, WeatherEntity::getRainDaily);
        fieldMappings.put(WeatherField.PRESSURE, WeatherEntity::getPressure);
        fieldMappings.put(WeatherField.HUMIDITY, WeatherEntity::getHumidity);
        fieldMappings.put(WeatherField.UV_RADIATION_INDEX, WeatherEntity::getUvRadiationIndex);
        fieldMappings.put(WeatherField.MISSING, WeatherEntity::getMissing);
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
            String station,
            WeatherGrouping grouping,
            WeatherCategory category,
            WeatherAggregate aggregate,
            Set<WeatherField> weatherFields,
            LocalDate startDate, LocalDate endDate,
            Integer startMonth, Integer endMonth) {
        return mapEntitiesToDto(grouping, aggregate, weatherFields,
            repo.searchWeather(category, station, startDate, endDate, startMonth, endMonth));
    }

    private WeatherDataDto mapEntitiesToDto(
            WeatherGrouping grouping,
            WeatherAggregate aggregate,
            Set<WeatherField> weatherFields,
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
            FIELD_EXTRACTORS.forEach((field, extractor) -> {
                if (weatherFields == null || weatherFields.isEmpty() || weatherFields.contains(field)) {
                    var value = extractor.apply(entity);
                    weatherData.getWeather().computeIfAbsent(station, _ -> new LinkedHashMap<>())
                        .computeIfAbsent(year, _ -> new LinkedHashMap<>())
                            .computeIfAbsent(group, _ -> new LinkedHashMap<>())
                                .computeIfAbsent(field.getKey(), _ -> new LinkedHashMap<>())
                                    .merge(category, value, (a, b) -> Math.round((a + b) * 10) / 10.0);
                }
            });
            if (grouping != WeatherGrouping.DAILY
                    && (aggregate == null || aggregate == WeatherAggregate.AVERAGE)
                    && entity.getCategory() == WeatherCategory.A) {
                String daysPerGroupKey = station + "-" + year + "-" + group;
                daysPerGroup.merge(daysPerGroupKey, 1, Integer::sum);
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
