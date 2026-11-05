package com.byte786.expensetracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.byte786.expensetracker.ExpenseApp

// Cat theme: cream paper, dark-brown ink, ginger accents.
private val Light = lightColorScheme(
    primary = Color(0xFFA8520F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF7DCC3),
    onPrimaryContainer = Color(0xFF3A1D05),
    secondary = Color(0xFF6B5A4C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E7DA),
    onSecondaryContainer = Color(0xFF2A1F17),
    tertiary = Color(0xFF4E5D6C),
    tertiaryContainer = Color(0xFFDCE6EF),
    background = Color(0xFFFFF7EE),
    onBackground = Color(0xFF2A1F17),
    surface = Color(0xFFFFF7EE),
    onSurface = Color(0xFF2A1F17),
    surfaceVariant = Color(0xFFF3E7DA),
    onSurfaceVariant = Color(0xFF6B5A4C),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFBF6),
    surfaceContainer = Color(0xFFFBF0E4),
    surfaceContainerHigh = Color(0xFFF6E9DB),
    surfaceContainerHighest = Color(0xFFF1E2D2),
    outline = Color(0xFF9C8A7A),
    outlineVariant = Color(0xFFEADBCB),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFF2A55E),
    onPrimary = Color(0xFF4A2300),
    primaryContainer = Color(0xFF6B3608),
    onPrimaryContainer = Color(0xFFFFDCC2),
    secondary = Color(0xFFD8C3B2),
    secondaryContainer = Color(0xFF3D3128),
    onSecondaryContainer = Color(0xFFF3E7DA),
    tertiary = Color(0xFFB4C8DA),
    background = Color(0xFF1C1611),
    onBackground = Color(0xFFF1E2D2),
    surface = Color(0xFF1C1611),
    onSurface = Color(0xFFF1E2D2),
    surfaceVariant = Color(0xFF3D3128),
    onSurfaceVariant = Color(0xFFD8C3B2),
    surfaceContainerLowest = Color(0xFF16110D),
    surfaceContainerLow = Color(0xFF241D17),
    surfaceContainer = Color(0xFF29211A),
    surfaceContainerHigh = Color(0xFF332A22),
    surfaceContainerHighest = Color(0xFF3D332A),
    outline = Color(0xFF9C8A7A),
    outlineVariant = Color(0xFF4A3D32),
)

/** Currency symbol shown before every amount. */
val LocalCurrency = staticCompositionLocalOf { "৳" }

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val prefs = ExpenseApp.instance.repo.prefs
    val mode by prefs.themeMode.collectAsState()
    val dynamic by prefs.dynamicColor.collectAsState()
    val currency by prefs.currency.collectAsState()
    val dark = when (mode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val ctx = LocalContext.current
    val scheme = when {
        dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> Dark
        else -> Light
    }
    CompositionLocalProvider(LocalCurrency provides currency) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

object MoneyColors {
    val expense: Color
        @Composable get() = if (isDark()) Color(0xFFFF8A80) else Color(0xFFB3261E)
    val income: Color
        @Composable get() = if (isDark()) Color(0xFF81C784) else Color(0xFF1E7A34)
    val transfer: Color
        @Composable get() = MaterialTheme.colorScheme.tertiary

    @Composable
    private fun isDark() = MaterialTheme.colorScheme.surface.luminance() < 0.5f
}
