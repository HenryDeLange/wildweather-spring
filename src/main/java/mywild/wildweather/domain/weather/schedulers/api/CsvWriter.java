package mywild.wildweather.domain.weather.schedulers.api;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
final public class CsvWriter {

    private CsvWriter() {
        // prevent instantiation
    }

    private static final String[] CSV_HEADERS = {
        "",
        "Date",
        "Outdoor Temperature (°C)",     // 0
        "Wind Speed (km/hr)",           // 1
        "Max Daily Gust (km/hr)",       // 2
        "Wind Direction (°)",           // 3
        "Rain Rate (mm/hr)",            // 4
        "Daily Rain (mm)",              // 5
        "Relative Pressure (hPa)",      // 6
        "Humidity (%)",                 // 7
        "Ultra-Violet Radiation Index"  // 8
    };

    private static final String SUMMARY_CSV_PREFIX = "{SOURCE}-high-lows-";

    public static final DateTimeFormatter CSV_NAME_DATE_FORMAT =  DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final DateTimeFormatter DATE_FIELD_FORMAT =  DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static Path getCsvPath(String source, Path parentPath, LocalDate startDate, LocalDate endDate) {
        var csvStartDateStamp = startDate.format(CSV_NAME_DATE_FORMAT);
        var csvEndDateStamp = endDate != null ? ("-" + endDate.format(CSV_NAME_DATE_FORMAT)) : "";
        return parentPath.resolve(SUMMARY_CSV_PREFIX.replace("{SOURCE}", source)
            + csvStartDateStamp + csvEndDateStamp + ".csv");
    }

    public static void writeCsvFile(
            Path path,
            List<FetchedWeatherRecord> records
    ) {
        log.debug("Writing CSV file: {}", path);
        try (
            FileWriter writer = new FileWriter(path.toFile(), Charset.defaultCharset());
            CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(CSV_HEADERS).get())
        ) {
            for (var record : records) {
                var dateValue = record.getDate();
                var lowValues = getCsvLowValues(record);
                var averageValues = getCsvAverageValues(record);
                var highValues = getCsvHighValues(record);
                printer.printRecord(Stream.of(
                    Stream.of("Average", dateValue.format(DATE_FIELD_FORMAT)),
                    averageValues.subList(0, 3).stream(),
                    Stream.of(Conversions.degreesToDirection(averageValues.get(3))),
                    averageValues.subList(4, averageValues.size()).stream()
                ).flatMap(s -> s).toArray());
                printer.printRecord(Stream.of(
                    Stream.of("High", dateValue.format(DATE_FIELD_FORMAT)),
                    highValues.subList(0, 3).stream(),
                    Stream.of(""),
                    highValues.subList(4, highValues.size()).stream()
                ).flatMap(s -> s).toArray());
                printer.printRecord(Stream.of(
                    Stream.of("Low", dateValue.format(DATE_FIELD_FORMAT)),
                    lowValues.subList(0, 3).stream(),
                    Stream.of(""),
                    lowValues.subList(4, lowValues.size()).stream()
                ).flatMap(s -> s).toArray());
            }
        }
        catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private static List<Double> getCsvLowValues(FetchedWeatherRecord record) {
        return Arrays.asList(
            record.getTemperature() != null ? record.getTemperature().getLow() : null,      // 0
            record.getWind() != null ? record.getWind().getLow() : null,                    // 1
            record.getGust() != null ? record.getGust().getLow() : null,                    // 2
            record.getWindDirection() != null ? record.getWindDirection().getLow() : null,  // 3
            record.getRainRate() != null ? record.getRainRate().getLow() : null,            // 4
            record.getRain() != null ? record.getRain().getLow() : null,                    // 5
            record.getPressure() != null ? record.getPressure().getLow() : null,            // 6
            record.getHumidity() != null ? record.getHumidity().getLow() : null,            // 7
            record.getUvRadiation() != null ? record.getUvRadiation().getLow() : null       // 8
        );
    }

    private static List<Double> getCsvAverageValues(FetchedWeatherRecord record) {
        return Arrays.asList(
            record.getTemperature() != null ? record.getTemperature().getAverage() : null,      // 0
            record.getWind() != null ? record.getWind().getAverage() : null,                    // 1
            record.getGust() != null ? record.getGust().getAverage() : null,                    // 2
            record.getWindDirection() != null ? record.getWindDirection().getAverage() : null,  // 3
            record.getRainRate() != null ? record.getRainRate().getAverage() : null,            // 4
            record.getRain() != null ? record.getRain().getAverage() : null,                    // 5
            record.getPressure() != null ? record.getPressure().getAverage() : null,            // 6
            record.getHumidity() != null ? record.getHumidity().getAverage() : null,            // 7
            record.getUvRadiation() != null ? record.getUvRadiation().getAverage() : null       // 8
        );
    }

    private static List<Double> getCsvHighValues(FetchedWeatherRecord record) {
        return Arrays.asList(
            record.getTemperature() != null ? record.getTemperature().getHigh() : null,     // 0
            record.getWind() != null ? record.getWind().getHigh() : null,                   // 1
            record.getGust() != null ? record.getGust().getHigh() : null,                   // 2
            record.getWindDirection() != null ? record.getWindDirection().getHigh() : null, // 3
            record.getRainRate() != null ? record.getRainRate().getHigh() : null,           // 4
            record.getRain() != null ? record.getRain().getHigh() : null,                   // 5
            record.getPressure() != null ? record.getPressure().getHigh() : null,           // 6
            record.getHumidity() != null ? record.getHumidity().getHigh() : null,           // 7
            record.getUvRadiation() != null ? record.getUvRadiation().getHigh() : null      // 8
        );
    }

}
