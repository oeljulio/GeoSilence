package com.personal.geosilence.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import com.personal.geosilence.data.AppPreferences
import com.personal.geosilence.data.AppThemeMode
import com.personal.geosilence.data.GeofenceZone
import com.personal.geosilence.data.hasValidLocation
import com.personal.geosilence.data.InstalledAppInfo
import com.personal.geosilence.location.GeofenceManager
import com.personal.geosilence.location.LocationHelper
import com.personal.geosilence.ui.theme.StatusGray
import com.personal.geosilence.ui.theme.StatusGreen
import com.personal.geosilence.ui.theme.StatusRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    hasLocationPermission: Boolean,
    onRequestLocationPermission: () -> Unit,
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences.getInstance(context) }
    val geofenceManager = remember { GeofenceManager(context) }
    val scope = rememberCoroutineScope()

    var isMasterEnabled by remember { mutableStateOf(prefs.isMasterEnabled) }
    var isInsideZone by remember { mutableStateOf(prefs.isInsideZone) }
    var blockedCount by remember { mutableStateOf(prefs.blockedCount) }
    var currentAddress by remember { mutableStateOf("Buscando ubicación...") }

    // Zona guardada y borrador editable
    var savedZone by remember {
        mutableStateOf(prefs.getZones().firstOrNull() ?: GeofenceZone(name = "Zona Principal", radiusMeters = 200f))
    }
    var zoneName by remember { mutableStateOf(savedZone.name) }
    var draftLatitude by remember { mutableDoubleStateOf(savedZone.latitude) }
    var draftLongitude by remember { mutableDoubleStateOf(savedZone.longitude) }
    var radiusMeters by remember { mutableFloatStateOf(savedZone.radiusMeters) }
    var blockInside by remember { mutableStateOf(savedZone.blockInside) }

    val hasChosenLocation = savedZone.copy(
        latitude = draftLatitude,
        longitude = draftLongitude
    ).hasValidLocation()
    val hasChanges = hasChosenLocation && (
        zoneName != savedZone.name ||
        draftLatitude != savedZone.latitude ||
        draftLongitude != savedZone.longitude ||
        radiusMeters != savedZone.radiusMeters ||
        blockInside != savedZone.blockInside
        )

    var showMapPicker by remember { mutableStateOf(false) }
    var showWelcome by remember {
        mutableStateOf(!prefs.hasCompletedWelcome)
    }
    var showBackgroundGuide by remember { mutableStateOf(false) }

    // Apps state
    var searchQuery by remember { mutableStateOf("") }
    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Check Notification Listener Permission
    var hasNotificationAccess by remember {
        mutableStateOf(
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        )
    }

    // Refresh state periodically or on resume
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val blockedSet = prefs.getBlockedPackages()

            val appList = packages.filter { app ->
                // Filtrar para mostrar principalmente apps de usuario o con launcher
                (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || pm.getLaunchIntentForPackage(app.packageName) != null
            }.map { app ->
                val label = pm.getApplicationLabel(app).toString()
                InstalledAppInfo(
                    packageName = app.packageName,
                    appName = label,
                    isBlocked = blockedSet.contains(app.packageName)
                )
            }.sortedBy { it.appName.lowercase() }

            withContext(Dispatchers.Main) {
                installedApps = appList
                isLoadingApps = false
            }
        }
    }

    // Refresca el estado visible mientras la app está abierta (permisos, zona y contador)
    LaunchedEffect(Unit) {
        while (true) {
            hasNotificationAccess = NotificationManagerCompat
                .getEnabledListenerPackages(context)
                .contains(context.packageName)
            isInsideZone = prefs.isInsideZone
            blockedCount = prefs.blockedCount
            delay(1000)
        }
    }

    // Consulta la dirección actual sin interferir con la geovalla.
    LaunchedEffect(hasLocationPermission) {
        while (true) {
            currentAddress = if (!LocationHelper.hasForegroundPermission(context)) {
                "Permiso de ubicación no concedido"
            } else {
                val address = withContext(Dispatchers.IO) {
                    val location = LocationHelper.getCurrentLocation(context)
                    location?.let {
                        LocationHelper.getAddressFromLocation(context, it)
                    }
                }
                address ?: "Ubicación no disponible"
            }

            delay(120_000L)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showWelcome || showBackgroundGuide) {
            WelcomeScreen(
                onRequestLocationPermission = onRequestLocationPermission,
                onOpenNotificationSettings = {
                    try {
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        )
                    } catch (_: Exception) {
                    }
                },
                onOpenRestrictedSettings = {
                    try {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                        )
                    } catch (_: Exception) {
                    }
                },
                onOpenBackgroundSettings = {
                    try {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                        )
                    } catch (_: Exception) {
                    }
                },
                onOpenBatterySettings = {
                    val pkg = context.packageName
                    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    val isIgnoring = pm?.isIgnoringBatteryOptimizations(pkg) == true

                    fun openAppDetails() {
                        try {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:$pkg")
                                }
                            )
                        } catch (_: Exception) {
                            try {
                                context.startActivity(
                                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                )
                            } catch (_: Exception) {
                            }
                        }
                    }

                    if (!isIgnoring) {
                        try {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:$pkg")
                                }
                            )
                        } catch (_: Exception) {
                            openAppDetails()
                        }
                    } else {
                        openAppDetails()
                    }
                },
                onFinish = {
                    if (showBackgroundGuide) {
                        showBackgroundGuide = false
                    } else {
                        prefs.hasCompletedWelcome = true
                        showWelcome = false
                    }
                },
                initialStep = if (showBackgroundGuide) 2 else 0,
                backgroundGuideOnly = showBackgroundGuide
            )
        } else {
            Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("GeoSilence", fontWeight = FontWeight.Bold)
                        }
                    },
                    actions = {
                        Switch(
                            checked = isMasterEnabled,
                            onCheckedChange = { enabled ->
                                isMasterEnabled = enabled
                                prefs.isMasterEnabled = enabled
                                val operationId = geofenceManager.newOperationId()
                                if (enabled) {
                                    scope.launch {
                                        val registered = geofenceManager.registerGeofences(
                                            prefs.getZones(),
                                            operationId
                                        )
                                        if (
                                            registered &&
                                            GeofenceManager.syncInsideState(
                                                context,
                                                prefs,
                                                operationId
                                            )
                                        ) {
                                            isInsideZone = prefs.isInsideZone
                                        }
                                    }
                                } else {
                                    scope.launch {
                                        geofenceManager.removeGeofences(operationId)
                                    }
                                }
                            },
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null
                            )
                        },
                        label = {
                            Text(
                                "Ubicación",
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )

                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = null
                            )
                        },
                        label = {
                            Text(
                                "Apps (${prefs.getBlockedPackages().size})",
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )

                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null
                            )
                        },
                        label = {
                            Text(
                                "Historial",
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )

                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null
                            )
                        },
                        label = {
                            Text(
                                "Ajustes",
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Permission Banners if required
                if (!hasNotificationAccess) {
                    PermissionWarningCard(
                        title = "Permiso de Acceso a Notificaciones",
                        description = "Es indispensable para interceptar y descartar notificaciones. " +
                            "Si Android dice «Se ha denegado el acceso» (ajustes restringidos): " +
                            "Info. de la app → menú ⋮ → Permitir ajustes restringidos, y vuelve a intentarlo.",
                        buttonText = "Activar en Ajustes",
                        onClick = {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        },
                        secondaryButtonText = "Info. de la app",
                        onSecondaryClick = {
                            try {
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Algunos fabricantes no exponen la pantalla de info de la app
                            }
                        }
                    )
                }

                if (!hasLocationPermission) {
                    PermissionWarningCard(
                        title = "Permiso de Ubicación en 2do Plano",
                        description = "Necesario para detectar cuando entras o sales de la zona geográfica.",
                        buttonText = "Conceder Permisos",
                        onClick = onRequestLocationPermission
                    )
                }

                // Real-time Status Card
                StatusCard(
                    isMasterEnabled = isMasterEnabled,
                    hasActiveZone = savedZone.isEnabled && savedZone.hasValidLocation(),
                    isInsideZone = isInsideZone,
                    blockInside = savedZone.blockInside,
                    blockedCount = blockedCount,
                    currentAddress = currentAddress
                )

                when (selectedTab) {
                    0 -> LocationConfigScreen(
                        zoneName = zoneName,
                        latitude = draftLatitude,
                        longitude = draftLongitude,
                        radius = radiusMeters,
                        blockInside = blockInside,
                        hasChosenLocation = hasChosenLocation,
                        hasChanges = hasChanges,
                        hasSavedZone = savedZone.hasValidLocation(),
                        onZoneNameChange = { zoneName = it },
                        onBlockInsideChange = { blockInside = it },
                        onOpenMapPicker = { showMapPicker = true },
                        onSaveZone = {
                            val newZone = savedZone.copy(
                                name = zoneName.ifBlank { "Mi Zona" },
                                latitude = draftLatitude,
                                longitude = draftLongitude,
                                radiusMeters = radiusMeters,
                                isEnabled = true,
                                blockInside = blockInside
                            )
                            prefs.saveZones(listOf(newZone))
                            savedZone = newZone
                            zoneName = newZone.name
                            val operationId = geofenceManager.newOperationId()
                            // Registrar primero y sincronizar después para evitar estados obsoletos.
                            scope.launch {
                                val ready = if (isMasterEnabled) {
                                    geofenceManager.registerGeofences(
                                        listOf(newZone),
                                        operationId
                                    )
                                } else {
                                    true
                                }

                                if (
                                    ready &&
                                    GeofenceManager.syncInsideState(
                                        context,
                                        prefs,
                                        operationId
                                    )
                                ) {
                                    isInsideZone = prefs.isInsideZone
                                }
                            }
                        }
                    )

                    1 -> AppsListScreen(
                        installedApps = installedApps,
                        searchQuery = searchQuery,
                        isLoading = isLoadingApps,
                        onSearchQueryChange = { searchQuery = it },
                        onToggleApp = { packageName, isBlocked ->
                            prefs.setPackageBlocked(packageName, isBlocked)
                            installedApps = installedApps.map {
                                if (it.packageName == packageName) it.copy(isBlocked = isBlocked) else it
                            }
                        }
                    )

                    2 -> HistoryScreen(
                        prefs = prefs,
                        onClear = {
                            prefs.clearLogs()
                            blockedCount = 0
                        },
                        onNavigateToSettings = { selectedTab = 3 }
                    )

                    3 -> SettingsScreen(
                        prefs = prefs,
                        themeMode = themeMode,
                        onThemeModeChange = onThemeModeChange,
                        onNavigateToHistory = { selectedTab = 2 },
                        onOpenBackgroundGuide = { showBackgroundGuide = true },
                        onToggleSimulation = {
                            val newState = !isInsideZone
                            isInsideZone = newState
                            prefs.isInsideZone = newState
                        }
                    )
                }
            }
        }

        if (showMapPicker) {
            BackHandler { showMapPicker = false }
            MapPickerScreen(
                initialLatitude = draftLatitude,
                initialLongitude = draftLongitude,
                initialRadiusMeters = radiusMeters,
                onConfirm = { lat, lng, radius ->
                    draftLatitude = lat
                    draftLongitude = lng
                    radiusMeters = radius
                    showMapPicker = false
                },
                onDismiss = { showMapPicker = false },
                onRequestLocationPermission = onRequestLocationPermission
            )
        }
        }
    }
}

