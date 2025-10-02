package mywild.wildweather.domain.weather.web;

import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import mywild.wildweather.domain.weather.logic.WeatherService;
import mywild.wildweather.framework.web.BaseController;

@Tag(name = "Weather", description = "View past weather data from my Ambient Weather stations.")
@RestController
public class WeatherController extends BaseController {

    @Autowired
    private WeatherService service;

    @Operation(summary = "Provides all weather data, for the specified (optional) date range.")
    @GetMapping("/weather")
    public List<WeatherDto> getWeather(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)LocalDate endDate) {
        return service.getWeather(startDate, endDate);
    }

    @Operation(summary = "Provides all weather data, for the specified day.")
    @GetMapping("/weather/{date}")
    public List<WeatherDto> getWeatherOnDay(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)LocalDate date) {
        return service.getWeatherOnDay(date);
    }

    @Operation(summary = "Provides all weather data, for the specified day at the specified station.")
    @GetMapping("/weather/{date}/{station}")
    public List<WeatherDto> getWeatherOnDay(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, 
            @PathVariable String station) {
        return service.getStationWeatherOnDay(date, station);
    }

}
