package com.personal.geosilence.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.personal.geosilence.data.AppPreferences
import com.personal.geosilence.data.hasValidLocation

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            Log.e("GeofenceReceiver", "GeofencingEvent es nulo")
            return
        }

        if (geofencingEvent.hasError()) {
            Log.e("GeofenceReceiver", "Error en Geofence: ${geofencingEvent.errorCode}")
            return
        }

        val prefs = AppPreferences.getInstance(context)
        val activeZoneIds = prefs.getZones()
            .filter { it.isEnabled && it.hasValidLocation() }
            .map { it.id }
            .toSet()

        val belongsToActiveZone = geofencingEvent.triggeringGeofences
            .orEmpty()
            .any { it.requestId in activeZoneIds }

        if (!belongsToActiveZone) {
            Log.w("GeofenceReceiver", "Evento ignorado: no pertenece a una zona activa")
            return
        }

        val transition = geofencingEvent.geofenceTransition

        when (transition) {
            Geofence.GEOFENCE_TRANSITION_ENTER, Geofence.GEOFENCE_TRANSITION_DWELL -> {
                Log.d("GeofenceReceiver", "Entrada detectada en geofence. Bloqueo activado.")
                prefs.isInsideZone = true
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                Log.d("GeofenceReceiver", "Salida detectada de geofence. Bloqueo desactivado.")
                prefs.isInsideZone = false
            }
            else -> {
                Log.w("GeofenceReceiver", "Transición desconocida: $transition")
            }
        }
    }
}
