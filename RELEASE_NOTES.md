## 🛠️ GeoSilence v1.1.8 - Forzar altura del mapa por JS y diagnóstico ampliado

Esta versión (**v1.1.8**) refuerza la corrección del mapa en blanco: el contenedor `#map` seguía midiendo `337x0`, así que ahora su altura se fuerza por JavaScript con prioridad máxima y el diagnóstico `[DEBUG]` muestra más información.

---

### 🐛 Correcciones

* **Forzar altura del mapa por JS y diagnóstico ampliado:**
  * `#map`, `document.documentElement` y `document.body` reciben `height = window.innerHeight` con `style.setProperty(..., 'important')` antes de crear el mapa, en `resize` y vía `setTimeout` a 0, 300 y 1000 ms, seguido de `map.invalidateSize()`.
  * Se eliminan las alturas en `%` dependientes de un padre sin altura computable (`#map` parte de `280px` fijos hasta la asignación por JS).
  * El diagnóstico `[DEBUG]` inicia con `v=1.1.8` y añade `getComputedStyle` (height/position/display), el atributo `style` de `#map` (40 caracteres) y la altura de cada padre hasta `body`.

---

## 🛠️ GeoSilence v1.1.7 - Corrección de altura del mapa (height 0)

Esta versión (**v1.1.7**) corrige el mapa en blanco de la pestaña Ubicación: el diagnóstico mostraba `mapSize=337x0`, es decir, Leaflet cargaba correctamente pero el contenedor `#map` tenía altura cero.

---

### 🐛 Correcciones

* **🗺️ Corrección de altura del mapa (height 0):**
  * `html, body { height: 100%; margin: 0; padding: 0; }` y `#map` posicionado de forma absoluta (`top/left/right/bottom: 0`) con `width/height: 100%`, eliminando las reglas de altura previas que colapsaban el contenedor.
  * Recálculo del tamaño de Leaflet con `map.invalidateSize()` a los 0 ms y 300 ms tras crear el mapa, más `window.addEventListener('resize', ...)` para redimensionados del WebView.
* **📊 Diagnóstico `[DEBUG]`:** Se mantiene intacta la barra de diagnóstico en pantalla y el resto de la lógica del mapa.

---

## 🛠️ GeoSilence v1.1.5 - Diagnóstico Temporal del WebView del Mapa

Esta versión (**v1.1.5**) añade instrumentación de depuración en `InteractiveMapView.kt` para investigar el problema del mapa en blanco en la pestaña Ubicación, manteniendo la incrustación directa de Leaflet 1.9.4 desde assets.

---

### 🔧 Cambios Técnicos

* **🐛 Diagnóstico Temporal del WebView del Mapa:**
  * Estado `debugInfo` visible en la barra informativa bajo el mapa.
  * `WebView.setWebContentsDebuggingEnabled(true)` y color de fondo `#F1F3F4` para inspección remota.
  * Captura de errores de JavaScript (`WebChromeClient.onConsoleMessage`) con prefijo `JS-ERR`.
  * Registro de errores de carga de página principal (`WebViewClient.onReceivedError`) con prefijo `ERR-PAGINA`.
  * Detección de colapso del renderizador (`WebViewClient.onRenderProcessGone`) con prefijo `RENDERER-CAIDO`.
  * Script de diagnóstico en `onPageFinished` que reporta: `typeof L`, className de `#map`, dimensiones del contenedor y ventana, texto de `#mapError` y versión de Chrome; ejecutado al cargar y repetido a los 2 segundos.

---

## 🛠️ GeoSilence v1.1.6 - Diagnóstico WebView v2

Esta versión (**v1.1.6**) mejora el sistema de diagnóstico del WebView con un script más robusto y completas capacidades de seguimiento.

---

### 🔧 Mejoras

* **📊 Diagnóstico WebView v2:** Script de diagnóstico en `onPageFinished` envuelto en IIFE que siempre devuelve una cadena, capturando `location.href`, `readyState`, `typeof L`, estado del mapa (`sí`/`no`), dimensiones, longitud del HTML y texto de `#mapError`. Se añadió acumulación de `debugInfo` sin sobrescribir lecturas previas.
* **📌 Seguimiento de Carga:** Se añadió registro de URL en `onPageStarted` (`start:`) y `onPageFinished` (`fin:`), con la URL truncada a 40 caracteres para mantener la legibilidad en la barra informativa.
* **🔒 Acumulación de Diagnóstico:** El `debugInfo` ahora acumula resultados anteriores en lugar de sobrescribirlos, permitiendo historial de diagnósticos a lo largo del tiempo.

---

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
