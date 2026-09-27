package info.rbuck.billiardscoreboard.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/**
 * A score number that rolls each digit like a combination-lock wheel when the value changes - one
 * digit per wheel, rolling up on an increase and down on a decrease, instead of the whole number
 * just popping to the new value. Felt-theme-only touch (see call sites): every other theme keeps a
 * plain [Text].
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

    val text = value.toString().padStart(minDigits, '0')
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        text.forEach { digit ->
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
                    fontFamily = FontFamily.Monospace,
                    color = if (color == Color.Unspecified) LocalContentColor.current else color,
                )
            }
        }
    }
}
