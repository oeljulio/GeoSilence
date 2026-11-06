## 🛠️ GeoSilence v1.1.1 - Corrección de Renderizado y Mejoras de Interfaz

Esta actualización de mantenimiento (**v1.1.1**) soluciona fallos detectados en el renderizado cartográfico y optimiza la navegación táctil y visual de la aplicación.

---

### 🐛 Correcciones y Mejoras

* **🗺️ Renderizado de Teselas de OpenStreetMap:** Configuración de `User-Agent` personalizado e invalidación de tamaño para resolver el lienzo en blanco y cumplir con las directivas del servidor de teselas cartográficas.
* **👆 Restauración del Desplazamiento Vertical:** Control direccional de gestos táctiles mediante `requestDisallowInterceptTouchEvent`, permitiendo hacer scroll vertical sin bloqueos sobre la vista del mapa cuando no se está arrastrando deliberadamente.
* **📱 Corrección de Desbordamiento en Pestañas:** Ajuste de línea única y truncamiento con `TextOverflow.Ellipsis` en la barra de navegación para evitar saltos de línea en la pestaña "Ubicación".

---

### 📦 Artefactos de Compilación
* **APK Generado:** `GeoSilence-debug.apk` adjunto a esta release, compilado automáticamente mediante GitHub Actions.
