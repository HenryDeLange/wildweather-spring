package mywild.wildweather.domain.weather.schedulers.api.weatherunderground;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import mywild.weatherunderground.openapi.client.api.WeatherUndergroundApi;
import mywild.weatherunderground.openapi.client.model.FormatEnum;
import mywild.weatherunderground.openapi.client.model.NumericPrecisionEnum;
import mywild.weatherunderground.openapi.client.model.UnitsEnum;
import mywild.wildweather.domain.weather.schedulers.api.AbstractFetcher;
import mywild.wildweather.domain.weather.schedulers.api.FetchedWeatherField;
import mywild.wildweather.domain.weather.schedulers.api.FetchedWeatherRecord;

/**
 * https://www.wunderground.com/member/api-keys
 * https://docs.google.com/document/d/1eKCnKXI9xnoMGRRzOL1xPCBihNV2rOet08qpE_gArAY
 * https://docs.google.com/document/d/13HTLgJDpsb39deFzk_YCQ5GoGoZCO_cRYzIxbwvgJLI
 * https://docs.google.com/document/d/1w8jbqfAk0tfZS5P7hYnar1JiitM0gQZB-clxDfG3aD0
 * 
 * Example URL:
 * https://api.weather.com/v2/pws/history/daily?startDate=20250101&endDate=20250131&format=json&units=m&numericPrecision=decimal&stationId=__STATION__&apiKey=__APIKEY__
 * 
 * Limit:
 * Less than 1 API calls per second.
 */

@Slf4j
@Service
public class WeatherUndergroundFetcher extends AbstractFetcher {

    private static final DateTimeFormatter API_DATE_FORMAT =  DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired
    private WeatherUndergroundApi api;

    // TODO: The daily endpoint seems to frequently have blank days even when the station was on during that day,
    //       maybe consider switching to the hourly endpoint instead?

    @Override
    public List<FetchedWeatherRecord> fetchRecords(String station, LocalDate apiStarDate, LocalDate apiEndDate, List<String> badDays) {
        var data = api.getDaily(station, FormatEnum.JSON, UnitsEnum.METRIC, 
            null, apiStarDate.format(API_DATE_FORMAT), apiEndDate.format(API_DATE_FORMAT),
            NumericPrecisionEnum.DECIMAL);
        if (data != null && data.getObservations() != null && !data.getObservations().isEmpty()) {
            List<FetchedWeatherRecord> records = new ArrayList<>(data.getObservations().size());
            for (var dataRecord : data.getObservations()) {
                var recordDate = dataRecord.getObsTimeUtc().toLocalDate();
                if (badDays == null || !badDays.contains(dataRecord.getObsTimeUtc().toLocalDate().format(WeatherUndergroundScheduler.BAD_DAYS_DATE_FORMAT))) {
                    var metricData = dataRecord.getMetric();
                    records.add(FetchedWeatherRecord.builder()
                        .date(recordDate)
                        .temperature(FetchedWeatherField.builder()
                            .low(metricData.getTempLow())
                            .average(metricData.getTempAvg())
                            .high(metricData.getTempHigh())
                            .build())
                        .rainRate(FetchedWeatherField.builder()
                            .low(metricData.getPrecipRate())
                            .average(metricData.getPrecipRate())
                            .high(metricData.getPrecipRate())
                            .build())
                        .rain(FetchedWeatherField.builder()
                            .low(metricData.getPrecipTotal())
                            .average(metricData.getPrecipTotal())
                            .high(metricData.getPrecipTotal())
                            .build())
                        .windDirection(FetchedWeatherField.builder()
                            .low(dataRecord.getWinddirAvg())
                            .average(dataRecord.getWinddirAvg())
                            .high(dataRecord.getWinddirAvg())
                            .build())
                        .wind(FetchedWeatherField.builder()
                            .low(metricData.getWindspeedLow())
                            .average(metricData.getWindspeedAvg())
                            .high(metricData.getWindspeedHigh())
                            .build())
                        .gust(FetchedWeatherField.builder()
                            .low(metricData.getWindgustLow())
                            .average(metricData.getWindgustAvg())
                            .high(metricData.getWindgustHigh())
                            .build())
                        .humidity(FetchedWeatherField.builder()
                            .low(dataRecord.getHumidityLow())
                            .average(dataRecord.getHumidityAvg())
                            .high(dataRecord.getHumidityHigh())
                            .build())
                        .pressure(FetchedWeatherField.builder()
                            .low(metricData.getPressureMin())
                            .average(metricData.getPressureTrend())
                            .high(metricData.getPressureMax())
                            .build())
                        .uvRadiation(FetchedWeatherField.builder()
                            .low(0.0)
                            .average(dataRecord.getUvHigh() != null ? dataRecord.getUvHigh() / 2.0 : null)
                            .high(dataRecord.getUvHigh())
                            .build())
                        .build());
                }
                else {
                    log.debug("       Skip known bad day : {}", recordDate);
                }
            }
            return records;
        }
        else {
            return null;
        }
    }

    @Override
    protected void apiLimitCheck() throws InterruptedException {
        // Sleep for 1 seconds to not spam the api too much
        Thread.sleep(Duration.ofSeconds(1));
    }

}
