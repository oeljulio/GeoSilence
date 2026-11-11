# 📍 GeoSilence - Bloqueador de Notificaciones por Ubicación

[![Build Android APK](https://github.com/oeljulio/GeoSilence/actions/workflows/build-apk.yml/badge.svg)](https://github.com/oeljulio/GeoSilence/actions/workflows/build-apk.yml)
[![Version](https://img.shields.io/badge/version-v1.4.3-blue.svg)](https://github.com/oeljulio/GeoSilence/releases)
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
* `INTERNET` / `ACCESS_NETWORK_STATE`: Para descargar las teselas de OpenStreetMap del mapa interactivo.
* `QUERY_ALL_PACKAGES`: Para listar las aplicaciones instaladas y permitir seleccionar cuáles silenciar.
* `RECEIVE_BOOT_COMPLETED`: Para restaurar la geocerca y las tareas de limpieza tras reiniciar el dispositivo.
* `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: Para solicitar la exención de ahorro de energía.

**Requisitos y funcionamiento:** la detección usa la API de geocercas de Google Play Services, por lo que requiere un dispositivo con GMS. No hay un intervalo de GPS fijo: es el sistema quien decide cuándo muestrear la posición para detectar la entrada o salida de la zona (habitualmente entre decenas de segundos y unos minutos, según el estado del dispositivo).

---

## 🛠️ Compilación y Desarrollo

Las compilaciones Android se realizan mediante **GitHub Actions**. No se
requiere ni se admite compilar el APK localmente desde Termux.

El workflow `Build Android APK` se ejecuta al subir cambios a `main` o
`master`, al abrir Pull Requests hacia esas ramas y manualmente mediante
`workflow_dispatch`. Ejecuta las pruebas unitarias y genera el APK de
depuración como artefacto descargable.

Para obtener el APK, abre la ejecución correspondiente en la pestaña
**Actions** del repositorio y descarga el artefacto `GeoSilence-Debug-APK`.

---

## 📁 Estructura del Proyecto

```text
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
```

---

## 📄 Licencia

Este proyecto se distribuye bajo la licencia GNU General Public License v3.0 (GPL-3.0). Consulta el archivo LICENSE para ver los términos y condiciones completos.

## Comportamiento de las notificaciones

Cuando se cumple la condición de bloqueo, GeoSilence cancela la notificación
de la aplicación seleccionada para retirarla del panel del sistema. Android
no permite que GeoSilence restaure esa notificación original después de
cancelarla. La aplicación conserva únicamente un registro informativo del
evento; no almacena ni recupera el contenido completo de la notificación.
