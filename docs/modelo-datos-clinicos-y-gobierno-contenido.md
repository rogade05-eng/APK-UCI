# Modelo de procedencia clínica y ciclo de vida de contenido

## Estado y límites

`:core:clinicaldata` contiene modelos y validadores de procedencia y gobierno, y ahora un **ledger local cifrado de observaciones sintéticas**. No es una historia clínica ni un expediente listo para datos reales. No hay interfaz para observaciones, base de datos, sincronización, unidades codificadas, conversiones, escalas, umbrales o interpretación clínica. El ledger no está conectado a la pantalla de traspaso ni a otras funciones. No introducir datos reales en este prototipo.

## Población y observaciones

`ClinicalPopulation` hace explícita la separación `ADULT`, `PEDIATRIC` y `NEONATAL`. Los módulos y paquetes de contenido deben seleccionar una población de forma explícita; este enum no habilita por sí mismo una biblioteca adulta o neonatal ni permite mezclar sus reglas, escalas, unidades o fuentes.

`ClinicalObservationRecord` separa código de parámetro, texto del valor y unidad declarada, e incluye tiempos de observación/registro, autoría, origen (`MANUAL`, `TRANSCRIBED`, `AUTHORIZED_IMPORT`, `DERIVED`), condición del dato y una referencia no destructiva para correcciones. El validador comprueba campos de procedencia, población esperada y estructura de la corrección. No convierte unidades ni interpreta el valor. Si falta el instante de observación, exige marcar el dato como incompleto.

## Ledger de observaciones cifrado

`EncryptedObservationLedgerStore` guarda cada registro como un archivo independiente bajo el directorio privado de la aplicación. Cifra y autentica los bytes con AES-256-GCM usando una clave no exportable de Android Keystore; cada archivo tiene un IV aleatorio y los metadatos de formato e ID se usan como datos autenticados asociados. No se escribe el registro en texto claro al almacenamiento. Cada entrada lleva secuencia y digest SHA-256 del archivo anterior; un checkpoint separado autentica el último ID, secuencia y digest con HMAC-SHA-256 bajo otra clave de Android Keystore. Las lecturas comprueban los tags GCM, continuidad de secuencias, cadena de digests, linaje de correcciones y checkpoint; ante inconsistencia fallan cerradas.

La interfaz del almacén solo añade y lee observaciones: no ofrece operaciones de editar o borrar. Una corrección se guarda como una observación nueva con `condition = CORRECTED` y una referencia a la anterior; el almacén exige que el registro citado exista y pertenezca al mismo episodio y población, conserva el original y evita reutilizar su ID. Las consultas por episodio exigen población explícita y devuelven el historial en orden de alta. El checkpoint no es un registro clínico y puede actualizarse al añadir una observación.

Esto detecta registros omitidos o reordenados mientras se conserva la clave de checkpoint, incluida la eliminación del último registro o del directorio del ledger. **No detecta un rollback coordinado de todos los archivos y del checkpoint a una copia antigua auténtica**; tampoco protege si se borran las claves, se reinicia el estado de la app o se compromete el dispositivo. Un cierre inesperado entre escribir la observación y actualizar el checkpoint deja el ledger en estado de fallo cerrado y no se repara automáticamente. Estos límites no constituyen una garantía de auditoría resistente a un dispositivo comprometido. Tampoco se implementan autenticación de usuario, roles, identidad verificable del autor, auditoría institucional, política de retención, recuperación/rotación de claves, importación autorizada ni sincronización. Las claves no requieren autenticación de usuario y no se afirma que el hardware del dispositivo las proteja. El cifrado no vuelve apropiado el almacén para información real ni lo convierte en un expediente clínico.

## Manifiesto para contenido clínico

`ClinicalContentManifest` registra tipo de contenido, población, jurisdicción e institución, versión, fuentes, estado de licencia declarado, propietario, revisor, aprobador y fechas de aprobación, vigencia y revisión. `activationIssues(context)` devuelve bloqueos si el contenido no está activo, no corresponde exactamente a la población o sitio solicitado, carece de fuentes o metadatos, no declara revisión de derechos despejada, usa al mismo titular como propietario y revisor, no está vigente o requiere revisión vencida.

El manifiesto no prueba que una licencia sea válida ni autentica a la persona que escribe un identificador; esos controles no están implementados. La marca de licencia es un dato pendiente de verificar documentalmente, no una decisión legal. Ningún protocolo, escala o algoritmo del catálogo actual utiliza este manifiesto como autorización para uso asistencial.

## Trazabilidad bibliográfica de procedimientos y estado de derechos

El catálogo mantiene ocho etiquetas de temas de procedimiento, sin pasos ni criterios. Cada fuente candidata lleva una población tipada (`ClinicalPopulation.PEDIATRIC`) y un estado tipado de revisión de derechos. El tema **Triage** enlaza, sin fusionar sus contenidos, tanto al índice editorial de *Urgencias Pediátricas* (2024) como a la actualización selectiva ETAT de la OMS (2016).

En el catálogo, ocho referencias están como `NOT_ASSESSED`; para la fuente de la OMS se registra `LICENSE_IDENTIFIED_COMPATIBILITY_PENDING`, porque se identificó CC BY-NC-SA 3.0 IGO pero la compatibilidad del uso/distribución y los derechos de materiales de terceros siguen pendientes. Este estado es metadato declarativo, no aprobación jurídica ni autorización. Las etiquetas de población tampoco certifican aplicabilidad local. La referencia de la OMS no reemplaza la guía ETAT completa ni el *Pocket Book*, y no se incorporó texto clínico de ella.

## Pruebas

Pruebas unitarias cubren la separación de población, datos incompletos, correcciones no autorreferenciales, metadatos mínimos del manifiesto, estado de licencia, vigencia, independencia declarada del revisor y correspondencia con institución/jurisdicción, además de población y estado tipado de las referencias candidatas. Pruebas instrumentadas con datos sintéticos verifican AES-GCM/Android Keystore, HMAC del checkpoint, reapertura del almacén, historial de correcciones, población, rechazos de entradas/linajes inválidos, ciphertext o checkpoint manipulados, archivos truncados y detección de eliminación intermedia, final o completa mientras la clave permanece disponible. El workflow de CI ejecuta estas pruebas en emulador Android. Son pruebas de software; no constituyen validación clínica, dictamen jurídico, protección contra rollback coordinado de una copia antigua, auditoría institucional ni certificación de seguridad.
