package mywild.wildweather.domain.weather.schedulers;

import java.nio.file.Path;

public interface Utils {

    static String getStationName(Path path) {
        var stationPath = path.getParent();
        if (stationPath == null) {
            return "UNKNOWN";
        }
        var stationName = stationPath.getFileName();
        if (stationName == null) {
            return "UNKNOWN";
        }
        return stationName.toString();
    }

}
