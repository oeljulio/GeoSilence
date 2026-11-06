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
        if (currentLogs.size > 50) {
            currentLogs.removeAt(currentLogs.size - 1)
        }
        val json = gson.toJson(currentLogs)
        prefs.edit().putString(KEY_RECENT_LOGS, json).apply()
        incrementBlockedCount()
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
