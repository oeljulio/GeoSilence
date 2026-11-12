package com.personal.geosilence.ui

import android.os.PowerManager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
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
import com.personal.geosilence.BuildConfig
import com.personal.geosilence.data.AppPreferences
import com.personal.geosilence.data.AppThemeMode
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
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onOpenBackgroundGuide: () -> Unit = {},
    onToggleSimulation: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var selectedCycle by remember { mutableStateOf(prefs.historyRetention) }
    var cleanupStatusMessage by remember { mutableStateOf<String?>(null) }
    var isCleaning by remember { mutableStateOf(false) }
    var isDiagnosticsExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Título de la sección
        Text(
            text = "Configuración",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Gestiona la retención de datos y las preferencias de la aplicación.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Selector de apariencia
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Apariencia",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Personaliza los colores de GeoSilence.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                val themeOptions = listOf(
                    Triple(
                        AppThemeMode.SYSTEM,
                        "Automático",
                        "Sigue el tema de tu dispositivo."
                    ),
                    Triple(
                        AppThemeMode.LIGHT,
                        "Claro",
                        "Mantiene siempre la interfaz clara."
                    ),
                    Triple(
                        AppThemeMode.DARK_AMOLED,
                        "Oscuro AMOLED",
                        "Fondo negro puro con detalles verde esmeralda."
                    )
                )

                themeOptions.forEach { (mode, title, description) ->
                    val selected = themeMode == mode

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable { onThemeModeChange(mode) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        tonalElevation = if (selected) 2.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { onThemeModeChange(mode) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = when (mode) {
                                    AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                    AppThemeMode.LIGHT -> Icons.Default.LightMode
                                    AppThemeMode.DARK_AMOLED -> Icons.Default.DarkMode
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card principal: Auto-eliminación del historial
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
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
                        shape = RoundedCornerShape(12.dp),
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
            icon = Icons.Default.Security,
            title = "Protección en segundo plano",
            subtitle = "Consulta cómo evitar que el fabricante cierre GeoSilence.",
            onClick = onOpenBackgroundGuide
        )

        Spacer(modifier = Modifier.height(8.dp))

        SettingsActionCard(
            icon = Icons.Default.BatteryChargingFull,
            title = "Optimización de Batería",
            subtitle = "Evita que Android suspenda el servicio de geolocalización.",
            onClick = {
                val pkg = context.packageName
                val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val isIgnoring = pm?.isIgnoringBatteryOptimizations(pkg) == true

                fun openAppDetails() {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:$pkg")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    }
                }

                if (!isIgnoring) {
                    try {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:$pkg")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        openAppDetails()
                    }
                } else {
                    openAppDetails()
                }
            }
        )

        // Herramientas de diagnóstico, plegadas inicialmente.
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isDiagnosticsExpanded = !isDiagnosticsExpanded
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Diagnóstico",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Herramientas de prueba de GeoSilence",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (isDiagnosticsExpanded) {
                            Icons.Default.ExpandLess
                        } else {
                            Icons.Default.ExpandMore
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(
                    visible = isDiagnosticsExpanded,
                    enter = expandVertically(
                        animationSpec = tween(durationMillis = 220)
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 180)
                    ),
                    exit = shrinkVertically(
                        animationSpec = tween(durationMillis = 180)
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = 120)
                    )
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Simulación de geovalla",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Alterna manualmente el estado dentro/fuera de la zona para comprobar el comportamiento del bloqueo sin desplazarte.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = onToggleSimulation,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Alternar estado de prueba")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Sección Información de la Aplicación
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("GeoSilence", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Versión ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
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
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
