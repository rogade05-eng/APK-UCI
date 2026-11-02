package org.uciasistant.feature.handoff

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptedHandoffStoreTest {
    @Test
    fun savedSnapshotIsEncryptedReadableAndDeletable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = EncryptedHandoffStore(context)
        val marker = "SYNTHETIC-SECRET-DO-NOT-USE"
        val draft = HandoffDraft(
            episodeReference = "TEST-ONLY-001",
            originInstitutionService = "Servicio ficticio de origen",
            destinationService = "Servicio ficticio de destino",
            activeProblems = marker,
        )
        var savedId: String? = null
        try {
            val saved = store.saveNew(draft)
            savedId = saved.id
            val encryptedFile = File(
                context.filesDir,
                "encrypted-handoff-records-v1/${saved.id}.enc",
            )

            assertTrue(encryptedFile.isFile)
            val encryptedBytes = encryptedFile.readBytes()
            val bytesAsText = String(encryptedBytes, StandardCharsets.ISO_8859_1)
            assertFalse(bytesAsText.contains(marker))
            assertEquals(draft, store.readRecord(saved.id).draft)
            assertTrue(store.listRecords().any { it.id == saved.id })
            assertTrue(store.deleteRecord(saved.id))
            assertFalse(encryptedFile.exists())
        } finally {
            savedId?.let(store::deleteRecord)
        }
    }

    @Test
    fun modifiedCiphertextFailsAuthentication() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = EncryptedHandoffStore(context)
        var savedId: String? = null
        try {
            val record = store.saveNew(HandoffDraft(activeProblems = "SYNTHETIC TEST"))
            savedId = record.id
            val encryptedFile = File(
                context.filesDir,
                "encrypted-handoff-records-v1/${record.id}.enc",
            )
            val bytes = encryptedFile.readBytes()
            bytes[bytes.lastIndex] = (bytes.last().toInt() xor 0x01).toByte()
            encryptedFile.writeBytes(bytes)

            val error = runCatching { store.readRecord(record.id) }.exceptionOrNull()
            assertTrue(error is IOException)
        } finally {
            savedId?.let(store::deleteRecord)
        }
    }
}
