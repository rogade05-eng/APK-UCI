package org.uciasistant.feature.handoff

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal data class StoredHandoffRecord(
    val id: String,
    val createdAtMillis: Long,
    val draft: HandoffDraft,
)

/** App-private, encrypted store for immutable local handoff snapshots. */
internal class EncryptedHandoffStore(context: Context) {
    private val appContext = context.applicationContext
    private val recordsDirectory = File(appContext.filesDir, RECORDS_DIRECTORY)

    @Synchronized
    fun saveNew(draft: HandoffDraft): StoredHandoffRecord {
        validateDraftSize(draft)
        if (!draft.hasContent) throw IllegalArgumentException("El borrador está vacío.")
        ensureDirectory()

        val record = StoredHandoffRecord(
            id = UUID.randomUUID().toString(),
            createdAtMillis = System.currentTimeMillis(),
            draft = draft,
        )
        val destination = recordFile(record.id)
        if (destination.exists()) throw IOException("No se pudo crear un identificador local único.")
        val temporary = File(recordsDirectory, "${record.id}$TEMP_SUFFIX")

        try {
            val encrypted = encrypt(serialize(record), record.id)
            FileOutputStream(temporary).use { output ->
                output.write(encrypted)
                output.fd.sync()
            }
            if (!temporary.renameTo(destination)) {
                throw IOException("No se pudo guardar el borrador cifrado local.")
            }
            return record
        } catch (error: Exception) {
            temporary.delete()
            throw if (error is IOException) error else IOException(
                "No se pudo guardar el borrador cifrado local.",
                error,
            )
        }
    }

    @Synchronized
    fun listRecords(): List<StoredHandoffRecord> {
        if (!recordsDirectory.exists()) return emptyList()
        if (!recordsDirectory.isDirectory) {
            throw IOException("El almacenamiento local de borradores no está disponible.")
        }
        val files = recordsDirectory.listFiles { file -> file.name.endsWith(RECORD_SUFFIX) }
            ?: throw IOException("No se pudo enumerar el almacenamiento local cifrado.")
        return files.map { file ->
            val id = file.name.removeSuffix(RECORD_SUFFIX)
            if (!isValidId(id)) throw IOException("Se encontró un archivo local no válido.")
            decryptRecord(file, id)
        }.sortedByDescending(StoredHandoffRecord::createdAtMillis)
    }

    @Synchronized
    fun readRecord(id: String): StoredHandoffRecord {
        if (!isValidId(id)) throw IOException("Identificador de borrador no válido.")
        val file = recordFile(id)
        if (!file.isFile) throw IOException("No se encontró el borrador local.")
        return decryptRecord(file, id)
    }

    @Synchronized
    fun deleteRecord(id: String): Boolean {
        if (!isValidId(id)) return false
        val file = recordFile(id)
        return !file.exists() || file.delete()
    }

    private fun decryptRecord(file: File, expectedId: String): StoredHandoffRecord {
        if (!file.isFile || file.length() !in MIN_ENCRYPTED_BYTES..MAX_RECORD_BYTES) {
            throw IOException("Un borrador local está incompleto o fuera de tamaño.")
        }
        try {
            FileInputStream(file).use { input ->
                val magic = ByteArray(MAGIC.size)
                readFully(input, magic)
                if (!magic.contentEquals(MAGIC)) throw IOException("Formato de borrador no válido.")

                val formatVersion = input.read()
                if (formatVersion != FORMAT_VERSION) {
                    throw IOException("Versión de almacenamiento no compatible.")
                }
                val ivLength = input.read()
                if (ivLength !in MIN_IV_LENGTH..MAX_IV_LENGTH) {
                    throw IOException("Parámetros de cifrado no válidos.")
                }
                val iv = ByteArray(ivLength)
                readFully(input, iv)
                val ciphertext = input.readBytes()
                if (ciphertext.size < GCM_TAG_BYTES || ciphertext.size > MAX_RECORD_BYTES) {
                    throw IOException("Borrador cifrado incompleto.")
                }
                val cleartext = createCipher(Cipher.DECRYPT_MODE, expectedId, iv)
                    .doFinal(ciphertext)
                return deserialize(cleartext, expectedId)
            }
        } catch (error: Exception) {
            throw if (error is IOException) error else IOException(
                "No se pudo autenticar o descifrar un borrador local.",
                error,
            )
        }
    }

    private fun encrypt(cleartext: ByteArray, id: String): ByteArray {
        val cipher = createCipher(Cipher.ENCRYPT_MODE, id)
        val ciphertext = cipher.doFinal(cleartext)
        if (cipher.iv.size !in MIN_IV_LENGTH..MAX_IV_LENGTH) {
            throw IOException("El sistema no generó parámetros de cifrado compatibles.")
        }
        if (ciphertext.size > MAX_RECORD_BYTES) throw IOException("El borrador es demasiado grande.")
        return ByteArrayOutputStream().also { buffer ->
            buffer.write(MAGIC)
            buffer.write(FORMAT_VERSION)
            buffer.write(cipher.iv.size)
            buffer.write(cipher.iv)
            buffer.write(ciphertext)
        }.toByteArray()
    }

