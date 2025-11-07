package mywild.wildweather.domain.weather.schedulers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

public class UtilsTest {

    @Test
    void getStationName_returnsStationForNestedPath() {
        Path p = Paths.get("root", "stationA", "data.csv");
        assertEquals("stationA", Utils.getStationName(p));
    }

    @Test
    void getStationName_returnsUnknownForNoParent() {
        Path p = Paths.get("file.csv");
        assertEquals("UNKNOWN", Utils.getStationName(p));
    }

    @Test
    void getStationName_returnsUnknownWhenParentIsRoot() {
        FileSystem fs = FileSystems.getDefault();
        Path root = fs.getRootDirectories().iterator().next();
        Path p = root.resolve("file.csv");
        assertEquals("UNKNOWN", Utils.getStationName(p));
    }

}
