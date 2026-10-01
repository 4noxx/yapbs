package info.rbuck.billiardscoreboard.ui.newmatch

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import info.rbuck.billiardscoreboard.domain.GameType
import info.rbuck.billiardscoreboard.domain.MatchStatePayload
import info.rbuck.billiardscoreboard.i18n.LocalStrings
import info.rbuck.billiardscoreboard.ui.bsApplication
import info.rbuck.billiardscoreboard.ui.headtohead.HeadToHeadInline
import info.rbuck.billiardscoreboard.ui.components.ChoiceChip
import info.rbuck.billiardscoreboard.ui.components.MatchSetupHeaderTitle
import info.rbuck.billiardscoreboard.ui.components.NumberStepper
import info.rbuck.billiardscoreboard.ui.components.PlayerSlotPicker
import info.rbuck.billiardscoreboard.ui.components.SettingsCard
import info.rbuck.billiardscoreboard.ui.components.SettingsLabel
import info.rbuck.billiardscoreboard.ui.components.TopBarStartButton

/**
 * One Pocket's setup screen - simpler than 14.1's (no innings limit, no handicap) since the
 * official rules have neither: just the two players, a race-to target (8 balls into your own
 * pocket, per BCA rules) and who breaks first.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewOnePocketMatchScreen(
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
    val s = LocalStrings.current

    var player1Id by rememberSaveable { mutableStateOf<String?>(null) }
    var player2Id by rememberSaveable { mutableStateOf<String?>(null) }
    val player1 = players.find { it.id == player1Id }
    val player2 = players.find { it.id == player2Id }
    var raceTo by rememberSaveable { mutableStateOf(app.settingsRepository.defaultRaceOnePocket.value) }
    var firstPlayer by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(rematchOfMatchId) {
        val id = rematchOfMatchId ?: return@LaunchedEffect
        val previous = (app.matchRepository.loadPayload(id) as? MatchStatePayload.OnePocket)?.state ?: return@LaunchedEffect
        player1Id = previous.playerIds.getOrNull(0)
        player2Id = previous.playerIds.getOrNull(1)
        raceTo = previous.settings.raceTo
        firstPlayer = previous.settings.firstBreakPlayer
    }

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
        viewModel.createOnePocketMatch(
            player1 = player1!!,
            player2 = player2!!,
            raceTo = raceTo,
            firstBreakPlayer = firstPlayer,
            onCreated = onMatchCreated,
        )
    }

    val settingsContent: @Composable () -> Unit = {
        Column {
            SettingsLabel(s.raceToPoints)
            NumberStepper(label = "", value = raceTo, onValueChange = { raceTo = it.coerceAtLeast(1) }, min = 1)
        }
        Column {
            SettingsLabel(s.openingBreak)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ChoiceChip(
                    selected = firstPlayer == 0,
                    onClick = { firstPlayer = 0 },
                    label = player1?.name ?: "${s.player} 1",
                    modifier = Modifier.weight(1f),
                )
                ChoiceChip(
                    selected = firstPlayer == 1,
                    onClick = { firstPlayer = 1 },
                    label = player2?.name ?: "${s.player} 2",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { MatchSetupHeaderTitle(gameType = GameType.ONE_POCKET, onGameTypeSelected = onGameTypeSelected) },
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
                                label = "${s.player} 1",
                                selected = player1,
                                players = players.filter { it.id != player2?.id },
                                clubs = clubs,
                                onSelect = { player1Id = it.id },
                                onCreate = { name, cb -> viewModel.createPlayer(name, cb) },
                            )
                            Spacer(Modifier.height(12.dp))
                            PlayerSlotPicker(
                                label = "${s.player} 2",
                                selected = player2,
                                players = players.filter { it.id != player1?.id },
                                clubs = clubs,
                                onSelect = { player2Id = it.id },
                                onCreate = { name, cb -> viewModel.createPlayer(name, cb) },
                            )
                            headToHeadLine()
                        }
                        Column(
                            modifier = Modifier.weight(1f).fillMaxHeight().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(22.dp),
                        ) {
                            settingsContent()
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    PlayerSlotPicker(
                        label = "${s.player} 1",
                        selected = player1,
                        players = players.filter { it.id != player2?.id },
                        clubs = clubs,
                        onSelect = { player1Id = it.id },
                        onCreate = { name, cb -> viewModel.createPlayer(name, cb) },
                    )
                    Spacer(Modifier.height(8.dp))
                    PlayerSlotPicker(
                        label = "${s.player} 2",
                        selected = player2,
                        players = players.filter { it.id != player1?.id },
                        clubs = clubs,
                        onSelect = { player2Id = it.id },
                        onCreate = { name, cb -> viewModel.createPlayer(name, cb) },
                    )
                    headToHeadLine()

                    Spacer(Modifier.height(20.dp))
                    SettingsCard { settingsContent() }
                }
            }
        }
    }
}
