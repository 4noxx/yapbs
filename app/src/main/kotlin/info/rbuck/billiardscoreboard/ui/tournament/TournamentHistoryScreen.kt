package info.rbuck.billiardscoreboard.ui.tournament

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import info.rbuck.billiardscoreboard.data.TournamentRecordEntity
import info.rbuck.billiardscoreboard.domain.tournament.TournamentMode
import info.rbuck.billiardscoreboard.i18n.AppLanguage
import info.rbuck.billiardscoreboard.i18n.Translations
import info.rbuck.billiardscoreboard.i18n.TournamentTextKey
import info.rbuck.billiardscoreboard.ui.bsApplication
import info.rbuck.billiardscoreboard.ui.components.ConfirmDialog
import java.text.DateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentHistoryScreen(
    onBack: () -> Unit,
    onOpenTournament: (String) -> Unit,
    onNewTournament: () -> Unit,
) {
    val app = bsApplication()
    val viewModel: TournamentHistoryViewModel = viewModel(
        factory = viewModelFactory {
            initializer { TournamentHistoryViewModel(app.tournamentRepository, app.playerRepository) }
        },
    )
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val names by viewModel.playerNames.collectAsStateWithLifecycle()
    val language by app.settingsRepository.language.collectAsStateWithLifecycle()
    fun t(key: TournamentTextKey, fallback: String) = Translations.tournamentText(key, language) ?: fallback
    var pendingDelete by remember { mutableStateOf<TournamentRecordEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t(TournamentTextKey.TOURNAMENTS_TITLE, "Tournaments")) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = onNewTournament) { Icon(Icons.Filled.Add, contentDescription = "New tournament") }
                },
            )
        },
    ) { padding ->
        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(t(TournamentTextKey.NO_TOURNAMENTS_YET, "No tournaments yet"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(entries, key = { it.id }) { entry ->
                    TournamentRow(
                        entry,
                        names,
                        language,
                        onClick = { onOpenTournament(entry.id) },
                        onDelete = { pendingDelete = entry },
                    )
                }
            }
        }
    }

    pendingDelete?.let { entry ->
        ConfirmDialog(
            title = t(TournamentTextKey.DELETE_TOURNAMENT_TITLE, "Delete tournament?"),
            message = t(TournamentTextKey.DELETE_TOURNAMENT_MESSAGE, "This will delete this tournament permanently."),
            confirmText = t(TournamentTextKey.DELETE, "Delete"),
            onConfirm = { viewModel.delete(entry); pendingDelete = null },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun TournamentRow(entry: TournamentRecordEntity, names: Map<String, String>, language: AppLanguage, onClick: () -> Unit, onDelete: () -> Unit) {
    fun t(key: TournamentTextKey, fallback: String) = Translations.tournamentText(key, language) ?: fallback
    val modeEnum = runCatching { TournamentMode.valueOf(entry.mode) }.getOrNull()
    val modeEnglishFallback = when (modeEnum) {
        TournamentMode.LOSER_STAYS -> "Loser stays"
        TournamentMode.WINNER_STAYS -> "Winner stays"
        TournamentMode.ROUND_ROBIN -> "Round robin"
        TournamentMode.SUDDEN_DEATH -> "Sudden death"
        TournamentMode.SINGLE_ELIMINATION -> "Single elimination"
        TournamentMode.PARTNER_ROTATION -> "Rotating doubles"
        null -> entry.mode
    }
    val modeLabel = modeEnum?.let { Translations.tournamentModeLabel(it, language) } ?: modeEnglishFallback
    val playerNames = entry.playerIds.split(",").filter { it.isNotBlank() }.map { names[it] ?: "?" }
    val status = if (entry.finished) {
        entry.championId?.let { t(TournamentTextKey.CHAMPION, "Champion: %s").format(names[it] ?: "?") } ?: t(TournamentTextKey.DRAW, "Draw")
    } else {
        t(TournamentTextKey.IN_PROGRESS, "In progress")
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp).weight(1f)) {
                Text(modeLabel, fontWeight = FontWeight.Bold)
                Text(playerNames.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(status, style = MaterialTheme.typography.bodySmall)
                Text(
                    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(entry.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete tournament")
            }
        }
    }
}
