# Adenda al contrato técnico de UCI Assistant

- **Versión:** 1.1
- **Fecha:** 30 de octubre de 2026
- **Estado:** cambios de alcance adoptados para el diseño; sujetos a aprobación clínica antes de habilitar contenido asistencial.
- **Documento base:** contrato técnico v1.0 proporcionado en la conversación.

## 1. Objeto y aplicación

Esta adenda incorpora al contrato v1.0 los cambios derivados de la revisión del *Programa de Especialización en Medicina Intensiva y Emergencias* del MINSAP/CNCI. Complementa el contrato; no reemplaza sus requisitos originales ni autoriza la utilización clínica de contenidos no validados.

El programa revisado es una referencia **curricular** para identificar dominios de trabajo. No es una guía clínica. La copia consultada enumera bibliografía de 1992–1997 y no permite establecer con certeza, solo a partir de su portada, la fecha formal de aprobación. Por tanto:

- No se copiarán de ese programa dosis, umbrales, algoritmos, escalas, tratamientos ni criterios de ingreso como reglas clínicas vigentes.
- Todo contenido clínico ejecutable o presentado como vigente tendrá fuentes clínicas apropiadas, derechos verificados, versión y aprobación independiente según población.
- Se podrá citar el documento y utilizarlo como índice temático; no se reproducirán textos extensos sin autorización.
- El alcance adulto/pediátrico y la mención de reanimación neonatal del programa no se considerarán validación de una biblioteca integral de cuidados intensivos neonatales.

## 2. Decisiones de producto incorporadas

**D-01 — Distribución general con configuración local.** El núcleo de UCI Assistant será común y no codificará criterios de ingreso, formularios, legislación ni protocolos de un país u hospital como reglas universales. Cada centro podrá configurar paquetes locales versionados y aprobados.

**D-02 — Offline por defecto.** La instalación local debe cubrir las funciones clínicas habilitadas sin conexión. El modo inicial será de almacenamiento local por dispositivo. La colaboración entre dispositivos queda condicionada a un servidor privado, identidad, permisos, cifrado, auditoría y resolución de conflictos validados; nunca se simulará sincronización mediante una carpeta compartida.

**D-03 — Aislamiento de población.** Adultos, pediatría y neonatología son dominios clínicos independientes. No se compartirán fórmulas, umbrales ni interpretaciones por conveniencia de implementación.

**D-04 — Alcance del programa fuente.** El PDF sirve para ampliar el mapa funcional y la biblioteca temática, no para aprobar contenido clínico.

**D-05 — Configuración institucional, no decisión automática.** La aplicación documenta y presenta información. No determina automáticamente admisión, prioridad, destino, diagnóstico, tratamiento o limitación de soporte.

## 3. Requisitos incorporados

### 3.1. Ingreso, traslado y continuidad asistencial — `PAT-CONT-01`

Cada ingreso deberá permitir documentar, cuando la información exista y la normativa lo permita:

- origen, institución/servicio remitente y tipo de ingreso;
- motivo y prioridad local de recepción, vinculados a la versión del criterio institucional utilizado;
- fechas/horas disponibles de derivación, salida, recepción y entrega clínica;
- unidad o servicio de destino y responsable que entrega/recibe;
- resumen estructurado de traspaso: problemas activos, alergias, intervenciones en curso, dispositivos, resultados pendientes y tareas por completar;
- eventos de traslado, cambios de estado, correcciones y autoría.

Los códigos de prioridad y criterios de ingreso/egreso se cargarán desde configuración local versionada. No habrá una escala universal precargada por este contrato. Una transferencia no sobrescribirá silenciosamente el ingreso ni sus notas anteriores.

### 3.2. Procedencia y calidad de la monitorización — `MON-DATA-01`

Además del parámetro, valor y unidad, cada observación deberá conservar como mínimo:

- instante de medición (`observedAt`) y de registro (`recordedAt`);
- usuario que la registra y origen del dato;
- unidad explícita y método/sitio cuando apliquen;
- clasificación invasiva, no invasiva, no aplicable o desconocida cuando sea pertinente;
- condición del dato: observado, estimado, corregido, incompleto o cuestionado;
- vínculo con ingreso y dispositivo, si corresponde;
- referencia al registro anterior cuando se trate de una corrección.

En la primera versión los datos se introducirán manualmente o se transcribirán. La integración automática con monitores sigue excluida. Una conversión de unidades solo se realizará si está definida y probada explícitamente; gramos no se interpretarán como kilogramos ni se aplicarán conversiones implícitas para producir un resultado clínico.

