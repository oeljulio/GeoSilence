## 🛠️ GeoSilence v1.1.3 - Empaquetado Local de Leaflet en Assets y Servicio Offline en WebView

Esta versión (**v1.1.3**) soluciona de forma definitiva el problema del mapa en blanco en la pestaña Ubicación al empaquetar Leaflet 1.9.4 dentro de los assets nativos de la aplicación y servirlos localmente mediante interceptación de peticiones en el WebView.

---

### 🐛 Correcciones y Mejoras

* **📦 Empaquetado Local de Leaflet (Sin CDN Externa):** Leaflet 1.9.4 (`leaflet.js` y `leaflet.css`) empaquetados en `app/src/main/assets/leaflet/`, evitando bloqueos de red, DNS o caídas de CDN que provocaban que el mapa se mostrara en blanco.
* **⚡ Interceptación Local en WebView (`shouldInterceptRequest`):** Configurado `WebViewClient` para interceptar peticiones hacia `https://geosilence.app/leaflet/` y servirlas directamente desde `ctx.assets.open(...)` con tipo MIME adecuado y manejo robusto de excepciones.
* **🔍 Diagnóstico y Logs de Consola:** Integración de `WebChromeClient` en `InteractiveMapView` para reenviar mensajes de la consola JavaScript a Logcat con la etiqueta `"GeoMap"`.
* **🤖 Sincronización y Actualización de Directrices:** Actualización de `AGENTS.md` (y su enlace simbólico `agents.md`) documentando la arquitectura de recursos locales y las reglas de integridad para preservar la carga offline.

---

### 📦 Artefactos de Compilación
* **APK Generado:** `GeoSilence-debug.apk` adjunto a esta release, compilado automáticamente mediante GitHub Actions con JDK 17.
