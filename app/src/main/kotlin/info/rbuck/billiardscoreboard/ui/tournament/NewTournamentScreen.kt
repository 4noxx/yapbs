package info.rbuck.billiardscoreboard.ui.tournament

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import info.rbuck.billiardscoreboard.domain.tournament.TournamentMode
import info.rbuck.billiardscoreboard.i18n.Translations
import info.rbuck.billiardscoreboard.i18n.TournamentTextKey
import info.rbuck.billiardscoreboard.ui.bsApplication
import info.rbuck.billiardscoreboard.ui.components.ChoiceChip
import info.rbuck.billiardscoreboard.ui.components.NumberStepper
import info.rbuck.billiardscoreboard.ui.components.PlayerSlotPicker
import info.rbuck.billiardscoreboard.ui.components.SettingsLabel
import info.rbuck.billiardscoreboard.ui.components.TopBarStartButton
import info.rbuck.billiardscoreboard.ui.theme.LocalUiScale

private const val MIN_PLAYERS = 2
private const val MAX_PLAYERS = 8
private const val DEFAULT_TARGET_WINS = 5
private const val DEFAULT_GAMES_PER_ENCOUNTER = 1
// PARTNER_ROTATION needs at least 4 (2v2) but otherwise behaves like every other mode's roster -
// up to MAX_PLAYERS, with extra players beyond 4 sitting out in rotation (see TournamentEngine).
private const val PARTNER_ROTATION_MIN_PLAYERS = 4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTournamentScreen(
    onBack: () -> Unit,
    onTournamentCreated: (String) -> Unit,
    rematchOfTournamentId: String? = null,
) {
    val app = bsApplication()
    val viewModel: NewTournamentViewModel = viewModel(
        factory = viewModelFactory {
            initializer { NewTournamentViewModel(app.playerRepository, app.tournamentRepository, app.clubRepository) }
        },
    )
    val players by viewModel.players.collectAsStateWithLifecycle()
    val clubs by viewModel.clubs.collectAsStateWithLifecycle()
    val language by app.settingsRepository.language.collectAsStateWithLifecycle()
    fun t(key: TournamentTextKey, fallback: String) = Translations.tournamentText(key, language) ?: fallback

    val slotPlayerIds: SnapshotStateList<String?> = remember { mutableStateListOf<String?>(null, null) }
    var mode by remember { mutableStateOf(TournamentMode.LOSER_STAYS) }
    var targetWins by remember { mutableStateOf(DEFAULT_TARGET_WINS) }
    var gamesPerEncounter by remember { mutableStateOf(DEFAULT_GAMES_PER_ENCOUNTER) }
    var randomOrder by remember { mutableStateOf(true) }

    LaunchedEffect(rematchOfTournamentId) {
        if (rematchOfTournamentId != null) {
            viewModel.loadForRematch(rematchOfTournamentId) { previous ->
                mode = previous.settings.mode
                previous.settings.targetWins?.let { targetWins = it }
                gamesPerEncounter = previous.settings.gamesPerEncounter
                // The previous roster is a concrete order the user may want to reuse or hand-tweak,
                // so a rematch defaults to keeping it rather than re-drawing.
                randomOrder = false
                slotPlayerIds.clear()
                slotPlayerIds.addAll(previous.playerIds)
            }
        }
    }

    // PARTNER_ROTATION needs at least 4 players (2v2) - switching to it pads the roster up to 4
    // if it's currently smaller, same as every other mode's MIN_PLAYERS floor.
    LaunchedEffect(mode) {
        if (mode == TournamentMode.PARTNER_ROTATION) {
            while (slotPlayerIds.size < PARTNER_ROTATION_MIN_PLAYERS) slotPlayerIds.add(null)
        }
    }

    val filledIds = slotPlayerIds.filterNotNull()
    val minPlayers = if (mode == TournamentMode.PARTNER_ROTATION) PARTNER_ROTATION_MIN_PLAYERS else MIN_PLAYERS
    val canStart = filledIds.size >= minPlayers && filledIds.distinct().size == filledIds.size

    val usesTargetWins = mode == TournamentMode.LOSER_STAYS || mode == TournamentMode.WINNER_STAYS || mode == TournamentMode.PARTNER_ROTATION
    val startTournament = {
        val targetOrNull = if (usesTargetWins) targetWins else null
        viewModel.createTournament(mode, targetOrNull, gamesPerEncounter, filledIds, randomOrder, onTournamentCreated)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t(TournamentTextKey.NEW_TOURNAMENT_TITLE, "New Tournament")) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    TopBarStartButton(
                        label = t(TournamentTextKey.START, "Start"),
                        enabled = canStart,
                        onClick = startTournament,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                },
            )
        },
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            val wide = maxWidth > 560.dp

            val roster = @Composable { modifier: Modifier ->
                Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    slotPlayerIds.forEachIndexed { index, id ->
                        val selectedPlayer = players.find { it.id == id }
                        val takenElsewhere = slotPlayerIds.filterIndexed { i, other -> i != index && other != null }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1f)) {
                                PlayerSlotPicker(
                                    label = "Player ${index + 1}",
                                    selected = selectedPlayer,
                                    players = players.filter { it.id !in takenElsewhere },
                                    clubs = clubs,
                                    onSelect = { slotPlayerIds[index] = it.id },
                                    onCreate = { name, cb -> viewModel.createPlayer(name, cb) },
                                )
                            }
                            if (slotPlayerIds.size > minPlayers) {
                                IconButton(onClick = { slotPlayerIds.removeAt(index) }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Remove player ${index + 1}")
                                }
                            }
                        }
                    }
                    if (slotPlayerIds.size < MAX_PLAYERS) {
                        TextButton(onClick = { slotPlayerIds.add(null) }) {
                            Text(t(TournamentTextKey.ADD_PLAYER, "+ Add player"))
                        }
                    }
                }
            }

            val modeEnglishFallback = when (mode) {
                TournamentMode.LOSER_STAYS -> "The loser stays at the table and breaks next. The winner goes to the back of the queue."
                TournamentMode.WINNER_STAYS -> "The winner stays at the table and breaks next. The loser goes to the back of the queue."
                TournamentMode.ROUND_ROBIN -> "Everyone plays everyone else exactly once. Most wins takes the tournament."
                TournamentMode.SUDDEN_DEATH -> "Like winner stays, but the loser is eliminated entirely. Last player left wins."
                TournamentMode.SINGLE_ELIMINATION -> "Classic knockout bracket. One loss eliminates you; byes fill in an uneven player count."
                TournamentMode.PARTNER_ROTATION -> "Doubles, at least 4 players. With exactly 4, partners rotate through all 3 pairings (AB-CD, AC-BD, AD-BC) each cycle. With more, 4 play each round and the rest wait, rotating fairly so everyone sits out about equally often. Both winners score a point."
            }
            val modeDescription = Translations.tournamentModeDescription(mode, language) ?: modeEnglishFallback
            fun modeLabel(m: TournamentMode, englishFallback: String) = Translations.tournamentModeLabel(m, language) ?: englishFallback

            val settings = @Composable { modifier: Modifier ->
                BoxWithConstraints(modifier = modifier) {
                    val uiScale = LocalUiScale.current
                    // Independent of `wide` (which decides the much bigger roster/settings split) -
                    // these two stepper fields are compact enough to sit side by side on their own,
                    // e.g. a phone in portrait, narrow enough that roster+settings stack, still has
                    // plenty of room for just these two next to each other instead of stacking them
                    // too. Measured on the settings column's own actual width, so it's correct both
                    // when that's the full screen width (stacked case) and half of it (wide case).
                    // Each NumberStepper is a fixed 152dp (40+72+40) wide and does NOT shrink to fit,
                    // so the threshold must scale with uiScale too (tablets get up to 1.35x) - without
                    // that, this passed on a tablet's roster+settings split (~350dp available) even
                    // though two 152*1.35=205dp-wide steppers plus their 16dp gap need ~426dp there,
                    // clipping the second stepper's "+" against its own rounded border.
                    val fieldsWide = maxWidth > 340.dp * uiScale
                    val modeBlockWidth = maxWidth
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(22.dp),
                    ) {
                        Column {
                            SettingsLabel(t(TournamentTextKey.MODE, "Mode"))
                            Spacer(Modifier.height(8.dp))
                            val modeOptions = listOf(
                                TournamentMode.LOSER_STAYS to modeLabel(TournamentMode.LOSER_STAYS, "Loser stays"),
                                TournamentMode.WINNER_STAYS to modeLabel(TournamentMode.WINNER_STAYS, "Winner stays"),
                                TournamentMode.ROUND_ROBIN to modeLabel(TournamentMode.ROUND_ROBIN, "Round robin"),
                                TournamentMode.SUDDEN_DEATH to modeLabel(TournamentMode.SUDDEN_DEATH, "Sudden death"),
                                TournamentMode.SINGLE_ELIMINATION to modeLabel(TournamentMode.SINGLE_ELIMINATION, "Single elimination"),
                                TournamentMode.PARTNER_ROTATION to modeLabel(TournamentMode.PARTNER_ROTATION, "Rotating doubles"),
                            )
                            // Two columns whenever the mode block is wide enough to fit both without
                            // the longer labels ("Single elimination") clipping; one column otherwise
                            // (narrow phone portrait). Threshold scales with uiScale like the steppers.
                            val modeColumns = if (modeBlockWidth > 360.dp * uiScale) 2 else 1
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                modeOptions.chunked(modeColumns).forEach { rowOptions ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        rowOptions.forEach { (m, label) ->
                                            ChoiceChip(selected = mode == m, onClick = { mode = m }, label = label, modifier = Modifier.weight(1f))
                                        }
                                        if (rowOptions.size < modeColumns) {
                                            Spacer(Modifier.weight((modeColumns - rowOptions.size).toFloat()))
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            // Reserve a fixed block for the description so the steppers below it land
                            // at the same vertical position for every mode - otherwise the long
                            // "Rotating doubles" text pushes them well below where they sit for the
                            // short modes. Sized for the tallest description; shorter ones top-align
                            // and leave the slack empty. Scales with uiScale like the rest.
                            Box(modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp * uiScale)) {
                                Text(modeDescription, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Column {
                            SettingsLabel(t(TournamentTextKey.STARTING_ORDER, "Starting order"))
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                ChoiceChip(
                                    selected = !randomOrder,
                                    onClick = { randomOrder = false },
                                    label = t(TournamentTextKey.ORDER_AS_LISTED, "As listed"),
                                    modifier = Modifier.weight(1f),
                                )
                                ChoiceChip(
                                    selected = randomOrder,
                                    onClick = { randomOrder = true },
                                    label = t(TournamentTextKey.ORDER_RANDOM, "Random draw"),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                t(
                                    TournamentTextKey.STARTING_ORDER_HINT,
                                    "Sets who starts and the order the remaining players join the queue.",
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        val gamesPerEncounterLabel = t(TournamentTextKey.GAMES_PER_ENCOUNTER, "Games per encounter")
                        val targetWinsLabel = t(TournamentTextKey.TARGET_WINS, "Points for tournament win")
                        val gamesPerEncounterStepper = @Composable { stepperModifier: Modifier ->
                            Box(modifier = stepperModifier) {
                                NumberStepper(
                                    label = "",
                                    value = gamesPerEncounter,
                                    onValueChange = { gamesPerEncounter = it.coerceIn(1, 9) },
                                    min = 1,
                                    max = 9,
                                )
                            }
                        }
                        val targetWinsStepper = @Composable { stepperModifier: Modifier ->
                            Box(modifier = stepperModifier) {
                                NumberStepper(label = "", value = targetWins, onValueChange = { targetWins = it.coerceAtLeast(1) }, min = 1)
                            }
                        }
                        // Side by side whenever there's room for it, so "Target wins" doesn't get
                        // pushed below the fold; stacked only when actually too narrow for both.
                        if (usesTargetWins && fieldsWide) {
                            // Labels and steppers each get their own shared Row instead of being
                            // paired off into two independent Columns - "Points for tournament win"
                            // wrapping to a 2nd line while "Games per encounter" stays on one would
                            // otherwise push its stepper down, leaving the two boxes at different
                            // heights even though each is the same height on its own. Both Rows are
                            // wrapped in one Column (itself the single direct child of the outer,
                            // 22dp-spaced settings Column) so that outer spacing lands only above/
                            // below this whole block, not between its own label and stepper rows -
                            // this inner Column's own tight spacing is what matches "New Match"'s
                            // label-directly-above-stepper look (e.g. "Race to (target)").
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                                    SettingsLabel(gamesPerEncounterLabel, modifier = Modifier.weight(1f))
                                    SettingsLabel(targetWinsLabel, modifier = Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                                    gamesPerEncounterStepper(Modifier.weight(1f))
                                    targetWinsStepper(Modifier.weight(1f))
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column {
                                    SettingsLabel(gamesPerEncounterLabel)
                                    Spacer(Modifier.height(6.dp))
                                    gamesPerEncounterStepper(Modifier)
                                }
                                if (usesTargetWins) {
                                    Column {
                                        SettingsLabel(targetWinsLabel)
                                        Spacer(Modifier.height(6.dp))
                                        targetWinsStepper(Modifier)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (wide) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(16.dp).verticalScroll(rememberScrollState())) {
                            roster(Modifier.fillMaxWidth())
                        }
                        VerticalDivider()
                        settings(Modifier.weight(1f).fillMaxHeight().padding(16.dp).verticalScroll(rememberScrollState()))
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    roster(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(20.dp))
                    settings(Modifier.fillMaxWidth())
                }
            }
        }
    }
}
