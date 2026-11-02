package org.uciasistant.feature.alerts

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

/** App-private AES-GCM storage for manually entered, non-clinical reminder text and due time. */
internal class EncryptedAlertReminderStore(context: Context) {
    private val appContext = context.applicationContext
    private val remindersDirectory = File(appContext.filesDir, REMINDERS_DIRECTORY)

    @Synchronized
    fun create(title: String, dueAtMillis: Long): AlertReminder {
        val normalizedTitle = title.trim()
        val nowMillis = System.currentTimeMillis()
        val validationError = validateAlertReminderInput(
            title = normalizedTitle,
            dueAtMillis = dueAtMillis,
            nowMillis = nowMillis,
        )
        if (validationError != null) throw IllegalArgumentException(validationError.name)
        ensureDirectory()

        val reminder = AlertReminder(
            id = UUID.randomUUID().toString(),
            createdAtMillis = nowMillis,
            dueAtMillis = dueAtMillis,
            title = normalizedTitle,
        )
        val destination = reminderFile(reminder.id)
        if (destination.exists()) throw IOException("No se pudo crear un identificador de recordatorio único.")
        val temporary = File(remindersDirectory, "${reminder.id}$TEMP_SUFFIX")

        try {
            val encrypted = encrypt(serialize(reminder), reminder.id)
            FileOutputStream(temporary).use { output ->
                output.write(encrypted)
                output.fd.sync()
            }
            if (!temporary.renameTo(destination)) {
                throw IOException("No se pudo guardar el recordatorio cifrado local.")
            }
            return reminder
        } catch (error: Exception) {
            temporary.delete()
            throw if (error is IOException) error else IOException(
                "No se pudo guardar el recordatorio cifrado local.",
                error,
            )
        }
    }

    @Synchronized
    fun listReminders(): List<AlertReminder> {
        if (!remindersDirectory.exists()) return emptyList()
        if (!remindersDirectory.isDirectory) {
            throw IOException("El almacenamiento local de recordatorios no está disponible.")
        }
        val files = remindersDirectory.listFiles { file -> file.name.endsWith(RECORD_SUFFIX) }
            ?: throw IOException("No se pudo enumerar el almacenamiento local cifrado.")
        return files.map { file ->
            val id = file.name.removeSuffix(RECORD_SUFFIX)
            if (!isValidId(id)) throw IOException("Se encontró un recordatorio local no válido.")
            decryptReminder(file, id)
        }.sortedBy(AlertReminder::dueAtMillis)
    }

    @Synchronized
    fun readReminder(id: String): AlertReminder? {
        if (!isValidId(id)) return null
        val file = reminderFile(id)
        if (!file.isFile) return null
        return decryptReminder(file, id)
    }

    @Synchronized
    fun deleteReminder(id: String): Boolean {
        if (!isValidId(id)) return false
        val file = reminderFile(id)
        return !file.exists() || file.delete()
    }

