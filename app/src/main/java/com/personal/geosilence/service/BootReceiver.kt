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
                val pendingResult = goAsync()
                val appContext = context.applicationContext
                val geofenceManager = GeofenceManager(appContext)

                val operationId = geofenceManager.newOperationId()

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val registered = geofenceManager.registerGeofences(
                            prefs.getZones(),
                            operationId
                        )

                        if (registered) {
                            val synced = GeofenceManager.syncInsideState(
                                appContext,
                                prefs,
                                operationId
                            )
                            if (!synced) {
                                Log.w(
                                    "BootReceiver",
                                    "No se pudo sincronizar el estado de la zona tras el reinicio"
                                )
                            }
                        } else {
                            Log.e(
                                "BootReceiver",
                                "No se pudieron restaurar las geovallas tras el reinicio"
                            )
                        }
                    } catch (exception: Exception) {
                        Log.e(
                            "BootReceiver",
                            "Error restaurando las geovallas tras el reinicio",
                            exception
                        )
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
