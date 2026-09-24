package info.rbuck.billiardscoreboard.ui.players

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import info.rbuck.billiardscoreboard.data.Club
import info.rbuck.billiardscoreboard.data.Player
import info.rbuck.billiardscoreboard.ui.bsApplication
import info.rbuck.billiardscoreboard.ui.clubs.ClubCrest
import info.rbuck.billiardscoreboard.ui.components.BallsDialogActions
import info.rbuck.billiardscoreboard.ui.components.ConfirmDialog
import info.rbuck.billiardscoreboard.ui.components.HideStatusBarInDialog
import info.rbuck.billiardscoreboard.ui.components.SingleDialogAction

/** Sentinel clubFilter value meaning "players with no club", distinct from null ("all clubs"). */
private const val NO_CLUB_FILTER = ""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersScreen(onBack: () -> Unit, onOpenClubs: () -> Unit, onOpenHeadToHead: () -> Unit = {}) {
    val app = bsApplication()
    val viewModel: PlayersViewModel = viewModel(
        factory = viewModelFactory {
            initializer { PlayersViewModel(app.playerRepository, app.matchRepository, app.clubRepository) }
        },
    )
    val players by viewModel.players.collectAsStateWithLifecycle()
    val clubs by viewModel.clubs.collectAsStateWithLifecycle()

    var showEditor by remember { mutableStateOf(false) }
    var editingPlayer by remember { mutableStateOf<Player?>(null) }
    var pendingDelete by remember { mutableStateOf<Player?>(null) }
    var sortMenuOpen by remember { mutableStateOf(false) }

    // null = "All clubs", NO_CLUB_FILTER = "No club", otherwise a club id.
    var clubFilter by remember { mutableStateOf<String?>(null) }
    var clubMenuExpanded by remember { mutableStateOf(false) }
    val clubById = remember(clubs) { clubs.associateBy { it.id } }
    val clubFilterLabel = when (clubFilter) {
        null -> "All clubs"
        NO_CLUB_FILTER -> "No club"
        else -> clubById[clubFilter]?.name ?: "All clubs"
    }
    val filteredPlayers = remember(players, clubFilter) {
        when (clubFilter) {
            null -> players
            NO_CLUB_FILTER -> players.filter { it.clubId == null }
            else -> players.filter { it.clubId == clubFilter }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Players") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = onOpenHeadToHead) { Icon(Icons.Filled.CompareArrows, contentDescription = "Head-to-head") }
                    IconButton(onClick = onOpenClubs) { Icon(Icons.Filled.Groups, contentDescription = "Clubs") }
                    IconButton(onClick = { sortMenuOpen = true }) { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort") }
                    DropdownMenu(
                        expanded = sortMenuOpen,
                        onDismissRequest = { sortMenuOpen = false },
                        properties = PopupProperties(focusable = false),
                    ) {
                        DropdownMenuItem(text = { Text("Sort by name") }, onClick = {
                            viewModel.setSort(PlayerSort.NAME); sortMenuOpen = false
                        })
                        DropdownMenuItem(text = { Text("Sort by frequency") }, onClick = {
                            viewModel.setSort(PlayerSort.FREQUENCY); sortMenuOpen = false
                        })
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("New player") },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                onClick = { editingPlayer = null; showEditor = true },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (clubs.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = clubMenuExpanded,
                    onExpandedChange = { clubMenuExpanded = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
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
                        properties = PopupProperties(focusable = false),
                        modifier = Modifier.exposedDropdownSize(),
                    ) {
                        DropdownMenuItem(text = { Text("All clubs") }, onClick = { clubFilter = null; clubMenuExpanded = false })
                        DropdownMenuItem(text = { Text("No club") }, onClick = { clubFilter = NO_CLUB_FILTER; clubMenuExpanded = false })
                        clubs.forEach { club ->
                            DropdownMenuItem(text = { Text(club.name) }, onClick = { clubFilter = club.id; clubMenuExpanded = false })
                        }
                    }
                }
            }
            if (filteredPlayers.isEmpty()) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(if (players.isEmpty()) "No players created yet. Tap + to add one." else "No players in this club.")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredPlayers, key = { it.id }) { player ->
                        val club = clubs.firstOrNull { it.id == player.clubId }
                        ListItem(
                            leadingContent = {
                                if (club?.crestPath != null) {
                                    ClubCrest(club.crestPath, size = 36.dp)
                                } else {
                                    PlayerAvatarPlaceholder(size = 36.dp)
                                }
                            },
                            headlineContent = { Text(player.name) },
                            supportingContent = club?.let { { Text(it.name) } },
                            modifier = Modifier.fillMaxWidth().clickable {
                                editingPlayer = player; showEditor = true
                            },
                            trailingContent = {
                                IconButton(onClick = { pendingDelete = player }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        PlayerEditorDialog(
            initialName = editingPlayer?.name.orEmpty(),
            initialClubId = editingPlayer?.clubId,
            clubs = clubs,
            onSave = { name, clubId ->
                viewModel.createOrUpdate(editingPlayer?.id, name, clubId)
                showEditor = false
            },
            onDismiss = { showEditor = false },
        )
    }

    pendingDelete?.let { player ->
        ConfirmDialog(
            title = "Delete player?",
            message = "This will delete ${player.name} permanently.",
            confirmText = "Delete",
            onConfirm = { viewModel.delete(player); pendingDelete = null },
            onDismiss = { pendingDelete = null },
        )
    }
}

/** Single-person icon shown for a player with no club, or whose club has no crest photo. */
@Composable
private fun PlayerAvatarPlaceholder(size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(size * 0.6f),
        )
    }
}

@Composable
private fun PlayerEditorDialog(
    initialName: String,
    initialClubId: String?,
    clubs: List<Club>,
    onSave: (String, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var clubId by remember { mutableStateOf(initialClubId) }
    var showClubPicker by remember { mutableStateOf(false) }
    val selectedClub = clubs.firstOrNull { it.id == clubId }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.9f).widthIn(max = 420.dp),
        title = { HideStatusBarInDialog(); Text(if (initialName.isEmpty()) "New player" else "Edit player") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Player name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { showClubPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(selectedClub?.name ?: "No club")
                }
            }
        },
        confirmButton = {
            BallsDialogActions(
                primaryLabel = "Save",
                onPrimary = { if (name.isNotBlank()) onSave(name, clubId) },
                onCancel = onDismiss,
                primaryEnabled = name.isNotBlank(),
            )
        },
    )

    if (showClubPicker) {
        ClubPickerDialog(
            clubs = clubs,
            onSelect = { clubId = it?.id; showClubPicker = false },
            onDismiss = { showClubPicker = false },
        )
    }
}

@Composable
private fun ClubPickerDialog(
    clubs: List<Club>,
    onSelect: (Club?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { HideStatusBarInDialog(); Text("Club") },
        text = {
            Column {
                ListItem(
                    headlineContent = { Text("No club") },
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(null) },
                )
                HorizontalDivider()
                androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(clubs, key = { it.id }) { club ->
                        ListItem(
                            leadingContent = { ClubCrest(club.crestPath, size = 36.dp) },
                            headlineContent = { Text(club.name) },
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(club) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = { SingleDialogAction(label = "Close", onClick = onDismiss) },
    )
}
