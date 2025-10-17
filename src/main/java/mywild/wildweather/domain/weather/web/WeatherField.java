package mywild.wildweather.domain.weather.web;

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

}
