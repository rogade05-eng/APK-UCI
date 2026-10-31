package org.uciasistant.feature.protocols

import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PendingProtocolDocumentStoreTest {
    @Test
    fun encryptedImportCanBeListedPreviewedAndDeleted() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = PendingProtocolDocumentStore(context)
        val source = File.createTempFile("protocol-test-", ".pdf", context.cacheDir)
        source.writeBytes(createSinglePagePdf())

        var imported: PendingProtocolDocument? = null
        var preview: File? = null
        try {
            val importedDocument = store.importPdf(Uri.fromFile(source))
            imported = importedDocument

            assertEquals(source.length(), importedDocument.sizeBytes)
            assertTrue(store.listDocuments().any { it.id == importedDocument.id })

            val encryptedPdf = File(
                context.filesDir,
                "pending-protocols/${importedDocument.id}.pdf.enc",
            )
            assertTrue(encryptedPdf.isFile)
            val encryptedText = String(encryptedPdf.readBytes(), StandardCharsets.ISO_8859_1)
            assertFalse(encryptedText.contains("%PDF-1.4"))

            val previewFile = store.createPreviewFile(importedDocument.id)
            preview = previewFile
            assertArrayEquals(source.readBytes(), previewFile.readBytes())
            val pageCount = ParcelFileDescriptor.open(
                previewFile,
                ParcelFileDescriptor.MODE_READ_ONLY,
            ).use { descriptor ->
                PdfRenderer(descriptor).use { renderer -> renderer.pageCount }
            }
            assertEquals(1, pageCount)

            assertTrue(store.deleteDocument(importedDocument.id))
            assertFalse(store.listDocuments().any { it.id == importedDocument.id })
        } finally {
            preview?.delete()
            imported?.let { store.deleteDocument(it.id) }
            source.delete()
            store.clearPreviewCache()
        }
    }

    private fun createSinglePagePdf(): ByteArray {
        val ascii = StandardCharsets.US_ASCII
        val output = StringBuilder("%PDF-1.4\n")
        val objectOffsets = mutableListOf<Int>()

        fun appendObject(body: String) {
            objectOffsets += output.toString().toByteArray(ascii).size
            val objectNumber = objectOffsets.size
            output.append(objectNumber).append(" 0 obj\n")
            output.append(body).append("\nendobj\n")
        }

        appendObject("<< /Type /Catalog /Pages 2 0 R >>")
        appendObject("<< /Type /Pages /Kids [3 0 R] /Count 1 >>")
        appendObject(
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 300 400] " +
                "/Resources << >> /Contents 4 0 R >>",
        )
        val pageContent = "0 0 m 100 100 l S"
        val pageContentLength = pageContent.toByteArray(ascii).size
        appendObject(
            "<< /Length $pageContentLength >>\nstream\n$pageContent\nendstream",
        )

        val crossReferenceOffset = output.toString().toByteArray(ascii).size
        output.append("xref\n0 ").append(objectOffsets.size + 1).append('\n')
        output.append("0000000000 65535 f \n")
        objectOffsets.forEach { offset ->
            output.append(offset.toString().padStart(10, '0')).append(" 00000 n \n")
        }
        output.append("trailer\n<< /Size ").append(objectOffsets.size + 1)
            .append(" /Root 1 0 R >>\nstartxref\n")
            .append(crossReferenceOffset)
            .append("\n%%EOF\n")
        return output.toString().toByteArray(ascii)
    }
}
