package info.rbuck.billiardscoreboard.ui.straightmatch

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import info.rbuck.billiardscoreboard.data.Club
import info.rbuck.billiardscoreboard.domain.straight.InningEndType
import info.rbuck.billiardscoreboard.domain.straight.PlayerStats
import info.rbuck.billiardscoreboard.domain.straight.StraightMatchEngine
import info.rbuck.billiardscoreboard.domain.straight.StraightMatchState
import androidx.compose.material.icons.automirrored.filled.List
import info.rbuck.billiardscoreboard.i18n.StraightMatchTextKey
import info.rbuck.billiardscoreboard.i18n.Translations
import info.rbuck.billiardscoreboard.obs.LiveScorePlayer
import info.rbuck.billiardscoreboard.obs.LiveScoreState
import info.rbuck.billiardscoreboard.ui.components.ActivePlayerIndicator
import info.rbuck.billiardscoreboard.ui.components.BallRackPicker
import info.rbuck.billiardscoreboard.ui.components.BallsDialogActions
import info.rbuck.billiardscoreboard.ui.components.ConfirmDialog
import info.rbuck.billiardscoreboard.ui.components.DialogButtonShape
import info.rbuck.billiardscoreboard.ui.components.NumberStepper
import info.rbuck.billiardscoreboard.ui.components.HideStatusBarInDialog
import info.rbuck.billiardscoreboard.ui.bsApplication
import info.rbuck.billiardscoreboard.ui.theme.LocalUiScale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StraightMatchScreen(
    matchId: String,
    onBack: () -> Unit,
    onSaveAndRematch: () -> Unit,
) {
    val app = bsApplication()
    val viewModel: StraightMatchViewModel = viewModel(
        factory = viewModelFactory {
            initializer { StraightMatchViewModel(matchId, app.matchRepository, app.playerRepository) }
        },
    )
    val language by app.settingsRepository.language.collectAsStateWithLifecycle()
    fun t(key: StraightMatchTextKey, fallback: String) = Translations.straightMatchText(key, language) ?: fallback
    val match by viewModel.match.collectAsStateWithLifecycle()
    val players by viewModel.players.collectAsStateWithLifecycle()
    val isArchived by viewModel.isArchived.collectAsStateWithLifecycle()
    val state = match ?: return

    // An archived match (opened from History) is a saved result - view only, no scoring controls.
    val readOnly = isArchived

    val name1 = players[state.playerIds.getOrNull(0)]?.name ?: "Player 1"
    val name2 = players[state.playerIds.getOrNull(1)]?.name ?: "Player 2"
    val score1 = StraightMatchEngine.score(state, 0)
    val score2 = StraightMatchEngine.score(state, 1)
    val stats1 = StraightMatchEngine.stats(state, 0)
    val stats2 = StraightMatchEngine.stats(state, 1)
    val currentRun1 = StraightMatchEngine.currentRun(state, 0)
    val currentRun2 = StraightMatchEngine.currentRun(state, 1)
    val currentPlayer = StraightMatchEngine.currentPlayerIndex(state)
    val ballsOnTable = StraightMatchEngine.ballsOnTable(state)
    val winner = StraightMatchEngine.winnerIndex(state)
    val isOver = winner != StraightMatchEngine.NO_WINNER

    val club1Id = players[state.playerIds.getOrNull(0)]?.clubId
    val club2Id = players[state.playerIds.getOrNull(1)]?.clubId
    var club1 by remember { mutableStateOf<Club?>(null) }
    var club2 by remember { mutableStateOf<Club?>(null) }
    LaunchedEffect(club1Id) { club1 = club1Id?.let { app.clubRepository.getById(it) } }
    LaunchedEffect(club2Id) { club2 = club2Id?.let { app.clubRepository.getById(it) } }
    LaunchedEffect(name1, name2, score1, score2, currentRun1, currentRun2, stats1, stats2, currentPlayer, isOver, club1, club2, state.settings.raceTo) {
        app.liveScoreRepository.publish(
            LiveScoreState(
                discipline = "14.1 Straight Pool",
                raceTo = state.settings.raceTo,
                player1 = LiveScorePlayer(
                    name = name1, club = club1?.name ?: "", crestPath = club1?.crestPath,
                    score = score1, currentRun = currentRun1, highestBreak = stats1.highestBreak,
                    innings = stats1.innings, atTurn = !isOver && currentPlayer == 0,
                ),
                player2 = LiveScorePlayer(
                    name = name2, club = club2?.name ?: "", crestPath = club2?.crestPath,
                    score = score2, currentRun = currentRun2, highestBreak = stats2.highestBreak,
                    innings = stats2.innings, atTurn = !isOver && currentPlayer == 1,
                ),
            ),
        )
    }
    DisposableEffect(Unit) { onDispose { app.liveScoreRepository.clear() } }

    var showBreakFoulDialog by remember { mutableStateOf(false) }
    var showRebreakDialog by remember { mutableStateOf(false) }
    var showThirdFoulDialog by remember { mutableStateOf(false) }
    var showRackDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showRebuildRulesDialog by remember { mutableStateOf(false) }
    var showSetBallsDialog by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    BackHandler(enabled = !readOnly && state.actions.isNotEmpty()) { showDiscardConfirm = true }

    fun onFoulClicked() {
        when {
            StraightMatchEngine.isBreakFoulPossible(state) -> showBreakFoulDialog = true
            StraightMatchEngine.wouldBeThirdConsecutiveFoul(state) -> showThirdFoulDialog = true
            else -> viewModel.foul(breakFoul = false, reRack = false)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("14.1 Straight Pool") },
                navigationIcon = {
                    IconButton(onClick = { if (readOnly || state.actions.isEmpty()) onBack() else showDiscardConfirm = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showRebuildRulesDialog = true }) {
                        Icon(Icons.Filled.Info, contentDescription = "Rebuild rules")
                    }
                    IconButton(onClick = { showHistoryDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "History")
                    }
                    if (!readOnly) {
                        IconButton(onClick = viewModel::undo, enabled = StraightMatchEngine.canUndo(state)) {
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
                .padding(12.dp),
        ) {
            // Landscape always gets the compact (icon-row) layout, even on a tablet where the raw
            // height comfortably exceeds 500dp - otherwise a wide screen falls into the "regular"
            // portrait-style layout and the icon grid ends up oversized and dominant.
            val compact = maxHeight < 500.dp || maxWidth > maxHeight
            if (compact) {
                CompactStraightLayout(
                    editable = !readOnly,
                    t = { key, fallback -> t(key, fallback) },
                    name1 = name1,
                    name2 = name2,
                    score1 = score1,
                    score2 = score2,
                    stats1 = stats1,
                    stats2 = stats2,
                    currentRun1 = currentRun1,
                    currentRun2 = currentRun2,
                    currentPlayer = currentPlayer,
                    ballsOnTable = ballsOnTable,
                    raceTo = state.settings.raceTo,
                    maxInnings = state.settings.maxInnings,
                    isOver = isOver,
                    winnerName = if (winner == 0) name1 else if (winner == 1) name2 else null,
                    isDraw = winner == StraightMatchEngine.DRAW,
                    onBallsClick = { showSetBallsDialog = true },
                    onAddBall = { viewModel.addBalls(1) },
                    onSubtractBall = { viewModel.addBalls(-1) },
                    onFoul = ::onFoulClicked,
                    onSwitch = { viewModel.endTurn(InningEndType.NONE) },
                    onRack = { showRackDialog = true },
                    onSaveAndFinish = { viewModel.saveToArchive(); onBack() },
                    onSaveAndRematch = { viewModel.saveToArchive(); onSaveAndRematch() },
                )
            } else {
                RegularStraightLayout(
                    editable = !readOnly,
                    t = { key, fallback -> t(key, fallback) },
                    name1 = name1,
                    name2 = name2,
                    score1 = score1,
                    score2 = score2,
                    stats1 = stats1,
                    stats2 = stats2,
                    currentRun1 = currentRun1,
                    currentRun2 = currentRun2,
                    currentPlayer = currentPlayer,
                    ballsOnTable = ballsOnTable,
                    raceTo = state.settings.raceTo,
                    maxInnings = state.settings.maxInnings,
                    isOver = isOver,
                    winnerName = if (winner == 0) name1 else if (winner == 1) name2 else null,
                    isDraw = winner == StraightMatchEngine.DRAW,
                    onBallsClick = { showSetBallsDialog = true },
                    onAddBall = { viewModel.addBalls(1) },
                    onSubtractBall = { viewModel.addBalls(-1) },
                    onFoul = ::onFoulClicked,
                    onSwitch = { viewModel.endTurn(InningEndType.NONE) },
                    onRack = { showRackDialog = true },
                    onSaveAndFinish = { viewModel.saveToArchive(); onBack() },
                    onSaveAndRematch = { viewModel.saveToArchive(); onSaveAndRematch() },
                )
            }
        }
    }

    if (showBreakFoulDialog) {
        AlertDialog(
            onDismissRequest = { showBreakFoulDialog = false },
            title = { HideStatusBarInDialog(); Text(t(StraightMatchTextKey.BREAK_FOUL_TITLE, "Break foul")) },
            text = { Text(t(StraightMatchTextKey.BREAK_FOUL_QUESTION, "Was this a break foul (illegal opening break)?")) },
            confirmButton = {
                TextButton(onClick = {
                    showBreakFoulDialog = false
                    showRebreakDialog = true
                }) { Text(t(StraightMatchTextKey.YES, "Yes")) }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.foul(breakFoul = false, reRack = false)
                    showBreakFoulDialog = false
                }) { Text(t(StraightMatchTextKey.NO_NORMAL_FOUL, "No, normal foul")) }
            },
        )
    }

    if (showRebreakDialog) {
        AlertDialog(
            onDismissRequest = { showRebreakDialog = false },
            title = { HideStatusBarInDialog(); Text(t(StraightMatchTextKey.REBREAK_TITLE, "Break foul  −2")) },
            text = {
                Text(
                    t(
                        StraightMatchTextKey.REBREAK_EXPLANATION,
                        "The incoming player may accept the table, or require another opening break. " +
                            "On a re-break the same player breaks again, the −2 stays, and it is still the first inning.",
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.foul(breakFoul = true, reRack = true)
                    showRebreakDialog = false
                }) { Text(t(StraightMatchTextKey.REQUIRE_REBREAK, "Require re-break")) }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.foul(breakFoul = true, reRack = false)
                    showRebreakDialog = false
                }) { Text(t(StraightMatchTextKey.OPPONENT_TAKES_TABLE, "Opponent takes table")) }
            },
        )
    }

    if (showThirdFoulDialog) {
        AlertDialog(
            onDismissRequest = { showThirdFoulDialog = false },
            title = { HideStatusBarInDialog(); Text(t(StraightMatchTextKey.THIRD_FOUL_TITLE, "Third consecutive foul")) },
            text = {
                Text(
                    t(
                        StraightMatchTextKey.THIRD_FOUL_EXPLANATION,
                        "−1 as usual plus an extra −15, all 15 balls are re-racked, and the same player " +
                            "must re-break. Only count it if the player was warned after the 2nd foul - " +
                            "otherwise it is just a normal foul.",
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.foul(breakFoul = false, reRack = true)
                    showThirdFoulDialog = false
                }) { Text(t(StraightMatchTextKey.THIRD_FOUL_CONFIRM, "Third foul  −16")) }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.foul(breakFoul = false, reRack = false)
                    showThirdFoulDialog = false
                }) { Text(t(StraightMatchTextKey.NORMAL_FOUL, "Normal foul")) }
            },
        )
    }

    if (showRackDialog) {
        var remaining by remember { mutableStateOf(1) }
        AlertDialog(
            onDismissRequest = { showRackDialog = false },
            title = {
                HideStatusBarInDialog()
                Text(t(StraightMatchTextKey.RERACK_TITLE, "Re-rack?"), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(t(StraightMatchTextKey.BALLS_REMAINING_QUESTION, "How many balls are still on the table?"))
                    Spacer(Modifier.height(16.dp))
                    NumberStepper(label = "", value = remaining, onValueChange = { remaining = it }, min = 0, max = 1)
                }
            },
            confirmButton = {
                BallsDialogActions(
                    primaryLabel = t(StraightMatchTextKey.RERACK_ACTION, "Re-rack"),
                    onPrimary = { viewModel.setBallsOnTable(remaining); showRackDialog = false },
                    onCancel = { showRackDialog = false },
                )
            },
        )
    }

    if (showSetBallsDialog) {
        var tempValue by remember(ballsOnTable) { mutableStateOf(ballsOnTable) }
        var foulToggled by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showSetBallsDialog = false },
            title = {
                HideStatusBarInDialog()
                Text(t(StraightMatchTextKey.BALLS_ON_TABLE_TITLE, "Balls on table?"), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    BallRackPicker(remaining = tempValue, onValueChange = { tempValue = it })
                    Spacer(Modifier.height(16.dp))
                    NumberStepper(label = "", value = tempValue, onValueChange = { tempValue = it }, min = 0, max = 15)
                }
            },
            confirmButton = {
                BallsDialogActions(
                    primaryLabel = t(StraightMatchTextKey.SET_ACTION, "Set"),
                    onPrimary = {
                        viewModel.setBallsOnTable(tempValue)
                        // If Foul was toggled, run the same break/third-foul detection as the X button so
                        // the penalty (and the turn switch a foul itself already causes) matches pressing X.
                        // Otherwise, a turn switch only makes sense if the run actually stopped mid-rack
                        // (2-15 balls left) - setting 0 or 1 means the rack was run out, which the engine
                        // itself already continues as the SAME player's next rack, so forcing a switch here
                        // would wrongly hand the table to the opponent mid-run.
                        if (foulToggled) {
                            val freshState = viewModel.match.value ?: state
                            when {
                                StraightMatchEngine.isBreakFoulPossible(freshState) -> showBreakFoulDialog = true
                                StraightMatchEngine.wouldBeThirdConsecutiveFoul(freshState) -> showThirdFoulDialog = true
                                else -> viewModel.foul(breakFoul = false, reRack = false)
                            }
                        } else if (tempValue >= 2) {
                            viewModel.endTurn(InningEndType.NONE)
                        }
                        showSetBallsDialog = false
                    },
                    onCancel = { showSetBallsDialog = false },
                    extraButton = {
                        FoulToggleButton(selected = foulToggled, onClick = { foulToggled = !foulToggled }, modifier = Modifier.weight(1f))
                    },
                )
            },
        )
    }

    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { HideStatusBarInDialog(); Text(t(StraightMatchTextKey.MATCH_HISTORY_TITLE, "Match history")) },
            text = {
                val rows = remember(state.actions) { buildHistoryRows(state) }
                HistoryTable(rows = rows, name1 = name1, name2 = name2, inningLabel = t(StraightMatchTextKey.INNING_ABBREV, "I"))
            },
            confirmButton = { TextButton(onClick = { showHistoryDialog = false }) { Text(t(StraightMatchTextKey.CLOSE, "Close")) } },
        )
    }

    if (showRebuildRulesDialog) {
        RebuildRulesDialog(onDismiss = { showRebuildRulesDialog = false })
    }

    if (showDiscardConfirm) {
        ConfirmDialog(
            title = t(StraightMatchTextKey.DISCARD_MATCH_TITLE, "Discard match?"),
            message = t(
                StraightMatchTextKey.DISCARD_MATCH_MESSAGE,
                "Going back now discards this match's progress - it hasn't been saved. Use the save icon instead to keep it.",
            ),
            confirmText = t(StraightMatchTextKey.DISCARD, "Discard"),
            onConfirm = { showDiscardConfirm = false; onBack() },
            onDismiss = { showDiscardConfirm = false },
        )
    }
}

