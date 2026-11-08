## 🛠️ GeoSilence v1.1.0 - Mapa interactivo, limpieza automática y ajustes

Esta versión (**v1.1.0**) añade la selección de la zona de silencio sobre un mapa interactivo, la purga automática del historial de notificaciones y una pantalla de ajustes. Además deja definitivamente resuelto el problema del mapa en blanco de la pestaña Ubicación.

---

### 🚀 Nuevas Funcionalidades

* **🗺️ Zona geográfica en mapa interactivo:**
  * Mapa OpenStreetMap con Leaflet 1.9.4 empaquetado e incrustado desde los assets locales de la app.
  * Selección del centro de la zona tocando el mapa o arrastrando el pin rojo.
  * Radio ajustable de 50 m a 3.000 m con slider continuo y atajos (100 m, 250 m, 500 m, 1 km, 2 km).
  * Dibujo en tiempo real del círculo que representa la geocerca.
* **🧹 Auto-eliminación del historial:** limpieza periódica con `WorkManager` según el ciclo elegido (Diario, Semanal o Nunca) y purga manual inmediata con recuento de registros eliminados.
* **⚙️ Pantalla de Ajustes:** retención de historial, accesos directos a permisos del sistema e información de versión.

### 🐛 Correcciones

* **🗺️ Mapa en blanco:** el contenedor `#map` no tenía altura computable. Ahora se posiciona de forma absoluta sobre todo el viewport y su altura se fuerza por JavaScript con `window.innerHeight`, recalculando Leaflet con `map.invalidateSize()` tras crear el mapa y al redimensionar.
* **🔁 Recursión al inicializar el mapa:** `forceMapSize` lleva una bandera anti-recursión, `initMap` se ejecuta una única vez y el tamaño se aplica después de crear `L.map()`, evitando el desbordamiento de pila.
* **🛰️ Teselas de OpenStreetMap:** se eliminan las cabeceras personalizadas que provocaban bloqueos del servidor y el mapa quedaba vacío.
* **📱 Pestañas:** `ScrollableTabRow` para mostrar los títulos completos sin truncar.

---

### 📦 Artefactos de Compilación
* **APK Generado:** `GeoSilence-debug.apk` adjunto a esta release, compilado automáticamente mediante GitHub Actions con JDK 17.