### 3.3. Comunicación, representante y documentación de preferencias — `CARE-HUM-01`

El expediente podrá documentar, con control de acceso y auditoría, el contacto o representante autorizado, comunicaciones clínicamente relevantes y constancias de consentimiento, preferencias u objetivos/limitaciones de atención cuando proceda.

La institución y jurisdicción determinarán los campos obligatorios y documentos aplicables. Esta función no sustituye la conversación clínica, la evaluación de capacidad, los formularios oficiales, las obligaciones legales ni la decisión del equipo tratante. No generará órdenes ni inferirá una decisión de limitación terapéutica a partir de texto libre.

Se aplicará minimización de datos: no se recopilarán detalles personales de familiares que no sean necesarios para la atención o para una obligación documentada.

### 3.4. Taxonomía ampliada de protocolos — `LIB-DOM-01`

La biblioteca clínica podrá organizarse, como mínimo, en estos dominios, siempre con fichas de población y aprobación propias:

- apoyo vital y emergencias;
- trauma, estabilización y traslado;
- vía aérea, oxigenoterapia y ventilación;
- monitorización y soporte cardiocirculatorio;
- afecciones respiratorias, cardiovasculares, digestivas, hematológicas y endocrinas;
- infección y sepsis;
- neurología, función renal y alteraciones hidroelectrolíticas;
- situaciones especiales de cuidados intensivos.

Esta lista amplía el mapa de contenidos, pero no obliga a implementar todos los protocolos o calculadoras en la primera APK. Los protocolos de emergencia se priorizarán solo cuando exista una referencia vigente y aprobación independiente. La lista no autoriza recomendaciones terapéuticas automáticas.

### 3.5. Indicadores de calidad — `QI-DEF-01`

Los indicadores institucionales serán descriptivos y configurables. Cada definición incluirá identificador, propósito, población, numerador, denominador, unidad, periodo, exclusiones, fuente de datos, responsable, versión y fecha de vigencia. La interfaz señalará datos faltantes o incompletos.

No se compararán servicios o periodos con definiciones incompatibles ni se presentarán indicadores descriptivos como pronósticos individuales. La aplicación no establecerá estándares de desempeño clínico sin una metodología y aprobación explícitas.

### 3.6. Docencia e investigación — `EDU-RES-01`

La biblioteca educativa y las funciones de investigación son **posteriores y opcionales**, no bloqueantes para la APK inicial. El material educativo identificará audiencia, objetivos, población, fuentes, autoría, versión y vigencia.

Una exportación para investigación requerirá propósito documentado, autorización aplicable, minimización/desidentificación, permisos, registro de auditoría y separación de los informes asistenciales. La función no debe habilitar por defecto la exportación masiva de datos identificables.

### 3.7. Gobierno y ciclo de vida del contenido — `CONTENT-GOV-01`

Toda escala, algoritmo, protocolo, indicador o ficha clínica tendrá metadatos que permitan identificar:

- identificador estable, tipo, población, jurisdicción e institución aplicables;
- título, autor/propietario clínico, revisor competente y aprobador;
- estado: `DRAFT`, `IN_REVIEW`, `APPROVED`, `ACTIVE`, `SUSPENDED` o `SUPERSEDED`;
- versión del contenido y de la fuente, referencias, licencia/derechos y fecha de consulta;
- fecha de aprobación, entrada en vigor, última revisión y próxima revisión;
- historial de cambios y motivo de modificación o retirada;
- evidencia y casos de prueba de validación para los algoritmos.

Solo contenido aprobado, vigente y compatible con la población y jurisdicción seleccionadas podrá presentarse como vigente. Contenido suspendido, obsoleto, vencido o incompleto se bloqueará o identificará claramente como no utilizable para decisiones clínicas. No se actualizarán algoritmos ni protocolos en silencio: cada cálculo conservará la versión de algoritmo y referencia usada.

La revisión de adultos, pediatría y neonatología será independiente y deberá tener responsables clínicos identificados. Para contenido farmacológico se requerirá además revisión por profesionales con competencia farmacológica, según el alcance y jurisdicción.

### 3.8. Selección y cambio de población — `POP-LOCK-01`

La población aplicable deberá estar explícita en el paciente, el ingreso y cada cálculo/contenido clínico pertinente. No se inferirá ni cambiará silenciosamente por edad, peso, unidades o resultado.

