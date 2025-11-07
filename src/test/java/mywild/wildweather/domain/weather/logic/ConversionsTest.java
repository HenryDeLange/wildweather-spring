package mywild.wildweather.domain.weather.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class ConversionsTest {

    @Test
    void testRoundToOneDecimal() {
        assertEquals(2.3, Conversions.roundToOneDecimal(2.34), 1e-9);
        assertEquals(2.4, Conversions.roundToOneDecimal(2.35), 1e-9);
        assertEquals(-1.3, Conversions.roundToOneDecimal(-1.26), 1e-9);
    }

    @Test
    void testDirectionToDegrees() {
        assertEquals(0.0, Conversions.directionToDegrees("N"), 1e-9);
        assertEquals(22.5, Conversions.directionToDegrees("NNE"), 1e-9);
        assertEquals(45.0, Conversions.directionToDegrees("NE"), 1e-9);
        assertEquals(67.5, Conversions.directionToDegrees("ENE"), 1e-9);
        assertEquals(90.0, Conversions.directionToDegrees("E"), 1e-9);
        assertEquals(112.5, Conversions.directionToDegrees("ESE"), 1e-9);
        assertEquals(135.0, Conversions.directionToDegrees("SE"), 1e-9);
        assertEquals(157.5, Conversions.directionToDegrees("SSE"), 1e-9);
        assertEquals(180.0, Conversions.directionToDegrees("S"), 1e-9);
        assertEquals(202.5, Conversions.directionToDegrees("SSW"), 1e-9);
        assertEquals(225.0, Conversions.directionToDegrees("SW"), 1e-9);
        assertEquals(247.5, Conversions.directionToDegrees("WSW"), 1e-9);
        assertEquals(270.0, Conversions.directionToDegrees("W"), 1e-9);
        assertEquals(292.5, Conversions.directionToDegrees("WNW"), 1e-9);
        assertEquals(315.0, Conversions.directionToDegrees("NW"), 1e-9);
        assertEquals(337.5, Conversions.directionToDegrees("NNW"), 1e-9);
        assertEquals(Double.NEGATIVE_INFINITY, Conversions.directionToDegrees("UNKNOWN"));
        assertEquals(0.0, Conversions.directionToDegrees(null), 1e-9);
        assertEquals(0.0, Conversions.directionToDegrees(""), 1e-9);
    }

}
