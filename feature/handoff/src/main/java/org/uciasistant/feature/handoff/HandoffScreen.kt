package org.uciasistant.feature.handoff

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.uciasistant.core.designsystem.PrototypeNotice
import org.uciasistant.core.designsystem.UciAssistantTheme

private enum class DraftAction {
    Exit,
    Clear,
}

/** Local encrypted handoff snapshots, not a complete patient record or official chart. */
@Composable
fun HandoffScreen(onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val store = remember(context) { EncryptedHandoffStore(context) }
    val coroutineScope = rememberCoroutineScope()
    var draft by remember { mutableStateOf(HandoffDraft()) }
    var records by remember { mutableStateOf(emptyList<StoredHandoffRecord>()) }
    var isLoadingRecords by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var storageError by remember { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var feedbackIsError by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<DraftAction?>(null) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var expandedRecordId by remember { mutableStateOf<String?>(null) }

    suspend fun reloadRecords() {
        isLoadingRecords = true
        val result = withContext(Dispatchers.IO) {
            runCatching { store.listRecords() }
        }
        result.fold(
            onSuccess = {
                records = it
                storageError = null
            },
            onFailure = {
                storageError = "No se pudo leer el archivo cifrado local. No se han omitido ni borrado registros."
            },
        )
        isLoadingRecords = false
    }

    LaunchedEffect(store) {
        reloadRecords()
    }

    fun requestExit() {
        if (draft.hasContent) {
            pendingAction = DraftAction.Exit
        } else {
            onBack()
        }
    }

    fun saveDraft() {
        if (!draft.hasContent || isSaving || isLoadingRecords || storageError != null) return
        val snapshot = draft
        isSaving = true
        feedback = null
        feedbackIsError = false
        coroutineScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { store.saveNew(snapshot) }
            }
            result.fold(
                onSuccess = {
                    draft = HandoffDraft()
                    feedback = "Borrador cifrado guardado en este dispositivo."
                    reloadRecords()
                },
                onFailure = { error ->
                    feedback = if (error is IllegalArgumentException) {
                        "No se guardó: reduce la longitud de los campos y vuelve a intentarlo."
                    } else {
                        "No se pudo guardar el borrador cifrado. El texto sigue en pantalla; no cierres si necesitas conservarlo."
                    }
                    feedbackIsError = true
                },
            )
            isSaving = false
        }
    }

    fun deleteRecord(id: String) {
        coroutineScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    if (!store.deleteRecord(id)) {
                        error("No se pudo eliminar el borrador local.")
                    }
                }
            }
            result.fold(
                onSuccess = {
                    pendingDeleteId = null
                    if (expandedRecordId == id) expandedRecordId = null
                    feedback = "Borrador eliminado del almacenamiento local."
                    reloadRecords()
                },
                onFailure = {
                    pendingDeleteId = null
                    storageError = "No se pudo eliminar el archivo local."
                },
            )
        }
    }

    BackHandler(enabled = true) {
        requestExit()
    }

    val actionToConfirm = pendingAction
    if (actionToConfirm != null) {
        val isExit = actionToConfirm == DraftAction.Exit
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { Text(if (isExit) "Descartar texto sin guardar" else "Borrar campos") },
            text = {
                Text(
                    if (isExit) {
                        "El texto que aún no se ha guardado se perderá. Los borradores cifrados existentes no se modificarán."
                    } else {
                        "Se borrará el texto temporal de esta pantalla. Los borradores guardados no se modificarán."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingAction = null
                        if (isExit) {
                            onBack()
                        } else {
                            draft = HandoffDraft()
                            feedback = null
                        }
                    },
                ) {
                    Text(if (isExit) "Salir y descartar" else "Borrar campos")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) {
                    Text("Seguir editando")
                }
            },
        )
    }

    val recordToDelete = pendingDeleteId
    if (recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Eliminar borrador local") },
            text = {
                Text(
                    "Se eliminará el archivo cifrado de la aplicación. No se podrá recuperar desde esta pantalla; la eliminación no garantiza borrado físico irrecuperable en memoria flash.",
                )
            },
            confirmButton = {
                TextButton(onClick = { deleteRecord(recordToDelete) }) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) {
                    Text("Cancelar")
                }
            },
        )
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
            TextButton(onClick = ::requestExit) {
                Text("← Inicio")
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Continuidad y entrega",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Borradores pediátricos de traspaso, cifrados y guardados solo en este dispositivo.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            PrototypeNotice(modifier = Modifier.fillMaxWidth())

            NoticeCard(
                title = "Almacenamiento local cifrado · no es la historia clínica",
                body = "Los borradores guardados se cifran con AES-GCM y una clave de Android Keystore. No se exportan ni sincronizan. Si se borran los datos de la app o se pierde la clave, no hay recuperación. No introduzcas datos reales: aún faltan autenticación, roles, auditoría de cambios, una política de retención y autorización institucional. La clave no requiere autenticación del usuario.",
            )

            NoticeCard(
                title = "Sin escalas ni prioridades predefinidas",
                body = "El motivo, la prioridad y la referencia a criterios locales son campos manuales. La app no asigna prioridad, no evalúa admisión o destino y no genera una decisión clínica.",
            )

            FormSection(
                title = "Referencia y destino",
                description = "Usa únicamente códigos y contenido ficticios en este prototipo.",
            ) {
                TextEntry(
                    label = "Código de episodio local (no nombre del paciente)",
                    value = draft.episodeReference,
                    onValueChange = { draft = draft.copy(episodeReference = it) },
                    singleLine = true,
                )
                TextEntry(
                    label = "Institución / servicio de origen",
                    value = draft.originInstitutionService,
                    onValueChange = { draft = draft.copy(originInstitutionService = it) },
                    singleLine = true,
                )
                TextEntry(
                    label = "Unidad o servicio de destino",
                    value = draft.destinationService,
                    onValueChange = { draft = draft.copy(destinationService = it) },
                    singleLine = true,
                )
                TextEntry(
                    label = "Tipo de ingreso o traslado (texto libre)",
                    value = draft.transferType,
                    onValueChange = { draft = draft.copy(transferType = it) },
                    singleLine = true,
                )
                TextEntry(
                    label = "Motivo del traslado (texto libre; no se interpreta)",
                    value = draft.transferReason,
                    onValueChange = { draft = draft.copy(transferReason = it) },
                    minLines = 2,
                )
                TextEntry(
                    label = "Prioridad según criterio institucional (si aplica)",
                    value = draft.localPriority,
                    onValueChange = { draft = draft.copy(localPriority = it) },
                    singleLine = true,
                )
                TextEntry(
                    label = "Identificador / versión del criterio local (si aplica)",
                    value = draft.localCriteriaReference,
                    onValueChange = { draft = draft.copy(localCriteriaReference = it) },
                    singleLine = true,
                )
            }

            FormSection(
                title = "Marcas de tiempo",
                description = "Entrada manual en formato libre; la plantilla no valida fechas, horas ni secuencias.",
            ) {
                TextEntry(
                    label = "Solicitud o derivación",
                    value = draft.referralDateTime,
                    onValueChange = { draft = draft.copy(referralDateTime = it) },
                    singleLine = true,
                    supportingText = "Ejemplo de formato: AAAA-MM-DD HH:MM",
                )
                TextEntry(
                    label = "Salida del servicio remitente",
                    value = draft.departureDateTime,
                    onValueChange = { draft = draft.copy(departureDateTime = it) },
                    singleLine = true,
                    supportingText = "Ejemplo de formato: AAAA-MM-DD HH:MM",
                )
                TextEntry(
                    label = "Recepción en destino",
                    value = draft.arrivalDateTime,
                    onValueChange = { draft = draft.copy(arrivalDateTime = it) },
                    singleLine = true,
                    supportingText = "Ejemplo de formato: AAAA-MM-DD HH:MM",
                )
                TextEntry(
                    label = "Entrega clínica",
                    value = draft.handoverDateTime,
                    onValueChange = { draft = draft.copy(handoverDateTime = it) },
                    singleLine = true,
                    supportingText = "Ejemplo de formato: AAAA-MM-DD HH:MM",
                )
            }

            FormSection(
                title = "Responsables",
                description = "Campos de texto; no hay autenticación, firma ni validación de identidad.",
            ) {
                TextEntry(
                    label = "Profesional que entrega",
                    value = draft.sendingProfessional,
                    onValueChange = { draft = draft.copy(sendingProfessional = it) },
                    singleLine = true,
                )
                TextEntry(
                    label = "Profesional que recibe",
                    value = draft.receivingProfessional,
                    onValueChange = { draft = draft.copy(receivingProfessional = it) },
                    singleLine = true,
                )
            }

            FormSection(
                title = "Resumen estructurado de entrega",
                description = "Texto libre, sin inferencias ni recomendaciones.",
            ) {
                TextEntry(
                    label = "Problemas activos",
                    value = draft.activeProblems,
                    onValueChange = { draft = draft.copy(activeProblems = it) },
                    minLines = 3,
                )
                TextEntry(
                    label = "Alergias",
                    value = draft.allergies,
                    onValueChange = { draft = draft.copy(allergies = it) },
                    minLines = 2,
                )
                TextEntry(
                    label = "Intervenciones en curso",
                    value = draft.interventionsInProgress,
                    onValueChange = { draft = draft.copy(interventionsInProgress = it) },
                    minLines = 3,
                )
                TextEntry(
                    label = "Dispositivos",
                    value = draft.devices,
                    onValueChange = { draft = draft.copy(devices = it) },
                    minLines = 2,
                )
                TextEntry(
                    label = "Resultados pendientes",
                    value = draft.pendingResults,
                    onValueChange = { draft = draft.copy(pendingResults = it) },
                    minLines = 2,
                )
                TextEntry(
                    label = "Tareas por completar",
                    value = draft.outstandingTasks,
                    onValueChange = { draft = draft.copy(outstandingTasks = it) },
                    minLines = 2,
                )
            }

            TextButton(
                onClick = ::saveDraft,
                enabled = draft.hasContent && !isSaving && !isLoadingRecords && storageError == null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isSaving) "Guardando cifrado…" else "Guardar nuevo borrador cifrado")
            }
            TextButton(
                onClick = { pendingAction = DraftAction.Clear },
                enabled = draft.hasContent,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Borrar el texto sin guardar")
            }

            if (feedback != null) {
                Text(
                    text = feedback.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (feedbackIsError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
            if (storageError != null) {
                Text(
                    text = storageError.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            SavedRecordsSection(
                records = records,
                isLoading = isLoadingRecords,
                storageError = storageError,
                expandedRecordId = expandedRecordId,
                onToggleExpanded = { id ->
                    expandedRecordId = if (expandedRecordId == id) null else id
                },
                onDelete = { pendingDeleteId = it },
            )

            Text(
                text = "PAT-CONT-01 · Los registros son snapshots inmutables; cada guardado crea un archivo nuevo. No hay historia de correcciones, identidad de autor ni integración con el registro oficial.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SavedRecordsSection(
    records: List<StoredHandoffRecord>,
    isLoading: Boolean,
    storageError: String?,
    expandedRecordId: String?,
    onToggleExpanded: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    FormSection(
        title = "Borradores guardados en este dispositivo",
        description = "Solo se muestran registros que pueden descifrarse correctamente. No existe sincronización ni respaldo configurado por la app.",
    ) {
        when {
            isLoading -> Text("Leyendo almacenamiento cifrado…")
            storageError != null -> Text(
                text = "No se pudo verificar la lista; no se asume que esté vacía.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            records.isEmpty() -> Text(
                text = "No hay borradores guardados.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> records.forEach { record ->
                SavedRecordCard(
                    record = record,
                    expanded = expandedRecordId == record.id,
                    onToggleExpanded = { onToggleExpanded(record.id) },
                    onDelete = { onDelete(record.id) },
                )
            }
        }
    }
}

@Composable
private fun SavedRecordCard(
    record: StoredHandoffRecord,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
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
                text = "CIFRADO LOCAL · BORRADOR NO OFICIAL",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = record.draft.episodeReference.ifBlank { "Sin código de episodio" },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Guardado ${DateFormat.format("yyyy-MM-dd HH:mm", Date(record.createdAtMillis))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${record.draft.originInstitutionService.ifBlank { "Origen no indicado" }} → " +
                    record.draft.destinationService.ifBlank { "Destino no indicado" },
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = onToggleExpanded) {
                Text(if (expanded) "Ocultar detalle" else "Ver detalle")
            }
            if (expanded) {
                HandoffRecordDetails(record.draft)
            }
            TextButton(onClick = onDelete) {
                Text("Eliminar archivo local")
            }
        }
    }
}

@Composable
private fun HandoffRecordDetails(draft: HandoffDraft) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        DetailField("Tipo", draft.transferType)
        DetailField("Motivo", draft.transferReason)
        DetailField("Prioridad local", draft.localPriority)
        DetailField("Referencia de criterio", draft.localCriteriaReference)
        DetailField("Solicitud / derivación", draft.referralDateTime)
        DetailField("Salida", draft.departureDateTime)
        DetailField("Recepción", draft.arrivalDateTime)
        DetailField("Entrega clínica", draft.handoverDateTime)
        DetailField("Entrega", draft.sendingProfessional)
        DetailField("Recepción por", draft.receivingProfessional)
        DetailField("Problemas activos", draft.activeProblems)
        DetailField("Alergias", draft.allergies)
        DetailField("Intervenciones en curso", draft.interventionsInProgress)
        DetailField("Dispositivos", draft.devices)
        DetailField("Resultados pendientes", draft.pendingResults)
        DetailField("Tareas pendientes", draft.outstandingTasks)
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    if (value.isNotBlank()) {
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FormSection(
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}

@Composable
private fun TextEntry(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = false,
    minLines: Int = 1,
    supportingText: String? = null,
) {
    val supportingContent: (@Composable () -> Unit)? = supportingText?.let { text ->
        { Text(text) }
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        maxLines = if (singleLine) 1 else maxOf(minLines, 6),
        supportingText = supportingContent,
    )
}

@Composable
private fun NoticeCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HandoffScreenPreview() {
    UciAssistantTheme {
        HandoffScreen(onBack = {})
    }
}
