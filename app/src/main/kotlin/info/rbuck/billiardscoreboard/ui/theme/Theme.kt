package info.rbuck.billiardscoreboard.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** The 4 selectable app themes - see Settings > Display. */
enum class AppTheme(val label: String) {
    LIGHT("Light"),
    DARK("Dark"),
    VINTAGE("Vintage"),
    FELT("Felt"),
}

private val LightColors = lightColorScheme(
    primary = NavyTile,
    onPrimary = CueWhite,
    primaryContainer = IndigoLightContainer,
    onPrimaryContainer = IndigoDarkText,
    secondary = IndigoPrimary,
    onSecondary = CueWhite,
    tertiary = OrangeBreakerDot,
    background = SlateBackground,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = MatchCardBackground,
    onSurfaceVariant = TextSecondary,
    outline = BorderMedium,
    outlineVariant = BorderLight,
    error = ErrorRed,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    surfaceDim = LightSurfaceDim,
    surfaceBright = CardSurface,
    inverseSurface = LightInverseSurface,
    inverseOnSurface = LightInverseOnSurface,
    inversePrimary = LightInversePrimary,
)

// Calculator-inspired: pure black background, dark-gray "digit tile" surfaces, orange accent.
private val DarkColors = darkColorScheme(
    primary = CalcOrange,
    onPrimary = Color.White,
    primaryContainer = CalcDigitGray,
    onPrimaryContainer = TextPrimaryDark,
    secondary = CalcFunctionGray,
    onSecondary = Color.Black,
    tertiary = CalcOrange,
    background = CalcBlack,
    onBackground = TextPrimaryDark,
    surface = CalcSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = CalcDigitGray,
    onSurfaceVariant = TextSecondaryDark,
    outline = CalcFunctionGray,
    outlineVariant = CalcDigitGray,
    error = ErrorRed,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = CalcSurfaceDark,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    surfaceDim = CalcBlack,
    surfaceBright = DarkSurfaceBright,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    inversePrimary = DarkInversePrimary,
)

// Vintage: warm cream page/surfaces, deep red for the "active" accent, mustard amber as the
// primary action color. Keeps the same light-background/dark-text contrast direction as
// LightColors throughout (surfaceVariant/onSurfaceVariant included) rather than inverting it the
// way DarkColors does - lots of screens use onSurfaceVariant as a generic secondary-text color on
// plain background/surface, not just on surfaceVariant itself, so surfaceVariant must stay in the
// same light family or that text goes illegible. primaryContainer is the one deliberate accent
// still using the dark charcoal, since it's only ever painted as its own filled container.
private val VintageColors = lightColorScheme(
    primary = VintageAmber,
    onPrimary = VintageCharcoal,
    primaryContainer = VintageCharcoal,
    onPrimaryContainer = VintageCream,
    secondary = VintageRed,
    onSecondary = VintageCream,
    tertiary = VintageAmber,
    background = VintageCream,
    onBackground = VintageTextDark,
    surface = VintageCreamSurface,
    onSurface = VintageTextDark,
    surfaceVariant = VintageTile,
    onSurfaceVariant = VintageTextSecondary,
    outline = VintageTextSecondary,
    outlineVariant = VintageCreamSurface,
    error = ErrorRed,
    surfaceContainerLowest = VintageSurfaceContainerLowest,
    surfaceContainerLow = VintageCream,
    surfaceContainer = VintageCreamSurface,
    surfaceContainerHigh = VintageTile,
    surfaceContainerHighest = VintageSurfaceContainerHighest,
    surfaceDim = VintageSurfaceContainerHighest,
    surfaceBright = VintageCream,
    inverseSurface = VintageCharcoal,
    inverseOnSurface = VintageCream,
    inversePrimary = VintageInversePrimary,
)

// Felt: a modern dark palette keyed off the table itself (felt-green primary, warm copper
// secondary) rather than Dark's calculator-neutral black/orange. Same light-on-dark contrast
// direction throughout as DarkColors (surfaceVariant/onSurfaceVariant included), since screens
// use onSurfaceVariant as a generic secondary-text color regardless of what it sits on.
private val FeltColors = darkColorScheme(
    primary = FeltGreen,
    onPrimary = Color.White,
    primaryContainer = FeltGreenContainer,
    onPrimaryContainer = FeltTextPrimary,
    secondary = FeltCopper,
    onSecondary = FeltCopperText,
    tertiary = FeltCopper,
    background = FeltBackground,
    onBackground = FeltTextPrimary,
    surface = FeltSurface,
    onSurface = FeltTextPrimary,
    surfaceVariant = FeltSurfaceHigh,
    onSurfaceVariant = FeltTextSecondary,
    outline = FeltOutline,
    outlineVariant = FeltOutlineVariant,
    error = ErrorRed,
    surfaceContainerLowest = FeltBackground,
    surfaceContainerLow = FeltSurfaceLow,
    surfaceContainer = FeltSurface,
    surfaceContainerHigh = FeltSurfaceHigh,
    surfaceContainerHighest = FeltSurfaceHighest,
    surfaceDim = FeltBackground,
    surfaceBright = FeltSurfaceBright,
    inverseSurface = FeltTextPrimary,
    inverseOnSurface = FeltBackground,
    inversePrimary = FeltGreenLight,
)

@Composable
fun BilliardScoreboardTheme(
    appTheme: AppTheme = if (isSystemInDarkTheme()) AppTheme.DARK else AppTheme.LIGHT,
    // Off by default: the app has its own deep-blue brand color, not the wallpaper-derived Material You palette.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && (appTheme == AppTheme.LIGHT || appTheme == AppTheme.DARK) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (appTheme == AppTheme.DARK) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        appTheme == AppTheme.DARK -> DarkColors
        appTheme == AppTheme.VINTAGE -> VintageColors
        appTheme == AppTheme.FELT -> FeltColors
        else -> LightColors
    }
    val uiScale = rememberUiScale()
    CompositionLocalProvider(LocalUiScale provides uiScale) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = scaledTypography(uiScale),
            content = content,
        )
    }
}
