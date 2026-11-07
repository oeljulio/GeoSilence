# 📋 Historial de Cambios (Changelog) - GeoSilence

Todos los cambios notables en este proyecto se documentan en este archivo, siguiendo el formato de [Keep a Changelog](https://keepachangelog.com/es-ES/1.0.0/) y versionado semántico [SemVer](https://semver.org/).

---

## [1.1.5] - 2026-11-07

### 🔧 Cambios Técnicos
* **🐛 Diagnóstico Temporal del WebView del Mapa:** Añadidos logs de depuración en `InteractiveMapView.kt` (`debugInfo`, `WebView.setWebContentsDebuggingEnabled`, captura de errores JS, `onReceivedError`, `onRenderProcessGone`, script de diagnóstico en `onPageFinished`) para investigar mapa en blanco en la pestaña Ubicación.

---

## [1.1.4] - 2026-11-07

### 🐛 Correcciones y Mejoras
* **📦 Leaflet Incrustado en el HTML desde Assets:** Lectura directa e incrustación inline de `leaflet.js` y `leaflet.css` desde los assets nativos (`app/src/main/assets/leaflet/`) dentro del HTML generado para el `WebView`, eliminando la dependencia de peticiones externas y resolviendo el mapa en blanco.
* **⚡ Inicialización Inmediata sin Depender de `window.onload`:** Inicialización directa de `initMap()` mediante bloque `try/catch`, asegurando el renderizado instantáneo del mapa sin esperar al evento `onload` de la ventana.
* **🚨 Panel de Errores Visible sobre el Mapa:** Inclusión de un contenedor de diagnóstico (`#mapError`) en pantalla y controlador global `window.onerror` para registrar y visualizar cualquier error de JavaScript o fallo de carga de Leaflet.
* **🛡️ Interceptor de Respaldo (`geosilence.app`):** Conservación íntegra de `shouldInterceptRequest` hacia `https://geosilence.app/leaflet/` como mecanismo de contingencia si no se proporcionan los assets en memoria.

---

## [1.1.3] - 2026-11-07

### 🐛 Correcciones y Mejoras
* **📦 Carga Local de Leaflet y Modo Offline:** Empaquetado completo de Leaflet 1.9.4 (`leaflet.js` y `leaflet.css`) en los assets nativos de la aplicación (`app/src/main/assets/leaflet/`) y servicio local en el WebView mediante interceptación de peticiones (`shouldInterceptRequest` hacia `https://geosilence.app/leaflet/`), eliminando la dependencia de CDN externa y resolviendo de forma definitiva el problema del mapa en blanco.
* **🔍 Depuración en WebView:** Asignación de `WebChromeClient` en `InteractiveMapView` con redirección de mensajes de JavaScript a logs de Logcat (`Log.d("GeoMap", ...)`).

### 📚 Documentación y Directrices
* **🤖 Actualización de `AGENTS.md`:** Documentada la estructura de `assets/leaflet/`, la entrega local mediante interceptación y directrices de integridad para prevenir dependencias de CDN.

---

## [1.1.2] - 2026-11-07

### 🐛 Correcciones y Mejoras
* **📱 Interfaz de Usuario:** Sustituido `TabRow` por `ScrollableTabRow` en `MainScreen` para mostrar títulos de pestañas completos sin cortes ni truncamiento ("Ubicación", etc.).
* **👆 Interacción Táctil en Mapa:** Simplificación de la lógica de interceptación táctil en `InteractiveMapView` para permitir desplazamiento y zoom fluidos en Leaflet sin colisiones con el scroll padre de Compose.
* **🗺️ Renderizado de Mapa:** Optimización en la carga de teselas cartográficas de OpenStreetMap eliminando bloqueos de cabecera.

### 📚 Documentación y Directrices
* **🤖 Directrices de IA y Protocolo de Release:** Incorporado `AGENTS.md` con especificación técnica de la aplicación, estándares de desarrollo, arquitectura y protocolo formal de auditoría y gestión de releases.

---

## [1.1.1] - 2026-11-06

### 🐛 Correcciones
* **🗺️ Renderizado de Mapa:** Eliminado el `User-Agent` personalizado para evitar bloqueos del servidor de teselas de OpenStreetMap, solucionando el problema del mapa en blanco.
* **👆 Interacción Táctil:** Simplificada la lógica de interceptación de toques en el WebView para permitir un desplazamiento y zoom fluidos sin interferencias del scroll de Compose.
* **📱 Interfaz de Usuario:** Sustituido `TabRow` por `ScrollableTabRow` para evitar el truncamiento del texto en las pestañas (solucionando el problema de "ubicaci...").

---

## [1.1.0] - 2026-11-06

### 🚀 Nuevas Funcionalidades
* **🗺️ Selección de Zona Geográfica en Mapa Interactivo con Radio Configurable**:
  * **Interfaz de Mapa:** Integración de vista cartográfica interactiva basada en OpenStreetMap y Leaflet mediante `InteractiveMapView`. Permite ubicar la zona central tocando cualquier punto del mapa o arrastrando el pin directamente.
  * **Ajuste de Radio:** Control de precisión mediante slider continuo (de 50 m a 3.000 m) y selectores rápidos tipo chips (100 m, 250 m, 500 m, 1 km, 2 km) con formateo dinámico en metros y kilómetros.
  * **Visualización Dinámica:** Dibujo en tiempo real sobre el mapa de la cobertura circular representativa de la geocerca de silencio, recalculada al instante al desplazar el slider o arrastrar el pin.
  * **Interacción Táctil Optimizada:** Aislamiento de eventos gestuales en el WebView para paneo y pinch-to-zoom sin conflictos con el desplazamiento vertical de Jetpack Compose.
* **🧹 Auto-eliminación del Historial de Notificaciones**:
  * **Lógica de Limpieza en 2do Plano:** Implementación de tareas programadas con `WorkManager` (`HistoryCleanupWorker` y `HistoryCleanupScheduler`) que evalúan y depuran periódicamente los registros de notificaciones según su antigüedad.
  * **Selector de Frecuencia:** Nueva opción en la configuración con tres ciclos disponibles:
    * **Diario:** Descarta registros con más de 24 horas de antigüedad.
    * **Semanal (predeterminado):** Descarta registros con más de 7 días.
    * **Nunca:** Conserva la totalidad del historial sin límite de tiempo.
  * **Persistencia y Rendimiento:** Almacenamiento persistente de la preferencia en `AppPreferences` y ejecución en corrutinas asíncronas (`Dispatchers.IO`) para garantizar fluidez absoluta de la aplicación.
  * **Acción Inmediata:** Botón para forzar una limpieza manual en cualquier momento con reporte numérico de registros eliminados.
* **⚙️ Pantalla de Ajustes y Banner de Estado**:
  * Incorporación de una cuarta pestaña (`Ajustes`) en la interfaz principal para concentrar opciones de retención de datos, accesos directos a permisos de sistema y detalles de la aplicación.
  * Banner informativo contextual en la vista de Historial que refleja la directiva de retención activa y enlaza directamente a su configuración.

### 🔧 Mejoras y Ajustes Técnicos
* Agregados los permisos `android.permission.INTERNET` y `android.permission.ACCESS_NETWORK_STATE` en `AndroidManifest.xml` para la descarga de teselas cartográficas.
* Incorporada la dependencia `androidx.work:work-runtime-ktx:2.9.1` en `libs.versions.toml` y `build.gradle.kts`.
* Actualizado `versionCode` a `2` y `versionName` a `"1.1.0"`.
* Configurada la integración de `RELEASE_NOTES.md` en el pipeline de GitHub Actions (`build-apk.yml`) para acompañar automáticamente los binarios generados.

---

## [1.0.0] - 2026-11-06

### ✨ Versión Inicial
* Geofencing mediante la API de Google Play Services con soporte para entrada, permanencia y salida de zonas geográficas.
* Servicio `NotificationListenerService` para la interceptación y cancelación de notificaciones en tiempo real cuando el dispositivo se encuentra dentro del área de silencio.
* Selección granular de aplicaciones bloqueadas con buscador integrado y persistencia local.
* Registro e historial local de eventos de bloqueo con marcas de tiempo.
* Modo de simulación manual para pruebas sin desplazamiento físico.
* Pipeline automatizado de CI/CD en GitHub Actions para compilación de APKs de depuración y publicación de releases al empujar etiquetas Git.
