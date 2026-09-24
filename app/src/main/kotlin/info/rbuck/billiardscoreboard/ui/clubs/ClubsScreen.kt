package info.rbuck.billiardscoreboard.ui.clubs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil3.compose.AsyncImage
import info.rbuck.billiardscoreboard.data.Club
import info.rbuck.billiardscoreboard.ui.bsApplication
import info.rbuck.billiardscoreboard.ui.components.ConfirmDialog
import info.rbuck.billiardscoreboard.ui.components.HideStatusBarInDialog

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ClubsScreen(onBack: () -> Unit) {
    val app = bsApplication()
    val viewModel: ClubsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ClubsViewModel(app.clubRepository) }
        },
    )
    val clubs by viewModel.clubs.collectAsStateWithLifecycle()

    var showEditor by remember { mutableStateOf(false) }
    var editingClub by remember { mutableStateOf<Club?>(null) }
    var pendingDelete by remember { mutableStateOf<Club?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clubs") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("New club") },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                onClick = { editingClub = null; showEditor = true },
            )
        },
    ) { padding ->
        if (clubs.isEmpty()) {
            Row(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text("No clubs created yet. Tap + to add one.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(clubs, key = { it.id }) { club ->
                    ListItem(
                        leadingContent = { ClubCrest(club.crestPath, size = 40.dp) },
                        headlineContent = { Text(club.name) },
                        modifier = Modifier.fillMaxWidth().clickable {
                            editingClub = club; showEditor = true
                        },
                        trailingContent = {
                            IconButton(onClick = { pendingDelete = club }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete")
                            }
                        },
                    )
                }
            }
        }
    }

    if (showEditor) {
        ClubEditorDialog(
            initialName = editingClub?.name.orEmpty(),
            initialCrestPath = editingClub?.crestPath,
            onSave = { name, crestUri ->
                viewModel.createOrUpdate(editingClub?.id, name, crestUri, editingClub?.crestPath)
                showEditor = false
            },
            onDismiss = { showEditor = false },
        )
    }

    pendingDelete?.let { club ->
        ConfirmDialog(
            title = "Delete club?",
            message = "This will delete ${club.name}. Players stay, just without a club.",
            confirmText = "Delete",
            onConfirm = { viewModel.delete(club); pendingDelete = null },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
fun ClubCrest(crestPath: String?, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (crestPath != null) {
            AsyncImage(
                model = crestPath,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                Icons.Filled.Groups,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ClubEditorDialog(
    initialName: String,
    initialCrestPath: String?,
    onSave: (String, Uri?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) pickedUri = uri }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.9f).widthIn(max = 420.dp),
        title = { HideStatusBarInDialog(); Text(if (initialName.isEmpty()) "New club" else "Edit club") },
        text = {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .clickable { pickImage.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (pickedUri != null) {
                        AsyncImage(
                            model = pickedUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        ClubCrest(initialCrestPath, size = 72.dp)
                    }
                }
                Spacer(Modifier.height(4.dp))
                OutlinedButton(onClick = {
                    pickImage.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text(if (initialCrestPath == null && pickedUri == null) "Choose crest" else "Change crest") }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Club name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onSave(name, pickedUri) }, enabled = name.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
