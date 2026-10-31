package org.uciasistant.feature.calculators

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class UnitConversionEngineTest {
    @Test
    fun convertsMassAndAcceptsSpanishDecimalComma() {
        val result = UnitConversionEngine.convert("1,5", MeasurementUnit.KILOGRAM, MeasurementUnit.GRAM)
        assertEquals(0, result.compareTo(BigDecimal("1500")))
        assertEquals("1500", UnitConversionEngine.format(result))
    }

    @Test
    fun convertsLengthAndVolume() {
        assertEquals(
            "1,65",
            UnitConversionEngine.format(
                UnitConversionEngine.convert("165", MeasurementUnit.CENTIMETER, MeasurementUnit.METER),
            ),
        )
        assertEquals(
            "0,25",
            UnitConversionEngine.format(
                UnitConversionEngine.convert("250", MeasurementUnit.MILLILITER, MeasurementUnit.LITER),
            ),
        )
    }

    @Test
    fun convertsTemperatureInBothDirections() {
        assertEquals(
            "98,6",
            UnitConversionEngine.format(
                UnitConversionEngine.convert("37", MeasurementUnit.CELSIUS, MeasurementUnit.FAHRENHEIT),
            ),
        )
        assertEquals(
            "37",
            UnitConversionEngine.format(
                UnitConversionEngine.convert("98,6", MeasurementUnit.FAHRENHEIT, MeasurementUnit.CELSIUS),
            ),
        )
    }

    @Test
    fun convertsBetweenCelsiusAndKelvin() {
        assertEquals(
            "273,15",
            UnitConversionEngine.format(
                UnitConversionEngine.convert("0", MeasurementUnit.CELSIUS, MeasurementUnit.KELVIN),
            ),
        )
        assertEquals(
            "0",
            UnitConversionEngine.format(
                UnitConversionEngine.convert("273,15", MeasurementUnit.KELVIN, MeasurementUnit.CELSIUS),
            ),
        )
    }

    @Test
    fun sameUnitLeavesValueUnchanged() {
        val input = BigDecimal("-2.125")
        assertEquals(input, UnitConversionEngine.convert(input, MeasurementUnit.CELSIUS, MeasurementUnit.CELSIUS))
    }

    @Test
    fun rejectsMixedMagnitudes() {
        assertThrows(IllegalArgumentException::class.java) {
            UnitConversionEngine.convert("1", MeasurementUnit.KILOGRAM, MeasurementUnit.METER)
        }
    }

    @Test
    fun rejectsInvalidOrAmbiguousNumbers() {
        listOf("", "NaN", "1.234,5", "1,2,3", "1e3", "--2").forEach { input ->
            assertThrows(IllegalArgumentException::class.java) {
                UnitConversionEngine.parseInput(input)
            }
        }
    }

    @Test
    fun rejectsOverlongNumbers() {
        assertThrows(IllegalArgumentException::class.java) {
            UnitConversionEngine.parseInput("1".repeat(41))
        }
    }
}
