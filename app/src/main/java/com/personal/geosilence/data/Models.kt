package com.personal.geosilence.data

import java.util.UUID

data class GeofenceZone(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Mi Zona",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radiusMeters: Float = 200f,
    val isEnabled: Boolean = true,
    val blockInside: Boolean = true
)

fun GeofenceZone.hasValidLocation(): Boolean =
    latitude.isFinite() &&
        longitude.isFinite() &&
        latitude in -90.0..90.0 &&
        longitude in -180.0..180.0 &&
        (latitude != 0.0 || longitude != 0.0)

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val isBlocked: Boolean = false
)

data class BlockedNotificationRecord(
    val id: String = UUID.randomUUID().toString(),
    val packageName: String,
    val appName: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class HistoryRetentionCycle(
    val id: String,
    val title: String,
    val description: String,
    val maxAgeMillis: Long?
) {
    DAILY(
        id = "daily",
        title = "Diario",
        description = "Eliminar registros con más de 24 horas",
        maxAgeMillis = 24 * 60 * 60 * 1000L
    ),
    WEEKLY(
        id = "weekly",
        title = "Semanal",
        description = "Eliminar registros con más de 7 días (predeterminado)",
        maxAgeMillis = 7 * 24 * 60 * 60 * 1000L
    ),
    NEVER(
        id = "never",
        title = "Nunca",
        description = "Conservar todo el historial sin límite de tiempo",
        maxAgeMillis = null
    );

    companion object {
        fun fromId(id: String?): HistoryRetentionCycle {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: WEEKLY
        }
    }
}
