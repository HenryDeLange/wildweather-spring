package mywild.wildweather.domain.weather.schedulers.api;

final class Conversions {

    private Conversions() {
        // prevent instantiation
    }

    static final String[] DIRECTIONS = {
        "N", "NNE", "NE", "ENE",
        "E", "ESE", "SE", "SSE",
        "S", "SSW", "SW", "WSW",
        "W", "WNW", "NW", "NNW"
    };

    static double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    static double fahToCel(double fahrenheit) {
        return roundToOneDecimal((fahrenheit - 32) * 5.0 / 9.0);
    }

    static double mphToKmh(double mph) {
        return roundToOneDecimal(mph * 1.609344);
    }

    static double inToMm(double inches) {
        return roundToOneDecimal(inches * 25.4);
    }

    static double inHgToHpa(double inHg) {
        return roundToOneDecimal(inHg * 33.8639);
    }

    static String degreesToDirection(Double degrees) {
        if (degrees == null || degrees.isNaN()) {
            return null;
        }
        double cleanDegrees = ((degrees % 360) + 360) % 360;
        int index = (int) Math.floor((cleanDegrees + 11.25) / 22.5) % DIRECTIONS.length;
        return DIRECTIONS[index];
    }

}
