package com.byte786.expensetracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.byte786.expensetracker.R
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.byte786.expensetracker.ui.Money
import com.byte786.expensetracker.ui.MoneyStyle
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

/** Changing the money format re-draws every screen (static local). */
val LocalMoneyStyle = staticCompositionLocalOf { MoneyStyle() }

/** Handmade look on: pencil card outlines (see ui.components.sketchBorder) and the handwriting font. */
val LocalHandmade = staticCompositionLocalOf { false }

/** Patrick Hand (SIL Open Font License), bundled in res/font. */
val HandFont = FontFamily(Font(R.font.patrick_hand))

private fun handTypography(): Typography {
    val d = Typography()
    fun TextStyle.hand(scale: Float = 1.08f) = copy(fontFamily = HandFont, fontSize = fontSize * scale, letterSpacing = 0.sp)
    return d.copy(
        displayLarge = d.displayLarge.hand(), displayMedium = d.displayMedium.hand(), displaySmall = d.displaySmall.hand(),
        headlineLarge = d.headlineLarge.hand(), headlineMedium = d.headlineMedium.hand(), headlineSmall = d.headlineSmall.hand(),
        titleLarge = d.titleLarge.hand(1.12f), titleMedium = d.titleMedium.hand(1.12f), titleSmall = d.titleSmall.hand(1.1f),
        bodyLarge = d.bodyLarge.hand(), bodyMedium = d.bodyMedium.hand(), bodySmall = d.bodySmall.hand(1.1f),
        labelLarge = d.labelLarge.hand(1.1f), labelMedium = d.labelMedium.hand(1.1f), labelSmall = d.labelSmall.hand(1.1f),
    )
}

/** Colours of the status cat, matching the theme. */
data class CatStyle(val fur: Color, val eyes: Color, val ink: Color, val furDark: Color)

val LocalCatStyle = staticCompositionLocalOf { CatStyle(Color(0xFFE8913A), Color(0xFF2A1F17), Color(0xFF2A1F17), Color(0xFFC46F22)) }

/**
 * Colour themes, each named after a cat. Ginger keeps the original hand-tuned colours;
 * the others are derived from a few key colours.
 */
