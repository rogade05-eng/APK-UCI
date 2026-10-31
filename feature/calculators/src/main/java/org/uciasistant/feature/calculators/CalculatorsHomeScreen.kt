package org.uciasistant.feature.calculators

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp

private enum class CalculatorTool {
    UnitConverter,
    Oxygenation,
}

@Composable
fun CalculatorsHomeScreen(onBack: () -> Unit) {
    var selectedTool by remember { mutableStateOf<CalculatorTool?>(null) }

    when (selectedTool) {
        CalculatorTool.UnitConverter -> UnitConverterScreen(onBack = { selectedTool = null })
        CalculatorTool.Oxygenation -> OxygenationCalculatorScreen(onBack = { selectedTool = null })
        null -> CalculatorsMenu(
            onBack = onBack,
            onOpenUnitConverter = { selectedTool = CalculatorTool.UnitConverter },
            onOpenOxygenation = { selectedTool = CalculatorTool.Oxygenation },
        )
    }
}

@Composable
private fun CalculatorsMenu(
    onBack: () -> Unit,
    onOpenUnitConverter: () -> Unit,
    onOpenOxygenation: () -> Unit,
) {
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
                Text("← Inicio")
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Calculadoras",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Herramientas offline; revisa las unidades y la población de referencia de cada una.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            CalculatorToolCard(
                title = "Índices de oxigenación pediátrica",
                description = "Calcula P/F, S/F, OI y OSI con las entradas disponibles; no clasifica PARDS.",
                source = "PALICC-2 · 2023 · población pediátrica, no neonatal",
                onClick = onOpenOxygenation,
            )
            CalculatorToolCard(
                title = "Conversor general de unidades",
                description = "Masa, longitud, volumen y temperatura. No incluye unidades de dosis.",
                source = "Conversión matemática general; sin interpretación clínica",
                onClick = onOpenUnitConverter,
            )
            Text(
                text = "Los cálculos no se guardan y no generan diagnóstico, recomendaciones terapéuticas ni alertas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CalculatorToolCard(
    title: String,
    description: String,
    source: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
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
            Text(
                text = source,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
