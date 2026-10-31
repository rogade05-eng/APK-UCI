package org.uciasistant.feature.calculators

import java.math.BigDecimal
import java.math.MathContext

internal enum class PhoenixRespiratorySupport {
    NONE,
    NON_INVASIVE_OR_OXYGEN,
    INVASIVE_MECHANICAL_VENTILATION,
}

internal data class PhoenixScoreInputs(
    val ageMonths: Int,
    val postconceptionalAgeAtAssessmentWeeks: Int,
    val isBirthHospitalization: Boolean,
    val respiratorySupport: PhoenixRespiratorySupport,
    val fio2Percent: BigDecimal? = null,
    val pao2MmHg: BigDecimal? = null,
    val spo2Percent: BigDecimal? = null,
    val vasoactiveMedicationCount: Int? = null,
    val lactateMmolL: BigDecimal? = null,
    val meanArterialPressureMmHg: BigDecimal? = null,
    val plateletsThousandPerMicroliter: BigDecimal? = null,
    val inr: BigDecimal? = null,
    val dDimerMgPerLiterFeu: BigDecimal? = null,
    val fibrinogenMgPerDl: BigDecimal? = null,
    val glasgowComaScale: Int? = null,
    val bilaterallyFixedPupils: Boolean? = null,
)

internal data class PhoenixDomainScore(
    val points: Int,
    val missingInputs: List<String> = emptyList(),
    val notes: List<String> = emptyList(),
)

internal data class PhoenixScoreResult(
    val respiratory: PhoenixDomainScore,
    val cardiovascular: PhoenixDomainScore,
    val coagulation: PhoenixDomainScore,
    val neurologic: PhoenixDomainScore,
) {
    val totalPoints: Int
        get() = respiratory.points + cardiovascular.points + coagulation.points + neurologic.points

    val missingInputs: List<String>
        get() = listOf(respiratory, cardiovascular, coagulation, neurologic)
            .flatMap(PhoenixDomainScore::missingInputs)
            .distinct()

    val isComplete: Boolean
        get() = missingInputs.isEmpty()
}

/**
 * Phoenix four-organ-system score arithmetic for one assessment timepoint.
 * Missing values contribute zero points as in the published software convention,
 * but are returned explicitly so the UI does not present them as normal findings.
 * This object does not apply sepsis or septic-shock classification thresholds.
 */
internal object PhoenixScoreCalculator {
    private val ZERO = BigDecimal.ZERO
    private val ONE_HUNDRED = BigDecimal("100")
    private val MAX_SF_SPO2 = BigDecimal("97")
    private val MATH_CONTEXT = MathContext.DECIMAL128

    fun calculate(inputs: PhoenixScoreInputs): PhoenixScoreResult {
        validate(inputs)
        return PhoenixScoreResult(
            respiratory = scoreRespiratory(inputs),
            cardiovascular = scoreCardiovascular(inputs),
            coagulation = scoreCoagulation(inputs),
            neurologic = scoreNeurologic(inputs),
        )
    }

    fun parse(raw: String): BigDecimal = UnitConversionEngine.parseInput(raw)

