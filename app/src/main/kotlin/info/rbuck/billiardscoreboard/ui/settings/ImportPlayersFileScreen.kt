package info.rbuck.billiardscoreboard.ui.settings

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import info.rbuck.billiardscoreboard.ui.bsApplication

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPlayersFileScreen(uri: Uri, onBack: () -> Unit) {
    val app = bsApplication()
    val context = LocalContext.current
    val viewModel: ImportPlayersFileViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ImportPlayersFileViewModel(app.playerRepository, app.clubRepository, context.applicationContext, uri) }
        },
    )
    val candidates by viewModel.candidates.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val loadFailed by viewModel.loadFailed.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import players") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (candidates.any { !it.alreadyExists }) {
                        TextButton(onClick = viewModel::selectAllNew) { Text("Select all") }
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                loadFailed -> ErrorState()
                candidates.isEmpty() -> EmptyState()
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize().weight(1f)) {
                        items(candidates, key = { it.name + "|" + it.club }) { candidate ->
                            ListItem(
                                leadingContent = {
                                    Checkbox(
                                        checked = candidate in selected,
                                        enabled = !candidate.alreadyExists,
                                        onCheckedChange = { viewModel.toggleSelected(candidate) },
                                    )
                                },
                                headlineContent = { Text(candidate.name) },
                                supportingContent = { Text(candidate.club ?: "No club") },
                                trailingContent = if (candidate.alreadyExists) {
                                    { Text("Already added", style = MaterialTheme.typography.labelSmall) }
                                } else {
                                    null
                                },
                                modifier = Modifier.fillMaxWidth().alpha(if (candidate.alreadyExists) 0.5f else 1f),
                            )
                            HorizontalDivider()
                        }
                    }
                    Surface(shadowElevation = 8.dp) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Button(onClick = viewModel::importSelected, enabled = selected.isNotEmpty()) {
                                Text("Import (${selected.size})")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Couldn't read this file", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Make sure it's a players file exported from YAPBS.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("No players found in this file", style = MaterialTheme.typography.bodyLarge)
    }
}
