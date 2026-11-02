package org.uciasistant.feature.alerts

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import java.io.IOException
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun ManualReminderSection() {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val store = remember(appContext) { EncryptedAlertReminderStore(appContext) }
    val scope = rememberCoroutineScope()

    var reminders by remember { mutableStateOf<List<AlertReminder>>(emptyList()) }
    var reminderTitle by remember { mutableStateOf("") }
    var dueAtMillis by remember { mutableStateOf(System.currentTimeMillis() + 60 * 60 * 1_000L) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var notificationsEnabled by remember { mutableStateOf(hasNotificationPermission(context)) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationsEnabled = granted
        statusMessage = if (granted) {
            "Permiso de notificaciones concedido."
        } else {
            "Sin permiso, los recordatorios seguirán en la lista local, pero Android no mostrará avisos."
        }
    }

    LaunchedEffect(store) {
        isLoading = true
        try {
            val loaded = withContext(Dispatchers.IO) { store.listReminders() }
            reminders = loaded
            errorMessage = null
        } catch (_: IOException) {
            errorMessage = "No se pudo abrir el almacenamiento cifrado de recordatorios."
        } finally {
            isLoading = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Recordatorios manuales",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Recordatorios generales, de una sola vez y no clínicos. No están asociados a pacientes, reglas clínicas ni protocolos.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ) {
            Text(
                text = "No escribas nombres, identificadores ni información clínica. El texto y la hora se guardan cifrados localmente; la notificación del sistema solo muestra un aviso genérico. Android puede retrasar u omitir avisos: no usar para urgencias, reevaluaciones clínicas ni tratamientos.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsEnabled) {
            OutlinedButton(
                onClick = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Permitir notificaciones de recordatorios")
            }
        } else {
            Text(
                text = "Android puede retrasar u omitir los avisos, y pueden deshabilitarse desde los ajustes del dispositivo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedTextField(
            value = reminderTitle,
            onValueChange = { value ->
                reminderTitle = value.take(MAX_ALERT_REMINDER_TITLE_CHARACTERS + 1)
                errorMessage = null
                statusMessage = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Título general") },
            supportingText = {
                Text("${reminderTitle.length}/$MAX_ALERT_REMINDER_TITLE_CHARACTERS caracteres · sin datos de pacientes")
            },
            singleLine = true,
        )

        OutlinedButton(
            onClick = {
                showDateTimePicker(context, dueAtMillis) { selected -> dueAtMillis = selected }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Fecha y hora: ${formatDueTime(dueAtMillis)}")
        }

        Button(
            onClick = {
                when (validateAlertReminderInput(reminderTitle, dueAtMillis, System.currentTimeMillis())) {
                    AlertReminderInputError.EMPTY_TITLE -> errorMessage = "Escribe un título general para el recordatorio."
                    AlertReminderInputError.TITLE_TOO_LONG -> errorMessage = "El título supera el límite de caracteres."
                    AlertReminderInputError.DUE_TIME_NOT_IN_FUTURE -> errorMessage = "Selecciona una fecha y hora futuras."
                    null -> {
                        isSaving = true
                        errorMessage = null
                        statusMessage = null
                        scope.launch {
                            try {
                                val saved = withContext(Dispatchers.IO) {
                                    store.create(reminderTitle, dueAtMillis)
                                }
                                AlertReminderWork.schedule(appContext, saved)
                                reminders = withContext(Dispatchers.IO) { store.listReminders() }
                                reminderTitle = ""
                                statusMessage = "Recordatorio guardado en este dispositivo."
                            } catch (_: Exception) {
                                errorMessage = "No se pudo guardar o programar el recordatorio."
                            } finally {
                                isSaving = false
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving && !isLoading,
        ) {
            Text(if (isSaving) "Guardando…" else "Guardar recordatorio")
        }

        errorMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        statusMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Text(
            text = "Pendientes: ${reminders.size}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (isLoading) {
            Text("Cargando recordatorios locales…", style = MaterialTheme.typography.bodySmall)
        } else if (reminders.isEmpty()) {
            Text(
                text = "No hay recordatorios guardados.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            reminders.forEach { reminder ->
                ReminderCard(
                    reminder = reminder,
                    onComplete = {
                        scope.launch {
                            AlertReminderWork.cancel(appContext, reminder.id)
                            AlertReminderNotifications.cancel(appContext, reminder.id)
                            try {
                                withContext(Dispatchers.IO) { store.deleteReminder(reminder.id) }
                                reminders = withContext(Dispatchers.IO) { store.listReminders() }
                                statusMessage = "Recordatorio eliminado."
                                errorMessage = null
                            } catch (_: IOException) {
                                errorMessage = "No se pudo eliminar el recordatorio."
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ReminderCard(reminder: AlertReminder, onComplete: () -> Unit) {
    val isDue = reminder.dueAtMillis <= System.currentTimeMillis()
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
                text = reminder.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isDue) "Vencido" else "Programado",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isDue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = formatDueTime(reminder.dueAtMillis),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onComplete) {
                Text("Marcar completado y borrar")
            }
        }
    }
}

private fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun formatDueTime(timestampMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
        .format(Date(timestampMillis))

private fun showDateTimePicker(context: Context, initialMillis: Long, onSelected: (Long) -> Unit) {
    val initial = Calendar.getInstance().apply {
        timeInMillis = maxOf(initialMillis, System.currentTimeMillis())
    }
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selected = Calendar.getInstance().apply {
                timeInMillis = initial.timeInMillis
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
            }
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    selected.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    selected.set(Calendar.MINUTE, minute)
                    selected.set(Calendar.SECOND, 0)
                    selected.set(Calendar.MILLISECOND, 0)
                    onSelected(selected.timeInMillis)
                },
                selected.get(Calendar.HOUR_OF_DAY),
                selected.get(Calendar.MINUTE),
                DateFormat.is24HourFormat(context),
            ).show()
        },
        initial.get(Calendar.YEAR),
        initial.get(Calendar.MONTH),
        initial.get(Calendar.DAY_OF_MONTH),
    ).apply {
        datePicker.minDate = System.currentTimeMillis()
    }.show()
}
