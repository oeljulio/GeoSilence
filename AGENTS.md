# Directrices de Desarrollo y Arquitectura (AGENTS.md)

Este documento define el contexto, estándares y directrices operativas para cualquier modelo de Inteligencia Artificial o agente de desarrollo que trabaje en el repositorio **GeoSilence**.

---

## 1. Rol y Perfil
Actúa como un **Ingeniero de Software Senior** especializado en desarrollo nativo para Android (Kotlin, Jetpack Compose y servicios en segundo plano). Tus decisiones deben priorizar:
- **Código limpio, modular, legible y fuertemente tipado:** Sigue las convenciones idiomáticas oficiales de Kotlin y las guías de arquitectura recomendadas para Android.
- **Principio de responsabilidad única y bajo acoplamiento:** Separa con claridad la capa de UI (Compose), capa de datos/persistencia (`Preferences` / `Models`), capa de geolocalización (`GeofenceManager`) y capa de servicios en segundo plano (`NotificationListenerService`, `BroadcastReceiver`, `WorkManager`).
- **Manejo defensivo de errores y registro de logs claro:** Captura excepciones potenciales (especialmente en parsing de JSON con Gson, APIs del sistema Android, componentes WebView y acceso a SharedPreferences) utilizando tags de log informativos (`Log.e`, `Log.d`, `Log.w`).
- **Rendimiento eficiente y consumo optimizado de recursos:** Minimiza el impacto en batería y memoria; respeta los límites de ejecución en segundo plano y las transiciones eficientes de geocercas (`Enter` / `Dwell` / `Exit`).

---

## 2. Visión General del Proyecto

- **Propósito:** GeoSilence es una aplicación nativa para Android diseñada para silenciar o descartar automáticamente las notificaciones de aplicaciones seleccionadas por el usuario cuando el dispositivo se encuentra dentro de un perímetro geográfico determinado (geocerca).
- **Stack Técnico:**
  - **Lenguaje:** Kotlin 2.0.21 (JVM target 17).
  - **Framework UI:** Jetpack Compose con Material 3 y Material Icons Extended.
  - **Compatibilidad Android:** `compileSdk = 35`, `targetSdk = 35`, `minSdk = 26` (Android 8.0 Oreo).
  - **Cartografía y Mapas:** Leaflet + OpenStreetMap embebido en `WebView` Android con puente JavaScript (`JavascriptInterface`), soporte táctil desacoplado del scroll contenedor y dibujo reactivo del radio de cobertura.
  - **Geolocalización:** Google Play Services Location (`GeofencingClient`).
  - **Intercepción de Notificaciones:** `NotificationListenerService` (`BIND_NOTIFICATION_LISTENER_SERVICE`).
  - **Tareas Periódicas en Segundo Plano:** Android Jetpack `WorkManager` para auto-eliminación y purga programada del historial de notificaciones.
  - **Persistencia:** `SharedPreferences` serializado con `Gson`.
  - **Herramienta de Compilación:** Gradle Wrapper 8.9 con Android Gradle Plugin 8.7.2 (`build.gradle.kts` con Kotlin DSL y catálogo centralizado `gradle/libs.versions.toml`).
  - **CI/CD:** GitHub Actions (`.github/workflows/build-apk.yml`) para compilar APK de depuración y publicar releases automáticas al empujar tags `v*`.

- **Estructura de Directorios:**
  - `app/src/main/AndroidManifest.xml`: Declaración central de permisos (`BIND_NOTIFICATION_LISTENER_SERVICE`, `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`, `RECEIVE_BOOT_COMPLETED`, etc.), actividades, receptores y servicios del sistema.
  - `app/src/main/java/com/personal/geosilence/`:
    - `MainActivity.kt`: Actividad principal, orquestación de solicitud de permisos en tiempo de ejecución (ubicación, notificaciones) y punto de anclaje de Jetpack Compose.
    - `data/`:
      - `Models.kt`: Modelos de datos (`GeofenceConfig`, `AppNotification`, `AppItem`, `RetentionPeriod`).
      - `AppPreferences.kt`: Persistencia local de configuración de geocerca, lista de apps bloqueadas, historial y política de retención.
    - `location/`:
      - `GeofenceManager.kt`: Registro, monitoreo y cancelación de geocercas GPS mediante `LocationServices.getGeofencingClient`.
    - `service/`:
      - `NotificationBlockerService.kt`: `NotificationListenerService` que intercepta y descarta notificaciones entrantes cuando el dispositivo está en zona y la app está activada.
      - `GeofenceBroadcastReceiver.kt`: Receptor de eventos de transición geográfica (`GEOFENCE_TRANSITION_ENTER`, `EXIT`, `DWELL`).
      - `BootReceiver.kt`: `BroadcastReceiver` para el evento `BOOT_COMPLETED` que reestablece las geocercas y tareas de limpieza al reiniciar el teléfono.
      - `HistoryCleanupWorker.kt`: `CoroutineWorker` que elimina notificaciones antiguas del historial según el período configurado.
      - `HistoryCleanupScheduler.kt`: Planificador de tareas periódicas con `WorkManager`.
    - `ui/`:
      - `MainScreen.kt`: Pantalla principal con pestañas navegables (Ubicación, Aplicaciones, Historial, Ajustes) y modo de prueba manual.
      - `SettingsScreen.kt`: Pantalla de ajustes para configurar retención, purga manual y accesos directos a permisos del sistema.
      - `map/InteractiveMapView.kt`: Componente Compose que envuelve el WebView del mapa y gestiona la interceptación de toques/scroll.
      - `map/MapHtmlProvider.kt`: Generador del código HTML/JS interactivo con Leaflet y OpenStreetMap.
      - `theme/`: Paleta de colores (`Color.kt`), tipografía (`Type.kt`) y tema Material 3 (`Theme.kt`).
  - `app/src/main/res/`: Recursos visuales, strings, colores y mipmaps.
  - `.github/workflows/`: Pipeline de GitHub Actions para compilación y despliegue.
  - `gradle/`: Configuración del wrapper y catálogo de versiones `libs.versions.toml`.
  - `CHANGELOG.md` & `RELEASE_NOTES.md`: Documentación de historial de cambios y notas de versión.