/** One row of the match-history table: a single player's turn, with that turn's score ("S") and running total ("Σ"). */
private data class HistoryRow(
    val inning: Int,
    val p1Serie: Int?,
    val p1Total: Int?,
    val p2Serie: Int?,
    val p2Total: Int?,
)

/**
 * Chronological, per-INNING breakdown of a straight-pool match, for the history table. One inning
 * is one full round - both players' turns, in whichever order they actually played - shown on a
 * single row, not one row per individual turn. Consecutive records always alternate players (every
 * EndInning/foul flips whose turn it is), so pairing them two at a time recovers the inning grouping.
 * The trailing entry is unpaired only while the match is mid-inning: it's the current, still-open
 * turn of whichever player is up right now, shown alone with the other side blank until it, too,
 * completes and gets paired with the next inning's opening turn.
 */
private fun buildHistoryRows(state: StraightMatchState): List<HistoryRow> {
    var p1Total = 0
    var p2Total = 0
    val rows = mutableListOf<HistoryRow>()
    val currentRecord = StraightMatchEngine.currentRecord(state)
    val allRecords = StraightMatchEngine.completedRecords(state) + listOfNotNull(currentRecord)

    var i = 0
    while (i < allRecords.size) {
        val first = allRecords[i]
        val second = allRecords.getOrNull(i + 1)
        val inning = rows.size + 1
        if (second != null && second.playerIndex != first.playerIndex) {
            val p1Record = if (first.playerIndex == 0) first else second
            val p2Record = if (first.playerIndex == 0) second else first
            p1Total += p1Record.net
            p2Total += p2Record.net
            rows += HistoryRow(inning, p1Record.net, p1Total, p2Record.net, p2Total)
            i += 2
        } else {
            if (first.playerIndex == 0) {
                p1Total += first.net
                rows += HistoryRow(inning, first.net, p1Total, null, null)
            } else {
                p2Total += first.net
                rows += HistoryRow(inning, null, null, first.net, p2Total)
            }
            i += 1
        }
    }
    return rows
}

