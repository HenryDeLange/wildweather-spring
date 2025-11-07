package mywild.wildweather.domain.weather.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import mywild.wildweather.domain.weather.data.entity.WeatherEntity;
import mywild.wildweather.domain.weather.web.dto.WeatherField;

public class WeatherFieldExtractorTest {

    @Test
    void testExtractorsContainExpectedFields() {
        assertTrue(WeatherFieldExtractor.EXTRACTORS.containsKey(WeatherField.TEMPERATURE));
        assertTrue(WeatherFieldExtractor.EXTRACTORS.containsKey(WeatherField.WIND_DIRECTION));
        assertTrue(WeatherFieldExtractor.EXTRACTORS.containsKey(WeatherField.MISSING));
    }

    @Test
    void testWindDirectionExtractorUsesDirectionConversion() {
        WeatherEntity e = mock(WeatherEntity.class);
        when(e.getWindDirection()).thenReturn("NE");

        var extractor = WeatherFieldExtractor.EXTRACTORS.get(WeatherField.WIND_DIRECTION);
        Double degrees = extractor.apply(e);

        assertEquals(45.0, degrees);
    }

}
