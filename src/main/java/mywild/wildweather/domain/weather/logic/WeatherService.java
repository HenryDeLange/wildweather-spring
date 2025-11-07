package mywild.wildweather.domain.weather.logic;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import mywild.wildweather.domain.weather.data.WeatherRepository;
import mywild.wildweather.domain.weather.data.entity.WeatherCategory;
import mywild.wildweather.domain.weather.web.dto.WeatherAggregate;
import mywild.wildweather.domain.weather.web.dto.WeatherDataDto;
import mywild.wildweather.domain.weather.web.dto.WeatherField;
import mywild.wildweather.domain.weather.web.dto.WeatherGrouping;
import mywild.wildweather.domain.weather.web.dto.WeatherStatusDto;

@Slf4j
@Validated
@Service
public class WeatherService {

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
        return Mapper.mapEntitiesToDto(grouping, aggregate, weatherFields,
            repo.searchWeather(category, station, startDate, endDate, startMonth, endMonth));
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
