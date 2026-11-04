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

private val Light = lightColorScheme(
    primary = Color(0xFF00796B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2DFDB),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFF4A635F),
    secondaryContainer = Color(0xFFCCE8E2),
    tertiary = Color(0xFF456179),
    tertiaryContainer = Color(0xFFCCE5FF),
    background = Color(0xFFF7FAF9),
    surface = Color(0xFFF7FAF9),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF80CBC4),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF005048),
    onPrimaryContainer = Color(0xFFA0F2E9),
    secondary = Color(0xFFB1CCC6),
    secondaryContainer = Color(0xFF334B47),
    tertiary = Color(0xFFADCAE6),
    tertiaryContainer = Color(0xFF2D4960),
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
        @Composable get() = if (isDark()) Color(0xFFFF8A80) else Color(0xFFD32F2F)
    val income: Color
        @Composable get() = if (isDark()) Color(0xFF81C784) else Color(0xFF2E7D32)
    val transfer: Color
        @Composable get() = MaterialTheme.colorScheme.tertiary

    @Composable
    private fun isDark() = MaterialTheme.colorScheme.surface.luminance() < 0.5f
}
