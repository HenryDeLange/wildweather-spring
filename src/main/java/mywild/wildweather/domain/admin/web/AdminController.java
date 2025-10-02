package mywild.wildweather.domain.admin.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import mywild.wildweather.domain.weather.logic.WeatherScheduler;
import mywild.wildweather.framework.web.BaseController;

@Tag(name = "Admin", description = "Admin actions.")
@RestController
public class AdminController extends BaseController {

    @Autowired
    private WeatherScheduler scheduler;

    @Operation(summary = "Manually trigger the processing of CSV files.")
    @PostMapping("/admin/process-files")
    public void triggerCsvProcessing(@RequestParam(required = false) boolean forceFullReload) {
        if (forceFullReload) {
            scheduler.resetProcessedCsvFiles();
        }
        scheduler.processCsvFiles();
    }

}
