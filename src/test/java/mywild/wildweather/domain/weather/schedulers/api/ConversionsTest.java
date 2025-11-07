package mywild.wildweather.domain.weather.schedulers.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

public class ConversionsTest {

	@Test
	void testRoundToOneDecimal() {
		assertEquals(2.3, Conversions.roundToOneDecimal(2.34), 1e-9);
		assertEquals(2.4, Conversions.roundToOneDecimal(2.35), 1e-9);
		assertEquals(-1.3, Conversions.roundToOneDecimal(-1.26), 1e-9);
		assertEquals(0.0, Conversions.roundToOneDecimal(0.04), 1e-9);
	}

	@Test
	void testFahToCel() {
		assertEquals(0.0, Conversions.fahToCel(32.0), 1e-9);
		assertEquals(100.0, Conversions.fahToCel(212.0), 1e-9);
		assertEquals(20.0, Conversions.fahToCel(68.0), 1e-9);
	}

	@Test
	void testMphToKmh() {
		assertEquals(1.6, Conversions.mphToKmh(1.0), 1e-9);
		assertEquals(0.0, Conversions.mphToKmh(0.0), 1e-9);
	}

	@Test
	void testInToMm() {
		assertEquals(25.4, Conversions.inToMm(1.0), 1e-9);
		assertEquals(0.0, Conversions.inToMm(0.0), 1e-9);
	}

	@Test
	void testInHgToHpa() {
		assertEquals(1013.2, Conversions.inHgToHpa(29.92), 1e-9);
	}

	@Test
	void testDegreesToDirection() {
		assertNull(Conversions.degreesToDirection(null));
		assertNull(Conversions.degreesToDirection(Double.NaN));
		assertEquals("N", Conversions.degreesToDirection(0.0));
		assertEquals("NNE", Conversions.degreesToDirection(22.5));
		assertEquals("NE", Conversions.degreesToDirection(45.0));
		assertEquals("ENE", Conversions.degreesToDirection(67.5));
		assertEquals("E", Conversions.degreesToDirection(90.0));
		assertEquals("ESE", Conversions.degreesToDirection(112.5));
		assertEquals("SE", Conversions.degreesToDirection(135.0));
		assertEquals("SSE", Conversions.degreesToDirection(157.5));
		assertEquals("S", Conversions.degreesToDirection(180.0));
		assertEquals("SSW", Conversions.degreesToDirection(202.5));
		assertEquals("SW", Conversions.degreesToDirection(225.0));
		assertEquals("WSW", Conversions.degreesToDirection(247.5));
		assertEquals("W", Conversions.degreesToDirection(270.0));
		assertEquals("WNW", Conversions.degreesToDirection(292.5));
		assertEquals("NW", Conversions.degreesToDirection(315.0));
		assertEquals("NNW", Conversions.degreesToDirection(337.5));
		assertEquals("NNE", Conversions.degreesToDirection(11.25));
		assertEquals("N", Conversions.degreesToDirection(348.75));
		assertEquals("N", Conversions.degreesToDirection(-10.0));
	}

}