    private fun validate(inputs: PhoenixScoreInputs) {
        require(inputs.ageMonths in 1 until 216) {
            "El alcance de esta pantalla es de 1 a 215 meses; no es neonatal ni incluye 18 años o más."
        }
        require(inputs.postconceptionalAgeAtAssessmentWeeks >= 37) {
            "El estudio Phoenix excluyó edad posconcepcional menor de 37 semanas."
        }
        require(!inputs.isBirthHospitalization) {
            "Phoenix no se desarrolló para hospitalizaciones del nacimiento."
        }
        require(
            inputs.fio2Percent == null ||
                inputs.fio2Percent >= BigDecimal("21") && inputs.fio2Percent <= ONE_HUNDRED,
        ) {
            "FiO₂ debe introducirse como porcentaje entre 21 y 100."
        }
        require(
            inputs.respiratorySupport != PhoenixRespiratorySupport.NONE ||
                inputs.fio2Percent == null || inputs.fio2Percent <= BigDecimal("21"),
        ) {
            "Revisa las entradas: FiO₂ superior a 21 % no coincide con ‘sin soporte’."
        }
        require(inputs.pao2MmHg == null || inputs.pao2MmHg > ZERO) {
            "PaO₂ debe ser mayor que 0 mmHg."
        }
        require(
            inputs.spo2Percent == null ||
                inputs.spo2Percent > ZERO && inputs.spo2Percent <= ONE_HUNDRED,
        ) {
            "SpO₂ debe ser mayor que 0 y no superar 100 %."
        }
        require(inputs.vasoactiveMedicationCount == null || inputs.vasoactiveMedicationCount in 0..6) {
            "El conteo de vasoactivos sistémicos debe estar entre 0 y 6."
        }
        require(inputs.lactateMmolL == null || inputs.lactateMmolL >= ZERO) {
            "El lactato no puede ser negativo."
        }
        require(
            inputs.meanArterialPressureMmHg == null || inputs.meanArterialPressureMmHg > ZERO,
        ) {
            "La presión arterial media debe ser mayor que 0 mmHg."
        }
        require(
            inputs.plateletsThousandPerMicroliter == null ||
                inputs.plateletsThousandPerMicroliter >= ZERO,
        ) {
            "Las plaquetas no pueden ser negativas."
        }
        require(inputs.inr == null || inputs.inr > ZERO) {
            "INR debe ser mayor que 0."
        }
        require(inputs.dDimerMgPerLiterFeu == null || inputs.dDimerMgPerLiterFeu >= ZERO) {
            "D-dímero no puede ser negativo."
        }
        require(inputs.fibrinogenMgPerDl == null || inputs.fibrinogenMgPerDl >= ZERO) {
            "El fibrinógeno no puede ser negativo."
        }
        require(inputs.glasgowComaScale == null || inputs.glasgowComaScale in 3..15) {
            "La escala de Glasgow debe ser un entero entre 3 y 15."
        }
    }

    private fun scoreRespiratory(inputs: PhoenixScoreInputs): PhoenixDomainScore {
        if (inputs.respiratorySupport == PhoenixRespiratorySupport.NONE) {
            return PhoenixDomainScore(points = 0)
        }

        val missing = mutableListOf<String>()
        val notes = mutableListOf<String>()
        val fio2Fraction = inputs.fio2Percent?.divide(ONE_HUNDRED, MATH_CONTEXT)
        val pfRatio = if (fio2Fraction != null && inputs.pao2MmHg != null) {
            inputs.pao2MmHg.divide(fio2Fraction, MATH_CONTEXT)
        } else {
            null
        }
        val sfRatio = if (
            fio2Fraction != null &&
            inputs.spo2Percent != null &&
            inputs.spo2Percent <= MAX_SF_SPO2
        ) {
            inputs.spo2Percent.divide(fio2Fraction, MATH_CONTEXT)
        } else {
            null
        }

        if (inputs.fio2Percent == null) missing += "FiO₂"
        if (inputs.pao2MmHg == null) missing += "PaO₂"
        if (inputs.spo2Percent == null) missing += "SpO₂"
        if (inputs.spo2Percent != null && inputs.spo2Percent > MAX_SF_SPO2) {
            notes += "SpO₂ >97 %: S/F no se usa según la condición de Phoenix."
        }
        if (pfRatio == null && sfRatio == null) {
            missing += "Ningún cociente respiratorio válido disponible"
        }

        val points = listOfNotNull(
            pfRatio?.let {
                respiratoryPoints(
                    ratio = it,
                    support = inputs.respiratorySupport,
                    anySupportLimit = BigDecimal("400"),
                    twoPointUpperLimit = BigDecimal("200"),
                    threePointLimit = BigDecimal("100"),
                )
            },
            sfRatio?.let {
                respiratoryPoints(
                    ratio = it,
                    support = inputs.respiratorySupport,
                    anySupportLimit = BigDecimal("292"),
                    twoPointUpperLimit = BigDecimal("220"),
                    threePointLimit = BigDecimal("148"),
                )
            },
        ).maxOrNull() ?: 0

        return PhoenixDomainScore(
            points = points,
            missingInputs = missing.distinct(),
            notes = notes,
        )
    }