@Composable
fun StatusCard(
    isMasterEnabled: Boolean,
    hasActiveZone: Boolean,
    isInsideZone: Boolean,
    blockInside: Boolean = true,
    blockedCount: Int,
    currentAddress: String
) {
    val isBlocking = isMasterEnabled &&
        hasActiveZone &&
        (isInsideZone == blockInside)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!isMasterEnabled) MaterialTheme.colorScheme.surfaceVariant
            else if (isBlocking) StatusGreen.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(
                                if (!isMasterEnabled) StatusRed
                                else if (isBlocking) StatusGreen
                                else StatusGray
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            !isMasterEnabled -> "Bloqueo desactivado"
                            !hasActiveZone -> "Sin zona activa"
                            isBlocking -> "BLOQUEANDO notificaciones"
                            else -> "PERMITIENDO notificaciones"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (!isMasterEnabled) MaterialTheme.colorScheme.error else Color.Unspecified
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when {
                    !hasActiveZone -> "Configura una ubicación para activar el bloqueo"
                    isInsideZone -> "Estás dentro de la zona"
                    else -> "Estás fuera de la zona"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Tu ubicación: $currentAddress",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Notificaciones bloqueadas: $blockedCount",
                style = MaterialTheme.typography.bodyMedium
            )

        }
    }
}

