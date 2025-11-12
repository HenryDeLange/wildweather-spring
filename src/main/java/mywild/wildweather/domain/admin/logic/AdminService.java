package mywild.wildweather.domain.admin.logic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import mywild.wildweather.domain.admin.web.dto.ApiStatus;
import mywild.wildweather.domain.admin.web.dto.CsvStatus;
import mywild.wildweather.domain.weather.schedulers.api.AmbientWeatherApiScheduler;
import mywild.wildweather.domain.weather.schedulers.api.WeatherUndergroundApiScheduler;
import mywild.wildweather.domain.weather.schedulers.csv.WeatherCsvScheduler;

@Slf4j
@Validated
@Service
public class AdminService {

    @Autowired
    private WeatherCsvScheduler csvScheduler;

    @Autowired
    private AmbientWeatherApiScheduler ambientWeatherApiScheduler;

    @Autowired
    private WeatherUndergroundApiScheduler weatherUndergroundApiScheduler;

    public void triggerCsvProcessing(boolean forceFullReload) {
        if (forceFullReload) {
            csvScheduler.resetProcessedCsvFiles();
        }
        csvScheduler.processCsvFiles();
    }

    public @Valid CsvStatus getCsvProcessStatus() {
        return new CsvStatus(csvScheduler.isRunning());
    }

    public void triggerAmbientWeatherApiProcessing() {
        ambientWeatherApiScheduler.processApiData();
    }

    public @Valid ApiStatus getAmbientWeatherApiProcessStatus() {
        return new ApiStatus(ambientWeatherApiScheduler.isRunning());
    }

    public void triggerWeatherUndergroundApiProcessing() {
        weatherUndergroundApiScheduler.processApiData();
    }

    public @Valid ApiStatus getWeatherUndergroundApiProcessStatus() {
        return new ApiStatus(weatherUndergroundApiScheduler.isRunning());
    }

}
