package mywild.wildweather.domain.weather.schedulers.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import mywild.ambientweather.openapi.client.api.AmbientWeatherApi;
import mywild.ambientweather.openapi.client.model.DeviceData;
import mywild.wildweather.domain.weather.data.WeatherRepository;

public class AmbientWeatherApiSchedulerIntegrationTest {

    @Test
    void testProcessApiDataWritesCsv() throws Exception {
        Path tempDir = Files.createTempDirectory("weather-integration-test-");

        Path stationDir = tempDir.resolve("station1");
        Files.createDirectories(stationDir);
        Path macFile = stationDir.resolve("macAddress.txt");
        String macAddress = "AA:BB:CC:DD";
        Files.writeString(macFile, macAddress);

        AmbientWeatherApiScheduler scheduler = new AmbientWeatherApiScheduler();

        var csvField = AmbientWeatherApiScheduler.class.getDeclaredField("csvRootFolder");
        csvField.setAccessible(true);
        csvField.set(scheduler, tempDir.toString());

        var api = mock(AmbientWeatherApi.class);
        var repo = mock(WeatherRepository.class);

        when(repo.findTopDateByStation(anyString())).thenReturn(LocalDate.now().minusDays(1));

        OffsetDateTime apiEndDate = LocalDate.now(ZoneOffset.UTC).atStartOfDay().atOffset(ZoneOffset.UTC)
                .minusSeconds(1);
        DeviceData rec = new DeviceData();
        rec.setDate(apiEndDate);
        rec.setTempf(68.0);
        rec.setWindspeedmph(5.0);
        rec.setWindgustmph(7.0);
        rec.setWinddir(180);
        rec.setHourlyrainin(0.0);
        rec.setDailyrainin(0.0);
        rec.setBaromrelin(29.92);
        rec.setHumidity(50);
        rec.setUv(1.0);

        when(api.getDeviceData(eq(macAddress), any(OffsetDateTime.class), anyInt())).thenReturn(List.of(rec));

        var apiField = AmbientWeatherApiScheduler.class.getDeclaredField("api");
        apiField.setAccessible(true);
        apiField.set(scheduler, api);

        var repoField = AmbientWeatherApiScheduler.class.getDeclaredField("repo");
        repoField.setAccessible(true);
        repoField.set(scheduler, repo);

        var isRunningField = AmbientWeatherApiScheduler.class.getDeclaredField("IS_RUNNING");
        isRunningField.setAccessible(true);
        AtomicBoolean flag = (AtomicBoolean) isRunningField.get(null);
        flag.set(false);

        try (MockedStatic<CsvWriter> csvMock = Mockito.mockStatic(CsvWriter.class)) {
            csvMock.when(() -> CsvWriter.getCsvPath(any(), any())).thenReturn(stationDir.resolve("out.csv"));
            csvMock.when(() -> CsvWriter.writeCsvFile(any(), any(), any(), any(), any())).thenAnswer(i -> null);

            scheduler.processApiData();

            csvMock.verify(() -> CsvWriter.writeCsvFile(any(), any(), any(), any(), any()));
        }
    }

}
