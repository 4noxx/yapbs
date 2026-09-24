package info.rbuck.billiardscoreboard.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType

/**
 * Uniform size multiplier for everything that can't go through [androidx.compose.material3.Typography]
 * (fixed dp button/icon/stepper sizes, raw `fontSize = N.sp` values). Derived from
 * `smallestScreenWidthDp` - the orientation-independent measure Android itself uses to tell tablets
 * from phones - so a phone in landscape doesn't get mistaken for a tablet.
 */
val LocalUiScale: ProvidableCompositionLocal<Float> = compositionLocalOf { 1f }

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

/** Multiplies every style in the default Material3 [Typography] by [factor], so every `MaterialTheme.typography.*` usage scales automatically. */
fun scaledTypography(factor: Float): Typography {
    if (factor == 1f) return Typography()
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.scaled(factor),
        displayMedium = base.displayMedium.scaled(factor),
        displaySmall = base.displaySmall.scaled(factor),
        headlineLarge = base.headlineLarge.scaled(factor),
        headlineMedium = base.headlineMedium.scaled(factor),
        headlineSmall = base.headlineSmall.scaled(factor),
        titleLarge = base.titleLarge.scaled(factor),
        titleMedium = base.titleMedium.scaled(factor),
        titleSmall = base.titleSmall.scaled(factor),
        bodyLarge = base.bodyLarge.scaled(factor),
        bodyMedium = base.bodyMedium.scaled(factor),
        bodySmall = base.bodySmall.scaled(factor),
        labelLarge = base.labelLarge.scaled(factor),
        labelMedium = base.labelMedium.scaled(factor),
        labelSmall = base.labelSmall.scaled(factor),
    )
}

private val TextUnit.isSp: Boolean
    get() = type == TextUnitType.Sp
