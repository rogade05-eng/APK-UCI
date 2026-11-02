package org.uciasistant.feature.alerts

internal data class AlertReminder(
    val id: String,
    val createdAtMillis: Long,
    val dueAtMillis: Long,
    val title: String,
)

internal enum class AlertReminderInputError {
    EMPTY_TITLE,
    TITLE_TOO_LONG,
    DUE_TIME_NOT_IN_FUTURE,
}

internal const val MAX_ALERT_REMINDER_TITLE_CHARACTERS = 120

/** Validates only reminder form fields; it defines no clinical rules or patient-specific criteria. */
internal fun validateAlertReminderInput(
    title: String,
    dueAtMillis: Long,
    nowMillis: Long,
): AlertReminderInputError? = when {
    title.isBlank() -> AlertReminderInputError.EMPTY_TITLE
    title.trim().length > MAX_ALERT_REMINDER_TITLE_CHARACTERS -> AlertReminderInputError.TITLE_TOO_LONG
    dueAtMillis <= nowMillis -> AlertReminderInputError.DUE_TIME_NOT_IN_FUTURE
    else -> null
}
