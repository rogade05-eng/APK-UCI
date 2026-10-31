package org.uciasistant.feature.calculators

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

internal data class OxygenationInputs(
    val fio2Percent: BigDecimal,
    val pao2MmHg: BigDecimal? = null,
    val spo2Percent: BigDecimal? = null,
    val meanAirwayPressureCmH2O: BigDecimal? = null,
)

internal data class OxygenationResults(
    val pfRatio: BigDecimal?,
    val sfRatio: BigDecimal?,
    val oxygenationIndex: BigDecimal?,
    val oxygenSaturationIndex: BigDecimal?,
    val saturationExceedsSfLimit: Boolean,
)

/** Source-based arithmetic indices only; this does not classify PARDS or recommend treatment. */
internal object OxygenationIndices {
    private val ZERO = BigDecimal.ZERO
    private val MIN_FIO2_PERCENT = BigDecimal("21")
    private val ONE_HUNDRED = BigDecimal("100")
    private val MAX_SF_SPO2 = BigDecimal("97")
    private val PRECISION = MathContext.DECIMAL128

    fun calculate(inputs: OxygenationInputs): OxygenationResults {
        require(inputs.fio2Percent >= MIN_FIO2_PERCENT && inputs.fio2Percent <= ONE_HUNDRED) {
            "Introduce FiO₂ en porcentaje (21–100); por ejemplo, 40 para 0,40."
        }
        require(inputs.pao2MmHg == null || inputs.pao2MmHg > ZERO) {
            "PaO₂ debe ser mayor que 0 mmHg."
        }
        require(inputs.spo2Percent == null || inputs.spo2Percent > ZERO && inputs.spo2Percent <= ONE_HUNDRED) {
            "SpO₂ debe ser mayor que 0 y no superar 100 %."
        }
        require(
            inputs.meanAirwayPressureCmH2O == null || inputs.meanAirwayPressureCmH2O > ZERO,
        ) {
            "La presión media de vía aérea debe ser mayor que 0 cmH₂O."
        }
        require(inputs.pao2MmHg != null || inputs.spo2Percent != null) {
            "Introduce PaO₂ o SpO₂ para calcular un índice."
        }

        val fio2Fraction = inputs.fio2Percent.divide(ONE_HUNDRED)
        val pfRatio = inputs.pao2MmHg?.divide(fio2Fraction, PRECISION)
        val saturationExceedsSfLimit = inputs.spo2Percent?.let { it > MAX_SF_SPO2 } ?: false
        val sfRatio = inputs.spo2Percent
            ?.takeUnless { saturationExceedsSfLimit }
            ?.divide(fio2Fraction, PRECISION)

        val oxygenationIndex = if (
            inputs.pao2MmHg != null && inputs.meanAirwayPressureCmH2O != null
        ) {
            fio2Fraction
                .multiply(inputs.meanAirwayPressureCmH2O)
                .multiply(ONE_HUNDRED)
                .divide(inputs.pao2MmHg, PRECISION)
        } else {
            null
        }

        val oxygenSaturationIndex = if (
            inputs.spo2Percent != null &&
            !saturationExceedsSfLimit &&
            inputs.meanAirwayPressureCmH2O != null
        ) {
            fio2Fraction
                .multiply(inputs.meanAirwayPressureCmH2O)
                .multiply(ONE_HUNDRED)
                .divide(inputs.spo2Percent, PRECISION)
        } else {
            null
        }

        return OxygenationResults(
            pfRatio = pfRatio,
            sfRatio = sfRatio,
            oxygenationIndex = oxygenationIndex,
            oxygenSaturationIndex = oxygenSaturationIndex,
            saturationExceedsSfLimit = saturationExceedsSfLimit,
        )
    }

    fun parse(raw: String): BigDecimal = UnitConversionEngine.parseInput(raw)

    fun format(value: BigDecimal): String = value
        .setScale(2, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
        .replace('.', ',')
}
