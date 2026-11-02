package org.uciasistant.feature.protocols

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.uciasistant.core.clinicaldata.ClinicalPopulation
import org.uciasistant.core.designsystem.PrototypeNotice

@Composable
fun ProtocolLibraryScreen(onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val coroutineScope = rememberCoroutineScope()
    val documentStore = remember(context) { PendingProtocolDocumentStore(context) }
    var documents by remember { mutableStateOf(emptyList<PendingProtocolDocument>()) }
    var isLoadingDocuments by remember { mutableStateOf(true) }
    var isImporting by remember { mutableStateOf(false) }
    var showRequirements by remember { mutableStateOf(false) }
    var showReferenceCatalog by remember { mutableStateOf(false) }
    var showProcedureTopicCatalog by remember { mutableStateOf(false) }
    var fileError by remember { mutableStateOf<String?>(null) }
    var activeDocument by remember { mutableStateOf<PendingProtocolDocument?>(null) }

    LaunchedEffect(documentStore) {
        val result = withContext(Dispatchers.IO) {
            runCatching {
                documentStore.clearPreviewCache()
                documentStore.listDocuments()
            }
        }
        result.fold(
            onSuccess = { documents = it },
            onFailure = { fileError = "No se pudo abrir la lista local de documentos." },
        )
        isLoadingDocuments = false
    }

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            isImporting = true
            fileError = null
            coroutineScope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        documentStore.importPdf(uri)
                        documentStore.listDocuments()
                    }
                }
                result.fold(
                    onSuccess = { documents = it },
                    onFailure = { error ->
                        fileError = error.message ?: "No se pudo importar el PDF seleccionado."
                    },
                )
                isImporting = false
            }
        }
    }

    val documentToReview = activeDocument
    if (documentToReview != null) {
        PdfReviewScreen(
            document = documentToReview,
            documentStore = documentStore,
            onBack = { activeDocument = null },
        )
    } else {
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

                LibrarySummaryCard(
                    documentCount = documents.size,
                    referenceCount = PediatricClinicalReferenceCatalog.references.size,
                    procedureTopicCount = PediatricProcedureTopicCatalog.topics.size,
                )

                ReferenceCatalogCard(
                    expanded = showReferenceCatalog,
                    onToggle = { showReferenceCatalog = !showReferenceCatalog },
                )

                ProcedureTopicCatalogCard(
                    expanded = showProcedureTopicCatalog,
                    onToggle = { showProcedureTopicCatalog = !showProcedureTopicCatalog },
                )

                DocumentPickerCard(
                    documents = documents,
                    isBusy = isLoadingDocuments || isImporting,
                    error = fileError,
                    onSelectPdf = { pdfPicker.launch(arrayOf("application/pdf")) },
                    onOpenDocument = { activeDocument = it },
                    onDeleteDocument = { document ->
                        coroutineScope.launch {
                            val result = withContext(Dispatchers.IO) {
                                runCatching {
                                    if (!documentStore.deleteDocument(document.id)) {
                                        throw IOException("No se pudo eliminar el documento local.")
                                    }
                                    documentStore.listDocuments()
                                }
                            }
                            result.fold(
                                onSuccess = { documents = it; fileError = null },
                                onFailure = { error ->
                                    fileError = error.message ?: "No se pudo eliminar el documento."
                                },
                            )
                        }
                    },
                )

                InfoCard(
                    title = "Documentos pendientes, no protocolos aprobados",
                    description = "Los PDF importados se guardan cifrados en el espacio privado de esta app para revisión local. No se publican en la biblioteca ni activan alertas. No importes informes ni archivos con datos identificables de pacientes.",
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
                    text = "Sin contenido clínico aprobado · Sin aplicación a pacientes",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LibrarySummaryCard(
    documentCount: Int,
    referenceCount: Int,
    procedureTopicCount: Int,
) {
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
                    text = "CATÁLOGO PEDIÁTRICO EN DESARROLLO",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = "$referenceCount fuentes · $procedureTopicCount temas de procedimiento",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (documentCount == 0) {
                    "No hay documentos pendientes de revisión. Aún no se publican protocolos clínicos ni instrucciones de procedimientos."
                } else {
                    "$documentCount ${if (documentCount == 1) "documento" else "documentos"} pendiente(s) de revisión; no están publicados ni sustituyen instrucciones clínicas aprobadas."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReferenceCatalogCard(
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val references = PediatricClinicalReferenceCatalog.references
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
                text = "Fuentes pediátricas candidatas · ${references.size}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Índice bibliográfico offline. No contiene protocolos, recomendaciones ni algoritmos; derechos, vigencia, aplicabilidad local y aprobación editorial siguen pendientes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onToggle) {
                Text(if (expanded) "Ocultar referencias" else "Mostrar referencias")
            }
            if (expanded) {
                references.forEach { reference ->
                    SourceReferenceCard(reference)
                }
            }
        }
    }
}

@Composable
private fun ProcedureTopicCatalogCard(
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val topics = PediatricProcedureTopicCatalog.topics
    val references = PediatricClinicalReferenceCatalog.references.associateBy(ClinicalReference::id)
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
                text = "Temas de procedimientos pediátricos · ${topics.size}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Temas identificados en el índice editorial de una fuente candidata; las referencias adicionales se muestran por separado. No incluye pasos, criterios ni instrucciones clínicas; no son procedimientos aprobados.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onToggle) {
                Text(if (expanded) "Ocultar temas" else "Mostrar temas candidatos")
            }
            if (expanded) {
                topics.forEach { topic ->
                    val sources = topic.sourceReferenceIds.mapNotNull(references::get)
                    ProcedureTopicCard(
                        topic = topic,
                        sources = sources,
                        hasMissingSources = sources.size != topic.sourceReferenceIds.size,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProcedureTopicCard(
    topic: PediatricProcedureTopic,
    sources: List<ClinicalReference>,
    hasMissingSources: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = "TEMA DE ÍNDICE · NO ES UNA GUÍA DE PROCEDIMIENTO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = topic.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (hasMissingSources || sources.isEmpty()) {
                Text(
                    text = "Una o más referencias bibliográficas no están disponibles.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            sources.forEachIndexed { index, source ->
                Text(
                    text = "FUENTE CANDIDATA ${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = source.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(text = source.citation, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = "${source.identifierLabel}: ${source.identifier}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ReferenceReviewMetadata(source)
                source.licenseNote?.let { license ->
                    Text(
                        text = "Licencia: $license",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceReferenceCard(reference: ClinicalReference) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = "REFERENCIA · NO PROTOCOLO APROBADO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = reference.domain,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = reference.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = reference.citation, style = MaterialTheme.typography.bodySmall)
            Text(
                text = "${reference.identifierLabel}: ${reference.identifier}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ReferenceReviewMetadata(reference)
            reference.licenseNote?.let { license ->
                Text(
                    text = "Licencia: $license",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReferenceReviewMetadata(reference: ClinicalReference) {
    Text(
        text = "Población etiquetada: ${reference.population.displayLabel()}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = "Estado de revisión de derechos: ${reference.rightsReviewStatus.displayLabel()}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun ClinicalPopulation.displayLabel(): String = when (this) {
    ClinicalPopulation.ADULT -> "adulta"
    ClinicalPopulation.PEDIATRIC -> "pediátrica"
    ClinicalPopulation.NEONATAL -> "neonatal"
}

private fun ReferenceRightsReviewStatus.displayLabel(): String = when (this) {
    ReferenceRightsReviewStatus.NOT_ASSESSED -> "no evaluados"
    ReferenceRightsReviewStatus.LICENSE_IDENTIFIED_COMPATIBILITY_PENDING ->
        "licencia identificada; compatibilidad y reutilización pendientes"
}

@Composable
private fun DocumentPickerCard(
    documents: List<PendingProtocolDocument>,
    isBusy: Boolean,
    error: String?,
    onSelectPdf: () -> Unit,
    onOpenDocument: (PendingProtocolDocument) -> Unit,
    onDeleteDocument: (PendingProtocolDocument) -> Unit,
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
                text = "Importar PDF para revisión",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Si el archivo está en la PC, cópialo primero al teléfono o a un almacenamiento accesible desde Android. Selecciona solo documentos de protocolo; no informes de pacientes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onSelectPdf, enabled = !isBusy) {
                Text(text = if (isBusy) "Importando o cargando…" else "Elegir PDF")
            }

            if (documents.isEmpty() && !isBusy) {
                Text(
                    text = "No hay PDF importados.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            documents.forEach { document ->
                PendingDocumentCard(
                    document = document,
                    onOpen = { onOpenDocument(document) },
                    onDelete = { onDeleteDocument(document) },
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
private fun PendingDocumentCard(
    document: PendingProtocolDocument,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "PENDIENTE DE REVISIÓN · NO APROBADO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = document.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = Formatter.formatShortFileSize(LocalContext.current, document.sizeBytes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onOpen) {
                Text(text = "Abrir para revisar")
            }
            TextButton(onClick = onDelete) {
                Text(text = "Eliminar del dispositivo")
            }
        }
    }
}

@Composable
private fun PdfReviewScreen(
    document: PendingProtocolDocument,
    documentStore: PendingProtocolDocumentStore,
    onBack: () -> Unit,
) {
    var previewFile by remember(document.id) { mutableStateOf<File?>(null) }
    var previewError by remember(document.id) { mutableStateOf<String?>(null) }
    var isPreparing by remember(document.id) { mutableStateOf(true) }

    BackHandler(onBack = onBack)

    LaunchedEffect(document.id) {
        val result = withContext(Dispatchers.IO) {
            runCatching { documentStore.createPreviewFile(document.id) }
        }
        result.fold(
            onSuccess = { previewFile = it },
            onFailure = { previewError = "No se pudo descifrar y abrir el PDF local." },
        )
        isPreparing = false
    }

    DisposableEffect(previewFile) {
        onDispose { previewFile?.delete() }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onBack) {
                Text(text = "← Protocolos")
            }
            Text(
                text = document.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = "Documento local pendiente de revisión. No está aprobado ni publicado para uso asistencial.",
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }

            when {
                isPreparing -> Text("Preparando vista local…")
                previewError != null -> Text(
                    text = previewError.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                )
                previewFile != null -> PdfPageList(
                    file = previewFile!!,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PdfPageList(file: File, modifier: Modifier = Modifier) {
    var pageCount by remember(file) { mutableStateOf<Int?>(null) }
    var pageCountError by remember(file) { mutableStateOf(false) }

    LaunchedEffect(file) {
        val result = withContext(Dispatchers.IO) {
            runCatching { readPdfPageCount(file) }
        }
        result.fold(
            onSuccess = { pageCount = it },
            onFailure = { pageCountError = true },
        )
    }

    when {
        pageCountError -> Text("No se pudieron leer las páginas del PDF.")
        pageCount == null -> Text("Cargando documento…")
        pageCount == 0 -> Text("El PDF no contiene páginas visibles.")
        else -> LazyColumn(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(count = pageCount ?: 0) { pageIndex ->
                val renderResult by produceState<Result<Bitmap>?>(
                    initialValue = null,
                    key1 = file,
                    key2 = pageIndex,
                ) {
                    value = withContext(Dispatchers.IO) {
                        runCatching { renderPdfPage(file, pageIndex) }
                    }
                }
                when {
                    renderResult == null -> Text("Cargando página ${pageIndex + 1}…")
                    renderResult?.isSuccess == true -> Image(
                        bitmap = renderResult!!.getOrThrow().asImageBitmap(),
                        contentDescription = "Página ${pageIndex + 1} del PDF",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                    )
                    else -> Text(
                        text = "No se pudo mostrar la página ${pageIndex + 1}.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
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

private fun readPdfPageCount(file: File): Int =
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
        PdfRenderer(descriptor).use { renderer -> renderer.pageCount }
    }

private fun renderPdfPage(file: File, pageIndex: Int): Bitmap =
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
        PdfRenderer(descriptor).use { renderer ->
            renderer.openPage(pageIndex).use { page ->
                val maxPageEdge = maxOf(page.width, page.height).coerceAtLeast(1)
                val scale = minOf(PDF_RENDER_SCALE, MAX_RENDER_EDGE / maxPageEdge)
                val bitmap = Bitmap.createBitmap(
                    (page.width * scale).toInt().coerceAtLeast(1),
                    (page.height * scale).toInt().coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888,
                )
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            }
        }
    }

private const val PDF_RENDER_SCALE = 2f
private const val MAX_RENDER_EDGE = 1600f
