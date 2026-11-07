## 🛠️ GeoSilence v1.1.4 - Incrustación Directa de Leaflet y Panel de Diagnóstico en Mapa

Esta versión (**v1.1.4**) corrige de forma definitiva la visualización del mapa en blanco en la pestaña Ubicación al incrustar el código fuente de Leaflet 1.9.4 directamente en el HTML desde los assets locales, independizar el ciclo de renderizado de `window.onload` y habilitar un panel visual de diagnóstico de errores en pantalla.

---

### 🐛 Correcciones y Mejoras

* **📦 Leaflet Incrustado Directamente en el HTML:** Lectura en tiempo de ejecución de `leaflet.js` y `leaflet.css` desde `app/src/main/assets/leaflet/` e incrustación en línea dentro del documento HTML del WebView, eliminando cualquier problema de interceptación de red o carga asíncrona de scripts.
* **⚡ Inicialización Inmediata del Mapa:** Desacoplamiento del evento `window.onload` ejecutando `initMap()` inmediatamente dentro de un bloque protegido contra fallos (`try/catch`), garantizando que el mapa se dibuje sin demoras.
* **🚨 Panel de Diagnóstico de Errores Visible (`#mapError`):** Panel superpuesto en el mapa con captura global de excepciones (`window.onerror`) que alerta inmediatamente si Leaflet o las capas de OpenStreetMap fallan.
* **🛡️ Interceptor de Respaldo Redundante:** Mantenimiento de `shouldInterceptRequest` en `InteractiveMapView.kt` hacia `https://geosilence.app/leaflet/` como mecanismo de contingencia si los assets no pudieran leerse en memoria.
* **🤖 Directrices Actualizadas:** Sincronización de `AGENTS.md` y `agents.md` documentando la arquitectura de incrustación de Leaflet y diagnóstico de errores.

---

### 📦 Artefactos de Compilación
* **APK Generado:** `GeoSilence-debug.apk` adjunto a esta release, compilado automáticamente mediante GitHub Actions con JDK 17.
