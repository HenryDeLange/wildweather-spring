package mywild.wildweather.domain.weather.schedulers.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import mywild.wildweather.domain.weather.data.WeatherRepository;
import mywild.wildweather.domain.weather.schedulers.Utils;

@RequiredArgsConstructor
@Slf4j
public abstract class AbstractScheduler {

    public static final DateTimeFormatter LOG_DATE_FORMAT =  DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static final DateTimeFormatter BAD_DAYS_DATE_FORMAT =  DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final AtomicBoolean IS_RUNNING = new AtomicBoolean(false);

    private final String name;

    private final String csvPrefix;

    private final AbstractFetcher fetcher;

    @Setter
    private int stopAtEmptyResponses = 2;

    @Value("${mywild.csv.folder}")
    private String csvRootFolder;

    @Autowired
    private WeatherRepository repo;

    public boolean isRunning() {
        return IS_RUNNING.get();
    }
    
    @SuppressWarnings("null")
    @Async
    public void processApiData(boolean fetchAllData) {
        if (!IS_RUNNING.compareAndSet(false, true)) {
            log.warn("Already busy processing {} data...", name);
            log.warn("The new request will be ignored.");
            return;
        }
        try (Stream<Path> paths = Files.walk(Paths.get(csvRootFolder))) {
            log.info("********************************************");
            log.info("Fetching {} API data", name);
            log.info("********************************************");
            List<Path> stationIdFiles = paths
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(csvPrefix + "-station-id.txt"))
                .toList();
            log.info("Found {} stations to process.", stationIdFiles.size());
            for (var stationIdPath : stationIdFiles) {
                var stationName = Utils.getStationName(stationIdPath);
                LocalDate mostRecentDatabaseDate = repo.findTopDateByStation(stationName);
                var apiFetches = 0;
                var readRecords = 0;
                var consecutiveEmptyResponses = 0;
                try (var reader = Files.newBufferedReader(stationIdPath)) {
                    var stationId = reader.readLine();
                    var apiFileContent = reader.readAllLines();
                    List<String> badDays = null;
                    if (apiFileContent.size() > 2) {
                        badDays = List.of(apiFileContent.get(2).split(",\\s*"));
                    }
                    log.info("----------------");
                    log.info("Processing {} API : {} ({})", name, stationName, stationId);
                    var apiStarDate = nextApiStartDate(null);
                    var apiEndDate = nextApiEndDate(null);
                    var currentEndDate = nextApiEndDate(null);
                    do {
                        var summaryCsvPath = CsvWriter.getCsvPath(
                            csvPrefix,
                            stationIdPath.getParent(),
                            apiStarDate,
                            csvEndDate(apiEndDate));
                        // Only generate files for observation periods that are new, as well as regenerate the current period
                        if (fetchAllData
                                || apiEndDate.equals(currentEndDate)
                                || (summaryCsvPath != null && !Files.exists(summaryCsvPath))) {
                            log.info("   Fetching data for {} : {} to {}", stationName, 
                                apiStarDate.format(LOG_DATE_FORMAT), apiEndDate.format(LOG_DATE_FORMAT));
                            var records = fetcher.fetchRecords(stationId, apiStarDate, apiEndDate, badDays);
                            if (records != null && !records.isEmpty()) {
                                readRecords = readRecords  + records.size();
                                // Save the record to a CSV file
                                if (readRecords >= 1) {
                                    CsvWriter.writeCsvFile(summaryCsvPath, records);
                                }
                                consecutiveEmptyResponses = 0;
                            }
                            else {
                                consecutiveEmptyResponses++;
                                log.info("       No data returned (count: {})", consecutiveEmptyResponses);
                            }
                            apiFetches++;
                            fetcher.apiLimitCheck();
                        }
                        else {
                            log.debug("   Skip {} - Found CSV file : {}",
                                apiEndDate, 
                                summaryCsvPath.getParent().getParent().relativize(summaryCsvPath).toString());
                        }
                        apiStarDate = nextApiStartDate(apiStarDate);
                        apiEndDate = nextApiEndDate(apiStarDate);
                    }
                    while (
                        (consecutiveEmptyResponses < stopAtEmptyResponses)
                        && (fetchAllData || mostRecentDatabaseDate == null
                            || shouldFetchMoreRecords(mostRecentDatabaseDate, apiStarDate))
                    );
                }
                catch (InterruptedException ex) {
                    log.warn("Processing interrupted!", ex);
                }
                finally {
                    log.info("   API Fetches  : {}", apiFetches);
                    log.info("   Records Read : {}", readRecords);
                }
            }
        }
        catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
        finally {
            log.info("********************************************");
            log.info("Processed all {} API data", name);
            log.info("********************************************");
            IS_RUNNING.set(false);
        }
    }

    protected abstract LocalDate nextApiStartDate(LocalDate currentStartDate);

    protected abstract LocalDate nextApiEndDate(LocalDate currentStartDate);

    protected abstract LocalDate csvEndDate(LocalDate currentEndDate);

    protected abstract boolean shouldFetchMoreRecords(LocalDate mostRecentDatabaseDate, LocalDate apiStarDate);

}
