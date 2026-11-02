package org.uciasistant.core.clinicaldata

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptedObservationLedgerStoreTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun roundTripsEncryptedObservationWithoutWritingPlaintext() = withStore { store, directory ->
        val observation = observation(value = "SYNTHETIC-SECRET-VALUE-7F4A")

        store.append(observation, ClinicalPopulation.PEDIATRIC)

        val encryptedFile = File(directory, "${observation.id}.enc")
        assertTrue(encryptedFile.isFile)
        val ciphertext = encryptedFile.readBytes()
        assertTrue(ciphertext.copyOfRange(0, 4).contentEquals(byteArrayOf(0x55, 0x43, 0x49, 0x4f)))
        assertFalse(containsBytes(ciphertext, observation.valueText.toByteArray(StandardCharsets.UTF_8)))
        assertEquals(observation, store.readById(observation.id))
        assertEquals(listOf(observation), store.readEpisode(observation.episodeId, ClinicalPopulation.PEDIATRIC))
    }

    @Test
    fun correctionIsAppendedAndKeepsOriginalObservation() = withStore { store, _ ->
        val original = observation(recordedAtMillis = 10_000L, value = "SYNTHETIC-ORIGINAL")
        val correction = observation(
            episodeId = original.episodeId,
            recordedAtMillis = 20_000L,
            condition = ObservationCondition.CORRECTED,
            correctsObservationId = original.id,
            value = "SYNTHETIC-CORRECTED",
        )

        store.append(original, ClinicalPopulation.PEDIATRIC)
        store.append(correction, ClinicalPopulation.PEDIATRIC)

        assertEquals(original, store.readById(original.id))
        assertEquals(correction, store.readById(correction.id))
        assertEquals(
            listOf(original, correction),
            store.readEpisode(original.episodeId, ClinicalPopulation.PEDIATRIC),
        )
        assertThrows(IllegalArgumentException::class.java) {
            store.append(original, ClinicalPopulation.PEDIATRIC)
        }
        assertEquals(original, store.readById(original.id))
    }

    @Test
    fun episodeQueriesAreScopedToTheRequestedPopulation() = withStore { store, _ ->
        val episodeId = "synthetic-shared-episode"
        val pediatric = observation(episodeId = episodeId, population = ClinicalPopulation.PEDIATRIC)
        val adult = observation(episodeId = episodeId, population = ClinicalPopulation.ADULT)
        val neonatal = observation(episodeId = episodeId, population = ClinicalPopulation.NEONATAL)

        store.append(pediatric, ClinicalPopulation.PEDIATRIC)
        store.append(adult, ClinicalPopulation.ADULT)
        store.append(neonatal, ClinicalPopulation.NEONATAL)

        assertEquals(listOf(pediatric), store.readEpisode(episodeId, ClinicalPopulation.PEDIATRIC))
        assertEquals(listOf(adult), store.readEpisode(episodeId, ClinicalPopulation.ADULT))
        assertEquals(listOf(neonatal), store.readEpisode(episodeId, ClinicalPopulation.NEONATAL))
        assertNull(store.readById("not-a-uuid"))
    }

    @Test
    fun appendRejectsPopulationMismatchAndInvalidCorrectionLineage() = withStore { store, _ ->
        val pediatric = observation()
        assertThrows(IllegalArgumentException::class.java) {
            store.append(pediatric, ClinicalPopulation.NEONATAL)
        }
        assertTrue(store.readEpisode(pediatric.episodeId, ClinicalPopulation.PEDIATRIC).isEmpty())

        val missingParent = observation(
            condition = ObservationCondition.CORRECTED,
            correctsObservationId = UUID.randomUUID().toString(),
        )
        assertThrows(IllegalArgumentException::class.java) {
            store.append(missingParent, ClinicalPopulation.PEDIATRIC)
        }

        val original = observation()
        store.append(original, ClinicalPopulation.PEDIATRIC)
        val otherEpisodeCorrection = observation(
            episodeId = "different-synthetic-episode",
            condition = ObservationCondition.CORRECTED,
            correctsObservationId = original.id,
        )
        assertThrows(IllegalArgumentException::class.java) {
            store.append(otherEpisodeCorrection, ClinicalPopulation.PEDIATRIC)
        }
        assertEquals(listOf(original), store.readEpisode(original.episodeId, ClinicalPopulation.PEDIATRIC))
    }

    @Test
    fun modifiedCiphertextFailsAuthentication() = withStore { store, directory ->
        val observation = observation()
        store.append(observation, ClinicalPopulation.PEDIATRIC)

        val encryptedFile = File(directory, "${observation.id}.enc")
        val tamperedBytes = encryptedFile.readBytes()
        tamperedBytes[tamperedBytes.lastIndex] = (tamperedBytes.last().toInt() xor 0x01).toByte()
        encryptedFile.writeBytes(tamperedBytes)

        val failure = assertThrows(IOException::class.java) {
            store.readById(observation.id)
        }
        assertNotNull(failure.cause)
    }

    @Test
    fun incompleteOrUnknownEncryptedFileFailsClosed() = withStore { store, directory ->
        val observation = observation()
        store.append(observation, ClinicalPopulation.PEDIATRIC)

        File(directory, "${UUID.randomUUID()}.enc").writeBytes(byteArrayOf(0x55, 0x43, 0x49, 0x4f))

        assertThrows(IOException::class.java) {
            store.readEpisode(observation.episodeId, ClinicalPopulation.PEDIATRIC)
        }
    }

    private fun observation(
        id: String = UUID.randomUUID().toString(),
        episodeId: String = "synthetic-episode-01",
        population: ClinicalPopulation = ClinicalPopulation.PEDIATRIC,
        recordedAtMillis: Long = 20_000L,
        condition: ObservationCondition = ObservationCondition.OBSERVED,
        correctsObservationId: String? = null,
        value: String = "SYNTHETIC-OBSERVATION-VALUE",
    ) = ClinicalObservationRecord(
        id = id,
        episodeId = episodeId,
        population = population,
        parameterCode = "SYNTHETIC-PARAMETER",
        valueText = value,
        unitCode = "SYNTHETIC-UNIT",
        observedAtMillis = 10_000L,
        recordedAtMillis = recordedAtMillis,
        authorId = "synthetic-test-author",
        origin = ObservationOrigin.MANUAL,
        condition = condition,
        correctsObservationId = correctsObservationId,
    )

    private fun withStore(block: (EncryptedObservationLedgerStore, File) -> Unit) {
        val directoryName = "observation-ledger-test-${UUID.randomUUID()}"
        val directory = File(context.filesDir, directoryName)
        try {
            block(EncryptedObservationLedgerStore(context, directoryName), directory)
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun containsBytes(haystack: ByteArray, needle: ByteArray): Boolean {
        if (needle.isEmpty() || needle.size > haystack.size) return false
        return (0..haystack.size - needle.size).any { start ->
            needle.indices.all { index -> haystack[start + index] == needle[index] }
        }
    }
}
