package mywild.wildweather.domain.weather.schedulers.csv;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import mywild.wildweather.domain.weather.data.WeatherRepository;
import mywild.wildweather.domain.weather.data.entity.WeatherCategory;
import mywild.wildweather.domain.weather.data.entity.WeatherEntity;

public class ProcessFullFilesTest {

    @Test
    void processAllFineScaleFiles_updatesMissingAndRecordsProcessed() throws Exception {
        Path root = Files.createTempDirectory("proc-full-root-");
        Path station = root.resolve("stationA");
        Files.createDirectories(station);
        Path csv = station.resolve("h00.csv");

        String header = "Date,Value\n";
        String row1 = "2025-11-07T00:00:00Z,1\n";
        String row2 = "2025-11-07T00:05:00Z,2\n";
        Files.writeString(csv, header + row1 + row2, StandardCharsets.UTF_8);

        ProcessFullFiles proc = new ProcessFullFiles();
        WeatherRepository mockRepo = mock(WeatherRepository.class);

        WeatherEntity existing = WeatherEntity.builder().station("stationA").date(java.time.LocalDate.of(2025, 11, 7))
                .category(WeatherCategory.A).build();
        when(mockRepo.findAllByDateAndStationOrderByDateAscCategoryAsc(existing.getDate(), "stationA"))
                .thenReturn(List.of(existing));

        when(mockRepo.findStations()).thenReturn(List.of());

        var f = ProcessFullFiles.class.getDeclaredField("repo");
        f.setAccessible(true);
        f.set(proc, mockRepo);

        WeatherCsvScheduler.clearProcessedFiles();

        proc.processAllFineScaleFiles(List.of(csv));

        String csvName = CsvUtils.getCsvName(csv);
        assertTrue(WeatherCsvScheduler.hasFileBeenProcessed(csvName));

        verify(mockRepo).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

}