Una corrección del perfil requerirá acción explícita autorizada, motivo, sello de tiempo y auditoría. Los cálculos previos conservarán sus datos y versión; no se recalcularán ni sustituirán automáticamente. Si la población de un algoritmo no coincide con la del caso, el cálculo devolverá un estado no válido/no aplicable y no un valor clínico aparentemente válido.

Neonatología deberá tener fuentes, validadores, casos de prueba y responsable clínico propios. El programa formativo revisado no puede emplearse como validación neonatal.

### 3.9. Configuración local y paquetes offline — `LOCAL-PACK-01`

Los protocolos, criterios de ingreso, formularios e indicadores locales se almacenarán en paquetes con identificador institucional, jurisdicción, versión, fecha de vigencia y estado de aprobación. Se conservará la versión que produjo un cálculo o informe. Un paquete inválido, incompleto o dirigido a otra población no se activará.

La consulta de contenido vigente no dependerá de una conexión externa durante la atención. Las actualizaciones serán explícitas, auditables y compatibles con el funcionamiento offline definido en el contrato base.

## 4. Priorización de entrega

### Base de la primera versión funcional

1. Expedientes e ingresos con población explícita y trazabilidad.
2. Evolución, historial y correcciones no destructivas.
3. Transferencia y entrega estructurada de paciente.
4. Monitorización con unidades, autoría, hora y procedencia del dato.
5. Modelo de datos para comunicación/representación y permisos correspondientes.
6. Gobierno de contenido y aislamiento poblacional desde el diseño.
7. Contenido offline inicial limitado a elementos aprobados.
8. Cálculos solo cuando tengan ficha clínica, fuentes y pruebas aprobadas. Un cálculo no validado permanecerá deshabilitado.

### Iteraciones posteriores

- Biblioteca ampliada de urgencias, trauma y afecciones por sistemas.
- Indicadores locales de calidad y reportes de gestión.
- Biblioteca educativa y exportación de investigación con controles específicos.
- Sincronización institucional después de definir y validar infraestructura privada.

La arquitectura puede prever estos módulos, pero no se considerarán funcionalidad entregada hasta que existan implementación, pruebas y aprobación correspondientes.

## 5. Criterios de aceptación añadidos

Se agregan a T01–T14 del contrato base:

| Código | Prueba | Condición de aceptación |
|---|---|---|
| T15 | Continuidad y traspaso | Un traslado conserva origen/destino, responsables, tiempos y resumen de entrega; las correcciones no eliminan el historial |
| T16 | Procedencia de observación | Distingue datos manuales, transcritos y derivados, conserva unidad/autor/hora y permite marcar estimado o cuestionado |
| T17 | Estado del contenido | Contenido suspendido, obsoleto, vencido o no aprobado no se presenta como vigente; cambios quedan auditados |
| T18 | Población incompatible | Un cálculo de población incorrecta se rechaza y no convierte gramos/kg o edades de forma tácita |
| T19 | Configuración institucional | Un criterio/protocolo local muestra institución, jurisdicción, versión y vigencia; no altera el motor común ni otra población |
| T20 | Funcionamiento offline | Expediente, historial y contenido activo instalado pueden consultarse sin Internet; no hay pérdida de cambios locales |
| T21 | Privacidad de investigación | La exportación de investigación está separada, autorizada, minimizada y auditada; no incluye identificadores por defecto |
| T22 | Corrección histórica | Corregir una medición o cambiar la población deja el valor previo, motivo, autor y hora accesibles en auditoría |
| T23 | Límite de fuente neonatal | Ningún algoritmo neonatal se activa solo por citar el programa curricular revisado; requiere fuente y validación neonatal propias |

## 6. Responsabilidades de aprobación

Antes de activar cualquier elemento clínico en una versión distribuible deben quedar registrados:

1. propietario clínico y población objetivo;
2. fuente clínica y derechos de uso;
3. revisor independiente adecuado a esa población;
4. responsable que aprueba y fecha de aprobación;
5. casos de prueba, resultados esperados y versión del algoritmo/contenido;
6. limitaciones conocidas y fecha de próxima revisión.

La revisión documental del programa curricular no satisface ninguno de estos criterios por sí sola. La presente adenda tampoco constituye certificación regulatoria ni validación clínica.

## 7. Trazabilidad de cambios

Esta adenda incorpora en forma normativa las recomendaciones del documento [Evaluación del programa de especialización](evaluacion-programa-especializacion-uci.md). Las decisiones que impliquen cambios de uso previsto, requisitos regulatorios o sincronización entre centros deberán tramitarse como una nueva revisión del contrato y conservar su historial.
