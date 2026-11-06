# 📍 GeoSilence - Bloqueador de Notificaciones por Ubicación

**GeoSilence** es una aplicación nativa para Android diseñada para silenciar o descartar automáticamente las notificaciones de aplicaciones seleccionadas (como WhatsApp, Slack, redes sociales o correo) cuando te encuentras en un área geográfica determinada (como tu oficina, escuela, universidad o casa).

---

## 🚀 Características

- **Geofencing Inteligente**: Define tu zona (coordenadas GPS y radio en metros) usando la API de Google Play Services para un consumo mínimo de batería.
- **Acceso a Notificaciones (`NotificationListenerService`)**: Cancela las notificaciones no deseadas en el instante en que son recibidas cuando estás dentro del radio establecido.
- **Selector de Apps**: Lista todas las aplicaciones instaladas en tu dispositivo con buscador integrado y switches individuales.
- **Historial de Descartes**: Registro local de las notificaciones que han sido bloqueadas y a qué hora.
- **Modo de Prueba**: Botón para simular "Dentro de Zona" / "Fuera de Zona" sin tener que desplazarte físicamente.
- **CI/CD Integrado con GitHub Actions**: Compila automáticamente el archivo `.apk` cada vez que subes un commit o creas un tag/release.

---

## 📥 Cómo descargar la aplicación desde GitHub

Este repositorio cuenta con un flujo de trabajo automatizado con **GitHub Actions** (`.github/workflows/build-apk.yml`).

### Opción 1: Descargar desde las "Actions" (Cualquier commit)
1. Ve a tu repositorio en GitHub desde el navegador de tu teléfono o PC.
2. Haz clic en la pestaña **"Actions"**.
3. Selecciona la ejecución más reciente del flujo **"Build and Release Android APK"**.
4. En la parte inferior, en la sección **"Artifacts"**, descarga el archivo **`GeoSilence-Debug-APK`**.
5. Descomprime el archivo `.zip` e instala el `.apk` resultante en tu teléfono Android.

### Opción 2: Descargar desde "Releases" (Recomendado)
Si creas una etiqueta (tag) en Git (por ejemplo `v1.0.0`), GitHub creará automáticamente una Release con el archivo APK listo para descargar con un solo clic:
```bash
git tag v1.0.0
git push origin v1.0.0
```
Luego ve a la sección **Releases** de tu repositorio y descarga directamente el archivo **`GeoSilence-debug.apk`**.

---

## ⚙️ Permisos requeridos y Configuración en Android

Para que la app funcione correctamente en segundo plano, debes concederle 3 permisos clave:

1. **Acceso a Notificaciones:**
   - La app te mostrará un aviso con un botón directo a *Ajustes > Acceso a notificaciones*.
   - Activa el interruptor para **GeoSilence**.
2. **Ubicación Todo el Tiempo (Segundo plano):**
   - Cuando solicite permisos de ubicación, selecciona **"Permitir todo el tiempo"** para que el Geofence funcione con la pantalla apagada.
3. **Desactivar Optimización de Batería:**
   - Permite que GeoSilence funcione sin restricciones de batería para que el sistema Android no cierre el servicio en segundo plano.

---

## 🛠️ Cómo subir este proyecto a tu GitHub desde Termux

1. **Configura tu usuario de Git (si es la primera vez):**
   ```bash
   git config --global user.name "Tu Nombre o Usuario"
   git config --global user.email "tu_correo@ejemplo.com"
   ```

2. **Crea un repositorio nuevo y vacío en [GitHub.com](https://github.com/new)** (por ejemplo llamado `geosilence` o `miapp`). *No marques la casilla de inicializar con README ni .gitignore*.

3. **Inicializa y sube tu código:**
   ```bash
   git init
   git add .
   git commit -m "feat: GeoSilence - Aplicación Android de bloqueo de notificaciones por ubicación"
   git branch -M main
   git remote add origin https://github.com/TU_USUARIO/TU_REPOSITORIO.git
   git push -u origin main
   ```
   *(Nota: Para el push te pedirá tu usuario de GitHub y un **Personal Access Token (classic)** con permisos de `repo` y `workflow`)*.

4. **Para generar un Release descargable directamente:**
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

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
