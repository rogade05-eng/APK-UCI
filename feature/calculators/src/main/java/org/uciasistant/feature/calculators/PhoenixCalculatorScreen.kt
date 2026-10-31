package org.uciasistant.feature.calculators

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private data class PhoenixFormState(
    val elapsedHours: String = "",
    val ageMonths: String = "",
    val postconceptionalAgeWeeks: String = "",
    val birthHospitalization: Boolean? = null,
    val respiratorySupport: PhoenixRespiratorySupport? = null,
    val fio2Percent: String = "",
    val pao2MmHg: String = "",
    val spo2Percent: String = "",
    val vasoactiveMedicationCount: String = "",
    val lactateMmolL: String = "",
    val meanArterialPressureMmHg: String = "",
    val platelets: String = "",
    val inr: String = "",
    val dDimer: String = "",
    val fibrinogen: String = "",
    val glasgowComaScale: String = "",
    val bilaterallyFixedPupils: Boolean? = null,
)

private data class PhoenixSnapshot(
    val elapsedHours: java.math.BigDecimal,
    val score: PhoenixScoreResult,
)

@Composable
fun PhoenixCalculatorScreen(onBack: () -> Unit) {
    var form by remember { mutableStateOf(PhoenixFormState()) }
    var calculationError by remember { mutableStateOf<String?>(null) }
    val snapshots = remember { mutableStateListOf<PhoenixSnapshot>() }

    fun addAssessment() {
        val result = runCatching {
            val elapsedHours = form.elapsedHours.takeIf(String::isNotBlank)
                ?.let(PhoenixScoreCalculator::parse)
                ?: throw IllegalArgumentException("Introduce la hora transcurrida desde el inicio (0–24).")
            require(elapsedHours >= java.math.BigDecimal.ZERO && elapsedHours <= java.math.BigDecimal("24")) {
                "La evaluación debe situarse entre 0 y 24 horas."
            }
            val score = PhoenixScoreCalculator.calculate(form.toInputs())
            PhoenixSnapshot(elapsedHours, score)
        }
        result.fold(
            onSuccess = {
                snapshots.add(it)
                calculationError = null
                form = PhoenixFormState()
            },
            onFailure = {
                calculationError = it.message ?: "Revisa las entradas."
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
                    text = "Phoenix · puntuación pediátrica",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Puntuación por evaluación; sin diagnóstico ni clasificación automática.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            PhoenixSourceNotice()

            PhoenixSection(title = "Población y ventana") {
                PhoenixInputField(
                    value = form.elapsedHours,
                    onValueChange = { form = form.copy(elapsedHours = it); calculationError = null },
                    label = "Horas desde inicio del encuentro (0–24) · obligatorio",
                    supportingText = "Admite decimales. Añade evaluaciones del mismo encuentro; el máximo se calcula entre las registradas.",
                )
                PhoenixInputField(
                    value = form.ageMonths,
                    onValueChange = { form = form.copy(ageMonths = it); calculationError = null },
                    label = "Edad cronológica (meses completos) · obligatorio",
                    supportingText = "Alcance disponible: 1–215 meses. No se corrige por prematuridad.",
                    keyboardType = KeyboardType.Number,
                )
                PhoenixInputField(
                    value = form.postconceptionalAgeWeeks,
                    onValueChange = {
                        form = form.copy(postconceptionalAgeWeeks = it)
                        calculationError = null
                    },
                    label = "Edad posconcepcional en esta evaluación (semanas completas) · obligatoria",
                    supportingText = "El estudio excluyó edad posconcepcional menor de 37 semanas. No es la edad usada para ajustar MAP.",
                    keyboardType = KeyboardType.Number,
                )
                Text("¿Es la hospitalización del nacimiento? · obligatorio", style = MaterialTheme.typography.labelLarge)
                ChoiceRow {
                    FilterChip(
                        selected = form.birthHospitalization == true,
                        onClick = { form = form.copy(birthHospitalization = true); calculationError = null },
                        label = { Text("Sí") },
                    )
                    FilterChip(
                        selected = form.birthHospitalization == false,
                        onClick = { form = form.copy(birthHospitalization = false); calculationError = null },
                        label = { Text("No") },
                    )
                }
            }

            PhoenixSection(title = "Respiratorio · máximo 3 puntos") {
                Text("Soporte respiratorio · obligatorio", style = MaterialTheme.typography.labelLarge)
                ChoiceRow {
                    FilterChip(
                        selected = form.respiratorySupport == PhoenixRespiratorySupport.NONE,
                        onClick = {
                            form = form.copy(respiratorySupport = PhoenixRespiratorySupport.NONE)
                            calculationError = null
                        },
                        label = { Text("Sin soporte") },
                    )
                    FilterChip(
                        selected = form.respiratorySupport == PhoenixRespiratorySupport.NON_INVASIVE_OR_OXYGEN,
                        onClick = {
                            form = form.copy(respiratorySupport = PhoenixRespiratorySupport.NON_INVASIVE_OR_OXYGEN)
                            calculationError = null
                        },
                        label = { Text("Oxígeno / no invasivo") },
                    )
                    FilterChip(
                        selected = form.respiratorySupport == PhoenixRespiratorySupport.INVASIVE_MECHANICAL_VENTILATION,
                        onClick = {
                            form = form.copy(
                                respiratorySupport = PhoenixRespiratorySupport.INVASIVE_MECHANICAL_VENTILATION,
                            )
                            calculationError = null
                        },
                        label = { Text("IMV") },
                    )
                }
                PhoenixInputField(
                    value = form.fio2Percent,
                    onValueChange = { form = form.copy(fio2Percent = it); calculationError = null },
                    label = "FiO₂ (%) · opcional",
                    supportingText = "Porcentaje: 40 equivale a una fracción de 0,40.",
                )
                PhoenixInputField(
                    value = form.pao2MmHg,
                    onValueChange = { form = form.copy(pao2MmHg = it); calculationError = null },
                    label = "PaO₂ (mmHg) · opcional",
                )
                PhoenixInputField(
                    value = form.spo2Percent,
                    onValueChange = { form = form.copy(spo2Percent = it); calculationError = null },
                    label = "SpO₂ (%) · opcional",
                    supportingText = "S/F se excluye si SpO₂ supera 97 %.",
                )
            }

            PhoenixSection(title = "Cardiovascular · máximo 6 puntos") {
                PhoenixInputField(
                    value = form.vasoactiveMedicationCount,
                    onValueChange = {
                        form = form.copy(vasoactiveMedicationCount = it)
                        calculationError = null
                    },
                    label = "Número de vasoactivos sistémicos concurrentes (0–6)",
                    supportingText = "Dobutamina, dopamina, epinefrina, milrinona, norepinefrina o vasopresina.",
                    keyboardType = KeyboardType.Number,
                )
                PhoenixInputField(
                    value = form.lactateMmolL,
                    onValueChange = { form = form.copy(lactateMmolL = it); calculationError = null },
                    label = "Lactato (mmol/L) · opcional",
                )
                PhoenixInputField(
                    value = form.meanArterialPressureMmHg,
                    onValueChange = {
                        form = form.copy(meanArterialPressureMmHg = it)
                        calculationError = null
                    },
                    label = "Presión arterial media medida (mmHg) · opcional",
                    supportingText = "Se prefiere la MAP medida; no se deriva desde PAS/PAD.",
                )
            }

            PhoenixSection(title = "Coagulación · máximo 2 puntos") {
                PhoenixInputField(
                    value = form.platelets,
                    onValueChange = { form = form.copy(platelets = it); calculationError = null },
                    label = "Plaquetas (×10³/µL) · opcional",
                )
                PhoenixInputField(
                    value = form.inr,
                    onValueChange = { form = form.copy(inr = it); calculationError = null },
                    label = "INR · opcional",
                )
                PhoenixInputField(
                    value = form.dDimer,
                    onValueChange = { form = form.copy(dDimer = it); calculationError = null },
                    label = "D-dímero (mg/L FEU) · opcional",
                )
                PhoenixInputField(
                    value = form.fibrinogen,
                    onValueChange = { form = form.copy(fibrinogen = it); calculationError = null },
                    label = "Fibrinógeno (mg/dL) · opcional",
                )
            }

            PhoenixSection(title = "Neurológico · máximo 2 puntos") {
                PhoenixInputField(
                    value = form.glasgowComaScale,
                    onValueChange = { form = form.copy(glasgowComaScale = it); calculationError = null },
                    label = "Glasgow (GCS, 3–15) · opcional",
                    keyboardType = KeyboardType.Number,
                )
                Text("¿Pupilas bilateralmente fijas? · opcional", style = MaterialTheme.typography.labelLarge)
                ChoiceRow {
                    FilterChip(
                        selected = form.bilaterallyFixedPupils == true,
                        onClick = {
                            form = form.copy(bilaterallyFixedPupils = true)
                            calculationError = null
                        },
                        label = { Text("Sí") },
                    )
                    FilterChip(
                        selected = form.bilaterallyFixedPupils == false,
                        onClick = {
                            form = form.copy(bilaterallyFixedPupils = false)
                            calculationError = null
                        },
                        label = { Text("No") },
                    )
                }
            }

            Button(onClick = ::addAssessment, modifier = Modifier.fillMaxWidth()) {
                Text("Añadir evaluación")
            }
            if (calculationError != null) {
                Text(
                    text = calculationError.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (snapshots.isNotEmpty()) {
                val highest = snapshots.maxByOrNull { it.score.totalPoints }
                highest?.let {
                    HighestPhoenixScoreCard(
                        snapshot = it,
                        totalAssessments = snapshots.size,
                    )
                }
                TextButton(onClick = { snapshots.clear() }) {
                    Text("Borrar evaluaciones de esta sesión")
                }
                Text(
                    text = "Evaluaciones añadidas (${snapshots.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                snapshots.sortedBy { it.elapsedHours }.forEachIndexed { index, snapshot ->
                    PhoenixSnapshotCard(index + 1, snapshot)
                }
            }

            Text(
                text = "Devuelve puntuaciones por sistema y el total de las evaluaciones introducidas. No aplica umbrales de sepsis o shock, no diagnostica ni recomienda tratamiento. Las entradas no se guardan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PhoenixSourceNotice() {
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
            Text("Fuente y límites", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                text = "Phoenix Sepsis Score, 4 sistemas. Fuentes: JAMA 2024, DOI 10.1001/jama.2024.0196 y DOI 10.1001/jama.2024.0179. Cada fila debe reunir datos de una misma evaluación; no combines máximos por sistema de horas distintas. Se muestra el máximo de las filas introducidas, no necesariamente todo el período de 24 h.",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "Los datos ausentes puntúan 0 según la implementación publicada; aquí se señalan como ausentes y no se interpretan como normales. No neonatal: se excluyen hospitalización del nacimiento y edad posconcepcional <37 semanas.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun PhoenixSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun PhoenixInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    supportingText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Decimal,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        supportingText = { supportingText?.let { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    )
}

@Composable
private fun ChoiceRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun HighestPhoenixScoreCard(snapshot: PhoenixSnapshot, totalAssessments: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Máximo observado en las evaluaciones introducidas", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "${snapshot.score.totalPoints} / 13 · hora ${UnitConversionEngine.format(snapshot.elapsedHours)}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = if (snapshot.score.isComplete) {
                    "La evaluación máxima no tiene variables ausentes. Se introdujeron $totalAssessments evaluación(es)."
                } else {
                    "Puntuación basada en datos conocidos; hay variables ausentes en esta evaluación. Se introdujeron $totalAssessments evaluación(es)."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun PhoenixSnapshotCard(index: Int, snapshot: PhoenixSnapshot) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = "Evaluación $index · hora ${UnitConversionEngine.format(snapshot.elapsedHours)} · total ${snapshot.score.totalPoints}/13",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Respiratorio ${snapshot.score.respiratory.points}/3 · Cardiovascular ${snapshot.score.cardiovascular.points}/6 · Coagulación ${snapshot.score.coagulation.points}/2 · Neurológico ${snapshot.score.neurologic.points}/2",
                style = MaterialTheme.typography.bodySmall,
            )
            if (!snapshot.score.isComplete) {
                Text(
                    text = "Ausentes: ${snapshot.score.missingInputs.joinToString()}. No equivalen a valores normales; por convención del cálculo aportan 0 puntos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            val notes = listOf(
                snapshot.score.respiratory,
                snapshot.score.cardiovascular,
                snapshot.score.coagulation,
                snapshot.score.neurologic,
            ).flatMap(PhoenixDomainScore::notes).distinct()
            notes.forEach { note ->
                Text(note, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun PhoenixFormState.toInputs(): PhoenixScoreInputs {
    val selectedSupport = respiratorySupport
        ?: throw IllegalArgumentException("Selecciona el tipo de soporte respiratorio.")
    val birthStatus = birthHospitalization
        ?: throw IllegalArgumentException("Indica si es la hospitalización del nacimiento.")
    return PhoenixScoreInputs(
        ageMonths = ageMonths.toIntOrNull()
            ?: throw IllegalArgumentException("Introduce la edad en meses completos."),
        postconceptionalAgeAtAssessmentWeeks = postconceptionalAgeWeeks.toIntOrNull()
            ?: throw IllegalArgumentException("Introduce la edad posconcepcional de la evaluación en semanas."),
        isBirthHospitalization = birthStatus,
        respiratorySupport = selectedSupport,
        fio2Percent = fio2Percent.optionalPhoenixDecimal(),
        pao2MmHg = pao2MmHg.optionalPhoenixDecimal(),
        spo2Percent = spo2Percent.optionalPhoenixDecimal(),
        vasoactiveMedicationCount = vasoactiveMedicationCount.optionalPhoenixInt(),
        lactateMmolL = lactateMmolL.optionalPhoenixDecimal(),
        meanArterialPressureMmHg = meanArterialPressureMmHg.optionalPhoenixDecimal(),
        plateletsThousandPerMicroliter = platelets.optionalPhoenixDecimal(),
        inr = inr.optionalPhoenixDecimal(),
        dDimerMgPerLiterFeu = dDimer.optionalPhoenixDecimal(),
        fibrinogenMgPerDl = fibrinogen.optionalPhoenixDecimal(),
        glasgowComaScale = glasgowComaScale.optionalPhoenixInt(),
        bilaterallyFixedPupils = bilaterallyFixedPupils,
    )
}

private fun String.optionalPhoenixDecimal() =
    takeIf(String::isNotBlank)?.let(PhoenixScoreCalculator::parse)

private fun String.optionalPhoenixInt(): Int? =
    takeIf(String::isNotBlank)?.toIntOrNull()
        ?: if (isBlank()) null else throw IllegalArgumentException("Introduce un número entero válido.")