    private fun respiratoryPoints(
        ratio: BigDecimal,
        support: PhoenixRespiratorySupport,
        anySupportLimit: BigDecimal,
        twoPointUpperLimit: BigDecimal,
        threePointLimit: BigDecimal,
    ): Int {
        if (support == PhoenixRespiratorySupport.NONE) return 0
        if (support == PhoenixRespiratorySupport.INVASIVE_MECHANICAL_VENTILATION) {
            if (ratio < threePointLimit) return 3
            if (ratio <= twoPointUpperLimit) return 2
        }
        return if (ratio < anySupportLimit) 1 else 0
    }

    private fun scoreCardiovascular(inputs: PhoenixScoreInputs): PhoenixDomainScore {
        val missing = mutableListOf<String>()
        val vasoactivePoints = inputs.vasoactiveMedicationCount?.let { count ->
            when {
                count == 0 -> 0
                count == 1 -> 1
                else -> 2
            }
        } ?: 0.also { missing += "Vasoactivos sistémicos" }

        val lactatePoints = inputs.lactateMmolL?.let { lactate ->
            when {
                lactate >= BigDecimal("11") -> 2
                lactate >= BigDecimal("5") -> 1
                else -> 0
            }
        } ?: 0.also { missing += "Lactato" }

        val mapPoints = inputs.meanArterialPressureMmHg?.let { map ->
            mapPointsForAge(inputs.ageMonths, map)
        } ?: 0.also { missing += "Presión arterial media" }

        return PhoenixDomainScore(
            points = (vasoactivePoints + lactatePoints + mapPoints).coerceAtMost(6),
            missingInputs = missing,
        )
    }

    private fun mapPointsForAge(ageMonths: Int, map: BigDecimal): Int {
        val (onePointFrom, zeroPointFrom) = when {
            ageMonths < 12 -> BigDecimal("25") to BigDecimal("39")
            ageMonths < 24 -> BigDecimal("31") to BigDecimal("44")
            ageMonths < 60 -> BigDecimal("32") to BigDecimal("45")
            ageMonths < 144 -> BigDecimal("36") to BigDecimal("49")
            else -> BigDecimal("38") to BigDecimal("52")
        }
        return when {
            map < onePointFrom -> 2
            map < zeroPointFrom -> 1
            else -> 0
        }
    }

    private fun scoreCoagulation(inputs: PhoenixScoreInputs): PhoenixDomainScore {
        val missing = mutableListOf<String>()
        var points = 0

        val platelets = inputs.plateletsThousandPerMicroliter
        if (platelets == null) missing += "Plaquetas" else if (platelets < BigDecimal("100")) points++

        val inr = inputs.inr
        if (inr == null) missing += "INR" else if (inr > BigDecimal("1.3")) points++

        val dDimer = inputs.dDimerMgPerLiterFeu
        if (dDimer == null) missing += "D-dímero (FEU)" else if (dDimer > BigDecimal("2")) points++

        val fibrinogen = inputs.fibrinogenMgPerDl
        if (fibrinogen == null) missing += "Fibrinógeno" else if (fibrinogen < BigDecimal("100")) points++

        return PhoenixDomainScore(
            points = points.coerceAtMost(2),
            missingInputs = missing,
        )
    }

    private fun scoreNeurologic(inputs: PhoenixScoreInputs): PhoenixDomainScore {
        val missing = mutableListOf<String>()
        val gcs = inputs.glasgowComaScale
        val fixedPupils = inputs.bilaterallyFixedPupils
        if (gcs == null) missing += "Glasgow (GCS)"
        if (fixedPupils == null) missing += "Pupilas bilateralmente fijas"

        val points = when {
            fixedPupils == true -> 2
            gcs != null && gcs <= 10 -> 1
            else -> 0
        }
        return PhoenixDomainScore(points = points.coerceAtMost(2), missingInputs = missing)
    }
}
