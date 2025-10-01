package mywild.wildweather.domain.weather.logic;

import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import mywild.wildweather.domain.weather.data.WeatherRepository;
import mywild.wildweather.domain.weather.web.WeatherDto;

@Validated
@Service
public class WeatherService {

    @Autowired
    private WeatherRepository repo;

    public @Valid List<WeatherDto> getWeather() {
        return WeatherMapper.INSTANCE.entityToDtoAsList(repo.findAll());
    }

    public @Valid List<WeatherDto> getWeatherOnDay(@Valid LocalDate date) {
        return WeatherMapper.INSTANCE.entityToDtoAsList(repo.findAllByDate(date));
    }

}
