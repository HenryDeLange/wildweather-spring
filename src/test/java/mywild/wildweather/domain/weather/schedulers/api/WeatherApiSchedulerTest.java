package mywild.wildweather.domain.weather.schedulers.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

public class WeatherApiSchedulerTest {

	@Test
	void testIsRunningReflectsIS_RUNNINGFlag() throws Exception {
		WeatherApiScheduler scheduler = new WeatherApiScheduler();

		Field f = WeatherApiScheduler.class.getDeclaredField("IS_RUNNING");
		f.setAccessible(true);
		AtomicBoolean flag = (AtomicBoolean) f.get(null);

		boolean original = flag.get();
		try {
			flag.set(true);
			assertTrue(scheduler.isRunning());
		}
		finally {
			flag.set(original);
		}
	}

	@Test
	void testProcessValueUpdatesMaps() throws Exception {
		WeatherApiScheduler scheduler = new WeatherApiScheduler();

		Map<Integer, Double> low = new HashMap<>();
		Map<Integer, Double> high = new HashMap<>();
		Map<Integer, List<Double>> average = new HashMap<>();

		Method m = WeatherApiScheduler.class.getDeclaredMethod("processValue", Map.class, Map.class, Map.class,
				int.class, double.class);
		m.setAccessible(true);

		m.invoke(scheduler, low, high, average, 1, 5.5);
		assertEquals(5.5, low.get(1));
		assertEquals(5.5, high.get(1));
		assertNotNull(average.get(1));
		assertEquals(1, average.get(1).size());
		assertEquals(5.5, average.get(1).get(0));

		m.invoke(scheduler, low, high, average, 1, 3.2);
		assertEquals(3.2, low.get(1));
		assertEquals(5.5, high.get(1));
		assertEquals(2, average.get(1).size());
	}

	@Test
	void testGetCalculatedAverageRoundsAndHandlesEmpty() throws Exception {
		WeatherApiScheduler scheduler = new WeatherApiScheduler();

		Map<Integer, List<Double>> average = new HashMap<>();
		List<Double> values = new ArrayList<>();
		values.add(1.11);
		values.add(2.22);
		average.put(0, values);

		Method m = WeatherApiScheduler.class.getDeclaredMethod("getCalculatedAverage", Map.class);
		m.setAccessible(true);

		@SuppressWarnings("unchecked")
		Map<Integer, Double> result = (Map<Integer, Double>) m.invoke(scheduler, average);
		assertEquals(1.7, result.get(0));

		average.put(1, new ArrayList<>());
		@SuppressWarnings("unchecked")
		Map<Integer, Double> result2 = (Map<Integer, Double>) m.invoke(scheduler, average);
		assertEquals(0.0, result2.get(1));
	}

}
