package mywild.wildweather.domain.weather.logic;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import mywild.wildweather.domain.weather.data.entity.WeatherEntity;
import mywild.wildweather.domain.weather.web.dto.WeatherField;

final public class WeatherFieldExtractor {

    private WeatherFieldExtractor() {
        // prevent instantiation
    }

    static final Map<WeatherField, Function<WeatherEntity, Double>> EXTRACTORS;
    static {
        Map<WeatherField, Function<WeatherEntity, Double>> fieldMappings = new LinkedHashMap<>();
        fieldMappings.put(WeatherField.TEMPERATURE, WeatherEntity::getTemperature);
        fieldMappings.put(WeatherField.WIND_SPEED, WeatherEntity::getWindSpeed);
        fieldMappings.put(WeatherField.WIND_MAX, WeatherEntity::getWindMax);
        fieldMappings.put(WeatherField.WIND_DIRECTION, e -> Conversions.directionToDegrees(e.getWindDirection()));
        fieldMappings.put(WeatherField.RAIN_RATE, WeatherEntity::getRainRate);
        fieldMappings.put(WeatherField.RAIN_DAILY, WeatherEntity::getRainDaily);
        fieldMappings.put(WeatherField.PRESSURE, WeatherEntity::getPressure);
        fieldMappings.put(WeatherField.HUMIDITY, WeatherEntity::getHumidity);
        fieldMappings.put(WeatherField.UV_RADIATION_INDEX, WeatherEntity::getUvRadiationIndex);
        fieldMappings.put(WeatherField.MISSING, WeatherEntity::getMissing);
        EXTRACTORS = Collections.unmodifiableMap(fieldMappings);
    }
    
}
