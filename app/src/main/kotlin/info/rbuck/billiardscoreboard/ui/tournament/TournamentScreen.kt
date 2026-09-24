package info.rbuck.billiardscoreboard.ui.tournament

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import info.rbuck.billiardscoreboard.domain.tournament.TournamentEngine
import info.rbuck.billiardscoreboard.domain.tournament.TournamentMode
import info.rbuck.billiardscoreboard.i18n.AppLanguage
import info.rbuck.billiardscoreboard.i18n.Translations
import info.rbuck.billiardscoreboard.i18n.TournamentTextKey
import info.rbuck.billiardscoreboard.ui.bsApplication
import info.rbuck.billiardscoreboard.ui.components.ConfirmDialog
import info.rbuck.billiardscoreboard.ui.theme.LocalUiScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentScreen(
    tournamentId: String,
    onBack: () -> Unit,
    onSaveAndRematch: () -> Unit,
) {
    val app = bsApplication()
    val viewModel: TournamentViewModel = viewModel(
        factory = viewModelFactory {
            initializer { TournamentViewModel(tournamentId, app.tournamentRepository, app.playerRepository) }
        },
    )
    val tournament by viewModel.tournament.collectAsStateWithLifecycle()
    val players by viewModel.players.collectAsStateWithLifecycle()
    val language by app.settingsRepository.language.collectAsStateWithLifecycle()
    fun t(key: TournamentTextKey, fallback: String) = Translations.tournamentText(key, language) ?: fallback
    val state = tournament ?: return

    fun nameOf(id: String) = players[id]?.name ?: "?"

    // Unified across every mode: sides.first/second are the two 1v1 players, or (PARTNER_ROTATION)
    // the two doubles teams - see TournamentEngine.currentSides. Lets the whole rest of this screen
    // stay mode-agnostic instead of branching singles vs. doubles everywhere.
    val sides = TournamentEngine.currentSides(state)
    val encounterScore = TournamentEngine.currentEncounterScore(state)
    val waiting = TournamentEngine.waitingPlayers(state)
    val winCounts = TournamentEngine.winCounts(state)
    // Individual-game score (e.g. "2:1") only earns its place next to the win count when an
    // encounter can actually produce one - with the gamesPerEncounter=1 default every encounter
    // is a single game, so it would just repeat the win count and add noise.
    val showGameTally = state.settings.gamesPerEncounter > 1
    val gameTally = if (showGameTally) TournamentEngine.gameTally(state) else emptyMap()
    val isOver = TournamentEngine.isOver(state)
    val championId = TournamentEngine.championId(state)
    val championTeam = TournamentEngine.championTeam(state)
    val isDraw = TournamentEngine.isDraw(state)
    val standings = TournamentEngine.standingsOrder(state)
    val standingsRanks = TournamentEngine.standingsRanks(state)
    val roundInfo = TournamentEngine.currentRound(state)
    val partnerRound = TournamentEngine.partnerRotationRound(state)

    val modeEnglishFallback = when (state.settings.mode) {
        TournamentMode.LOSER_STAYS -> "Loser stays"
        TournamentMode.WINNER_STAYS -> "Winner stays"
        TournamentMode.ROUND_ROBIN -> "Round robin"
        TournamentMode.SUDDEN_DEATH -> "Sudden death"
        TournamentMode.SINGLE_ELIMINATION -> "Single elimination"
        TournamentMode.PARTNER_ROTATION -> "Rotating doubles"
    }
    val modeLabel = Translations.tournamentModeLabel(state.settings.mode, language) ?: modeEnglishFallback
    val subtitle = state.settings.targetWins?.let {
        if (language == AppLanguage.EN) {
            "$modeLabel · target $it wins"
        } else {
            "$modeLabel · $it ${t(TournamentTextKey.TARGET_WINS, "points for tournament win")}"
        }
    } ?: modeLabel

    // Leaving mid-tournament doesn't discard anything - every recorded game is saved immediately
    // (see TournamentViewModel.mutate) and the tournament stays resumable from the Tournaments
    // list. Still confirm before leaving, same as every other match screen, so a stray back tap
    // doesn't silently drop the player out of an unfinished tournament.
    var showLeaveConfirm by remember { mutableStateOf(false) }
    BackHandler(enabled = !isOver) { showLeaveConfirm = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t(TournamentTextKey.TOURNAMENT_TITLE, "Tournament")) },
                navigationIcon = {
                    IconButton(onClick = { if (isOver) onBack() else showLeaveConfirm = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::undo, enabled = TournamentEngine.canUndo(state)) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }
                },
            )
        },
    ) { padding ->
        // Side-by-side table/standings only in actual landscape rotation - a tablet in portrait is
        // still wider than 560dp, but splitting it into two columns there leaves standings a
        // near-empty sliver next to a lot of unused space below; stacked reads much better there.
        // Uses the device's real orientation (like RebuildRulesDialog) rather than comparing
        // measured maxWidth/maxHeight, which can misreport a resized emulator window's aspect ratio.
        val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            val wide = maxWidth > 560.dp && isLandscape

            val header = @Composable {
                Text(subtitle, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }

            // In LOSER_STAYS/WINNER_STAYS/SUDDEN_DEATH, side 0 is always the "resident" (the
            // player staying at the table under this mode's rule) and side 1 the challenger - so
            // they always render in the same slot, never swapping sides depending on who happened
            // to be sitting where. No such concept for ROUND_ROBIN/SINGLE_ELIMINATION/PARTNER_ROTATION.
            // Nothing to show yet in the very first encounter - "staying (lost/won last)" only
            // makes sense once there's a previous result to have stayed on the back of (see
            // TournamentEngine.deriveRotation's "nobody has 'stayed' yet" comment).
            val residentLabel = if (state.results.isEmpty()) null else when (state.settings.mode) {
                TournamentMode.LOSER_STAYS -> t(TournamentTextKey.STAYING_LOST_LAST, "Staying (lost last)")
                TournamentMode.WINNER_STAYS, TournamentMode.SUDDEN_DEATH -> t(TournamentTextKey.STAYING_WON_LAST, "Staying (won last)")
                TournamentMode.ROUND_ROBIN, TournamentMode.SINGLE_ELIMINATION, TournamentMode.PARTNER_ROTATION -> null
            }
            val challengerLabel = if (residentLabel != null) t(TournamentTextKey.CHALLENGER, "Challenger") else null

            val roundLabel = when {
                state.settings.mode == TournamentMode.PARTNER_ROTATION ->
                    partnerRound?.let { t(TournamentTextKey.ROUND_NUMBER, "Round %d").format(it) }
                roundInfo != null -> t(TournamentTextKey.ROUND, "Round %d of %d").format(roundInfo.first, roundInfo.second)
                else -> null
            } ?: t(TournamentTextKey.TABLE_NOW, "Table now")

            val finishedBlock = @Composable {
                val resultText = when {
                    isDraw -> t(TournamentTextKey.DRAW, "It's a draw!")
                    championTeam != null -> t(TournamentTextKey.TEAM_WINS_TOURNAMENT, "%s win the tournament!")
                        .format(championTeam.joinToString(" & ") { nameOf(it) })
                    championId != null -> t(TournamentTextKey.WINS_TOURNAMENT, "%s wins the tournament!").format(nameOf(championId))
                    else -> t(TournamentTextKey.TOURNAMENT_FINISHED, "Tournament finished")
                }
                Text(resultText, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onBack, modifier = Modifier.weight(1f)) { Text(t(TournamentTextKey.SAVE_AND_FINISH, "Save & finish")) }
                    Button(onClick = onSaveAndRematch, modifier = Modifier.weight(1f)) { Text(t(TournamentTextKey.SAVE_AND_REMATCH, "Save & Rematch")) }
                }
            }

            val tableSection: @Composable ColumnScope.() -> Unit = {
                if (sides != null && !isOver) {
                    Text(roundLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        EncounterScoreCard(
                            names = sides.first.map { nameOf(it) },
                            score = encounterScore?.first ?: 0,
                            roleLabel = residentLabel,
                            canRemove = TournamentEngine.canRemoveLastEncounterGameFor(state, 0),
                            onAdd = { viewModel.recordEncounterGame(winnerId = sides.first[0], loserId = sides.second[0]) },
                            onRemove = { viewModel.removeLastEncounterGameFor(0) },
                            modifier = Modifier.weight(1f),
                        )
                        EncounterScoreCard(
                            names = sides.second.map { nameOf(it) },
                            score = encounterScore?.second ?: 0,
                            roleLabel = challengerLabel,
                            canRemove = TournamentEngine.canRemoveLastEncounterGameFor(state, 1),
                            onAdd = { viewModel.recordEncounterGame(winnerId = sides.second[0], loserId = sides.first[0]) },
                            onRemove = { viewModel.removeLastEncounterGameFor(1) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else if (isOver) {
                    finishedBlock()
                }
                if (waiting.isNotEmpty() && !isOver) {
                    Spacer(Modifier.height(16.dp))
                    // Card (no border - flat surfaceVariant tile like the tournament-list rows in
                    // TournamentHistoryScreen) rather than bare text, and stretched down to the
                    // bottom of its column so the screen doesn't read as half-empty once there's
                    // little else below the score cards. Weighted 1:3 against the standings card
                    // below (stacked/narrow layout only - each is the sole weighted child of its
                    // own column in the wide layout, so the ratio is a no-op there) since it only
                    // ever holds one line of names, while standings needs room for up to 8 rows.
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    ) {
                        Text(
                            t(TournamentTextKey.WAITING, "Waiting: %s").format(waiting.joinToString(", ") { nameOf(it) }),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }

            val standingsSection: @Composable ColumnScope.() -> Unit = {
                // Card (no border) instead of bare text+rows, stretched down to the bottom of its
                // column - a short standings list otherwise leaves a lot of unclaimed space
                // (especially in the wide two-column layout), reading as unfinished rather than
                // intentionally simple.
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().weight(3f),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(t(TournamentTextKey.STANDINGS, "Standings"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        standings.forEachIndexed { index, id ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val isChampion = id == championId || championTeam?.contains(id) == true
                                Text(
                                    "${standingsRanks[id] ?: index + 1}. ${nameOf(id)}",
                                    fontWeight = if (isChampion) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f),
                                )
                                if (showGameTally) {
                                    val (won, lost) = gameTally[id] ?: (0 to 0)
                                    Text("$won:$lost", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 16.dp))
                                }
                                Text("${winCounts[id] ?: 0}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            if (index < standings.lastIndex) HorizontalDivider()
                        }
                    }
                }
            }

            if (wide) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        header()
                        Spacer(Modifier.height(16.dp))
                        tableSection()
                    }
                    Spacer(Modifier.width(24.dp))
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        standingsSection()
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    header()
                    Spacer(Modifier.height(16.dp))
                    tableSection()
                    Spacer(Modifier.height(24.dp))
                    standingsSection()
                }
            }
        }
    }

    if (showLeaveConfirm) {
        ConfirmDialog(
            title = t(TournamentTextKey.LEAVE_TOURNAMENT_TITLE, "Leave tournament?"),
            message = t(
                TournamentTextKey.LEAVE_TOURNAMENT_MESSAGE,
                "The tournament isn't finished yet. Your progress is saved - you can resume it later from Tournaments.",
            ),
            confirmText = t(TournamentTextKey.LEAVE, "Leave"),
            onConfirm = { showLeaveConfirm = false; onBack() },
            onDismiss = { showLeaveConfirm = false },
        )
    }
}

