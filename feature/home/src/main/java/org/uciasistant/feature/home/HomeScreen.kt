package org.uciasistant.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import org.uciasistant.core.designsystem.PrototypeNotice
import org.uciasistant.core.designsystem.UciAssistantTheme

@Composable
fun HomeScreen(
    onOpenProtocols: () -> Unit,
    onOpenAlerts: () -> Unit,
    onOpenCalculator: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    text = "PROTOTIPO · PEDIATRÍA",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "UCI Assistant",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Concepto de interfaz para apoyo profesional en UCI pediátrica y urgencias.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            PrototypeNotice(modifier = Modifier.fillMaxWidth())

            SectionHeading(text = "ENTORNOS PREVISTOS")
            ContextCard(
                title = "UCI pediátrica",
                description = "Entorno previsto; flujo clínico pendiente de definición.",
            )
            ContextCard(
                title = "Urgencias pediátricas",
                description = "Entorno previsto; flujo clínico pendiente de definición.",
            )

            Spacer(modifier = Modifier.height(2.dp))
            SectionHeading(text = "HERRAMIENTAS DISPONIBLES")
            AvailableFeatureCard(
                title = "Calculadoras y conversores",
                description = "Incluye conversión de unidades e índices pediátricos de oxigenación P/F, S/F, OI y OSI; no clasifica PARDS.",
                onClick = onOpenCalculator,
            )

            SectionHeading(text = "FUNCIONES CLÍNICAS EN DEFINICIÓN")
            PlannedFeatureCard(
                title = "Protocolos de consulta",
                description = "Biblioteca prevista. Contenido, derechos y aprobaciones pendientes.",
                onClick = onOpenProtocols,
            )
            PlannedFeatureCard(
                title = "Alertas individualizadas",
                description = "Se prevé enlazarlas con un protocolo aprobado para consulta. No hay criterios ni alertas activas.",
                onClick = onOpenAlerts,
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Límites de esta versión",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Incluye conversión de unidades e índices de oxigenación, sin clasificación clínica ni interpretación automática. No almacena pacientes, no contiene protocolos asistenciales y no genera recomendaciones, IA, prescripción automática ni control de equipos.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Text(
                text = "Sin datos de pacientes · Sin diagnóstico autónomo · Sin prescripción automática",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun ContextCard(title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
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

@Composable
private fun AvailableFeatureCard(
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "HERRAMIENTAS OFFLINE · SIN DIAGNÓSTICO NI RECOMENDACIÓN",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            TextButton(onClick = onClick) {
                Text(text = "Abrir calculadoras")
            }
        }
    }
}

@Composable
private fun PlannedFeatureCard(
    title: String,
    description: String,
    onClick: () -> Unit,
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
                text = "EN DEFINICIÓN · NO DISPONIBLE PARA USO CLÍNICO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
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
            TextButton(onClick = onClick) {
                Text(text = "Ver alcance del módulo")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    UciAssistantTheme {
        HomeScreen(
            onOpenProtocols = {},
            onOpenAlerts = {},
            onOpenCalculator = {},
        )
    }
}
