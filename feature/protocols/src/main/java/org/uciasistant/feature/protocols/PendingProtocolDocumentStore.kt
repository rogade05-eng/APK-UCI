package org.uciasistant.feature.protocols

import android.content.Context
import android.net.Uri
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
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal data class PendingProtocolDocument(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val importedAtMillis: Long,
)

/** Stores only locally selected protocol PDFs, encrypted with a non-exportable Android Keystore key. */
internal class PendingProtocolDocumentStore(context: Context) {
    private val appContext = context.applicationContext
    private val documentsDirectory = File(appContext.filesDir, DOCUMENTS_DIRECTORY)
    private val previewDirectory = File(appContext.cacheDir, PREVIEW_DIRECTORY)

    fun listDocuments(): List<PendingProtocolDocument> {
        ensureDirectory(documentsDirectory)
        documentsDirectory.listFiles { file -> file.name.endsWith(METADATA_SUFFIX) }
            .orEmpty()
            .mapNotNull { metadataFile ->
                runCatching {
                    val document = decryptMetadata(metadataFile)
                    if (pdfFile(document.id).isFile) document else null
                }.getOrNull()
            }
            .sortedByDescending(PendingProtocolDocument::importedAtMillis)
    }

    fun importPdf(uri: Uri): PendingProtocolDocument {
        ensureDirectory(documentsDirectory)
        val id = UUID.randomUUID().toString()
        val encryptedPdf = pdfFile(id)
        val temporaryPdf = File(documentsDirectory, "$id$PDF_SUFFIX.tmp")
        val metadata = metadataFile(id)
        val temporaryMetadata = File(documentsDirectory, "$id$METADATA_SUFFIX.tmp")
        var pdfPublished = false

        try {
            val name = queryDisplayName(uri)
            val sizeBytes = encryptPdf(uri, temporaryPdf)
            if (!temporaryPdf.renameTo(encryptedPdf)) {
                throw IOException("No se pudo guardar el PDF en el almacenamiento privado.")
            }
            pdfPublished = true

            val document = PendingProtocolDocument(
                id = id,
                name = name,
                sizeBytes = sizeBytes,
                importedAtMillis = System.currentTimeMillis(),
            )
            FileOutputStream(temporaryMetadata).use { output ->
                output.write(encryptMetadata(document))
            }
            if (!temporaryMetadata.renameTo(metadata)) {
                throw IOException("No se pudo registrar el documento importado.")
            }
            return document
        } catch (error: Exception) {
            temporaryPdf.delete()
            temporaryMetadata.delete()
            if (pdfPublished) encryptedPdf.delete()
            throw error
        }
    }

    fun deleteDocument(id: String): Boolean {
        if (!isValidId(id)) return false
        val pdfDeleted = deleteIfExists(pdfFile(id))
        val metadataDeleted = deleteIfExists(metadataFile(id))
        return pdfDeleted && metadataDeleted
    }

    fun createPreviewFile(id: String): File {
        if (!isValidId(id)) throw IOException("Identificador de documento no válido.")
        val encryptedPdf = pdfFile(id)
        if (!encryptedPdf.isFile) throw IOException("No se encontró el PDF importado.")
        ensureDirectory(previewDirectory)
        val destination = File(previewDirectory, "$id.pdf")
        val temporary = File(previewDirectory, "$id.pdf.tmp")
        destination.delete()
        temporary.delete()

        try {
            decryptPdf(encryptedPdf, temporary)
            if (!temporary.renameTo(destination)) {
                throw IOException("No se pudo preparar el PDF para la vista previa.")
            }
            return destination
        } catch (error: Exception) {
            temporary.delete()
            destination.delete()
            throw error
        }
    }

    fun clearPreviewCache() {
        previewDirectory.listFiles()?.forEach(File::delete)
    }

    private fun encryptPdf(uri: Uri, destination: File): Long {
        val input = appContext.contentResolver.openInputStream(uri)
            ?: throw IOException("Android no pudo abrir el archivo seleccionado.")

        input.use { source ->
            val prefix = readAndValidatePdfHeader(source)
            val cipher = createCipher(Cipher.ENCRYPT_MODE)
            var totalBytes = prefix.size.toLong()

            FileOutputStream(destination).use { rawOutput ->
                writeEnvelopeHeader(rawOutput, cipher.iv)
                CipherOutputStream(rawOutput, cipher).use { encryptedOutput ->
                    encryptedOutput.write(prefix)
                    val buffer = ByteArray(COPY_BUFFER_SIZE)
                    while (true) {
                        val count = source.read(buffer)
                        if (count < 0) break
                        if (count == 0) continue
                        encryptedOutput.write(buffer, 0, count)
                        totalBytes += count
                    }
                }
            }
            return totalBytes
        }
    }

    private fun decryptPdf(source: File, destination: File) {
        FileInputStream(source).use { rawInput ->
            val iv = readEnvelopeHeader(rawInput)
            val cipher = createCipher(Cipher.DECRYPT_MODE, iv)
            CipherInputStream(rawInput, cipher).use { decryptedInput ->
                FileOutputStream(destination).use { output ->
                    decryptedInput.copyTo(output)
                }
            }
        }
    }

