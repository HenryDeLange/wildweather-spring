package mywild.wildweather.domain.weather.web;

import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import mywild.wildweather.domain.weather.logic.WeatherService;
import mywild.wildweather.framework.web.BaseController;

@Tag(name = "Weather", description = "View past weather from my Ambient Weather stations.")
@RestController
public class WeatherController extends BaseController {

    @Autowired
    private WeatherService service;

    @Operation(summary = "Provides the list of all Weather data.")
    @GetMapping("/weather")
    public List<WeatherDto> getWeather() {
        return service.getWeather();
    }

    @Operation(summary = "Provides the list of all Weather data for the specified day.")
    @GetMapping("/weather/{date}")
    public List<WeatherDto> getWeatherOnDay(@PathVariable LocalDate date) {
        return service.getWeatherOnDay(date);
    }

}
