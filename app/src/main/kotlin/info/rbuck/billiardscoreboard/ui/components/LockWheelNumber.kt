package info.rbuck.billiardscoreboard.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import info.rbuck.billiardscoreboard.ui.theme.LocalSplitFlapStyle
import info.rbuck.billiardscoreboard.ui.theme.LocalThemeFont

/**
 * A score number that rolls each digit like a combination-lock wheel when the value changes - one
 * digit per wheel, rolling up on an increase and down on a decrease, instead of the whole number
 * just popping to the new value. Used across all themes (see call sites); digits use the active
 * theme's display font ([LocalThemeFont] - Felt/Vintage/Flap) where set, else plain monospace for
 * even digit widths during the roll. Under Flap ([LocalSplitFlapStyle]), each digit instead does a
 * real split-flap card flip (see [SplitFlapDigit]) - the one place in the app meant to look like a
 * physical airport board, per request ("nur die Zahl im Scoreboard als Faltblatt").
 */
@Composable
fun LockWheelNumber(
    value: Int,
    modifier: Modifier = Modifier,
    fontSize: TextUnit,
    color: Color = Color.Unspecified,
    minDigits: Int = 1,
) {
    var previousValue by remember { mutableIntStateOf(value) }
    val increasing = value >= previousValue
    SideEffect { previousValue = value }

    val rawText = value.toString().padStart(minDigits, '0')
    val splitFlap = LocalSplitFlapStyle.current
    // Flap reads as a real airport board: unused leading positions stay blank (space) rather than
    // showing "0" - so e.g. 14.1's 3-digit counter starts as blank-blank-0, and a score of 5 shows
    // as blank-blank-5. The last position always shows a digit, even at 0. Other themes are
    // unaffected (padStart already only kicks in when minDigits > 1, which only Flap's call site
    // currently uses).
    val text = if (splitFlap) {
        val firstNonZero = rawText.indexOfFirst { it != '0' }
        val blanks = if (firstNonZero == -1) rawText.length - 1 else firstNonZero
        rawText.mapIndexed { i, c -> if (i < blanks) ' ' else c }.joinToString("")
    } else {
        rawText
    }
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(if (splitFlap) 2.dp else 1.dp)) {
        text.forEach { digit ->
            if (splitFlap) {
                SplitFlapPanel(fontSize = fontSize) {
                    SplitFlapDigit(digit = digit, fontSize = fontSize, color = color)
                }
            } else {
                RollingDigit(digit = digit, fontSize = fontSize, color = color, increasing = increasing)
            }
        }
    }
}

/**
 * Like [LockWheelNumber] but for an already-formatted string rather than a plain integer - e.g.
 * Training's Level 4 breakball-bonus count ("14+5"). Every character becomes its own card under
 * Flap, "+" included, instead of falling back to plain text once the format stops being a pure
 * number.
 */
@Composable
fun LockWheelText(text: String, modifier: Modifier = Modifier, fontSize: TextUnit, color: Color = Color.Unspecified) {
    val splitFlap = LocalSplitFlapStyle.current
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(if (splitFlap) 2.dp else 1.dp)) {
        text.forEach { char ->
            if (splitFlap) {
                SplitFlapPanel(fontSize = fontSize) {
                    SplitFlapDigit(digit = char, fontSize = fontSize, color = color)
                }
            } else {
                Text(
                    char.toString(),
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = LocalThemeFont.current ?: FontFamily.Monospace,
                    color = if (color == Color.Unspecified) LocalContentColor.current else color,
                )
            }
        }
    }
}

/** The non-Flap digit: a whole-glyph vertical roll, like a combination-lock wheel. */
@Composable
private fun RollingDigit(digit: Char, fontSize: TextUnit, color: Color, increasing: Boolean) {
    AnimatedContent(
        targetState = digit,
        transitionSpec = {
            val distance = { size: Int -> size }
            if (increasing) {
                (slideInVertically(tween(220)) { h -> distance(h) } + fadeIn(tween(220))) togetherWith
                    (slideOutVertically(tween(220)) { h -> -distance(h) } + fadeOut(tween(220)))
            } else {
                (slideInVertically(tween(220)) { h -> -distance(h) } + fadeIn(tween(220))) togetherWith
                    (slideOutVertically(tween(220)) { h -> distance(h) } + fadeOut(tween(220)))
            }
        },
        label = "lockWheelDigit",
    ) { targetDigit ->
        Text(
            targetDigit.toString(),
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            fontFamily = LocalThemeFont.current ?: FontFamily.Monospace,
            color = if (color == Color.Unspecified) LocalContentColor.current else color,
        )
    }
}

