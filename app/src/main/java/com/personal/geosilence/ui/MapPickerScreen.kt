package com.personal.geosilence.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.geosilence.location.LocationHelper
import com.personal.geosilence.ui.map.InteractiveMapView
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

private const val FALLBACK_LAT = 40.4168
private const val FALLBACK_LNG = -3.7038

private fun formatRadius(meters: Float): String =
    if (meters >= 1000f) {
        String.format(Locale.getDefault(), "%.1f km", meters / 1000f)
    } else {
        "${meters.toInt()} m"
    }

/**
 * Selector de la zona a pantalla completa. Trabaja con un borrador local:
 * "Listo" lo devuelve por onConfirm y cerrar/atrás lo descarta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerScreen(
    initialLatitude: Double,
    initialLongitude: Double,
    initialRadiusMeters: Float,
    onConfirm: (latitude: Double, longitude: Double, radiusMeters: Float) -> Unit,
    onDismiss: () -> Unit,
    onRequestLocationPermission: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnRequestPermission by rememberUpdatedState(onRequestLocationPermission)

    val hasZone = initialLatitude != 0.0 && initialLongitude != 0.0
    var latitude by remember { mutableStateOf(if (hasZone) initialLatitude else FALLBACK_LAT) }
    var longitude by remember { mutableStateOf(if (hasZone) initialLongitude else FALLBACK_LNG) }
    var radius by remember { mutableFloatStateOf(initialRadiusMeters) }
    var isLoadingLocation by remember { mutableStateOf(!hasZone) }

    // Sin zona guardada: se parte de la ubicación GPS actual, con Madrid como respaldo
    LaunchedEffect(Unit) {
        if (hasZone) return@LaunchedEffect
        val location = LocationHelper.getCurrentLocation(context)
        if (location != null) {
            latitude = location.latitude
            longitude = location.longitude
        } else {
            latitude = FALLBACK_LAT
            longitude = FALLBACK_LNG
            if (!LocationHelper.hasForegroundPermission(context)) {
                currentOnRequestPermission()
            }
        }
        isLoadingLocation = false
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            TopAppBar(
                title = { Text("Elegir zona", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                },
                actions = {
                    TextButton(onClick = { onConfirm(latitude, longitude, radius) }) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Listo", fontWeight = FontWeight.Bold)
                    }
                }
            )

            Box(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)) {
                if (isLoadingLocation) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Buscando tu ubicación…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    InteractiveMapView(
                        latitude = latitude,
                        longitude = longitude,
                        radiusMeters = radius,
                        onLocationSelected = { newLat, newLng ->
                            latitude = newLat
                            longitude = newLng
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    SmallFloatingActionButton(
                        onClick = {
                            scope.launch {
                                val location = LocationHelper.getCurrentLocation(context)
                                if (location != null) {
                                    latitude = location.latitude
                                    longitude = location.longitude
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 36.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Mi ubicación")
                    }
                }
            }

            RadiusPanel(radius = radius, onRadiusChange = { radius = it })
        }
    }
}

@Composable
private fun RadiusPanel(radius: Float, onRadiusChange: (Float) -> Unit) {
    val presets = listOf(100f, 250f, 500f, 1000f, 2000f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        tonalElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Radio de silencio",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = formatRadius(radius),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Slider(
                value = radius,
                onValueChange = onRadiusChange,
                valueRange = 50f..3000f,
                steps = 58
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    FilterChip(
                        selected = abs(radius - preset) < 25f,
                        onClick = { onRadiusChange(preset) },
                        label = { Text(formatRadius(preset), fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Toca el mapa o arrastra el pin rojo para centrar la zona",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
