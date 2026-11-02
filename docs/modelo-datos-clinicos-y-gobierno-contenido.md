# Modelo de procedencia clínica y ciclo de vida de contenido

## Estado y límites

Se añadió `:core:clinicaldata` como **modelo de dominio y validadores**, no como expediente clínico ni almacenamiento. No hay interfaz para pacientes, base de datos, sincronización, unidades codificadas, conversiones, escalas, umbrales o interpretación clínica. No introducir datos reales en este prototipo.

## Población y observaciones

`ClinicalPopulation` hace explícita la separación `ADULT`, `PEDIATRIC` y `NEONATAL`. Los módulos y paquetes de contenido deben seleccionar una población de forma explícita; este enum no habilita por sí mismo una biblioteca adulta o neonatal.

`ClinicalObservationRecord` separa código de parámetro, texto del valor y unidad declarada, e incluye tiempos de observación/registro, autoría, origen (`MANUAL`, `TRANSCRIBED`, `AUTHORIZED_IMPORT`, `DERIVED`), condición del dato y una referencia no destructiva para correcciones. El validador comprueba campos de procedencia, población esperada y referencias de corrección. No convierte unidades, evalúa el valor, ni crea un historial persistente. Si falta el instante de observación, exige marcar el dato como incompleto.

El modelo aún no está conectado a la pantalla de traspaso o a un expediente, no cifra datos ni implementa autenticación, roles, auditoría o retención. Para un uso posterior serían necesarios un almacenamiento apropiado, identidades y permisos, historial inmutable y controles adicionales.

## Manifiesto para contenido clínico

`ClinicalContentManifest` registra tipo de contenido, población, jurisdicción e institución, versión, fuentes, estado de licencia declarado, propietario, revisor, aprobador y fechas de aprobación, vigencia y revisión. `activationIssues(context)` devuelve bloqueos si el contenido no está activo, no corresponde exactamente a la población o sitio solicitado, carece de fuentes o metadatos, no declara revisión de derechos despejada, usa al mismo titular como propietario y revisor, no está vigente o requiere revisión vencida.

El manifiesto no prueba que una licencia sea válida ni autentica a la persona que escribe un identificador; esos controles no están implementados. La marca de licencia es un dato pendiente de verificar documentalmente, no una decisión legal. Ningún protocolo, escala o algoritmo del catálogo actual utiliza este manifiesto como autorización para uso asistencial.

## Trazabilidad bibliográfica de procedimientos

El catálogo mantiene ocho etiquetas de temas de procedimiento, sin pasos ni criterios. Ahora cada tema conserva una o más referencias candidatas con población explícita; el tema **Triage** enlaza, sin fusionar sus contenidos, tanto al índice editorial de *Urgencias Pediátricas* (2024) como a la actualización selectiva ETAT de la OMS (2016). La referencia de la OMS no reemplaza la guía ETAT completa ni el *Pocket Book*, y no se incorporó texto clínico de ella. La edición, derechos y aplicabilidad de cada fuente siguen pendientes de verificación para cualquier contenido.

## Pruebas

Pruebas unitarias cubren la separación de población, datos incompletos, correcciones no autorreferenciales, metadatos mínimos del manifiesto, estado de licencia, vigencia, independencia declarada del revisor y correspondencia con institución/jurisdicción. Son pruebas de software del contrato de datos, no validación clínica, jurídica ni de seguridad integral.
