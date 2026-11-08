package com.personal.geosilence.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.CancellationSignal
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Utilidades para comprobar el permiso de ubicación y obtener la posición del usuario
 * con el cliente fusionado de Google Play Services.
 */
object LocationHelper {

    private const val TAG = "LocationHelper"
    private const val FALLBACK_TIMEOUT_MS = 2000L

    fun hasForegroundPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Devuelve la ubicación actual (o la última conocida si la consulta no responde a tiempo).
     * Devuelve null si no hay permiso o no se pudo obtener ninguna posición.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context, timeoutMs: Long = 6000L): Location? {
        if (!hasForegroundPermission(context)) return null

        val client = LocationServices.getFusedLocationProviderClient(context)

        val current = try {
            withTimeoutOrNull(timeoutMs) {
                suspendCancellableCoroutine { continuation ->
                    val cancellationSignal = CancellationSignal()
                    continuation.invokeOnCancellation {
                        try {
                            cancellationSignal.cancel()
                        } catch (e: Exception) {
                            Log.w(TAG, "No se pudo cancelar la petición de ubicación", e)
                        }
                    }
                    try {
                        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationSignal)
                            .addOnSuccessListener { location ->
                                if (continuation.isActive) continuation.resume(location)
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error obteniendo la ubicación actual", e)
                                if (continuation.isActive) continuation.resume(null)
                            }
                    } catch (e: SecurityException) {
                        Log.e(TAG, "Permiso de ubicación denegado", e)
                        if (continuation.isActive) continuation.resume(null)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción consultando la ubicación actual", e)
            null
        }

        if (current != null) return current

        return try {
            withTimeoutOrNull(FALLBACK_TIMEOUT_MS) {
                suspendCancellableCoroutine { continuation ->
                    client.lastLocation
                        .addOnSuccessListener { location ->
                            if (continuation.isActive) continuation.resume(location)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error obteniendo la última ubicación conocida", e)
                            if (continuation.isActive) continuation.resume(null)
                        }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción consultando la última ubicación", e)
            null
        }
    }
}
