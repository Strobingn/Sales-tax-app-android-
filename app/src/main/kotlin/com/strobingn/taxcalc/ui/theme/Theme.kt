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
    primary = Color(0xFF455A64),      // Dark slate grey (buttons, primary actions)
    onPrimary = Color.White,          // White text on primary
    primaryContainer = Color(0xFFE0E0E0),  // Light grey container
    onPrimaryContainer = Color(0xFF212121), // Dark grey text
    
    secondary = Color(0xFF757575),    // Medium grey (secondary elements)
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF5F5F5),
    onSecondaryContainer = Color(0xFF424242),
    
    tertiary = Color(0xFF607D8B),      // Blue grey accent
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFECEFF1),
    onTertiaryContainer = Color(0xFF37474F),
    
    surface = Color(0xFFFAFAFA),       // Very light grey background
    onSurface = Color(0xFF212121),    // Dark grey text
    surfaceVariant = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFF424242),
    surfaceContainer = Color(0xFFF5F5F5),
    
    error = Color(0xFFB00020),        // Modern red
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    
    background = Color(0xFFFAFAFA),    // Very light grey
    onBackground = Color(0xFF212121),  // Dark grey
    outline = Color(0xFFBDBDBD)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF90A4AE),      // Light blue grey (buttons glow)
    onPrimary = Color.Black,          // Black text on primary
    primaryContainer = Color(0xFF607D8B),
    onPrimaryContainer = Color.White,
    
    secondary = Color(0xFFAFAFAF),    // Light grey
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF424242),
    onSecondaryContainer = Color(0xFFE0E0E0),
    
    tertiary = Color(0xFF78909C),      // Light blue grey accent
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF455A64),
    onTertiaryContainer = Color(0xFFECEFF1),
    
    surface = Color(0xFF121212),       // Very dark grey background
    onSurface = Color(0xFFE0E0E0),    // Light grey text
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFBDBDBD),
    surfaceContainer = Color(0xFF212121),
    
    error = Color(0xFFCF6679),        // Modern error color
    onError = Color.Black,
    errorContainer = Color(0xFF410002),
    onErrorContainer = Color(0xFFFFDAD6),
    
    background = Color(0xFF121212),    // Very dark grey
    onBackground = Color(0xFFE0E0E0), // Light grey
    outline = Color(0xFF424242)
)

@Composable
fun TaxCalcTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,  // Disabled for consistent grey theme
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