enum class CatPalette(
    val label: String,
    val lightPrimary: Color,
    val lightBg: Color,
    val lightInk: Color,
    val darkPrimary: Color,
    val darkBg: Color,
    val darkInk: Color,
    val cat: CatStyle,
) {
    SKETCHBOOK(
        "Sketchbook / chalkboard", Color(0xFF2F5D9E), Color(0xFFFBF5E6), Color(0xFF2B2B35), Color(0xFFF2D06B), Color(0xFF1E2A23), Color(0xFFEDEBE3),
        CatStyle(Color(0xFFF4EFE4), Color(0xFF2B2B35), Color(0xFF2B2B35), Color(0xFFD8CFC0)),
    ),
    GINGER(
        "Ginger", Color(0xFFA8520F), Color(0xFFFFF7EE), Color(0xFF2A1F17), Color(0xFFF2A55E), Color(0xFF1C1611), Color(0xFFF1E2D2),
        CatStyle(Color(0xFFE8913A), Color(0xFF2A1F17), Color(0xFF2A1F17), Color(0xFFC46F22)),
    ),
    MIDNIGHT(
        "Midnight black cat", Color(0xFF5B4B8A), Color(0xFFF6F4FA), Color(0xFF1D1A24), Color(0xFFF2C14E), Color(0xFF121116), Color(0xFFE9E6F0),
        CatStyle(Color(0xFF2E2A28), Color(0xFFF2C14E), Color(0xFFE9E1D6), Color(0xFF1B1816)),
    ),
    SIAMESE(
        "Siamese cream", Color(0xFF6F4E37), Color(0xFFFBF6EE), Color(0xFF2B2018), Color(0xFFD9B99B), Color(0xFF1A1714), Color(0xFFF0E6DA),
        CatStyle(Color(0xFFF1E3CC), Color(0xFF3D8FD1), Color(0xFF4A3426), Color(0xFF5A4033)),
    ),
    GREY_TABBY(
        "Grey tabby", Color(0xFF4F6475), Color(0xFFF4F6F8), Color(0xFF1C2228), Color(0xFFA9C2D6), Color(0xFF15181B), Color(0xFFE2E7EC),
        CatStyle(Color(0xFF9AA0A6), Color(0xFF2E7D32), Color(0xFF263238), Color(0xFF666C73)),
    ),
    CALICO(
        "Calico", Color(0xFFB0413E), Color(0xFFFFF8F3), Color(0xFF2A1E1C), Color(0xFFF4A79D), Color(0xFF1B1514), Color(0xFFF3E4DF),
        CatStyle(Color(0xFFFAF4EA), Color(0xFF2A1F17), Color(0xFF2A1F17), Color(0xFFE8913A)),
    ),
    ;

    fun scheme(dark: Boolean, amoled: Boolean): ColorScheme {
        val base = when {
            this == GINGER && !dark -> Light
            this == GINGER -> Dark
            !dark -> derivedLight()
            else -> derivedDark()
        }
        if (!dark || !amoled) return base
        return base.copy(
            background = Color.Black, surface = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF0B0B0B),
            surfaceContainer = Color(0xFF121212),
            surfaceContainerHigh = Color(0xFF1A1A1A),
            surfaceContainerHighest = Color(0xFF222222),
        )
    }

    private fun derivedLight(): ColorScheme {
        val p = lightPrimary
        val bg = lightBg
        val ink = lightInk
        val muted = lerp(ink, bg, 0.35f)
        return lightColorScheme(
            primary = p, onPrimary = Color.White,
            primaryContainer = lerp(bg, p, 0.22f), onPrimaryContainer = lerp(p, Color.Black, 0.6f),
            secondary = muted, onSecondary = Color.White,
            secondaryContainer = lerp(bg, p, 0.10f), onSecondaryContainer = ink,
            tertiary = Color(0xFF4E5D6C), tertiaryContainer = Color(0xFFDCE6EF),
            background = bg, onBackground = ink, surface = bg, onSurface = ink,
            surfaceVariant = lerp(bg, p, 0.10f), onSurfaceVariant = muted,
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = lerp(bg, Color.White, 0.5f),
            surfaceContainer = lerp(bg, p, 0.05f),
            surfaceContainerHigh = lerp(bg, p, 0.08f),
            surfaceContainerHighest = lerp(bg, p, 0.11f),
            outline = lerp(ink, bg, 0.55f), outlineVariant = lerp(bg, p, 0.16f),
        )
    }

    private fun derivedDark(): ColorScheme {
        val p = darkPrimary
        val bg = darkBg
        val ink = darkInk
        val muted = lerp(ink, bg, 0.25f)
        return darkColorScheme(
            primary = p, onPrimary = lerp(p, Color.Black, 0.75f),
            primaryContainer = lerp(bg, p, 0.35f), onPrimaryContainer = lerp(p, Color.White, 0.7f),
            secondary = muted, secondaryContainer = lerp(bg, p, 0.16f), onSecondaryContainer = ink,
            tertiary = Color(0xFFB4C8DA),
            background = bg, onBackground = ink, surface = bg, onSurface = ink,
            surfaceVariant = lerp(bg, p, 0.16f), onSurfaceVariant = muted,
            surfaceContainerLowest = lerp(bg, Color.Black, 0.3f),
            surfaceContainerLow = lerp(bg, p, 0.04f),
            surfaceContainer = lerp(bg, p, 0.06f),
            surfaceContainerHigh = lerp(bg, p, 0.10f),
            surfaceContainerHighest = lerp(bg, p, 0.14f),
            outline = lerp(ink, bg, 0.45f), outlineVariant = lerp(bg, p, 0.20f),
        )
    }

    companion object {
        fun of(name: String) = entries.firstOrNull { it.name == name } ?: GINGER
    }
}

val FontScales = listOf(0.9f, 1f, 1.15f, 1.3f)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val prefs = ExpenseApp.instance.repo.prefs
    val mode by prefs.themeMode.collectAsState()
    val dynamic by prefs.dynamicColor.collectAsState()
    val currency by prefs.currency.collectAsState()
    val paletteName by prefs.palette.collectAsState()
    val amoled by prefs.amoled.collectAsState()
    val fontSize by prefs.fontSize.collectAsState()
    val symbolAfter by prefs.symbolAfter.collectAsState()
    val decimals by prefs.decimals.collectAsState()
    val indian by prefs.indianGrouping.collectAsState()
    val handmade by prefs.handmade.collectAsState()
    val dark = when (mode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val ctx = LocalContext.current
    val palette = CatPalette.of(paletteName)
    val scheme = when {
        dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val d = if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
            if (dark && amoled) d.copy(background = Color.Black, surface = Color.Black, surfaceContainerLowest = Color.Black) else d
        }
        else -> palette.scheme(dark, amoled)
    }
    val moneyStyle = MoneyStyle(symbolAfter = symbolAfter, decimals = decimals, indianGrouping = indian)
    Money.style = moneyStyle
    val density = LocalDensity.current
    val scale = FontScales.getOrElse(fontSize) { 1f }
    CompositionLocalProvider(
        LocalCurrency provides currency,
        LocalMoneyStyle provides moneyStyle,
        LocalCatStyle provides palette.cat,
        LocalHandmade provides handmade,
        LocalDensity provides Density(density.density, density.fontScale * scale),
    ) {
        if (handmade) MaterialTheme(colorScheme = scheme, typography = remember { handTypography() }, content = content)
        else MaterialTheme(colorScheme = scheme, content = content)
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
