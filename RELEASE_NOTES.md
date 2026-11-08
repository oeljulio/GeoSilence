## 🛠️ GeoSilence v1.2.0 - Selector de zona a pantalla completa y bloqueo dentro/fuera

Esta versión (**v1.2.0**) saca el mapa de la pestaña **Ubicación** a un selector a pantalla completa, añade la opción de bloquear **dentro** o **fuera** de la zona y hace que los avisos de permisos se actualicen solos.

---

### 🚀 Nuevas Funcionalidades

* **🗺️ Selector de zona a pantalla completa (`MapPickerScreen`):**
  * La pestaña **Ubicación** ya no incrusta el mapa: muestra un resumen (nombre, coordenadas y radio) y un botón *Elegir en el mapa* / *Cambiar en el mapa*.
  * Mapa OpenStreetMap con Leaflet 1.9.4 incrustado desde los assets locales, botón *Mi ubicación*, slider de 50 m a 3.000 m y atajos (100 m, 250 m, 500 m, 1 km, 2 km).
  * *Listo* aplica la posición y el radio al borrador; cerrar o el botón atrás descarta los cambios.
* **🎚️ Bloqueo dentro o fuera de la zona:** nueva opción `GeofenceZone.blockInside` con dos casillas mutuamente excluyentes. `AppPreferences.shouldBlockNow()` compara `isInsideZone` con ese valor y es lo que consulta `NotificationBlockerService`.
* **📍 `LocationHelper`:** comprueba el permiso de ubicación y obtiene la posición actual con `FusedLocationProviderClient` (respaldo a `lastLocation` y tiempo máximo acotado) para centrar el mapa y recalcular si estás dentro o fuera al guardar.
* **✅ Guardado con confirmación:** la zona se da de alta como geocerca solo al guardar, el botón indica si hay cambios sin guardar y el resultado se informa en pantalla.

### 🐛 Correcciones

* **🔔 Aviso de notificaciones siempre actualizado:** el aviso de permisos y la tarjeta de estado se refrescan cada segundo mientras la app está abierta, por lo que desaparecen al conceder el permiso sin reiniciar.
* **🛠️ Ayuda para ajustes restringidos:** si Android sigue denegando el acceso a notificaciones, el aviso explica la ruta (*Info. de la app → ⋮ → Permitir ajustes restringidos*) y ofrece un botón directo a la pantalla de información de la app.
* **🏷️ Versión en Ajustes:** la pantalla de Ajustes muestra la versión real desde `BuildConfig.VERSION_NAME` en lugar de un texto fijo desactualizado.

### 🔧 Mejoras y Ajustes Técnicos

* **🧭 Tarjeta de estado con modo:** muestra *bloqueando* / *permitiendo* / *desactivado* según el modo configurado e indica si estás dentro o fuera de la zona.
* **🗺️ Ajustes del mapa:** control de zoom arriba a la derecha, encuadre automático del círculo (`window.fitCircle`) y `window.setLocation` que solo recentra cuando el punto queda fuera de los límites visibles.
* `InteractiveMapView.kt` queda como host mínimo del `WebView`; `buildConfig = true` en `app/build.gradle.kts`.

---

### 📦 Artefactos de Compilación
* **APK Generado:** `GeoSilence-debug.apk` adjunto a esta release, compilado automáticamente mediante GitHub Actions con JDK 17.
