# 📍 GeoSilence - Bloqueador de Notificaciones por Ubicación

**GeoSilence** es una aplicación nativa para Android diseñada para silenciar o descartar automáticamente las notificaciones de aplicaciones seleccionadas (como WhatsApp, Slack, redes sociales o correo) cuando te encuentras en un área geográfica determinada (como tu oficina, escuela, universidad o casa).

---

## 🚀 Características

- **Geofencing Inteligente**: Define tu zona (coordenadas GPS y radio en metros) usando la API de Google Play Services para un consumo mínimo de batería.
- **Acceso a Notificaciones (`NotificationListenerService`)**: Cancela las notificaciones no deseadas en el instante en que son recibidas cuando estás dentro del radio establecido.
- **Selector de Apps**: Lista todas las aplicaciones instaladas en tu dispositivo con buscador integrado y switches individuales.
- **Historial de Descartes**: Registro local de las notificaciones que han sido bloqueadas y a qué hora.
- **Modo de Prueba**: Botón para simular "Dentro de Zona" / "Fuera de Zona" sin tener que desplazarte físicamente.
- **CI/CD con GitHub Actions**: Compilación automatizada del proyecto en cada integración.

---

## ⚙️ Permisos requeridos y Configuración en Android

Para que la app funcione correctamente en segundo plano, se gestionan 3 permisos clave:

1. **Acceso a Notificaciones:**
   - Requiere el permiso `BIND_NOTIFICATION_LISTENER_SERVICE` otorgado en *Ajustes > Acceso a notificaciones*.
2. **Ubicación en segundo plano:**
   - Permiso `ACCESS_BACKGROUND_LOCATION` para que el Geofence continúe activo con la pantalla bloqueada.
3. **Optimización de Batería:**
   - Exclusión de optimización de batería para evitar la suspensión del servicio por parte del sistema operativo.

---

## 🧱 Estructura del Proyecto

```
miapp/
├── .github/workflows/build-apk.yml    # CI/CD para compilar el APK en la nube
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml        # Declaración de permisos y servicios
│   │   ├── java/com/personal/geosilence/
│   │   │   ├── MainActivity.kt        # Actividad principal y permisos
│   │   │   ├── data/
│   │   │   │   ├── Models.kt          # Modelos de datos
│   │   │   │   └── AppPreferences.kt  # Persistencia de configuración
│   │   │   ├── location/
│   │   │   │   └── GeofenceManager.kt # Gestión de geocercas GPS
│   │   │   ├── service/
│   │   │   │   ├── NotificationBlockerService.kt # Listener de notificaciones
│   │   │   │   ├── GeofenceBroadcastReceiver.kt  # Eventos de entrada/salida
│   │   │   │   └── BootReceiver.kt               # Restaura geocercas al reiniciar
│   │   │   └── ui/
│   │   │       ├── MainScreen.kt      # Pantalla con Compose Material 3
│   │   │       └── theme/             # Tema visual
│   │   └── res/                       # Iconos y estilos
│   └── build.gradle.kts
├── gradle/wrapper/                    # Wrapper de Gradle para compilación autónoma
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```
