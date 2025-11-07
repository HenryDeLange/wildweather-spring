package mywild.wildweather.domain.weather.schedulers.api;

// assertions used via Mockito verifications
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
import org.junit.jupiter.api.Test;
import mywild.ambientweather.openapi.client.model.DeviceData;

public class WeatherApiSchedulerIntegrationTest {

    @Test
    void testProcessApiDataWritesCsv() throws Exception {
        Path tempDir = Files.createTempDirectory("weather-integration-test-");

        // Create station folder and macAddress.txt
        Path stationDir = tempDir.resolve("station1");
        Files.createDirectories(stationDir);
        Path macFile = stationDir.resolve("macAddress.txt");
        String macAddress = "AA:BB:CC:DD";
        Files.writeString(macFile, macAddress);

        // Prepare scheduler and inject properties/mocks via reflection
        WeatherApiScheduler scheduler = new WeatherApiScheduler();

        // set csvRootFolder
        var csvField = WeatherApiScheduler.class.getDeclaredField("csvRootFolder");
        csvField.setAccessible(true);
        csvField.set(scheduler, tempDir.toString());

        // mock AmbientWeatherApi and WeatherRepository
        var api = mock(mywild.ambientweather.openapi.client.api.AmbientWeatherApi.class);
        var repo = mock(mywild.wildweather.domain.weather.data.WeatherRepository.class);

        // repo returns an old date so processing will run
        when(repo.findTopDateByStation(anyString())).thenReturn(LocalDate.now().minusDays(1));

        // Build a DeviceData record matching today's processing date used by scheduler
        OffsetDateTime apiEndDate = LocalDate.now(ZoneOffset.UTC)
                .atStartOfDay()
                .atOffset(ZoneOffset.UTC)
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

        var apiField = WeatherApiScheduler.class.getDeclaredField("api");
        apiField.setAccessible(true);
        apiField.set(scheduler, api);

        var repoField = WeatherApiScheduler.class.getDeclaredField("repo");
        repoField.setAccessible(true);
        repoField.set(scheduler, repo);

        // Ensure IS_RUNNING is false before execution
        var isRunningField = WeatherApiScheduler.class.getDeclaredField("IS_RUNNING");
        isRunningField.setAccessible(true);
        java.util.concurrent.atomic.AtomicBoolean flag = (java.util.concurrent.atomic.AtomicBoolean) isRunningField
                .get(null);
        flag.set(false);

        // Run the processing but mock CsvWriter.writeCsvFile (static) so test doesn't depend on CsvWriter internals
        try (org.mockito.MockedStatic<CsvWriter> csvMock = org.mockito.Mockito.mockStatic(CsvWriter.class)) {
            // Provide a concrete CSV path so the scheduler doesn't depend on CsvWriter.getCsvPath implementation
            csvMock.when(() -> CsvWriter.getCsvPath(any(), any())).thenReturn(stationDir.resolve("out.csv"));
            csvMock.when(() -> CsvWriter.writeCsvFile(any(), any(), any(), any(), any())).thenAnswer(i -> null);

            scheduler.processApiData();

            // verify CsvWriter.writeCsvFile was invoked at least once
            csvMock.verify(() -> CsvWriter.writeCsvFile(any(), any(), any(), any(), any()));
        }
    }

}
