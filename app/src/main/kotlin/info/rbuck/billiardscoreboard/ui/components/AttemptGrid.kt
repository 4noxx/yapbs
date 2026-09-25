package info.rbuck.billiardscoreboard.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.rbuck.billiardscoreboard.domain.training.TrainingAttempt
import info.rbuck.billiardscoreboard.domain.training.TrainingExercise
import info.rbuck.billiardscoreboard.domain.training.TrainingMatchEngine
import info.rbuck.billiardscoreboard.ui.theme.LocalUiScale

/**
 * Numbered grid of attempt tiles - pending (grey, number), current (outlined), completed (colored, ball count).
 *
 * Tiles default to square (aspect-ratio-driven) sizing. Pass [tileHeight] to fix their height instead - needed
 * when a wide [perRow] (e.g. one row of 10) would otherwise make width-driven square tiles tall enough to starve
 * a sibling weight(1f) element of vertical space on short/wide landscape screens.
 */
@Composable
fun AttemptGrid(
    attempts: List<TrainingAttempt>,
    totalCount: Int,
    currentIndex: Int,
    exercise: TrainingExercise,
    modifier: Modifier = Modifier,
    perRow: Int = 5,
    tileHeight: Dp? = null,
) {
    val uiScale = LocalUiScale.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        (0 until totalCount step perRow).forEach { rowStart ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                for (i in rowStart until minOf(rowStart + perRow, totalCount)) {
                    val completedAttempt = attempts.getOrNull(i)
                    AttemptTile(
                        number = i + 1,
                        attempt = completedAttempt,
                        isCurrent = completedAttempt == null && i == currentIndex,
                        exercise = exercise,
                        uiScale = uiScale,
                        tileHeight = tileHeight,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AttemptTile(
    number: Int,
    attempt: TrainingAttempt?,
    isCurrent: Boolean,
    exercise: TrainingExercise,
    uiScale: Float,
    tileHeight: Dp?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val (background, content) = when {
        attempt != null && attempt.ballCount > 0 -> colors.primaryContainer to colors.onPrimaryContainer
        attempt != null -> colors.errorContainer to colors.onErrorContainer
        else -> colors.surfaceVariant to colors.onSurfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = background,
        contentColor = content,
        border = if (isCurrent) BorderStroke(2.dp, colors.primary) else null,
        modifier = if (tileHeight != null) modifier.height(tileHeight) else modifier.aspectRatio(1f),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = attempt?.let { TrainingMatchEngine.formatBallCount(exercise, it.ballCount) } ?: number.toString(),
                fontSize = 15.sp * uiScale,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
