package org.uciasistant.feature.calculators

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun OxygenationCalculatorScreen(onBack: () -> Unit) {
    var fio2 by remember { mutableStateOf("") }
    var pao2 by remember { mutableStateOf("") }
    var spo2 by remember { mutableStateOf("") }
    var meanAirwayPressure by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<OxygenationResults?>(null) }
    var inputError by remember { mutableStateOf<String?>(null) }

    fun clearResult() {
        results = null
        inputError = null
    }

    fun calculate() {
        val calculation = runCatching {
            OxygenationIndices.calculate(
                OxygenationInputs(
                    fio2Percent = OxygenationIndices.parse(fio2),
                    pao2MmHg = pao2.optionalDecimal(),
                    spo2Percent = spo2.optionalDecimal(),
                    meanAirwayPressureCmH2O = meanAirwayPressure.optionalDecimal(),
                ),
            )
        }
        calculation.fold(
            onSuccess = {
                results = it
                inputError = null
            },
            onFailure = {
                results = null
                inputError = it.message ?: "Revisa las entradas."
            },
        )
    }

    BackHandler(onBack = onBack)

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TextButton(onClick = onBack) {
                Text("← Calculadoras")
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Índices de oxigenación pediátrica",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "P/F · S/F · OI · OSI",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SourceNotice()

            CalculatorInput(
                value = fio2,
                onValueChange = { fio2 = it; clearResult() },
                label = "FiO₂ (%) · obligatorio",
                supportingText = "Introduce el porcentaje: 40 equivale a una fracción de 0,40.",
            )
            CalculatorInput(
                value = pao2,
                onValueChange = { pao2 = it; clearResult() },
                label = "PaO₂ (mmHg) · opcional",
                supportingText = "Necesaria para P/F y, junto con MAP, para OI.",
            )
            CalculatorInput(
                value = spo2,
                onValueChange = { spo2 = it; clearResult() },
                label = "SpO₂ (%) · opcional",
                supportingText = "Necesaria para S/F y, junto con MAP, para OSI.",
            )
            CalculatorInput(
                value = meanAirwayPressure,
                onValueChange = { meanAirwayPressure = it; clearResult() },
                label = "Presión media de vía aérea, MAP (cmH₂O) · opcional",
                supportingText = "Se requiere para calcular OI u OSI.",
            )

            Button(
                onClick = ::calculate,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Calcular índices disponibles")
            }

            if (inputError != null) {
                Text(
                    text = inputError.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            results?.let { calculatedResults ->
                ResultsCard(
                    results = calculatedResults,
                    hasPao2 = pao2.isNotBlank(),
                    hasSpo2 = spo2.isNotBlank(),
                    hasMap = meanAirwayPressure.isNotBlank(),
                )
            }

            Text(
                text = "Devuelve únicamente los índices calculados. No clasifica PARDS ni genera diagnóstico, tratamiento o alerta. No se guardan las entradas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CalculatorInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    supportingText: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        supportingText = { Text(supportingText) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

@Composable
private fun SourceNotice() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Fuente y alcance",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "PALICC-2 (2023; DOI 10.1097/PCC.0000000000003147), pediatría; no neonatal. Medir en estado estable, no durante desaturaciones transitorias. La fuente limita los cocientes basados en SpO₂ a SpO₂ ≤ 97 %.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ResultsCard(
    results: OxygenationResults,
    hasPao2: Boolean,
    hasSpo2: Boolean,
    hasMap: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Resultados",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            ResultLine("PaO₂/FiO₂ (P/F)", results.pfRatio, "mmHg")
            ResultLine("SpO₂/FiO₂ (S/F)", results.sfRatio, "ratio")
            ResultLine("Índice de oxigenación (OI)", results.oxygenationIndex, "adimensional")
            ResultLine("Índice de saturación de oxígeno (OSI)", results.oxygenSaturationIndex, "adimensional")

            if (results.saturationExceedsSfLimit) {
                Text(
                    text = "S/F y OSI no se muestran: SpO₂ supera 97 %, límite de uso indicado por PALICC-2.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (!hasPao2) {
                Text("P/F y OI requieren PaO₂.", style = MaterialTheme.typography.bodySmall)
            }
            if (!hasSpo2) {
                Text("S/F y OSI requieren SpO₂.", style = MaterialTheme.typography.bodySmall)
            }
            if (!hasMap) {
                Text("OI y OSI requieren MAP.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ResultLine(label: String, value: java.math.BigDecimal?, unit: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = if (value == null) "—" else "${OxygenationIndices.format(value)} $unit",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

private fun String.optionalDecimal() = takeIf(String::isNotBlank)?.let(OxygenationIndices::parse)
