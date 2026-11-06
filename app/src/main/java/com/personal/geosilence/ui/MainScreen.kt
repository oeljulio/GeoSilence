package com.personal.geosilence.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.android.gms.location.LocationServices
import com.personal.geosilence.data.AppPreferences
import com.personal.geosilence.data.GeofenceZone
import com.personal.geosilence.data.InstalledAppInfo
import com.personal.geosilence.location.GeofenceManager
import com.personal.geosilence.ui.map.InteractiveMapView
import com.personal.geosilence.ui.theme.StatusGreen
import com.personal.geosilence.ui.theme.StatusRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    hasLocationPermission: Boolean,
    onRequestLocationPermission: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences.getInstance(context) }
    val geofenceManager = remember { GeofenceManager(context) }
    val scope = rememberCoroutineScope()

    var isMasterEnabled by remember { mutableStateOf(prefs.isMasterEnabled) }
    var isInsideZone by remember { mutableStateOf(prefs.isInsideZone) }
    var blockedCount by remember { mutableStateOf(prefs.blockedCount) }

    // Geofence Zone state
    val zones = remember { prefs.getZones() }
    val currentZone = zones.firstOrNull() ?: GeofenceZone(name = "Zona Principal", radiusMeters = 200f)
    var zoneName by remember { mutableStateOf(currentZone.name) }
    var latitudeText by remember { mutableStateOf(if (currentZone.latitude != 0.0) currentZone.latitude.toString() else "") }
    var longitudeText by remember { mutableStateOf(if (currentZone.longitude != 0.0) currentZone.longitude.toString() else "") }
    var radiusMeters by remember { mutableFloatStateOf(currentZone.radiusMeters) }

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
                            if (enabled) {
                                geofenceManager.registerGeofences(prefs.getZones())
                            } else {
                                geofenceManager.removeGeofences()
                            }
                        },
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            )
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
                    description = "Es indispensable para interceptar y descartar notificaciones.",
                    buttonText = "Activar en Ajustes",
                    onClick = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        context.startActivity(intent)
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
                isInsideZone = isInsideZone,
                blockedCount = blockedCount,
                onToggleSimulation = {
                    // Botón para probar manualmente el estado sin moverse
                    val newState = !isInsideZone
                    isInsideZone = newState
                    prefs.isInsideZone = newState
                }
            )

            // Tabs for navigation: Zonas, Apps a Bloquear, Historial, Ajustes
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Ubicación") },
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Apps (${prefs.getBlockedPackages().size})") },
                    icon = { Icon(Icons.Default.Apps, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Historial") },
                    icon = { Icon(Icons.Default.History, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Ajustes") },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) }
                )
            }

            when (selectedTab) {
                0 -> LocationConfigScreen(
                    zoneName = zoneName,
                    latitude = latitudeText,
                    longitude = longitudeText,
                    radius = radiusMeters,
                    onZoneNameChange = { zoneName = it },
                    onLatitudeChange = { latitudeText = it },
                    onLongitudeChange = { longitudeText = it },
                    onRadiusChange = { radiusMeters = it },
                    onGetCurrentLocation = {
                        try {
                            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                            fusedClient.lastLocation.addOnSuccessListener { loc ->
                                if (loc != null) {
                                    latitudeText = loc.latitude.toString()
                                    longitudeText = loc.longitude.toString()
                                }
                            }
                        } catch (e: SecurityException) {
                            onRequestLocationPermission()
                        }
                    },
                    onSaveZone = {
                        val lat = latitudeText.toDoubleOrNull() ?: 0.0
                        val lng = longitudeText.toDoubleOrNull() ?: 0.0
                        val newZone = GeofenceZone(
                            id = currentZone.id,
                            name = zoneName.ifBlank { "Mi Zona" },
                            latitude = lat,
                            longitude = lng,
                            radiusMeters = radiusMeters,
                            isEnabled = true
                        )
                        prefs.saveZones(listOf(newZone))
                        geofenceManager.registerGeofences(listOf(newZone))
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
                    onNavigateToHistory = { selectedTab = 2 }
                )
            }
        }
    }
}

