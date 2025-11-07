package mywild.wildweather.domain.weather.schedulers.csv;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.BufferedReader;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.List;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;

public class CsvUtilsTest {

    @Test
    void getHeadersTransformsAndNormalizes() throws Exception {
        String csv = "\"\",Outdoor Temperature (°C),\"Hourly Rain\",,Wind (m/s)\n" + "1,12.3,0.0,,5\n";
        BufferedReader reader = new BufferedReader(new StringReader(csv));

        String[] headers = CsvUtils.getHeaders(reader);

        assertArrayEquals(new String[] { "COL0", "Outdoor Temperature", "Rain Rate", "COL3", "Wind" }, headers);
    }

    @Test
    void getRecordsParsesDataRowsUsingTransformedHeaders() throws Exception {
        String csv = "\"\",Outdoor Temperature (°C),\"Hourly Rain\",,Wind (m/s)\n" + "1,12.3,0.0,,5\n";
        BufferedReader reader = new BufferedReader(new StringReader(csv));

        String[] headers = CsvUtils.getHeaders(reader);
        List<CSVRecord> records = CsvUtils.getRecords(reader, headers);

        assertEquals(1, records.size());
        CSVRecord r = records.get(0);
        assertEquals("12.3", r.get("Outdoor Temperature"));
        assertEquals("0.0", r.get("Rain Rate"));
        assertEquals("", r.get("COL3"));
    }

    @Test
    void getCsvNameUsesParentDirectory() throws Exception {
        Path p = Path.of("stationA", "sample.csv");
        String name = CsvUtils.getCsvName(p);
        assertEquals("stationA -> sample.csv", name);
    }

}
