package info.rbuck.billiardscoreboard.ui.archive

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import info.rbuck.billiardscoreboard.data.MatchRecordEntity
import info.rbuck.billiardscoreboard.data.Player
import info.rbuck.billiardscoreboard.data.TrainingRecordEntity
import info.rbuck.billiardscoreboard.domain.GameType
import info.rbuck.billiardscoreboard.i18n.LocalStrings
import info.rbuck.billiardscoreboard.domain.training.TrainingExercise
import info.rbuck.billiardscoreboard.domain.training.TrainingMatchEngine
import info.rbuck.billiardscoreboard.ui.bsApplication
import info.rbuck.billiardscoreboard.ui.components.ChoiceChip
import info.rbuck.billiardscoreboard.ui.components.ConfirmDialog
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

private enum class ArchiveTab { MATCHES, TRAINING }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    onBack: () -> Unit,
    onOpenSimpleMatch: (String) -> Unit,
    onOpenStraightMatch: (String) -> Unit,
) {
    val app = bsApplication()
    val viewModel: ArchiveViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ArchiveViewModel(app.matchRepository, app.playerRepository, app.trainingRecordRepository) }
        },
    )
    val matches by viewModel.matches.collectAsStateWithLifecycle()
    val trainingSessions by viewModel.trainingSessions.collectAsStateWithLifecycle()
    val players by viewModel.players.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<MatchRecordEntity?>(null) }
    var pendingTrainingDelete by remember { mutableStateOf<TrainingRecordEntity?>(null) }
    var openTraining by remember { mutableStateOf<TrainingRecordEntity?>(null) }
    var tab by remember { mutableStateOf(ArchiveTab.MATCHES) }
    var filter by remember { mutableStateOf("") }
    var gameTypeFilter by remember { mutableStateOf<GameType?>(null) }
    val s = LocalStrings.current

    val query = filter.trim().lowercase()
    val visibleMatches = remember(matches, players, query, gameTypeFilter) {
        matches.filter { e ->
            (gameTypeFilter == null || e.gameType == gameTypeFilter!!.name) &&
                (query.isEmpty() || matchFilterText(e, players).contains(query))
        }
    }
    val visibleTraining = remember(trainingSessions, players, query) {
        trainingSessions.filter { e -> query.isEmpty() || trainingFilterText(e, players).contains(query) }
    }

    fun open(entity: MatchRecordEntity) {
        if (entity.gameType == GameType.STRAIGHT_POOL.name) onOpenStraightMatch(entity.id) else onOpenSimpleMatch(entity.id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.historyTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            val segmentColors = SegmentedButtonDefaults.colors(
                activeContainerColor = MaterialTheme.colorScheme.primary,
                activeContentColor = MaterialTheme.colorScheme.onPrimary,
                activeBorderColor = MaterialTheme.colorScheme.primary,
                inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                inactiveBorderColor = MaterialTheme.colorScheme.outline,
            )
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                SegmentedButton(
                    selected = tab == ArchiveTab.MATCHES,
                    onClick = { tab = ArchiveTab.MATCHES },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    colors = segmentColors,
                ) { Text(s.historyTabMatches) }
                SegmentedButton(
                    selected = tab == ArchiveTab.TRAINING,
                    onClick = { tab = ArchiveTab.TRAINING },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    colors = segmentColors,
                ) { Text(s.historyTabTraining) }
            }

            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                placeholder = { Text(s.historyFilterPlaceholder) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (filter.isNotEmpty()) {
                        IconButton(onClick = { filter = "" }) { Icon(Icons.Filled.Close, contentDescription = s.close) }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )

            if (tab == ArchiveTab.MATCHES && matches.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    ChoiceChip(selected = gameTypeFilter == null, onClick = { gameTypeFilter = null }, label = s.historyFilterAllTypes)
                    GameType.entries.forEach { gt ->
                        ChoiceChip(selected = gameTypeFilter == gt, onClick = { gameTypeFilter = gt }, label = gt.displayName)
                    }
                }
            }

            when (tab) {
                ArchiveTab.MATCHES -> {
                    when {
                        matches.isEmpty() -> EmptyState(s.historyNoMatches)
                        visibleMatches.isEmpty() -> EmptyState(s.historyNoResults)
                        else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(visibleMatches, key = { it.id }) { entity ->
                                val name1 = players[entity.player1Id]?.name ?: s.genericPlayer1
                                val name2 = players[entity.player2Id]?.name ?: s.genericPlayer2
                                val gameType = GameType.entries.firstOrNull { it.name == entity.gameType }
                                val date = DateFormat.getDateInstance().format(Date(entity.createdAt))
                                ListItem(
                                    headlineContent = { Text("$name1 vs $name2  ${entity.summary}") },
                                    supportingContent = { Text("${gameType?.displayName ?: entity.gameType} · $date") },
                                    modifier = Modifier.fillMaxWidth().clickable { open(entity) },
                                    trailingContent = {
                                        Row {
                                            IconButton(onClick = {
                                                viewModel.rematch(entity) { result ->
                                                    when (result) {
                                                        is RematchResult.Simple -> onOpenSimpleMatch(result.matchId)
                                                        is RematchResult.Straight -> onOpenStraightMatch(result.matchId)
                                                    }
                                                }
                                            }) {
                                                Icon(Icons.Filled.Replay, contentDescription = s.historyRematch)
                                            }
                                            IconButton(onClick = { pendingDelete = entity }) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Delete")
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }

                ArchiveTab.TRAINING -> {
                    when {
                        trainingSessions.isEmpty() -> EmptyState(s.historyNoTraining)
                        visibleTraining.isEmpty() -> EmptyState(s.historyNoResults)
                        else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(visibleTraining, key = { it.id }) { entity ->
                                val playerName = players[entity.playerId]?.name ?: s.genericPlayer
                                val exercise = TrainingExercise.entries.firstOrNull { it.name == entity.exercise }
                                val date = DateFormat.getDateInstance().format(Date(entity.createdAt))
                                ListItem(
                                    headlineContent = { Text("$playerName  ${entity.summary}") },
                                    supportingContent = { Text("${exercise?.displayName ?: entity.exercise} · $date") },
                                    modifier = Modifier.fillMaxWidth().clickable { openTraining = entity },
                                    trailingContent = {
                                        IconButton(onClick = { pendingTrainingDelete = entity }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { entity ->
        ConfirmDialog(
            title = s.historyDeleteMatchTitle,
            message = s.historyDeleteMatchMessage,
            confirmText = s.delete,
            onConfirm = { viewModel.delete(entity); pendingDelete = null },
            onDismiss = { pendingDelete = null },
        )
    }

    pendingTrainingDelete?.let { entity ->
        ConfirmDialog(
            title = s.historyDeleteTrainingTitle,
            message = s.historyDeleteTrainingMessage,
            confirmText = s.delete,
            onConfirm = { viewModel.deleteTraining(entity); pendingTrainingDelete = null },
            onDismiss = { pendingTrainingDelete = null },
        )
    }

    openTraining?.let { entity ->
        TrainingSessionDialog(
            entity = entity,
            playerName = players[entity.playerId]?.name ?: s.genericPlayer,
            inningScores = remember(entity.id) { viewModel.inningScores(entity) },
            onDismiss = { openTraining = null },
        )
    }
}

/** Lowercased haystack a match row is matched against by the filter field: both player names, the
 * discipline and the formatted date - so "9-ball", a surname or "2026" all narrow the list. */
private fun matchFilterText(entity: MatchRecordEntity, players: Map<String, Player>): String {
    val name1 = players[entity.player1Id]?.name.orEmpty()
    val name2 = players[entity.player2Id]?.name.orEmpty()
    val gameType = GameType.entries.firstOrNull { it.name == entity.gameType }?.displayName ?: entity.gameType
    val date = DateFormat.getDateInstance().format(Date(entity.createdAt))
    return "$name1 $name2 $gameType $date".lowercase()
}

private fun trainingFilterText(entity: TrainingRecordEntity, players: Map<String, Player>): String {
    val name = players[entity.playerId]?.name.orEmpty()
    val exercise = TrainingExercise.entries.firstOrNull { it.name == entity.exercise }?.displayName ?: entity.exercise
    val date = DateFormat.getDateInstance().format(Date(entity.createdAt))
    return "$name $exercise $date".lowercase()
}

@Composable
private fun EmptyState(text: String) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(text, modifier = Modifier.padding(top = 24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrainingSessionDialog(
    entity: TrainingRecordEntity,
    playerName: String,
    inningScores: List<Int>,
    onDismiss: () -> Unit,
) {
    val s = LocalStrings.current
    val exercise = TrainingExercise.entries.firstOrNull { it.name == entity.exercise }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${exercise?.displayName ?: entity.exercise}") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(playerName, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                if (exercise?.targetPoints != null) {
                    Text("${s.historyTotal}: ${entity.totalPoints} / ${exercise.targetPoints}")
                    Text("${s.historyAverage}: ${round1(entity.average)} (${s.historyReference} ${exercise.referenceAverage})")
                } else {
                    Text("${s.historyHighRun}: ${entity.highRun}")
                    Text("${s.historyInnings}: ${entity.innings}")
                }
                if (inningScores.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Column(modifier = Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState())) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Cell(s.historyInning, Modifier.weight(1f), bold = true)
                            Cell(s.historyScore, Modifier.weight(1f), bold = true)
                        }
                        HorizontalDivider()
                        inningScores.forEachIndexed { index, score ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Cell((index + 1).toString().padStart(2, '0'), Modifier.weight(1f))
                                Cell(
                                    exercise?.let { TrainingMatchEngine.formatBallCount(it, score) } ?: score.toString(),
                                    Modifier.weight(1f),
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(s.close) } },
    )
}

@Composable
private fun Cell(text: String, modifier: Modifier = Modifier, bold: Boolean = false) {
    Text(
        text = text,
        modifier = modifier.padding(vertical = 6.dp),
        textAlign = TextAlign.Center,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    )
}

private fun round1(value: Float): Float = (value * 10).roundToInt() / 10f
