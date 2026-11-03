# Investigación y propuesta de resolución de decisiones del producto

- **Fecha de corte:** 3 de noviembre de 2026
- **Objetivo:** traducir los 12 temas pendientes en propuestas concretas y criterios verificables de “OK”.
- **Alcance:** investigación de requisitos; no es dictamen jurídico, clasificación regulatoria, aprobación clínica ni autorización para usar datos reales.

## Resumen ejecutivo

La investigación permite fijar varias reglas de diseño, pero no permite que el equipo de desarrollo sustituya decisiones del hospital, del MINSAP o del CECMED:

1. **Alcance de producto ya acordado:** Cuba; pediatría primero; UCI y urgencias pediátricas; profesionales/residentes; offline; sincronización opcional; sin IA, diagnóstico autónomo, prescripción automática ni control directo de equipos. UCI Assistant complementa, no sustituye, la historia clínica oficial.
2. **Punto regulatorio de alto impacto:** CECMED publicó en 2025 la Regulación E 130-25 para inscripción de fabricantes y registro sanitario del software de uso médico. Define SaMD de forma que puede incluir aplicaciones móviles con finalidad médica; si UCI Assistant cae dentro de esa definición, la regulación exige una ruta de calidad, riesgos, pruebas, ciberseguridad y validación clínica. La clase depende del uso previsto y del impacto posible. No se debe autodeclarar “no dispositivo médico” por ser offline, no usar IA o mostrar un descargo. Se necesita preconsulta/clasificación de CECMED por función; esto **no bloquea seguir desarrollando y probando el prototipo**. [CECMED E 130-25](https://www.cecmed.cu/sites/default/files/adjuntos/ambitor/AR%20No.%2000-510.pdf) · [CECMED E 117-23, regla 11](https://www.cecmed.cu/sites/default/files/adjuntos/Reglamentacion/Res.No_.28.Regulaci%C3%B3n%20E%20117-23%20Reglas%20de%20clasificaci%C3%B3n%20por%20riesgo%20para%20equipos%20y%20dispositivos%20m%C3%A9dicos_0.pdf).
3. **Punto de datos personales:** en Cuba, los datos de salud están protegidos como datos sensibles. La Ley 149/2022 y la Resolución 58/2022 exigen definir finalidad, información al titular, seguridad, conservación y derechos; la resolución también contempla respaldo y limita dónde se hospedan o replican bases de datos personales. “Guardarlo solo en el teléfono” no equivale por sí mismo a autorización ni cumplimiento. [Gaceta Oficial No. 90/2022, Ley 149 y Resolución 58](https://www.minjus.gob.cu/sites/default/files/archivos/publicacion/2022-08/goc-2022-o90_0_0.pdf).
4. **Decisión clínica:** la OMS recomienda el apoyo digital a trabajadores de salud para tareas que están dentro de su ámbito profesional y destaca la validez de la información subyacente, la privacidad y la capacidad del sistema para apoyar la función. No suministra umbrales o protocolos para inventar. [OMS, recomendaciones sobre intervenciones digitales, 2019](https://www.who.int/publications/i/item/9789241550505).
5. **Interoperabilidad:** HL7 FHIR es una norma para intercambiar información, no una integración automática ni una autorización. Hay que elegir versión, perfiles y mapeos con el hospital concreto. [HL7 FHIR: visión general](https://www.hl7.org/fhir/overview.html).
6. **Seguridad Android:** Keystore dificulta extraer claves y puede imponer restricciones de uso; no autentica al profesional, no define roles, no sustituye auditoría y no garantiza que todas las claves estén respaldadas por hardware. [Android Keystore](https://developer.android.com/privacy-and-security/keystore).

## Decisiones y criterios de “OK”

### 1. Uso previsto y alcance de cada función

**Investigación y propuesta.** Mantener las decisiones ya adoptadas en el README y la adenda técnica. No describir toda la aplicación con una sola frase regulatoria: las funciones de consulta, registro, cálculos y alertas pueden tener finalidades y riesgos distintos. Para cada una, documentar quién la usa, dónde, para qué, qué datos recibe, qué muestra, qué acción humana se espera y qué no hace. CECMED clasifica el software según su finalidad y consecuencias posibles, no solo por la tecnología empleada.

**OK de alcance.** Existe una ficha por función con: identificador/nombre; usuario; entorno; población; finalidad; entradas; salida; decisión/acción humana esperada; autonomía; exclusiones; consecuencias previsibles de un error; y responsable del producto. La ficha se usa para solicitar criterio regulatorio, no para declarar una exención.

**Puede hacerse durante el desarrollo.** Implementar pantallas, modelos, almacenamiento sintético y pruebas sin declarar la función lista para decisiones clínicas.

### 2. Dominios pediátricos y límites de población

**Investigación y propuesta.** La adenda v1.1 ya propone un mapa amplio: apoyo vital/emergencias; trauma y traslado; vía aérea/oxigenación/ventilación; monitorización y soporte cardiocirculatorio; sistemas respiratorio, cardiovascular, digestivo, hematológico, endocrino, infeccioso/sepsis, neurológico, renal/electrolítico y situaciones especiales de UCI. Se puede desarrollar esa estructura por módulos, sin recortar la meta pediátrica, y habilitar cada tema solo cuando tenga su ficha y contenido listo. La primera etapa sigue siendo pediátrica; adultos y neonatología permanecen como bibliotecas distintas y fuera del contenido clínico inicial. No conviene imponer un único corte de edad a todas las escalas: cada fuente/algoritmo define su población y exclusiones.

**OK de alcance poblacional.** Para cada contenido se registra el grupo (pediátrico, adulto o neonatal), edades incluidas/excluidas y, cuando aplique, edad gestacional o corregida. Los límites proceden de la fuente aplicable o de una política clínica local documentada; no se infieren por peso ni se copian de otra población. Se rechaza o marca “no aplicable” cualquier cálculo fuera de su población.

### 3. Usuarios, identidad y permisos

**Investigación y propuesta.** Los usuarios previstos ya incluyen equipo clínico de UCI/urgencias pediátricas y residentes. Como borrador de separación de funciones, conviene distinguir: (a) usuario asistencial autorizado; (b) autor/editor de contenido; (c) revisor clínico independiente por población; (d) aprobador/publicador institucional; y (e) administrador técnico. Estos son roles de diseño propuestos, no asignaciones ya aprobadas. Autor y revisor/aprobador no deben confundirse para el mismo contenido. La identidad clínica debe provenir del mecanismo que el centro autorice; un texto libre `authorId` no verifica identidad.

**OK de permisos.** El centro entrega una matriz rol × acción (leer, crear, corregir, aprobar, activar, exportar y administrar usuarios) y define cómo autentica usuarios con y sin red. Cada evento atribuible tiene identidad, fecha, acción y objeto; el contenido clínico no se puede autoaprobar. La matriz queda aprobada por el responsable clínico y de seguridad de la institución.

### 4. Protocolos, procedimientos y fuentes

**Investigación y propuesta.** El catálogo actual es solo bibliográfico: contiene referencias candidatas y temas, no protocolos aprobados ni instrucciones asistenciales. El documento del catálogo registra ocho referencias con derechos `NOT_ASSESSED` y una referencia OMS con licencia identificada pero compatibilidad de distribución pendiente. La OMS declara CC BY-NC-SA 3.0 IGO para publicaciones; eso no resuelve por sí solo el uso comercial, las adaptaciones ni derechos de terceros. Las restricciones de algoritmos ECC de AHA también se tratan por separado en la investigación bibliográfica existente. No copiar pasos de libros o guías solo porque estén disponibles en línea.

**OK para un elemento de contenido.** Su ficha incluye fuente primaria exacta y edición; población/entorno; jurisdicción; finalidad; titular y permiso para reproducción, traducción y distribución offline; propietario clínico; revisor independiente; aprobador institucional; versión; fecha de vigencia/revisión; historial de cambios; plan de retirada; y pruebas vinculadas. El contenido se muestra como vigente solo después de esos pasos. Hasta entonces, mantenerlo como metadato candidato o borrador claramente inactivo.

### 5. Calculadoras y escalas

**Investigación y propuesta.** El prototipo ya contiene conversión general de unidades, índices pediátricos P/F, S/F, OI y OSI basados en PALICC-2, y la puntuación pediátrica Phoenix por dominios. Están descritos como cálculos, sin diagnóstico automático ni recomendaciones. La investigación bibliográfica ya distingue guías, definiciones, criterios, escalas y modelos pronósticos, y advierte que una referencia o una prueba unitaria no valida un uso clínico.

**OK técnico de cálculo.** Para cada cálculo: fuente/versión; población/edad; fórmula fiel a la fuente; variables y unidades; conversiones autorizadas; precisión/redondeo; ventanas; manejo de datos ausentes, incoherentes o fuera de rango; texto de interpretación permitido; y casos de prueba independientes con entrada y resultado esperado. No usar una fórmula/rango de otra población.

**OK clínico/regulatorio.** Además del OK matemático, definir uso previsto, limitaciones visibles, evaluación de riesgo y la validación clínica que CECMED determine si la función resulta SaMD. El CI exitoso del prototipo acredita pruebas de software, no validación clínica.

### 6. Alertas individualizadas

**Investigación y propuesta.** El motor clínico está inactivo; los recordatorios existentes son manuales, genéricos, no urgentes y sin relación con pacientes. Mantenerlo así hasta definir cada alerta. La OMS limita el apoyo digital a tareas dentro del ámbito del trabajador y resalta la validez de la lógica clínica y las condiciones de privacidad. Las alertas y protocolos son funciones separadas: un aviso podrá enlazar a un protocolo versionado, pero no sustituirlo ni crear una orden.

**OK de cada alerta.** Se documentan fuente y versión; criterio exacto; datos y ventana temporal requeridos; población/exclusiones; conducta ante datos ausentes o desactualizados; prioridad y texto; condiciones de repetición/silencio/acuse; acción humana esperada; versión de protocolo enlazada; responsable y conjunto de pruebas (incluidos falsos positivos previsibles y fallos de datos). Sin criterio proveniente de una fuente adoptada y aprobación local, queda inactiva.

### 7. Datos del paciente y flujos de trabajo

**Investigación y propuesta.** El modelo `ClinicalObservationRecord` ya conserva origen, unidad, tiempos, autor declarado, condición y vínculo de corrección; el ledger cifra observaciones sintéticas. Sin embargo, no hay pantalla del ledger, expediente longitudinal ni integración con el traspaso. El traspaso guarda snapshots locales cifrados y no es historia clínica oficial. La Ley 149 limita la recogida a lo relevante y necesario, exige finalidad concreta e información comprensible y reconoce derechos de acceso/corrección/cancelación.

**OK del modelo de datos.** Para cada campo se define finalidad, si identifica directa o indirectamente, obligatoriedad, origen (manual o sistema autorizado), unidad/código, autoría, momento de medición/registro, calidad, corrección, consulta y borrado. Se define el flujo de ingreso/episodio, observación, traspaso y corrección sin sobrescritura silenciosa. Se confirma qué identificador es necesario y cuál debe evitarse.

**Antes de datos reales.** Confirmar responsable/encargado del tratamiento, autoridad institucional, fundamento/consentimiento cuando corresponda, aviso al titular y tratamiento de datos de menores con asesoría local. No usar datos reales en desarrollo, CI o demostraciones; usar datos sintéticos.

### 8. Seguridad, autenticación y auditoría

**Investigación y propuesta.** El cifrado local y Keystore son una parte, no toda la solución. La implementación actual no autentica a cada profesional, no aplica roles, no verifica `authorId` ni registra auditoría institucional. Keystore reduce la extracción de material clave, pero un proceso/dispositivo comprometido aún puede usar claves y no todos los dispositivos garantizan protección hardware. La guía Android recomienda proteger almacenamiento y minimizar datos; OWASP MASVS/MASTG ofrece controles verificables para autenticación, almacenamiento, criptografía, red, plataforma, privacidad e integridad.

**OK de seguridad.** Entregar modelo de amenazas; autenticación/roles; bloqueo de sesión; permisos mínimos; protección de pantalla, notificaciones, archivos temporales y registros; cifrado en reposo; canal cifrado si se sincroniza; auditoría no destructiva; gestión de claves/rotación/recuperación; manejo de pérdida/compromiso del dispositivo; y pruebas de seguridad correspondientes. La institución acepta el riesgo residual y el procedimiento de incidentes. No afirmar seguridad integral ni usar datos reales antes de completar este gate.

### 9. Privacidad, conservación, respaldo y base jurídica

**Investigación.** La Ley 149/2022 clasifica la salud como dato sensible; define principios de minimización, finalidad, seguridad, información y derechos. Para menores contempla autonomía progresiva o representación legal, según corresponda. El artículo 39 establece conservación conforme a la normativa de la materia y finalidad; indica un régimen de hasta cinco años salvo que una ley disponga otro plazo o el titular consienta otro. Por ello, “cinco años” no debe fijarse automáticamente para una historia/registro clínico: hay que identificar la norma sanitaria/institucional aplicable y obtener criterio jurídico. La Resolución 58/2022 requiere considerar respaldo, informar dónde se almacena y por cuánto tiempo, y establece como regla servidores nacionales para hospedar/replicar bases de datos personales, salvo excepciones previstas. La aplicación actual deshabilita backups Android en algunos datos locales; ese ajuste no resuelve el respaldo institucional que pueda ser obligatorio.

**Propuesta provisional.** No activar almacenamiento de pacientes reales ni sincronización externa. Diseñar minimización, aviso, solicitudes de acceso/corrección/cancelación, plazo de conservación y destrucción según el responsable institucional; especificar respaldo cifrado, restauración y jurisdicción. No interpretar “offline” como exención de las reglas de datos personales.

**OK legal y operativo.** Aprobación escrita de asesoría jurídica/privacidad y del responsable institucional que determine: base y finalidad; aviso/consentimiento o excepción aplicable; derechos; ubicación; conservación/borrado; copias y restauración; declaración/registro que corresponda; incidentes; y transferencias. No establecer un plazo o modalidad de consentimiento unilateralmente en el código.

### 10. Importación, exportación e interoperabilidad

**Investigación y propuesta.** No se ha seleccionado sistema clínico ni interfaz. HL7 FHIR es un estándar de intercambio con recursos y perfiles; no define por sí solo las reglas locales, terminologías, autorización ni la integración. Por eso no conviene congelar R4/R5 hasta conocer el sistema/guía de implementación del hospital. Si el socio lo soporta, FHIR es una opción candidata; si no, usar el formato autorizado por esa institución. El modo sin red se mantiene como requisito; sincronización es opcional.

**OK de integración.** El hospital identifica sistema, responsable, protocolo/versión, dirección de intercambio, campos y terminologías/mapeos, autorización, autenticación, cifrado, errores, conflictos, auditoría y comportamiento offline. Una prueba con datos sintéticos demuestra que no se altera población, unidad, hora, autoría ni procedencia. No conectar monitores ni controlar equipos.

### 11. Verificación, validación y pruebas de uso

**Investigación y propuesta.** CECMED E 130-25 separa verificación analítica (entrada → resultado técnico) de validación clínica (significado clínico en el uso previsto), establece control de versiones, ciberseguridad y gestión de riesgos, y especifica la evidencia de pruebas del sistema. Para SaMD, la regulación contempla validación clínica basada en evidencia para el mismo uso, extrapolación justificada o nuevos datos, y reportes de ensayo emitidos por laboratorio acreditado o reconocido por CECMED. La OMS señala que el soporte digital debe operar dentro del ámbito del personal y con lógica válida.

**OK de desarrollo.** CI reproducible, pruebas unitarias/instrumentadas, casos de borde y datos sintéticos; estas actividades pueden continuar en paralelo.

**OK para uso clínico.** Plan trazable por función y versión: entradas, resultados esperados derivados de fuentes; datos ausentes/inconsistentes; población y límites; regresión; seguridad/usabilidad en el entorno previsto; pruebas offline/reinicio/actualización; validación analítica y clínica que determine CECMED; aprobaciones institucionales y, si se evalúan pacientes/datos, autorizaciones éticas/regulatorias aplicables. Los especialistas no son requisito previo para escribir el código, pero la evaluación requerida sí debe completarse antes de habilitar la función para atención.

### 12. Clasificación regulatoria y salida a producción

**Investigación y propuesta.** CECMED E 130-25 (edición 5) cubre requisitos de inscripción de fabricantes y registro sanitario del software de uso médico, incluidos los elementos de gestión de calidad, requisitos funcionales, arquitectura, riesgos, ensayos, ciberseguridad y validación clínica. Indica que desarrolladores de SaMD se inscriben como fabricantes, sujeto a su alcance. E 117-23, regla 11, clasifica software de apoyo a decisiones según la importancia de la información y el daño potencial; el nivel puede subir si las decisiones pueden causar daño grave, irreversible o muerte. **No se asigna una clase a UCI Assistant en esta investigación.** La finalidad y riesgo deben presentarse a CECMED para criterio aplicable a cada módulo.

**OK regulatorio/de producción.** Respuesta o determinación formal de CECMED sobre aplicabilidad, clase/ruta, fabricante/solicitante, requisitos de calidad, evidencias y permisos; patrocinio/centro institucional; contenido y datos autorizados; firma de producción; versiones y cambios controlados; proceso de retirada/actualización/soporte; y aprobaciones locales. Un APK debug que pasa CI no equivale a autorización sanitaria ni de uso clínico.

## Matriz resumida

| N.º | Estado según la investigación | Qué permite dar el OK |
|---|---|---|
| 1. Uso previsto | Puede redactarse ahora por función; clasificación formal pendiente | Ficha de uso previsto completa y enviada a la autoridad regulatoria cuando aplique |
| 2. Población | Pediatría primero ya acordada; límites específicos pendientes | Mapa de dominios y edades/exclusiones citados por contenido |
| 3. Roles | Usuarios generales definidos; matriz institucional pendiente | Identidad y permisos aprobados por el centro |
| 4. Protocolos | Catálogo candidato, no aprobado; derechos pendientes | Fuente/licencia/adopción local y aprobaciones por elemento |
| 5. Calculadoras | Implementaciones limitadas existentes; pruebas de software pasaron | Especificación, pruebas de referencia y validación necesaria por uso previsto |
| 6. Alertas | Motor clínico inactivo; recordatorios manuales solamente | Regla de cada alerta, fuente, protocolo enlazado, riesgo y aprobación |
| 7. Datos | Ledger técnico separado, sin UI ni expediente | Diccionario de datos y flujos aprobados; autorización antes de datos reales |
| 8. Seguridad | Cifrado parcial; autenticación/roles/auditoría pendientes | Diseño de seguridad y pruebas; riesgos residuales aceptados |
| 9. Privacidad/conservación | Ley 149/2022 y Resolución 58/2022 identificadas; interpretación institucional pendiente | Política escrita de finalidad, derechos, retención, respaldo y ubicación |
| 10. Interoperabilidad | No hay sistema seleccionado; FHIR es opción, no decisión | Contrato/mapeo del sistema autorizado y prueba sintética |
| 11. Validación | CI de software existe; validación clínica no demostrada | Informe/versionado de pruebas y revisión clínica/regulatoria previa al uso |
| 12. Producción | APK debug; ruta CECMED por confirmar | Clasificación/ruta aplicable, registro/permisos, firma y soporte de producción |

## Qué puede avanzar ya sin esperar estos OK

- Implementar los módulos pendientes con datos sintéticos y límites de uso claros.
- Completar modelos, pantallas, navegación offline, pruebas, trazabilidad y documentación.
- Preparar fichas y prototipos de protocolos/alertas sin activar contenido clínico no aprobado.
- Verificar calculadoras contra fuentes identificables sin inventar fórmulas y sin presentarlas como recomendación clínica.
- Preparar la preconsulta CECMED y la ficha de privacidad/datos para la institución.

## Fuentes principales

- CECMED. [Resolución 45/2025 y Regulación E 130-25, edición 5](https://www.cecmed.cu/sites/default/files/adjuntos/ambitor/AR%20No.%2000-510.pdf).
- CECMED. [Resolución 28/2023 y Regulación E 117-23, regla 11](https://www.cecmed.cu/sites/default/files/adjuntos/Reglamentacion/Res.No_.28.Regulaci%C3%B3n%20E%20117-23%20Reglas%20de%20clasificaci%C3%B3n%20por%20riesgo%20para%20equipos%20y%20dispositivos%20m%C3%A9dicos_0.pdf).
- Ministerio de Justicia de Cuba. [Gaceta Oficial No. 90/2022: Ley 149/2022 y Resolución 58/2022](https://www.minjus.gob.cu/sites/default/files/archivos/publicacion/2022-08/goc-2022-o90_0_0.pdf).
- OMS. [Recommendations on digital interventions for health system strengthening](https://www.who.int/publications/i/item/9789241550505), 2019.
- Android Developers. [Android Keystore system](https://developer.android.com/privacy-and-security/keystore).
- HL7. [FHIR Overview](https://www.hl7.org/fhir/overview.html).
- OWASP. [Mobile Application Security Verification Standard](https://mas.owasp.org/MASVS/).
- Documentos internos ya vigentes: [adenda al contrato técnico v1.1](adenda-contrato-tecnico-v1.1.md), [investigación bibliográfica clínica](investigacion-bibliografica-clinica.md), [catálogo pediátrico](catalogo-fuentes-pediatricas.md), [modelo de datos y gobierno](modelo-datos-clinicos-y-gobierno-contenido.md), [alertas y recordatorios](alertas-y-recordatorios.md) y [traspaso clínico](traspaso-clinico.md).
