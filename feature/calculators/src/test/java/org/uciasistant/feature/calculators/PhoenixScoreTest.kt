package org.uciasistant.feature.calculators

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoenixScoreTest {
    @Test
    fun scoresACompleteLowScoreAssessment() {
        val result = PhoenixScoreCalculator.calculate(
            completeInputs(
                meanArterialPressureMmHg = BigDecimal("49"),
                lactateMmolL = BigDecimal("4.9"),
                plateletsThousandPerMicroliter = BigDecimal("100"),
                inr = BigDecimal("1.3"),
                dDimerMgPerLiterFeu = BigDecimal("2"),
                fibrinogenMgPerDl = BigDecimal("100"),
                glasgowComaScale = 11,
                bilaterallyFixedPupils = false,
            ),
        )

        assertEquals(0, result.respiratory.points)
        assertEquals(0, result.cardiovascular.points)
        assertEquals(0, result.coagulation.points)
        assertEquals(0, result.neurologic.points)
        assertEquals(0, result.totalPoints)
        assertTrue(result.isComplete)
    }

    @Test
    fun capsTheFourDomainSumAtThirteenAndUsesWorstRespiratoryMethod() {
        val result = PhoenixScoreCalculator.calculate(
            completeInputs(
                respiratorySupport = PhoenixRespiratorySupport.INVASIVE_MECHANICAL_VENTILATION,
                fio2Percent = BigDecimal("50"),
                pao2MmHg = BigDecimal("40"),
                spo2Percent = BigDecimal("90"),
                vasoactiveMedicationCount = 2,
                lactateMmolL = BigDecimal("11"),
                meanArterialPressureMmHg = BigDecimal("35"),
                plateletsThousandPerMicroliter = BigDecimal("99"),
                inr = BigDecimal("1.31"),
                dDimerMgPerLiterFeu = BigDecimal("2.01"),
                fibrinogenMgPerDl = BigDecimal("99"),
                glasgowComaScale = 10,
                bilaterallyFixedPupils = true,
            ),
        )

        assertEquals(3, result.respiratory.points)
        assertEquals(6, result.cardiovascular.points)
        assertEquals(2, result.coagulation.points)
        assertEquals(2, result.neurologic.points)
        assertEquals(13, result.totalPoints)
        assertTrue(result.isComplete)
    }

    @Test
    fun nonInvasiveSupportCannotScoreMoreThanOneRespiratoryPoint() {
        val result = PhoenixScoreCalculator.calculate(
            completeInputs(
                respiratorySupport = PhoenixRespiratorySupport.NON_INVASIVE_OR_OXYGEN,
                fio2Percent = BigDecimal("50"),
                pao2MmHg = BigDecimal("20"),
                spo2Percent = BigDecimal("70"),
            ),
        )

        assertEquals(1, result.respiratory.points)
    }

    @Test
    fun roomAirWithoutRespiratorySupportScoresZeroEvenWithALowRatio() {
        val result = PhoenixScoreCalculator.calculate(
            completeInputs(
                respiratorySupport = PhoenixRespiratorySupport.NONE,
                fio2Percent = BigDecimal("21"),
                pao2MmHg = BigDecimal("20"),
            ),
        )

        assertEquals(0, result.respiratory.points)
        assertTrue(result.respiratory.missingInputs.isEmpty())
    }

    @Test
    fun respiratoryBoundariesMatchThePublishedScoreBands() {
        val cases = listOf(
            BigDecimal("400") to 0,
            BigDecimal("399.99") to 1,
            BigDecimal("200.01") to 1,
            BigDecimal("200") to 2,
            BigDecimal("100") to 2,
            BigDecimal("99.99") to 3,
        )

        cases.forEach { (pfRatio, expected) ->
            val input = completeInputs(
                respiratorySupport = PhoenixRespiratorySupport.INVASIVE_MECHANICAL_VENTILATION,
                fio2Percent = BigDecimal("50"),
                pao2MmHg = pfRatio.multiply(BigDecimal("0.5")),
            )
            assertEquals("P/F $pfRatio", expected, PhoenixScoreCalculator.calculate(input).respiratory.points)
        }
    }

    @Test
    fun usesSaturationRatioAtThePublished97PercentLimit() {
        val result = PhoenixScoreCalculator.calculate(
            completeInputs(
                respiratorySupport = PhoenixRespiratorySupport.NON_INVASIVE_OR_OXYGEN,
                fio2Percent = BigDecimal("50"),
                spo2Percent = BigDecimal("97"),
            ),
        )

        assertEquals(1, result.respiratory.points)
        assertFalse(result.respiratory.notes.any { it.contains("S/F no se usa") })
    }

    @Test
    fun excludesSaturationRatioAboveNinetySevenAndRetainsBloodGasPathway() {
        val result = PhoenixScoreCalculator.calculate(
            completeInputs(
                respiratorySupport = PhoenixRespiratorySupport.INVASIVE_MECHANICAL_VENTILATION,
                fio2Percent = BigDecimal("50"),
                pao2MmHg = BigDecimal("80"),
                spo2Percent = BigDecimal("98"),
            ),
        )

        assertEquals(2, result.respiratory.points)
        assertTrue(result.respiratory.notes.any { it.contains("S/F no se usa") })
    }

    @Test
    fun followsPublishedMissingDataConventionAndShowsEveryOmission() {
        val result = PhoenixScoreCalculator.calculate(
            completeInputs(
                vasoactiveMedicationCount = null,
                lactateMmolL = null,
                meanArterialPressureMmHg = null,
                plateletsThousandPerMicroliter = null,
                inr = null,
                dDimerMgPerLiterFeu = null,
                fibrinogenMgPerDl = null,
                glasgowComaScale = null,
                bilaterallyFixedPupils = null,
            ),
        )

        assertEquals(0, result.totalPoints)
        assertFalse(result.isComplete)
        assertTrue(result.missingInputs.containsAll(listOf("Lactato", "Plaquetas", "Glasgow (GCS)")))
    }

    @Test
    fun scoresAgeAdjustedMapAtBoundaries() {
        val cases = listOf(
            11 to (BigDecimal("24.99") to 2),
            11 to (BigDecimal("25") to 1),
            11 to (BigDecimal("39") to 0),
            12 to (BigDecimal("30.99") to 2),
            12 to (BigDecimal("31") to 1),
            12 to (BigDecimal("44") to 0),
            60 to (BigDecimal("35.99") to 2),
            60 to (BigDecimal("36") to 1),
            60 to (BigDecimal("49") to 0),
        )

        cases.forEach { (ageMonths, mapCase) ->
            val (map, expectedPoints) = mapCase
            val result = PhoenixScoreCalculator.calculate(
                completeInputs(ageMonths = ageMonths, meanArterialPressureMmHg = map),
            )
            assertEquals("age=$ageMonths months, MAP=$map", expectedPoints, result.cardiovascular.points)
        }
    }

    @Test
    fun excludesOutOfScopeAgePrematurityAndBirthHospitalization() {
        val invalidInputs = listOf(
            completeInputs(ageMonths = 0),
            completeInputs(ageMonths = 216),
            completeInputs(postconceptionalAgeAtAssessmentWeeks = 36),
            completeInputs(isBirthHospitalization = true),
            completeInputs(fio2Percent = BigDecimal("40")),
        )

        invalidInputs.forEach { input ->
            org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
                PhoenixScoreCalculator.calculate(input)
            }
        }
    }

    private fun completeInputs(
        ageMonths: Int = 60,
        postconceptionalAgeAtAssessmentWeeks: Int = 40 + ageMonths * 435 / 100,
        isBirthHospitalization: Boolean = false,
        respiratorySupport: PhoenixRespiratorySupport = PhoenixRespiratorySupport.NONE,
        fio2Percent: BigDecimal? = null,
        pao2MmHg: BigDecimal? = null,
        spo2Percent: BigDecimal? = null,
        vasoactiveMedicationCount: Int? = 0,
        lactateMmolL: BigDecimal? = BigDecimal("2"),
        meanArterialPressureMmHg: BigDecimal? = BigDecimal("49"),
        plateletsThousandPerMicroliter: BigDecimal? = BigDecimal("150"),
        inr: BigDecimal? = BigDecimal("1"),
        dDimerMgPerLiterFeu: BigDecimal? = BigDecimal("0.5"),
        fibrinogenMgPerDl: BigDecimal? = BigDecimal("200"),
        glasgowComaScale: Int? = 15,
        bilaterallyFixedPupils: Boolean? = false,
    ) = PhoenixScoreInputs(
        ageMonths = ageMonths,
        postconceptionalAgeAtAssessmentWeeks = postconceptionalAgeAtAssessmentWeeks,
        isBirthHospitalization = isBirthHospitalization,
        respiratorySupport = respiratorySupport,
        fio2Percent = fio2Percent,
        pao2MmHg = pao2MmHg,
        spo2Percent = spo2Percent,
        vasoactiveMedicationCount = vasoactiveMedicationCount,
        lactateMmolL = lactateMmolL,
        meanArterialPressureMmHg = meanArterialPressureMmHg,
        plateletsThousandPerMicroliter = plateletsThousandPerMicroliter,
        inr = inr,
        dDimerMgPerLiterFeu = dDimerMgPerLiterFeu,
        fibrinogenMgPerDl = fibrinogenMgPerDl,
        glasgowComaScale = glasgowComaScale,
        bilaterallyFixedPupils = bilaterallyFixedPupils,
    )
}
