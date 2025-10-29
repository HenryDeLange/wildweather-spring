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
import mywild.wildweather.domain.weather.data.WeatherRepository;
import mywild.wildweather.domain.weather.data.entity.WeatherCategory;
import mywild.wildweather.domain.weather.data.entity.WeatherEntity;
import mywild.wildweather.domain.weather.web.dto.WeatherAggregate;
import mywild.wildweather.domain.weather.web.dto.WeatherDataDto;
import mywild.wildweather.domain.weather.web.dto.WeatherField;
import mywild.wildweather.domain.weather.web.dto.WeatherGrouping;
import mywild.wildweather.domain.weather.web.dto.WeatherStatusDto;

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

    private static Double mapDirection(String direction) {
        if (direction == null || direction.isBlank())
            return 0.0;
        switch (direction) {
            case "N":
                return 0.0;
            case "NNE":
                return 22.5;
            case "NE":
                return 45.0;
            case "ENE":
                return 67.5;
            case "E":
                return 90.0;
            case "ESE":
                return 112.5;
            case "SE":
                return 135.0;
            case "SSE":
                return 157.5;
            case "S":
                return 180.0;
            case "SSW":
                return 202.5;
            case "SW":
                return 225.0;
            case "WSW":
                return 247.5;
            case "W":
                return 270.0;
            case "WNW":
                return 292.5;
            case "NW":
                return 315.0;
            case "NNW":
                return 337.5;
            default:
                return Double.NEGATIVE_INFINITY;
        }
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
        var calcAverage = (aggregate == null || aggregate == WeatherAggregate.AVERAGE);
        Map<String, Integer> daysPerGroup = new HashMap<>();
        Map<String, Integer> daysWithDataPerGroup = new HashMap<>();
        var weatherData = new WeatherDataDto();
        for (var weatherDay : entities) {
            var station = weatherDay.getStation();
            var year = weatherDay.getDate().getYear();
            var group = grouping == WeatherGrouping.DAILY ? weatherDay.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
                : grouping == WeatherGrouping.WEEKLY ? String.format("%02d", weatherDay.getDate().get(WeekFields.ISO.weekOfYear()))
                : grouping == WeatherGrouping.MONTHLY ? String.format("%02d", weatherDay.getDate().getMonthValue())
                : grouping == WeatherGrouping.YEARLY ? String.valueOf(weatherDay.getDate().getYear())
                    : weatherDay.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
            var category = weatherDay.getCategory();
            var groupMap = weatherData.getWeather().computeIfAbsent(station, _ -> new LinkedHashMap<>())
                        .computeIfAbsent(year, _ -> new LinkedHashMap<>())
                            .computeIfAbsent(group, _ -> new LinkedHashMap<>());
            if (category == WeatherCategory.A) {
                daysPerGroup.merge(getDaysPerGroupKey(station, year, group), 1, Integer::sum);
                if (weatherDay.getMissing() < 100) {
                    daysWithDataPerGroup.merge(getDaysPerGroupKey(station, year, group), 1, Integer::sum);
                }
            }
            FIELD_EXTRACTORS.forEach((field, extractor) -> {
                if (weatherFields == null || weatherFields.isEmpty() || weatherFields.contains(field)) {
                    var fieldMap = groupMap.computeIfAbsent(field.getKey(), _ -> new LinkedHashMap<>());
                    if (weatherDay.getMissing() < 100 || field == WeatherField.MISSING) {
                        var value = extractor.apply(weatherDay);
                        if (category == WeatherCategory.H) {
                            fieldMap.merge(category, value, calcAverage ? Math::max : Double::sum);
                        }
                        else if (category == WeatherCategory.L) {
                            fieldMap.merge(category, value, calcAverage ? Math::min : Double::sum);
                        }
                        else {
                            fieldMap.merge(category, value, Double::sum);
                        }
                    }
                    else {
                        fieldMap.putIfAbsent(category, null);
                    }
                }
            });
        }
        weatherData.getWeather().forEach((station, yearMap) -> {
            yearMap.forEach((year, groupMap) -> {
                groupMap.forEach((group, fieldMap) -> {
                    fieldMap.forEach((field, categoryMap) -> {
                        categoryMap.replaceAll((category, total) -> {
                            if (total != null) {
                                if (calcAverage && category == WeatherCategory.A) {
                                    Integer days;
                                    if (WeatherField.fromKey(field) == WeatherField.MISSING) {
                                        days = daysPerGroup.getOrDefault(getDaysPerGroupKey(station, year, group), 1);
                                    }
                                    else {
                                        days = daysWithDataPerGroup.getOrDefault(getDaysPerGroupKey(station, year, group), 1);
                                    }
                                    return Math.round(total / (double) days * 10.0) / 10.0;
                                }
                                else {
                                    return Math.round(total * 10.0) / 10.0;
                                }
                            }
                            else {
                                return null;
                            }
                        });
                    });
                });
            });
        });
        return weatherData;
    }

    private String getDaysPerGroupKey(String station, int year, String group) {
        String daysPerGroupKey = station + "-" + year + "-" + group;
        return daysPerGroupKey;
    }

    public List<String> getWeatherStations() {
        return repo.findStations();
    }

    public List<WeatherStatusDto> getWeatherStatus() {
        var stations = repo.findStations();
        return stations.stream()
            .<WeatherStatusDto>map(station -> {
                var date = repo.findTopDateByStation(station);
                return WeatherStatusDto.builder()
                    .station(station)
                    .lastProcessedOn(date)
                    .build();
            })
            .toList();
    }

}
