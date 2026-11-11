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
import com.google.android.gms.tasks.Task
import com.personal.geosilence.data.AppPreferences
import com.personal.geosilence.data.GeofenceZone
import com.personal.geosilence.data.hasValidLocation
import com.personal.geosilence.service.GeofenceBroadcastReceiver
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class GeofenceManager(private val context: Context) {

    private val geofencingClient: GeofencingClient =
        LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        PendingIntent.getBroadcast(context, 1001, intent, flags)
    }

    /**
     * Reserva una versión nueva para cada solicitud antes de iniciar su corrutina.
     * Las operaciones anteriores que aún no hayan comenzado quedarán obsoletas.
     */
    fun newOperationId(): Long = operationCounter.incrementAndGet()

    @SuppressLint("MissingPermission")
    suspend fun registerGeofences(
        zones: List<GeofenceZone>,
        operationId: Long
    ): Boolean = operationMutex.withLock {
        withContext(NonCancellable) {
            if (!isCurrentOperation(operationId)) {
                return@withContext false
            }

            try {
                // No liberar el bloqueo hasta que Google Play Services responda.
                geofencingClient.removeGeofences(geofencePendingIntent).awaitResult()

                if (!isCurrentOperation(operationId)) {
                    return@withContext false
                }

                val activeZones = zones.filter {
                    it.isEnabled && it.hasValidLocation()
                }

                if (activeZones.isEmpty()) {
                    Log.d(
                        "GeofenceManager",
                        "No hay zonas activas; registros anteriores eliminados"
                    )
                    return@withContext true
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
                        .setLoiteringDelay(10000)
                        .build()
                }

                val request = GeofencingRequest.Builder().apply {
                    setInitialTrigger(
                        GeofencingRequest.INITIAL_TRIGGER_ENTER or
                            GeofencingRequest.INITIAL_TRIGGER_DWELL
                    )
                    addGeofences(geofenceList)
                }.build()

                geofencingClient.addGeofences(
                    request,
                    geofencePendingIntent
                ).awaitResult()

                val stillCurrent = isCurrentOperation(operationId)

                if (stillCurrent) {
                    Log.d(
                        "GeofenceManager",
                        "Geofences registradas correctamente: ${geofenceList.size}"
                    )
                }

                stillCurrent
            } catch (exception: Exception) {
                Log.e(
                    "GeofenceManager",
                    "Error registrando geofences: ${exception.message}",
                    exception
                )
                false
            }
        }
    }

    suspend fun removeGeofences(operationId: Long): Boolean =
        operationMutex.withLock {
            withContext(NonCancellable) {
                if (!isCurrentOperation(operationId)) {
                    return@withContext false
                }

                try {
                    geofencingClient.removeGeofences(
                        geofencePendingIntent
                    ).awaitResult()

                    isCurrentOperation(operationId)
                } catch (exception: Exception) {
                    Log.e(
                        "GeofenceManager",
                        "Error eliminando geofences: ${exception.message}",
                        exception
                    )
                    false
                }
            }
        }

    companion object {
        /** Radio mínimo efectivo de la geocerca. */
        const val MIN_RADIUS_METERS = 50f

        private val operationMutex = Mutex()
        private val operationCounter = AtomicLong(0L)

        private fun isCurrentOperation(operationId: Long): Boolean =
            operationCounter.get() == operationId

        /**
         * Recalcula si el dispositivo está dentro de la zona activa.
         * No permite que una consulta antigua sobrescriba una operación posterior.
         */
        suspend fun syncInsideState(
            context: Context,
            prefs: AppPreferences,
            operationId: Long
        ): Boolean {
            if (!isCurrentOperation(operationId)) return false

            val zone = prefs.getZones().firstOrNull {
                it.isEnabled && it.hasValidLocation()
            }

            if (zone == null) {
                if (!isCurrentOperation(operationId)) return false
                prefs.isInsideZone = false
                return true
            }

            val location = LocationHelper.getCurrentLocation(context) ?: return false

            if (!isCurrentOperation(operationId)) return false

            prefs.isInsideZone = isLocationInsideZone(
                location.latitude,
                location.longitude,
                zone
            )

            return true
        }

        fun isLocationInsideZone(
            latitude: Double,
            longitude: Double,
            zone: GeofenceZone
        ): Boolean {
            val results = FloatArray(1)
            Location.distanceBetween(
                latitude,
                longitude,
                zone.latitude,
                zone.longitude,
                results
            )
            val distanceMeters = results[0]
            return distanceMeters <= zone.radiusMeters.coerceAtLeast(MIN_RADIUS_METERS)
        }
    }
}

/**
 * Espera el resultado real de Google Play Services.
 * Se utiliza dentro de NonCancellable para no liberar el Mutex
 * mientras la operación subyacente continúa pendiente.
 */
private suspend fun <T> Task<T>.awaitResult(): T =
    suspendCoroutine { continuation ->
        addOnSuccessListener { result ->
            continuation.resume(result)
        }.addOnFailureListener { exception ->
            continuation.resumeWithException(exception)
        }.addOnCanceledListener {
            continuation.resumeWithException(
                CancellationException("Operación de Google Play Services cancelada")
            )
        }
    }
