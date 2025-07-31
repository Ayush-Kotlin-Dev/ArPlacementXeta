package com.xeta.arplacement.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF03A9F4), // changed to vibrant blue
    onPrimary = Color(0xFF001F3F),
    primaryContainer = Color(0xFF01579B), // changed to vibrant blue container
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF009688), // changed to vibrant teal
    onSecondary = Color(0xFF003A36),
    secondaryContainer = Color(0xFF00695C), // changed to vibrant teal container
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFFFFC107), // changed to vibrant amber
    onTertiary = Color(0xFF2D1600),
    tertiaryContainer = Color(0xFF8A4F00),
    onTertiaryContainer = Color.White,
    error = Color(0xFFE91E63), // changed to vibrant red
    onError = Color.White,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color.White,
    surface = Color(0xFF0F0D13),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF2B2930),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    background = Color(0xFF0F0D13),
    onBackground = Color(0xFFE6E1E5)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2196F3), // changed to vibrant blue
    onPrimary = Color.White,
    primaryContainer = Color(0xFF03A9F4), // changed to vibrant blue container
    onPrimaryContainer = Color(0xFF001F3F),
    secondary = Color(0xFF009688), // changed to vibrant teal
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF00695C), // changed to vibrant teal container
    onSecondaryContainer = Color(0xFF003A36),
    tertiary = Color(0xFFFFC107), // changed to vibrant amber
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDB3),
    onTertiaryContainer = Color(0xFF2D1600),
    error = Color(0xFFE91E63), // changed to vibrant red
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F)
)

@Composable
fun ArPlacementXetaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled to use our custom colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}