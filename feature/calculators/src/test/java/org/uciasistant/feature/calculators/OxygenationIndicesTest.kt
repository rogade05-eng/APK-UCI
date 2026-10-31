package org.uciasistant.feature.calculators

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OxygenationIndicesTest {
    @Test
    fun calculatesFourIndicesFromOneConsistentSetOfInputs() {
        val result = OxygenationIndices.calculate(
            OxygenationInputs(
                fio2Percent = BigDecimal("50"),
                pao2MmHg = BigDecimal("80"),
                spo2Percent = BigDecimal("90"),
                meanAirwayPressureCmH2O = BigDecimal("10"),
            ),
        )

        assertEquals("160", OxygenationIndices.format(result.pfRatio!!))
        assertEquals("180", OxygenationIndices.format(result.sfRatio!!))
        assertEquals("6,25", OxygenationIndices.format(result.oxygenationIndex!!))
        assertEquals("5,56", OxygenationIndices.format(result.oxygenSaturationIndex!!))
        assertFalse(result.saturationExceedsSfLimit)
    }

    @Test
    fun calculatesBloodGasRatioWithoutMeanAirwayPressure() {
        val result = OxygenationIndices.calculate(
            OxygenationInputs(
                fio2Percent = BigDecimal("40"),
                pao2MmHg = BigDecimal("60"),
            ),
        )

        assertEquals("150", OxygenationIndices.format(result.pfRatio!!))
        assertNull(result.sfRatio)
        assertNull(result.oxygenationIndex)
        assertNull(result.oxygenSaturationIndex)
    }

    @Test
    fun withholdsSaturationBasedIndicesAboveSourceLimit() {
        val result = OxygenationIndices.calculate(
            OxygenationInputs(
                fio2Percent = BigDecimal("50"),
                pao2MmHg = BigDecimal("80"),
                spo2Percent = BigDecimal("98"),
                meanAirwayPressureCmH2O = BigDecimal("10"),
            ),
        )

        assertEquals("160", OxygenationIndices.format(result.pfRatio!!))
        assertNull(result.sfRatio)
        assertNull(result.oxygenSaturationIndex)
        assertTrue(result.saturationExceedsSfLimit)
    }

    @Test
    fun includesSaturationBasedIndicesAtThe97PercentLimit() {
        val result = OxygenationIndices.calculate(
            OxygenationInputs(
                fio2Percent = BigDecimal("21"),
                spo2Percent = BigDecimal("97"),
                meanAirwayPressureCmH2O = BigDecimal("10"),
            ),
        )

        assertEquals("461,9", OxygenationIndices.format(result.sfRatio!!))
        assertEquals("2,16", OxygenationIndices.format(result.oxygenSaturationIndex!!))
        assertFalse(result.saturationExceedsSfLimit)
    }

    @Test
    fun rejectsInvalidInputsRatherThanSilentlyConvertingThem() {
        val invalidInputs = listOf(
            OxygenationInputs(fio2Percent = BigDecimal.ZERO, pao2MmHg = BigDecimal("80")),
            OxygenationInputs(fio2Percent = BigDecimal("20"), pao2MmHg = BigDecimal("80")),
            OxygenationInputs(fio2Percent = BigDecimal("101"), pao2MmHg = BigDecimal("80")),
            OxygenationInputs(fio2Percent = BigDecimal("50"), pao2MmHg = BigDecimal.ZERO),
            OxygenationInputs(fio2Percent = BigDecimal("50"), spo2Percent = BigDecimal("101")),
            OxygenationInputs(fio2Percent = BigDecimal("50")),
        )

        invalidInputs.forEach { input ->
            org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
                OxygenationIndices.calculate(input)
            }
        }
    }

    @Test
    fun parsesSpanishDecimalAndFormatsWithoutThousandsSeparators() {
        val parsed = OxygenationIndices.parse("37,5")
        assertEquals("37,5", OxygenationIndices.format(parsed))
    }
}
