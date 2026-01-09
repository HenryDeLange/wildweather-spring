package mywild.wildweather.domain.admin.logic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import mywild.wildweather.domain.admin.web.dto.ApiStatus;
import mywild.wildweather.domain.admin.web.dto.CsvStatus;
import mywild.wildweather.domain.weather.schedulers.api.ambientweather.AmbientWeatherScheduler;
import mywild.wildweather.domain.weather.schedulers.api.openmeteo.OpenMeteoScheduler;
import mywild.wildweather.domain.weather.schedulers.api.weatherunderground.WeatherUndergroundScheduler;
import mywild.wildweather.domain.weather.schedulers.csv.WeatherCsvScheduler;

@Slf4j
@Validated
@Service
public class AdminService {

    @Autowired
    private WeatherCsvScheduler csvScheduler;

    @Autowired
    private AmbientWeatherScheduler ambientWeatherScheduler;

    @Autowired
    private WeatherUndergroundScheduler weatherUndergroundScheduler;

    @Autowired
    private OpenMeteoScheduler openMeteoScheduler;

    public void triggerCsvProcessing(boolean forceFullReload) {
        if (forceFullReload) {
            csvScheduler.resetAllProcessedCsvFiles();
        }
        else {
            csvScheduler.resetLatestWeatherUndergroundProcessedCsvFiles();
        }
        csvScheduler.processCsvFiles();
    }

    public @Valid CsvStatus getCsvProcessStatus() {
        return new CsvStatus(csvScheduler.isRunning());
    }

    public void triggerAmbientWeatherApiProcessing(boolean fetchAllData) {
        ambientWeatherScheduler.processApiData(fetchAllData);
    }

    public @Valid ApiStatus getAmbientWeatherApiProcessStatus() {
        return new ApiStatus(ambientWeatherScheduler.isRunning());
    }

    public void triggerWeatherUndergroundApiProcessing(boolean fetchAllData) {
        weatherUndergroundScheduler.processApiData(fetchAllData);
    }

    public @Valid ApiStatus getWeatherUndergroundApiProcessStatus() {
        return new ApiStatus(weatherUndergroundScheduler.isRunning());
    }

    public void triggerOpenMeteoApiProcessing(boolean fetchAllData) {
        openMeteoScheduler.processApiData(fetchAllData);
    }

    public @Valid ApiStatus getOpenMeteoApiProcessStatus() {
        return new ApiStatus(openMeteoScheduler.isRunning());
    }

}
