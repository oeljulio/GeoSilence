package com.personal.geosilence.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.personal.geosilence.data.AppPreferences
import com.personal.geosilence.location.GeofenceManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Dispositivo reiniciado. Restaurando Geofences...")
            val prefs = AppPreferences.getInstance(context)
            if (prefs.isMasterEnabled) {
                val zones = prefs.getZones()
                val geofenceManager = GeofenceManager(context)
                geofenceManager.registerGeofences(zones)
            }
        }
    }
}
