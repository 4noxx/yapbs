package info.rbuck.billiardscoreboard.ui.training

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import info.rbuck.billiardscoreboard.domain.training.TrainingAttempt
import info.rbuck.billiardscoreboard.domain.training.TrainingExercise
import info.rbuck.billiardscoreboard.domain.training.TrainingMatchEngine
import info.rbuck.billiardscoreboard.i18n.Translations
import info.rbuck.billiardscoreboard.ui.bsApplication
import info.rbuck.billiardscoreboard.ui.components.AttemptGrid
import info.rbuck.billiardscoreboard.ui.components.BallRackPicker
import info.rbuck.billiardscoreboard.ui.components.BallsDialogActions
import info.rbuck.billiardscoreboard.ui.components.ConfirmDialog
import info.rbuck.billiardscoreboard.ui.components.HideStatusBarInDialog
import info.rbuck.billiardscoreboard.ui.components.NumberStepper
import info.rbuck.billiardscoreboard.ui.theme.LocalUiScale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScoreScreen(
    exercise: TrainingExercise,
    playerId: String,
    onBack: () -> Unit,
) {
    val app = bsApplication()
    val viewModel: TrainingViewModel = viewModel(
        factory = viewModelFactory {
            initializer { TrainingViewModel(exercise, playerId, app.playerRepository, app.trainingRecordRepository) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val player by viewModel.player.collectAsStateWithLifecycle()
    val language by app.settingsRepository.language.collectAsStateWithLifecycle()
    val saveTrainingToHistory by app.settingsRepository.saveTrainingToHistory.collectAsStateWithLifecycle()

    val ballsOnTable = TrainingMatchEngine.ballsOnTable(state)
    val currentRun = TrainingMatchEngine.currentRun(state)
    val completedAttempts = TrainingMatchEngine.completedAttempts(state)
    val livesRemaining = TrainingMatchEngine.livesRemaining(state)
    val stats = TrainingMatchEngine.stats(state)
    val isSessionOver = TrainingMatchEngine.isSessionOver(state)

    var showRackDialog by remember { mutableStateOf(false) }
    var showSetBallsDialog by remember { mutableStateOf(false) }
    var showEndAttemptDialog by remember { mutableStateOf(false) }
    var showResults by remember { mutableStateOf(false) }
    var showRulesDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(isSessionOver) {
        if (isSessionOver) {
            showResults = true
            // Played through to the end - the fixed-inning exercises. HighRun (open-ended) never
            // hits this and is saved from the "Finish" action instead.
            if (saveTrainingToHistory) viewModel.persistToHistory()
        }
    }

    BackHandler(enabled = state.actions.isNotEmpty()) { showDiscardConfirm = true }

    val uiScale = LocalUiScale.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exercise.displayName) },
                navigationIcon = {
                    IconButton(onClick = { if (state.actions.isEmpty()) onBack() else showDiscardConfirm = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showHistoryDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "History")
                    }
                    IconButton(onClick = { showRulesDialog = true }) {
                        Icon(Icons.Filled.Info, contentDescription = "Rules")
                    }
                    IconButton(onClick = viewModel::undo, enabled = TrainingMatchEngine.canUndo(state)) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }
                },
            )
        },
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            val landscape = maxWidth > maxHeight
            val buttonSize = 56.dp * uiScale

            val header: @Composable () -> Unit = {
                TrainingHeader(
                    playerName = player?.name ?: "Player",
                    exercise = exercise,
                    completedAttempts = completedAttempts,
                    livesRemaining = livesRemaining,
                    uiScale = uiScale,
                )
            }
            val pointsCard: @Composable (Modifier, Boolean) -> Unit = { modifier, showHeader ->
                TrainingPointsCard(
                    exercise = exercise,
                    currentRun = currentRun,
                    stats = stats,
                    completedAttempts = completedAttempts,
                    uiScale = uiScale,
                    compact = landscape,
                    modifier = modifier,
                    header = if (showHeader) header else null,
                )
            }
            val grid: @Composable (Int, Dp?) -> Unit = { perRow, tileHeight ->
                if (exercise.showsAttemptGrid) {
                    AttemptGrid(
                        attempts = completedAttempts,
                        totalCount = exercise.attemptCount ?: 0,
                        currentIndex = completedAttempts.size,
                        exercise = exercise,
                        modifier = Modifier.fillMaxWidth(),
                        perRow = perRow,
                        tileHeight = tileHeight,
                    )
                }
            }
            val buttons: @Composable (Modifier) -> Unit = { modifier ->
                TrainingButtonRow(
                    exercise = exercise,
                    ballsOnTable = ballsOnTable,
                    livesRemaining = livesRemaining,
                    buttonSize = buttonSize,
                    uiScale = uiScale,
                    onSubtract = { viewModel.addBalls(-1) },
                    onAdd = { viewModel.addBalls(1) },
                    onBallsClick = { showSetBallsDialog = true },
                    onRack = {
                        when {
                            exercise.breakballBonus -> viewModel.forceBreakballRack()
                            exercise == TrainingExercise.HIGH_RUN -> showRackDialog = true
                            else -> viewModel.setBallsOnTable(0)
                        }
                    },
                    onError = viewModel::recordError,
                    onEndAttempt = { showEndAttemptDialog = true },
                    modifier = modifier,
                )
            }

            if (landscape && exercise.showsAttemptGrid) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    pointsCard(Modifier.fillMaxWidth().weight(1f), true)
                    Spacer(Modifier.height(8.dp))
                    grid(10, buttonSize)
                    Spacer(Modifier.height(8.dp))
                    buttons(Modifier.fillMaxWidth())
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    pointsCard(Modifier.fillMaxWidth().weight(1f), true)
                    Spacer(Modifier.height(16.dp))
                    grid(5, null)
                    if (exercise.showsAttemptGrid) Spacer(Modifier.height(16.dp))
                    buttons(Modifier.fillMaxWidth())
                }
            }
        }
    }

    if (showRackDialog) {
        var remaining by remember { mutableStateOf(1) }
        AlertDialog(
            onDismissRequest = { showRackDialog = false },
            title = {
                HideStatusBarInDialog()
                Text("Re-rack?", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("How many balls are still on the table?")
                    Spacer(Modifier.height(16.dp))
                    NumberStepper(label = "", value = remaining, onValueChange = { remaining = it }, min = 0, max = 1)
                }
            },
            confirmButton = {
                BallsDialogActions(
                    primaryLabel = "Re-rack",
                    onPrimary = { viewModel.setBallsOnTable(remaining); showRackDialog = false },
                    onCancel = { showRackDialog = false },
                )
            },
        )
    }

    if (showSetBallsDialog) {
        var tempValue by remember(ballsOnTable) { mutableStateOf(ballsOnTable) }
        AlertDialog(
            onDismissRequest = { showSetBallsDialog = false },
            title = {
                HideStatusBarInDialog()
                Text("Balls on table?", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
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
                    primaryLabel = "Set",
                    onPrimary = { viewModel.setBallsOnTable(tempValue); showSetBallsDialog = false },
                    onCancel = { showSetBallsDialog = false },
                )
            },
        )
    }

    if (showEndAttemptDialog) {
        AlertDialog(
            onDismissRequest = { showEndAttemptDialog = false },
            title = {
                HideStatusBarInDialog()
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Next inning?", modifier = Modifier.weight(1f))
                    IconButton(onClick = { showEndAttemptDialog = false }) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel")
                    }
                }
            },
            text = { Text("Continue with the next inning, or finish the training session?") },
            confirmButton = {
                TextButton(onClick = { viewModel.endAttempt(); showEndAttemptDialog = false }) { Text("Next inning") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.endAttempt()
                    showEndAttemptDialog = false
                    // "Finish" is the only played-through path for the open-ended HighRun; the
                    // fixed-inning exercises save on their own once the last inning is done, and
                    // finishing one early is deliberately not counted as played through.
                    if (saveTrainingToHistory && exercise.isOpenEnded && state.actions.isNotEmpty()) {
                        viewModel.persistToHistory()
                    }
                    onBack()
                }) { Text("Finish") }
            },
        )
    }

    if (showResults) {
        AlertDialog(
            onDismissRequest = onBack,
            title = { HideStatusBarInDialog(); Text("Training finished") },
            text = {
                Column {
                    if (exercise.targetPoints != null) {
                        Text("Total: ${stats.totalPoints} / ${exercise.targetPoints}")
                        Text("Average: ${round1(stats.average)} (reference ${exercise.referenceAverage})")
                    } else {
                        Text("High run: ${stats.highRun}")
                        Text("Innings: ${stats.innings}")
                    }
                }
            },
            confirmButton = { TextButton(onClick = onBack) { Text("Done") } },
        )
    }

    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { HideStatusBarInDialog(); Text("Training history") },
            text = {
                val rows = remember(state.actions) { buildTrainingHistoryRows(state, exercise) }
                TrainingHistoryTable(rows = rows)
            },
            confirmButton = { TextButton(onClick = { showHistoryDialog = false }) { Text("Close") } },
        )
    }

    if (showRulesDialog) {
        AlertDialog(
            onDismissRequest = { showRulesDialog = false },
            title = { HideStatusBarInDialog(); Text(exercise.displayName) },
            text = { Text(Translations.trainingRule(exercise, language)) },
            confirmButton = { TextButton(onClick = { showRulesDialog = false }) { Text("Close") } },
        )
    }

    if (showDiscardConfirm) {
        ConfirmDialog(
            title = "Discard training?",
            message = "Going back now discards this training session's progress - it hasn't been saved.",
            confirmText = "Discard",
            onConfirm = { showDiscardConfirm = false; onBack() },
            onDismiss = { showDiscardConfirm = false },
        )
    }
}

