package com.personal.geosilence.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.personal.geosilence.data.AppPreferences
import com.personal.geosilence.data.GeofenceZone
import com.personal.geosilence.service.GeofenceBroadcastReceiver

class GeofenceManager(private val context: Context) {

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        PendingIntent.getBroadcast(context, 1001, intent, flags)
    }

    @SuppressLint("MissingPermission")
    fun registerGeofences(zones: List<GeofenceZone>, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        val activeZones = zones.filter { it.isEnabled }
        if (activeZones.isEmpty()) {
            removeGeofences()
            onSuccess()
            return
        }

        val geofenceList = activeZones.map { zone ->
            Geofence.Builder()
                .setRequestId(zone.id)
                .setCircularRegion(
                    zone.latitude,
                    zone.longitude,
                    zone.radiusMeters.coerceAtLeast(MIN_RADIUS_METERS)
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(
                    Geofence.GEOFENCE_TRANSITION_ENTER or
                    Geofence.GEOFENCE_TRANSITION_EXIT or
                    Geofence.GEOFENCE_TRANSITION_DWELL
                )
                .setLoiteringDelay(10000) // 10 segundos para dwell
                .build()
        }

        val request = GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_DWELL)
            addGeofences(geofenceList)
        }.build()

        geofencingClient.addGeofences(request, geofencePendingIntent)
            .addOnSuccessListener {
                Log.d("GeofenceManager", "Geofences registradas correctamente: ${geofenceList.size}")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e("GeofenceManager", "Error registrando geofences: ${exception.message}", exception)
                onFailure(exception)
            }
    }

    fun removeGeofences(onComplete: () -> Unit = {}) {
        geofencingClient.removeGeofences(geofencePendingIntent)
            .addOnCompleteListener {
                onComplete()
            }
    }

    companion object {
        /** Radio mínimo efectivo de la geocerca; debe coincidir en el registro y en el cálculo dentro/fuera. */
        const val MIN_RADIUS_METERS = 50f

        /**
         * Recalcula si el dispositivo está dentro de la zona guardada con la posición actual.
         * Si no hay zona activa o no se obtiene posición, conserva el estado previo.
         */
        suspend fun syncInsideState(context: Context, prefs: AppPreferences) {
            val zone = prefs.getZones().firstOrNull { it.isEnabled } ?: return
            val location = LocationHelper.getCurrentLocation(context) ?: return
            prefs.isInsideZone = isLocationInsideZone(location.latitude, location.longitude, zone)
        }

        fun isLocationInsideZone(latitude: Double, longitude: Double, zone: GeofenceZone): Boolean {
            val results = FloatArray(1)
            Location.distanceBetween(
                latitude, longitude,
                zone.latitude, zone.longitude,
                results
            )
            val distanceMeters = results[0]
            return distanceMeters <= zone.radiusMeters.coerceAtLeast(MIN_RADIUS_METERS)
        }
    }
}
