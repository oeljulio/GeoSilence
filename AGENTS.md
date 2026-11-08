# AGENTS.md — GeoSilence

## Resumen del proyecto

GeoSilence es una app nativa de Android que silencia y descarta las notificaciones de las aplicaciones seleccionadas por el usuario cuando el dispositivo está dentro **o fuera** de una geocerca definida sobre un mapa (oficina, casa, universidad…), según el comportamiento elegido. El usuario elige el centro y el radio de la zona en un selector a pantalla completa, el modo de bloqueo (dentro / fuera), las apps a bloquear y el ciclo de retención del historial.

## Stack y versiones

| Componente | Versión |
|---|---|
| Kotlin | 2.0.21 (JVM target 17) |
| Android Gradle Plugin | 8.5.2 |
| Gradle Wrapper | 8.7 |
| compileSdk / targetSdk | 35 |
| minSdk | 26 (Android 8.0) |
| Jetpack Compose | BOM 2024.10.00, Material 3 + Material Icons Extended |
| Google Play Services Location | 21.3.0 (`GeofencingClient` y `FusedLocationProviderClient`) |
| WorkManager | 2.9.1 |
| Gson | 2.11.0 (persistencia en `SharedPreferences`) |
| Leaflet | 1.9.4, empaquetado en `app/src/main/assets/leaflet/` |

Versión actual de la app: **1.2.0** (`versionCode 12`, `versionName "1.2.0"` en `app/build.gradle.kts`).

Lógica de bloqueo: `GeofenceZone.blockInside` (por defecto `true`; el JSON antiguo de Gson sigue cargando) decide si se bloquea dentro de la zona o fuera de ella. `AppPreferences.shouldBlockNow()` compara `isInsideZone` con el `blockInside` de la zona guardada y es lo que consulta `NotificationBlockerService`.

Servicios del sistema: `NotificationListenerService` (`BIND_NOTIFICATION_LISTENER_SERVICE`), `GeofenceBroadcastReceiver` (ENTER / EXIT / DWELL), `BootReceiver` (restaura geocercas y tareas tras reiniciar) y `HistoryCleanupWorker` / `HistoryCleanupScheduler` sobre `WorkManager`.

Permisos declarados en el manifest: `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`, `INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`, `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, `QUERY_ALL_PACKAGES`.

## Estructura de carpetas

```
.
├── .github/workflows/build-apk.yml   # CI/CD: compila APK y publica release en tags v*
├── app/
│   ├── build.gradle.kts              # versionCode / versionName
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/leaflet/           # leaflet.js y leaflet.css (1.9.4)
│       ├── java/com/personal/geosilence/
│       │   ├── MainActivity.kt       # Permisos en runtime + punto de entrada Compose
│       │   ├── data/                 # Models.kt (GeofenceZone con blockInside,
│       │   │                         #   InstalledAppInfo, BlockedNotificationRecord,
│       │   │                         #   HistoryRetentionCycle)
│       │   │                         # AppPreferences.kt (SharedPreferences + Gson,
│       │   │                         #   shouldBlockNow())
│       │   ├── location/             # GeofenceManager.kt (registro de geocercas)
│       │   │                         # LocationHelper.kt (permiso y posición actual)
│       │   ├── service/              # NotificationBlockerService, GeofenceBroadcastReceiver,
│       │   │                         #   BootReceiver, HistoryCleanupWorker,
│       │   │                         #   HistoryCleanupScheduler
│       │   └── ui/
│       │       ├── MainScreen.kt     # Pestañas (Ubicación, Apps, Historial, Ajustes),
│       │       │                     #   borrador de zona y tarjeta de estado
│       │       ├── MapPickerScreen.kt # Selector de zona a pantalla completa
│       │       ├── SettingsScreen.kt
│       │       ├── map/InteractiveMapView.kt  # Host del WebView del mapa en Compose
│       │       ├── map/MapHtmlProvider.kt     # HTML/CSS/JS de Leaflet
│       │       └── theme/            # Color, Type, Theme (Material 3)
│       └── res/                      # Iconos, strings, colores, tema
├── gradle/libs.versions.toml         # Catálogo de versiones
├── build.gradle.kts, settings.gradle.kts, gradle.properties
├── CHANGELOG.md, RELEASE_NOTES.md
└── README.md
```

## Compilación y CI

**Este proyecto no se compila en local.** El entorno de desarrollo es Termux sobre Android y no dispone de JDK 17 ni Android SDK.

La compilación se delega a GitHub Actions (`.github/workflows/build-apk.yml`), que se dispara al:

- hacer push a `main` / `master` o abrir un pull request → genera y adjunta el APK de depuración como artifact;
- subir un tag `v*` → además crea la GitHub Release con `GeoSilence-debug.apk` y el contenido de `RELEASE_NOTES.md` como cuerpo.

El workflow usa JDK 17 (Temurin) y ejecuta `./gradlew assembleDebug --stacktrace`.

## Proceso de release

1. Subir `versionName` y `versionCode` en `app/build.gradle.kts` (el `versionCode` nunca baja).
2. Actualizar `CHANGELOG.md` con la entrada de la nueva versión y `RELEASE_NOTES.md` con las notas que se publicarán en la release.
3. Actualizar la tabla de versiones de este archivo y del `README.md` si cambian.
4. `git add -A` y commit con Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`…).
5. `git push origin main`.
6. Crear y subir el tag: `git tag vX.Y.Z && git push origin vX.Y.Z`.

