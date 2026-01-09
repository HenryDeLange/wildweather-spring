package mywild.wildweather.domain.weather.schedulers.api.climateserv;

import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mywild.climateserv.openapi.client.api.ClimateServApi;
import mywild.wildweather.domain.weather.schedulers.api.AbstractFetcher;
import mywild.wildweather.domain.weather.schedulers.api.FetchedWeatherRecord;

/**
 * https://www.chc.ucsb.edu/data
 * https://climateserv.servirglobal.net/
 * https://github.com/SERVIR/ClimateSERV/blob/master/docs/api.rst
 * https://climateserv.servirglobal.net/develop-api
 * 
 * Example:
 * https://climateserv.servirglobal.net/api/submitDataRequest/?datatype=0&ensemble=false&begintime=06%2F16%2F2025&endtime=11%2F29%2F2025&intervaltype=0&operationtype=5&dateType_Category=default&isZip_CurrentDataType=false&geometry=%7B%22type%22:%22FeatureCollection%22,%22features%22:%5B%7B%22type%22:%22Feature%22,%22properties%22:%7B%7D,%22geometry%22:%7B%22type%22:%22Point%22,%22coordinates%22:%5B26.655423,-33.674522%5D%7D%7D%5D%7D
 * https://climateserv.servirglobal.net/api/getDataRequestProgress/?id=76d0a745-b303-4410-a05b-9453d15b9893
 * https://climateserv.servirglobal.net/api/getDataFromRequest/?id=76d0a745-b303-4410-a05b-9453d15b9893
 * 
 * Limit:
 * ?
 */

@NoArgsConstructor
@Slf4j
@Service
public class ClimateServFetcher extends AbstractFetcher {

    @Autowired
    private ClimateServApi api;

    @Override
    public List<FetchedWeatherRecord> fetchRecords(String station, LocalDate apiStarDate, LocalDate apiEndDate, List<String> badDays) {
        // TODO: Implement this
        return null;
    }

    @Override
    protected void apiLimitCheck() throws InterruptedException {
        // TODO: Implement this
    }

}
