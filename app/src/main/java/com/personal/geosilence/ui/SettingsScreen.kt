package com.personal.geosilence.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.geosilence.data.AppPreferences
import com.personal.geosilence.data.HistoryRetentionCycle
import com.personal.geosilence.service.HistoryCleanupScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pantalla de Configuración con ajuste de frecuencia de auto-eliminación
 * del historial (Diario, Semanal, Nunca) y estado del sistema.
 */
@Composable
fun SettingsScreen(
    prefs: AppPreferences,
    onNavigateToHistory: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var selectedCycle by remember { mutableStateOf(prefs.historyRetention) }
    var cleanupStatusMessage by remember { mutableStateOf<String?>(null) }
    var isCleaning by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Título de la sección
        Text(
            text = "Configuración",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Gestiona la retención de datos y las preferencias de la aplicación.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Card principal: Auto-eliminación del historial
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoDelete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Auto-eliminación del Historial",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Define la antigüedad máxima de los registros de notificaciones bloqueadas antes de ser descartados automáticamente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Opciones de ciclo: Diario, Semanal (predeterminado), Nunca
                HistoryRetentionCycle.entries.forEach { cycle ->
                    val isSelected = selectedCycle == cycle
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                selectedCycle = cycle
                                prefs.historyRetention = cycle
                                HistoryCleanupScheduler.runImmediateCleanup(context)
                                scope.launch(Dispatchers.IO) {
                                    val removed = prefs.performAutoCleanup(cycle)
                                    withContext(Dispatchers.Main) {
                                        cleanupStatusMessage = if (removed > 0) {
                                            "Frecuencia guardada: ${cycle.title}. Se purgaron $removed registros antiguos."
                                        } else {
                                            "Frecuencia guardada: ${cycle.title}."
                                        }
                                    }
                                }
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surface,
                        tonalElevation = if (isSelected) 2.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    selectedCycle = cycle
                                    prefs.historyRetention = cycle
                                    HistoryCleanupScheduler.runImmediateCleanup(context)
                                    scope.launch(Dispatchers.IO) {
                                        val removed = prefs.performAutoCleanup(cycle)
                                        withContext(Dispatchers.Main) {
                                            cleanupStatusMessage = if (removed > 0) {
                                                "Frecuencia guardada: ${cycle.title}. Se purgaron $removed registros antiguos."
                                            } else {
                                                "Frecuencia guardada: ${cycle.title}."
                                            }
                                        }
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = cycle.title,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    if (cycle == HistoryRetentionCycle.WEEKLY) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "Recomendado",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = cycle.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Botón para forzar limpieza inmediata
                OutlinedButton(
                    onClick = {
                        isCleaning = true
                        scope.launch(Dispatchers.IO) {
                            val removed = prefs.performAutoCleanup()
                            withContext(Dispatchers.Main) {
                                isCleaning = false
                                cleanupStatusMessage = if (removed > 0) {
                                    "Limpieza completada: $removed registros eliminados."
                                } else {
                                    "El historial ya está limpio y al día."
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCleaning
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isCleaning) "Limpiando..." else "Ejecutar limpieza manual ahora")
                }

                cleanupStatusMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sección de Permisos del Sistema
        Text(
            text = "Accesos y Permisos",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        SettingsActionCard(
            icon = Icons.Default.Notifications,
            title = "Acceso a Notificaciones",
            subtitle = "Requerido para interceptar y cancelar notificaciones en zona.",
            onClick = {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        SettingsActionCard(
            icon = Icons.Default.BatteryChargingFull,
            title = "Optimización de Batería",
            subtitle = "Evita que Android suspenda el servicio de geolocalización.",
            onClick = {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    context.startActivity(intent)
                }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Sección Información de la Aplicación
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("GeoSilence", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Versión 1.1.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Silencio inteligente y selectivo activado por geolocalización.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SettingsActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 12.sp
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}
