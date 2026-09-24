package info.rbuck.billiardscoreboard.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Visual alternative to the numeric stepper for setting balls-on-table: a 1-2-3-4-5 triangle rack
 * of 15 tappable balls, numbered 15 (apex) down to 1 (base). Tapping ball N sets the count to N;
 * every ball numbered <= the current count is highlighted, so the highlighted balls always read
 * as "these are the ones still on the table". Shared between the 14.1 match scoreboard and Training
 * (High Run, Equal Offense) so both use the exact same "balls on table" input.
 */
@Composable
fun BallRackPicker(remaining: Int, onValueChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val rows = listOf(listOf(15), listOf(14, 13), listOf(12, 11, 10), listOf(9, 8, 7, 6), listOf(5, 4, 3, 2, 1))
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { n ->
                    val selected = n <= remaining
                    Surface(
                        onClick = { onValueChange(n) },
                        shape = CircleShape,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(n.toString(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
