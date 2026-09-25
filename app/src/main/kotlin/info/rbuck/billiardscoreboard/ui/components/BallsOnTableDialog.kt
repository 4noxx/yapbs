package info.rbuck.billiardscoreboard.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * "Balls on table?" dialog shared by the 14.1 match scoreboard and Training: a wide, side-by-side
 * layout - the tappable ball-rack triangle on the left, the numeric stepper and action buttons
 * stacked on the right - instead of Material3's default AlertDialog (title/text/buttons stacked
 * top to bottom), which left too little room for the triangle to size its balls comfortably.
 */
@Composable
fun BallsOnTableDialog(
    title: String,
    remaining: Int,
    onValueChange: (Int) -> Unit,
    primaryLabel: String,
    onPrimary: () -> Unit,
    onCancel: () -> Unit,
    onDismissRequest: () -> Unit,
    cancelLabel: String = "Cancel",
    extraButton: (@Composable (Modifier) -> Unit)? = null,
) {
    val controlHeight = 56.dp
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        HideStatusBarInDialog()
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            // No fillMaxWidth: the dialog wraps to its content's width (capped for very wide
            // screens), so the padding around the ball triangle/buttons reads the same on every
            // side instead of a fixed dialog width leaving lopsided empty space left and right.
            modifier = Modifier.widthIn(max = 860.dp),
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(20.dp))
                // height(IntrinsicSize.Max) gives the row a definite height (the ball triangle's,
                // since it's the taller side) so the button column's fillMaxHeight below can then
                // stretch to match it and bottom-align its buttons flush with the triangle's last row.
                Row(modifier = Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    BallRackPicker(remaining = remaining, onValueChange = onValueChange, ballSize = 56.dp, fontSize = 22.sp)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.width(IntrinsicSize.Min).fillMaxHeight(),
                    ) {
                        NumberStepper(label = "", value = remaining, onValueChange = onValueChange, min = 0, max = 15, height = controlHeight)
                        // Flexible gap, not a fixed one: pins the stepper flush to the triangle's top
                        // (the 15-ball) and the Foul/Cancel/Setzen group flush to its bottom, while
                        // that group keeps the same tight 12dp spacing among its own buttons.
                        Spacer(Modifier.weight(1f))
                        extraButton?.invoke(Modifier.fillMaxWidth().height(controlHeight))
                        Button(
                            onClick = onCancel,
                            shape = DialogButtonShape,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                            modifier = Modifier.fillMaxWidth().height(controlHeight),
                        ) { Text(cancelLabel, maxLines = 1) }
                        Button(
                            onClick = onPrimary,
                            shape = DialogButtonShape,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                            modifier = Modifier.fillMaxWidth().height(controlHeight),
                        ) { Text(primaryLabel, maxLines = 1) }
                    }
                }
            }
        }
    }
}
