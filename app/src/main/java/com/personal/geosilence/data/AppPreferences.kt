package com.personal.geosilence.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "geosilence_prefs"
        private const val KEY_MASTER_ENABLED = "master_enabled"
        private const val KEY_IS_INSIDE_ZONE = "is_inside_zone"
        private const val KEY_BLOCKED_PACKAGES = "blocked_packages"
        private const val KEY_ZONES_JSON = "zones_json"
        private const val KEY_BLOCKED_COUNT = "blocked_count"
        private const val KEY_RECENT_LOGS = "recent_logs"
        private const val KEY_HISTORY_RETENTION = "history_retention_cycle"
        private const val MAX_LOGS_LIMIT = 1000

        @Volatile
        private var instance: AppPreferences? = null

        fun getInstance(context: Context): AppPreferences {
            return instance ?: synchronized(this) {
                instance ?: AppPreferences(context.applicationContext).also { instance = it }
            }
        }
    }

    var isMasterEnabled: Boolean
        get() = prefs.getBoolean(KEY_MASTER_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_MASTER_ENABLED, value).apply()

    var isInsideZone: Boolean
        get() = prefs.getBoolean(KEY_IS_INSIDE_ZONE, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_INSIDE_ZONE, value).apply()

    var blockedCount: Int
        get() = prefs.getInt(KEY_BLOCKED_COUNT, 0)
        private set(value) = prefs.edit().putInt(KEY_BLOCKED_COUNT, value).apply()

    var historyRetention: HistoryRetentionCycle
        get() {
            val savedId = prefs.getString(KEY_HISTORY_RETENTION, HistoryRetentionCycle.WEEKLY.id)
            return HistoryRetentionCycle.fromId(savedId)
        }
        set(value) = prefs.edit().putString(KEY_HISTORY_RETENTION, value.id).apply()

    fun incrementBlockedCount() {
        blockedCount = blockedCount + 1
    }

    fun getBlockedPackages(): Set<String> {
        return prefs.getStringSet(KEY_BLOCKED_PACKAGES, emptySet()) ?: emptySet()
    }

    fun setPackageBlocked(packageName: String, blocked: Boolean) {
        val current = getBlockedPackages().toMutableSet()
        if (blocked) {
            current.add(packageName)
        } else {
            current.remove(packageName)
        }
        prefs.edit().putStringSet(KEY_BLOCKED_PACKAGES, current).apply()
    }

    fun isPackageBlocked(packageName: String): Boolean {
        return getBlockedPackages().contains(packageName)
    }

    fun getZones(): List<GeofenceZone> {
        val json = prefs.getString(KEY_ZONES_JSON, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<GeofenceZone>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveZones(zones: List<GeofenceZone>) {
        val json = gson.toJson(zones)
        prefs.edit().putString(KEY_ZONES_JSON, json).apply()
    }

    fun addBlockedLog(record: BlockedNotificationRecord) {
        val currentLogs = getRecentLogs().toMutableList()
        currentLogs.add(0, record)
        if (currentLogs.size > MAX_LOGS_LIMIT) {
            currentLogs.removeAt(currentLogs.size - 1)
        }
        val json = gson.toJson(currentLogs)
        prefs.edit().putString(KEY_RECENT_LOGS, json).apply()
        incrementBlockedCount()
        // Ejecución preventiva de auto-limpieza
        performAutoCleanup()
    }

    /**
     * Elimina registros del historial que superen el límite de antigüedad configurado.
     * @return Cantidad de registros eliminados.
     */
    fun performAutoCleanup(retention: HistoryRetentionCycle = historyRetention): Int {
        val maxAge = retention.maxAgeMillis ?: return 0
        val cutoffTime = System.currentTimeMillis() - maxAge
        val currentLogs = getRecentLogs()
        val filteredLogs = currentLogs.filter { it.timestamp >= cutoffTime }
        val removedCount = currentLogs.size - filteredLogs.size
        if (removedCount > 0) {
            val json = gson.toJson(filteredLogs)
            prefs.edit().putString(KEY_RECENT_LOGS, json).apply()
        }
        return removedCount
    }

    fun getRecentLogs(): List<BlockedNotificationRecord> {
        val json = prefs.getString(KEY_RECENT_LOGS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<BlockedNotificationRecord>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearLogs() {
        prefs.edit().remove(KEY_RECENT_LOGS).putInt(KEY_BLOCKED_COUNT, 0).apply()
    }
}
