package mywild.wildweather.domain.weather.schedulers.api;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    private static final String SUMMARY_CSV_PREFIX = "{SOURCE}-high-lows-details-";

    private static final DateTimeFormatter CSV_NAME_DATE_FORMAT =  DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final DateTimeFormatter DATE_FIELD_FORMAT =  DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static Path getCsvPath(String source, Path parentPath, LocalDate startDate, LocalDate endDate) {
        var csvStartDateStamp = startDate.format(CSV_NAME_DATE_FORMAT);
        var csvEndDateStamp = endDate != null ? ("-" + endDate.format(CSV_NAME_DATE_FORMAT)) : "";
        return parentPath.resolve(SUMMARY_CSV_PREFIX.replace("{SOURCE}", source)
            + csvStartDateStamp + csvEndDateStamp + ".csv");
    }
    
    public static void writeSingleDayCsvFile(
            Path path,
            LocalDate date,
            List<Double> averages,
            List<Double> highs,
            List<Double> lows
    ) {
        log.debug("Writing CSV file: {}", path);
        if (!Files.exists(path)) {
            try (
                FileWriter writer = new FileWriter(path.toFile(), Charset.defaultCharset());
                CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(CSV_HEADERS).get())
            ) {
                printer.printRecord(Stream.of(
                    Stream.of("Average", date.format(DATE_FIELD_FORMAT)),
                    averages.subList(0, 3).stream(),
                    Stream.of(Conversions.degreesToDirection(averages.get(3))),
                    averages.subList(4, averages.size()).stream()
                ).flatMap(s -> s).toArray());
                printer.printRecord(Stream.of(
                    Stream.of("High", date.format(DATE_FIELD_FORMAT)),
                    highs.subList(0, 3).stream(),
                    Stream.of(""),
                    highs.subList(4, highs.size()).stream()
                ).flatMap(s -> s).toArray());
                printer.printRecord(Stream.of(
                    Stream.of("Low", date.format(DATE_FIELD_FORMAT)),
                    lows.subList(0, 3).stream(),
                    Stream.of(""),
                    lows.subList(4, lows.size()).stream()
                ).flatMap(s -> s).toArray());
            }
            catch (IOException ex) {
                log.error(ex.getMessage(), ex);
            }
        }
        else {
            log.warn("   CSV file already exists : {}", path);
        }
    }

    public static void writeMultiDayCsvFile(
            Path path,
            List<LocalDate> dates,
            List<List<Double>> averages,
            List<List<Double>> highs,
            List<List<Double>> lows
    ) {
        log.debug("Writing CSV file: {}", path);
        try (
            FileWriter writer = new FileWriter(path.toFile(), Charset.defaultCharset());
            CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(CSV_HEADERS).get())
        ) {
            for (int i = 0; i < dates.size(); i++) {
                var dateValue = dates.get(i);
                var lowValues = lows.get(i);
                var averageValues = averages.get(i);
                var highValues = highs.get(i);
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

}
