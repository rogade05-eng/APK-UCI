# APK-UCI

Documentación y desarrollo de **UCI Assistant**, una aplicación Android offline-first para apoyo al trabajo en Medicina Intensiva. Cuba es el mercado inicial; la primera dirección funcional incluye bibliotecas de protocolos de consulta y alertas individualizadas para profesionales, como funciones separadas, sin IA, diagnóstico autónomo, prescripción automática ni control directo de equipos. Las alertas enlazarían al protocolo vigente aprobado correspondiente para consulta; el profesional conserva la decisión y no hay ejecución automática. **La primera población seleccionada es pediatría**, con intención de abarcar todas las áreas pediátricas posibles desde la primera etapa. Los usuarios previstos incluyen al equipo clínico de UCI pediátrica y residentes; los entornos iniciales elegidos son UCI pediátrica y urgencias pediátricas. La dirección de captura es entrada manual y posible importación/exportación desde un sistema clínico autorizado por la institución; no se ha seleccionado ni aprobado un sistema o integración. La preferencia es conservar datos identificables de pacientes en almacenamiento local cifrado, sujeta a revisión y autorización. UCI Assistant se concibe como herramienta complementaria, no como historia clínica oficial; cualquier transferencia de resultados al registro institucional requerirá una ruta autorizada. La base jurídica y la retención siguen pendientes. El catálogo de dominios, los límites de edad/subgrupos, los flujos por entorno, los roles/permisos, la supervisión, el centro piloto y el uso previsto de cada función siguen por definir. Adultos y neonatología quedan fuera de la primera etapa; se mantienen bibliotecas clínicas independientes para las tres poblaciones.

## Estado de desarrollo

Se creó un **andamiaje Android nativo** en Kotlin/Jetpack Compose con navegación local y módulos separados:

- `:app`: actividad y navegación entre pantallas.
- `:feature:home`: pantalla inicial de alcance y estado.
- `:feature:protocols`: estado vacío de la biblioteca; no contiene protocolos clínicos.
- `:feature:alerts`: estado vacío de alertas; no evalúa datos ni activa reglas.
- `:core:designsystem`: tema visual y aviso de prototipo reutilizable.

Las pantallas de protocolos y alertas son marcadores de alcance, no funciones clínicas. No almacena datos de pacientes, no contiene escalas, umbrales, alertas activas ni recomendaciones clínicas; tampoco declara permisos de red ni se conecta a monitores, ventiladores o historias clínicas. No debe usarse en atención clínica. El identificador de paquete `org.uciasistant.app` es provisional.

Para compilar localmente se requiere JDK 17 y Android SDK Platform 35. Usa `./gradlew :app:assembleDebug` (Windows: `gradlew.bat :app:assembleDebug`); el APK queda en `app/build/outputs/apk/debug/app-debug.apk`. La compilación local no se pudo ejecutar en este entorno porque no hay Java, Gradle ni Kotlin instalados. **La compilación remota sí se completó** en [GitHub Actions, ejecución 41107762804](https://github.com/rogade05-eng/APK-UCI/actions/runs/41107762804), sobre el commit `1a9ad2e`: produjo el APK de depuración y pasó las comprobaciones de firma, identificador de paquete y actividad lanzadora. Descarga `uci-assistant-debug-apk` desde la sección **Artifacts** de esa ejecución; caduca el 13 de noviembre de 2026. No se completó una prueba de ejecución en emulador/dispositivo. La versión debug no es un lanzamiento de producción ni una autorización de uso clínico.

## Documentación

- [Adenda al contrato técnico v1.1](docs/adenda-contrato-tecnico-v1.1.md): requisitos adoptados para continuidad asistencial, monitorización, gobierno de contenido, poblaciones, configuración local y criterios de aceptación.
- [Evaluación del programa de especialización](docs/evaluacion-programa-especializacion-uci.md): análisis de la fuente curricular y fundamento de los cambios.
- [Investigación bibliográfica clínica](docs/investigacion-bibliografica-clinica.md): fuentes jerarquizadas por dominio y población, límites/ventanas de escalas, licencias, validación y requisitos de seguridad, privacidad, uso offline e interoperabilidad.

> Estado actual: documentación y andamiaje de interfaz no clínico. No constituye validación clínica ni dictamen jurídico; no implica autorización regulatoria, aprobación de piloto ni que haya algoritmos clínicos implementados.