---

## 3. Reglas de Modificación de Código

1. **Análisis previo:** Lee y comprende los archivos existentes relacionados antes de proponer o escribir cualquier cambio. Analiza dependencias y efectos colaterales entre servicios en segundo plano y persistencia.
2. **Consistencia:** Respeta los patrones de diseño, nombres de variables y estilos ya establecidos en el código Kotlin y Jetpack Compose.
3. **Integridad:** No rompas funcionalidades existentes al refactorizar o añadir nuevas características. En particular:
   - Preserva la sincronización bidireccional entre el mapa (`InteractiveMapView.kt` / `MapHtmlProvider.kt`) y el estado Compose (`latitude`, `longitude`, `radiusMeters`).
   - Mantén la interoperabilidad segura en el puente JavaScript (`@JavascriptInterface`).
   - No elimines ni alteres inadvertidamente la lógica crítica de segundo plano (`BootReceiver`, `HistoryCleanupWorker`, `NotificationBlockerService`).
4. **Política de Versiones:** **NO modifiques el número de versión** (`versionCode`, `versionName` en `app/build.gradle.kts`, `README.md`, `CHANGELOG.md` ni `RELEASE_NOTES.md`) a menos que el usuario lo solicite expresamente.
5. **Preservación de comentarios:** Mantén intactos los comentarios existentes y explicaciones salvo que una modificación de código solicitada los vuelva redundantes o inexactos.
6. **Verificación:** Comprueba la sintaxis y valida la compatibilidad de tipos antes de dar por finalizada la tarea.

---

## 4. Flujo de Git y GitHub

Antes de realizar commits o subir cambios:
- **Inspección Previa:** Ejecuta siempre `git status` y revisa `git diff` para verificar que solo se incluyan los cambios intencionados.
- **Mensajes de Commit:** Usa estrictamente la convención *Conventional Commits*:
  - `feat:` para nuevas características.
  - `fix:` para corrección de fallos o bugs.
  - `refactor:` para refactorizaciones de código sin cambio de comportamiento.
  - `docs:` para cambios o adiciones en documentación.
  - `chore:` para mantenimiento, dependencias o configuración.
- **Commits Atómicos:** Realiza commits pequeños y con un único propósito claro.
- **Ramas:** No envíes cambios directamente a `main` si la tarea implica modificaciones estructurales de alto impacto; trabaja en ramas temáticas (`feature/` o `fix/`).
- **Tags y Releases:** No generes ni subas tags `v*` salvo indicación explícita del usuario, ya que disparan el workflow de release en GitHub Actions.

---

## 5. Entorno de Ejecución y Comandos Útiles

El proyecto se gestiona comúnmente en entornos Linux/Android (Termux):

- **Ver estado del repositorio:**
  ```bash
  git status
  ```
- **Revisar diferencias antes de añadir archivos:**
  ```bash
  git diff
  ```
- **Compilar APK de prueba (si el entorno cuenta con JDK y SDK):**
  ```bash
  ./gradlew assembleDebug --stacktrace
  ```
- **Limpiar caché de compilación:**
  ```bash
  ./gradlew clean
  ```

> [!NOTE]
> En entornos móviles ligeros como Termux donde no se disponga de un runtime local de Java (JDK 17) ni Android SDK para ejecutar `./gradlew`, la compilación y generación de APKs se delega al pipeline automatizado de GitHub Actions (`.github/workflows/build-apk.yml`).

---

## 6. Procedimiento de Auditoría y Publicación (Review & Release Manager)

Cuando un agente de IA actúe en el rol de **Code Reviewer & Release Manager**, debe seguir estrictamente este flujo antes de realizar cambios o publicar en GitHub:

### Paso 1: Extracción de Reglas y Contexto
1. Leer y analizar este archivo (`AGENTS.md`).
2. Extraer reglas operativas, estándares de calidad, comandos y directivas de despliegue.

### Paso 2: Auditoría y Verificación de la Aplicación
1. Realizar revisiones estáticas de código, linter y comprobaciones de compilación disponibles.
2. Evaluar calidad de código, casos de borde, posibles regresiones y completitud documental.
3. Identificar si se requieren ajustes en `AGENTS.md` o en la configuración del proyecto.

### Paso 3: Propuesta y Aprobación del Usuario (Puerta Obligatoria)
**Antes de modificar archivos o hacer push a GitHub**, el agente debe presentar un reporte estructurado de propuesta:

```markdown
## 📋 Execution & Review Proposal

### 🔍 Audit Summary
- **Status**: [PASSED / FAILED / ISSUES DETECTED]
- **Key Findings**: [Descripción breve de las comprobaciones realizadas]

### ✏️ Proposed Modifications to `AGENTS.md`
- [Lista de modificaciones propuestas]

### 📦 GitHub Release Details
- **Target Branch**: `main` / `release/vX.Y.Z`
- **Suggested Tag/Version**: `vX.Y.Z`
- **Commit Message**: `release: vX.Y.Z - [Resumen]`

---
⚠️ **Action Required**: Do you approve the updates to `agents.md` and the GitHub upload? (Yes/No/Modify)
```

Solo tras la aprobación explícita del usuario (`Yes` o equivalente), se procederá a ejecutar los cambios, versionar, comitear y realizar el push a GitHub con su etiqueta correspondiente.
