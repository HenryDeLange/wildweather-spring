package mywild.wildweather.domain.weather.schedulers.api.ambientweather;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mywild.ambientweather.openapi.client.api.AmbientWeatherApi;
import mywild.wildweather.domain.weather.schedulers.api.AbstractFetcher;
import mywild.wildweather.domain.weather.schedulers.api.Conversions;
import mywild.wildweather.domain.weather.schedulers.api.FetchedWeatherField;
import mywild.wildweather.domain.weather.schedulers.api.FetchedWeatherRecord;

/**
 * https://ambientweather.net/account/keys
 * https://ambientweather.docs.apiary.io/
 * https://github.com/ambient-weather/api-docs/wiki/Device-Data-Specs
 *
 * Example:
 * ?
 * 
 * Limit:
 * Not more than 1 request per second
 */

@NoArgsConstructor
@Slf4j
@Service
public class AmbientWeatherFetcher extends AbstractFetcher {

    private static final int EXPECTED_RECORDS_PER_DAY = 24 * (60 / 5); // 288 (Every 5 minutes)

    private static final String FIELD_TMP = "tmp";
    private static final String FIELD_WND = "wnd";
    private static final String FIELD_GST = "gst";
    private static final String FIELD_WDR = "wdr";
    private static final String FIELD_RNR = "rnr";
    private static final String FIELD_RNT = "rnt";
    private static final String FIELD_PRS = "prs";
    private static final String FIELD_HMT = "hmt";
    private static final String FIELD_UVI = "uvi";

    @Autowired
    private AmbientWeatherApi api;

    @Override
    public List<FetchedWeatherRecord> fetchRecords(String station, LocalDate apiStarDate, LocalDate apiEndDate, List<String> badDays) {
        OffsetDateTime apiEndDateTime = apiEndDate.atTime(OffsetTime.MAX);
        var data = api.getDeviceData(station, apiEndDateTime, EXPECTED_RECORDS_PER_DAY);
        if (data != null && !data.isEmpty()) {
            List<FetchedWeatherRecord> records = new ArrayList<>(data.size());
            Map<String, Double> low = new LinkedHashMap<>();
            Map<String, List<Double>> average = new LinkedHashMap<>();
            Map<String, Double> high = new LinkedHashMap<>();
            for (var dataRecord : data) {
                var recordDate = dataRecord.getDate().toLocalDate();
                if (recordDate.equals(apiEndDate)) {
                    processValue(low, average, high, FIELD_TMP, Conversions.fahToCel(dataRecord.getTempf()));
                    processValue(low, average, high, FIELD_WND, Conversions.mphToKmh(dataRecord.getWindspeedmph()));
                    processValue(low, average, high, FIELD_GST, Conversions.mphToKmh(dataRecord.getWindgustmph()));
                    processValue(low, average, high, FIELD_WDR, dataRecord.getWinddir());
                    processValue(low, average, high, FIELD_RNR, Conversions.inToMm(dataRecord.getHourlyrainin()));
                    processValue(low, average, high, FIELD_RNT, Conversions.inToMm(dataRecord.getDailyrainin()));
                    processValue(low, average, high, FIELD_PRS, Conversions.inHgToHpa(dataRecord.getBaromrelin()));
                    processValue(low, average, high, FIELD_HMT, dataRecord.getHumidity());
                    processValue(low, average, high, FIELD_UVI, dataRecord.getUv());
                }
                else {
                    log.debug("   Not processing records for date {} while busy processing {}",
                        recordDate, apiEndDate);
                    break;
                }
                Map<String, Double> calculatedAverage = getCalculatedAverage(average);
                records.add(FetchedWeatherRecord.builder()
                    .date(recordDate)
                    .temperature(FetchedWeatherField.builder()
                        .low(low.get(FIELD_TMP))
                        .average(calculatedAverage.get(FIELD_TMP))
                        .high(high.get(FIELD_TMP))
                        .build())
                    .rain(FetchedWeatherField.builder()
                        .low(low.get(FIELD_RNT))
                        .average(calculatedAverage.get(FIELD_RNT))
                        .high(high.get(FIELD_RNT))
                        .build())
                    .rainRate(FetchedWeatherField.builder()
                        .low(low.get(FIELD_RNR))
                        .average(calculatedAverage.get(FIELD_RNR))
                        .high(high.get(FIELD_RNR))
                        .build())
                    .windDirection(FetchedWeatherField.builder()
                        .low(low.get(FIELD_WDR))
                        .average(calculatedAverage.get(FIELD_WDR))
                        .high(high.get(FIELD_WDR))
                        .build())
                    .wind(FetchedWeatherField.builder()
                        .low(low.get(FIELD_WND))
                        .average(calculatedAverage.get(FIELD_WND))
                        .high(high.get(FIELD_WND))
                        .build())
                    .gust(FetchedWeatherField.builder()
                        .low(low.get(FIELD_GST))
                        .average(calculatedAverage.get(FIELD_GST))
                        .high(high.get(FIELD_GST))
                        .build())
                    .humidity(FetchedWeatherField.builder()
                        .low(low.get(FIELD_HMT))
                        .average(calculatedAverage.get(FIELD_HMT))
                        .high(high.get(FIELD_HMT))
                        .build())
                    .pressure(FetchedWeatherField.builder()
                        .low(low.get(FIELD_PRS))
                        .average(calculatedAverage.get(FIELD_PRS))
                        .high(high.get(FIELD_PRS))
                        .build())
                    .uvRadiation(FetchedWeatherField.builder()
                        .low(low.get(FIELD_UVI))
                        .average(calculatedAverage.get(FIELD_UVI))
                        .high(high.get(FIELD_UVI))
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
        // Sleep for 2 seconds to comply with API guidelines (of 1 request per second)
        Thread.sleep(Duration.ofSeconds(2));
    }

    private void processValue(
            Map<String, Double> low,
            Map<String, List<Double>> average,
            Map<String, Double> high,
            String field,
            double value
    ) {
        low.merge(field, value, Math::min);
        average.computeIfAbsent(field, k -> new ArrayList<>()).add(value);
        high.merge(field, value, Math::max);
    }

    private Map<String, Double> getCalculatedAverage(Map<String, List<Double>> average) {
        Map<String, Double> calculatedAverage = average.entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> {
                List<Double> vals = e.getValue();
                if (vals == null || vals.isEmpty()) {
                    return 0.0;
                }
                double avg = vals.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                return Conversions.roundToOneDecimal(avg);
            },
            (a, b) -> a,
            LinkedHashMap::new
        ));
        return calculatedAverage;
    }

}
