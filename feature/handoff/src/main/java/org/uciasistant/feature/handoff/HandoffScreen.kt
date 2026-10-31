package org.uciasistant.feature.handoff

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.uciasistant.core.designsystem.PrototypeNotice
import org.uciasistant.core.designsystem.UciAssistantTheme

private enum class DraftAction {
    Exit,
    Clear,
}

/** A temporary, non-persistent handoff form. It is not an official clinical record. */
@Composable
fun HandoffScreen(onBack: () -> Unit) {
    var draft by remember { mutableStateOf(HandoffDraft()) }
    var pendingAction by remember { mutableStateOf<DraftAction?>(null) }

    fun requestExit() {
        if (draft.hasContent) {
            pendingAction = DraftAction.Exit
        } else {
            onBack()
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
            title = { Text(if (isExit) "Descartar borrador" else "Borrar campos") },
            text = {
                Text(
                    if (isExit) {
                        "Al salir, el texto temporal se perderá. No se ha guardado ni enviado."
                    } else {
                        "Se borrará todo el texto de esta pantalla. No se puede recuperar."
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
                    text = "Plantilla temporal para estructurar datos de un traslado pediátrico.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            PrototypeNotice(modifier = Modifier.fillMaxWidth())

            NoticeCard(
                title = "Solo en memoria · no es el registro oficial",
                body = "El texto permanece únicamente mientras esta pantalla está abierta. No se guarda, cifra, exporta, copia ni sincroniza. No introduzcas datos reales de pacientes ni uses esta plantilla para una entrega asistencial.",
            )

            NoticeCard(
                title = "Sin escalas ni prioridades predefinidas",
                body = "El motivo, la prioridad y la referencia a criterios locales son campos de texto manuales. La app no asigna prioridad, no evalúa admisión o destino y no genera una decisión clínica.",
            )

            FormSection(
                title = "Referencia y destino",
                description = "Usa solo códigos de episodio ficticios en este prototipo.",
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
                onClick = { pendingAction = DraftAction.Clear },
                enabled = draft.hasContent,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Borrar todo el borrador")
            }

            Text(
                text = "Especificación de referencia: PAT-CONT-01 de la adenda técnica. Esta pantalla no implementa persistencia, auditoría, firma, autorización institucional ni integración con historia clínica.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
