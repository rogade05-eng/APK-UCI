# Evaluación del programa de especialización frente al contrato de UCI Assistant

- **Fecha de revisión:** 30 de octubre de 2026
- **Tipo de revisión:** análisis de alcance y requisitos; no es validación clínica ni regulatoria.
- **Seguimiento:** las recomendaciones se incorporaron como requisitos en la [adenda al contrato técnico v1.1](adenda-contrato-tecnico-v1.1.md).

## 1. Fuente y límites de esta evaluación

Se revisó el documento titulado **Programa de Especialización en Medicina Intensiva y Emergencias**, atribuido en su portada al Ministerio de Salud Pública de Cuba (MINSAP), Área de Docencia e Investigaciones y Comisión Nacional de Cuidados Intensivos:

[Consultar el PDF original](https://files.sld.cu/urgencia/files/2010/08/programa-de-la-especialidad-en-cuidados-intensivos-y-emergencias-para-medicos.pdf)

El PDF es un **programa curricular de posgrado**, no una guía clínica ni un catálogo de algoritmos validado para una aplicación. La ruta del archivo contiene `2010/08`, pero la portada consultada no permite confirmar por sí sola la fecha formal de aprobación o publicación. La bibliografía que el propio documento enumera incluye, entre otras, referencias de 1992, 1994, 1995, 1996 y 1997; por ello no debe asumirse que sus contenidos terapéuticos reflejan guías actuales.

Esta evaluación utiliza el documento como **mapa temático y de competencias**. No valida recomendaciones, escalas, dosis, umbrales, algoritmos de reanimación ni procedimientos. Antes de publicar contenido clínico se requieren fuentes vigentes, derechos de uso verificados y aprobación independiente de especialistas.

## 2. Conclusión ejecutiva

El programa aporta una perspectiva que complementa bien el contrato: UCI Assistant puede apoyar no solo el cálculo y el expediente, sino también la continuidad de cuidados, la atención de urgencias y trauma, la comunicación con paciente/familia, la calidad del servicio y la formación continua.

Recomiendo incorporar estos temas como **requisitos funcionales y categorías de contenido**, pero no ampliar de golpe el alcance clínico de la primera APK. Las modificaciones de mayor valor son:

1. Hacer explícitos el origen del paciente, la priorización/criterios locales de ingreso, el traslado, la recepción y la entrega estructurada del caso.
2. Extender la biblioteca de protocolos a trauma, apoyo vital, urgencias y problemas por sistemas —digestivo, hematológico y endocrino, entre otros— mediante incorporación por fases.
3. Registrar procedencia y contexto de las observaciones de monitorización, distinguiendo medición invasiva/no invasiva y dato manual/estimado.
4. Añadir funciones limitadas para relación con familiares, consentimiento, objetivos y límites de atención, siempre conforme a la legislación y políticas locales; la aplicación solo documenta, no sustituye el proceso clínico o legal.
5. Definir indicadores de calidad de forma reproducible y configurable por institución, evitando comparaciones no validadas.
6. Añadir gobierno editorial para cada protocolo, escala y algoritmo: responsable, población, jurisdicción, fuentes, aprobación, vigencia, revisión y retirada.
7. Mantener separadas las bibliotecas de adultos, pediatría y neonatología. El programa cubano describe perfiles adulto y pediátrico y menciona reanimación neonatal, pero **no valida por sí solo un catálogo integral de cuidados intensivos neonatales**.

La recomendación principal es **no tratar este PDF como autoridad clínica de implementación**. Es una buena fuente para detectar vacíos de alcance; los criterios clínicos deben provenir de referencias actuales y aprobadas para cada población y jurisdicción.

## 3. Qué aporta el programa

El documento describe una formación amplia en Medicina Intensiva y Emergencias, con actividad asistencial, docente, investigativa y gerencial. En el contenido analítico se destacan:

- Organización de la atención progresiva del paciente grave, criterios de ingreso, registros clínicos, indicadores de calidad, trabajo en equipo y relación médico-paciente-familia.
- Apoyo vital, atención prehospitalaria, trauma y reanimación para adultos y población pediátrica; incluye temas de reanimación neonatal.
- Control de vía aérea y ventilación, procedimientos respiratorios y complicaciones relacionadas.
- Monitorización invasiva y no invasiva y procedimientos del sistema cardiocirculatorio.
- Atención del paciente crítico por sistemas: respiratorio, cardiovascular, digestivo, hematológico, endocrino, infecciones/sepsis y situaciones especiales.
- Investigación en Medicina Intensiva, proceso de enseñanza-aprendizaje y gestión hospitalaria de los cuidados progresivos.
- Consideraciones éticas, legales, humanas y de interacción con la familia.

El documento también está ligado a un sistema sanitario y un contexto nacional concretos. Los nombres de servicios, normas institucionales, reglas legales y componentes administrativos locales no deben trasladarse literalmente a una APK de distribución general.

## 4. Matriz de brechas respecto del contrato

| Tema del programa | Cobertura actual en el contrato | Evaluación y ajuste recomendado | Prioridad |
|---|---|---|---|
| Continuidad desde urgencias, traslado y recepción | Se registran ingreso, egreso y traslado; no se especifica el circuito de recepción ni entrega clínica | Añadir origen, servicio remitente, motivo/prioridad local, tiempos, evaluación de recepción y lista estructurada de entrega/traspaso. Los criterios de ingreso deben ser configurables por centro, no universales | Alta |
| Apoyo vital, emergencias y trauma | Hay procedimientos y protocolos; no aparece un dominio claro de atención prehospitalaria/trauma | Crear categorías de apoyo vital, trauma, estabilización y transporte. Publicar algoritmos solo desde guías vigentes, autorizadas y aprobadas por población | Media-alta |
| Vía aérea y ventilación | Cobertura amplia | Conservar. Añadir registro de incidencias/complicaciones, oxigenoterapia y comprobaciones de equipos como listas de apoyo; nunca controlar ventiladores | Media |
| Monitorización invasiva/no invasiva | Se registran signos vitales y variables hemodinámicas; falta contexto de obtención | Guardar fuente, método, sitio, unidad, hora de medición, autor, carácter invasivo/no invasivo y marca de dato estimado. Mantener fuera de la primera versión la integración directa con equipos | Alta |
| Cobertura por sistemas | Hay sepsis, neurología, lesión renal y algunos protocolos respiratorios/cardiacos; digestivo, hematología y endocrino no quedan explícitos | Añadir estos dominios al catálogo y a la hoja de ruta; no implica entregar de inmediato calculadoras o recomendaciones para cada uno | Media |
| Ética, familia y aspectos legales | No existe un módulo explícito | Incorporar campos mínimos para representante/contacto, comunicación relevante, consentimiento y objetivos/limitaciones de atención cuando corresponda; adaptar a legislación local y no presentarlos como sustitutos de formularios legales | Alta |
| Calidad y gestión de la unidad | Se mencionan informes/estadísticas, sin definición de indicadores | Añadir catálogo de indicadores con definición, numerador/denominador, fuente, periodo, exclusiones y versión; habilitar configuración local y evitar rankings/benchmarking sin metodología compatible | Media |
| Docencia e investigación | No se especifican funciones concretas | Considerar una biblioteca educativa opcional. Cualquier exportación para investigación debe ser minimizada/desidentificada, autorizada, auditable y separada del flujo asistencial | Posterior |
| Población neonatal | El contrato la contempla como biblioteca independiente | Mantenerlo, pero exigir fuentes neonatales específicas y revisión por especialista neonatal. El programa solo aporta un apoyo curricular limitado y no permite extrapolar escalas, rangos o dosis | Crítica |
| Contexto institucional | Hay protocolos locales. Al cierre de esta evaluación, la distribución objetivo seguía abierta; el 30-oct-2026 se definió Cuba como mercado inicial, aún sin centro piloto | Separar motor común de paquetes locales versionados. No codificar el SIUM, normas cubanas ni criterios de una institución como valores universales | Alta |

## 5. Cambios propuestos al contrato

### 5.1. Ampliar el expediente y la continuidad asistencial (sección 1.2)

Añadir al registro de cada ingreso, según disponibilidad y normativa local:

- Origen del paciente y servicio remitente; tipo de ingreso (programado, urgencia, traslado u otro).
- Motivo y prioridad de recepción, con referencia a los criterios institucionales vigentes.
- Fecha/hora de salida del centro remitente, recepción y entrega clínica, cuando esos datos existan.
- Resumen estructurado de traspaso: problemas activos, intervenciones en curso, alergias, dispositivos, pendientes y responsable que entrega/recibe.
- Trazabilidad de cambios de estado, transferencias y correcciones del expediente.

Los criterios de ingreso, traslado y egreso serán **configurables por institución y versión**. La aplicación no decidirá automáticamente admisión, prioridad ni destino.

### 5.2. Especificar mejor la monitorización (sección 1.3)

Cada observación debería admitir, además del valor y la unidad:

- fecha/hora de medición y zona horaria de presentación;
- autor y origen del dato (medición manual, equipo, cálculo o transcripción);
- método y sitio cuando sean clínicamente relevantes;
- clasificación invasiva/no invasiva cuando corresponda;
- indicador de estimado, corregido, incompleto o cuestionado;
- referencia al episodio/ingreso y al dispositivo asociado, si aplica.

Esto mejora la interpretación y auditoría sin contradecir la exclusión de integración directa con monitores en la primera versión.

### 5.3. Extender la biblioteca clínica sin convertirla en guía automática (secciones 1.10 y 1.11)

Incluir categorías de consulta para apoyo vital, urgencias, trauma, estabilización/traslado y afecciones digestivas, hematológicas y endocrinas. Para cada elemento se conservarán población, ámbito, institución, versión, vigencia, referencias, propietario clínico y estado de aprobación. El contenido se entregará offline y se podrá retirar o marcar obsoleto de forma trazable.

Los protocolos de emergencia y reanimación deben tener una fuente actual aprobada. El programa formativo puede inspirar la taxonomía, pero no debe proporcionar los algoritmos vigentes ni sus dosis.

### 5.4. Añadir un apartado transversal de ética y comunicación

Propuesta de requisito funcional:

> UCI Assistant permitirá documentar, con permisos y auditoría, los datos de contacto/representación, comunicaciones relevantes con paciente o familia y constancias clínicas de consentimiento, preferencias u objetivos de atención cuando proceda. El formato y la obligatoriedad dependerán de la institución y jurisdicción. La aplicación no reemplaza la conversación clínica, la valoración de capacidad, los formularios oficiales ni el asesoramiento legal.

No es necesario convertir esto en un módulo autónomo de decisiones al final de la vida para la primera versión; sí conviene prever el modelo de datos y los permisos para evitar improvisarlo después.

### 5.5. Definir los indicadores antes de reportarlos (sección 1.11)

Cada indicador institucional debe documentar definición, unidad, numerador, denominador, periodo, exclusiones, fuente, responsable y versión. Mostrar faltantes y calidad del dato. No comparar unidades, centros o periodos con definiciones distintas ni presentar estadísticas descriptivas como predicciones clínicas.

### 5.6. Incorporar gobierno de contenido (secciones 1.4, 1.10 y 4)

Agregar a la ficha de cada protocolo, escala, referencia y algoritmo:

- jurisdicción e institución aplicables;
- autor/propietario, revisor por especialidad y aprobador;
- estado: borrador, en revisión, aprobado, vigente, suspendido u obsoleto;
- fecha de publicación, entrada en vigor y próxima revisión;
- versión de la fuente, derechos/licencia y fecha de consulta;
- historial de cambios, motivo del cambio y compatibilidad por población;
- plan de pruebas y evidencia de validación independiente.

Un contenido en borrador, vencido o suspendido no debe mostrarse como recomendación vigente. El versionado de datos no es un sustituto de la revisión clínica.

### 5.7. Precisar el aislamiento entre poblaciones

El perfil de población se guardará en el paciente y en cada cálculo/elemento clínico pertinente. El cambio de perfil no será implícito por edad o peso: requerirá una acción explícita, autorización cuando aplique, registro de auditoría y nueva validación del contexto. Una combinación incompatible debe terminar en estado no aplicable/entrada inválida, nunca en un resultado aparentemente válido.

Para neonatología se requerirá un conjunto bibliográfico, validador, casos de prueba y responsable clínico propios. El programa revisado no es suficiente para cubrirlos.

## 6. Propuesta de prioridad por fases

### Antes de cerrar la especificación de la primera APK

- Declarar que el PDF es referencia curricular, no fuente de algoritmos.
- Definir población, institución/jurisdicción y uso previsto de cada contenido.
- Aprobar el flujo de revisión clínica y publicación/retirada de contenidos.
- Diseñar la trazabilidad de ingreso, transferencia, monitorización y cambios de población.
- Resolver cómo se configurarán protocolos institucionales y permisos.

### Primera entrega funcional

- Expedientes e ingresos de las tres poblaciones, con las barreras ya definidas.
- Evolución, monitorización con procedencia del dato, historial y auditoría.
- Transferencia y entrega estructurada de pacientes.
- Contenido clínico offline inicial y reducido, con propietario y validación documentados.
- Calculadoras únicamente cuando tengan especificación aprobada y pruebas clínicas independientes.

### Iteraciones siguientes

- Protocolos de urgencia/trauma y cobertura ampliada por sistemas.
- Indicadores locales de calidad y gestión.
- Biblioteca docente opcional y exportación de datos para investigación bajo gobernanza específica.

Esta secuencia mantiene la arquitectura amplia, pero evita que una sola entrega prometa a la vez un expediente completo, todas las poblaciones y todos los módulos clínicos sin evidencia suficiente.

## 7. Decisiones de producto que conviene cerrar

El programa estudiado está ligado a una red asistencial nacional e instituciones acreditadas. Para una APK general recomiendo:

- **Distribución:** núcleo genérico para distintos hospitales, con paquetes locales de protocolos, criterios de admisión y formularios. No incluir reglas nacionales como predeterminadas universales.
- **Uso sin servidor:** un dispositivo/instalación local como modalidad inicial. El intercambio entre dispositivos solo tras definir servidor privado, identidad, permisos, cifrado, auditoría y conciliación de conflictos.
- **Contenido:** catálogos clínicos firmados/versionados y descargables opcionalmente; la aplicación debe conservar funcionamiento offline y dejar visible la fecha de vigencia del contenido.

## 8. Elementos que no conviene trasladar literalmente

- Dosis, secuencias de reanimación, criterios terapéuticos, rangos o algoritmos presentes en bibliografía antigua o no acompañados por una ficha clínica validada.
- Criterios institucionales de ingreso, estructura del sistema sanitario o procedimientos administrativos propios del contexto cubano.
- Escalas o fórmulas no documentadas en el contrato con población, versión, unidades, derechos y validación independiente.
- Contenido político o ideológico del plan docente. Para el producto son transferibles, de forma neutral, la dignidad del paciente, el trabajo en equipo, la evaluación crítica de evidencia y la comunicación respetuosa.
- Material textual extenso del PDF sin revisar derechos de reproducción. Es preferible usarlo como índice temático, citarlo y crear contenido original aprobado.

## 9. Criterios de aceptación adicionales sugeridos

Añadir a la tabla de pruebas del contrato:

| Código | Prueba | Condición de aceptación |
|---|---|---|
| T15 | Continuidad y entrega | Un traslado conserva origen, marcas de tiempo, responsable y resumen de entrega; las correcciones mantienen historial |
| T16 | Procedencia de observaciones | Se distinguen datos manuales, transcritos, estimados e invasivos/no invasivos sin perder unidad ni autor |
| T17 | Gobierno de contenido | Un protocolo vencido, suspendido o no aprobado no aparece como vigente; cada cambio es auditable |
| T18 | Aislamiento poblacional | Un caso de prueba de otra población se rechaza; no se convierten gramos/kg ni edades de forma tácita |
| T19 | Configuración institucional | Criterios y protocolos locales muestran institución, jurisdicción, versión y vigencia, sin alterar el motor común |
| T20 | Privacidad para investigación | La exportación de investigación aplica minimización, permisos y auditoría y no se confunde con el informe asistencial |

## 10. Dictamen

**Sí conviene ampliar el contrato** con continuidad de atención, trauma/urgencias, monitorización con procedencia, aspectos humanos/éticos, indicadores de calidad y gobierno del contenido. **No conviene ampliar la primera entrega** hasta convertirla en un sistema completo de emergencias, gestión hospitalaria, docencia e investigación. Esos temas deben quedar en una hoja de ruta explícita.

La aportación más valiosa del PDF es mostrar que la Medicina Intensiva es un proceso de equipo y de continuidad asistencial, no solo un conjunto de cálculos. Su limitación principal es que se trata de un currículo contextual e históricamente antiguo, con alcance adulto/pediátrico y cobertura neonatal insuficiente para validar la biblioteca neonatal del producto.
