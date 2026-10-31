package org.uciasistant.feature.calculators

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import org.uciasistant.core.designsystem.UciAssistantTheme
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun UnitConverterScreen(onBack: () -> Unit) {
    var category by remember { mutableStateOf(MeasurementCategory.MASS) }
    var fromUnit by remember { mutableStateOf(MeasurementUnit.KILOGRAM) }
    var toUnit by remember { mutableStateOf(MeasurementUnit.GRAM) }
    var input by remember { mutableStateOf("") }
    val units = category.units
    val conversion = remember(input, fromUnit, toUnit) {
        if (input.isBlank()) null else runCatching {
            UnitConversionEngine.convert(input, fromUnit, toUnit)
        }
    }

    BackHandler(onBack = onBack)

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
                Text(text = "← Calculadoras")
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Conversor de unidades",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Conversión matemática general, disponible sin conexión.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SafetyNotice()

            Text(
                text = "Magnitud",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MeasurementCategory.entries.forEach { option ->
                    FilterChip(
                        selected = category == option,
                        onClick = {
                            category = option
                            fromUnit = option.units.first()
                            toUnit = option.units.last()
                        },
                        label = { Text(option.label) },
                    )
                }
            }

            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Valor") },
                supportingText = { Text("Usa coma o punto decimal; no uses separadores de miles.") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                UnitSelector(
                    selected = fromUnit,
                    options = units,
                    modifier = Modifier.weight(1f),
                    onSelect = { fromUnit = it },
                )
                TextButton(
                    onClick = {
                        val previousFrom = fromUnit
                        fromUnit = toUnit
                        toUnit = previousFrom
                    },
                ) {
                    Text("⇄")
                }
                UnitSelector(
                    selected = toUnit,
                    options = units,
                    modifier = Modifier.weight(1f),
                    onSelect = { toUnit = it },
                )
            }

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
                        text = "Resultado",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    when {
                        input.isBlank() -> Text(
                            text = "Introduce un valor para convertir.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        conversion?.isSuccess == true -> Text(
                            text = "${UnitConversionEngine.format(conversion.getOrThrow())} ${toUnit.symbol()}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        else -> Text(
                            text = conversion?.exceptionOrNull()?.message ?: "No se pudo convertir el valor.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Text(
                text = "Unidades disponibles: kg/g; m/cm/mm; L/mL; °C/°F/K. No incluye unidades de dosis.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun UnitSelector(
    selected: MeasurementUnit,
    options: List<MeasurementUnit>,
    modifier: Modifier = Modifier,
    onSelect: (MeasurementUnit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(selected.symbol())
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun SafetyNotice() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Herramienta general, no clínica",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "No calcula dosis, infusiones, escalas ni recomendaciones; no valida datos de pacientes y no los guarda. No usar para tomar decisiones asistenciales.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun MeasurementUnit.symbol(): String = when (this) {
    MeasurementUnit.KILOGRAM -> "kg"
    MeasurementUnit.GRAM -> "g"
    MeasurementUnit.METER -> "m"
    MeasurementUnit.CENTIMETER -> "cm"
    MeasurementUnit.MILLIMETER -> "mm"
    MeasurementUnit.LITER -> "L"
    MeasurementUnit.MILLILITER -> "mL"
    MeasurementUnit.CELSIUS -> "°C"
    MeasurementUnit.FAHRENHEIT -> "°F"
    MeasurementUnit.KELVIN -> "K"
}

@Preview(showBackground = true)
@Composable
private fun UnitConverterScreenPreview() {
    UciAssistantTheme {
        UnitConverterScreen(onBack = {})
    }
}
