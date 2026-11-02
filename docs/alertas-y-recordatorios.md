# Alertas clínicas y recordatorios manuales

## Estado

El motor de alertas clínicas permanece **inactivo**: no implementa reglas, umbrales, cálculos, ingestión de datos de pacientes ni asociaciones con protocolos aprobados. Esa correspondencia y sus criterios no se inventan en el código. La pantalla distingue ese trabajo pendiente de los **recordatorios manuales genéricos** disponibles.

## Recordatorios incluidos

- Permiten crear un recordatorio de una sola vez con título breve y fecha/hora elegida por el usuario, consultarlo y marcarlo completado mediante borrado.
- No se asocian a pacientes, episodios, tratamientos, tareas clínicas ni protocolos; la aplicación no puede detectar de forma fiable si alguien introduce esos datos en el título. No introducir información identificable o clínica.
- El título y la hora se guardan en archivos locales cifrados con AES-GCM y una clave no exportable de Android Keystore. El texto del recordatorio no se incluye en la notificación del sistema; solo se muestra un aviso genérico. WorkManager recibe un identificador opaco y la programación necesaria, no el título.
- El permiso de notificaciones del sistema es opcional. Sin él, el recordatorio permanece visible en la lista local, pero no se muestra un aviso del sistema.
- WorkManager ofrece programación **no exacta** y Android puede demorar u omitir la entrega por condiciones del sistema, permisos, ahorro de batería u otros factores. No usar esta función para urgencias, reevaluaciones clínicas, tratamientos, dosis, monitorización ni sustitución de una alerta asistencial.
- La función opera localmente y no sincroniza datos. No se añadió ingestión de señales, conexión con dispositivos ni automatización clínica.

## Límites y validación

Los recordatorios no son el cumplimiento de las alertas individualizadas previstas en el contrato del producto. El motor clínico futuro necesitará criterios y fuentes identificables, población y entorno definidos, datos autorizados, controles de calidad y correspondencia versionada con protocolos aprobados para consulta; este trabajo de desarrollo no queda bloqueado por esa validación futura.

Las pruebas automatizadas cubren validación de campos, guardado/listado/lectura/borrado local, ausencia del título en el archivo cifrado y rechazo de ciphertext alterado. La integración de notificaciones, entrega tras reinicio, comportamiento en fabricantes/dispositivos y puntualidad no se consideran validados exhaustivamente. La persistencia en Android Keystore implica que una pérdida o invalidación de la clave puede volver irrecuperables los recordatorios; no hay recuperación ni sincronización.
