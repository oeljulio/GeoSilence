package com.personal.geosilence.data

import java.util.UUID

data class GeofenceZone(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Mi Zona",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radiusMeters: Float = 200f,
    val isEnabled: Boolean = true
)

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
