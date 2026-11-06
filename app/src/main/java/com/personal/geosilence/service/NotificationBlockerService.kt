package com.personal.geosilence.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.personal.geosilence.data.AppPreferences
import com.personal.geosilence.data.BlockedNotificationRecord

class NotificationBlockerService : NotificationListenerService() {

    private lateinit var prefs: AppPreferences

    override fun onCreate() {
        super.onCreate()
        prefs = AppPreferences.getInstance(applicationContext)
        Log.d(TAG, "NotificationBlockerService creado")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "NotificationBlockerService conectado al sistema")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "NotificationBlockerService desconectado")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val targetPackage = sbn.packageName ?: return

        // No interceptar notificaciones de nuestra propia app
        if (targetPackage == packageName) return

        // Verificar si el maestro está activo
        if (!prefs.isMasterEnabled) {
            return
        }

        // Verificar si estamos actualmente en la zona restringida
        if (!prefs.isInsideZone) {
            return
        }

        // Verificar si la app emisora está en la lista de bloqueadas
        if (prefs.isPackageBlocked(targetPackage)) {
            try {
                // Cancelar inmediatamente la notificación del panel
                cancelNotification(sbn.key)

                // Extraer título o texto para el registro informativo
                val extras = sbn.notification?.extras
                val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                    ?: extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                    ?: "Notificación"

                val appName = try {
                    val pm = packageManager
                    val appInfo = pm.getApplicationInfo(targetPackage, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    targetPackage
                }

                val record = BlockedNotificationRecord(
                    packageName = targetPackage,
                    appName = appName,
                    title = title
                )

                prefs.addBlockedLog(record)
                Log.i(TAG, "Notificación bloqueada de: $appName ($targetPackage) - '$title'")
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelando notificación de $targetPackage", e)
            }
        }
    }

    companion object {
        private const val TAG = "NotificationBlocker"
    }
}
