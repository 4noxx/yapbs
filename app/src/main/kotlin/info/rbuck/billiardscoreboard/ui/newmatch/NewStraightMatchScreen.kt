package info.rbuck.billiardscoreboard.ui.newmatch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import info.rbuck.billiardscoreboard.domain.GameType
import info.rbuck.billiardscoreboard.domain.Handicap
import info.rbuck.billiardscoreboard.domain.MatchStatePayload
import info.rbuck.billiardscoreboard.domain.straight.StraightMatchEngine
import info.rbuck.billiardscoreboard.ui.bsApplication
import info.rbuck.billiardscoreboard.ui.headtohead.HeadToHeadInline
import info.rbuck.billiardscoreboard.ui.components.ChoiceChip
import info.rbuck.billiardscoreboard.ui.components.MatchSetupHeaderTitle
import info.rbuck.billiardscoreboard.ui.components.NumberStepper
import info.rbuck.billiardscoreboard.ui.components.PlayerSlotPicker
import info.rbuck.billiardscoreboard.ui.components.SettingsCard
import info.rbuck.billiardscoreboard.ui.components.SettingsLabel
import info.rbuck.billiardscoreboard.ui.components.TopBarStartButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewStraightMatchScreen(
    rematchOfMatchId: String? = null,
    onBack: () -> Unit,
    onMatchCreated: (String) -> Unit,
    onGameTypeSelected: (GameType) -> Unit,
    onOpenHeadToHead: (String, String) -> Unit = { _, _ -> },
) {
    val app = bsApplication()
    val viewModel: NewMatchViewModel = viewModel(
        factory = viewModelFactory {
            initializer { NewMatchViewModel(app.playerRepository, app.matchRepository, app.clubRepository) }
        },
    )
    val players by viewModel.players.collectAsStateWithLifecycle()
    val clubs by viewModel.clubs.collectAsStateWithLifecycle()

    var player1Id by rememberSaveable { mutableStateOf<String?>(null) }
    var player2Id by rememberSaveable { mutableStateOf<String?>(null) }
    val player1 = players.find { it.id == player1Id }
    val player2 = players.find { it.id == player2Id }
    var raceTo by rememberSaveable { mutableStateOf(app.settingsRepository.defaultRaceStraightPool.value) }
    var maxInningsLimit by rememberSaveable { mutableStateOf<Int?>(null) }
    var firstPlayer by rememberSaveable { mutableStateOf(0) }
    var handicapEnabled by rememberSaveable { mutableStateOf(false) }
    var handicapPlayer by rememberSaveable { mutableStateOf(0) }
    var handicapPoints by rememberSaveable { mutableStateOf(0) }

    // "Save & Rematch" on a finished match's scoreboard lands here with rematchOfMatchId set -
    // prefill the same two players and settings instead of starting from blank defaults.
    LaunchedEffect(rematchOfMatchId) {
        val id = rematchOfMatchId ?: return@LaunchedEffect
        val previous = (app.matchRepository.loadPayload(id) as? MatchStatePayload.Straight)?.state ?: return@LaunchedEffect
        player1Id = previous.playerIds.getOrNull(0)
        player2Id = previous.playerIds.getOrNull(1)
        raceTo = previous.settings.raceTo
        maxInningsLimit = previous.settings.maxInnings
        firstPlayer = previous.settings.firstPlayerIndex
        handicapEnabled = previous.settings.handicap != null
        handicapPlayer = previous.settings.handicap?.playerIndex ?: 0
        handicapPoints = previous.settings.handicap?.points ?: 0
    }

    val maxHandicap = StraightMatchEngine.maxHandicapFor(raceTo)
    val canStart = player1 != null && player2 != null && player1?.id != player2?.id

    val headToHeadLine: @Composable () -> Unit = {
        val p1 = player1
        val p2 = player2
        if (p1 != null && p2 != null && p1.id != p2.id) {
            Spacer(Modifier.height(10.dp))
            HeadToHeadInline(
                playerAId = p1.id,
                playerBId = p2.id,
                playerAName = p1.name,
                playerBName = p2.name,
                onOpen = { onOpenHeadToHead(p1.id, p2.id) },
            )
        }
    }

    val startMatch = {
        val handicap = if (handicapEnabled && handicapPoints > 0) Handicap(handicapPlayer, handicapPoints) else null
        viewModel.createStraightMatch(
            player1 = player1!!,
            player2 = player2!!,
            raceTo = raceTo,
            maxInnings = maxInningsLimit,
            firstPlayerIndex = firstPlayer,
            handicap = handicap,
            onCreated = onMatchCreated,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { MatchSetupHeaderTitle(gameType = GameType.STRAIGHT_POOL, onGameTypeSelected = onGameTypeSelected) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    TopBarStartButton(
                        label = "Start",
                        enabled = canStart,
                        onClick = startMatch,
                        modifier = Modifier.padding(end = 12.dp),
                    )
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
            val wide = maxWidth > 560.dp
            val isPortrait = maxHeight > maxWidth
            if (wide) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(16.dp)) {
                            PlayerSlotPicker(
                                label = "Player 1",
                                selected = player1,
                                players = players.filter { it.id != player2?.id },
                                clubs = clubs,
                                onSelect = { player1Id = it.id },
                                onCreate = { name, cb -> viewModel.createPlayer(name, cb) },
                            )
                            Spacer(Modifier.height(12.dp))
                            PlayerSlotPicker(
                                label = "Player 2",
                                selected = player2,
                                players = players.filter { it.id != player1?.id },
                                clubs = clubs,
                                onSelect = { player2Id = it.id },
                                onCreate = { name, cb -> viewModel.createPlayer(name, cb) },
                            )
                            headToHeadLine()
                        }

                        VerticalDivider()

                        if (isPortrait) {
                            Column(
                                modifier = Modifier.weight(1f).fillMaxHeight().padding(16.dp).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(22.dp),
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        SettingsLabel("Race to (points)")
                                        NumberStepper(label = "", value = raceTo, onValueChange = { raceTo = it.coerceAtLeast(1) }, min = 1, step = 5)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        SettingsLabel("Innings limit")
                                        NumberStepper(
                                            label = "",
                                            value = maxInningsLimit ?: 0,
                                            onValueChange = { maxInningsLimit = if (it <= 0) null else it },
                                            min = 0,
                                            max = 50,
                                            step = 5,
                                            formatValue = { if (it <= 0) "-" else it.toString() },
                                        )
                                    }
                                }
                                Column {
                                    SettingsLabel("Opening break")
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        ChoiceChip(
                                            selected = firstPlayer == 0,
                                            onClick = { firstPlayer = 0 },
                                            label = player1?.name ?: "Player 1",
                                            modifier = Modifier.weight(1f),
                                        )
                                        ChoiceChip(
                                            selected = firstPlayer == 1,
                                            onClick = { firstPlayer = 1 },
                                            label = player2?.name ?: "Player 2",
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    SettingsLabel("Handicap")
                                    Switch(checked = handicapEnabled, onCheckedChange = { handicapEnabled = it })
                                }
                                AnimatedVisibility(visible = handicapEnabled) {
                                    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
                                        Column {
                                            SettingsLabel("Handicap for")
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                                ChoiceChip(
                                                    selected = handicapPlayer == 0,
                                                    onClick = { handicapPlayer = 0 },
                                                    label = player1?.name ?: "Player 1",
                                                    modifier = Modifier.weight(1f),
                                                )
                                                ChoiceChip(
                                                    selected = handicapPlayer == 1,
                                                    onClick = { handicapPlayer = 1 },
                                                    label = player2?.name ?: "Player 2",
                                                    modifier = Modifier.weight(1f),
                                                )
                                            }
                                        }
                                        Column {
                                            SettingsLabel("Points")
                                            NumberStepper(
                                                label = "",
                                                value = handicapPoints,
                                                onValueChange = { handicapPoints = it.coerceIn(0, maxHandicap) },
                                                min = 0,
                                                max = maxHandicap,
                                                step = 5,
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.weight(1f).fillMaxHeight().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(28.dp),
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        SettingsLabel("Race to (points)")
                                        NumberStepper(label = "", value = raceTo, onValueChange = { raceTo = it.coerceAtLeast(1) }, min = 1, step = 5)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        SettingsLabel("Innings limit")
                                        NumberStepper(
                                            label = "",
                                            value = maxInningsLimit ?: 0,
                                            onValueChange = { maxInningsLimit = if (it <= 0) null else it },
                                            min = 0,
                                            max = 50,
                                            step = 5,
                                            formatValue = { if (it <= 0) "-" else it.toString() },
                                        )
                                    }
                                }
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    SettingsLabel("Opening break")
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        ChoiceChip(
                                            selected = firstPlayer == 0,
                                            onClick = { firstPlayer = 0 },
                                            label = player1?.name ?: "Player 1",
                                            modifier = Modifier.weight(1f),
                                        )
                                        ChoiceChip(
                                            selected = firstPlayer == 1,
                                            onClick = { firstPlayer = 1 },
                                            label = player2?.name ?: "Player 2",
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                                AnimatedVisibility(visible = handicapEnabled) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        SettingsLabel("Handicap for")
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                            ChoiceChip(
                                                selected = handicapPlayer == 0,
                                                onClick = { handicapPlayer = 0 },
                                                label = player1?.name ?: "Player 1",
                                                modifier = Modifier.weight(1f),
                                            )
                                            ChoiceChip(
                                                selected = handicapPlayer == 1,
                                                onClick = { handicapPlayer = 1 },
                                                label = player2?.name ?: "Player 2",
                                                modifier = Modifier.weight(1f),
                                            )
                                        }
                                    }
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        SettingsLabel("Handicap")
                                        Switch(checked = handicapEnabled, onCheckedChange = { handicapEnabled = it })
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        AnimatedVisibility(visible = handicapEnabled) {
                                            Column {
                                                SettingsLabel("Points")
                                                NumberStepper(
                                                    label = "",
                                                    value = handicapPoints,
                                                    onValueChange = { handicapPoints = it.coerceIn(0, maxHandicap) },
                                                    min = 0,
                                                    max = maxHandicap,
                                                    step = 5,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                PlayerSlotPicker(
                    label = "Player 1",
                    selected = player1,
                    players = players.filter { it.id != player2?.id },
                    clubs = clubs,
                    onSelect = { player1Id = it.id },
                    onCreate = { name, cb -> viewModel.createPlayer(name, cb) },
                )
                Spacer(Modifier.height(8.dp))
                PlayerSlotPicker(
                    label = "Player 2",
                    selected = player2,
                    players = players.filter { it.id != player1?.id },
                    clubs = clubs,
                    onSelect = { player2Id = it.id },
                    onCreate = { name, cb -> viewModel.createPlayer(name, cb) },
                )
                headToHeadLine()

                Spacer(Modifier.height(20.dp))
                SettingsCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            SettingsLabel("Race to (points)")
                            NumberStepper(label = "", value = raceTo, onValueChange = { raceTo = it.coerceAtLeast(1) }, min = 1, step = 5)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            SettingsLabel("Innings limit")
                            NumberStepper(
                                label = "",
                                value = maxInningsLimit ?: 0,
                                onValueChange = { maxInningsLimit = if (it <= 0) null else it },
                                min = 0,
                                max = 50,
                                step = 5,
                                formatValue = { if (it <= 0) "-" else it.toString() },
                            )
                        }
                    }
                    Column {
                        SettingsLabel("Opening break")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            ChoiceChip(
                                selected = firstPlayer == 0,
                                onClick = { firstPlayer = 0 },
                                label = player1?.name ?: "Player 1",
                                modifier = Modifier.weight(1f),
                            )
                            ChoiceChip(
                                selected = firstPlayer == 1,
                                onClick = { firstPlayer = 1 },
                                label = player2?.name ?: "Player 2",
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    HorizontalDivider()
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        SettingsLabel("Handicap")
                        Switch(checked = handicapEnabled, onCheckedChange = { handicapEnabled = it })
                    }
                }

                AnimatedVisibility(visible = handicapEnabled) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(Modifier.height(20.dp))
                        SettingsCard {
                            Column {
                                SettingsLabel("Handicap for")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    ChoiceChip(
                                        selected = handicapPlayer == 0,
                                        onClick = { handicapPlayer = 0 },
                                        label = player1?.name ?: "Player 1",
                                        modifier = Modifier.weight(1f),
                                    )
                                    ChoiceChip(
                                        selected = handicapPlayer == 1,
                                        onClick = { handicapPlayer = 1 },
                                        label = player2?.name ?: "Player 2",
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                            Column {
                                NumberStepper(
                                    label = "Points",
                                    value = handicapPoints,
                                    onValueChange = { handicapPoints = it.coerceIn(0, maxHandicap) },
                                    min = 0,
                                    max = maxHandicap,
                                    step = 5,
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}
