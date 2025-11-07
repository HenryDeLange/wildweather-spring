package mywild.wildweather.domain.weather.logic;

public interface Conversions {

    static double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    static Double directionToDegrees(String direction) {
        if (direction == null || direction.isBlank())
            return 0.0;
        switch (direction) {
            case "N":
                return 0.0;
            case "NNE":
                return 22.5;
            case "NE":
                return 45.0;
            case "ENE":
                return 67.5;
            case "E":
                return 90.0;
            case "ESE":
                return 112.5;
            case "SE":
                return 135.0;
            case "SSE":
                return 157.5;
            case "S":
                return 180.0;
            case "SSW":
                return 202.5;
            case "SW":
                return 225.0;
            case "WSW":
                return 247.5;
            case "W":
                return 270.0;
            case "WNW":
                return 292.5;
            case "NW":
                return 315.0;
            case "NNW":
                return 337.5;
            default:
                return Double.NEGATIVE_INFINITY;
        }
    }

}
