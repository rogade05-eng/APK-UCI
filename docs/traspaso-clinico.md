# Continuidad y entrega pediátrica — plantilla temporal

**Estado:** prototipo de interfaz; no es un registro asistencial ni está autorizado para uso clínico.

## Alcance implementado

La pantalla `:feature:handoff` ofrece campos manuales para organizar un borrador de traslado: referencia local de episodio, origen y destino, tipo/motivo, prioridad y versión de criterio institucional introducidas por la persona usuaria, tiempos de derivación/salida/recepción/entrega, profesionales que entregan/reciben y resumen de problemas activos, alergias, intervenciones, dispositivos, resultados pendientes y tareas.

La plantilla solo conserva el texto en estado Compose mientras permanece abierta. No guarda en disco, no cifra, no exporta, no copia al portapapeles, no sincroniza, no autentica al personal, no firma y no se integra con un expediente. Al salir, se pide confirmar el descarte cuando hay texto. No se deben introducir datos reales de pacientes.

Los campos de fecha/hora son texto libre: no se interpretan ni validan formato, zona horaria, autoría o secuencia. Prioridad, motivo y referencia institucional tampoco se evalúan. No se seleccionan destino, admisión o urgencia; no se crean recomendaciones ni notificaciones. El borrador no sustituye la entrega verbal ni el registro oficial de la institución.

## Fuente de requisitos

Los campos se derivan del requisito funcional **PAT-CONT-01** de la [adenda al contrato técnico v1.1](adenda-contrato-tecnico-v1.1.md), que requiere registrar origen, institución/servicio remitente, tipo de ingreso, motivo/prioridad local, tiempos disponibles, destino, responsables y un resumen estructurado de entrega. La [evaluación del programa de especialización](evaluacion-programa-especializacion-uci.md) utiliza el programa curricular del MINSAP únicamente como mapa temático; no se tomó de él ninguna dosis, escala, prioridad o algoritmo clínico.

## Límites de privacidad y pruebas

El formulario es volátil, pero texto clínico escrito en pantalla puede seguir expuesto a observación, capturas de pantalla, teclado, memoria del proceso o mecanismos del sistema operativo. No se ha validado protección frente a capturas, vistas previas del selector de aplicaciones, autocompletado, informes de fallos, copias de seguridad o análisis forense. Por eso el prototipo prohíbe datos reales y no debe usarse para una entrega asistencial.

Las pruebas unitarias actuales solo verifican el estado vacío/no vacío del borrador y el tratamiento de espacios en blanco. No verifican usabilidad clínica, campos obligatorios, integridad temporal, seguridad, auditoría ni resultados clínicos.

## Trabajo pendiente

Persistencia local cifrada, autenticación/autorización, auditoría no destructiva, identidad y roles, campos institucionales versionados, formato temporal validado, firma/acuse, minimización de datos, política de retención/borrado, exclusión de respaldo y pruebas de seguridad requieren especificación propia antes de almacenar información identificable. Importación/exportación a una historia clínica autorizada no está implementada. Estas actividades no bloquean completar la plantilla temporal, pero sí deben resolverse antes de manejar datos reales o habilitar el flujo asistencial.
