package mywild.wildweather.domain.weather.schedulers.csv;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import mywild.wildweather.domain.weather.data.WeatherRepository;

public class WeatherCsvSchedulerTest {

    @AfterEach
    void cleanup() throws Exception {
        Field isRunningField = WeatherCsvScheduler.class.getDeclaredField("IS_RUNNING");
        isRunningField.setAccessible(true);
        AtomicBoolean isRunning = (AtomicBoolean) isRunningField.get(null);
        isRunning.set(false);
        WeatherCsvScheduler.clearProcessedFiles();
    }

    @Test
    void processCsvFiles_callsSummaryAndFullProcessors() throws Exception {
        Path root = Files.createTempDirectory("csv-root-");
        Path csv = root.resolve("some.csv");
        Files.writeString(csv, "a,b,c\n1,2,3\n");

        WeatherCsvScheduler scheduler = new WeatherCsvScheduler();

        ProcessSummaryFiles mockSummary = Mockito.mock(ProcessSummaryFiles.class);
        ProcessFullFiles mockFull = Mockito.mock(ProcessFullFiles.class);
        WeatherRepository mockRepo = Mockito.mock(WeatherRepository.class);

        Field folderField = WeatherCsvScheduler.class.getDeclaredField("csvRootFolder");
        folderField.setAccessible(true);
        folderField.set(scheduler, root.toString());

        Field fSummary = WeatherCsvScheduler.class.getDeclaredField("processSummaryFiles");
        fSummary.setAccessible(true);
        fSummary.set(scheduler, mockSummary);
        Field fFull = WeatherCsvScheduler.class.getDeclaredField("processFullFiles");
        fFull.setAccessible(true);
        fFull.set(scheduler, mockFull);
        Field fRepo = WeatherCsvScheduler.class.getDeclaredField("repo");
        fRepo.setAccessible(true);
        fRepo.set(scheduler, mockRepo);

        when(mockSummary.processAllSummaryFiles(ArgumentMatchers.any()))
                .thenReturn(Collections.emptyList());

        assertFalse(scheduler.isRunning());
        scheduler.processCsvFiles();

        verify(mockSummary).processAllSummaryFiles(ArgumentMatchers.any());
        verify(mockFull).processAllFineScaleFiles(ArgumentMatchers.anyList());
        assertFalse(scheduler.isRunning());
    }

    @Test
    void processCsvFiles_returnsImmediatelyWhenAlreadyRunning() throws Exception {
        WeatherCsvScheduler scheduler = new WeatherCsvScheduler();

        Field isRunningField = WeatherCsvScheduler.class.getDeclaredField("IS_RUNNING");
        isRunningField.setAccessible(true);
        AtomicBoolean isRunning = (AtomicBoolean) isRunningField.get(null);
        isRunning.set(true);

        ProcessSummaryFiles mockSummary = Mockito.mock(ProcessSummaryFiles.class);
        ProcessFullFiles mockFull = Mockito.mock(ProcessFullFiles.class);
        Field fSummary = WeatherCsvScheduler.class.getDeclaredField("processSummaryFiles");
        fSummary.setAccessible(true);
        fSummary.set(scheduler, mockSummary);
        Field fFull = WeatherCsvScheduler.class.getDeclaredField("processFullFiles");
        fFull.setAccessible(true);
        fFull.set(scheduler, mockFull);

        scheduler.processCsvFiles();

        verify(mockSummary, never()).processAllSummaryFiles(ArgumentMatchers.any());
        verify(mockFull, never()).processAllFineScaleFiles(ArgumentMatchers.anyList());

        isRunning.set(false);
        assertTrue(!scheduler.isRunning());
    }
    
    @Test
    void clearProcessedFiles_shouldEmptyTheList() {
        WeatherCsvScheduler.markFileAsProcessed("test1.csv");
        WeatherCsvScheduler.markFileAsProcessed("test2.csv");
        
        WeatherCsvScheduler.clearProcessedFiles();
        
        assertFalse(WeatherCsvScheduler.hasFileBeenProcessed("test1.csv"));
        assertFalse(WeatherCsvScheduler.hasFileBeenProcessed("test2.csv"));
    }

    @Test
    void hasFileBeenProcessed_shouldReturnTrueForProcessedFiles() {
        WeatherCsvScheduler.markFileAsProcessed("test.csv");
        
        assertTrue(WeatherCsvScheduler.hasFileBeenProcessed("test.csv"));
        assertFalse(WeatherCsvScheduler.hasFileBeenProcessed("other.csv"));
    }

    @Test 
    void markFileAsProcessed_shouldAddFileToProcessedList() {
        String testFile = "newfile.csv";
        assertFalse(WeatherCsvScheduler.hasFileBeenProcessed(testFile));
        
        WeatherCsvScheduler.markFileAsProcessed(testFile);
        
        assertTrue(WeatherCsvScheduler.hasFileBeenProcessed(testFile));
    }

    @Test
    void resetProcessedCsvFiles_shouldClearListAndRepository() throws Exception {
        WeatherCsvScheduler scheduler = new WeatherCsvScheduler();
        WeatherRepository mockRepo = Mockito.mock(WeatherRepository.class);
        
        Field fRepo = WeatherCsvScheduler.class.getDeclaredField("repo");
        fRepo.setAccessible(true);
        fRepo.set(scheduler, mockRepo);

        WeatherCsvScheduler.markFileAsProcessed("test.csv");
        scheduler.resetAllProcessedCsvFiles();

        verify(mockRepo).deleteAll();
        assertFalse(WeatherCsvScheduler.hasFileBeenProcessed("test.csv"));
    }

}
