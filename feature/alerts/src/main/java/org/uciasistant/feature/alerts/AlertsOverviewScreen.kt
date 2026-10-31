package org.uciasistant.feature.alerts

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.uciasistant.core.designsystem.PrototypeNotice

@Composable
fun AlertsOverviewScreen(onBack: () -> Unit) {
    var showRequirements by remember { mutableStateOf(false) }

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
                    text = "Alertas individualizadas",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Módulo separado de la biblioteca de protocolos.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            PrototypeNotice(modifier = Modifier.fillMaxWidth())

            AlertsSummaryCard()

            InfoCard(
                title = "No se evalúan pacientes",
                description = "No hay reglas, activadores, cálculos ni umbrales implementados. La aplicación no recibe ni procesa datos de pacientes y no genera notificaciones clínicas.",
            )

            TextButton(onClick = { showRequirements = !showRequirements }) {
                Text(
                    text = if (showRequirements) {
                        "Ocultar decisiones pendientes"
                    } else {
                        "Ver decisiones pendientes"
                    },
                )
            }

            if (showRequirements) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RequirementCard(
                        number = "01",
                        title = "Definir población y alcance",
                        description = "Acordar los dominios y subgrupos pediátricos. Las reglas, escalas y unidades para adultos, pediatría y neonatología deben mantenerse separadas.",
                    )
                    RequirementCard(
                        number = "02",
                        title = "Especificar los datos de entrada",
                        description = "Definir qué información autorizada usaría cada alerta, su origen, actualización y controles de calidad. No se solicitan datos en esta versión.",
                    )
                    RequirementCard(
                        number = "03",
                        title = "Investigar criterios y documentar su validación",
                        description = "El desarrollo y las pruebas pueden avanzar con fuentes identificadas. La revisión especializada y la validación son necesarias antes de habilitar una regla para uso asistencial, no un bloqueo para completar el código.",
                    )
                    RequirementCard(
                        number = "04",
                        title = "Vincular contenido aprobado",
                        description = "Definir la correspondencia de cada alerta con el protocolo vigente aprobado que se ofrecería para consulta.",
                    )
                    RequirementCard(
                        number = "05",
                        title = "Mantener la decisión profesional",
                        description = "La alerta sería informativa: el profesional revisaría el protocolo y decidiría. No habría diagnóstico autónomo, prescripción automática ni acción sobre equipos.",
                    )
                }
            }

            InfoCard(
                title = "Relación prevista con Protocolos",
                description = "Las alertas y la biblioteca son funciones distintas. Si se aprueba una alerta, su enlace abriría el protocolo vigente correspondiente para consulta; esa relación aún no está definida.",
            )

            Text(
                text = "Sin alertas activas · Sin datos de pacientes · Sin automatización clínica",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AlertsSummaryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
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
                    text = "MÓDULO NO ACTIVO",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = "0 alertas activas",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "No hay reglas aprobadas ni señales conectadas a esta aplicación.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
                text = "PENDIENTE $number",
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
