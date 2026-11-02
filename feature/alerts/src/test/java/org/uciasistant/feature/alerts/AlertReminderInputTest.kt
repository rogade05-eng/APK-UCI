package org.uciasistant.feature.alerts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlertReminderInputTest {
    @Test
    fun acceptsBriefTitleAndFutureTime() {
        assertNull(
            validateAlertReminderInput(
                title = " Revisar una nota general ",
                dueAtMillis = 2_000L,
                nowMillis = 1_000L,
            ),
        )
    }

    @Test
    fun rejectsBlankOrOverlongTitle() {
        assertEquals(
            AlertReminderInputError.EMPTY_TITLE,
            validateAlertReminderInput("   ", 2_000L, 1_000L),
        )
        assertEquals(
            AlertReminderInputError.TITLE_TOO_LONG,
            validateAlertReminderInput("x".repeat(MAX_ALERT_REMINDER_TITLE_CHARACTERS + 1), 2_000L, 1_000L),
        )
    }

    @Test
    fun rejectsTimeThatIsNotInTheFuture() {
        assertEquals(
            AlertReminderInputError.DUE_TIME_NOT_IN_FUTURE,
            validateAlertReminderInput("Reminder", 1_000L, 1_000L),
        )
    }
}
