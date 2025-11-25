package mywild.wildweather.domain.weather.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import mywild.wildweather.domain.weather.data.WeatherRepository;
import mywild.wildweather.domain.weather.data.entity.WeatherCategory;
import mywild.wildweather.domain.weather.web.dto.WeatherAggregate;
import mywild.wildweather.domain.weather.web.dto.WeatherGrouping;

public class WeatherServiceTest {

    @Test
    void testGetWeatherDelegatesToMapperAndRepo() throws Exception {
        WeatherService svc = new WeatherService();
        WeatherRepository repo = mock(WeatherRepository.class);

        var repoField = WeatherService.class.getDeclaredField("repo");
        repoField.setAccessible(true);
        repoField.set(svc, repo);

        when(repo.searchWeather(any(), any(), any(), any(), any(), any())).thenReturn(List.of());

        var dto = svc.getWeather(List.of("s1"), WeatherGrouping.DAILY, WeatherCategory.A, WeatherAggregate.AVERAGE, null,
                LocalDate.now(), LocalDate.now(), null, null);
        assertNotNull(dto);
        assertTrue(dto.getWeather().isEmpty());
    }

    @Test
    void testGetWeatherStationsAndStatus() throws Exception {
        WeatherService svc = new WeatherService();
        WeatherRepository repo = mock(WeatherRepository.class);
        var repoField = WeatherService.class.getDeclaredField("repo");
        repoField.setAccessible(true);
        repoField.set(svc, repo);

        var myStationsField = WeatherService.class.getDeclaredField("myStations");
        myStationsField.setAccessible(true);
        myStationsField.set(svc, List.of("s1"));

        when(repo.findStations()).thenReturn(List.of("s1", "s2"));
        when(repo.findBottomDateByStation("s1")).thenReturn(LocalDate.of(2025, 1, 1));
        when(repo.findTopDateByStation("s1")).thenReturn(LocalDate.of(2025, 1, 15));
        when(repo.findBottomDateByStation("s2")).thenReturn(LocalDate.of(2025, 1, 2));
        when(repo.findTopDateByStation("s2")).thenReturn(LocalDate.of(2025, 1, 20));

        var stations = svc.getWeatherStations();
        assertEquals(2, stations.size());

        var status = svc.getWeatherStatus();
        assertEquals(2, status.size());
        assertEquals("s1", status.get(0).getStation());
        assertTrue(status.get(0).isMyStation());
        assertTrue(!status.get(1).isMyStation());
    }

}
