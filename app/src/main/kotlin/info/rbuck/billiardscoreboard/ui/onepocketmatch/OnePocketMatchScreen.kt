package info.rbuck.billiardscoreboard.ui.onepocketmatch

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SwapVert
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import info.rbuck.billiardscoreboard.data.Club
import info.rbuck.billiardscoreboard.domain.onepocket.OnePocketMatchEngine
import info.rbuck.billiardscoreboard.i18n.LocalStrings
import info.rbuck.billiardscoreboard.i18n.StraightMatchTextKey
import info.rbuck.billiardscoreboard.i18n.Translations
import info.rbuck.billiardscoreboard.obs.LiveScorePlayer
import info.rbuck.billiardscoreboard.obs.LiveScoreState
import info.rbuck.billiardscoreboard.ui.bsApplication
import info.rbuck.billiardscoreboard.ui.components.ActivePlayerIndicator
import info.rbuck.billiardscoreboard.ui.components.ConfirmDialog
import info.rbuck.billiardscoreboard.ui.components.FeltAccentSurface
import info.rbuck.billiardscoreboard.ui.components.HideStatusBarInDialog
import info.rbuck.billiardscoreboard.ui.components.LockWheelNumber
import info.rbuck.billiardscoreboard.ui.theme.AppTheme
import info.rbuck.billiardscoreboard.ui.theme.LocalUiScale

