package info.rbuck.billiardscoreboard.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

/**
 * Felt theme's "styled button" treatment (grootstudio.dev's styled-button recipe): a light-center-
 * to-dark-edge radial gradient fill, a lighter accent rim border, and a matching colored glow -
 * used in place of a flat filled Surface/Button wherever Felt wants that look (score action
 * buttons, setup-screen toggle chips and Start buttons, ...). [accent] is the button's own color
 * (which Material role varies by call site - primary for most, secondary/error for a "danger" or
 * secondary-grouped action); white content color is assumed since the gradient is always vivid
 * enough that dark content would lose contrast against its lighter (top-left) region.
 */
@Composable
fun FeltAccentSurface(
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp),
    glowElevation: Dp = 14.dp,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        Surface(
            onClick = onClick,
            enabled = false,
            shape = shape,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        ) {
            Box(contentAlignment = Alignment.Center) { content() }
        }
        return
    }
    val lightColor = lerp(accent, Color.White, 0.35f)
    val darkColor = lerp(accent, Color.Black, 0.35f)
    val borderColor = lerp(accent, Color.White, 0.45f)
    val glowColor = accent.copy(alpha = 0.6f)
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = Color.Transparent,
        contentColor = Color.White,
        border = BorderStroke(1.5.dp, borderColor),
        modifier = modifier.shadow(glowElevation, shape, ambientColor = glowColor, spotColor = glowColor),
    ) {
        // matchParentSize (not fillMaxSize) for the gradient background: fillMaxSize would demand
        // the maximum width/height the incoming constraints allow and Surface would then grow to
        // fit it - fine for callers with an already-fixed size (score buttons: .weight().height()),
        // but it stretched the button to fill its entire parent row wherever a caller left width
        // unconstrained (Start button, ChoiceChip). matchParentSize instead sizes the background to
        // whatever [content] (plus the surrounding Box's own constraints) actually resolves to.
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier.matchParentSize().drawWithCache {
                    // CSS radial-gradient's default (farthest-corner) reaches the box's far corner,
                    // so the highlight fades out gently across the whole shape. Brush.radialGradient's
                    // own default radius is only half the SHORTER side, which completed the fade well
                    // before a wide button's edges - drawing a small, hard-edged circle in the middle
                    // instead of a subtle sheen (see grootstudio.dev's reference button vs. ours).
                    val radius = hypot(size.width, size.height) / 2f
                    val brush = Brush.radialGradient(
                        colors = listOf(lightColor, darkColor),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = radius,
                    )
                    onDrawBehind { drawRect(brush) }
                },
            )
            content()
        }
    }
}
