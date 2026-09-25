package info.rbuck.billiardscoreboard.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.rbuck.billiardscoreboard.ui.theme.LocalUiScale

@Composable
fun NumberStepper(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    min: Int = 0,
    max: Int = Int.MAX_VALUE,
    step: Int = 1,
    height: androidx.compose.ui.unit.Dp? = null,
    formatValue: (Int) -> String = { it.toString() },
) {
    val uiScale = LocalUiScale.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (label.isNotEmpty()) {
            Text(label)
            Spacer(Modifier.width(12.dp))
        }
        Row(
            modifier = Modifier
                .height(height ?: (44.dp * uiScale))
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepBox(text = "−", onClick = { onValueChange((value - step).coerceAtLeast(min)) })
            Box(
                modifier = Modifier
                    .width(72.dp * uiScale)
                    .fillMaxHeight()
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = formatValue(value),
                    fontSize = 18.sp * uiScale,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    // Tabular figures - without this, digit "1" in particular can sit visually
                    // off-center within its own measured width in some fonts (its ink isn't
                    // centered in its advance the way "5" etc. are), most noticeable once scaled
                    // up on a tablet.
                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
                )
            }
            StepBox(text = "+", onClick = { onValueChange((value + step).coerceAtMost(max)) })
        }
    }
}

@Composable
private fun StepBox(text: String, onClick: () -> Unit) {
    val uiScale = LocalUiScale.current
    Box(
        modifier = Modifier
            .width(40.dp * uiScale)
            .fillMaxHeight()
            .clickable(onClick = onClick)
            // Horizontal breathing room so "+"/"-" never crowd the box's own edge (and, at the
            // ends of the row, the rounded outer border) - a bare fillMaxHeight Box with no inset
            // let the glyph sit flush against it. Kept generous (not just proportional to the
            // scaled-up box) since a bold "+" glyph's own ink still reads as tight to the border
            // on a tablet's larger uiScale even after the box grows.
            .padding(horizontal = 10.dp * uiScale),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 18.sp * uiScale,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
