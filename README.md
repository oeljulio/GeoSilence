# 📍 GeoSilence - Bloqueador de Notificaciones por Ubicación

[![Build Android APK](https://github.com/oeljulio/GeoSilence/actions/workflows/build-apk.yml/badge.svg)](https://github.com/oeljulio/GeoSilence/actions/workflows/build-apk.yml)
[![Version](https://img.shields.io/badge/version-v1.1.0-blue.svg)](https://github.com/oeljulio/GeoSilence/releases)

**GeoSilence** es una aplicación nativa para Android diseñada para silenciar o descartar automáticamente las notificaciones de aplicaciones seleccionadas (como WhatsApp, Slack, redes sociales o correo) cuando te encuentras en un área geográfica determinada (como tu oficina, escuela, universidad o casa).

---

## 🚀 Características Principales

* **🗺️ Mapa Interactivo con Cobertura Dinámica:**
  * Vista cartográfica con OpenStreetMap y Leaflet 1.9.4 incrustado directamente en el `WebView` desde los assets locales de la app (`app/src/main/assets/leaflet/`), sin dependencia de CDNs.
  * Selección de la ubicación central mediante toque en el mapa o arrastre directo del pin.
  * Ajuste de radio de 50 m a 3.000 m con slider continuo y botones de acceso rápido (100 m, 250 m, 500 m, 1 km, 2 km).
  * Dibujo en tiempo real del área circular que representa la zona de geofencing sobre el mapa.
* **Geofencing Inteligente de Bajo Consumo:** Define tu zona usando la API de Google Play Services con transiciones eficientes (Enter / Dwell / Exit).
* **🧹 Auto-eliminación del Historial de Notificaciones:**
  * Limpieza periódica en segundo plano mediante `WorkManager` sin ralentizar la interfaz.
  * Tres ciclos de retención configurables: **Diario**, **Semanal** *(predeterminado)* o **Nunca**.
  * Opción de purga manual inmediata.
* **Acceso a Notificaciones (`NotificationListenerService`):** Cancela las notificaciones no deseadas en el instante exacto en que se reciben si te encuentras dentro del perímetro.
* **Selector Granular de Apps:** Lista las aplicaciones instaladas con buscador y switches independientes.
* **Historial Detallado:** Registro local de aplicaciones y notificaciones bloqueadas.
* **Pestaña de Ajustes:** Gestión centralizada de retención, atajos a permisos y detalles de versión.
* **Modo de Prueba:** Botón para alternar manualmente "Dentro de Zona" / "Fuera de Zona" para testeo sin desplazamiento físico.
* **CI/CD Automatizado con GitHub Actions:** Compilación automática del APK de depuración y publicación de releases al empujar etiquetas `vX.Y.Z`.

---

## 📱 Requisitos

| Requisito | Valor |
|---|---|
| Android mínimo | 8.0 Oreo (`minSdk 26`) |
| Android de compilación | API 35 (`compileSdk 35`, `targetSdk 35`) |
| Lenguaje | Kotlin 2.0.20 (JVM target 17) |
| JDK para compilar | 17 |

---

## ⚙️ Permisos requeridos y Configuración en Android

Para que la app funcione correctamente en segundo plano, se gestionan los siguientes permisos:

1. **Acceso a Notificaciones:**
   * Requiere el permiso `BIND_NOTIFICATION_LISTENER_SERVICE` otorgado en *Ajustes > Acceso a notificaciones*.
2. **Ubicación en segundo plano:**
   * Permisos `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` y `ACCESS_BACKGROUND_LOCATION` para monitoreo continuo con pantalla apagada.
3. **Acceso a Red / Internet:**
   * Permisos `INTERNET` y `ACCESS_NETWORK_STATE` para la descarga dinámica de teselas cartográficas en el mapa.
4. **Notificaciones y reinicio:**
   * `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`, `WAKE_LOCK` y `RECEIVE_BOOT_COMPLETED` para restaurar las geocercas tras reiniciar el dispositivo.
5. **Optimización de Batería:**
   * Exclusión de optimización de batería para evitar que Android suspenda los servicios en segundo plano.

---

## 🛠️ Compilación

El APK **no se compila en local**: el proyecto se desarrolla en Termux y la compilación se delega a GitHub Actions (`.github/workflows/build-apk.yml`, JDK 17 + `./gradlew assembleDebug`).

* **Push a `main` o pull request:** genera el APK de depuración y lo adjunta como artifact `GeoSilence-Debug-APK`.
* **Push de un tag `vX.Y.Z`:** además crea la release de GitHub con `GeoSilence-debug.apk` y el contenido de `RELEASE_NOTES.md`.

Flujo de release:

```bash
# 1. Actualizar versionName/versionCode en app/build.gradle.kts,
#    CHANGELOG.md y RELEASE_NOTES.md
git add -A
git commit -m "chore: release vX.Y.Z"
git push origin main
git tag vX.Y.Z
git push origin vX.Y.Z
```

---

## 🧱 Estructura del Proyecto

```
miapp/
├── .github/workflows/build-apk.yml    # CI/CD para compilar APK y publicar releases en tags v*
├── AGENTS.md                          # Convenciones y reglas de desarrollo
├── CHANGELOG.md                       # Registro histórico de versiones y cambios
├── RELEASE_NOTES.md                   # Notas de la versión actual para la release
├── app/
│   ├── build.gradle.kts               # versionCode / versionName
│   └── src/main/
│       ├── AndroidManifest.xml        # Declaración de permisos, actividades y servicios
│       ├── assets/leaflet/            # Recursos locales de Leaflet 1.9.4 (JS y CSS)
│       ├── java/com/personal/geosilence/
│       │   ├── MainActivity.kt        # Actividad principal y orquestación de permisos
│       │   ├── data/
│       │   │   ├── Models.kt          # Modelos de datos y ciclos de retención
│       │   │   └── AppPreferences.kt  # Persistencia y auto-eliminación
│       │   ├── location/
│       │   │   └── GeofenceManager.kt # Gestión de geocercas GPS con Play Services
│       │   ├── service/
│       │   │   ├── NotificationBlockerService.kt # Listener de notificaciones
│       │   │   ├── GeofenceBroadcastReceiver.kt  # Eventos de entrada/salida
│       │   │   ├── BootReceiver.kt               # Restaura geocercas y tareas tras reiniciar
│       │   │   ├── HistoryCleanupWorker.kt       # Worker periódico de WorkManager
│       │   │   └── HistoryCleanupScheduler.kt    # Programador de tareas de limpieza
│       │   └── ui/
│       │       ├── MainScreen.kt      # Pantalla principal con pestañas Compose
│       │       ├── SettingsScreen.kt  # Pantalla de Ajustes y retención de historial
│       │       ├── map/
│       │       │   ├── InteractiveMapView.kt # Componente Compose de mapa (WebView)
│       │       │   └── MapHtmlProvider.kt    # HTML/JS de Leaflet y OpenStreetMap
│       │       └── theme/             # Tema visual Material 3
│       └── res/                       # Iconos y recursos visuales
├── gradle/
│   ├── libs.versions.toml             # Catálogo de dependencias centralizado
│   └── wrapper/                       # Wrapper autónomo de Gradle
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 📄 Licencia

Proyecto personal de uso privado.
