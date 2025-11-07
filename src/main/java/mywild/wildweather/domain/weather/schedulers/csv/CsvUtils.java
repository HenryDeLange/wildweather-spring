package mywild.wildweather.domain.weather.schedulers.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import lombok.extern.slf4j.Slf4j;
import mywild.wildweather.domain.weather.schedulers.Utils;

@Slf4j
public class CsvUtils {

    static String getCsvName(Path path) {
        return Utils.getStationName(path) + " -> " + path.getFileName();
    }

    static String[] getHeaders(BufferedReader reader) throws IOException {
        String headerLine = reader.readLine();
        String[] headers = headerLine != null ? headerLine.split(",") : new String[] {};
        for (int i = 0; i < headers.length; i++) {
            var header = headers[i].replace("\"", "");
            if (header.isBlank()) {
                // Replace empty headers with the column index instead
                header = "COL" + i;
            }
            if (header.contains("(")) {
                header = header.substring(0, header.indexOf("(")).trim();
            }
            if (header.equals("Hourly Rain")) {
                header = "Rain Rate";
            }
            headers[i] = header;
        }
        log.trace("Headers: {}", Arrays.toString(headers));
        return headers;
    }

    static List<CSVRecord> getRecords(BufferedReader reader, String[] headers) throws IOException {
        List<CSVRecord> records = CSVFormat.Builder
            .create()
            .setHeader(headers)
            .setSkipHeaderRecord(false) // getHeaders() will already read teh header and move the reader to the first data line
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .get()
            .parse(reader).getRecords();
        log.trace("Records: {}", records.size());
        return records;
    }
    
}
