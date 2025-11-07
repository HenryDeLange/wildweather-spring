package mywild.wildweather.domain.weather.schedulers.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class CsvWriterTest {
    @Test
    void writeCsvFileCreatesSummaryWithExpectedRows() throws Exception {
        Path parent = Files.createTempDirectory("csv-writer-parent-");

        LocalDateTime dateTime = LocalDateTime.of(2025, 11, 7, 0, 0);
        Path csvPath = CsvWriter.getCsvPath(parent, dateTime);

        List<Double> averages = Arrays.asList(12.3, 5.0, 8.1, 90.0, 0.2, 1.0, 1013.2, 55.0, 0.0);
        List<Double> highs = Arrays.asList(14.0, 7.0, 9.0, 95.0, 0.5, 2.0, 1015.0, 60.0, 0.0);
        List<Double> lows = Arrays.asList(10.0, 3.0, 6.2, 85.0, 0.0, 0.0, 1010.0, 50.0, 0.0);

        if (Files.exists(csvPath)) {
            Files.delete(csvPath);
        }

        CsvWriter.writeCsvFile(csvPath, LocalDate.of(2025, 11, 7), averages, highs, lows);

        List<String> lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
        assertEquals(true, lines.size() >= 4);
        boolean hasAverage = lines.stream().anyMatch(s -> s.contains("Average"));
        boolean hasHigh = lines.stream().anyMatch(s -> s.contains("High"));
        boolean hasLow = lines.stream().anyMatch(s -> s.contains("Low"));
        assertEquals(true, hasAverage);
        assertEquals(true, hasHigh);
        assertEquals(true, hasLow);
    }
}
