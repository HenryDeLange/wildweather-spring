package mywild.wildweather.domain.weather.web;

import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import mywild.wildweather.domain.weather.logic.WeatherService;
import mywild.wildweather.framework.error.BadRequestException;
import mywild.wildweather.framework.web.BaseController;

@Tag(name = "Weather", description = "Historic weather data captured by my Ambient Weather stations.")
@RestController
public class WeatherController extends BaseController {

    @Autowired
    private WeatherService service;

    @Operation(summary = "Provides all weather data, for the optional filter criteria.")
    @GetMapping("/weather")
    public WeatherDataDto getWeather(
            @RequestParam(required = false) String station,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer startMonth,
            @RequestParam(required = false) Integer endMonth
    ) {
        if ((startMonth != null && (startMonth < 1 || startMonth > 12))
                || (endMonth != null && (endMonth < 1 || endMonth > 12))
                || (startMonth != null && endMonth != null && startMonth > endMonth)) {
            throw new BadRequestException(station);
        }
        return service.getWeather(station, startDate, endDate, startMonth, endMonth);
    }

    @Operation(summary = "Provides all weather data, for the specified date at the specified station.")
    @GetMapping("/weather/{date}/{station}")
    public WeatherDataDto getStationWeatherOnDay(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, 
            @PathVariable String station) {
        return service.getStationWeatherOnDay(date, station);
    }

}
