package mywild.wildweather.domain.weather.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum WeatherField {
    TEMPERATURE         ("tmp"),
    WIND_SPEED          ("wSp"),
    WIND_MAX            ("wMx"),
    WIND_DIRECTION      ("wDr"),
    RAIN_RATE           ("rRt"),
    RAIN_DAILY          ("rDy"),
    PRESSURE            ("prs"),
    HUMIDITY            ("hmd"),
    UV_RADIATION_INDEX  ("uvI"),
    MISSING             ("mis"),
    ;

    private String key;

    public static WeatherField fromKey(String key) {
        for (WeatherField field : WeatherField.values()) {
            if (field.getKey().equals(key)) {
                return field;
            }
        }
        return null;
    }

}
