package info.rbuck.billiardscoreboard.ui.simplematch

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import info.rbuck.billiardscoreboard.data.Club
import info.rbuck.billiardscoreboard.domain.GameType
import info.rbuck.billiardscoreboard.domain.simple.SimpleAction
import info.rbuck.billiardscoreboard.domain.simple.SimpleMatchEngine
import info.rbuck.billiardscoreboard.obs.LiveScorePlayer
import info.rbuck.billiardscoreboard.obs.LiveScoreState
import info.rbuck.billiardscoreboard.ui.bsApplication
import info.rbuck.billiardscoreboard.ui.components.ActivePlayerIndicator
import info.rbuck.billiardscoreboard.ui.components.ConfirmDialog
import info.rbuck.billiardscoreboard.ui.components.HideStatusBarInDialog
import info.rbuck.billiardscoreboard.ui.theme.LocalUiScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleMatchScreen(
    matchId: String,
    onBack: () -> Unit,
    onSaveAndRematch: (GameType) -> Unit,
) {
    val app = bsApplication()
    val viewModel: SimpleMatchViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SimpleMatchViewModel(matchId, app.matchRepository, app.playerRepository) }
        },
    )
    val match by viewModel.match.collectAsStateWithLifecycle()
    val players by viewModel.players.collectAsStateWithLifecycle()
    val isArchived by viewModel.isArchived.collectAsStateWithLifecycle()
    val state = match ?: return

    // An archived match (opened from History) is a saved result - view only, no scoring controls.
    val readOnly = isArchived

    val name1 = players[state.playerIds.getOrNull(0)]?.name ?: "Player 1"
    val name2 = players[state.playerIds.getOrNull(1)]?.name ?: "Player 2"
    val score1 = SimpleMatchEngine.score(state, 0)
    val score2 = SimpleMatchEngine.score(state, 1)
    val runouts1 = SimpleMatchEngine.runoutCount(state, 0)
    val runouts2 = SimpleMatchEngine.runoutCount(state, 1)
    val breakPlayer = SimpleMatchEngine.nextBreakPlayer(state)
    val winner = SimpleMatchEngine.winnerIndex(state)
    val isOver = winner != SimpleMatchEngine.NO_WINNER
    val winnerName = if (winner == 0) name1 else if (winner == 1) name2 else null
    val isDraw = winner == SimpleMatchEngine.DRAW

    val club1Id = players[state.playerIds.getOrNull(0)]?.clubId
    val club2Id = players[state.playerIds.getOrNull(1)]?.clubId
    var club1 by remember { mutableStateOf<Club?>(null) }
    var club2 by remember { mutableStateOf<Club?>(null) }
    LaunchedEffect(club1Id) { club1 = club1Id?.let { app.clubRepository.getById(it) } }
    LaunchedEffect(club2Id) { club2 = club2Id?.let { app.clubRepository.getById(it) } }
    LaunchedEffect(name1, name2, score1, score2, breakPlayer, isOver, club1, club2, state.settings.raceTo, state.settings.gameType) {
        app.liveScoreRepository.publish(
            LiveScoreState(
                discipline = state.settings.gameType.displayName,
                raceTo = state.settings.raceTo,
                player1 = LiveScorePlayer(
                    name = name1, club = club1?.name ?: "", crestPath = club1?.crestPath,
                    score = score1, currentRun = null, highestBreak = null, innings = null,
                    atTurn = !isOver && breakPlayer == 0,
                ),
                player2 = LiveScorePlayer(
                    name = name2, club = club2?.name ?: "", crestPath = club2?.crestPath,
                    score = score2, currentRun = null, highestBreak = null, innings = null,
                    atTurn = !isOver && breakPlayer == 1,
                ),
            ),
        )
    }
    DisposableEffect(Unit) { onDispose { app.liveScoreRepository.clear() } }

    var showHistory by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    BackHandler(enabled = !readOnly && state.actions.isNotEmpty()) { showDiscardConfirm = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.settings.gameType.displayName) },
                navigationIcon = {
                    IconButton(onClick = { if (readOnly || state.actions.isEmpty()) onBack() else showDiscardConfirm = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showHistory = true }) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Match history")
                    }
                    if (!readOnly) {
                        IconButton(onClick = viewModel::undo, enabled = SimpleMatchEngine.canUndo(state)) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                        }
                        IconButton(onClick = { viewModel.saveToArchive(); onBack() }) {
                            Icon(Icons.Filled.Save, contentDescription = "Save")
                        }
                    }
                },
            )
        },
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            // Landscape always gets the compact (side-by-side) layout, even on a tablet where the
            // raw height comfortably exceeds 500dp - otherwise a wide screen falls into the
            // "portrait" stacked layout and leaves the cards short with empty space below.
            val compact = maxHeight < 500.dp || maxWidth > maxHeight
            val uiScale = LocalUiScale.current
            val buttonSize = 56.dp * uiScale

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
            ) {
                Text("Race to ${state.settings.raceTo}", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(if (compact) 8.dp else 16.dp))

                val player1Card = @Composable {
                    PlayerScoreCard(
                        name = name1,
                        score = score1,
                        runouts = runouts1,
                        isBreaking = breakPlayer == 0,
                        enabled = !isOver && !readOnly,
                        canRemove = SimpleMatchEngine.canRemoveLastRackFor(state, 0),
                        onAdd = { viewModel.addWin(0) },
                        onRunout = { viewModel.addWin(0, isRunout = true) },
                        onRemove = { viewModel.removeLastFor(0) },
                        buttonSize = buttonSize,
                        compact = compact,
                        modifier = if (compact) Modifier.weight(1f).fillMaxHeight() else Modifier.fillMaxWidth().weight(1f),
                    )
                }
                val player2Card = @Composable {
                    PlayerScoreCard(
                        name = name2,
                        score = score2,
                        runouts = runouts2,
                        isBreaking = breakPlayer == 1,
                        enabled = !isOver && !readOnly,
                        canRemove = SimpleMatchEngine.canRemoveLastRackFor(state, 1),
                        onAdd = { viewModel.addWin(1) },
                        onRunout = { viewModel.addWin(1, isRunout = true) },
                        onRemove = { viewModel.removeLastFor(1) },
                        buttonSize = buttonSize,
                        compact = compact,
                        modifier = if (compact) Modifier.weight(1f).fillMaxHeight() else Modifier.fillMaxWidth().weight(1f),
                    )
                }

                if (compact) {
                    // Landscape: side by side, cards stretch to fill the available height.
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        player1Card()
                        player2Card()
                    }
                } else {
                    // Portrait: stacked full-width, cards stretch to fill the available height.
                    Column(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        player1Card()
                        player2Card()
                    }
                }

                if (isOver) {
                    Spacer(Modifier.height(if (compact) 8.dp else 24.dp))
                    Text(
                        text = if (winnerName != null) "$winnerName won the match!" else if (isDraw) "The match is a draw!" else "Match finished",
                        style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(if (compact) 8.dp else 16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (readOnly) {
                            Button(onClick = { onSaveAndRematch(state.settings.gameType) }) {
                                Text("Rematch")
                            }
                        } else {
                            Button(onClick = { viewModel.saveToArchive(); onBack() }) {
                                Text("Save & finish")
                            }
                            Button(onClick = { viewModel.saveToArchive(); onSaveAndRematch(state.settings.gameType) }) {
                                Text("Save & Rematch")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showHistory) {
        AlertDialog(
            onDismissRequest = { showHistory = false },
            title = { HideStatusBarInDialog(); Text("Match history") },
            text = {
                if (state.actions.isEmpty()) {
                    Text("No racks recorded yet.")
                } else {
                    val rows = remember(state.actions) { buildSimpleHistoryRows(state.actions, name1, name2) }
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(rows.size) { index ->
                            val row = rows[index]
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            ) {
                                Text("${row.inning}. ${row.name}", modifier = Modifier.weight(1f))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                ) {
                                    Text(
                                        "${row.score1}:${row.score2}",
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    )
                                }
                                if (row.isRunout) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    ) {
                                        Icon(
                                            Icons.Filled.Rocket,
                                            contentDescription = "Runout",
                                            modifier = Modifier.padding(6.dp).size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showHistory = false }) { Text("Close") } },
        )
    }

    if (showDiscardConfirm) {
        ConfirmDialog(
            title = "Discard match?",
            message = "Going back now discards this match's progress - it hasn't been saved. Use the save icon instead to keep it.",
            confirmText = "Discard",
            onConfirm = { showDiscardConfirm = false; onBack() },
            onDismiss = { showDiscardConfirm = false },
        )
    }
}

/** One rack in the match-history list: the winner's name and the running race score right after that rack. */
private data class SimpleHistoryRow(
    val inning: Int,
    val name: String,
    val score1: Int,
    val score2: Int,
    val isRunout: Boolean,
)

private fun buildSimpleHistoryRows(actions: List<SimpleAction>, name1: String, name2: String): List<SimpleHistoryRow> {
    var score1 = 0
    var score2 = 0
    return actions.mapIndexed { index, action ->
        if (action.playerIndex == 0) score1++ else score2++
        SimpleHistoryRow(
            inning = index + 1,
            name = if (action.playerIndex == 0) name1 else name2,
            score1 = score1,
            score2 = score2,
            isRunout = action.isRunout,
        )
    }
}

@Composable
private fun PlayerScoreCard(
    name: String,
    score: Int,
    runouts: Int,
    isBreaking: Boolean,
    enabled: Boolean,
    canRemove: Boolean,
    onAdd: () -> Unit,
    onRunout: () -> Unit,
    onRemove: () -> Unit,
    buttonSize: Dp,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val uiScale = LocalUiScale.current
    // Same name/score/stats sizing formula as StraightPlayerCard (14.1), so every scoreboard's
    // player card reads at the same height - only the stats line's content differs per game type.
    val isTablet = uiScale > 1f
    val compactScoreMultiplier = when {
        isTablet -> 4f
        !compact -> 2.2f
        else -> 1f
    }
    val compactTextMultiplier = if (isTablet) 1.5f else 1f
    // Buttons sit outside the grey card (matching 14.1's layout) rather than nested inside it -
    // same fixed height as every other scoreboard's action row, stretched to fill the width.
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(if (compact) 12.dp else 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    val baseNameStyle = MaterialTheme.typography.titleLarge
                    Text(
                        name,
                        style = baseNameStyle.copy(
                            fontSize = baseNameStyle.fontSize * compactTextMultiplier,
                            lineHeight = baseNameStyle.lineHeight * compactTextMultiplier,
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                    Text(
                        text = score.toString(),
                        fontSize = 44.sp * uiScale * compactScoreMultiplier,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    val baseStatsStyle = MaterialTheme.typography.bodyMedium
                    Text(
                        "Runouts: $runouts",
                        style = baseStatsStyle.copy(
                            fontSize = baseStatsStyle.fontSize * compactTextMultiplier,
                            lineHeight = baseStatsStyle.lineHeight * compactTextMultiplier,
                        ),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            if (isBreaking) {
                ActivePlayerIndicator(compact)
            }
        }
        Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SquareIconButton(
                icon = { Icon(Icons.Filled.Remove, contentDescription = "Remove last rack", modifier = Modifier.size(38.dp * uiScale)) },
                onClick = onRemove,
                enabled = enabled && canRemove,
                modifier = Modifier.weight(1f).height(buttonSize),
            )
            SquareIconButton(
                icon = { Icon(Icons.Filled.Add, contentDescription = "Rack win", modifier = Modifier.size(38.dp * uiScale)) },
                onClick = onAdd,
                enabled = enabled,
                modifier = Modifier.weight(1f).height(buttonSize),
            )
            SquareIconButton(
                icon = { Icon(Icons.Filled.Rocket, contentDescription = "Runout", modifier = Modifier.size(38.dp * uiScale)) },
                onClick = onRunout,
                enabled = enabled,
                modifier = Modifier.weight(1f).height(buttonSize),
            )
        }
    }
}

@Composable
private fun SquareIconButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f).compositeOver(MaterialTheme.colorScheme.surfaceVariant)
        },
        contentColor = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            icon()
        }
    }
}
