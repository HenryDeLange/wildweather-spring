package mywild.wildweather.domain.weather.schedulers.csv;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class WeatherCsvSchedulerIntegrationTest {

    @AfterEach
    void cleanup() throws Exception {
        Field isRunningField = WeatherCsvScheduler.class.getDeclaredField("IS_RUNNING");
        isRunningField.setAccessible(true);
        AtomicBoolean isRunning = (AtomicBoolean) isRunningField.get(null);
        isRunning.set(false);
        WeatherCsvScheduler.PROCESSED_CSV_FILES.clear();
    }

    @Test
    void schedulerWalksFolderAndInvokesProcessors() throws Exception {
        Path root = Files.createTempDirectory("csv-integ-root-");

        Path station = root.resolve("stationA");
        Files.createDirectories(station);
        Path c1 = station.resolve("a.csv");
        Path c2 = station.resolve("b.csv");
        Files.writeString(c1, "h1\n1\n");
        Files.writeString(c2, "h2\n2\n");

        WeatherCsvScheduler scheduler = new WeatherCsvScheduler();

        ProcessSummaryFiles mockSummary = Mockito.mock(ProcessSummaryFiles.class);
        ProcessFullFiles mockFull = Mockito.mock(ProcessFullFiles.class);
        Field folderField = WeatherCsvScheduler.class.getDeclaredField("csvRootFolder");
        folderField.setAccessible(true);
        folderField.set(scheduler, root.toString());

        Field fSummary = WeatherCsvScheduler.class.getDeclaredField("processSummaryFiles");
        fSummary.setAccessible(true);
        fSummary.set(scheduler, mockSummary);
        Field fFull = WeatherCsvScheduler.class.getDeclaredField("processFullFiles");
        fFull.setAccessible(true);
        fFull.set(scheduler, mockFull);

        Mockito.when(mockSummary.processAllSummaryFiles(ArgumentMatchers.any())).thenAnswer(invocation -> {
            Stream<Path> paths = invocation.getArgument(0);
            return paths.filter(p -> p.getFileName().toString().contains("a")).toList();
        });

        scheduler.processCsvFiles();

        verify(mockSummary).processAllSummaryFiles(ArgumentMatchers.any());
        verify(mockFull).processAllFineScaleFiles(ArgumentMatchers.anyList());

        assertFalse(scheduler.isRunning());
    }

}
