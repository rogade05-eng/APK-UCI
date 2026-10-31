# Puntuación Phoenix pediátrica (evaluaciones puntuales)

- **Módulo:** `:feature:calculators`
- **Población implementada:** 1–215 meses, edad posconcepcional en la evaluación ≥37 semanas y hospitalización que no sea la del nacimiento. No es módulo neonatal.
- **Salida:** puntuación de los cuatro sistemas Phoenix (respiratorio, cardiovascular, coagulación y neurológico), con total de 0–13 por evaluación.
- **No implementa:** criterios binarios de sepsis o shock, cribado, diagnóstico, pronóstico individual, recomendaciones ni tratamiento.

## Alcance temporal y datos

Cada fila corresponde a una evaluación manual en una hora indicada desde el inicio del encuentro (0–24 h). El código suma los cuatro subscores de esa misma evaluación; no suma el máximo de cada sistema tomado de momentos distintos. La pantalla muestra el máximo entre las evaluaciones que la persona haya introducido, pero no afirma que se hayan registrado todas las observaciones requeridas durante las primeras 24 horas.

La fuente primaria describe el peor **total** observado durante las primeras 24 horas; por eso la pantalla conserva cada evaluación añadida mientras permanece abierta y calcula el máximo entre esas filas. No guarda datos al salir. No registra identidad del paciente, hora de reloj ni el historial clínico.

## Componentes implementados

- **Respiratorio (0–3):** usa soporte respiratorio y P/F y/o S/F cuando las entradas permiten obtenerlos. Sin soporte, la puntuación del sistema es 0 según la especificación del instrumento. Si hay soporte no invasivo/oxígeno, el componente no supera 1; con ventilación mecánica invasiva se aplican los niveles de 2 y 3 publicados. S/F no se usa si SpO₂ >97%. Si ambos cocientes son utilizables, se conserva el mayor subscore respiratorio, no se suman entre sí.
- **Cardiovascular (0–6):** suma los componentes de número de medicamentos vasoactivos sistémicos concurrentes, lactato y MAP ajustada por grupo de edad. El conteo se limita a los seis fármacos enumerados por la fuente de implementación; la dosis no se calcula. Se solicita MAP medida, sin derivarla de presión sistólica/diastólica.
- **Coagulación (0–2):** suma hasta dos puntos entre plaquetas, INR, D-dímero en mg/L FEU y fibrinógeno.
- **Neurológico (0–2):** GCS y pupilas bilateralmente fijas, con tope de dos puntos.

Las unidades, límites, rangos de edad y comparaciones numéricas implementados están en `PhoenixScore.kt` y se prueban en `PhoenixScoreTest.kt`. No se incluye Phoenix-8 ni se incorporan sistemas adicionales.

## Datos ausentes y transparencia

La implementación de referencia del paquete Phoenix mapea entradas ausentes a cero puntos y aclara que eso no constituye imputar un valor clínico normal. Esta app sigue la convención del instrumento para la aritmética, pero marca cada entrada ausente en la pantalla y muestra el total como basado solo en los datos documentados. Por tanto, una ausencia puede reducir el total observado y nunca debe leerse como una medición normal.

Los datos ingresados no se persisten. El sistema conserva temporalmente los subscores de cada fila únicamente en memoria durante esa pantalla. No se crea una alerta a partir de un total.

## Exclusiones de población

La cohorte de desarrollo excluyó hospitalizaciones del nacimiento y niños con edad posconcepcional menor de 37 semanas. Phoenix no ajusta la edad en meses usada para el estrato de MAP por prematuridad. Para preservar la separación pediatría/neonatología del producto, esta pantalla además restringe el cálculo a 1–215 meses y solicita la edad posconcepcional de la evaluación como filtro de alcance. No debe utilizarse para menores de un mes ni como regla neonatal.

## Pruebas

Los tests cubren total máximo, límites respiratorios, techo de puntuación con soporte no invasivo, SpO₂ >97%, MAP por grupos de edad, ausencias visibles y restricciones de edad/población. El total de trece puntos se comprueba con un caso sintético. Las pruebas unitarias verifican el código, no su desempeño clínico local.

## Fuentes

1. Russell S, et al. *Development and Validation of the Phoenix Criteria for Pediatric Sepsis and Septic Shock.* JAMA. 2024. [DOI: 10.1001/jama.2024.0196](https://doi.org/10.1001/jama.2024.0196).
2. Schlapbach LJ, et al. *International Consensus Criteria for Pediatric Sepsis and Septic Shock.* JAMA. 2024. [DOI: 10.1001/jama.2024.0179](https://doi.org/10.1001/jama.2024.0179).
3. CU DBMI Pediatric Sepsis Definition Task Force. *The Phoenix Sepsis Criteria*, package implementation notes and rubric. [Project documentation](https://cu-dbmi-peds.github.io/phoenix/articles/phoenix.html).

La puntuación es un cálculo de instrumento, no un diagnóstico; esta implementación omite deliberadamente el umbral diagnóstico de sepsis y el criterio de shock.