@Composable
fun StatusCard(
    isMasterEnabled: Boolean,
    isInsideZone: Boolean,
    blockedCount: Int,
    onToggleSimulation: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!isMasterEnabled) MaterialTheme.colorScheme.surfaceVariant
            else if (isInsideZone) StatusGreen.copy(alpha = 0.15f)
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
                                else if (isInsideZone) StatusGreen
                                else MaterialTheme.colorScheme.outline
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (!isMasterEnabled) "Bloqueo Desactivado"
                        else if (isInsideZone) "DENTRO DE LA ZONA (Bloqueando)"
                        else "FUERA DE LA ZONA (Permitiendo)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Notificaciones bloqueadas: $blockedCount",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onToggleSimulation,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Alternar estado (Test)", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun PermissionWarningCard(
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit
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
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(buttonText)
            }
        }
    }
}

@Composable
fun LocationConfigScreen(
    zoneName: String,
    latitude: String,
    longitude: String,
    radius: Float,
    onZoneNameChange: (String) -> Unit,
    onLatitudeChange: (String) -> Unit,
    onLongitudeChange: (String) -> Unit,
    onRadiusChange: (Float) -> Unit,
    onGetCurrentLocation: () -> Unit,
    onSaveZone: () -> Unit
) {
    var saveSuccessMessage by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val latDouble = latitude.toDoubleOrNull() ?: 0.0
    val lngDouble = longitude.toDoubleOrNull() ?: 0.0
    val presetRadii = listOf(100f, 250f, 500f, 1000f, 2000f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text("Configurar Zona Geográfica", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = zoneName,
            onValueChange = onZoneNameChange,
            label = { Text("Nombre del lugar (ej. Oficina, Escuela, Casa)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Vista interactiva del mapa con OpenStreetMap y Leaflet
        InteractiveMapView(
            latitude = latDouble,
            longitude = lngDouble,
            radiusMeters = radius,
            onLocationSelected = { newLat, newLng ->
                onLatitudeChange(String.format(Locale.US, "%.6f", newLat))
                onLongitudeChange(String.format(Locale.US, "%.6f", newLng))
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Control de Radio Interactivo
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Radio de Silencio:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (radius >= 1000f) {
                                String.format(Locale.getDefault(), "%.1f km", radius / 1000f)
                            } else {
                                "${radius.toInt()} m"
                            },
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Slider(
                    value = radius,
                    onValueChange = onRadiusChange,
                    valueRange = 50f..3000f,
                    steps = 58
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    presetRadii.forEach { preset ->
                        val label = if (preset >= 1000f) "${(preset / 1000).toInt()} km" else "${preset.toInt()} m"
                        val isSelected = Math.abs(radius - preset) < 25f

                        FilterChip(
                            selected = isSelected,
                            onClick = { onRadiusChange(preset) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = latitude,
                onValueChange = onLatitudeChange,
                label = { Text("Latitud") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = longitude,
                onValueChange = onLongitudeChange,
                label = { Text("Longitud") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onGetCurrentLocation,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors()
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Usar mi ubicación GPS actual")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                onSaveZone()
                saveSuccessMessage = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Guardar y Activar Zona")
        }

        if (saveSuccessMessage) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "¡Zona guardada y geofence activada correctamente!",
                color = StatusGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            singleLine = true
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredApps, key = { it.packageName }) { app ->
                    AppRowItem(
                        app = app,
                        onToggle = { isBlocked ->
                            onToggleApp(app.packageName, isBlocked)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
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
            .clickable { onToggle(!app.isBlocked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (appIconBitmap != null) {
            Image(
                bitmap = appIconBitmap!!,
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Android, contentDescription = null)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
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
            .padding(16.dp)
    ) {
        // Banner con información de la auto-eliminación periódica activa
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .clickable { onNavigateToSettings() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
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
            Text("Notificaciones Descartadas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                Text("Aún no se ha bloqueado ninguna notificación.", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(logs, key = { it.id }) { log ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(log.appName, fontWeight = FontWeight.Bold)
                                Text(
                                    dateFormatter.format(Date(log.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(log.title, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