/** The dark card + center crease chrome behind one Flap-theme digit - a static frame that stays
 * put while the digit inside it flips. */
@Composable
private fun SplitFlapPanel(fontSize: TextUnit, content: @Composable () -> Unit) {
    val panelColor = MaterialTheme.colorScheme.surfaceVariant
    val borderColor = MaterialTheme.colorScheme.outline
    val horizontalPadding = (fontSize.value * 0.09f).dp
    val verticalPadding = (fontSize.value * 0.06f).dp
    Box(
        modifier = Modifier
            .background(
                Brush.verticalGradient(listOf(panelColor, Color.Black.copy(alpha = 0.55f))),
                RoundedCornerShape((fontSize.value * 0.06f).dp),
            )
            .border(1.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape((fontSize.value * 0.06f).dp))
            .drawWithContent {
                drawContent()
                // The crease - a dark line with a faint highlight just below it - is what reads as
                // the physical split between a flap display's top and bottom leaf. Lines up with
                // SplitFlapDigit's own top/bottom half split since this panel's padding is symmetric.
                val midY = size.height / 2f
                drawLine(
                    color = Color.Black.copy(alpha = 0.65f),
                    start = Offset(0f, midY),
                    end = Offset(size.width, midY),
                    strokeWidth = 1.5.dp.toPx(),
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = Offset(0f, midY + 1.5.dp.toPx()),
                    end = Offset(size.width, midY + 1.5.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Clips to the top half of the layer's own bounds - used on a full-height [SplitFlapPiece] so the
 * piece's natural center (the clip rectangle's pivot for rotation) lands exactly on the crease. */
private val TopHalfShape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rectangle(Rect(Offset.Zero, size.copy(height = size.height / 2)))
}

/** Clips to the bottom half of the layer's own bounds - see [TopHalfShape]. */
private val BottomHalfShape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rectangle(Rect(Offset.Zero, size.copy(height = size.height / 2)).translate(Offset(0f, size.height / 2)))
}

/**
 * One full-height split-flap "piece": the glyph centered exactly like every other piece (so all
 * instances - the two static halves and the animated flap - line up pixel for pixel), left
 * unclipped here since clipping is applied by the caller via [TopHalfShape]/[BottomHalfShape] on
 * this composable's own [Modifier]. Technique (clip a full-size, pre-centered piece instead of
 * measuring a half-size box) ported from
 * https://medium.com/bestsecret-tech/creating-a-split-flap-display-in-compose - Compose's own
 * height-coercion rules made an earlier half-size-box-and-align version of this show the same
 * half twice.
 */
@Composable
private fun SplitFlapPiece(char: Char, fontSize: TextUnit, fontFamily: FontFamily, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth())
        Text(
            char.toString(),
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            fontFamily = fontFamily,
            color = color,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth())
    }
}

/**
 * A single split-flap character: static top/bottom halves (the "settled" look) with a hinged flap
 * that swings down over the top half to reveal the new value - two quarter-turns run back to back
 * (old value 0°->90°, then new value 90°->180°, i.e. -90°) instead of one continuous 180° turn with
 * a single piece, since a piece rotated past 90° would otherwise render its own glyph mirrored (no
 * back-face culling in Compose) - the second quarter-turn swaps to a pre-mirrored copy of the new
 * glyph (rotationY/rotationZ = 180°) so it un-mirrors back to normal exactly as it comes edge-on.
 * The static bottom half (and the overlay's glyph) swap to the new digit at that exact midpoint.
 */