Solo se crea el tag cuando el usuario lo indica expresamente, ya que dispara la publicación de la release.

## Convenciones de código

- Kotlin idiomático, tipado fuerte y responsabilidad única: UI (Compose), datos (`AppPreferences` / `Models`), geolocalización (`GeofenceManager`) y servicios en segundo plano claramente separados.
- Manejo defensivo de errores con logs informativos (`Log.e`, `Log.w`, `Log.d`) en parsing de JSON, APIs del sistema, `WebView` y `SharedPreferences`.
- Eficiencia en batería: transiciones de geocerca mínimas y trabajo de fondo acotado en `WorkManager`.
- Comentarios solo cuando el motivo no sea evidente en el código; mantener los existentes si siguen siendo ciertos.
- Sin dependencias nuevas sin justificación: añadirlas primero al catálogo `gradle/libs.versions.toml`.

## Reglas para agentes

- **No ejecutar `./gradlew`** (ni `assembleDebug` ni `clean`) en Termux: no hay JDK/SDK. Se valida con revisión estática y el APK llega desde CI.
- **No cambiar la lógica del mapa sin un motivo concreto.** Ya funcionó a base de ajustes delicados; cualquier cambio debe ser mínimo y explicado.
- **Cambios mínimos y acotados** al problema solicitado: sin refactors ni "mejoras" colaterales.
- **No subir archivos temporales** (`prompt.txt`, notas sueltas, dumps, capturas de depuración). Van en `.gitignore`.
- No tocar `versionCode` / `versionName` ni los documentos de versión salvo que se pida.
- No crear tags ni hacer push de releases sin autorización explícita.
- No eliminar ni alterar `BootReceiver`, `HistoryCleanupWorker`, `HistoryCleanupScheduler` ni `NotificationBlockerService`.

## Notas del mapa

- `MapHtmlProvider.kt` genera el HTML: **el contenedor `#map` necesita altura explícita**. `html`, `body` y `#map` se posicionan y dimensionan a partir de `window.innerHeight` con `setProperty(..., 'important')`; sin altura computable Leaflet renderiza un lienzo de 0 px y el mapa aparece en blanco.
- `forceMapSize()` lleva la bandera `window.__forcing` para no reentrar en sí misma, y `initMap()` usa `initMapOnce` / `initDone` para ejecutarse una sola vez. **No quitar esas banderas**: provocaban `RangeError: Maximum call stack size exceeded` al inicializar.
- `L.map()` se crea antes de cualquier ajuste de tamaño; `invalidateSize()` se difiere con `setTimeout` y en `resize`/`orientationchange` solo se llama a `forceMapSize()`.
- Leaflet se incrusta inline desde `assets/leaflet/`, con `shouldInterceptRequest` hacia `https://geosilence.app/leaflet/` como respaldo y el panel `#mapError` para mostrar errores de JS.
- El mapa solo se monta dentro de `MapPickerScreen.kt`, a pantalla completa; la pestaña **Ubicación** muestra un resumen de la zona y abre el selector. `InteractiveMapView.kt` es únicamente el host: un `AndroidView` con el `WebView` a `fillMaxSize()` que, al terminar de cargar, aplica `setRadius` y luego `fitCircle` (con un `delay` de 400 ms) y destruye el `WebView` en `onRelease`.
- `window.setLocation` solo recentra si el punto queda fuera de los límites actuales (`getBounds().pad(-0.25)`); el encuadre inicial y el del botón *Mi ubicación* van por `window.fitCircle`, que hace `fitBounds` de los límites del círculo (`maxZoom: 17`, con `try/catch`).
- El control de zoom de Leaflet se coloca arriba a la derecha (`zoomControl: false` en `L.map` + `L.control.zoom({ position: 'topright' })`).
- La sincronización con Compose es bidireccional: `window.setLocation`, `window.setRadius`, `window.fitCircle` y `forceMapSize` desde Kotlin; `AndroidBridge.onLocationSelected` desde el JavaScript.
