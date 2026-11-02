# Continuidad y entrega pediátrica — borradores cifrados

**Estado:** prototipo de almacenamiento local; no es un expediente clínico completo, no sustituye el registro oficial y no está autorizado para uso asistencial.

## Alcance implementado

La pantalla `:feature:handoff` reúne campos manuales para organizar un borrador de traslado: referencia local de episodio, origen y destino, tipo/motivo, prioridad y versión de criterio institucional introducidas por la persona usuaria, tiempos de derivación/salida/recepción/entrega, profesionales que entregan/reciben y resumen de problemas activos, alergias, intervenciones, dispositivos, resultados pendientes y tareas.

Al guardar, cada acción crea una **instantánea inmutable nueva**. Los archivos se guardan en el directorio privado de la app, cifrados con AES-GCM y una clave de Android Keystore; el identificador UUID también se autentica como dato asociado. No se guardan títulos ni códigos de episodio en el nombre de archivo. El formulario no incluye una acción de exportación ni copia automática al portapapeles y no sincroniza; el sistema operativo aún puede permitir copiar texto seleccionado manualmente. El manifiesto deshabilita el backup de la app (`allowBackup=false`) y la ventana del módulo usa `FLAG_SECURE` mientras está abierta. Si se borran los datos de la app, se desinstala o la clave deja de estar disponible, los borradores no se pueden recuperar.

La eliminación es manual y borra el archivo de la app; no garantiza la sobreescritura irrecuperable de bloques de memoria flash. No se permite editar un snapshot guardado: cada guardado produce otro archivo. No hay historial de correcciones, autor autenticado, firma, control de acceso, bloqueo de sesión ni auditoría atribuible.

## Límites funcionales

Los campos de fecha/hora son texto libre: no se interpretan ni validan formato, zona horaria, autoría o secuencia. Prioridad, motivo y referencia institucional tampoco se evalúan. No se seleccionan destino, admisión o urgencia; no se crean recomendaciones ni notificaciones. No hay datos demográficos, órdenes, administración de medicamentos, evolución clínica general, interoperabilidad ni expediente longitudinal.

La clave no requiere autenticación del usuario. Quien tenga acceso al dispositivo desbloqueado y a la app puede abrir los borradores; por ello, a pesar del cifrado y `FLAG_SECURE`, el prototipo **prohíbe datos reales de pacientes** hasta añadir autenticación/autorización, roles, auditoría, retención y aprobación institucional. `FLAG_SECURE` no evita una fotografía externa, observación directa ni todas las exposiciones por teclado/sistema operativo. El comportamiento de copias de seguridad debe verificarse en dispositivos/versiones destino antes de cualquier uso real.

## Fuente de requisitos

Los campos se derivan de **PAT-CONT-01** de la [adenda al contrato técnico v1.1](adenda-contrato-tecnico-v1.1.md). El cifrado local responde al requisito de almacenamiento local y a la investigación de seguridad Android en [Android Keystore](https://developer.android.com/privacy-and-security/keystore); no se afirma conformidad con MASVS ni certificación de seguridad.

## Pruebas

- Pruebas unitarias: campos vacíos, texto en blanco y orden de serialización del modelo.
- Prueba instrumentada configurada para emulador: guardar/recuperar datos sintéticos, verificar que el texto no aparece en el archivo cifrado, borrar el archivo y comprobar que alterar el ciphertext falla la autenticación GCM.
- **Ejecución de las nuevas pruebas de almacenamiento pendiente de CI.** No se deben presentar como verificadas hasta que la ejecución remota termine correctamente.
- Las pruebas no evalúan autenticación, roles, auditoría, resistencia física, dispositivos de fabricantes distintos, cumplimiento jurídico, usabilidad clínica ni resultados asistenciales.

## Trabajo pendiente

Autenticación y autorización, identidades/roles, auditoría no destructiva, formato temporal validado, firma/acuse, enlaces versionados a episodio, política de retención/borrado, revisión de copias, recuperación segura de claves y pruebas de seguridad requieren definición propia antes de almacenar información real. Importación/exportación a una historia clínica autorizada no está implementada. Estos elementos no bloquean el prototipo de cifrado ni sus pruebas sintéticas, pero sí cualquier uso con pacientes.
