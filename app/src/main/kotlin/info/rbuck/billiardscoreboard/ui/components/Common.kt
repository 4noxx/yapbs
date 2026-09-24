package info.rbuck.billiardscoreboard.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import info.rbuck.billiardscoreboard.data.Club
import info.rbuck.billiardscoreboard.data.Player
import info.rbuck.billiardscoreboard.domain.GameType
import info.rbuck.billiardscoreboard.ui.theme.LocalUiScale

/** clubId used as the "no club" filter option - distinct from null, which means "all clubs". */
private const val NO_CLUB_FILTER = ""

/** Shared "this player is up" marker, used on player cards across all game types. */
val ActivePlayerDotColor = Color(0xFFFF6D00)

@Composable
fun BoxScope.ActivePlayerIndicator(compact: Boolean = false) {
    val uiScale = LocalUiScale.current
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding((if (compact) 6 else 10).dp * uiScale)
            .size((if (compact) 8 else 12).dp * uiScale)
            .clip(CircleShape)
            .background(ActivePlayerDotColor),
    )
}

/** Clickable "Start" action for a setup screen's top bar - the single way to start once
 * everything required is filled in, replacing a separate large button pinned to the bottom of
 * the screen. Filled and enabled once [enabled] (all required fields chosen), muted and disabled
 * otherwise - same READY/OPEN-style at-a-glance state the old status pill gave, but tappable. */
@Composable
fun TopBarStartButton(label: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = modifier.widthIn(min = 140.dp * LocalUiScale.current),
    ) {
        Text(
            label,
            fontSize = 12.sp * LocalUiScale.current,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
        )
    }
}

private fun GameType.badgeText() = if (this == GameType.STRAIGHT_POOL) "14.1" else displayName.substringBefore("-")
private fun GameType.subtitleText() = if (this == GameType.STRAIGHT_POOL) displayName.uppercase() else "${displayName.uppercase()} POOL"

/** Setup-screen header title: circular discipline badge + "New Match" + small uppercase subtitle. Tapping the subtitle opens a menu to switch discipline. */
@Composable
fun MatchSetupHeaderTitle(gameType: GameType, onGameTypeSelected: (GameType) -> Unit) {
    val uiScale = LocalUiScale.current
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .size(32.dp * uiScale)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(gameType.badgeText(), color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp * uiScale, fontWeight = FontWeight.Bold)
        }
        Column {
            Text("New Match", fontSize = 16.sp * uiScale, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, lineHeight = 18.sp * uiScale)
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { expanded = true },
                ) {
                    Text(
                        gameType.subtitleText(),
                        fontSize = 11.sp * uiScale,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = 0.5.sp,
                    )
                    Icon(
                        Icons.Filled.ArrowDropDown,
                        contentDescription = "Change discipline",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp * uiScale),
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    properties = PopupProperties(focusable = false),
                ) {
                    GameType.entries.forEach { entry ->
                        DropdownMenuItem(
                            text = { Text(entry.displayName) },
                            onClick = {
                                expanded = false
                                if (entry != gameType) onGameTypeSelected(entry)
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Small bold uppercase field label used throughout setup-screen cards ("RACE TO (TARGET)", "BREAK MODE", ...). */
@Composable
fun SettingsLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier,
        fontSize = 10.sp * LocalUiScale.current,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Bordered rounded card used to group setup-screen fields (race-to, break mode, first break, handicap) into one visual block. */
@Composable
fun SettingsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            content = content,
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String = "Cancel",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { HideStatusBarInDialog(); Text(title) },
        text = { Text(message) },
        confirmButton = {
            BallsDialogActions(
                primaryLabel = confirmText,
                onPrimary = onConfirm,
                onCancel = onDismiss,
                cancelLabel = dismissText,
            )
        },
    )
}

/** Above this content width there's room to put the club filter and player list side by side instead of stacked. */
private val WidePickerBreakpoint = 480.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerPickerDialog(
    title: String,
    players: List<Player>,
    clubs: List<Club>,
    onSelect: (Player) -> Unit,
    onCreateNew: (String) -> Unit,
    onDismiss: () -> Unit,
    defaultClubId: String? = null,
) {
    var query by remember { mutableStateOf("") }
    // null = "All clubs", NO_CLUB_FILTER = "No club", otherwise a club id. Pre-set to the app's default
    // club - but only if that club still exists: a stale saved default (its club since deleted, e.g.
    // after a database reset) must not silently filter every player out with no way to see or clear it.
    var clubFilter by remember { mutableStateOf(defaultClubId?.takeIf { id -> clubs.any { it.id == id } }) }
    var clubMenuExpanded by remember { mutableStateOf(false) }

    val clubById = remember(clubs) { clubs.associateBy { it.id } }
    val clubFilterLabel = when (clubFilter) {
        null -> "All clubs"
        NO_CLUB_FILTER -> "No club"
        else -> clubById[clubFilter]?.name ?: "All clubs"
    }

    val clubFiltered = remember(players, clubFilter) {
        when (clubFilter) {
            null -> players
            NO_CLUB_FILTER -> players.filter { it.clubId == null }
            else -> players.filter { it.clubId == clubFilter }
        }
    }
    val filtered = remember(clubFiltered, query) {
        if (query.isBlank()) clubFiltered else clubFiltered.filter { it.name.contains(query, ignoreCase = true) }
    }
    val canCreate = query.isNotBlank() && filtered.none { it.name.equals(query.trim(), ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.92f).widthIn(max = 720.dp),
        title = { HideStatusBarInDialog(); Text(title) },
        text = {
            BoxWithConstraints {
                val wide = maxWidth > WidePickerBreakpoint && clubs.isNotEmpty()
                val clubDropdown = @Composable {
                    ExposedDropdownMenuBox(expanded = clubMenuExpanded, onExpandedChange = { clubMenuExpanded = it }) {
                        OutlinedTextField(
                            value = clubFilterLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Club") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clubMenuExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                        )
                        DropdownMenu(
                            expanded = clubMenuExpanded,
                            onDismissRequest = { clubMenuExpanded = false },
                            modifier = Modifier.exposedDropdownSize(),
                            properties = PopupProperties(focusable = false),
                        ) {
                            DropdownMenuItem(text = { Text("All clubs") }, onClick = { clubFilter = null; clubMenuExpanded = false })
                            DropdownMenuItem(text = { Text("No club") }, onClick = { clubFilter = NO_CLUB_FILTER; clubMenuExpanded = false })
                            clubs.forEach { club ->
                                DropdownMenuItem(text = { Text(club.name) }, onClick = { clubFilter = club.id; clubMenuExpanded = false })
                            }
                        }
                    }
                }
                val searchAndList = @Composable {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Search or create") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (canCreate) {
                        ListItem(
                            headlineContent = { Text("Create \"${query.trim()}\"") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCreateNew(query.trim()) },
                        )
                        HorizontalDivider()
                    }
                    LazyColumn(modifier = Modifier.heightIn(max = 420.dp).padding(top = 8.dp)) {
                        items(filtered, key = { it.id }) { player ->
                            PlayerRow(player = player, clubName = clubById[player.clubId]?.name, onClick = { onSelect(player) })
                            HorizontalDivider()
                        }
                    }
                }

                if (wide) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.width(200.dp)) { clubDropdown() }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) { searchAndList() }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (clubs.isNotEmpty()) {
                            clubDropdown()
                            Spacer(Modifier.height(8.dp))
                        }
                        searchAndList()
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

/** Single-line player row - name and club side by side, so both stay readable without cramping row height. */
@Composable
private fun PlayerRow(player: Player, clubName: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(player.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (clubName != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                clubName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
