package mywild.wildweather.domain.weather.logic;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import mywild.wildweather.domain.weather.web.dto.WeatherStationDto;

@Slf4j
@Validated
@Service
public class WeatherService {

    @Value("#{'${mywild.my-stations}'.split(',\\s*')}")
    private List<String> myStations;

    @Autowired
    private WeatherRepository repo;

    public @Valid WeatherDataDto getWeather(
            List<String> stations,
            WeatherGrouping grouping,
            WeatherCategory category,
            WeatherAggregate aggregate,
            Set<WeatherField> weatherFields,
            LocalDate startDate, LocalDate endDate,
            Integer startMonth, Integer endMonth) {
        return Mapper.mapEntitiesToDto(grouping, aggregate, weatherFields,
            repo.searchWeather(stations, category, startDate, endDate, startMonth, endMonth));
    }

    public List<String> getWeatherStations() {
        return repo.findStations();
    }

    public List<WeatherStationDto> getWeatherStatus() {
        var stations = repo.findStations();
        return stations.stream()
            .<WeatherStationDto>map(station -> {
                var startDate = repo.findBottomDateByStation(station);
                var endDate = repo.findTopDateByStation(station);
                return WeatherStationDto.builder()
                    .station(station)
                    .startDate(startDate)
                    .endDate(endDate)
                    .isMyStation(myStations.contains(station))
                    .build();
            })
            .toList();
    }

}
