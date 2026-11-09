package com.personal.geosilence.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.personal.geosilence.data.AppPreferences
import com.personal.geosilence.location.GeofenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Dispositivo reiniciado. Restaurando Geofences...")
            val prefs = AppPreferences.getInstance(context)
            if (prefs.isMasterEnabled) {
                val zones = prefs.getZones()
                val geofenceManager = GeofenceManager(context)
                geofenceManager.registerGeofences(zones)
                // Recalcular dentro/fuera: tras el reinicio el estado guardado puede estar obsoleto
                val pendingResult = goAsync()
                val appContext = context.applicationContext
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        GeofenceManager.syncInsideState(appContext, prefs)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            // Restaurar programación de auto-limpieza de historial
            HistoryCleanupScheduler.schedulePeriodicCleanup(context)
        }
    }
}
