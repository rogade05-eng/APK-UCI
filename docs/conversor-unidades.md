# Conversor de unidades — especificación técnica

- **Estado:** herramienta matemática general implementada; no es calculadora clínica.
- **Población/datos:** no solicita ni guarda datos de pacientes.
- **Alcance:** conversión de masa, longitud, volumen y temperatura; funciona localmente sin red.
- **Exclusiones:** no calcula dosis, concentración, velocidad de infusión, puntuaciones, umbrales ni recomendaciones. No valida entradas clínicas y no debe usarse para tomar decisiones asistenciales.

## Unidades y definiciones

| Magnitud | Unidades | Base interna | Conversión |
|---|---|---|---|
| Masa | kg, g | g | 1 kg = 1000 g |
| Longitud | m, cm, mm | m | 1 m = 100 cm = 1000 mm |
| Volumen | L, mL | mL | 1 L = 1000 mL |
| Temperatura | °C, °F, K | °C | °F = (°C × 9/5) + 32; K = °C + 273,15 |

Las conversiones de las tres magnitudes decimales se calculan con `BigDecimal`. Para °F a °C se usa división decimal de precisión alta; la presentación redondea a un máximo de ocho decimales (`HALF_UP`) y elimina ceros finales. El redondeo solo afecta la representación mostrada.

La entrada acepta coma o punto como separador decimal, no ambos a la vez; no acepta separadores de miles, notación científica, `NaN` ni infinito. El resultado no se persiste.

## Verificación de software

Pruebas JVM cubren conversión kg↔g, cm→m, mL→L, °C↔°F, °C↔K, misma unidad, magnitudes incompatibles, entrada decimal española y formatos numéricos inválidos o demasiado largos. La CI ejecuta estas pruebas antes de ensamblar la APK.

Estas pruebas verifican implementación aritmética, no seguridad clínica ni adecuación para pacientes. La conversión de unidades no reemplaza la confirmación de unidades en el equipo o sistema institucional.

## Fuentes técnicas

- NIST, [Metric (SI) Prefixes](https://www.nist.gov/pml/owm/metric-si-prefixes): factores de prefijos decimales.
- NIST, [SI Units — Temperature](https://www.nist.gov/pml/owm/si-units-temperature): relaciones Celsius/Fahrenheit/Kelvin.
