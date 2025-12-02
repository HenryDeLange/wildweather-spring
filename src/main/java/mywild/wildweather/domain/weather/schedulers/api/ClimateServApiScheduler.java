package mywild.wildweather.domain.weather.schedulers.api;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import mywild.climateserv.openapi.client.api.ClimateServApi;
import mywild.wildweather.domain.weather.data.WeatherRepository;

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
 */

@Slf4j
@Service
public class ClimateServApiScheduler {

    private final int START_YEAR = 1981;

    public static final String CS_CSV_PREFIX = "api-climateserv-chirps";

    private static final AtomicBoolean IS_RUNNING = new AtomicBoolean(false);

    @Value("${mywild.csv.folder}")
    private String csvRootFolder;

    @Autowired
    private ClimateServApi api;

    @Autowired
    private WeatherRepository repo;

    // @Scheduled(cron = "0 0 1 * * *") // Run at 1AM
    // void scheduledApiProcessing() {
    //     processApiData(false);
    // }

    public boolean isRunning() {
        return IS_RUNNING.get();
    }

    @SuppressWarnings("null")
    @Async
    public void processApiData(boolean fetchAllData) {
        if (!IS_RUNNING.compareAndSet(false, true)) {
            log.warn("Already busy processing ClimateSERV data... The new request will be ignored.");
            return;
        }
        // try (Stream<Path> paths = Files.walk(Paths.get(csvRootFolder))) {
        //     log.info("********************************************");
        //     log.info("Fetching ClimateSERV API data");
        //     log.info("********************************************");
        //     List<Path> stationIdFiles = paths
        //         .filter(Files::isRegularFile)
        //         .filter(path -> path.toString().endsWith(CS_CSV_PREFIX + "-station-id.txt"))
        //         .toList();
        //     for (var stationIdPath : stationIdFiles) {
        //         var station = Utils.getStationName(stationIdPath);
        //         LocalDate mostRecentDatabaseDate = repo.findTopDateByStation(station);
        //         var readRecords = 0;
        //         var processedMonths = 0;
        //         var consecutiveEmptyResponses = 0;
        //         try (var reader = Files.newBufferedReader(stationIdPath)) {
        //             var stationId = reader.readLine();
        //             if (stationId != null && stationId.equalsIgnoreCase("SKIP")) {
        //                 log.info("Skipping Weather Underground API fetching for : {}", station);
        //                 continue;
        //             }
        //             var apiFileContent = reader.readAllLines();
        //             List<String> badDays = null;
        //             if (apiFileContent.size() > 2) {
        //                 badDays = List.of(apiFileContent.get(2).split(",\\s*"));
        //             }
        //             log.info("----------------");
        //             log.info("Processing Weather Underground API : {} -> {}", stationId, station);
        //             LocalDate currentDate = LocalDate.now();
        //             LocalDate apiEndDate = currentDate.minusDays(1); // Yesterday midnight
        //             LocalDate apiStarDate = LocalDate.now().withDayOfMonth(1); // Start of the current month
        //             do {
        //                 var summaryCsvPath = CsvWriter.getCsvPath(WU_CSV_PREFIX,
        //                     stationIdPath.getParent(), apiStarDate, apiEndDate.with(TemporalAdjusters.lastDayOfMonth()));
        //                 // Only generate files for observation months that are new, or for the current month
        //                 if (fetchAllData
        //                         || apiEndDate.equals(currentDate.minusDays(1))
        //                         || (summaryCsvPath != null && !Files.exists(summaryCsvPath))) {
        //                     // Fetch the API data
        //                     log.info("   Fetching data for {} : {} to {}", stationId, apiStarDate.format(API_DATE_FORMAT), apiEndDate.format(API_DATE_FORMAT));
        //                     // Example URL:
        //                     // https://api.weather.com/v2/pws/history/daily?startDate=20250101&endDate=20250131&format=json&units=m&numericPrecision=decimal&stationId=<STATION>&apiKey=<APIKEY>
        //                     var httpData = api.getDailyWithHttpInfo(stationId, FormatEnum.JSON, UnitsEnum.METRIC, 
        //                         null, apiStarDate.format(API_DATE_FORMAT), apiEndDate.format(API_DATE_FORMAT),
        //                         NumericPrecisionEnum.DECIMAL);
        //                     var data = httpData.getData();
        //                     if (data != null && data.getObservations() != null && !data.getObservations().isEmpty()) {
        //                         // Calculate the daily low/ave/high values
        //                         List<LocalDate> dates = new ArrayList<>();
        //                         List<List<Double>> lows = new ArrayList<>();
        //                         List<List<Double>> averages = new ArrayList<>();
        //                         List<List<Double>> highs = new ArrayList<>();
        //                         for (var dataRecord : data.getObservations()) {
        //                             List<Double> low = new ArrayList<>();
        //                             List<Double> average = new ArrayList<>();
        //                             List<Double> high = new ArrayList<>();
        //                             var recordDate = dataRecord.getObsTimeUtc().toLocalDate();
        //                             if (badDays == null || !badDays.contains(dataRecord.getObsTimeUtc().toLocalDate().format(BAD_DAYS_DATE_FORMAT))) {
        //                                 low     .add(dataRecord.getMetric().getTempLow());
        //                                 average .add(dataRecord.getMetric().getTempAvg());
        //                                 high    .add(dataRecord.getMetric().getTempHigh());
        //                                 low     .add(dataRecord.getMetric().getWindspeedLow());
        //                                 average .add(dataRecord.getMetric().getWindspeedAvg());
        //                                 high    .add(dataRecord.getMetric().getWindspeedHigh());
        //                                 low     .add(dataRecord.getMetric().getWindgustLow());
        //                                 average .add(dataRecord.getMetric().getWindgustAvg());
        //                                 high    .add(dataRecord.getMetric().getWindgustHigh());
        //                                 low     .add(dataRecord.getWinddirAvg());
        //                                 average .add(dataRecord.getWinddirAvg());
        //                                 high    .add(dataRecord.getWinddirAvg());
        //                                 low     .add(dataRecord.getMetric().getPrecipRate());
        //                                 average .add(dataRecord.getMetric().getPrecipRate());
        //                                 high    .add(dataRecord.getMetric().getPrecipRate());
        //                                 low     .add(dataRecord.getMetric().getPrecipTotal());
        //                                 average .add(dataRecord.getMetric().getPrecipTotal());
        //                                 high    .add(dataRecord.getMetric().getPrecipTotal());
        //                                 low     .add(dataRecord.getMetric().getPressureMin());
        //                                 average .add(dataRecord.getMetric().getPressureTrend());
        //                                 high    .add(dataRecord.getMetric().getPressureMax());
        //                                 low     .add(dataRecord.getHumidityLow());
        //                                 average .add(dataRecord.getHumidityAvg());
        //                                 high    .add(dataRecord.getHumidityHigh());
        //                                 low     .add(0.0);
        //                                 average .add(dataRecord.getUvHigh() != null ? dataRecord.getUvHigh() / 2.0 : null);
        //                                 high    .add(dataRecord.getUvHigh());
        //                                 readRecords++;
        //                                 dates.add(recordDate);
        //                                 lows.add(low);
        //                                 averages.add(average);
        //                                 highs.add(high);
        //                             }
        //                             else {
        //                                 log.info("       Skip known bad day : {}", recordDate);
        //                             }
        //                         }
        //                         // Save the record to a CSV file
        //                         if (readRecords >= 1) {
        //                             CsvWriter.writeMultiDayCsvFile(
        //                                 summaryCsvPath, 
        //                                 dates,
        //                                 averages,
        //                                 highs,
        //                                 lows);
        //                         }
        //                         consecutiveEmptyResponses = 0;
        //                     }
        //                     else {
        //                         consecutiveEmptyResponses++;
        //                         log.info("       No data returned (count: {}) : {} - {}",
        //                             consecutiveEmptyResponses, 
        //                             httpData.getStatusCode(), 
        //                             httpData.getData() == null ? "Response was null" 
        //                                 : httpData.getData().getObservations() == null ? "Observations was null"
        //                                     : "Observations was empty");
        //                     }
        //                     processedMonths++;
        //                     // Sleep for 1 seconds to not spam the api too much
        //                     Thread.sleep(Duration.ofSeconds(1));
        //                 }
        //                 else {
        //                     log.debug("   Skip {} - Found CSV file : {}",
        //                         apiEndDate, 
        //                         summaryCsvPath.getParent().getParent().relativize(summaryCsvPath).toString());
        //                 }
        //                 apiStarDate = apiStarDate.minusMonths(1);
        //                 apiEndDate = apiStarDate.plusMonths(1).minusDays(1);
        //             }
        //             while (
        //                 (consecutiveEmptyResponses < STOP_AT_EMPTY_RESPONSES)
        //                 && (fetchAllData || mostRecentDatabaseDate == null
        //                     || mostRecentDatabaseDate.withDayOfMonth(1).isBefore(apiStarDate))
        //             );
        //         }
        //         catch (InterruptedException ex) {
        //             log.warn("Processing interrupted!", ex);
        //         }
        //         finally {
        //             log.info("   Read Records   : {}", readRecords);
        //             log.info("   Processed Months : {}", processedMonths);
        //         }
        //     }
        // }
        // catch (IOException ex) {
        //     log.error(ex.getMessage(), ex);
        // }
        // finally {
        //     log.info("********************************************");
        //     log.info("Processed all ClimateSERV API data");
        //     log.info("********************************************");
        //     IS_RUNNING.set(false);
        // }
    }

}
