package org.uciasistant.core.clinicaldata

/** Data-provenance model only; it contains no clinical thresholds, conversions, or interpretations. */
data class ClinicalObservationRecord(
    val id: String,
    val episodeId: String,
    val population: ClinicalPopulation,
    val parameterCode: String,
    val valueText: String,
    val unitCode: String?,
    val observedAtMillis: Long?,
    val recordedAtMillis: Long,
    val authorId: String?,
    val origin: ObservationOrigin,
    val condition: ObservationCondition,
    val methodOrSite: String? = null,
    val deviceId: String? = null,
    val correctsObservationId: String? = null,
) {
    /** Checks record integrity and population scope without assigning clinical meaning. */
    fun validationIssues(expectedPopulation: ClinicalPopulation): List<ObservationValidationIssue> = buildList {
        if (id.isBlank()) add(ObservationValidationIssue.MISSING_ID)
        if (episodeId.isBlank()) add(ObservationValidationIssue.MISSING_EPISODE_ID)
        if (population != expectedPopulation) add(ObservationValidationIssue.POPULATION_MISMATCH)
        if (parameterCode.isBlank()) add(ObservationValidationIssue.MISSING_PARAMETER_CODE)
        if (valueText.isBlank()) add(ObservationValidationIssue.MISSING_VALUE)
        if (unitCode != null && unitCode.isBlank()) add(ObservationValidationIssue.INVALID_UNIT_CODE)
        if (recordedAtMillis <= 0L) add(ObservationValidationIssue.INVALID_RECORDED_TIME)
        if (observedAtMillis != null && observedAtMillis <= 0L) {
            add(ObservationValidationIssue.INVALID_OBSERVED_TIME)
        } else if (observedAtMillis == null && condition != ObservationCondition.INCOMPLETE) {
            add(ObservationValidationIssue.OBSERVED_TIME_MISSING_WITHOUT_INCOMPLETE_STATUS)
        }
        if (authorId.isNullOrBlank()) add(ObservationValidationIssue.MISSING_AUTHOR)
        if (methodOrSite != null && methodOrSite.isBlank()) add(ObservationValidationIssue.INVALID_METHOD_OR_SITE)
        if (deviceId != null && deviceId.isBlank()) add(ObservationValidationIssue.INVALID_DEVICE_ID)

        when {
            condition == ObservationCondition.CORRECTED && correctsObservationId.isNullOrBlank() ->
                add(ObservationValidationIssue.MISSING_CORRECTION_REFERENCE)

            condition == ObservationCondition.CORRECTED && correctsObservationId == id ->
                add(ObservationValidationIssue.SELF_REFERENCING_CORRECTION)

            condition != ObservationCondition.CORRECTED && !correctsObservationId.isNullOrBlank() ->
                add(ObservationValidationIssue.UNEXPECTED_CORRECTION_REFERENCE)
        }
    }
}

enum class ObservationOrigin {
    MANUAL,
    TRANSCRIBED,
    AUTHORIZED_IMPORT,
    DERIVED,
}

enum class ObservationCondition {
    OBSERVED,
    ESTIMATED,
    CORRECTED,
    INCOMPLETE,
    QUESTIONED,
}

enum class ObservationValidationIssue {
    MISSING_ID,
    MISSING_EPISODE_ID,
    POPULATION_MISMATCH,
    MISSING_PARAMETER_CODE,
    MISSING_VALUE,
    INVALID_UNIT_CODE,
    INVALID_RECORDED_TIME,
    INVALID_OBSERVED_TIME,
    OBSERVED_TIME_MISSING_WITHOUT_INCOMPLETE_STATUS,
    MISSING_AUTHOR,
    INVALID_METHOD_OR_SITE,
    INVALID_DEVICE_ID,
    MISSING_CORRECTION_REFERENCE,
    SELF_REFERENCING_CORRECTION,
    UNEXPECTED_CORRECTION_REFERENCE,
}
