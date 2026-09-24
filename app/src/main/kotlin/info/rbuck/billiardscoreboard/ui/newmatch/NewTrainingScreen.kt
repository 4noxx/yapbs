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
import androidx.compose.material3.Text
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
import info.rbuck.billiardscoreboard.data.Player
import info.rbuck.billiardscoreboard.domain.training.TrainingExercise
import info.rbuck.billiardscoreboard.i18n.TrainingSetupTextKey
import info.rbuck.billiardscoreboard.i18n.Translations
import info.rbuck.billiardscoreboard.ui.bsApplication
import info.rbuck.billiardscoreboard.ui.components.PlayerSlotPicker
import info.rbuck.billiardscoreboard.ui.components.TopBarStartButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTrainingScreen(
    onBack: () -> Unit,
    onStart: (TrainingExercise, Player) -> Unit,
) {
    val app = bsApplication()
    val viewModel: NewMatchViewModel = viewModel(
        factory = viewModelFactory {
            initializer { NewMatchViewModel(app.playerRepository, app.matchRepository, app.clubRepository) }
        },
    )
    val players by viewModel.players.collectAsStateWithLifecycle()
    val clubs by viewModel.clubs.collectAsStateWithLifecycle()
    val language by app.settingsRepository.language.collectAsStateWithLifecycle()
    fun t(key: TrainingSetupTextKey, fallback: String) = Translations.trainingSetupText(key, language) ?: fallback

    var playerId by remember { mutableStateOf<String?>(null) }
    var selectedExercise by remember { mutableStateOf<TrainingExercise?>(null) }
    val player = players.find { it.id == playerId }
    val canStart = player != null && selectedExercise != null

    val startTraining = {
        if (player != null && selectedExercise != null) onStart(selectedExercise!!, player)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Training") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    TopBarStartButton(
                        label = "Start",
                        enabled = canStart,
                        onClick = startTraining,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                },
            )
        },
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        ) {
            val wide = maxWidth > 560.dp
            val exerciseList = @Composable { modifier: Modifier ->
                Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TrainingExercise.entries.forEach { exercise ->
                        ExerciseOptionCard(
                            exercise = exercise,
                            selected = selectedExercise == exercise,
                            onClick = { selectedExercise = exercise },
                            t = { key, fallback -> t(key, fallback) },
                        )
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
                        Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(16.dp)) {
                            PlayerSlotPicker(
                                label = t(TrainingSetupTextKey.PLAYER, "Player"),
                                selected = player,
                                players = players,
                                clubs = clubs,
                                onSelect = { playerId = it.id },
                                onCreate = { name, cb -> viewModel.createPlayer(name, cb) },
                            )
                        }
                        androidx.compose.material3.VerticalDivider()
                        exerciseList(Modifier.weight(1f).fillMaxHeight().padding(16.dp).verticalScroll(rememberScrollState()))
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    PlayerSlotPicker(
                        label = t(TrainingSetupTextKey.PLAYER, "Player"),
                        selected = player,
                        players = players,
                        clubs = clubs,
                        onSelect = { playerId = it.id },
                        onCreate = { name, cb -> viewModel.createPlayer(name, cb) },
                    )
                    Spacer(Modifier.height(14.dp))
                    exerciseList(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun ExerciseOptionCard(
    exercise: TrainingExercise,
    selected: Boolean,
    onClick: () -> Unit,
    t: (TrainingSetupTextKey, String) -> String,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(exercise.displayName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            val detail = if (exercise.isOpenEnded) {
                t(TrainingSetupTextKey.OPEN_ENDED, "Open-ended · no inning limit")
            } else {
                t(TrainingSetupTextKey.INNINGS_TARGET, "%d innings · target %d pts")
                    .format(exercise.attemptCount, exercise.targetPoints) +
                    (exercise.referenceAverage?.let { t(TrainingSetupTextKey.REF_SUFFIX, " · ref Ø %s").format(it) } ?: "")
            }
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Start)
        }
    }
}
