package mywild.wildweather.domain.admin.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import mywild.wildweather.domain.weather.schedulers.api.WeatherApiScheduler;
import mywild.wildweather.domain.weather.schedulers.csv.WeatherCsvScheduler;
import mywild.wildweather.framework.web.BaseController;

@Tag(name = "Admin", description = "Admin actions.")
@RestController
public class AdminController extends BaseController {

    @Autowired
    private WeatherCsvScheduler csvScheduler;

    @Autowired
    private WeatherApiScheduler apiScheduler;

    @Operation(summary = "Manually trigger the processing of Ambient Weather CSV files.")
    @PostMapping("/admin/process/csv")
    public void triggerCsvProcessing(@RequestParam(required = false) boolean forceFullReload) {
        if (forceFullReload) {
            csvScheduler.resetProcessedCsvFiles();
        }
        csvScheduler.processCsvFiles();
    }

    @Operation(summary = "Manually trigger the processing of Ambient Weather API data.")
    @PostMapping("/admin/process/api")
    public void triggerApiProcessing(@RequestParam(required = false) boolean processAllAvailable) {
        apiScheduler.processApiData(processAllAvailable);
    }

}
