package org.uciasistant.feature.calculators

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

internal enum class MeasurementFamily {
    MASS,
    LENGTH,
    VOLUME,
    TEMPERATURE,
}

internal enum class MeasurementUnit(
    val label: String,
    val family: MeasurementFamily,
    /** Multiplier to the family's base unit; null for temperature units. */
    val factorToBase: BigDecimal?,
) {
    KILOGRAM("Kilogramo (kg)", MeasurementFamily.MASS, BigDecimal("1000")),
    GRAM("Gramo (g)", MeasurementFamily.MASS, BigDecimal.ONE),

    METER("Metro (m)", MeasurementFamily.LENGTH, BigDecimal.ONE),
    CENTIMETER("Centímetro (cm)", MeasurementFamily.LENGTH, BigDecimal("0.01")),
    MILLIMETER("Milímetro (mm)", MeasurementFamily.LENGTH, BigDecimal("0.001")),

    LITER("Litro (L)", MeasurementFamily.VOLUME, BigDecimal("1000")),
    MILLILITER("Mililitro (mL)", MeasurementFamily.VOLUME, BigDecimal.ONE),

    CELSIUS("Grados Celsius (°C)", MeasurementFamily.TEMPERATURE, null),
    FAHRENHEIT("Grados Fahrenheit (°F)", MeasurementFamily.TEMPERATURE, null),
    KELVIN("Kelvin (K)", MeasurementFamily.TEMPERATURE, null),
}

internal enum class MeasurementCategory(
    val label: String,
    val family: MeasurementFamily,
) {
    MASS("Masa", MeasurementFamily.MASS),
    LENGTH("Longitud", MeasurementFamily.LENGTH),
    VOLUME("Volumen", MeasurementFamily.VOLUME),
    TEMPERATURE("Temperatura", MeasurementFamily.TEMPERATURE),
    ;

    val units: List<MeasurementUnit>
        get() = MeasurementUnit.entries.filter { it.family == family }
}

/** General-purpose, offline unit arithmetic only; it contains no clinical scoring or dosing rules. */
internal object UnitConversionEngine {
    private const val MAX_INPUT_LENGTH = 40
    private val DECIMAL_INPUT = Regex("^[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)$")
    private val FIVE = BigDecimal("5")
    private val NINE = BigDecimal("9")
    private val THIRTY_TWO = BigDecimal("32")
    private val KELVIN_OFFSET = BigDecimal("273.15")

    fun parseInput(raw: String): BigDecimal {
        val trimmed = raw.trim()
        require(trimmed.isNotEmpty()) { "Introduce un valor." }
        require(trimmed.length <= MAX_INPUT_LENGTH) { "El valor introducido es demasiado largo." }
        require(!(trimmed.contains(',') && trimmed.contains('.'))) {
            "Usa un solo separador decimal; no uses separadores de miles."
        }

        val normalized = trimmed.replace(',', '.')
        require(DECIMAL_INPUT.matches(normalized)) {
            "Introduce un número decimal válido, sin separadores de miles."
        }
        return BigDecimal(normalized)
    }

    fun convert(raw: String, from: MeasurementUnit, to: MeasurementUnit): BigDecimal =
        convert(parseInput(raw), from, to)

    fun convert(value: BigDecimal, from: MeasurementUnit, to: MeasurementUnit): BigDecimal {
        require(from.family == to.family) { "Selecciona unidades de la misma magnitud." }
        if (from == to) return value

        if (from.family == MeasurementFamily.TEMPERATURE) {
            val celsius = when (from) {
                MeasurementUnit.CELSIUS -> value
                MeasurementUnit.FAHRENHEIT -> value.subtract(THIRTY_TWO)
                    .multiply(FIVE)
                    .divide(NINE, MathContext.DECIMAL128)
                MeasurementUnit.KELVIN -> value.subtract(KELVIN_OFFSET)
                else -> error("Combinación de temperatura no compatible.")
            }
            return when (to) {
                MeasurementUnit.CELSIUS -> celsius
                MeasurementUnit.FAHRENHEIT -> celsius.multiply(NINE).divide(FIVE).add(THIRTY_TWO)
                MeasurementUnit.KELVIN -> celsius.add(KELVIN_OFFSET)
                else -> error("Combinación de temperatura no compatible.")
            }
        }

        val sourceFactor = requireNotNull(from.factorToBase)
        val targetFactor = requireNotNull(to.factorToBase)
        return value.multiply(sourceFactor).divide(targetFactor)
    }

    /** Formats at most eight fractional digits, using the Spanish decimal comma. */
    fun format(value: BigDecimal): String = value
        .setScale(8, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
        .replace('.', ',')
}
