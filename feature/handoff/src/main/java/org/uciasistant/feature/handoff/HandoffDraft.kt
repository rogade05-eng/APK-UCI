package org.uciasistant.feature.handoff

/** Manual handoff fields. Stored copies are encrypted by [EncryptedHandoffStore]. */
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
        get() = fieldsInStorageOrder().any(String::isNotBlank)

    internal fun fieldsInStorageOrder(): List<String> = listOf(
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
    )

    internal companion object {
        const val STORED_FIELD_COUNT = 19

        fun fromStorageFields(fields: List<String>): HandoffDraft {
            require(fields.size == STORED_FIELD_COUNT)
            return HandoffDraft(
                episodeReference = fields[0],
                originInstitutionService = fields[1],
                destinationService = fields[2],
                transferType = fields[3],
                transferReason = fields[4],
                localPriority = fields[5],
                localCriteriaReference = fields[6],
                referralDateTime = fields[7],
                departureDateTime = fields[8],
                arrivalDateTime = fields[9],
                handoverDateTime = fields[10],
                sendingProfessional = fields[11],
                receivingProfessional = fields[12],
                activeProblems = fields[13],
                allergies = fields[14],
                interventionsInProgress = fields[15],
                devices = fields[16],
                pendingResults = fields[17],
                outstandingTasks = fields[18],
            )
        }
    }
}
