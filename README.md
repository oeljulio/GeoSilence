# 📍 GeoSilence - Bloqueador de Notificaciones por Ubicación

[![Build Android APK](https://github.com/oeljulio/GeoSilence/actions/workflows/build-apk.yml/badge.svg)](https://github.com/oeljulio/GeoSilence/actions/workflows/build-apk.yml)
[![Version](https://img.shields.io/badge/version-v1.1.3-blue.svg)](https://github.com/oeljulio/GeoSilence/releases)

**GeoSilence** es una aplicación nativa para Android diseñada para silenciar o descartar automáticamente las notificaciones de aplicaciones seleccionadas (como WhatsApp, Slack, redes sociales o correo) cuando te encuentras en un área geográfica determinada (como tu oficina, escuela, universidad o casa).

---

## 🚀 Características Principales

* **🗺️ Mapa Interactivo con Cobertura Dinámica:**
  * Vista cartográfica fluida con OpenStreetMap y Leaflet integrada directamente en Jetpack Compose.
  * Selección de la ubicación central mediante toque en el mapa o arrastre directo del pin.
  * Ajuste de radio de 50 m a 3.000 m con slider continuo y botones de acceso rápido (100 m, 250 m, 500 m, 1 km, 2 km).
  * Dibujo en tiempo real del área circular que representa la zona de geofencing sobre el mapa.
* **Geofencing Inteligente de Bajo Consumo:** Define tu zona usando la API de Google Play Services con transiciones eficientes (Enter / Dwell / Exit).
* **🧹 Auto-eliminación del Historial de Notificaciones:**
  * Limpieza periódica en segundo plano mediante `WorkManager` sin ralentizar la interfaz.
  * Tres ciclos de retención configurables: **Diario**, **Semanal** *(predeterminado)* o **Nunca**.
  * Opción de purga manual inmediata.
* **Acceso a Notificaciones (`NotificationListenerService`):** Cancela las notificaciones no deseadas en el instante exacto en que se reciben si te encuentras dentro del perímetro.
* **Selector Granular de Apps:** Lista todas las aplicaciones instaladas con buscador y switches independientes.
* **Historial Detallado:** Registro local de aplicaciones y notificaciones bloqueadas.
* **Pestaña de Ajustes:** Gestión centralizada de retención, atajos a permisos y detalles de versión.
* **Modo de Prueba:** Botón para alternar manualmente "Dentro de Zona" / "Fuera de Zona" para testeo sin desplazamiento físico.
* **CI/CD Automatizado con GitHub Actions:** Compilación automática de APKs de depuración y publicación de releases al empujar etiquetas `v*`.

---

## ⚙️ Permisos requeridos y Configuración en Android

Para que la app funcione correctamente en segundo plano, se gestionan los siguientes permisos:

1. **Acceso a Notificaciones:**
   * Requiere el permiso `BIND_NOTIFICATION_LISTENER_SERVICE` otorgado en *Ajustes > Acceso a notificaciones*.
2. **Ubicación en segundo plano:**
   * Permisos `ACCESS_FINE_LOCATION` y `ACCESS_BACKGROUND_LOCATION` para monitoreo continuo con pantalla apagada.
3. **Acceso a Red / Internet:**
   * Permisos `INTERNET` y `ACCESS_NETWORK_STATE` para la descarga dinámica de teselas cartográficas en el mapa.
4. **Optimización de Batería:**
   * Exclusión de optimización de batería para evitar que Android suspenda los servicios en segundo plano.

---

## 🧱 Estructura del Proyecto

```
miapp/
├── .github/workflows/build-apk.yml    # CI/CD para compilar APK y publicar releases en GitHub
├── CHANGELOG.md                       # Registro histórico de versiones y cambios
├── RELEASE_NOTES.md                   # Notas de la versión actual para la release
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml        # Declaración de permisos, actividades y servicios
│   │   ├── assets/leaflet/            # Recursos locales de Leaflet 1.9.4 (JS y CSS)
│   │   ├── java/com/personal/geosilence/
│   │   │   ├── MainActivity.kt        # Actividad principal y orquestación de permisos
│   │   │   ├── data/
│   │   │   │   ├── Models.kt          # Modelos de datos y ciclos de retención
│   │   │   │   └── AppPreferences.kt  # Persistencia y auto-eliminación
│   │   │   ├── location/
│   │   │   │   └── GeofenceManager.kt # Gestión de geocercas GPS con Play Services
│   │   │   ├── service/
│   │   │   │   ├── NotificationBlockerService.kt # Listener de notificaciones
│   │   │   │   ├── GeofenceBroadcastReceiver.kt  # Eventos de entrada/salida
│   │   │   │   ├── BootReceiver.kt               # Restaura geocercas y tareas tras reiniciar
│   │   │   │   ├── HistoryCleanupWorker.kt       # Worker periódico de WorkManager
│   │   │   │   └── HistoryCleanupScheduler.kt    # Programador de tareas de limpieza
│   │   │   └── ui/
│   │   │       ├── MainScreen.kt      # Pantalla principal con pestañas Compose
│   │   │       ├── SettingsScreen.kt  # Pantalla de Ajustes y retención de historial
│   │   │       ├── map/
│   │   │       │   ├── InteractiveMapView.kt # Componente Compose de mapa
│   │   │       │   └── MapHtmlProvider.kt    # Motor Leaflet y OpenStreetMap
│   │   │       └── theme/             # Tema visual Material 3
│   │   └── res/                       # Iconos y recursos visuales
│   └── build.gradle.kts
├── gradle/
│   ├── libs.versions.toml             # Catálogo de dependencias centralizado
│   └── wrapper/                       # Wrapper autónomo de Gradle
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```