    private fun readAndValidatePdfHeader(input: java.io.InputStream): ByteArray {
        val buffer = ByteArray(PDF_HEADER_SCAN_BYTES)
        var count = 0
        while (count < buffer.size) {
            val read = input.read(buffer, count, buffer.size - count)
            if (read <= 0) break
            count += read
        }
        val prefix = buffer.copyOf(count)
        val header = String(prefix, Charsets.US_ASCII)
        if (!header.contains(PDF_HEADER)) {
            throw IOException("El archivo seleccionado no parece ser un PDF válido.")
        }
        return prefix
    }

    private fun queryDisplayName(uri: Uri): String {
        appContext.contentResolver.query(
            uri,
            arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameColumn = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameColumn >= 0) {
                    cursor.getString(nameColumn)
                        ?.takeIf { it.isNotBlank() }
                        ?.let { return it.take(MAX_NAME_LENGTH) }
                }
            }
        }
        return "documento.pdf"
    }

    private fun encryptMetadata(document: PendingProtocolDocument): ByteArray {
        val cleartext = ByteArrayOutputStream().also { buffer ->
            DataOutputStream(buffer).use { output ->
                output.writeUTF(document.id)
                output.writeUTF(document.name)
                output.writeLong(document.sizeBytes)
                output.writeLong(document.importedAtMillis)
            }
        }.toByteArray()
        val cipher = createCipher(Cipher.ENCRYPT_MODE)
        val encrypted = cipher.doFinal(cleartext)
        return ByteArrayOutputStream().also { buffer ->
            buffer.write(MAGIC)
            buffer.write(cipher.iv.size)
            buffer.write(cipher.iv)
            buffer.write(encrypted)
        }.toByteArray()
    }

    private fun decryptMetadata(file: File): PendingProtocolDocument {
        FileInputStream(file).use { input ->
            val iv = readEnvelopeHeader(input)
            val encrypted = input.readBytes()
            val cleartext = createCipher(Cipher.DECRYPT_MODE, iv).doFinal(encrypted)
            DataInputStream(ByteArrayInputStream(cleartext)).use { data ->
                val id = data.readUTF()
                val name = data.readUTF()
                val sizeBytes = data.readLong()
                val importedAtMillis = data.readLong()
                if (!isValidId(id) || sizeBytes < 0L) {
                    throw IOException("Metadatos de documento no válidos.")
                }
                return PendingProtocolDocument(id, name, sizeBytes, importedAtMillis)
            }
        }
    }

    private fun writeEnvelopeHeader(output: java.io.OutputStream, iv: ByteArray) {
        output.write(MAGIC)
        output.write(iv.size)
        output.write(iv)
    }

    private fun readEnvelopeHeader(input: java.io.InputStream): ByteArray {
        val magic = ByteArray(MAGIC.size)
        var offset = 0
        while (offset < magic.size) {
            val read = input.read(magic, offset, magic.size - offset)
            if (read < 0) throw IOException("Archivo cifrado incompleto.")
            offset += read
        }
        if (!magic.contentEquals(MAGIC)) throw IOException("Formato de almacenamiento no válido.")

        val ivLength = input.read()
        if (ivLength !in MIN_IV_LENGTH..MAX_IV_LENGTH) {
            throw IOException("Parámetros de cifrado no válidos.")
        }
        val iv = ByteArray(ivLength)
        offset = 0
        while (offset < iv.size) {
            val read = input.read(iv, offset, iv.size - offset)
            if (read < 0) throw IOException("Archivo cifrado incompleto.")
            offset += read
        }
        return iv
    }

    private fun createCipher(mode: Int, iv: ByteArray? = null): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        if (mode == Cipher.ENCRYPT_MODE) {
            cipher.init(mode, getOrCreateKey())
        } else {
            requireNotNull(iv) { "Falta el vector de inicialización." }
            cipher.init(mode, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        }
        return cipher
    }

    @Synchronized
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

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
                .build(),
        )
        return generator.generateKey()
    }

    private fun ensureDirectory(directory: File) {
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("No se pudo crear el almacenamiento privado de documentos.")
        }
    }

    private fun pdfFile(id: String) = File(documentsDirectory, "$id$PDF_SUFFIX")

    private fun metadataFile(id: String) = File(documentsDirectory, "$id$METADATA_SUFFIX")

    private fun deleteIfExists(file: File): Boolean = !file.exists() || file.delete()

    private fun isValidId(id: String): Boolean =
        runCatching { UUID.fromString(id).toString() == id }.getOrDefault(false)

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "uci_assistant_protocol_pdf_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_SIZE_BITS = 256
        const val GCM_TAG_BITS = 128
        const val MIN_IV_LENGTH = 12
        const val MAX_IV_LENGTH = 16
        const val COPY_BUFFER_SIZE = 16 * 1024
        const val PDF_HEADER_SCAN_BYTES = 1024
        const val PDF_HEADER = "%PDF-"
        const val MAX_NAME_LENGTH = 256
        const val DOCUMENTS_DIRECTORY = "pending-protocols"
        const val PREVIEW_DIRECTORY = "pending-protocol-previews"
        const val PDF_SUFFIX = ".pdf.enc"
        const val METADATA_SUFFIX = ".meta.enc"
        val MAGIC = byteArrayOf(0x55, 0x43, 0x49, 0x50) // UCIP
    }
}
