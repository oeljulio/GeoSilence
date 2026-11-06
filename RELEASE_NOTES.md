## 🚀 GeoSilence v1.1.0 - Mapa Interactivo y Auto-limpieza de Historial

Nos complace presentar la versión **v1.1.0** de **GeoSilence**, incorporando un mapa interactivo para delimitar tus zonas de silencio con total precisión y un sistema inteligente de auto-eliminación periódica del historial de notificaciones.

---

### 🌟 Novedades Destacadas

#### 1. 🗺️ Selección de Zona Geográfica en Mapa con Cobertura Dinámica
* **Mapa Interactivo:** Integración de mapa completo con OpenStreetMap y Leaflet. Puedes seleccionar el centro de la zona de silencio tocando cualquier lugar o arrastrando el pin directamente.
* **Control de Radio en Metros y Kilómetros:** Deslizador continuo interactivo (50 m - 3.000 m) y botones de acceso rápido (100 m, 250 m, 500 m, 1 km, 2 km).
* **Visualización Dinámica en Tiempo Real:** Visualiza sobre el mapa una cobertura circular semitransparente que se redimensiona al instante mientras ajustas el radio o mueves el pin.

#### 2. 🧹 Auto-eliminación Periódica del Historial de Notificaciones
* **Limpieza en Segundo Plano:** Tareas periódicas gestionadas con `WorkManager` que depuran registros obsoletos sin gastar batería ni ralentizar la aplicación.
* **Selector de Frecuencia Configurable:**
  * **Diario:** Descarta notificaciones con más de 24 horas de antigüedad.
  * **Semanal (predeterminado):** Descarta registros con más de 7 días.
  * **Nunca:** Preserva todo el historial de eventos.
* **Limpieza Inmediata Manual:** Posibilidad de ejecutar la purga al instante con confirmación del número de elementos eliminados.

#### 3. ⚙️ Nueva Pestaña de Ajustes
* Gestión centralizada de las preferencias de retención.
* Accesos rápidos para conceder permisos de acceso a notificaciones y deshabilitar optimizaciones de batería.
* Banner de estado en la pestaña de Historial que muestra el ciclo activo.

---

### 📦 Artefactos de Compilación
* **APK Generado:** `GeoSilence-debug.apk` adjunto a esta release, compilado mediante GitHub Actions con JDK 17 y Gradle 8.5.2.
