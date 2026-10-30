package org.uciasistant.feature.protocols

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.uciasistant.core.designsystem.PrototypeNotice

@Composable
fun ProtocolLibraryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showRequirements by remember { mutableStateOf(false) }
    var checkingFile by remember { mutableStateOf(false) }
    var selectedPdfName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPdfSize by rememberSaveable { mutableStateOf<Long?>(null) }
    var fileError by rememberSaveable { mutableStateOf<String?>(null) }

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            checkingFile = true
            fileError = null
            coroutineScope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching { inspectPdf(context, uri) }
                }
                result.fold(
                    onSuccess = { file ->
                        selectedPdfName = file.name
                        selectedPdfSize = file.sizeBytes
                        fileError = null
                    },
                    onFailure = { error ->
                        selectedPdfName = null
                        selectedPdfSize = null
                        fileError = error.message ?: "No se pudo comprobar el archivo seleccionado."
                    },
                )
                checkingFile = false
            }
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextButton(onClick = onBack) {
                Text(text = "← Inicio")
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Protocolos de consulta",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Biblioteca independiente de las alertas, para consulta profesional.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            PrototypeNotice(modifier = Modifier.fillMaxWidth())

            LibrarySummaryCard()

            DocumentPickerCard(
                isChecking = checkingFile,
                selectedName = selectedPdfName,
                selectedSize = selectedPdfSize?.let {
                    Formatter.formatShortFileSize(context, it)
                },
                error = fileError,
                onSelectPdf = { pdfPicker.launch(arrayOf("application/pdf")) },
            )

            InfoCard(
                title = "Aún no hay protocolos disponibles",
                description = "No se han cargado documentos con fuente, derechos de uso y aprobación verificados. Seleccionar un PDF no lo aprueba ni lo publica en la biblioteca.",
            )

            TextButton(onClick = { showRequirements = !showRequirements }) {
                Text(
                    text = if (showRequirements) {
                        "Ocultar requisitos de publicación"
                    } else {
                        "Ver requisitos de publicación"
                    },
                )
            }

            if (showRequirements) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RequirementCard(
                        number = "01",
                        title = "Definir alcance y población",
                        description = "Precisar los dominios pediátricos y los límites de inclusión antes de organizar el contenido.",
                    )
                    RequirementCard(
                        number = "02",
                        title = "Confirmar fuente y derechos",
                        description = "Documentar la procedencia del material y contar con autorización para su uso y adaptación.",
                    )
                    RequirementCard(
                        number = "03",
                        title = "Completar revisiones",
                        description = "Someter cada documento a revisión clínica e institucional y a la evaluación regulatoria que corresponda.",
                    )
                    RequirementCard(
                        number = "04",
                        title = "Controlar versión y vigencia",
                        description = "Definir responsables de mantenimiento, fechas de revisión y retirada de versiones sustituidas.",
                    )
                }
            }

            InfoCard(
                title = "Relación con Alertas",
                description = "Una futura alerta podrá abrir el protocolo vigente aprobado que corresponda para consulta. La correspondencia todavía no está definida; el profesional conserva la decisión.",
            )

            Text(
                text = "Sin contenido clínico · Sin aplicación a pacientes",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LibrarySummaryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Text(
                    text = "BIBLIOTECA NO PUBLICADA",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = "0 protocolos",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "No hay documentos aprobados cargados en esta versión.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DocumentPickerCard(
    isChecking: Boolean,
    selectedName: String?,
    selectedSize: String?,
    error: String?,
    onSelectPdf: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Seleccionar documento para revisión",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Elige un PDF desde el selector de Android. Si viene de la PC, primero cópialo al teléfono o a un almacenamiento accesible desde el dispositivo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onSelectPdf, enabled = !isChecking) {
                Text(text = if (isChecking) "Comprobando PDF…" else "Elegir PDF")
            }
            if (selectedName != null) {
                Text(
                    text = "PDF seleccionado · Pendiente de revisión",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = selectedName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = selectedSize ?: "Tamaño no disponible",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Solo se verificaron el formato y los metadatos. El contenido no se guarda ni se publica en esta versión.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (error != null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun RequirementCard(number: String, title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "PASO $number",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InfoCard(title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private data class PdfFileMetadata(
    val name: String,
    val sizeBytes: Long?,
)

private fun inspectPdf(context: Context, uri: Uri): PdfFileMetadata {
    val header = context.contentResolver.openInputStream(uri)?.use { input ->
        val bytes = ByteArray(5)
        var count = 0
        while (count < bytes.size) {
            val read = input.read(bytes, count, bytes.size - count)
            if (read <= 0) break
            count += read
        }
        String(bytes, 0, count, Charsets.US_ASCII)
    } ?: throw IllegalArgumentException("No se pudo leer el archivo seleccionado.")

    if (header != "%PDF-") {
        throw IllegalArgumentException("El archivo no parece ser un PDF válido.")
    }

    var name = "documento.pdf"
    var sizeBytes: Long? = null
    context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
        null,
        null,
        null,
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameColumn >= 0) {
                name = cursor.getString(nameColumn)?.takeIf { it.isNotBlank() } ?: name
            }
            val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) {
                sizeBytes = cursor.getLong(sizeColumn)
            }
        }
    }
    return PdfFileMetadata(name = name, sizeBytes = sizeBytes)
}
