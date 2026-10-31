# Índices pediátricos de oxigenación

- **Módulo:** `:feature:calculators`
- **Población de referencia:** pediatría según PALICC-2; no es módulo neonatal.
- **Función implementada:** cálculo aritmético de P/F, S/F, OI y OSI cuando están disponibles las entradas.
- **No implementa:** clasificación de PARDS, criterios diagnósticos, umbrales de gravedad, tratamiento ni alertas.

## Especificación de entradas y ecuaciones

| Entrada | Unidad en la interfaz | Regla de entrada |
|---|---|---|
| FiO₂ | Porcentaje (21–100) | Se convierte a fracción: FiO₂% / 100. |
| PaO₂ | mmHg, opcional | Debe ser mayor que cero. |
| SpO₂ | %, opcional | Debe ser mayor que cero y no superar 100. Para S/F y OSI se requiere SpO₂ ≤97%. |
| MAP (presión media de vía aérea) | cmH₂O, opcional | Debe ser mayor que cero; se usa en OI y OSI. |

Ecuaciones:

- `P/F = PaO₂ / FiO₂(fracción)`
- `S/F = SpO₂ / FiO₂(fracción)`
- `OI = FiO₂(fracción) × MAP(cmH₂O) × 100 / PaO₂(mmHg)`
- `OSI = FiO₂(fracción) × MAP(cmH₂O) × 100 / SpO₂(%)`

Los valores opcionales no se imputan. Si faltan entradas, solo se calculan los índices que sí pueden obtenerse; no se sustituyen datos ausentes por cero ni por un valor normal. Los cocientes basados en SpO₂ se omiten si SpO₂ supera 97%, según PALICC-2. La medición de oxigenación debe ser en estado estable, no durante una desaturación transitoria.

El resultado se presenta con hasta dos decimales (`HALF_UP`). La interfaz no aplica puntos de corte ni etiqueta a la persona como caso de PARDS.

## Pruebas

Los tests JVM cubren un conjunto consistente (FiO₂ 50%, PaO₂ 80 mmHg, SpO₂ 90%, MAP 10 cmH₂O), casos con mediciones parciales, omisión de S/F y OSI cuando SpO₂ >97%, y entradas inválidas. La fórmula se verifica independientemente del UI.

## Fuente

Emeriaud G, et al. *Executive Summary of the Second International Guidelines for the Diagnosis and Management of Pediatric Acute Respiratory Distress Syndrome (PALICC-2).* Pediatric Critical Care Medicine. 2023;24(2):143–168. [DOI: 10.1097/PCC.0000000000003147](https://doi.org/10.1097/PCC.0000000000003147).

PALICC-2 especifica las ecuaciones y condiciones de medición. Este módulo devuelve índices aritméticos solamente; no codifica la clasificación o manejo de PALICC-2.
