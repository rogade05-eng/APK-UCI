package org.uciasistant.core.clinicaldata

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

/** Append-only local ledger. No edit or delete operation is exposed. */
internal class EncryptedObservationLedgerStore(
    context: Context,
    storageDirectoryName: String = RECORDS_DIRECTORY,
) {
    private val appContext = context.applicationContext
    private val recordsDirectory: File

    init {
        require(storageDirectoryName.matches(STORAGE_DIRECTORY_NAME_PATTERN)) {
            "Nombre del almacenamiento local no válido."
        }
        recordsDirectory = File(appContext.filesDir, storageDirectoryName)
    }

    fun append(record: ClinicalObservationRecord, expectedPopulation: ClinicalPopulation) {
        synchronized(STORE_LOCK) {
            val issues = record.validationIssues(expectedPopulation)
            if (issues.isNotEmpty()) {
                throw IllegalArgumentException("Registro de observación no válido: ${issues.joinToString()}.")
            }
            if (!isValidId(record.id)) throw IllegalArgumentException("Identificador de observación no válido.")
            validateRecordSize(record)

            val existing = readAllRecords()
            if (existing.any { it.id == record.id }) {
                throw IllegalArgumentException("El identificador ya existe; el registro es inmutable.")
            }
            record.correctsObservationId?.let { originalId ->
                val original = existing.singleOrNull { it.id == originalId }
                    ?: throw IllegalArgumentException("No se encontró la observación que se intenta corregir.")
                if (original.episodeId != record.episodeId || original.population != record.population) {
                    throw IllegalArgumentException("La corrección debe pertenecer al mismo episodio y población.")
                }
            }

            ensureDirectory()
            writeEncryptedRecord(record)
        }
    }

    fun readEpisode(episodeId: String, population: ClinicalPopulation): List<ClinicalObservationRecord> =
        synchronized(STORE_LOCK) {
            if (episodeId.isBlank()) throw IllegalArgumentException("Referencia de episodio vacía.")
            readAllRecords()
                .filter { it.episodeId == episodeId && it.population == population }
                .sortedWith(compareBy(ClinicalObservationRecord::recordedAtMillis, ClinicalObservationRecord::id))
        }

    fun readById(id: String): ClinicalObservationRecord? = synchronized(STORE_LOCK) {
        if (!isValidId(id)) return@synchronized null
        readAllRecords().singleOrNull { it.id == id }
    }

    private fun readAllRecords(): List<ClinicalObservationRecord> {
        if (!recordsDirectory.exists()) return emptyList()
        if (!recordsDirectory.isDirectory) {
            throw IOException("El almacenamiento local de observaciones no está disponible.")
        }
        val files = recordsDirectory.listFiles { file -> file.name.endsWith(RECORD_SUFFIX) }
            ?: throw IOException("No se pudo enumerar el libro local cifrado.")
        val records = files.map { file ->
            val id = file.name.removeSuffix(RECORD_SUFFIX)
            if (!isValidId(id)) throw IOException("Se encontró un registro local no válido.")
            readEncryptedRecord(file, id)
        }
        if (records.map(ClinicalObservationRecord::id).distinct().size != records.size) {
            throw IOException("El libro contiene identificadores duplicados.")
        }
        val byId = records.associateBy(ClinicalObservationRecord::id)
        records.forEach { record ->
            val correctedId = record.correctsObservationId ?: return@forEach
            val original = byId[correctedId]
                ?: throw IOException("El historial de corrección está incompleto.")
            if (original.episodeId != record.episodeId || original.population != record.population) {
                throw IOException("La corrección no coincide con episodio y población.")
            }
        }
        return records
    }

    private fun writeEncryptedRecord(record: ClinicalObservationRecord) {
        val destination = recordFile(record.id)
        if (destination.exists()) throw IOException("El registro local ya existe.")
        val temporary = File(recordsDirectory, "${record.id}$TEMP_SUFFIX")
        try {
            val cleartext = serialize(record)
            val cipher = createCipher(Cipher.ENCRYPT_MODE, record.id)
            val ciphertext = cipher.doFinal(cleartext)
            if (cipher.iv.size !in MIN_IV_LENGTH..MAX_IV_LENGTH || ciphertext.size > MAX_CIPHERTEXT_BYTES) {
                throw IOException("El sistema no generó un registro cifrado compatible.")
            }
            FileOutputStream(temporary).use { output ->
                output.write(MAGIC)
                output.write(FORMAT_VERSION)
                output.write(cipher.iv.size)
                output.write(cipher.iv)
                output.write(ciphertext)
                output.fd.sync()
            }
            if (!temporary.renameTo(destination)) throw IOException("No se pudo publicar el registro cifrado.")
        } catch (error: Exception) {
            temporary.delete()
            throw if (error is IOException) error else IOException(
                "No se pudo guardar la observación cifrada.",
                error,
            )
        }
    }

    private fun readEncryptedRecord(file: File, expectedId: String): ClinicalObservationRecord {
        if (!file.isFile || file.length() !in MIN_ENCRYPTED_BYTES..MAX_CIPHERTEXT_BYTES) {
            throw IOException("Un registro cifrado está incompleto o fuera de tamaño.")
        }
        try {
            FileInputStream(file).use { input ->
                val magic = ByteArray(MAGIC.size)
                readFully(input, magic)
                if (!magic.contentEquals(MAGIC)) throw IOException("Formato de registro no válido.")
                if (input.read() != FORMAT_VERSION) throw IOException("Versión de registro no compatible.")
                val ivLength = input.read()
                if (ivLength !in MIN_IV_LENGTH..MAX_IV_LENGTH) {
                    throw IOException("Parámetros de cifrado no válidos.")
                }
                val iv = ByteArray(ivLength)
                readFully(input, iv)
                val ciphertext = input.readBytes()
                if (ciphertext.size < GCM_TAG_BYTES || ciphertext.size > MAX_CIPHERTEXT_BYTES) {
                    throw IOException("Ciphertext de observación incompleto.")
                }
                val cleartext = createCipher(Cipher.DECRYPT_MODE, expectedId, iv).doFinal(ciphertext)
                val record = deserialize(cleartext, expectedId)
                if (record.validationIssues(record.population).isNotEmpty()) {
                    throw IOException("La observación cifrada no supera la validación de integridad.")
                }
                return record
            }
        } catch (error: Exception) {
            throw if (error is IOException) error else IOException(
                "No se pudo autenticar o descifrar una observación local.",
                error,
            )
        }
    }

    private fun serialize(record: ClinicalObservationRecord): ByteArray = ByteArrayOutputStream().also { buffer ->
        DataOutputStream(buffer).use { output ->
            output.writeInt(SCHEMA_VERSION)
            output.writeUTF(record.id)
            output.writeUTF(record.episodeId)
            output.writeUTF(record.population.name)
            output.writeUTF(record.parameterCode)
            output.writeUTF(record.valueText)
            output.writeNullableString(record.unitCode)
            output.writeNullableLong(record.observedAtMillis)
            output.writeLong(record.recordedAtMillis)
            output.writeNullableString(record.authorId)
            output.writeUTF(record.origin.name)
            output.writeUTF(record.condition.name)
            output.writeNullableString(record.methodOrSite)
            output.writeNullableString(record.deviceId)
            output.writeNullableString(record.correctsObservationId)
        }
    }.toByteArray().also { bytes ->
        if (bytes.size > MAX_CIPHERTEXT_BYTES) throw IOException("La observación supera el tamaño permitido.")
    }

    private fun deserialize(cleartext: ByteArray, expectedId: String): ClinicalObservationRecord {
        DataInputStream(ByteArrayInputStream(cleartext)).use { input ->
            if (input.readInt() != SCHEMA_VERSION) throw IOException("Versión de observación no compatible.")
            val id = input.readUTF()
            val episodeId = input.readUTF()
            val population = enumValueOf<ClinicalPopulation>(input.readUTF())
            val parameterCode = input.readUTF()
            val valueText = input.readUTF()
            val unitCode = input.readNullableString()
            val observedAtMillis = input.readNullableLong()
            val recordedAtMillis = input.readLong()
            val authorId = input.readNullableString()
            val origin = enumValueOf<ObservationOrigin>(input.readUTF())
            val condition = enumValueOf<ObservationCondition>(input.readUTF())
            val methodOrSite = input.readNullableString()
            val deviceId = input.readNullableString()
            val correctsObservationId = input.readNullableString()
            if (id != expectedId || input.available() != 0) throw IOException("Metadatos de observación no válidos.")
            return ClinicalObservationRecord(
                id = id,
                episodeId = episodeId,
                population = population,
                parameterCode = parameterCode,
                valueText = valueText,
                unitCode = unitCode,
                observedAtMillis = observedAtMillis,
                recordedAtMillis = recordedAtMillis,
                authorId = authorId,
                origin = origin,
                condition = condition,
                methodOrSite = methodOrSite,
                deviceId = deviceId,
                correctsObservationId = correctsObservationId,
            )
        }
    }

    private fun validateRecordSize(record: ClinicalObservationRecord) {
        val values = listOfNotNull(
            record.id,
            record.episodeId,
            record.parameterCode,
            record.valueText,
            record.unitCode,
            record.authorId,
            record.methodOrSite,
            record.deviceId,
            record.correctsObservationId,
        )
        if (values.any { it.length > MAX_FIELD_CHARACTERS } || values.sumOf { it.length } > MAX_TOTAL_CHARACTERS) {
            throw IllegalArgumentException("La observación supera el tamaño permitido.")
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
        MAGIC + byteArrayOf(FORMAT_VERSION.toByte()) + id.toByteArray(Charsets.UTF_8)

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
            throw IOException("No se pudo crear el libro local de observaciones.")
        }
        if (!recordsDirectory.isDirectory) {
            throw IOException("El almacenamiento de observaciones no está disponible.")
        }
    }

    private fun recordFile(id: String) = File(recordsDirectory, "$id$RECORD_SUFFIX")

    private fun readFully(input: FileInputStream, destination: ByteArray) {
        var offset = 0
        while (offset < destination.size) {
            val count = input.read(destination, offset, destination.size - offset)
            if (count < 0) throw IOException("Archivo cifrado incompleto.")
            offset += count
        }
    }

    private fun isValidId(id: String): Boolean =
        runCatching { UUID.fromString(id).toString() == id }.getOrDefault(false)

    private fun DataOutputStream.writeNullableString(value: String?) {
        writeBoolean(value != null)
        value?.let { writeUTF(it) }
    }

    private fun DataOutputStream.writeNullableLong(value: Long?) {
        writeBoolean(value != null)
        value?.let { writeLong(it) }
    }

    private fun DataInputStream.readNullableString(): String? = if (readBoolean()) readUTF() else null

    private fun DataInputStream.readNullableLong(): Long? = if (readBoolean()) readLong() else null

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "uci_assistant_observation_ledger_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_SIZE_BITS = 256
        const val GCM_TAG_BITS = 128
        const val GCM_TAG_BYTES = GCM_TAG_BITS / 8
        const val MIN_IV_LENGTH = 12
        const val MAX_IV_LENGTH = 16
        const val FORMAT_VERSION = 1
        const val SCHEMA_VERSION = 1
        const val MAGIC_BYTES = 4
        const val MIN_ENCRYPTED_BYTES = MAGIC_BYTES + 2 + MIN_IV_LENGTH + GCM_TAG_BYTES
        const val MAX_FIELD_CHARACTERS = 4_096
        const val MAX_TOTAL_CHARACTERS = 32_768
        const val MAX_CIPHERTEXT_BYTES = 128 * 1024
        const val RECORDS_DIRECTORY = "encrypted-observation-ledger-v1"
        const val RECORD_SUFFIX = ".enc"
        const val TEMP_SUFFIX = ".tmp"
        val STORAGE_DIRECTORY_NAME_PATTERN = Regex("[A-Za-z0-9._-]{1,100}")
        val MAGIC = byteArrayOf(0x55, 0x43, 0x49, 0x4f) // UCIO
        val KEY_LOCK = Any()
        val STORE_LOCK = Any()
    }
}
