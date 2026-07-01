package com.strobingn.taxcalc.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Color(0xFF0D9488),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF134E4A),
    secondary = Color(0xFF134E4A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2DFDB),
    onSecondaryContainer = Color(0xFF134E4A),
    tertiary = Color(0xFF14B8A6),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF99F6E4),
    onTertiaryContainer = Color(0xFF134E4A),
    surface = Color(0xFFFAFAF9),
    onSurface = Color(0xFF1C1917),
    surfaceVariant = Color(0xFFF5F5F4),
    onSurfaceVariant = Color(0xFF78716C),
    surfaceContainer = Color(0xFFF5F5F4),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF7F1D1D),
    background = Color(0xFFFAFAF9),
    onBackground = Color(0xFF1C1917),
    outline = Color(0xFFA8A29E)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF14B8A6),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF134E4A),
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Color(0xFF5EEAD4),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF115E59),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = Color(0xFF2DD4BF),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF0F766E),
    onTertiaryContainer = Color(0xFF99F6E4),
    surface = Color(0xFF1C1917),
    onSurface = Color(0xFFE7E5E4),
    surfaceVariant = Color(0xFF292524),
    onSurfaceVariant = Color(0xFFA8A29E),
    surfaceContainer = Color(0xFF292524),
    error = Color(0xFFFF7C7C),
    onError = Color.Black,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFFE4E6),
    background = Color(0xFF1C1917),
    onBackground = Color(0xFFE7E5E4),
    outline = Color(0xFF57534E)
)

@Composable
fun TaxCalcTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
