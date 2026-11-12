package com.personal.geosilence.ui

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun WelcomeScreen(
    onRequestLocationPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenRestrictedSettings: () -> Unit,
    onOpenBackgroundSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onFinish: () -> Unit,
    initialStep: Int = 0,
    backgroundGuideOnly: Boolean = false
) {
    var currentStep by remember(initialStep) {
        mutableIntStateOf(initialStep.coerceIn(0, 2))
    }

    val manufacturer = Build.MANUFACTURER.orEmpty()
        .uppercase()
    val brand = Build.BRAND.orEmpty()
        .uppercase()

    val isHonor = manufacturer.contains("HONOR") || brand.contains("HONOR")
    val isHuawei = manufacturer.contains("HUAWEI") || brand.contains("HUAWEI")
    val isSamsung = manufacturer.contains("SAMSUNG") || brand.contains("SAMSUNG")
    val isXiaomi = manufacturer.contains("XIAOMI") ||
        brand.contains("XIAOMI") ||
        brand.contains("REDMI") ||
        brand.contains("POCO")
    val isMotorola = manufacturer.contains("MOTOROLA") ||
        brand.contains("MOTOROLA")

    val backgroundInstructions = when {
        isHonor -> """
            En HONOR (MagicOS):

            1. Abre Ajustes → Aplicaciones → Inicio de aplicaciones.
            2. Busca GeoSilence.
            3. Desactiva «Gestionar automáticamente».
            4. Activa estas tres opciones:
               • Inicio automático.
               • Inicio secundario.
               • Ejecutar en segundo plano.

            Esta configuración ayuda a evitar que el sistema cierre GeoSilence automáticamente.
        """.trimIndent()

        isHuawei -> """
            En Huawei (EMUI):

            1. Abre Ajustes → Aplicaciones → Inicio de aplicaciones.
            2. Busca GeoSilence.
            3. Desactiva la gestión automática.
            4. Permite el inicio automático, el inicio secundario y la ejecución en segundo plano si aparecen.

            Los nombres pueden variar según la versión de EMUI.
        """.trimIndent()

        isSamsung -> """
            En Samsung:

            1. Abre Ajustes → Batería.
            2. Busca los límites de uso en segundo plano.
            3. Comprueba que GeoSilence no esté en las aplicaciones suspendidas ni en suspensión profunda.
            4. Si está disponible, añádela a las aplicaciones que nunca se suspenden.

            Los nombres pueden variar según la versión de One UI.
        """.trimIndent()

        isXiaomi -> """
            En Xiaomi, Redmi o POCO:

            1. Abre Ajustes → Aplicaciones → Permisos.
            2. Busca el permiso de inicio automático y permite GeoSilence si existe.
            3. En los ajustes de batería de GeoSilence, selecciona «Sin restricciones» si está disponible.

            Los menús pueden variar según MIUI o HyperOS.
        """.trimIndent()

        isMotorola -> """
            En Motorola:

            1. Abre Ajustes → Aplicaciones → GeoSilence.
            2. Revisa el uso de batería de la aplicación.
            3. Permite la actividad en segundo plano o el uso sin restricciones si esas opciones aparecen.

            Los nombres dependen de la versión de Android.
        """.trimIndent()

        else -> """
            En algunos teléfonos, el fabricante añade restricciones adicionales
            para ahorrar batería.

            1. Abre la información de GeoSilence en los ajustes del teléfono.
            2. Busca las opciones de batería y actividad en segundo plano.
            3. Permite la ejecución en segundo plano y el inicio automático si existen.

            Si tu teléfono no ofrece estas opciones, no necesitas configurarlas.
        """.trimIndent()
    }

    val titles = listOf(
        "Permiso de ubicación",
        "Acceso a notificaciones",
        "Protección en segundo plano"
    )

    val descriptions = listOf(
        "GeoSilence necesita conocer tu ubicación para detectar cuándo entras o sales de la zona configurada.",
        "Este acceso permite detectar y descartar las notificaciones de las aplicaciones que hayas seleccionado.",
        "Android y algunos fabricantes pueden limitar la actividad en segundo plano para ahorrar batería o cerrar aplicaciones."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            modifier = Modifier.size(68.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "GEOSILENCE",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (backgroundGuideOnly) "Protección en segundo plano"
                else "Bienvenido a GeoSilence",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (backgroundGuideOnly)
                "Consulta cómo evitar que tu teléfono cierre GeoSilence."
            else "Vamos a preparar la aplicación para que funcione correctamente.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (!backgroundGuideOnly) {
            LinearProgressIndicator(
                progress = { (currentStep + 1) / 3f },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Paso ${currentStep + 1} de 3",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = titles[currentStep],
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = descriptions[currentStep],
                    style = MaterialTheme.typography.bodyMedium
                )

                if (currentStep == 1) {
                    Text(
                        text = """
                            ¿Android bloquea el acceso?

                            Si has instalado GeoSilence mediante un APK, Android puede
                            bloquear este permiso por seguridad.

                            Para permitirlo:
                            1. Abre Información de la aplicación de GeoSilence.
                            2. Pulsa los tres puntos de la esquina superior.
                            3. Selecciona «Permitir ajustes restringidos», si aparece.
                            4. Vuelve aquí y activa el acceso a notificaciones.
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (currentStep) {
                    0 -> OutlinedButton(
                        onClick = onRequestLocationPermission,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Configurar ubicación")
                    }

                    1 -> {
                        OutlinedButton(
                            onClick = onOpenNotificationSettings,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Activar acceso a notificaciones")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = onOpenRestrictedSettings,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Abrir ajustes restringidos")
                        }
                    }

                    2 -> {
                        Text(
                            text = backgroundInstructions,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = onOpenBackgroundSettings,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Abrir ajustes de GeoSilence")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = onOpenBatterySettings,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.BatteryChargingFull, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Configurar batería")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (backgroundGuideOnly) {
            TextButton(onClick = onFinish) {
                Text("Cerrar guía")
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 0) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Atrás")
                    }
                }

                Button(
                    onClick = {
                        if (currentStep < 2) currentStep++
                        else onFinish()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (currentStep == 2) "Finalizar" else "Siguiente")
                }
            }

            TextButton(onClick = onFinish) {
                Text("Omitir bienvenida")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}
