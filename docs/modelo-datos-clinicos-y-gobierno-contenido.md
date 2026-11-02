# Modelo de procedencia clínica y ciclo de vida de contenido

## Estado y límites

`:core:clinicaldata` contiene modelos y validadores de procedencia y gobierno, y ahora un **ledger local cifrado de observaciones sintéticas**. No es una historia clínica ni un expediente listo para datos reales. No hay interfaz para observaciones, base de datos, sincronización, unidades codificadas, conversiones, escalas, umbrales o interpretación clínica. El ledger no está conectado a la pantalla de traspaso ni a otras funciones. No introducir datos reales en este prototipo.

## Población y observaciones

`ClinicalPopulation` hace explícita la separación `ADULT`, `PEDIATRIC` y `NEONATAL`. Los módulos y paquetes de contenido deben seleccionar una población de forma explícita; este enum no habilita por sí mismo una biblioteca adulta o neonatal ni permite mezclar sus reglas, escalas, unidades o fuentes.

`ClinicalObservationRecord` separa código de parámetro, texto del valor y unidad declarada, e incluye tiempos de observación/registro, autoría, origen (`MANUAL`, `TRANSCRIBED`, `AUTHORIZED_IMPORT`, `DERIVED`), condición del dato y una referencia no destructiva para correcciones. El validador comprueba campos de procedencia, población esperada y estructura de la corrección. No convierte unidades ni interpreta el valor. Si falta el instante de observación, exige marcar el dato como incompleto.

## Ledger de observaciones cifrado

`EncryptedObservationLedgerStore` guarda cada registro como un archivo independiente bajo el directorio privado de la aplicación. Cifra y autentica los bytes con AES-256-GCM usando una clave no exportable de Android Keystore; cada archivo tiene un IV aleatorio y los metadatos de formato e ID se usan como datos autenticados asociados. No se escribe el registro en texto claro al almacenamiento. El formato es versionado y las lecturas fallan cerradas ante archivos incompletos, malformados o cuyo tag GCM no valida.

La interfaz del almacén solo añade y lee registros: no ofrece operaciones de editar o borrar. Una corrección se guarda como una observación nueva con `condition = CORRECTED` y una referencia a la anterior; el almacén exige que el registro citado exista y pertenezca al mismo episodio y población, conserva el original y evita reutilizar su ID. Las consultas por episodio exigen población explícita y nunca mezclan poblaciones.

Estos límites describen la API y el formato local, no una garantía de auditoría resistente a un dispositivo comprometido. El ledger **no detecta la eliminación o restauración de archivos completos, rollback de copias, manipulación por root ni acceso desde un dispositivo comprometido**; tampoco implementa autenticación de usuario, roles, identidad verificable del autor, auditoría institucional, política de retención, recuperación/rotación de claves, importación autorizada ni sincronización. La clave no requiere autenticación de usuario y no se afirma que el hardware del dispositivo la proteja. El cifrado no vuelve apropiado el almacén para información real ni lo convierte en un expediente clínico.

## Manifiesto para contenido clínico

`ClinicalContentManifest` registra tipo de contenido, población, jurisdicción e institución, versión, fuentes, estado de licencia declarado, propietario, revisor, aprobador y fechas de aprobación, vigencia y revisión. `activationIssues(context)` devuelve bloqueos si el contenido no está activo, no corresponde exactamente a la población o sitio solicitado, carece de fuentes o metadatos, no declara revisión de derechos despejada, usa al mismo titular como propietario y revisor, no está vigente o requiere revisión vencida.

El manifiesto no prueba que una licencia sea válida ni autentica a la persona que escribe un identificador; esos controles no están implementados. La marca de licencia es un dato pendiente de verificar documentalmente, no una decisión legal. Ningún protocolo, escala o algoritmo del catálogo actual utiliza este manifiesto como autorización para uso asistencial.

## Trazabilidad bibliográfica de procedimientos y estado de derechos

El catálogo mantiene ocho etiquetas de temas de procedimiento, sin pasos ni criterios. Cada fuente candidata lleva una población tipada (`ClinicalPopulation.PEDIATRIC`) y un estado tipado de revisión de derechos. El tema **Triage** enlaza, sin fusionar sus contenidos, tanto al índice editorial de *Urgencias Pediátricas* (2024) como a la actualización selectiva ETAT de la OMS (2016).

En el catálogo, ocho referencias están como `NOT_ASSESSED`; para la fuente de la OMS se registra `LICENSE_IDENTIFIED_COMPATIBILITY_PENDING`, porque se identificó CC BY-NC-SA 3.0 IGO pero la compatibilidad del uso/distribución y los derechos de materiales de terceros siguen pendientes. Este estado es metadato declarativo, no aprobación jurídica ni autorización. Las etiquetas de población tampoco certifican aplicabilidad local. La referencia de la OMS no reemplaza la guía ETAT completa ni el *Pocket Book*, y no se incorporó texto clínico de ella.

## Pruebas

Pruebas unitarias cubren la separación de población, datos incompletos, correcciones no autorreferenciales, metadatos mínimos del manifiesto, estado de licencia, vigencia, independencia declarada del revisor y correspondencia con institución/jurisdicción, además de población y estado tipado de las referencias candidatas. Pruebas instrumentadas con datos sintéticos verifican cifrado AES-GCM/Android Keystore, lectura, historial de correcciones, población, rechazos de entradas o linajes inválidos y fallo cerrado al alterar ciphertext o truncar un archivo. El workflow de CI ejecuta estas pruebas en emulador Android. Son pruebas de software; no constituyen validación clínica, dictamen jurídico, auditoría resistente a alteración ni certificación de seguridad.
