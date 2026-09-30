package info.rbuck.billiardscoreboard.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType

/**
 * Uniform size multiplier for everything that can't go through [androidx.compose.material3.Typography]
 * (fixed dp button/icon/stepper sizes, raw `fontSize = N.sp` values). Derived from
 * `smallestScreenWidthDp` - the orientation-independent measure Android itself uses to tell tablets
 * from phones - so a phone in landscape doesn't get mistaken for a tablet.
 */
val LocalUiScale: ProvidableCompositionLocal<Float> = compositionLocalOf { 1f }

/** The active theme's display font (Felt: Audiowide, Vintage: Chicle - see Fonts.kt), null for
 * Light/Dark. For spots that draw a raw [androidx.compose.material3.Text] with an explicit
 * `fontFamily` instead of a `MaterialTheme.typography.*` style (which already re-fonts itself via
 * [scaledTypography]) - e.g. the score digits' monospace requirement for even digit widths during
 * the lock-wheel roll animation. */
val LocalThemeFont: ProvidableCompositionLocal<FontFamily?> = compositionLocalOf { null }

@Composable
fun rememberUiScale(): Float {
    val smallestWidthDp = LocalConfiguration.current.smallestScreenWidthDp
    return when {
        smallestWidthDp >= 720 -> 1.35f
        smallestWidthDp >= 600 -> 1.2f
        else -> 1f
    }
}

/** Scales this [TextStyle]'s font size (and line height, if it's also an sp value) by [factor]. */
fun TextStyle.scaled(factor: Float): TextStyle {
    if (factor == 1f) return this
    val scaledFontSize = if (fontSize.isSp) fontSize * factor else fontSize
    val scaledLineHeight = if (lineHeight.type == TextUnitType.Sp) lineHeight * factor else lineHeight
    return copy(fontSize = scaledFontSize, lineHeight = scaledLineHeight)
}

/** Multiplies every style in the default Material3 [Typography] by [factor] and, if [fontFamily] is
 * given (Felt/Vintage's themed display fonts - see Fonts.kt), applies it to every style too, so
 * every `MaterialTheme.typography.*` usage scales/re-fonts automatically. */
fun scaledTypography(factor: Float, fontFamily: FontFamily? = null): Typography {
    if (factor == 1f && fontFamily == null) return Typography()
    val base = Typography()
    fun TextStyle.styled() = scaled(factor).let { if (fontFamily != null) it.copy(fontFamily = fontFamily) else it }
    return Typography(
        displayLarge = base.displayLarge.styled(),
        displayMedium = base.displayMedium.styled(),
        displaySmall = base.displaySmall.styled(),
        headlineLarge = base.headlineLarge.styled(),
        headlineMedium = base.headlineMedium.styled(),
        headlineSmall = base.headlineSmall.styled(),
        titleLarge = base.titleLarge.styled(),
        titleMedium = base.titleMedium.styled(),
        titleSmall = base.titleSmall.styled(),
        bodyLarge = base.bodyLarge.styled(),
        bodyMedium = base.bodyMedium.styled(),
        bodySmall = base.bodySmall.styled(),
        labelLarge = base.labelLarge.styled(),
        labelMedium = base.labelMedium.styled(),
        labelSmall = base.labelSmall.styled(),
    )
}

private val TextUnit.isSp: Boolean
    get() = type == TextUnitType.Sp
