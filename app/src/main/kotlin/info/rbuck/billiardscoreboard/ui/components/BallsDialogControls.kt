package info.rbuck.billiardscoreboard.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Corner radius shared by every button in a "balls on table"/"re-rack" style dialog's action row,
 * so Cancel, Set/Re-rack (and an optional extra toggle) all read as one consistent group. */
val DialogButtonShape = RoundedCornerShape(14.dp)

/** Action row for "balls on table"/"re-rack" dialogs: an optional extra toggle (e.g. 14.1's Foul
 * button), Cancel, and the primary action (Set/Re-rack), rendered as one centered group of
 * equal-width filled pill buttons - instead of the default AlertDialog layout, which splits
 * dismiss/confirm to opposite corners. */
@Composable
fun BallsDialogActions(
    primaryLabel: String,
    onPrimary: () -> Unit,
    onCancel: () -> Unit,
    cancelLabel: String = "Cancel",
    primaryEnabled: Boolean = true,
    extraButton: (@Composable RowScope.() -> Unit)? = null,
) {
    val buttonPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        extraButton?.invoke(this)
        Button(
            onClick = onCancel,
            shape = DialogButtonShape,
            contentPadding = buttonPadding,
            modifier = Modifier.weight(1f),
        ) { Text(cancelLabel, maxLines = 1) }
        Button(
            onClick = onPrimary,
            enabled = primaryEnabled,
            shape = DialogButtonShape,
            contentPadding = buttonPadding,
            modifier = Modifier.weight(1f),
        ) { Text(primaryLabel, maxLines = 1) }
    }
}

/** Single full-width filled pill button for a dialog whose action row is just one
 * acknowledgement (e.g. "Close") - same [DialogButtonShape] as [BallsDialogActions] so every
 * dialog's buttons read as one consistent, filled style instead of mixing in flat [TextButton]s. */
@Composable
fun SingleDialogAction(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = DialogButtonShape,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) { Text(label, maxLines = 1) }
}
