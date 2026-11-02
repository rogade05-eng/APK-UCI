package org.uciasistant.core.clinicaldata

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClinicalObservationRecordTest {
    private val validRecord = ClinicalObservationRecord(
        id = "obs-synthetic-001",
        episodeId = "episode-synthetic-001",
        population = ClinicalPopulation.PEDIATRIC,
        parameterCode = "TEST-PARAMETER",
        valueText = "TEST-VALUE",
        unitCode = "TEST-UNIT",
        observedAtMillis = 10_000L,
        recordedAtMillis = 11_000L,
        authorId = "test-user",
        origin = ObservationOrigin.MANUAL,
        condition = ObservationCondition.OBSERVED,
    )

    @Test
    fun acceptsCompleteRecordForMatchingPopulation() {
        assertTrue(validRecord.validationIssues(ClinicalPopulation.PEDIATRIC).isEmpty())
    }

    @Test
    fun rejectsPopulationMismatchAndMissingAuthor() {
        val record = validRecord.copy(authorId = null)

        assertEquals(
            listOf(
                ObservationValidationIssue.POPULATION_MISMATCH,
                ObservationValidationIssue.MISSING_AUTHOR,
            ),
            record.validationIssues(ClinicalPopulation.NEONATAL),
        )
    }

    @Test
    fun missingObservedTimeMustBeExplicitlyMarkedIncomplete() {
        val record = validRecord.copy(observedAtMillis = null)

        assertEquals(
            listOf(ObservationValidationIssue.OBSERVED_TIME_MISSING_WITHOUT_INCOMPLETE_STATUS),
            record.validationIssues(ClinicalPopulation.PEDIATRIC),
        )
        assertTrue(
            record.copy(condition = ObservationCondition.INCOMPLETE)
                .validationIssues(ClinicalPopulation.PEDIATRIC)
                .isEmpty(),
        )
    }

    @Test
    fun correctionsMustPointToAnotherObservation() {
        assertEquals(
            listOf(ObservationValidationIssue.MISSING_CORRECTION_REFERENCE),
            validRecord.copy(condition = ObservationCondition.CORRECTED)
                .validationIssues(ClinicalPopulation.PEDIATRIC),
        )
        assertEquals(
            listOf(ObservationValidationIssue.SELF_REFERENCING_CORRECTION),
            validRecord.copy(
                condition = ObservationCondition.CORRECTED,
                correctsObservationId = validRecord.id,
            ).validationIssues(ClinicalPopulation.PEDIATRIC),
        )
    }
}
