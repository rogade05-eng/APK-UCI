package org.uciasistant.feature.alerts

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptedAlertReminderStoreTest {
    @Test
    fun reminderTextIsEncryptedReadableAndDeletable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = EncryptedAlertReminderStore(context)
        val secretMarker = "SYNTHETIC-REMINDER-SECRET"
        var savedId: String? = null
        try {
            val dueAtMillis = System.currentTimeMillis() + 60 * 60 * 1_000L
            val saved = store.create(secretMarker, dueAtMillis)
            savedId = saved.id
            val encryptedFile = File(
                context.filesDir,
                "encrypted-alert-reminders-v1/${saved.id}.enc",
            )

            assertTrue(encryptedFile.isFile)
            assertFalse(String(encryptedFile.readBytes(), StandardCharsets.ISO_8859_1).contains(secretMarker))
            assertEquals(secretMarker, store.readReminder(saved.id)?.title)
            assertTrue(store.listReminders().any { it.id == saved.id })
            assertTrue(store.deleteReminder(saved.id))
            assertNull(store.readReminder(saved.id))
        } finally {
            savedId?.let(store::deleteReminder)
        }
    }

    @Test
    fun modifiedCiphertextFailsAuthentication() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = EncryptedAlertReminderStore(context)
        var savedId: String? = null
        try {
            val reminder = store.create(
                title = "SYNTHETIC TEST ONLY",
                dueAtMillis = System.currentTimeMillis() + 60 * 60 * 1_000L,
            )
            savedId = reminder.id
            val encryptedFile = File(
                context.filesDir,
                "encrypted-alert-reminders-v1/${reminder.id}.enc",
            )
            val bytes = encryptedFile.readBytes()
            bytes[bytes.lastIndex] = (bytes.last().toInt() xor 0x01).toByte()
            encryptedFile.writeBytes(bytes)

            assertTrue(runCatching { store.readReminder(reminder.id) }.exceptionOrNull() is IOException)
        } finally {
            savedId?.let(store::deleteReminder)
        }
    }
}