@Composable
private fun SplitFlapDigit(digit: Char, fontSize: TextUnit, color: Color) {
    val fontFamily = LocalThemeFont.current ?: FontFamily.Monospace
    val textColor = if (color == Color.Unspecified) LocalContentColor.current else color
    val pieceHeight = (fontSize.value * 1.15f).dp
    val pieceWidth = (fontSize.value * 0.56f).dp
    val density = LocalDensity.current

    var pastDigit by remember { mutableStateOf(digit) }
    var finalDigit by remember { mutableStateOf(digit) }
    var rotation by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(digit) {
        if (digit != finalDigit) {
            finalDigit = digit
            // A real flap doesn't fall at a constant rate - gravity accelerates it off the top, then
            // the mechanism decelerates it into the stop at the bottom. CubicBezierEasing(0.3, 0,
            // 0.8, 0.8) gives that same quick-start/soft-settle shape instead of a flat linear turn.
            animate(initialValue = 0f, targetValue = -180f, animationSpec = tween(260, easing = FlapEasing)) { value, _ ->
                rotation = value
                if (value <= -90f) pastDigit = digit
            }
            rotation = 0f
            pastDigit = digit
        }
    }

    // How close the flap is to edge-on (|rotation| == 90) right now, 0 at either resting face and 1
    // at the exact midpoint - drives both the shadow it casts on the piece below and how dark its
    // own (foreshortened, nearly-invisible) face reads while turning through the light.
    val edgeProximity = (1f - kotlin.math.abs(rotation / -180f - 0.5f) * 2f).coerceIn(0f, 1f)

    Box(modifier = Modifier.width(pieceWidth).height(pieceHeight)) {
        SplitFlapPiece(
            char = finalDigit,
            fontSize = fontSize,
            fontFamily = fontFamily,
            color = textColor,
            modifier = Modifier.graphicsLayer { clip = true; shape = TopHalfShape },
        )
        SplitFlapPiece(
            char = pastDigit,
            fontSize = fontSize,
            fontFamily = fontFamily,
            color = textColor,
            modifier = Modifier.graphicsLayer { clip = true; shape = BottomHalfShape },
        )
        // The falling flap's own shadow sweeping across the bottom piece just ahead of it landing -
        // strongest as the flap passes edge-on (about to land) and only while it's still above the
        // crease (first half of the turn), matching how the light is cut off from above.
        if (rotation in -90f..0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(pieceHeight / 2)
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = edgeProximity * 0.5f), Color.Transparent))),
            )
        }
        if (rotation != 0f) {
            if (rotation >= -90f) {
                Box(
                    modifier = Modifier.graphicsLayer {
                        rotationX = rotation
                        cameraDistance = 16f * density.density
                        shadowElevation = edgeProximity * 5f * density.density
                        shape = TopHalfShape
                        clip = false
                    },
                ) {
                    SplitFlapPiece(
                        char = pastDigit,
                        fontSize = fontSize,
                        fontFamily = fontFamily,
                        color = textColor,
                        modifier = Modifier.graphicsLayer { clip = true; shape = TopHalfShape },
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer { clip = true; shape = TopHalfShape }
                            .background(Color.Black.copy(alpha = edgeProximity * 0.55f)),
                    )
                }
            } else {
                Box(
                    modifier = Modifier.graphicsLayer {
                        rotationX = rotation
                        rotationY = 180f
                        rotationZ = 180f
                        cameraDistance = 16f * density.density
                        shadowElevation = edgeProximity * 5f * density.density
                        shape = BottomHalfShape
                        clip = false
                    },
                ) {
                    SplitFlapPiece(
                        char = finalDigit,
                        fontSize = fontSize,
                        fontFamily = fontFamily,
                        color = textColor,
                        modifier = Modifier.graphicsLayer { clip = true; shape = BottomHalfShape },
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer { clip = true; shape = BottomHalfShape }
                            .background(Color.Black.copy(alpha = edgeProximity * 0.55f)),
                    )
                }
            }
        }
    }
}

/** A flap doesn't fall at a constant rate - see the [LaunchedEffect] in [SplitFlapDigit]. */
private val FlapEasing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.8f)