/**
 * One Pocket's scoreboard: both players share one set of turn actions (pot in your own pocket,
 * foul, or a plain miss/safety) applied to whoever's shot it currently is - unlike 8/9/10-Ball's
 * independent per-player rack buttons, since only one player can be at the table at a time here,
 * same as 14.1's shared action row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnePocketMatchScreen(
    matchId: String,
    onBack: () -> Unit,
    onSaveAndRematch: () -> Unit,
) {
    val app = bsApplication()
    val viewModel: OnePocketMatchViewModel = viewModel(
        factory = viewModelFactory {
            initializer { OnePocketMatchViewModel(matchId, app.matchRepository, app.playerRepository) }
        },
    )
    val match by viewModel.match.collectAsStateWithLifecycle()
    val players by viewModel.players.collectAsStateWithLifecycle()
    val isArchived by viewModel.isArchived.collectAsStateWithLifecycle()
    val appTheme by app.settingsRepository.appTheme.collectAsStateWithLifecycle()
    val isFelt = appTheme == AppTheme.FELT || appTheme == AppTheme.LIGHT
    val language by app.settingsRepository.language.collectAsStateWithLifecycle()
    fun t(key: StraightMatchTextKey, fallback: String) = Translations.straightMatchText(key, language) ?: fallback
    val s = LocalStrings.current
    val state = match ?: return

    // An archived match (opened from History) is a saved result - view only, no scoring controls.
    val readOnly = isArchived

    val name1 = players[state.playerIds.getOrNull(0)]?.name ?: "${s.player} 1"
    val name2 = players[state.playerIds.getOrNull(1)]?.name ?: "${s.player} 2"
    val score1 = OnePocketMatchEngine.score(state, 0)
    val score2 = OnePocketMatchEngine.score(state, 1)
    val fouls1 = OnePocketMatchEngine.foulStreak(state, 0)
    val fouls2 = OnePocketMatchEngine.foulStreak(state, 1)
    val currentPlayer = OnePocketMatchEngine.currentPlayerIndex(state)
    val winner = OnePocketMatchEngine.winnerIndex(state)
    val isOver = winner != OnePocketMatchEngine.NO_WINNER
    val winnerName = if (winner == 0) name1 else if (winner == 1) name2 else null

    val club1Id = players[state.playerIds.getOrNull(0)]?.clubId
    val club2Id = players[state.playerIds.getOrNull(1)]?.clubId
    var club1 by remember { mutableStateOf<Club?>(null) }
    var club2 by remember { mutableStateOf<Club?>(null) }
    LaunchedEffect(club1Id) { club1 = club1Id?.let { app.clubRepository.getById(it) } }
    LaunchedEffect(club2Id) { club2 = club2Id?.let { app.clubRepository.getById(it) } }
    LaunchedEffect(name1, name2, score1, score2, currentPlayer, isOver, club1, club2, state.settings.raceTo) {
        app.liveScoreRepository.publish(
            LiveScoreState(
                discipline = "One Pocket",
                raceTo = state.settings.raceTo,
                player1 = LiveScorePlayer(
                    name = name1, club = club1?.name ?: "", crestPath = club1?.crestPath,
                    score = score1, currentRun = null, highestBreak = null, innings = null,
                    atTurn = !isOver && currentPlayer == 0,
                ),
                player2 = LiveScorePlayer(
                    name = name2, club = club2?.name ?: "", crestPath = club2?.crestPath,
                    score = score2, currentRun = null, highestBreak = null, innings = null,
                    atTurn = !isOver && currentPlayer == 1,
                ),
            ),
        )
    }
    DisposableEffect(Unit) { onDispose { app.liveScoreRepository.clear() } }

    var showThirdFoulDialog by remember { mutableStateOf(false) }
    var showRulesDialog by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    BackHandler(enabled = !readOnly && state.actions.isNotEmpty()) { showDiscardConfirm = true }

    fun onFoulClicked() {
        if (OnePocketMatchEngine.wouldBeThirdFoul(state, currentPlayer)) {
            showThirdFoulDialog = true
        } else {
            viewModel.foul()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.onePocket) },
                navigationIcon = {
                    IconButton(onClick = { if (readOnly || state.actions.isEmpty()) onBack() else showDiscardConfirm = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showRulesDialog = true }) {
                        Icon(Icons.Filled.Info, contentDescription = s.onePocketRules)
                    }
                    if (!readOnly) {
                        IconButton(onClick = viewModel::undo, enabled = OnePocketMatchEngine.canUndo(state)) {
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
            val compact = maxHeight < 500.dp || maxWidth > maxHeight
            val uiScale = LocalUiScale.current
            val buttonHeight = 56.dp * uiScale

            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    t(StraightMatchTextKey.RACE_TO, "Race to %d").format(state.settings.raceTo),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(if (compact) 8.dp else 16.dp))

                val player1Card = @Composable {
                    OnePocketPlayerCard(
                        name = name1,
                        score = score1,
                        fouls = fouls1,
                        isCurrent = currentPlayer == 0,
                        compact = compact,
                        isFelt = isFelt,
                        foulsLabel = s.onePocketFouls,
                        modifier = if (compact) Modifier.weight(1f).fillMaxHeight() else Modifier.fillMaxWidth().weight(1f),
                    )
                }
                val player2Card = @Composable {
                    OnePocketPlayerCard(
                        name = name2,
                        score = score2,
                        fouls = fouls2,
                        isCurrent = currentPlayer == 1,
                        compact = compact,
                        isFelt = isFelt,
                        foulsLabel = s.onePocketFouls,
                        modifier = if (compact) Modifier.weight(1f).fillMaxHeight() else Modifier.fillMaxWidth().weight(1f),
                    )
                }

                if (compact) {
                    Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        player1Card()
                        player2Card()
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        player1Card()
                        player2Card()
                    }
                }

                if (isOver) {
                    Spacer(Modifier.height(if (compact) 8.dp else 24.dp))
                    Text(
                        text = winnerName?.let { t(StraightMatchTextKey.MATCH_WON_BY, "%s won the match!").format(it) }
                            ?: t(StraightMatchTextKey.MATCH_FINISHED, "Match finished"),
                        style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(if (compact) 8.dp else 16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (readOnly) {
                            Button(onClick = onSaveAndRematch) { Text(t(StraightMatchTextKey.REMATCH, "Rematch")) }
                        } else {
                            Button(onClick = { viewModel.saveToArchive(); onBack() }) { Text(t(StraightMatchTextKey.SAVE_AND_FINISH, "Save & finish")) }
                            Button(onClick = { viewModel.saveToArchive(); onSaveAndRematch() }) { Text(t(StraightMatchTextKey.SAVE_AND_REMATCH, "Save & Rematch")) }
                        }
                    }
                } else {
                    Spacer(Modifier.height(if (compact) 8.dp else 16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OnePocketActionButton(
                            icon = { Icon(Icons.Filled.SwapVert, contentDescription = s.onePocketMiss, modifier = Modifier.size(32.dp * uiScale)) },
                            onClick = viewModel::endTurn,
                            enabled = !readOnly,
                            isFelt = isFelt,
                            modifier = Modifier.weight(1f).height(buttonHeight),
                        )
                        OnePocketActionButton(
                            icon = { Icon(Icons.Filled.Add, contentDescription = s.onePocketPot, modifier = Modifier.size(38.dp * uiScale)) },
                            onClick = { viewModel.addPocket() },
                            enabled = !readOnly,
                            isFelt = isFelt,
                            modifier = Modifier.weight(1f).height(buttonHeight),
                        )
                        OnePocketActionButton(
                            icon = { Icon(Icons.Filled.Close, contentDescription = s.onePocketFoul, modifier = Modifier.size(32.dp * uiScale)) },
                            onClick = ::onFoulClicked,
                            enabled = !readOnly,
                            isFelt = isFelt,
                            modifier = Modifier.weight(1f).height(buttonHeight),
                        )
                    }
                }
            }
        }
    }

    if (showThirdFoulDialog) {
        AlertDialog(
            onDismissRequest = { showThirdFoulDialog = false },
            title = { HideStatusBarInDialog(); Text(s.onePocketThirdFoulTitle) },
            text = { Text(s.onePocketThirdFoulMessage) },
            confirmButton = {
                TextButton(onClick = { viewModel.foul(); showThirdFoulDialog = false }) { Text(s.onePocketThirdFoulConfirm) }
            },
            dismissButton = {
                TextButton(onClick = { showThirdFoulDialog = false }) { Text(s.cancel) }
            },
        )
    }

    if (showRulesDialog) {
        AlertDialog(
            onDismissRequest = { showRulesDialog = false },
            title = { HideStatusBarInDialog(); Text(s.onePocketRules) },
            text = { Text(s.onePocketRulesText) },
            confirmButton = { TextButton(onClick = { showRulesDialog = false }) { Text(s.close) } },
        )
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

@Composable
private fun OnePocketPlayerCard(
    name: String,
    score: Int,
    fouls: Int,
    isCurrent: Boolean,
    compact: Boolean,
    foulsLabel: String,
    isFelt: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiScale = LocalUiScale.current
    val isTablet = uiScale > 1f
    val compactScoreMultiplier = when {
        isTablet -> 4f
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
                LockWheelNumber(
                    value = score,
                    fontSize = 44.sp * uiScale * compactScoreMultiplier,
                    color = MaterialTheme.colorScheme.primary,
                )
                val baseStatsStyle = MaterialTheme.typography.bodyMedium
                Text(
                    "$foulsLabel: $fouls",
                    style = baseStatsStyle.copy(
                        fontSize = baseStatsStyle.fontSize * compactTextMultiplier,
                        lineHeight = baseStatsStyle.lineHeight * compactTextMultiplier,
                    ),
                    textAlign = TextAlign.Center,
                    color = if (fouls > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (isCurrent) {
            ActivePlayerIndicator(compact)
        }
    }
}

@Composable
private fun OnePocketActionButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    enabled: Boolean,
    isFelt: Boolean,
    modifier: Modifier = Modifier,
) {
    if (isFelt) {
        FeltAccentSurface(
            accent = MaterialTheme.colorScheme.primary,
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
            modifier = modifier,
        ) { icon() }
        return
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
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
