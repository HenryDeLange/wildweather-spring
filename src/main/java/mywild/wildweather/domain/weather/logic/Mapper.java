package mywild.wildweather.domain.weather.logic;

import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import mywild.wildweather.domain.weather.data.entity.WeatherCategory;
import mywild.wildweather.domain.weather.data.entity.WeatherEntity;
import mywild.wildweather.domain.weather.web.dto.WeatherAggregate;
import mywild.wildweather.domain.weather.web.dto.WeatherDataDto;
import mywild.wildweather.domain.weather.web.dto.WeatherField;
import mywild.wildweather.domain.weather.web.dto.WeatherGrouping;

final public class Mapper {

    private Mapper() {
        // prevent instantiation
    }

    static WeatherDataDto mapEntitiesToDto(
            WeatherGrouping grouping,
            WeatherAggregate aggregate,
            Set<WeatherField> weatherFields,
            List<WeatherEntity> entities) {
        var calcAverage = aggregate == null || aggregate == WeatherAggregate.AVERAGE;
        Map<String, Integer> daysWithDataPerGroupAndField = new HashMap<>();
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
            WeatherFieldExtractor.EXTRACTORS.forEach((field, extractor) -> {
                if (weatherFields == null || weatherFields.isEmpty() || weatherFields.contains(field)) {
                    var fieldMap = groupMap.computeIfAbsent(field.getKey(), _ -> new LinkedHashMap<>());
                    var value = extractor.apply(weatherDay);
                    if ((value != null && weatherDay.getMissing() < 100) || field == WeatherField.MISSING) {
                        if (category == WeatherCategory.H) {
                            fieldMap.merge(category, value, calcAverage ? Math::max : Double::sum);
                        }
                        else if (category == WeatherCategory.L) {
                            fieldMap.merge(category, value, calcAverage ? Math::min : Double::sum);
                        }
                        else {
                            fieldMap.merge(category, value, Double::sum);
                            daysWithDataPerGroupAndField.merge(getDaysPerGroupAndFieldKey(station, year, group, field), 1, Integer::sum);
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
                                    var weatherField = WeatherField.fromKey(field);
                                    var key = getDaysPerGroupAndFieldKey(station, year, group, weatherField);
                                    var days = daysWithDataPerGroupAndField.getOrDefault(key, 1);
                                    return Conversions.roundToOneDecimal(total / (double) days);
                                }
                                else {
                                    return Conversions.roundToOneDecimal(total);
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

    private static String getDaysPerGroupAndFieldKey(String station, int year, String group, WeatherField field) {
        return station + "-" + year + "-" + group + "-" + field;
    }
    
}
