# Novedades de GeoSilence v1.4.3 (2026-11-11)

Esta versión reúne las mejoras y correcciones incorporadas desde v1.4.0.

## Novedades

- Nueva bienvenida de tres pasos con orientación sobre permisos y configuración en segundo plano.
- Información mejorada sobre los ajustes restringidos de Android para el acceso a notificaciones.
- Siete pruebas unitarias para modelos y validación de coordenadas.

## Correcciones

- Sincronización más segura de las operaciones de registro y eliminación de geocercas.
- Recalculado del estado dentro/fuera al reactivar la aplicación y después de reiniciar el dispositivo.
- Validación de coordenadas geográficas y rechazo de ubicaciones inválidas.
- Radio mínimo de 50 m coherente entre el registro y la comprobación de geocercas.
- Receiver de geocerca configurado como no exportado.

## Permisos y seguridad

- Eliminados los permisos no utilizados `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION` y `WAKE_LOCK`.
- Se conserva `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, necesario para las funciones de configuración de batería.

## Integración continua

- GitHub Actions ejecuta las pruebas unitarias antes de generar el APK de depuración.