/** One side of the current encounter: name(s), its running sub-game score (see
 * [TournamentEngine.currentEncounterScore]), and a −/+ pair below the card - "+" records a game
 * win for this side (folds into a tournament win once [TournamentSettings.gamesPerEncounter] is
 * reached), "−" takes back this side's own last game in the current encounter. Same
 * name-top/score-middle/info-bottom layout as the regular match scoreboards' (8/9/10-Ball, 14.1)
 * [info.rbuck.billiardscoreboard.ui.simplematch.SimpleMatchScreen] player cards - [roleLabel]
 * takes the "extra info" slot at the bottom (e.g. "Runouts: N" there), blank when there's nothing
 * to say yet (see the "no previous encounter" case in [TournamentScreen]). */
@Composable
private fun EncounterScoreCard(
    names: List<String>,
    score: Int,
    roleLabel: String?,
    canRemove: Boolean,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiScale = LocalUiScale.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            modifier = Modifier.fillMaxWidth().height(180.dp * uiScale),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(names.joinToString(" & "), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, maxLines = 2)
                Text(score.toString(), fontSize = 44.sp * uiScale, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(roleLabel ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            EncounterButton(
                icon = Icons.Filled.Remove,
                contentDescription = "Remove last game",
                onClick = onRemove,
                enabled = canRemove,
                filled = false,
                modifier = Modifier.weight(1f).height(48.dp * uiScale),
            )
            EncounterButton(
                icon = Icons.Filled.Add,
                contentDescription = "Record game win",
                onClick = onAdd,
                enabled = true,
                filled = true,
                modifier = Modifier.weight(1f).height(48.dp * uiScale),
            )
        }
    }
}

/** "−" reads as a muted, secondary action (same tone as the card itself); "+" is the primary
 * call-to-action - matching how the redesign's reference visually separates the two. */
@Composable
private fun EncounterButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean,
    filled: Boolean,
    modifier: Modifier,
) {
    val uiScale = LocalUiScale.current
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f).compositeOver(MaterialTheme.colorScheme.surfaceVariant)
            filled -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = when {
            !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
            filled -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = modifier,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(24.dp * uiScale))
        }
    }
}