    private fun decryptReminder(file: File, expectedId: String): AlertReminder {
        if (!file.isFile || file.length() !in MIN_ENCRYPTED_BYTES..MAX_ENCRYPTED_BYTES) {
            throw IOException("Un recordatorio local está incompleto o fuera de tamaño.")
        }
        try {
            FileInputStream(file).use { input ->
                val magic = ByteArray(MAGIC.size)
                readFully(input, magic)
                if (!magic.contentEquals(MAGIC)) throw IOException("Formato de recordatorio no válido.")

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
                if (ciphertext.size < GCM_TAG_BYTES || ciphertext.size > MAX_ENCRYPTED_BYTES) {
                    throw IOException("Recordatorio cifrado incompleto.")
                }
                val cleartext = createCipher(Cipher.DECRYPT_MODE, expectedId, iv).doFinal(ciphertext)
                return deserialize(cleartext, expectedId)
            }
        } catch (error: Exception) {
            throw if (error is IOException) error else IOException(
                "No se pudo autenticar o descifrar un recordatorio local.",
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
        if (ciphertext.size > MAX_ENCRYPTED_BYTES) throw IOException("El recordatorio es demasiado grande.")
        return ByteArrayOutputStream().also { buffer ->
            buffer.write(MAGIC)
            buffer.write(FORMAT_VERSION)
            buffer.write(cipher.iv.size)
            buffer.write(cipher.iv)
            buffer.write(ciphertext)
        }.toByteArray()
    }

    private fun serialize(reminder: AlertReminder): ByteArray {
        return ByteArrayOutputStream().also { buffer ->
            DataOutputStream(buffer).use { output ->
                output.writeInt(SCHEMA_VERSION)
                output.writeUTF(reminder.id)
                output.writeLong(reminder.createdAtMillis)
                output.writeLong(reminder.dueAtMillis)
                output.writeUTF(reminder.title)
            }
        }.toByteArray().also { bytes ->
            if (bytes.size > MAX_ENCRYPTED_BYTES) throw IOException("El recordatorio es demasiado grande.")
        }
    }

    private fun deserialize(cleartext: ByteArray, expectedId: String): AlertReminder {
        DataInputStream(ByteArrayInputStream(cleartext)).use { input ->
            if (input.readInt() != SCHEMA_VERSION) {
                throw IOException("Versión de recordatorio no compatible.")
            }
            val id = input.readUTF()
            val createdAtMillis = input.readLong()
            val dueAtMillis = input.readLong()
            val title = input.readUTF()
            if (id != expectedId || !isValidId(id) || createdAtMillis < 0L || dueAtMillis <= createdAtMillis) {
                throw IOException("Metadatos de recordatorio no válidos.")
            }
            if (title.isBlank() || title.length > MAX_ALERT_REMINDER_TITLE_CHARACTERS || input.available() != 0) {
                throw IOException("Contenido de recordatorio no válido.")
            }
            return AlertReminder(
                id = id,
                createdAtMillis = createdAtMillis,
                dueAtMillis = dueAtMillis,
                title = title,
            )
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

    private fun readFully(input: FileInputStream, destination: ByteArray) {
        var offset = 0
        while (offset < destination.size) {
            val count = input.read(destination, offset, destination.size - offset)
            if (count < 0) throw IOException("Archivo de recordatorio cifrado incompleto.")
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
        if (!remindersDirectory.exists() && !remindersDirectory.mkdirs()) {
            throw IOException("No se pudo abrir el almacenamiento local de recordatorios.")
        }
        if (!remindersDirectory.isDirectory) {
            throw IOException("El almacenamiento local de recordatorios no está disponible.")
        }
    }

    private fun reminderFile(id: String) = File(remindersDirectory, "$id$RECORD_SUFFIX")

    private fun isValidId(id: String): Boolean =
        runCatching { UUID.fromString(id).toString() == id }.getOrDefault(false)

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "uci_assistant_alert_reminders_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_SIZE_BITS = 256
        const val GCM_TAG_BITS = 128
        const val GCM_TAG_BYTES = GCM_TAG_BITS / 8
        const val MIN_IV_LENGTH = 12
        const val MAX_IV_LENGTH = 16
        const val FORMAT_VERSION = 1
        const val SCHEMA_VERSION = 1
        const val MAX_ENCRYPTED_BYTES = 4 * 1024
        const val MAGIC_BYTES = 4
        const val MIN_ENCRYPTED_BYTES = MAGIC_BYTES + 2 + MIN_IV_LENGTH + GCM_TAG_BYTES
        const val REMINDERS_DIRECTORY = "encrypted-alert-reminders-v1"
        const val RECORD_SUFFIX = ".enc"
        const val TEMP_SUFFIX = ".tmp"
        val MAGIC = byteArrayOf(0x55, 0x43, 0x49, 0x52) // UCIR
        val KEY_LOCK = Any()
    }
}
