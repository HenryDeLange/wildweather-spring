package mywild.wildweather.domain.weather.schedulers.csv;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeast;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import mywild.wildweather.domain.weather.data.WeatherRepository;
import mywild.wildweather.domain.weather.data.entity.WeatherCategory;

public class ProcessSummaryFilesTest {

    @Test
    void processAllSummaryFiles_savesNewRecordForSummaryCsv() throws Exception {
        Path root = Files.createTempDirectory("proc-sum-root-");
        Path station = root.resolve("stationA");
        Files.createDirectories(station);
        Path csv = station.resolve("ambient-weather-high-lows-details-20251107.csv");

        String header = ",Date,Outdoor Temperature,Wind Speed,Max Daily Gust,Wind Direction,Rain Rate,Daily Rain,Relative Pressure,Humidity,Ultra-Violet Radiation Index\n";
        String row = "A,2025-11-07,12.3,5.0,8.0,90.0,0.1,1.2,1013.2,55.0,0.0\n";
        Files.writeString(csv, header + row, StandardCharsets.UTF_8);

        ProcessSummaryFiles proc = new ProcessSummaryFiles();
        WeatherRepository mockRepo = mock(WeatherRepository.class);

        when(mockRepo.findByDateAndStationAndCategory(LocalDate.of(2025, 11, 7), "stationA", WeatherCategory.A))
                .thenReturn(null);

        var f = ProcessSummaryFiles.class.getDeclaredField("repo");
        f.setAccessible(true);
        f.set(proc, mockRepo);

        WeatherCsvScheduler.clearProcessedFiles();

        List<Path> fine = proc.processAllSummaryFiles(Stream.of(csv));

        assertTrue(fine.isEmpty());

        verify(mockRepo, atLeast(1)).save(org.mockito.ArgumentMatchers.any());
    }

}
