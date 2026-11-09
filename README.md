# 📍 GeoSilence - Bloqueador de Notificaciones por Ubicación

[![Build Android APK](https://github.com/oeljulio/GeoSilence/actions/workflows/build-apk.yml/badge.svg)](https://github.com/oeljulio/GeoSilence/actions/workflows/build-apk.yml)
[![Version](https://img.shields.io/badge/version-v1.4.0-blue.svg)](https://github.com/oeljulio/GeoSilence/releases)
[![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE)

**GeoSilence** es una aplicación nativa para Android proyectada para silenciar o descartar automáticamente las notificaciones de aplicaciones seleccionadas (como WhatsApp, Slack, redes sociales o correo) cuando el dispositivo entra **(o sale)** de un área geográfica determinada (como tu oficina, escuela, universidad o casa).

---

## 🚀 Funcionalidades Principales

* 🗺️ **Selector de zona a pantalla completa:** Mapa interactivo basado en OpenStreetMap (Leaflet 1.9.4) incrustado en `WebView` local para arrastrar el marcador y definir el área.
* 🎚️ **Acceso rápido y ajuste continuo de radio:** Botones de acceso rápido de 100 m, 250 m, 500 m y 1 km, junto con un deslizador continuo para ajustar el radio de forma precisa desde 50 m hasta 3.000 m.
* 🔄 **Modo de bloqueo inverso (Dentro / Fuera):** Permite elegir entre bloquear notificaciones al **estar dentro** de la zona o silenciar al **salir** de ella.
* 📱 **Selección de aplicaciones:** Listado de aplicaciones instaladas en el dispositivo para marcar cuáles deben ser silenciadas con buscador y switches independientes.
* 📜 **Historial de notificaciones:** Registro de notificaciones capturadas y bloqueadas con rotación e historial automático programable (`WorkManager`).
* ⚙️ **Ajustes y batería:** Verificación inteligente de estado de optimización de batería (`PowerManager`) con redirección directa a la configuración del sistema para garantizar la ejecución en segundo plano.

---

## 📲 Guía de Instalación y Primeros Pasos

1. Dirígete a la sección [Releases](https://github.com/oeljulio/GeoSilence/releases) del repositorio y descarga el archivo `.apk` de la última versión disponible.
2. Instala el binario en tu dispositivo Android (permite la instalación desde fuentes desconocidas si el sistema lo solicita).
3. Abre **GeoSilence** y concede los permisos requeridos de **Ubicación** y **Acceso a Notificaciones**.
4. Ve a la pantalla de **Ajustes** y pulsa en **Optimización de Batería** para configurar la ejecución en segundo plano sin restricciones.

---

## 🔒 Privacidad y Datos Cartográficos

* **Procesamiento 100 % Local:** Todas las geocercas configuradas, aplicaciones seleccionadas e historial de notificaciones capturadas se guardan exclusivamente en el dispositivo mediante `SharedPreferences`. No se envían datos a servidores externos.
* **Uso de red para mapas:** El mapa descarga las teselas de cartografía mediante conexión a Internet desde **OpenStreetMap** a través de Leaflet.
* **Atribución:** Contiene datos de © [OpenStreetMap](https://www.openstreetmap.org/copyright) contributors.

---

## ⚙️ Permisos Requeridos

* `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`: Para determinar la posición del dispositivo respecto a la geocerca.
* `ACCESS_BACKGROUND_LOCATION`: Para mantener la verificación geográfica continua en segundo plano.
* `BIND_NOTIFICATION_LISTENER_SERVICE`: Para interceptar y descartar notificaciones de las apps seleccionadas.
* `POST_NOTIFICATIONS`: Para mostrar notificaciones de estado y servicio del sistema.
* `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_LOCATION`: Para ejecutar el servicio de geocerca en primer plano mientras se monitoriza la zona.
* `INTERNET` / `ACCESS_NETWORK_STATE`: Para descargar las teselas de OpenStreetMap del mapa interactivo.
* `QUERY_ALL_PACKAGES`: Para listar las aplicaciones instaladas y permitir seleccionar cuáles silenciar.
* `RECEIVE_BOOT_COMPLETED`: Para restaurar la geocerca y las tareas de limpieza tras reiniciar el dispositivo.
* `WAKE_LOCK`: Para que las transiciones de geocerca se procesen aunque el dispositivo entre en reposo.
* `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: Para solicitar la exención de ahorro de energía.

---

## 🛠️ Compilación y Desarrollo

El repositorio cuenta con el wrapper de Gradle (`gradlew`), lo que permite realizar construcciones locales sin dependencias globales instaladas.

```bash
# Clonar el repositorio
git clone [https://github.com/oeljulio/GeoSilence.git](https://github.com/oeljulio/GeoSilence.git)
cd GeoSilence

# Compilar APK de depuración en local
./gradlew assembleDebug

Nota de CI/CD: El flujo habitual de CI/CD en GitHub Actions (build-apk.yml) genera y compila automáticamente los APKs de prueba en los pushes a main y en los Pull Requests dirigidos a las ramas principales main o master.

📁 Estructura del Proyecto

GeoSilence/
├── .github/
│   └── workflows/
│       └── build-apk.yml
├── app/
│   ├── build.gradle.kts
│   └── src/
├── AGENTS.md
├── CHANGELOG.md
├── LICENSE
├── README.md
└── RELEASE_NOTES.md

📄 Licencia

Este proyecto se distribuye bajo la licencia GNU General Public License v3.0 (GPL-3.0). Consulta el archivo LICENSE para ver los términos y condiciones completos.