    private fun serialize(record: StoredHandoffRecord): ByteArray {
        val fields = record.draft.fieldsInStorageOrder()
        if (fields.size != HandoffDraft.STORED_FIELD_COUNT) {
            throw IOException("La estructura del borrador no es compatible.")
        }
        return ByteArrayOutputStream().also { buffer ->
            DataOutputStream(buffer).use { output ->
                output.writeInt(SCHEMA_VERSION)
                output.writeUTF(record.id)
                output.writeLong(record.createdAtMillis)
                output.writeInt(fields.size)
                fields.forEach(output::writeUTF)
            }
        }.toByteArray().also { bytes ->
            if (bytes.size > MAX_RECORD_BYTES) throw IOException("El borrador es demasiado grande.")
        }
    }

    private fun deserialize(cleartext: ByteArray, expectedId: String): StoredHandoffRecord {
        DataInputStream(ByteArrayInputStream(cleartext)).use { input ->
            if (input.readInt() != SCHEMA_VERSION) {
                throw IOException("Versión de borrador no compatible.")
            }
            val id = input.readUTF()
            val createdAtMillis = input.readLong()
            val fieldCount = input.readInt()
            if (id != expectedId || !isValidId(id) || createdAtMillis < 0L) {
                throw IOException("Metadatos de borrador no válidos.")
            }
            if (fieldCount != HandoffDraft.STORED_FIELD_COUNT) {
                throw IOException("Estructura de borrador no válida.")
            }
            val fields = List(fieldCount) { input.readUTF() }
            if (input.available() != 0) throw IOException("Datos sobrantes en el borrador.")
            return StoredHandoffRecord(
                id = id,
                createdAtMillis = createdAtMillis,
                draft = HandoffDraft.fromStorageFields(fields),
            )
        }
    }

    private fun validateDraftSize(draft: HandoffDraft) {
        val fields = draft.fieldsInStorageOrder()
        if (fields.any { it.length > MAX_FIELD_CHARACTERS } ||
            fields.sumOf { it.length } > MAX_TOTAL_CHARACTERS
        ) {
            throw IllegalArgumentException("El borrador supera el tamaño permitido.")
        }
    }

    private fun createCipher(mode: Int, id: String, iv: ByteArray? = null): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        if (mode == Cipher.ENCRYPT_MODE) {
            cipher.init(mode, getOrCreateKey())
        } else {
            cipher.init(mode, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, requireNotNull(iv)))
        }
        cipher.updateAAD(associatedData(id))
        return cipher
    }

    private fun associatedData(id: String): ByteArray =
        (MAGIC + byteArrayOf(FORMAT_VERSION.toByte()) + id.toByteArray(Charsets.UTF_8))

    private fun readFully(input: FileInputStream, destination: ByteArray) {
        var offset = 0
        while (offset < destination.size) {
            val count = input.read(destination, offset, destination.size - offset)
            if (count < 0) throw IOException("Archivo de borrador cifrado incompleto.")
            offset += count
        }
    }

    private fun getOrCreateKey(): SecretKey = synchronized(KEY_LOCK) {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return@synchronized it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .setRandomizedEncryptionRequired(true)
                .setUserAuthenticationRequired(false)
                .build(),
        )
        generator.generateKey()
    }

    private fun ensureDirectory() {
        if (!recordsDirectory.exists() && !recordsDirectory.mkdirs()) {
            throw IOException("No se pudo abrir el almacenamiento local de borradores.")
        }
        if (!recordsDirectory.isDirectory) {
            throw IOException("El almacenamiento local de borradores no está disponible.")
        }
    }

    private fun recordFile(id: String) = File(recordsDirectory, "$id$RECORD_SUFFIX")

    private fun isValidId(id: String): Boolean =
        runCatching { UUID.fromString(id).toString() == id }.getOrDefault(false)

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "uci_assistant_handoff_records_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_SIZE_BITS = 256
        const val GCM_TAG_BITS = 128
        const val GCM_TAG_BYTES = GCM_TAG_BITS / 8
        const val MIN_IV_LENGTH = 12
        const val MAX_IV_LENGTH = 16
        const val FORMAT_VERSION = 1
        const val SCHEMA_VERSION = 1
        const val MAX_FIELD_CHARACTERS = 4_096
        const val MAX_TOTAL_CHARACTERS = 32_768
        const val MAX_RECORD_BYTES = 256 * 1024
        const val MIN_ENCRYPTED_BYTES = MAGIC_BYTES + 2 + MIN_IV_LENGTH + GCM_TAG_BYTES
        const val RECORDS_DIRECTORY = "encrypted-handoff-records-v1"
        const val RECORD_SUFFIX = ".enc"
        const val TEMP_SUFFIX = ".tmp"
        val MAGIC = byteArrayOf(0x55, 0x43, 0x49, 0x48) // UCIH
        const val MAGIC_BYTES = 4
        val KEY_LOCK = Any()
    }
}