@Composable
private fun HistoryTable(rows: List<HistoryRow>, name1: String, name2: String, inningLabel: String = "I") {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.weight(1f))
            Text(name1, modifier = Modifier.weight(2f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(name2, modifier = Modifier.weight(2f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Surface(color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
            Row(modifier = Modifier.fillMaxWidth()) {
                HistoryCell(inningLabel, modifier = Modifier.weight(1f), bold = true)
                HistoryCell("S", modifier = Modifier.weight(1f), bold = true)
                HistoryCell("Σ", modifier = Modifier.weight(1f), bold = true)
                HistoryCell("S", modifier = Modifier.weight(1f), bold = true)
                HistoryCell("Σ", modifier = Modifier.weight(1f), bold = true)
            }
        }
        Column(modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
            rows.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    HistoryCell(row.inning.toString().padStart(2, '0'), modifier = Modifier.weight(1f))
                    HistoryCell(row.p1Serie?.toString() ?: "", modifier = Modifier.weight(1f))
                    HistoryCell(row.p1Total?.toString() ?: "", modifier = Modifier.weight(1f))
                    HistoryCell(row.p2Serie?.toString() ?: "", modifier = Modifier.weight(1f))
                    HistoryCell(row.p2Total?.toString() ?: "", modifier = Modifier.weight(1f))
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun HistoryCell(text: String, modifier: Modifier = Modifier, bold: Boolean = false) {
    Text(
        text = text,
        modifier = modifier.padding(vertical = 6.dp),
        textAlign = TextAlign.Center,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    )
}

/** Toggle button for the "Balls on table" dialog's Foul option - outlined when off, filled when on.
 * Takes a [modifier] so it can be given the same [androidx.compose.foundation.layout.RowScope.weight]
 * as its Cancel/Set siblings in [info.rbuck.billiardscoreboard.ui.components.BallsDialogActions]. */
@Composable
private fun FoulToggleButton(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = DialogButtonShape,
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
        modifier = modifier,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
            Text("Foul", fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun RegularStraightLayout(
    editable: Boolean = true,
    t: (StraightMatchTextKey, String) -> String,
    name1: String,
    name2: String,
    score1: Int,
    score2: Int,
    stats1: PlayerStats,
    stats2: PlayerStats,
    currentRun1: Int,
    currentRun2: Int,
    currentPlayer: Int,
    ballsOnTable: Int,
    raceTo: Int,
    maxInnings: Int?,
    isOver: Boolean,
    winnerName: String?,
    isDraw: Boolean,
    onBallsClick: () -> Unit,
    onAddBall: () -> Unit,
    onSubtractBall: () -> Unit,
    onFoul: () -> Unit,
    onSwitch: () -> Unit,
    onRack: () -> Unit,
    onSaveAndFinish: () -> Unit,
    onSaveAndRematch: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (maxInnings != null) {
                t(StraightMatchTextKey.RACE_TO_INNINGS_LIMIT, "Race to %d - Innings limit %d").format(raceTo, maxInnings)
            } else {
                t(StraightMatchTextKey.RACE_TO, "Race to %d").format(raceTo)
            },
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(12.dp))

        val inningLabel = t(StraightMatchTextKey.INNING_ABBREV, "I")
        StraightPlayerCard(name1, score1, stats1, currentRun1, isCurrent = currentPlayer == 0, modifier = Modifier.fillMaxWidth().weight(1f), inningLabel = inningLabel)

        Spacer(Modifier.height(16.dp))
        // Same fixed-height, full-width-stretched row as the landscape layout uses - keeps button
        // height identical in both orientations and leaves the two player cards above/below the
        // room they need, instead of the old width-derived tile grid which grew tall enough on a
        // portrait tablet to squeeze the cards short and clip their bottom stats line.
        val uiScale = LocalUiScale.current
        val actionButtonHeight = CompactActionButtonHeight * uiScale
        val actionIconSize = Modifier.size(24.dp * uiScale)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            BallsDisplayTile(value = ballsOnTable, fontSize = 18.sp * uiScale, onClick = onBallsClick, enabled = editable, modifier = Modifier.weight(1f).height(actionButtonHeight))
            ActionIconButton(icon = { Text("-", fontSize = 20.sp * uiScale, fontWeight = FontWeight.Bold) }, onClick = onSubtractBall, enabled = !isOver && editable, modifier = Modifier.weight(1f).height(actionButtonHeight))
            ActionIconButton(icon = { Text("+", fontSize = 20.sp * uiScale, fontWeight = FontWeight.Bold) }, onClick = onAddBall, enabled = !isOver && editable, modifier = Modifier.weight(1f).height(actionButtonHeight))
            ActionIconButton(icon = { RackIcon(size = 18.dp * uiScale) }, onClick = onRack, enabled = !isOver && editable, modifier = Modifier.weight(1f).height(actionButtonHeight))
            ActionIconButton(icon = { Icon(Icons.Filled.SwapVert, contentDescription = "Switch", modifier = actionIconSize) }, onClick = onSwitch, enabled = !isOver && editable, modifier = Modifier.weight(1f).height(actionButtonHeight))
            ActionIconButton(icon = { Icon(Icons.Filled.Close, contentDescription = "Foul", modifier = actionIconSize) }, onClick = onFoul, enabled = !isOver && editable, modifier = Modifier.weight(1f).height(actionButtonHeight))
        }
        Spacer(Modifier.height(16.dp))

        StraightPlayerCard(name2, score2, stats2, currentRun2, isCurrent = currentPlayer == 1, modifier = Modifier.fillMaxWidth().weight(1f), inningLabel = inningLabel)

        if (isOver) {
            Spacer(Modifier.height(20.dp))
            Text(
                text = when {
                    winnerName != null -> t(StraightMatchTextKey.MATCH_WON_BY, "%s won the match!").format(winnerName)
                    isDraw -> t(StraightMatchTextKey.MATCH_DRAW, "The match is a draw!")
                    else -> t(StraightMatchTextKey.MATCH_FINISHED, "Match finished")
                },
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (editable) {
                    Button(onClick = onSaveAndFinish) { Text(t(StraightMatchTextKey.SAVE_AND_FINISH, "Save & finish")) }
                    Button(onClick = onSaveAndRematch) { Text(t(StraightMatchTextKey.SAVE_AND_REMATCH, "Save & Rematch")) }
                } else {
                    Button(onClick = onSaveAndRematch) { Text(t(StraightMatchTextKey.REMATCH, "Rematch")) }
                }
            }
        }
    }
}

@Composable
private fun CompactStraightLayout(
    editable: Boolean = true,
    t: (StraightMatchTextKey, String) -> String,
    name1: String,
    name2: String,
    score1: Int,
    score2: Int,
    stats1: PlayerStats,
    stats2: PlayerStats,
    currentRun1: Int,
    currentRun2: Int,
    currentPlayer: Int,
    ballsOnTable: Int,
    raceTo: Int,
    maxInnings: Int?,
    isOver: Boolean,
    winnerName: String?,
    isDraw: Boolean,
    onBallsClick: () -> Unit,
    onAddBall: () -> Unit,
    onSubtractBall: () -> Unit,
    onFoul: () -> Unit,
    onSwitch: () -> Unit,
    onRack: () -> Unit,
    onSaveAndFinish: () -> Unit,
    onSaveAndRematch: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            if (maxInnings != null) {
                t(StraightMatchTextKey.RACE_TO_INNINGS_LIMIT, "Race to %d - Innings limit %d").format(raceTo, maxInnings)
            } else {
                t(StraightMatchTextKey.RACE_TO, "Race to %d").format(raceTo)
            },
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val inningLabel = t(StraightMatchTextKey.INNING_ABBREV, "I")
            StraightPlayerCard(
                name1, score1, stats1, currentRun1,
                isCurrent = currentPlayer == 0,
                compact = true,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                inningLabel = inningLabel,
            )
            StraightPlayerCard(
                name2, score2, stats2, currentRun2,
                isCurrent = currentPlayer == 1,
                compact = true,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                inningLabel = inningLabel,
            )
        }

        if (isOver) {
            Spacer(Modifier.height(10.dp))
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = when {
                        winnerName != null -> t(StraightMatchTextKey.WINS_SHORT, "%s wins!").format(winnerName)
                        isDraw -> t(StraightMatchTextKey.DRAW_SHORT, "Draw!")
                        else -> t(StraightMatchTextKey.FINISHED_SHORT, "Finished")
                    },
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (editable) {
                        Button(onClick = onSaveAndFinish) { Text(t(StraightMatchTextKey.SAVE_AND_FINISH, "Save & finish")) }
                        Button(onClick = onSaveAndRematch) { Text(t(StraightMatchTextKey.SAVE_AND_REMATCH, "Save & Rematch")) }
                    } else {
                        Button(onClick = onSaveAndRematch) { Text(t(StraightMatchTextKey.REMATCH, "Rematch")) }
                    }
                }
            }
        } else {
            Spacer(Modifier.height(12.dp))
            val uiScale = LocalUiScale.current
            val actionButtonHeight = CompactActionButtonHeight * uiScale
            val actionIconSize = Modifier.size(24.dp * uiScale)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                BallsDisplayTile(value = ballsOnTable, fontSize = 18.sp * uiScale, onClick = onBallsClick, enabled = editable, modifier = Modifier.weight(1f).height(actionButtonHeight))
                ActionIconButton(icon = { Text("-", fontSize = 20.sp * uiScale, fontWeight = FontWeight.Bold) }, onClick = onSubtractBall, enabled = editable, modifier = Modifier.weight(1f).height(actionButtonHeight))
                ActionIconButton(icon = { Text("+", fontSize = 20.sp * uiScale, fontWeight = FontWeight.Bold) }, onClick = onAddBall, enabled = editable, modifier = Modifier.weight(1f).height(actionButtonHeight))
                ActionIconButton(icon = { RackIcon(size = 18.dp * uiScale) }, onClick = onRack, enabled = editable, modifier = Modifier.weight(1f).height(actionButtonHeight))
                ActionIconButton(icon = { Icon(Icons.Filled.SwapVert, contentDescription = "Switch", modifier = actionIconSize) }, onClick = onSwitch, enabled = editable, modifier = Modifier.weight(1f).height(actionButtonHeight))
                ActionIconButton(icon = { Icon(Icons.Filled.Close, contentDescription = "Foul", modifier = actionIconSize) }, onClick = onFoul, enabled = editable, modifier = Modifier.weight(1f).height(actionButtonHeight))
            }
        }
    }
}

