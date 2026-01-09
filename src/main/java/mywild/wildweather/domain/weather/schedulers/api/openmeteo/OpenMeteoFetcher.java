package mywild.wildweather.domain.weather.schedulers.api.openmeteo;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mywild.openmeteo.openapi.client.api.OpenMeteoApi;
import mywild.openmeteo.openapi.client.model.DailyVariable;
import mywild.wildweather.domain.weather.schedulers.api.AbstractFetcher;
import mywild.wildweather.domain.weather.schedulers.api.FetchedWeatherField;
import mywild.wildweather.domain.weather.schedulers.api.FetchedWeatherRecord;

/**
 * https://open-meteo.com/
 * https://open-meteo.com/en/docs
 * 
 * Example:
 * https://archive-api.open-meteo.com/v1/archive?latitude=-33.674522&longitude=26.655423&start_date=1994-01-01&end_date=1994-12-31&daily=rain_sum,precipitation_sum,temperature_2m_mean,temperature_2m_max,temperature_2m_min,wind_direction_10m_dominant,wind_gusts_10m_max,wind_speed_10m_max,wind_gusts_10m_mean,wind_speed_10m_mean,wind_gusts_10m_min,wind_speed_10m_min&timezone=auto
 * 
 * Limit:
 * Less than 10 000 API calls per day.
 * Less than 5 000 API calls per hour.
 * Less than 600 API calls per minute.
 */

@NoArgsConstructor
@Slf4j
@Service
public class OpenMeteoFetcher extends AbstractFetcher {

    private static final List<DailyVariable> FIELDS = List.of(
        DailyVariable.TEMPERATURE_2M_MIN,
        DailyVariable.TEMPERATURE_2M_MEAN,
        DailyVariable.TEMPERATURE_2M_MAX,
        DailyVariable.RAIN_SUM,
        DailyVariable.PRECIPITATION_SUM,
        DailyVariable.WIND_DIRECTION_10M_DOMINANT,
        DailyVariable.WIND_SPEED_10M_MIN,
        DailyVariable.WIND_SPEED_10M_MEAN,
        DailyVariable.WIND_SPEED_10M_MAX,
        DailyVariable.WIND_GUSTS_10M_MIN,
        DailyVariable.WIND_GUSTS_10M_MEAN,
        DailyVariable.WIND_GUSTS_10M_MAX,
        DailyVariable.RELATIVE_HUMIDITY_2M_MIN,
        DailyVariable.RELATIVE_HUMIDITY_2M_MEAN,
        DailyVariable.RELATIVE_HUMIDITY_2M_MAX,
        DailyVariable.SURFACE_PRESSURE_MIN,
        DailyVariable.SURFACE_PRESSURE_MEAN,
        DailyVariable.SURFACE_PRESSURE_MAX
    );

    @Autowired
    private OpenMeteoApi api;

    @Override
    public List<FetchedWeatherRecord> fetchRecords(String station, LocalDate apiStarDate, LocalDate apiEndDate, List<String> badDays) {
        var coords = station.split(",");
        float latitude = Float.parseFloat(coords[0].trim());
        float longitude = Float.parseFloat(coords[1].trim());
        var data = api.getArchive(
            latitude,
            longitude, 
            apiStarDate, 
            apiEndDate, 
            FIELDS,
            "Africa/Johannesburg");
        if (data != null && data.getDaily().getTime() != null && !data.getDaily().getTime().isEmpty()) {
            var dataRecord = data.getDaily();
            List<FetchedWeatherRecord> records = new ArrayList<>(dataRecord.getTime().size());
            for (int i = 0; i < dataRecord.getTime().size(); i++) {
                var recordDate = dataRecord.getTime().get(i);
                records.add(FetchedWeatherRecord.builder()
                    .date(recordDate)
                    .temperature(FetchedWeatherField.builder()
                        .low((double) dataRecord.getTemperature2mMin().get(i))
                        .average((double) dataRecord.getTemperature2mMean().get(i))
                        .high((double) dataRecord.getTemperature2mMax().get(i))
                        .build())
                    .rain(FetchedWeatherField.builder()
                        .low((double) dataRecord.getRainSum().get(i))
                        .average((double) dataRecord.getRainSum().get(i))
                        .high((double) dataRecord.getRainSum().get(i))
                        .build())
                    .windDirection(FetchedWeatherField.builder()
                        .low((double) dataRecord.getWindDirection10mDominant().get(i))
                        .average((double) dataRecord.getWindDirection10mDominant().get(i))
                        .high((double) dataRecord.getWindDirection10mDominant().get(i))
                        .build())
                    .wind(FetchedWeatherField.builder()
                        .low((double) dataRecord.getWindSpeed10mMin().get(i))
                        .average((double) dataRecord.getWindSpeed10mMean().get(i))
                        .high((double) dataRecord.getWindSpeed10mMax().get(i))
                        .build())
                    .gust(FetchedWeatherField.builder()
                        .low((double) dataRecord.getWindGusts10mMin().get(i))
                        .average((double) dataRecord.getWindGusts10mMean().get(i))
                        .high((double) dataRecord.getWindGusts10mMax().get(i))
                        .build())
                    .humidity(FetchedWeatherField.builder()
                        .low((double) dataRecord.getRelativeHumidity2mMin().get(i))
                        .average((double) dataRecord.getRelativeHumidity2mMean().get(i))
                        .high((double) dataRecord.getRelativeHumidity2mMax().get(i))
                        .build())
                    .pressure(FetchedWeatherField.builder()
                        .low((double) dataRecord.getSurfacePressureMin().get(i))
                        .average((double) dataRecord.getSurfacePressureMean().get(i))
                        .high((double) dataRecord.getSurfacePressureMax().get(i))
                        .build())
                    .build());
            }
            return records;
        }
        else {
            return null;
        }
    }

    @Override
    protected void apiLimitCheck() throws InterruptedException {
        // Sleep for 5 seconds to not spam the api too much
        // Note:
        // Even though the spec states 1 per second, it seems to not be the case:
        // "429 - Too Many Requests" if sleeping for 1 second
        Thread.sleep(Duration.ofSeconds(5));
    }

}
