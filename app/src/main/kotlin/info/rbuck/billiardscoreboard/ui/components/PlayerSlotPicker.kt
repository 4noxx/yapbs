package info.rbuck.billiardscoreboard.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.rbuck.billiardscoreboard.data.Club
import info.rbuck.billiardscoreboard.data.Player
import info.rbuck.billiardscoreboard.ui.bsApplication
import info.rbuck.billiardscoreboard.ui.theme.AmberAttention
import info.rbuck.billiardscoreboard.ui.theme.LocalUiScale
import androidx.compose.runtime.collectAsState

@Composable
fun PlayerSlotPicker(
    label: String,
    selected: Player?,
    players: List<Player>,
    clubs: List<Club>,
    onSelect: (Player) -> Unit,
    onCreate: (String, (Player) -> Unit) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    val clubName = selected?.clubId?.let { id -> clubs.firstOrNull { it.id == id }?.name }
    val uiScale = LocalUiScale.current

    Column {
        Text(
            label.uppercase(),
            fontSize = 10.sp * uiScale,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = if (selected != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 2.dp, bottom = 3.dp),
        )
        Surface(
            onClick = { showDialog = true },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, if (selected == null) AmberAttention else MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth().height(69.dp * uiScale),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp * uiScale)
                            .clip(CircleShape)
                            .background(
                                if (selected != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected != null) {
                            Text(
                                selected.name.take(1).uppercase(),
                                fontSize = 11.sp * uiScale,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        } else {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp * uiScale),
                            )
                        }
                    }
                    Column {
                        Text(
                            selected?.name ?: "Select player...",
                            fontSize = 13.sp * uiScale,
                            fontWeight = FontWeight.Medium,
                            color = if (selected != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (clubName != null) {
                            Text(clubName, fontSize = 10.sp * uiScale, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                }
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = "Select",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp * uiScale),
                )
            }
        }
    }

    if (showDialog) {
        val defaultClubId by bsApplication().settingsRepository.defaultClubId.collectAsState()
        PlayerPickerDialog(
            title = label,
            players = players,
            clubs = clubs,
            onSelect = {
                onSelect(it)
                showDialog = false
            },
            onCreateNew = { name ->
                onCreate(name) { created ->
                    onSelect(created)
                }
                showDialog = false
            },
            onDismiss = { showDialog = false },
            defaultClubId = defaultClubId,
        )
    }
}
