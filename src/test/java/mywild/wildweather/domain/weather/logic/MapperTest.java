package mywild.wildweather.domain.weather.logic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import mywild.wildweather.domain.weather.data.entity.WeatherCategory;
import mywild.wildweather.domain.weather.data.entity.WeatherEntity;
import mywild.wildweather.domain.weather.web.dto.WeatherAggregate;
import mywild.wildweather.domain.weather.web.dto.WeatherGrouping;

public class MapperTest {

    @Test
    void testMapEntitiesToDtoProducesStructure() {
        WeatherEntity e = mock(WeatherEntity.class);
        when(e.getStation()).thenReturn("s1");
        when(e.getDate()).thenReturn(LocalDate.of(2025, 1, 1));
        when(e.getCategory()).thenReturn(WeatherCategory.A);
        when(e.getMissing()).thenReturn(0.0);
        when(e.getTemperature()).thenReturn(10.0);
        when(e.getWindSpeed()).thenReturn(5.0);
        when(e.getWindMax()).thenReturn(7.0);
        when(e.getWindDirection()).thenReturn("N");
        when(e.getRainRate()).thenReturn(0.1);
        when(e.getRainDaily()).thenReturn(1.0);
        when(e.getPressure()).thenReturn(1013.2);
        when(e.getHumidity()).thenReturn(50.0);
        when(e.getUvRadiationIndex()).thenReturn(1.0);

        var dto = Mapper.mapEntitiesToDto(WeatherGrouping.DAILY, WeatherAggregate.AVERAGE, null, List.of(e));

        assertNotNull(dto);
        assertTrue(dto.getWeather().containsKey("s1"));
        var yearMap = dto.getWeather().get("s1");
        assertTrue(yearMap.containsKey(2025));
        var groupMap = yearMap.get(2025);
        assertTrue(groupMap.containsKey("2025-01-01"));
        var fieldMap = groupMap.get("2025-01-01");
        assertNotNull(fieldMap);
        assertFalse(fieldMap.isEmpty());
    }

}
