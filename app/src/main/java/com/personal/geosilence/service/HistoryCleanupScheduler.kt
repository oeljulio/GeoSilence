package com.personal.geosilence.service

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Gestor del ciclo de programación periódica con WorkManager para
 * la auto-eliminación del historial de notificaciones.
 */
object HistoryCleanupScheduler {

    private const val UNIQUE_WORK_NAME = "GeoSilence_HistoryCleanupWork"
    private const val TAG = "HistoryCleanupScheduler"

    /**
     * Programa la tarea periódica (se ejecuta cada 12 horas si la batería no está baja).
     */
    fun schedulePeriodicCleanup(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val periodicWork = PeriodicWorkRequestBuilder<HistoryCleanupWorker>(
                12, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWork
            )
            Log.d(TAG, "Tarea periódica de limpieza de historial programada correctamente.")
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo programar la tarea periódica: ${e.message}", e)
        }
    }

    /**
     * Ejecuta una limpieza inmediata en segundo plano sin bloquear el hilo principal.
     */
    fun runImmediateCleanup(context: Context) {
        try {
            val immediateWork = OneTimeWorkRequestBuilder<HistoryCleanupWorker>().build()
            WorkManager.getInstance(context).enqueue(immediateWork)
            Log.d(TAG, "Limpieza inmediata de historial encolada.")
        } catch (e: Exception) {
            Log.e(TAG, "Error al encolar limpieza inmediata: ${e.message}", e)
        }
    }
}
