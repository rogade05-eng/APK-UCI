package org.uciasistant.feature.handoff

/** In-memory form state only. This model is deliberately not persisted or exported. */
internal data class HandoffDraft(
    val episodeReference: String = "",
    val originInstitutionService: String = "",
    val destinationService: String = "",
    val transferType: String = "",
    val transferReason: String = "",
    val localPriority: String = "",
    val localCriteriaReference: String = "",
    val referralDateTime: String = "",
    val departureDateTime: String = "",
    val arrivalDateTime: String = "",
    val handoverDateTime: String = "",
    val sendingProfessional: String = "",
    val receivingProfessional: String = "",
    val activeProblems: String = "",
    val allergies: String = "",
    val interventionsInProgress: String = "",
    val devices: String = "",
    val pendingResults: String = "",
    val outstandingTasks: String = "",
) {
    val hasContent: Boolean
        get() = listOf(
            episodeReference,
            originInstitutionService,
            destinationService,
            transferType,
            transferReason,
            localPriority,
            localCriteriaReference,
            referralDateTime,
            departureDateTime,
            arrivalDateTime,
            handoverDateTime,
            sendingProfessional,
            receivingProfessional,
            activeProblems,
            allergies,
            interventionsInProgress,
            devices,
            pendingResults,
            outstandingTasks,
        ).any(String::isNotBlank)
}
