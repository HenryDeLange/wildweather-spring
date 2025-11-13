package mywild.wildweather.domain.admin.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import mywild.wildweather.domain.admin.logic.AdminService;
import mywild.wildweather.domain.admin.web.dto.ApiStatus;
import mywild.wildweather.domain.admin.web.dto.CsvStatus;
import mywild.wildweather.framework.web.BaseController;

@Tag(name = "Admin", description = "Admin actions.")
@RestController
public class AdminController extends BaseController {

    @Autowired
    private AdminService service;

    @Operation(summary = "Manually trigger the processing of Ambient Weather CSV files.")
    @PostMapping("/admin/process/csv")
    public void triggerCsvProcessing(@RequestParam(required = false) boolean forceFullReload) {
        service.triggerCsvProcessing(forceFullReload);
    }

    @Operation(summary = "Status of processing the Ambient Weather CSV files.")
    @GetMapping("/admin/process/csv")
    public CsvStatus getCsvProcessStatus() {
        return service.getCsvProcessStatus();
    }

    @Operation(summary = "Manually trigger the processing of Ambient Weather API data.")
    @PostMapping("/admin/process/api/ambient-weather")
    public void triggerAmbientWeatherApiProcessing() {
        service.triggerAmbientWeatherApiProcessing();
    }

    @Operation(summary = "Status of processing the Ambient Weather API data.")
    @GetMapping("/admin/process/api/ambient-weather")
    public ApiStatus getAmbientWeatherApiProcessStatus() {
        return service.getAmbientWeatherApiProcessStatus();
    }

    @Operation(summary = "Manually trigger the processing of Weather Underground API data.")
    @PostMapping("/admin/process/api/weather-underground")
    public void triggerWeatherUndergroundApiProcessing(@RequestParam(required = false) boolean fetchAllData) {
        service.triggerWeatherUndergroundApiProcessing(fetchAllData);
    }

    @Operation(summary = "Status of processing the Weather Underground API data.")
    @GetMapping("/admin/process/api/weather-underground")
    public ApiStatus getWeatherUndergroundApiProcessStatus() {
        return service.getWeatherUndergroundApiProcessStatus();
    }

}
