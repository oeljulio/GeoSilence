## 🛠️ GeoSilence v1.1.1 - Corrección de Renderizado y Mejoras de Interfaz

Esta actualización de mantenimiento (**v1.1.1**) soluciona fallos detectados en el renderizado cartográfico y optimiza la navegación táctil y visual de la aplicación.

---

### 🐛 Correcciones y Mejoras

* **🗺️ Renderizado de Mapa:** Eliminación del `User-Agent` personalizado para evitar bloqueos por parte del servidor de mapas, resolviendo el problema de la pantalla en blanco.
* **👆 Interacción Táctil:** Optimización de la gestión de eventos táctiles en la vista del mapa para asegurar un desplazamiento y zoom fluidos.
* **📱 Interfaz de Usuario:** Implementación de `ScrollableTabRow` para garantizar que los nombres de las pestañas (como "Ubicación") se muestren completos sin truncamiento.

---

### 📦 Artefactos de Compilación
* **APK Generado:** `GeoSilence-debug.apk` adjunto a esta release, compilado automáticamente mediante GitHub Actions.