/** Landscape action-icon height - well under half the size the old aspectRatio(1f)-across-full-width tiles used to be. */
private val CompactActionButtonHeight = 56.dp

/** All action buttons - the ball stepper and the icon actions alike - share this same rounded-square shape. */
private val ActionButtonShape = RoundedCornerShape(14.dp)

@Composable
private fun ActionIconButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = ActionButtonShape,
        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            icon()
        }
    }
}

/** Landscape-row-sized sibling of [BallsNumberTile] - same filled, tappable styling, smaller text. */
@Composable
private fun BallsDisplayTile(value: Int, fontSize: TextUnit, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = ActionButtonShape,
        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(value.toString(), fontSize = fontSize, fontWeight = FontWeight.Bold)
        }
    }
}

/** Six balls in a triangle rack, mirroring the classic Billiard Score rack icon. */
@Composable
private fun RackIcon(size: Dp) {
    val color = MaterialTheme.colorScheme.onPrimary
    Canvas(modifier = Modifier.size(size)) {
        val r = this.size.minDimension / 9f
        val rows = listOf(1, 2, 3)
        val rowSpacing = this.size.height / 3.4f
        var y = r * 1.2f
        for (count in rows) {
            val rowWidth = (count - 1) * r * 2.2f
            var x = (this.size.width - rowWidth) / 2f
            repeat(count) {
                drawCircle(color = color, radius = r, center = Offset(x, y))
                x += r * 2.2f
            }
            y += rowSpacing
        }
    }
}

