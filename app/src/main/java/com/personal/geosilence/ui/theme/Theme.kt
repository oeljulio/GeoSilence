package com.personal.geosilence.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.personal.geosilence.data.AppThemeMode

private val LightColorScheme = lightColorScheme(
    primary = Emerald,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD0F5E2),
    onPrimaryContainer = Color(0xFF002114),
    secondary = Color(0xFF426B57),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC5EBD2),
    onSecondaryContainer = Color(0xFF002112),
    tertiary = Color(0xFF38656D),
    background = Color(0xFFF8FBF8),
    surface = Color(0xFFF8FBF8),
    surfaceVariant = Color(0xFFE0E9E2),
    onSurface = Color(0xFF191D1A),
    onSurfaceVariant = Color(0xFF414942),
    outline = Color(0xFF717A72)
)

private val SystemDarkColorScheme = darkColorScheme(
    primary = EmeraldLight,
    onPrimary = Color(0xFF00391F),
    primaryContainer = Color(0xFF00522E),
    onPrimaryContainer = Color(0xFF9FF5BC),
    secondary = Color(0xFFA9D0B7),
    onSecondary = Color(0xFF123724),
    secondaryContainer = Color(0xFF294D37),
    onSecondaryContainer = Color(0xFFC5EBD2),
    tertiary = Color(0xFFA0CFD7),
    background = Color(0xFF111411),
    surface = Color(0xFF111411),
    surfaceVariant = Color(0xFF303A32),
    onSurface = Color(0xFFE1E9E1),
    onSurfaceVariant = Color(0xFFC0CAC1),
    outline = Color(0xFF89948B)
)

private val AmoledColorScheme = darkColorScheme(
    primary = EmeraldLight,
    onPrimary = Color(0xFF00391F),
    primaryContainer = Color(0xFF00522E),
    onPrimaryContainer = Color(0xFF9FF5BC),
    secondary = Color(0xFFA9D0B7),
    onSecondary = Color(0xFF123724),
    secondaryContainer = Color(0xFF173D2A),
    onSecondaryContainer = Color(0xFFC5EBD2),
    tertiary = Color(0xFFA0CFD7),
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF171D19),
    onSurface = Color(0xFFE6F2E9),
    onSurfaceVariant = Color(0xFFB8C9BD),
    outline = Color(0xFF84958A)
)

@Composable
fun GeoSilenceTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        AppThemeMode.LIGHT -> LightColorScheme
        AppThemeMode.DARK_AMOLED -> AmoledColorScheme
        AppThemeMode.SYSTEM -> {
            if (isSystemInDarkTheme()) SystemDarkColorScheme else LightColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
