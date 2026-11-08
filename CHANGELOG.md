## [1.3.0] - 2026-11-08

### Corregido
- **Optimización de Batería:** Implementada verificación inteligente con `PowerManager`. Si el permiso flotante nativo ya fue aceptado o la capa de personalización del fabricante exige configuración manual (Xiaomi, Huawei, etc.), la app abre directamente la pantalla de Detalles de la Aplicación.
- **Mapa / Selección de Zona:** Eliminado el botón preset sobrante de 2,0 km para mejorar la interfaz visual, manteniendo el rango completo en el deslizador (slider) hasta 2.0 km.
- **CI/CD Workflow:** Actualizadas las dependencias de GitHub Actions a `v5` para corregir los avisos de deprecación.

# 📋 Historial de Cambios (Changelog) - GeoSilence

Todos los cambios notables en este proyecto se documentan en este archivo, siguiendo el formato de [Keep a Changelog](https://keepachangelog.com/es-ES/1.0.0/) y versionado semántico [SemVer](https://semver.org/).

---

## [1.2.0] - 2026-11-08

### 🚀 Nuevas Funcionalidades
* **🗺️ Selector de zona a pantalla completa:** la pestaña **Ubicación** ya no incrusta el mapa; muestra un resumen de la zona (nombre, coordenadas y radio) y abre `MapPickerScreen`, una pantalla completa con el mapa, el botón *Mi ubicación*, el slider de radio (50 m a 3.000 m) y los accesos rápidos de 100 m a 2 km. *Listo* aplica el borrador y cerrar o el botón atrás lo descarta.
* **🎚️ Bloqueo dentro o fuera de la zona:** nueva opción por zona `GeofenceZone.blockInside` con dos casillas mutuamente excluyentes. `AppPreferences.shouldBlockNow()` compara `isInsideZone` con ese valor y es lo que consulta `NotificationBlockerService`.
* **📍 `LocationHelper`:** utilidad nueva que comprueba el permiso de ubicación y obtiene la posición actual con `FusedLocationProviderClient` (con respaldo a `lastLocation` y tiempo máximo acotado); se usa para centrar el mapa y para recalcular si estás dentro o fuera al guardar la zona.
* **✅ Guardado con confirmación:** la zona solo se da de alta como geocerca cuando se guarda, el botón indica si hay cambios sin guardar y el resultado se informa en la pantalla.

### 🐛 Correcciones
* **🔔 Aviso de notificaciones siempre actualizado:** el aviso de permisos y la tarjeta de estado se refrescan cada segundo mientras la app está abierta, por lo que desaparecen al conceder el permiso sin reiniciar.
* **🛠️ Ayuda para ajustes restringidos:** si Android sigue denegando el acceso a notificaciones, el aviso explica la ruta (*Info. de la app → ⋮ → Permitir ajustes restringidos*) y ofrece un botón directo a la pantalla de información de la app.

### 🔧 Mejoras y Ajustes Técnicos
* **🧭 Tarjeta de estado con modo:** muestra *bloqueando* / *permitiendo* / *desactivado* según el modo configurado e indica si estás dentro o fuera de la zona.
* **🗺️ Ajustes del mapa:** control de zoom arriba a la derecha, encuadre automático del círculo (`window.fitCircle`) y `setLocation` que solo recentra cuando el punto queda fuera de los límites visibles.
* `InteractiveMapView.kt` queda como host mínimo del `WebView`; la versión sube a `versionCode 12` / `versionName "1.2.0"`.

---

## [1.1.0] - 2026-11-08

### 🚀 Nuevas Funcionalidades
* **🗺️ Selección de Zona Geográfica en Mapa Interactivo con Radio Configurable**:
  * **Interfaz de Mapa:** Vista cartográfica interactiva basada en OpenStreetMap y Leaflet 1.9.4 empaquetado e incrustado desde los assets locales. Permite ubicar la zona central tocando cualquier punto del mapa o arrastrando el pin directamente.
  * **Ajuste de Radio:** Control de precisión mediante slider continuo (de 50 m a 3.000 m) y selectores rápidos tipo chips (100 m, 250 m, 500 m, 1 km, 2 km) con formateo dinámico en metros y kilómetros.
  * **Visualización Dinámica:** Dibujo en tiempo real sobre el mapa de la cobertura circular representativa de la geocerca de silencio, recalculada al instante al desplazar el slider o arrastrar el pin.
  * **Interacción Táctil Optimizada:** Aislamiento de eventos gestuales en el WebView para paneo y pinch-to-zoom sin conflictos con el desplazamiento vertical de Jetpack Compose.
* **🧹 Auto-eliminación del Historial de Notificaciones**:
  * **Lógica de Limpieza en 2do Plano:** Tareas programadas con `WorkManager` (`HistoryCleanupWorker` y `HistoryCleanupScheduler`) que evalúan y depuran periódicamente los registros de notificaciones según su antigüedad.
  * **Selector de Frecuencia:** Tres ciclos disponibles: **Diario** (más de 24 horas), **Semanal** (predeterminado, más de 7 días) y **Nunca** (historial completo).
  * **Persistencia y Rendimiento:** Preferencia persistida en `AppPreferences` y ejecución en corrutinas asíncronas (`Dispatchers.IO`).
  * **Acción Inmediata:** Botón para forzar una limpieza manual con reporte numérico de registros eliminados.
* **⚙️ Pantalla de Ajustes y Banner de Estado**:
  * Cuarta pestaña (`Ajustes`) con opciones de retención de datos, accesos directos a permisos del sistema y detalles de la aplicación.
  * Banner informativo en la vista de Historial que refleja la directiva de retención activa y enlaza a su configuración.

### 🐛 Correcciones
* **🗺️ Corrección del mapa en blanco:** el contenedor `#map` se dimensiona con altura explícita (posición absoluta sobre todo el viewport y altura forzada por JS con `window.innerHeight`), y `map.invalidateSize()` recalcula Leaflet tras crear el mapa y al redimensionar.
* **🔁 Corrección de la recursión al inicializar el mapa:** `forceMapSize` es no reentrante (bandera `__forcing`), `initMap` se ejecuta una única vez y el tamaño se aplica después de crear `L.map()`, evitando `RangeError: Maximum call stack size exceeded`.
* **📦 Leaflet incrustado en el HTML:** lectura de `leaflet.js` y `leaflet.css` desde `app/src/main/assets/leaflet/`, con el interceptor hacia `https://geosilence.app/leaflet/` como respaldo y panel `#mapError` para errores de JavaScript.
* **📱 Pestañas completas:** `ScrollableTabRow` en sustitución de `TabRow` para mostrar los títulos sin truncar.
* **🛰️ Teselas de OpenStreetMap:** peticiones sin cabeceras personalizadas que podían ser bloqueadas por el servidor de mapas.

### 🔧 Mejoras y Ajustes Técnicos
* Permisos `INTERNET` y `ACCESS_NETWORK_STATE` en `AndroidManifest.xml` para la descarga de teselas cartográficas.
* Dependencia `androidx.work:work-runtime-ktx` en el catálogo de versiones y en `build.gradle.kts`.
* Kotlin actualizado a 2.0.21.
* `networkTimeout` del Gradle Wrapper elevado a 60 s para evitar fallos de descarga del wrapper en CI.
* Integración de `RELEASE_NOTES.md` en el pipeline de GitHub Actions (`build-apk.yml`): la release publica `GeoSilence-debug.apk` con las notas de `RELEASE_NOTES.md` como cuerpo al subir un tag `vX.Y.Z`.

---

## [1.0.0] - 2026-11-06

### ✨ Versión Inicial
* Geofencing mediante la API de Google Play Services con soporte para entrada, permanencia y salida de zonas geográficas.
* Servicio `NotificationListenerService` para la interceptación y cancelación de notificaciones en tiempo real cuando el dispositivo se encuentra dentro del área de silencio.
* Selección granular de aplicaciones bloqueadas con buscador integrado y persistencia local.
* Registro e historial local de eventos de bloqueo con marcas de tiempo.
* Modo de simulación manual para pruebas sin desplazamiento físico.
* Pipeline automatizado de CI/CD en GitHub Actions para compilación de APKs de depuración y publicación de releases al empujar etiquetas Git.
