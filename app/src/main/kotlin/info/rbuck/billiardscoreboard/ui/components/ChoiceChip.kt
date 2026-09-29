package info.rbuck.billiardscoreboard.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import info.rbuck.billiardscoreboard.ui.bsApplication
import info.rbuck.billiardscoreboard.ui.theme.AppTheme
import info.rbuck.billiardscoreboard.ui.theme.LocalUiScale

/** A rounded-square selectable choice, styled to match the app's tile/button language (used instead of the default FilterChip look).
 * Felt-selected reads through [FeltAccentSurface] instead - it's self-detecting (not threaded from every one of this
 * shared component's many call sites) so every screen's chips (theme/language picker, break mode, first break, tournament
 * mode, ...) pick up the same gradient+glow treatment automatically. */
@Composable
fun ChoiceChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val uiScale = LocalUiScale.current
    val appTheme by bsApplication().settingsRepository.appTheme.collectAsStateWithLifecycle()
    if (selected && (appTheme == AppTheme.FELT || appTheme == AppTheme.LIGHT)) {
        FeltAccentSurface(
            accent = MaterialTheme.colorScheme.primary,
            onClick = onClick,
            shape = RoundedCornerShape(8.dp),
            glowElevation = 8.dp,
            modifier = modifier.height(36.dp * uiScale),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
                Text(
                    label.uppercase(),
                    fontSize = 11.sp * uiScale,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        return
    }
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(150),
        label = "chipBg",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(150),
        label = "chipText",
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        modifier = modifier.height(36.dp * uiScale),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
            Text(
                label.uppercase(),
                color = textColor,
                fontSize = 11.sp * uiScale,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