@Composable
private fun StraightPlayerCard(
    name: String,
    score: Int,
    stats: PlayerStats,
    currentRun: Int,
    isCurrent: Boolean,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
    inningLabel: String = "I",
) {
    // Extra boost on top of the normal LocalUiScale multiplier, on a tablet (uiScale > 1f) - phone
    // (uiScale == 1f) is untouched. Same in portrait and landscape, so text doesn't shrink on rotation.
    val uiScale = LocalUiScale.current
    val isTablet = uiScale > 1f
    val compactScoreMultiplier = when {
        isTablet -> 4f
        // Regular (non-compact, i.e. portrait full-height card) layout has plenty of vertical room
        // even on a phone - large modern phones (e.g. Pixel 9 Pro) otherwise show a score that looks
        // tiny in that much empty card space. Compact (landscape/short-screen) phone layout is left
        // at 1x since its cards are noticeably shorter and a bigger boost risks overflow there.
        !compact -> 2.2f
        else -> 1f
    }
    val compactTextMultiplier = if (isTablet) 1.5f else 1f
    Box(modifier = modifier) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(if (compact) 10.dp else 12.dp),
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
                val avg = (stats.average * 10).roundToInt() / 10f
                val baseStatsStyle = MaterialTheme.typography.bodyMedium
                Text(
                    "Ø $avg • HS:${stats.highestBreak} • $inningLabel:${stats.innings} • S:$currentRun",
                    style = baseStatsStyle.copy(
                        fontSize = baseStatsStyle.fontSize * compactTextMultiplier,
                        lineHeight = baseStatsStyle.lineHeight * compactTextMultiplier,
                    ),
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (isCurrent) {
            ActivePlayerIndicator(compact)
        }
    }
}
