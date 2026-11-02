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
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.KeyStore
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Append-only local ledger. No edit or delete operation is exposed. */
internal class EncryptedObservationLedgerStore(
    context: Context,
    storageDirectoryName: String = RECORDS_DIRECTORY,
    keyAliasSuffix: String = storageDirectoryName,
) {
    private val appContext = context.applicationContext
    private val recordsDirectory: File
    private val encryptionKeyAlias: String
    private val integrityKeyAlias: String
    private val anchorMarkerAliasPrefix: String
    private val anchorSentinelAlias: String

    init {
        require(storageDirectoryName.matches(STORAGE_DIRECTORY_NAME_PATTERN)) {
            "Nombre del almacenamiento local no válido."
        }
        require(keyAliasSuffix.matches(KEY_ALIAS_SUFFIX_PATTERN)) {
            "Sufijo de clave local no válido."
        }
        recordsDirectory = File(appContext.filesDir, storageDirectoryName)
        encryptionKeyAlias = "$ENCRYPTION_KEY_ALIAS_PREFIX-$keyAliasSuffix"
        integrityKeyAlias = "$INTEGRITY_KEY_ALIAS_PREFIX-$keyAliasSuffix"
        anchorMarkerAliasPrefix = "$ANCHOR_MARKER_ALIAS_PREFIX-$keyAliasSuffix-"
        anchorSentinelAlias = "$ANCHOR_SENTINEL_ALIAS_PREFIX-$keyAliasSuffix"
    }

    fun append(record: ClinicalObservationRecord, expectedPopulation: ClinicalPopulation) {
        synchronized(STORE_LOCK) {
            val issues = record.validationIssues(expectedPopulation)
            if (issues.isNotEmpty()) {
                throw IllegalArgumentException("Registro de observación no válido: ${issues.joinToString()}.")
            }
            if (!isValidId(record.id)) throw IllegalArgumentException("Identificador de observación no válido.")
            validateRecordSize(record)

            val existing = readAllEntries()
            if (existing.any { it.record.id == record.id }) {
                throw IllegalArgumentException("El identificador ya existe; el registro es inmutable.")
            }
            record.correctsObservationId?.let { originalId ->
                val original = existing.singleOrNull { it.record.id == originalId }
                    ?: throw IllegalArgumentException("No se encontró la observación que se intenta corregir.")
                if (original.record.episodeId != record.episodeId ||
                    original.record.population != record.population
                ) {
                    throw IllegalArgumentException("La corrección debe pertenecer al mismo episodio y población.")
                }
            }

            val lastEntry = existing.lastOrNull()
            if (lastEntry?.sequence == Long.MAX_VALUE) {
                throw IOException("Se alcanzó el límite de secuencia del ledger.")
            }
            val sequence = (lastEntry?.sequence ?: 0L) + 1L
            val previousDigest = lastEntry?.fileDigest ?: EMPTY_CHAIN_DIGEST
            ensureDirectory()
            val fileDigest = writeEncryptedRecord(record, sequence, previousDigest)
            writeAuthenticatedHead(
                LedgerHead(
                    sequence = sequence,
                    observationId = record.id,
                    fileDigest = fileDigest,
                ),
            )
            ensureMonotonicAnchor(sequence)
        }
    }

    fun readEpisode(episodeId: String, population: ClinicalPopulation): List<ClinicalObservationRecord> =
        synchronized(STORE_LOCK) {
            if (episodeId.isBlank()) throw IllegalArgumentException("Referencia de episodio vacía.")
            readAllEntries()
                .asSequence()
                .map(LedgerEntry::record)
                .filter { it.episodeId == episodeId && it.population == population }
                .toList()
        }

    fun readById(id: String): ClinicalObservationRecord? = synchronized(STORE_LOCK) {
        if (!isValidId(id)) return@synchronized null
        readAllEntries().singleOrNull { it.record.id == id }?.record
    }

    private fun readAllEntries(): List<LedgerEntry> {
        val encryptionKeyExists = keyExists(encryptionKeyAlias)
        val integrityKeyExists = keyExists(integrityKeyAlias)
        val anchorState = readAnchorState()
        if (!recordsDirectory.exists()) {
            if (anchorState.hasSentinel || anchorState.markerSequences.isNotEmpty()) {
                throw IOException("Falta el ledger asociado al ancla de Android Keystore.")
            }
            if (encryptionKeyExists || integrityKeyExists) clearUncommittedLedgerKeys()
            return emptyList()
        }
        if (!recordsDirectory.isDirectory) {
            throw IOException("El almacenamiento local de observaciones no está disponible.")
        }

        val files = recordsDirectory.listFiles { file -> file.name.endsWith(RECORD_SUFFIX) }
            ?: throw IOException("No se pudo enumerar el libro local cifrado.")
        if (files.isEmpty()) {
            if (headFile().exists() || anchorState.hasSentinel || anchorState.markerSequences.isNotEmpty()) {
                throw IOException("El checkpoint autenticado indica que faltan registros del ledger.")
            }
            if (encryptionKeyExists || integrityKeyExists) clearUncommittedLedgerKeys()
            recordsDirectory.listFiles { file -> file.name.endsWith(TEMP_SUFFIX) }
                ?.forEach { it.delete() }
            return emptyList()
        }

        val entries = files.map { file ->
            val id = file.name.removeSuffix(RECORD_SUFFIX)
            if (!isValidId(id)) throw IOException("Se encontró un registro local no válido.")
            readEncryptedRecord(file, id)
        }.sortedBy(LedgerEntry::sequence)

        if (entries.map { it.record.id }.distinct().size != entries.size) {
            throw IOException("El libro contiene identificadores duplicados.")
        }
        entries.forEachIndexed { index, entry ->
            val expectedSequence = index + 1L
            if (entry.sequence != expectedSequence) {
                throw IOException("La secuencia del ledger está incompleta o fue reordenada.")
            }
            val expectedPreviousDigest = if (index == 0) {
                EMPTY_CHAIN_DIGEST
            } else {
                entries[index - 1].fileDigest
            }
            if (!MessageDigest.isEqual(entry.previousFileDigest, expectedPreviousDigest)) {
                throw IOException("La cadena de integridad del ledger está rota.")
            }
        }

        val entriesById = entries.associateBy { it.record.id }
        entries.forEach { entry ->
            val correctedId = entry.record.correctsObservationId ?: return@forEach
            val original = entriesById[correctedId]
                ?: throw IOException("El historial de corrección está incompleto.")
            if (original.sequence >= entry.sequence ||
                original.record.episodeId != entry.record.episodeId ||
                original.record.population != entry.record.population
            ) {
                throw IOException("La corrección no coincide con su historial de episodio y población.")
            }
        }

        val lastEntry = entries.last()
        if (anchorState.latestSequence > lastEntry.sequence) {
            throw IOException("El estado del ledger es anterior al ancla persistida en Android Keystore.")
        }
        val currentHead = if (headFile().exists()) readAuthenticatedHead() else null
        if (currentHead != null) {
            if (currentHead.sequence > lastEntry.sequence) {
                throw IOException("El checkpoint apunta a registros que ya no están presentes.")
            }
            val headEntry = entries[currentHead.sequence.toInt() - 1]
            if (currentHead.observationId != headEntry.record.id ||
                !MessageDigest.isEqual(currentHead.fileDigest, headEntry.fileDigest)
            ) {
                throw IOException("El checkpoint autenticado no coincide con la cadena del ledger.")
            }
        }

        if (currentHead == null || currentHead.sequence < lastEntry.sequence) {
            writeAuthenticatedHead(
                LedgerHead(lastEntry.sequence, lastEntry.record.id, lastEntry.fileDigest),
            )
        }
        ensureMonotonicAnchor(lastEntry.sequence, anchorState)
        return entries
    }

    private fun writeEncryptedRecord(
        record: ClinicalObservationRecord,
        sequence: Long,
        previousFileDigest: ByteArray,
    ): ByteArray {
        val destination = recordFile(record.id)
        if (destination.exists()) throw IOException("El registro local ya existe.")
        val temporary = File(recordsDirectory, "${record.id}$TEMP_SUFFIX")
        try {
            val cleartext = serialize(sequence, previousFileDigest, record)
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
            moveNewFile(temporary, destination)
            return sha256(destination.readBytes())
        } catch (error: Exception) {
            temporary.delete()
            throw if (error is IOException) error else IOException(
                "No se pudo guardar la observación cifrada.",
                error,
            )
        }
    }

    private fun readEncryptedRecord(file: File, expectedId: String): LedgerEntry {
        if (!file.isFile || file.length() !in MIN_ENCRYPTED_FILE_BYTES..MAX_ENCRYPTED_FILE_BYTES) {
            throw IOException("Un registro cifrado está incompleto o fuera de tamaño.")
        }
        try {
            val encoded = FileInputStream(file).use { it.readBytes() }
            if (encoded.size !in MIN_ENCRYPTED_FILE_BYTES..MAX_ENCRYPTED_FILE_BYTES) {
                throw IOException("Un registro cifrado cambió de tamaño durante la lectura.")
            }
            val input = DataInputStream(ByteArrayInputStream(encoded))
            val magic = ByteArray(MAGIC.size)
            input.readFully(magic)
            if (!magic.contentEquals(MAGIC)) throw IOException("Formato de registro no válido.")
            if (input.read() != FORMAT_VERSION) throw IOException("Versión de registro no compatible.")
            val ivLength = input.read()
            if (ivLength !in MIN_IV_LENGTH..MAX_IV_LENGTH) {
                throw IOException("Parámetros de cifrado no válidos.")
            }
            val iv = ByteArray(ivLength)
            input.readFully(iv)
            val ciphertext = input.readBytes()
            if (ciphertext.size < GCM_TAG_BYTES || ciphertext.size > MAX_CIPHERTEXT_BYTES) {
                throw IOException("Ciphertext de observación incompleto.")
            }
            val cleartext = createCipher(Cipher.DECRYPT_MODE, expectedId, iv).doFinal(ciphertext)
            val entry = deserialize(cleartext, expectedId)
            if (entry.record.validationIssues(entry.record.population).isNotEmpty()) {
                throw IOException("La observación cifrada no supera la validación de integridad.")
            }
            validateRecordSize(entry.record)
            return entry.copy(fileDigest = sha256(encoded))
        } catch (error: Exception) {
            throw if (error is IOException) error else IOException(
                "No se pudo autenticar o descifrar una observación local.",
                error,
            )
        }
    }

    private fun serialize(
        sequence: Long,
        previousFileDigest: ByteArray,
        record: ClinicalObservationRecord,
    ): ByteArray = ByteArrayOutputStream().also { buffer ->
        DataOutputStream(buffer).use { output ->
            output.writeInt(SCHEMA_VERSION)
            output.writeLong(sequence)
            output.writeInt(previousFileDigest.size)
            output.write(previousFileDigest)
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

    private fun deserialize(cleartext: ByteArray, expectedId: String): LedgerEntry {
        DataInputStream(ByteArrayInputStream(cleartext)).use { input ->
            if (input.readInt() != SCHEMA_VERSION) throw IOException("Versión de observación no compatible.")
            val sequence = input.readLong()
            if (sequence <= 0L) throw IOException("Secuencia de ledger no válida.")
            val digestLength = input.readInt()
            if (digestLength != SHA256_BYTES) throw IOException("Digest de cadena no válido.")
            val previousFileDigest = ByteArray(digestLength)
            input.readFully(previousFileDigest)
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
            if (id != expectedId || input.available() != 0) {
                throw IOException("Metadatos de observación no válidos.")
            }
            return LedgerEntry(
                sequence = sequence,
                previousFileDigest = previousFileDigest,
                record = ClinicalObservationRecord(
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
                ),
                fileDigest = EMPTY_CHAIN_DIGEST,
            )
        }
    }

    private fun writeAuthenticatedHead(head: LedgerHead) {
        val destination = headFile()
        val temporary = File(recordsDirectory, "$HEAD_FILE_NAME$TEMP_SUFFIX")
        try {
            val payload = serializeHeadPayload(head)
            val mac = calculateHmac(payload, getOrCreateIntegrityKey())
            FileOutputStream(temporary).use { output ->
                output.write(payload)
                output.write(mac)
                output.fd.sync()
            }
            moveReplacingFile(temporary, destination)
        } catch (error: Exception) {
            temporary.delete()
            throw if (error is IOException) error else IOException(
                "No se pudo actualizar el checkpoint autenticado del ledger.",
                error,
            )
        }
    }

    private fun readAuthenticatedHead(): LedgerHead {
        val file = headFile()
        if (!file.isFile || file.length() !in MIN_HEAD_FILE_BYTES..MAX_HEAD_FILE_BYTES) {
            throw IOException("Falta o está dañado el checkpoint autenticado del ledger.")
        }
        try {
            val encoded = file.readBytes()
            if (encoded.size !in MIN_HEAD_FILE_BYTES..MAX_HEAD_FILE_BYTES) {
                throw IOException("Checkpoint del ledger incompleto.")
            }
            val payloadSize = encoded.size - HMAC_BYTES
            val payload = encoded.copyOfRange(0, payloadSize)
            val suppliedMac = encoded.copyOfRange(payloadSize, encoded.size)
            val expectedMac = calculateHmac(payload, getExistingIntegrityKey())
            if (!MessageDigest.isEqual(suppliedMac, expectedMac)) {
                throw IOException("La autenticación del checkpoint del ledger falló.")
            }
            return deserializeHeadPayload(payload)
        } catch (error: Exception) {
            throw if (error is IOException) error else IOException(
                "No se pudo autenticar o leer el checkpoint del ledger.",
                error,
            )
        }
    }

    private fun serializeHeadPayload(head: LedgerHead): ByteArray = ByteArrayOutputStream().also { buffer ->
        DataOutputStream(buffer).use { output ->
            output.write(HEAD_MAGIC)
            output.writeInt(HEAD_FORMAT_VERSION)
            output.writeLong(head.sequence)
            output.writeUTF(head.observationId)
            output.writeInt(head.fileDigest.size)
            output.write(head.fileDigest)
        }
    }.toByteArray()

    private fun deserializeHeadPayload(payload: ByteArray): LedgerHead {
        DataInputStream(ByteArrayInputStream(payload)).use { input ->
            val magic = ByteArray(HEAD_MAGIC.size)
            input.readFully(magic)
            if (!magic.contentEquals(HEAD_MAGIC) || input.readInt() != HEAD_FORMAT_VERSION) {
                throw IOException("Formato de checkpoint no compatible.")
            }
            val sequence = input.readLong()
            val observationId = input.readUTF()
            val digestLength = input.readInt()
            if (sequence <= 0L || !isValidId(observationId) || digestLength != SHA256_BYTES) {
                throw IOException("Metadatos del checkpoint no válidos.")
            }
            val fileDigest = ByteArray(digestLength)
            input.readFully(fileDigest)
            if (input.available() != 0) throw IOException("Checkpoint del ledger malformado.")
            return LedgerHead(sequence, observationId, fileDigest)
        }
    }

    private fun createCipher(mode: Int, id: String, iv: ByteArray? = null): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        if (mode == Cipher.ENCRYPT_MODE) {
            cipher.init(mode, getOrCreateEncryptionKey())
        } else {
            cipher.init(
                mode,
                getExistingEncryptionKey(),
                GCMParameterSpec(GCM_TAG_BITS, requireNotNull(iv)),
            )
        }
        cipher.updateAAD(associatedData(id))
        return cipher
    }

    private fun associatedData(id: String): ByteArray =
        MAGIC + byteArrayOf(FORMAT_VERSION.toByte()) + id.toByteArray(Charsets.UTF_8)

    private fun getOrCreateEncryptionKey(): SecretKey = getOrCreateKey(
        alias = encryptionKeyAlias,
        algorithm = KeyProperties.KEY_ALGORITHM_AES,
    ) { alias ->
        KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(AES_KEY_SIZE_BITS)
            .setRandomizedEncryptionRequired(true)
            .setUserAuthenticationRequired(false)
            .build()
    }

    private fun getOrCreateIntegrityKey(): SecretKey = getOrCreateKey(
        alias = integrityKeyAlias,
        algorithm = KeyProperties.KEY_ALGORITHM_HMAC_SHA256,
    ) { alias ->
        KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
        )
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setKeySize(HMAC_KEY_SIZE_BITS)
            .setUserAuthenticationRequired(false)
            .build()
    }

    private fun getOrCreateKey(
        alias: String,
        algorithm: String,
        spec: (String) -> KeyGenParameterSpec,
    ): SecretKey = synchronized(KEY_LOCK) {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return@synchronized it }
        val generator = KeyGenerator.getInstance(algorithm, ANDROID_KEYSTORE)
        generator.init(spec(alias))
        generator.generateKey()
    }

    private fun getExistingEncryptionKey(): SecretKey = getExistingKey(encryptionKeyAlias)

    private fun getExistingIntegrityKey(): SecretKey = getExistingKey(integrityKeyAlias)

    private fun getExistingKey(alias: String): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(alias, null) as? SecretKey
            ?: throw IOException("Falta una clave Android Keystore requerida para el ledger.")
    }

    private fun keyExists(alias: String): Boolean = try {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.containsAlias(alias)
    } catch (error: Exception) {
        throw IOException("No se pudo comprobar el estado de las claves del ledger.", error)
    }

    /** Reads the Keystore-side commit marker, which is separate from app-private ledger files. */
    private fun readAnchorState(): AnchorState = try {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val aliases = buildList {
            val enumeration = keyStore.aliases()
            while (enumeration.hasMoreElements()) add(enumeration.nextElement())
        }
        val markerSequences = aliases.asSequence()
            .filter { it.startsWith(anchorMarkerAliasPrefix) }
            .map { alias ->
                val suffix = alias.removePrefix(anchorMarkerAliasPrefix)
                if (!suffix.matches(ANCHOR_SEQUENCE_PATTERN)) {
                    throw IOException("Alias de ancla de ledger no válido.")
                }
                suffix.toLongOrNull()?.takeIf { it > 0L }
                    ?: throw IOException("Secuencia de ancla de ledger no válida.")
            }
            .toList()
        val hasSentinel = keyStore.containsAlias(anchorSentinelAlias)
        AnchorState(hasSentinel, markerSequences)
    } catch (error: Exception) {
        throw if (error is IOException) error else IOException(
            "No se pudo leer el ancla monotónica de Android Keystore.",
            error,
        )
    }

    private fun ensureMonotonicAnchor(sequence: Long, state: AnchorState = readAnchorState()) {
        if (state.latestSequence > sequence) {
            throw IOException("El ledger está por detrás del ancla persistida en Android Keystore.")
        }
        if (state.hasSentinel && state.markerSequences.isEmpty()) {
            throw IOException("Falta el marcador monotónico del ledger.")
        }

        if (state.latestSequence < sequence) createAnchorKey(anchorMarkerAlias(sequence))
        if (!state.hasSentinel) createAnchorKey(anchorSentinelAlias)

        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        readAnchorState().markerSequences
            .filter { it < sequence }
            .forEach { oldSequence -> keyStore.deleteEntry(anchorMarkerAlias(oldSequence)) }
        val committed = readAnchorState()
        if (!committed.hasSentinel || committed.latestSequence != sequence) {
            throw IOException("No se pudo confirmar la secuencia del ledger en Android Keystore.")
        }
    }

    private fun createAnchorKey(alias: String) {
        getOrCreateKey(
            alias = alias,
            algorithm = KeyProperties.KEY_ALGORITHM_HMAC_SHA256,
        ) { keyAlias ->
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
            )
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setKeySize(HMAC_KEY_SIZE_BITS)
                .setUserAuthenticationRequired(false)
                .build()
        }
    }

    private fun anchorMarkerAlias(sequence: Long): String =
        "$anchorMarkerAliasPrefix${sequence.toString().padStart(ANCHOR_SEQUENCE_DIGITS, '0')}"

    private fun clearUncommittedLedgerKeys() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            keyStore.deleteEntry(encryptionKeyAlias)
            keyStore.deleteEntry(integrityKeyAlias)
        } catch (error: Exception) {
            throw IOException("No se pudieron limpiar claves de un append interrumpido sin registros.", error)
        }
    }

    private fun calculateHmac(payload: ByteArray, key: SecretKey): ByteArray =
        Mac.getInstance(HMAC_TRANSFORMATION).run {
            init(key)
            doFinal(payload)
        }

    private fun sha256(bytes: ByteArray): ByteArray =
        MessageDigest.getInstance(SHA256_TRANSFORMATION).digest(bytes)

    private fun ensureDirectory() {
        if (!recordsDirectory.exists() && !recordsDirectory.mkdirs()) {
            throw IOException("No se pudo crear el libro local de observaciones.")
        }
        if (!recordsDirectory.isDirectory) {
            throw IOException("El almacenamiento de observaciones no está disponible.")
        }
    }

    private fun moveNewFile(temporary: File, destination: File) {
        try {
            Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } catch (error: AtomicMoveNotSupportedException) {
            Files.move(temporary.toPath(), destination.toPath())
        }
    }

    private fun moveReplacingFile(temporary: File, destination: File) {
        try {
            Files.move(
                temporary.toPath(),
                destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (error: AtomicMoveNotSupportedException) {
            Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun recordFile(id: String) = File(recordsDirectory, "$id$RECORD_SUFFIX")

    private fun headFile() = File(recordsDirectory, HEAD_FILE_NAME)

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

    private data class LedgerEntry(
        val sequence: Long,
        val previousFileDigest: ByteArray,
        val record: ClinicalObservationRecord,
        val fileDigest: ByteArray,
    )

    private data class LedgerHead(
        val sequence: Long,
        val observationId: String,
        val fileDigest: ByteArray,
    )

    private data class AnchorState(
        val hasSentinel: Boolean,
        val markerSequences: List<Long>,
    ) {
        val latestSequence: Long get() = markerSequences.maxOrNull() ?: 0L
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ENCRYPTION_KEY_ALIAS_PREFIX = "uci_assistant_observation_ledger_v2"
        const val INTEGRITY_KEY_ALIAS_PREFIX = "uci_assistant_observation_integrity_v2"
        const val ANCHOR_MARKER_ALIAS_PREFIX = "uci_obs_anchor_commit_v2"
        const val ANCHOR_SENTINEL_ALIAS_PREFIX = "uci_obs_anchor_root_v2"
        const val ANCHOR_SEQUENCE_DIGITS = 20
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val HMAC_TRANSFORMATION = "HmacSHA256"
        const val SHA256_TRANSFORMATION = "SHA-256"
        const val AES_KEY_SIZE_BITS = 256
        const val HMAC_KEY_SIZE_BITS = 256
        const val GCM_TAG_BITS = 128
        const val GCM_TAG_BYTES = GCM_TAG_BITS / 8
        const val HMAC_BYTES = 32
        const val SHA256_BYTES = 32
        const val MIN_IV_LENGTH = 12
        const val MAX_IV_LENGTH = 16
        const val FORMAT_VERSION = 1
        const val SCHEMA_VERSION = 2
        const val HEAD_FORMAT_VERSION = 1
        const val MAX_FIELD_CHARACTERS = 4_096
        const val MAX_TOTAL_CHARACTERS = 32_768
        const val MAX_CIPHERTEXT_BYTES = 128 * 1024
        const val MAX_HEAD_FILE_BYTES = 256
        const val RECORDS_DIRECTORY = "encrypted-observation-ledger-v2"
        const val RECORD_SUFFIX = ".enc"
        const val TEMP_SUFFIX = ".tmp"
        const val HEAD_FILE_NAME = "ledger.head"
        val MAGIC = byteArrayOf(0x55, 0x43, 0x49, 0x4f) // UCIO
        val HEAD_MAGIC = byteArrayOf(0x55, 0x43, 0x49, 0x48) // UCIH
        val MIN_ENCRYPTED_FILE_BYTES = MAGIC.size + 2 + MIN_IV_LENGTH + GCM_TAG_BYTES
        val MAX_ENCRYPTED_FILE_BYTES = MAGIC.size + 2 + MAX_IV_LENGTH + MAX_CIPHERTEXT_BYTES
        val MIN_HEAD_FILE_BYTES = HEAD_MAGIC.size + 4 + 8 + 2 + 36 + 4 + SHA256_BYTES + HMAC_BYTES
        val EMPTY_CHAIN_DIGEST = ByteArray(SHA256_BYTES)
        val STORAGE_DIRECTORY_NAME_PATTERN = Regex("[A-Za-z0-9._-]{1,100}")
        val KEY_ALIAS_SUFFIX_PATTERN = Regex("[A-Za-z0-9._-]{1,100}")
        val ANCHOR_SEQUENCE_PATTERN = Regex("[0-9]{$ANCHOR_SEQUENCE_DIGITS}")
        val KEY_LOCK = Any()
        val STORE_LOCK = Any()
    }
}
