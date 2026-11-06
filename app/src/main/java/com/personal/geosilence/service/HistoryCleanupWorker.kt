package com.personal.geosilence.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.personal.geosilence.data.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker en segundo plano para la auto-eliminación periódica del historial
 * de notificaciones según el ciclo configurado (Diario, Semanal, Nunca).
 */
class HistoryCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val prefs = AppPreferences.getInstance(applicationContext)
            val removedCount = prefs.performAutoCleanup()
            Log.i(TAG, "Auto-limpieza de historial ejecutada con éxito. Registros eliminados: $removedCount")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error en HistoryCleanupWorker: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        const val TAG = "HistoryCleanupWorker"
    }
}