private fun round1(value: Float): Float = (value * 10).roundToInt() / 10f

/** One row of the training-history table: a single inning's ball count. */
private data class TrainingHistoryRow(val inning: Int, val score: String)

/**
 * Chronological, per-inning breakdown of a training session, for the history table. Includes the
 * current (still open) inning once something has actually happened in it - matching the same
 * still-open-turn convention used by the real match scoreboards' history tables.
 */
private fun buildTrainingHistoryRows(state: info.rbuck.billiardscoreboard.domain.training.TrainingMatchState, exercise: TrainingExercise): List<TrainingHistoryRow> {
    val completed = TrainingMatchEngine.completedAttempts(state)
    val current = TrainingMatchEngine.currentAttempt(state)
    val attempts = if (current.ballCount != 0 || current.livesLost != 0) completed + current else completed
    return attempts.mapIndexed { index, attempt ->
        TrainingHistoryRow(index + 1, TrainingMatchEngine.formatBallCount(exercise, attempt.ballCount))
    }
}

@Composable
private fun TrainingHistoryTable(rows: List<TrainingHistoryRow>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
            Row(modifier = Modifier.fillMaxWidth()) {
                TrainingHistoryCell("I", modifier = Modifier.weight(1f), bold = true)
                TrainingHistoryCell("S", modifier = Modifier.weight(1f), bold = true)
            }
        }
        Column(modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
            rows.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    TrainingHistoryCell(row.inning.toString().padStart(2, '0'), modifier = Modifier.weight(1f))
                    TrainingHistoryCell(row.score, modifier = Modifier.weight(1f))
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun TrainingHistoryCell(text: String, modifier: Modifier = Modifier, bold: Boolean = false) {
    Text(
        text = text,
        modifier = modifier.padding(vertical = 6.dp),
        textAlign = TextAlign.Center,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    )
}

@Composable
private fun TrainingHeader(
    playerName: String,
    exercise: TrainingExercise,
    completedAttempts: List<info.rbuck.billiardscoreboard.domain.training.TrainingAttempt>,
    livesRemaining: Int,
    uiScale: Float,
) {
    // Same name-sizing formula as every match scoreboard's player card, so the name reads at the
    // same size whether it's on 8-Ball, 14.1, or here.
    val compactTextMultiplier = if (uiScale > 1f) 1.5f else 1f
    val baseNameStyle = MaterialTheme.typography.titleLarge
    Text(
        playerName,
        style = baseNameStyle.copy(
            fontSize = baseNameStyle.fontSize * compactTextMultiplier,
            lineHeight = baseNameStyle.lineHeight * compactTextMultiplier,
        ),
        textAlign = TextAlign.Center,
    )
    if (exercise.showsAttemptGrid) {
        val baseInningStyle = MaterialTheme.typography.bodyMedium
        Text(
            "Inning ${completedAttempts.size + 1}/${exercise.attemptCount}",
            style = baseInningStyle.copy(
                fontSize = baseInningStyle.fontSize * compactTextMultiplier,
                lineHeight = baseInningStyle.lineHeight * compactTextMultiplier,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (exercise.hasLives) {
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(exercise.livesPerAttempt) { i ->
                Icon(
                    if (i < livesRemaining) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp * uiScale),
                )
            }
        }
    }
}

@Composable
private fun TrainingPointsCard(
    exercise: TrainingExercise,
    currentRun: Int,
    stats: info.rbuck.billiardscoreboard.domain.training.TrainingStats,
    completedAttempts: List<info.rbuck.billiardscoreboard.domain.training.TrainingAttempt>,
    uiScale: Float,
    compact: Boolean,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
) {
    // Same name/score/stats sizing formula and SpaceBetween layout as every match scoreboard's
    // player card (StraightPlayerCard for 14.1, PlayerScoreCard for 8/9/10-Ball), so Training reads
    // at the same size and the stats line sits flush at the bottom instead of floating mid-card.
    val isTablet = uiScale > 1f
    val compactScoreMultiplier = when {
        isTablet -> 4f
        !compact -> 2.2f
        else -> 1f
    }
    val compactTextMultiplier = if (isTablet) 1.5f else 1f
    // Grey surfaceVariant, no border - matches the player card style used by every real match screen
    // (8-Ball/9-Ball/10-Ball, 14.1), which Training previously didn't (it was plain white and bordered).
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
            // Ceiling on top of the shared formula: in the landscape "attempt grid" layout this card
            // shares its column with a header, divider, grid and button row above/below it, so at the
            // big tablet multiplier the score text can be taller than the card actually has room for
            // and get clipped by the Surface's rounded-corner shape. On every other layout (portrait,
            // or landscape without a grid) there's ample room and this ceiling is never the binding
            // constraint, so the score still renders at the exact same size every match scoreboard uses.
            val targetScoreSp = 44f * uiScale * compactScoreMultiplier
            val reservedForStats = 28.dp
            val scoreFontSize = minOf(targetScoreSp, (maxHeight - reservedForStats).value / 1.3f).sp
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                if (header != null) header()
                Text(
                    TrainingMatchEngine.formatBallCount(exercise, currentRun),
                    fontSize = scoreFontSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                val baseStatsStyle = MaterialTheme.typography.bodyMedium
                val statsStyle = baseStatsStyle.copy(
                    fontSize = baseStatsStyle.fontSize * compactTextMultiplier,
                    lineHeight = baseStatsStyle.lineHeight * compactTextMultiplier,
                )
                if (exercise.targetPoints != null) {
                    Text(
                        "Total ${stats.totalPoints} / ${exercise.targetPoints}  ·  Ø ${round1(stats.average)} (ref ${exercise.referenceAverage})",
                        style = statsStyle,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Text(
                        "High run: ${stats.highRun}  ·  Inning: ${completedAttempts.size + 1}",
                        style = statsStyle,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun TrainingButtonRow(
    exercise: TrainingExercise,
    ballsOnTable: Int,
    livesRemaining: Int,
    buttonSize: Dp,
    uiScale: Float,
    onSubtract: () -> Unit,
    onAdd: () -> Unit,
    onBallsClick: () -> Unit,
    onRack: () -> Unit,
    onError: () -> Unit,
    onEndAttempt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        TrainingTile(onClick = onBallsClick, modifier = Modifier.weight(1f).size(buttonSize)) {
            Text(ballsOnTable.toString(), fontSize = 30.sp * uiScale, fontWeight = FontWeight.Bold)
        }
        TrainingActionButton(
            onClick = onSubtract,
            modifier = Modifier.weight(1f).size(buttonSize),
        ) { Text("−", fontSize = 38.sp * uiScale, fontWeight = FontWeight.Bold) }
        TrainingActionButton(
            onClick = onAdd,
            modifier = Modifier.weight(1f).size(buttonSize),
        ) { Text("+", fontSize = 38.sp * uiScale, fontWeight = FontWeight.Bold) }
        TrainingActionButton(
            onClick = onRack,
            modifier = Modifier.weight(1f).size(buttonSize),
        ) { RackTriangle(size = 25.dp * uiScale) }
        if (exercise.hasLives) {
            TrainingActionButton(
                onClick = onError,
                enabled = livesRemaining > 0,
                modifier = Modifier.weight(1f).size(buttonSize),
            ) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = "Miss / foul",
                    modifier = Modifier.size(35.dp * uiScale),
                )
            }
        }
        TrainingActionButton(
            onClick = onEndAttempt,
            modifier = Modifier.weight(1f).size(buttonSize),
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = "Next inning",
                modifier = Modifier.size(35.dp * uiScale),
            )
        }
    }
}

/** Filled balls-on-table tile, matching the other action buttons - tapping it opens the same "set balls
 * on table" dialog as the 14.1 match scoreboard. */
@Composable
private fun TrainingTile(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun TrainingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
    }
}

/** Six balls in a triangle rack, matching the icon used on the 14.1 match scoreboard. */
@Composable
private fun RackTriangle(size: Dp) {
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