@Composable
fun PermissionWarningCard(
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit,
    secondaryButtonText: String? = null,
    onSecondaryClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
            Text(description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(buttonText)
                }
                if (secondaryButtonText != null && onSecondaryClick != null) {
                    OutlinedButton(
                        onClick = onSecondaryClick,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text(secondaryButtonText)
                    }
                }
            }
        }
    }
}

@Composable
fun LocationConfigScreen(
    zoneName: String,
    latitude: Double,
    longitude: Double,
    radius: Float,
    blockInside: Boolean,
    hasChosenLocation: Boolean,
    hasChanges: Boolean,
    hasSavedZone: Boolean,
    onZoneNameChange: (String) -> Unit,
    onBlockInsideChange: (Boolean) -> Unit,
    onOpenMapPicker: () -> Unit,
    onSaveZone: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            "Configurar Zona Geográfica",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Define dónde y cuándo deben aplicarse los bloqueos.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = zoneName,
            onValueChange = onZoneNameChange,
            label = { Text("Nombre del lugar (ej. Oficina, Escuela, Casa)") },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Resumen de la zona elegida en el selector a pantalla completa
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                if (hasChosenLocation) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Zona seleccionada",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Lat: ${String.format(Locale.US, "%.5f", latitude)} · " +
                            "Lng: ${String.format(Locale.US, "%.5f", longitude)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Radio: ${formatRadiusDefault(radius)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = "Aún no has elegido la ubicación",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onOpenMapPicker,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (hasChosenLocation) "Cambiar en el mapa" else "Elegir en el mapa")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Comportamiento: bloquear dentro de la zona o fuera de ella
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Comportamiento",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBlockInsideChange(true) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = blockInside,
                        onCheckedChange = { checked -> onBlockInsideChange(checked) }
                    )
                    Text("Bloquear dentro de la zona", style = MaterialTheme.typography.bodyMedium)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBlockInsideChange(false) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = !blockInside,
                        onCheckedChange = { checked -> onBlockInsideChange(!checked) }
                    )
                    Text("Bloquear fuera de la zona", style = MaterialTheme.typography.bodyMedium)
                }

                Text(
                    text = if (blockInside) "Dentro: bloquea · Fuera: permite"
                    else "Dentro: permite · Fuera: bloquea",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSaveZone,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            enabled = hasChosenLocation
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Guardar y Activar Zona")
        }

        Spacer(modifier = Modifier.height(8.dp))
        if (!hasChosenLocation) {
            Text(
                "Elige la ubicación en el mapa para poder guardar.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        } else if (hasSavedZone && !hasChanges) {
            Text(
                "✓ Zona guardada y geofence activo",
                color = StatusGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        } else {
            Text(
                "Hay cambios sin guardar",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
    }
}

private fun formatRadiusDefault(meters: Float): String =
    if (meters >= 1000f) {
        String.format(Locale.getDefault(), "%.1f km", meters / 1000f)
    } else {
        "${meters.toInt()} m"
    }

@Composable
fun AppsListScreen(
    installedApps: List<InstalledAppInfo>,
    searchQuery: String,
    isLoading: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onToggleApp: (String, Boolean) -> Unit
) {
    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) installedApps
        else installedApps.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("Buscar aplicación...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            singleLine = true
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    AppRowItem(
                        app = app,
                        onToggle = { isBlocked ->
                            onToggleApp(app.packageName, isBlocked)
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun AppRowItem(
    app: InstalledAppInfo,
    onToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var appIconBitmap by remember(app.packageName) {
        mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
    }

    LaunchedEffect(app.packageName) {
        withContext(Dispatchers.IO) {
            try {
                val iconDrawable = context.packageManager.getApplicationIcon(app.packageName)
                val bitmap = iconDrawable.toBitmap(96, 96)
                withContext(Dispatchers.Main) {
                    appIconBitmap = bitmap.asImageBitmap()
                }
            } catch (e: Exception) {
                // Default icon fallback
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
            )
            .clickable { onToggle(!app.isBlocked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (appIconBitmap != null) {
            Image(
                bitmap = appIconBitmap!!,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Android, contentDescription = null)
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Switch(
            checked = app.isBlocked,
            onCheckedChange = { onToggle(it) }
        )
    }
}

@Composable
fun HistoryScreen(
    prefs: AppPreferences,
    onClear: () -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    var logs by remember { mutableStateOf(prefs.getRecentLogs()) }
    val currentRetention = remember { prefs.historyRetention }
    val dateFormatter = remember { SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        // Banner con información de la auto-eliminación periódica activa
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .clickable { onNavigateToSettings() },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoDelete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Auto-eliminación: ${currentRetention.title}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = currentRetention.description,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                Text(
                    text = "Ajustes",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Notificaciones descartadas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (logs.isNotEmpty()) {
                TextButton(onClick = {
                    onClear()
                    logs = emptyList()
                }) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Limpiar todo")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Aún no se ha bloqueado ninguna notificación.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(logs, key = { it.id }) { log ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    log.appName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    dateFormatter.format(Date(log.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                log.title,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
