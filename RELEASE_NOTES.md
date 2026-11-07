## 🛠️ GeoSilence v1.1.2 - Corrección de Navegación, Interacción Táctil y Directrices de Desarrollo

Esta actualización de mantenimiento (**v1.1.2**) soluciona el truncamiento de texto en la barra de navegación, perfecciona la interacción táctil en el mapa embebido y formaliza las directrices de desarrollo para asistentes de IA en el repositorio.

---

### 🐛 Correcciones y Mejoras

* **📱 Interfaz de Usuario:** Implementación de `ScrollableTabRow` con `TextOverflow.Clip` en `MainScreen`, garantizando que todos los nombres de pestañas ("Ubicación", etc.) se visualicen completos sin saltos de línea ni truncamiento.
* **👆 Interacción Táctil en Mapa:** Optimización en la gestión de eventos táctiles (`requestDisallowInterceptTouchEvent`) en `InteractiveMapView`, permitiendo paneo y zoom fluidos sobre Leaflet / OpenStreetMap sin interferir con el scroll vertical de Compose.
* **🗺️ Renderizado de Mapa:** Eliminación de cabeceras bloqueadas por OpenStreetMap para asegurar la carga inmediata de teselas cartográficas.
* **🤖 Directrices de Arquitectura (`AGENTS.md`):** Incorporación de directrices técnicas de desarrollo, convenciones de Git y protocolo formal de auditoría y release.

---

### 📦 Artefactos de Compilación
* **APK Generado:** `GeoSilence-debug.apk` adjunto a esta release, compilado automáticamente mediante GitHub Actions con JDK 17.
