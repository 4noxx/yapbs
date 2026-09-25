package info.rbuck.billiardscoreboard.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import info.rbuck.billiardscoreboard.data.PlayerExportEntry
import info.rbuck.billiardscoreboard.data.writePlayerExportFile
import info.rbuck.billiardscoreboard.i18n.AppLanguage
import info.rbuck.billiardscoreboard.i18n.LocalStrings
import info.rbuck.billiardscoreboard.i18n.Translations
import info.rbuck.billiardscoreboard.obs.ObsWebSocketStatus
import info.rbuck.billiardscoreboard.ui.bsApplication
import info.rbuck.billiardscoreboard.ui.components.ChoiceChip
import info.rbuck.billiardscoreboard.ui.components.ConfirmDialog
import info.rbuck.billiardscoreboard.ui.components.HideStatusBarInDialog
import info.rbuck.billiardscoreboard.ui.components.NumberStepper
import info.rbuck.billiardscoreboard.ui.components.SettingsCard
import info.rbuck.billiardscoreboard.ui.theme.AppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenImportPlayersFile: (Uri) -> Unit) {
    val app = bsApplication()
    val settings = app.settingsRepository
    val s = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val openDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onOpenImportPlayersFile)
    }
    val appTheme by settings.appTheme.collectAsStateWithLifecycle()
    val tournamentEnabled by settings.tournamentEnabled.collectAsStateWithLifecycle()
    val saveTrainingToHistory by settings.saveTrainingToHistory.collectAsStateWithLifecycle()
    val historyRetentionDays by settings.historyRetentionDays.collectAsStateWithLifecycle()
    val keepScreenOn by settings.keepScreenOn.collectAsStateWithLifecycle()
    val obsWsEnabled by settings.obsWebSocketEnabled.collectAsStateWithLifecycle()
    val obsWsHost by settings.obsWebSocketHost.collectAsStateWithLifecycle()
    val obsWsPort by settings.obsWebSocketPort.collectAsStateWithLifecycle()
    val obsWsPassword by settings.obsWebSocketPassword.collectAsStateWithLifecycle()
    val obsWsSourcePrefix by settings.obsWebSocketSourcePrefix.collectAsStateWithLifecycle()
    val obsRegieTileEnabled by settings.obsRegieTileEnabled.collectAsStateWithLifecycle()
    val obsWakeOnLanMac by settings.obsWakeOnLanMac.collectAsStateWithLifecycle()
    val obsCameraSourceName by settings.obsCameraSourceName.collectAsStateWithLifecycle()
    val obsPauseSceneName by settings.obsPauseSceneName.collectAsStateWithLifecycle()
    val obsAutoHideSourceNames by settings.obsAutoHideSourceNames.collectAsStateWithLifecycle()
    val obsStraightOnlySourceNames by settings.obsStraightPoolOnlySourceNames.collectAsStateWithLifecycle()
    val obsWsStatus by app.obsWebSocketClient.status.collectAsStateWithLifecycle()
    val obsWsErrorDetail by app.obsWebSocketClient.lastErrorDetail.collectAsStateWithLifecycle()
    val raceTo8Ball by settings.defaultRace8Ball.collectAsStateWithLifecycle()
    val raceTo9Ball by settings.defaultRace9Ball.collectAsStateWithLifecycle()
    val raceTo10Ball by settings.defaultRace10Ball.collectAsStateWithLifecycle()
    val raceToStraightPool by settings.defaultRaceStraightPool.collectAsStateWithLifecycle()
    val defaultClubId by settings.defaultClubId.collectAsStateWithLifecycle()
    val clubs by app.clubRepository.observeClubs().collectAsStateWithLifecycle(initialValue = emptyList())
    val language by settings.language.collectAsStateWithLifecycle()

    var showClubPicker by remember { mutableStateOf(false) }
    var showClearHistoryConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.settingsTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column {
                SettingsSectionTitle(s.settingsSectionRaceTargets)
                Spacer(Modifier.height(8.dp))
                SettingsCard {
                    RaceTargetRow(label = s.settingsRace8Ball, value = raceTo8Ball, onValueChange = settings::setDefaultRace8Ball)
                    HorizontalDivider()
                    RaceTargetRow(label = s.settingsRace9Ball, value = raceTo9Ball, onValueChange = settings::setDefaultRace9Ball)
                    HorizontalDivider()
                    RaceTargetRow(label = s.settingsRace10Ball, value = raceTo10Ball, onValueChange = settings::setDefaultRace10Ball)
                    HorizontalDivider()
                    RaceTargetRow(label = s.settingsRaceStraightPool, value = raceToStraightPool, onValueChange = settings::setDefaultRaceStraightPool)
                }
            }

            Column {
                SettingsSectionTitle(s.settingsSectionPlayerSelection)
                Spacer(Modifier.height(8.dp))
                SettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(s.settingsDefaultClub, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                clubs.firstOrNull { it.id == defaultClubId }?.name ?: s.settingsAllClubs,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { showClubPicker = true }) { Text(s.change) }
                    }
                }
            }

            Column {
                SettingsSectionTitle(s.settingsSectionTournament)
                Spacer(Modifier.height(8.dp))
                SettingsCard {
                    SettingRow(label = s.settingsTournamentMode, checked = tournamentEnabled, onCheckedChange = settings::setTournamentEnabled)
                    Text(
                        s.settingsTournamentModeDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column {
                SettingsSectionTitle(s.settingsSectionTraining)
                Spacer(Modifier.height(8.dp))
                SettingsCard {
                    SettingRow(
                        label = s.settingsSaveTrainingToHistory,
                        checked = saveTrainingToHistory,
                        onCheckedChange = settings::setSaveTrainingToHistory,
                    )
                    Text(
                        s.settingsSaveTrainingToHistoryDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column {
                SettingsSectionTitle(s.settingsSectionDataPrivacy)
                Spacer(Modifier.height(8.dp))
                SettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(s.settingsRetention, style = MaterialTheme.typography.bodyLarge)
                        NumberStepper(
                            label = "",
                            value = historyRetentionDays,
                            onValueChange = settings::setHistoryRetentionDays,
                            min = 0,
                            max = 3650,
                            step = 30,
                            formatValue = { if (it == 0) "∞" else it.toString() },
                        )
                    }
                    Text(
                        s.settingsRetentionDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(s.settingsClearHistory, style = MaterialTheme.typography.bodyLarge)
                        TextButton(onClick = { showClearHistoryConfirm = true }) { Text(s.settingsClearHistoryButton) }
                    }
                    HorizontalDivider()
                    Text(
                        s.settingsPrivacyNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column {
                SettingsSectionTitle(s.settingsSectionDisplay)
                Spacer(Modifier.height(8.dp))
                SettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(s.settingsTheme, style = MaterialTheme.typography.bodyLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppTheme.entries.forEach { entry ->
                                ChoiceChip(
                                    selected = appTheme == entry,
                                    onClick = { settings.setAppTheme(entry) },
                                    label = entry.label,
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                    SettingRow(label = s.settingsKeepScreenOn, checked = keepScreenOn, onCheckedChange = settings::setKeepScreenOn)
                }
            }

            Column {
                SettingsSectionTitle(s.settingsSectionLanguage)
                Spacer(Modifier.height(8.dp))
                SettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(s.settingsLanguageRow, style = MaterialTheme.typography.bodyLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppLanguage.entries.forEach { entry ->
                                ChoiceChip(
                                    selected = language == entry,
                                    onClick = { settings.setLanguage(entry) },
                                    label = entry.code,
                                )
                            }
                        }
                    }
                }
            }

            Column {
                SettingsSectionTitle(s.settingsSectionExportImport)
                Spacer(Modifier.height(8.dp))
                SettingsCard {
                    // No network involved - this writes a file and hands it to Android's own Share sheet
                    // (Nearby Share, Bluetooth, email, cable, cloud drive, ...) on export, and reads a file
                    // the user picks via the system document picker on import. The app never opens a
                    // network connection for this, unlike the LAN-based sync this replaced.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(s.settingsExportPlayers, style = MaterialTheme.typography.bodyLarge)
                        TextButton(onClick = {
                            scope.launch {
                                val entries = app.playerRepository.observeActivePlayers().first().map { player ->
                                    val clubName = player.clubId?.let { id -> clubs.firstOrNull { it.id == id }?.name }
                                    PlayerExportEntry(name = player.name, club = clubName)
                                }
                                val uri = writePlayerExportFile(context, entries)
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, s.settingsSharePlayers))
                            }
                        }) { Text(s.settingsExport) }
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(s.settingsImportPlayers, style = MaterialTheme.typography.bodyLarge)
                        TextButton(onClick = { openDocumentLauncher.launch(arrayOf("*/*")) }) { Text(s.settingsImport) }
                    }
                }
            }

            Column {
                SettingsSectionTitle("OBS WebSocket")
                Spacer(Modifier.height(8.dp))
                SettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("OBS WebSocket", style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = obsWsEnabled, onCheckedChange = settings::setObsWebSocketEnabled)
                    }
                    AnimatedVisibility(visible = obsWsEnabled) {
                        Column {
                            SettingRow(
                                label = s.settingsObsRegieTile,
                                checked = obsRegieTileEnabled,
                                onCheckedChange = settings::setObsRegieTileEnabled,
                            )
                            HorizontalDivider()
                            Spacer(Modifier.height(8.dp))
                            // Connects out to OBS's own built-in obs-websocket server and pushes live-score values
                            // into named text sources - see docs/obs-scripts/ANLEITUNG.md for OBS-side setup.
                            Text(
                                "Verbindet sich mit OBS' eingebautem WebSocket-Server (Tools → WebSocket " +
                                    "Server Settings) und schreibt Spielstand-Werte direkt in dort angelegte " +
                                    "Textquellen. Setup-Anleitung: docs/obs-scripts/ANLEITUNG.md.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            // Consent reminder: this feature publishes player names/clubs to the LAN, normally so they
                            // can be pulled into a public stream. The club running the app is the data controller for
                            // that, so the app's job is to make the implication visible at the moment it's switched on.
                            Text(
                                Translations.obsWebSocketPrivacyNotice(language)
                                    ?: "While active, player names and clubs are served to your local network - " +
                                    "usually to be shown in a stream. Make sure the players shown agree to this.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(12.dp))

                            var hostText by remember(obsWsHost) { mutableStateOf(obsWsHost) }
                            OutlinedTextField(
                                value = hostText,
                                onValueChange = { hostText = it; settings.setObsWebSocketHost(it.trim()) },
                                label = { Text("OBS-Rechner IP-Adresse") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(12.dp))

                            var portText by remember(obsWsPort) { mutableStateOf(obsWsPort.toString()) }
                            var passwordText by remember(obsWsPassword) { mutableStateOf(obsWsPassword) }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Port")
                                Spacer(Modifier.width(12.dp))
                                OutlinedTextField(
                                    value = portText,
                                    onValueChange = { text ->
                                        portText = text.filter(Char::isDigit).take(5)
                                        portText.toIntOrNull()?.takeIf { it in 1..65535 }?.let(settings::setObsWebSocketPort)
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.width(120.dp),
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = passwordText,
                                onValueChange = { passwordText = it; settings.setObsWebSocketPassword(it) },
                                label = { Text("Passwort") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(12.dp))

                            var wakeOnLanMacText by remember(obsWakeOnLanMac) { mutableStateOf(obsWakeOnLanMac) }
                            OutlinedTextField(
                                value = wakeOnLanMacText,
                                onValueChange = {
                                    // Auto-format as the user types: hex only, uppercased, ":" after
                                    // every 2 chars, capped at 6 octets. Cursor jumps to the end on
                                    // each keystroke (fine for a left-to-right MAC field).
                                    val formatted = formatMacInput(it)
                                    wakeOnLanMacText = formatted
                                    settings.setObsWakeOnLanMac(formatted)
                                },
                                label = { Text(s.settingsObsWakeOnLanMac) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                s.settingsObsWakeOnLanMacDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(12.dp))

                            var cameraSourceText by remember(obsCameraSourceName) { mutableStateOf(obsCameraSourceName) }
                            OutlinedTextField(
                                value = cameraSourceText,
                                onValueChange = { cameraSourceText = it; settings.setObsCameraSourceName(it.trim()) },
                                label = { Text(s.settingsObsCameraSource) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                s.settingsObsCameraSourceDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(12.dp))

                            var sourcePrefixText by remember(obsWsSourcePrefix) { mutableStateOf(obsWsSourcePrefix) }
                            OutlinedTextField(
                                value = sourcePrefixText,
                                onValueChange = { sourcePrefixText = it; settings.setObsWebSocketSourcePrefix(it.trim()) },
                                label = { Text("Tisch-Präfix (optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                Translations.obsWebSocketSourcePrefixHint(language)
                                    ?: "Only needed if several tablets write into the same OBS instance (e.g. " +
                                    "several tables in one scene). Each tablet then needs its own value (e.g. " +
                                    "»table1«, »table2«) and OBS needs its own set of text sources with that " +
                                    "prefix (e.g. »table1_name1« instead of »name1«). Leave empty for a single table.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(12.dp))

                            var pauseSceneText by remember(obsPauseSceneName) { mutableStateOf(obsPauseSceneName) }
                            OutlinedTextField(
                                value = pauseSceneText,
                                onValueChange = { pauseSceneText = it; settings.setObsPauseSceneName(it.trim()) },
                                label = { Text(s.settingsObsPauseScene) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                s.settingsObsPauseSceneDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(12.dp))

                            var autoHideText by remember(obsAutoHideSourceNames) { mutableStateOf(obsAutoHideSourceNames) }
                            OutlinedTextField(
                                value = autoHideText,
                                onValueChange = { autoHideText = it; settings.setObsAutoHideSourceNames(it) },
                                label = { Text(s.settingsObsAutoHide) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                s.settingsObsAutoHideDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(12.dp))

                            var straightOnlyText by remember(obsStraightOnlySourceNames) { mutableStateOf(obsStraightOnlySourceNames) }
                            OutlinedTextField(
                                value = straightOnlyText,
                                onValueChange = { straightOnlyText = it; settings.setObsStraightPoolOnlySourceNames(it) },
                                label = { Text(s.settingsObsStraightOnly) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                s.settingsObsStraightOnlyDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(12.dp))

                            val (statusLabel, statusColor) = when (obsWsStatus) {
                                ObsWebSocketStatus.CONNECTED -> "Verbunden" to MaterialTheme.colorScheme.primary
                                ObsWebSocketStatus.CONNECTING -> "Verbinde…" to MaterialTheme.colorScheme.onSurfaceVariant
                                ObsWebSocketStatus.AUTH_FAILED -> "Falsches Passwort" to MaterialTheme.colorScheme.error
                                ObsWebSocketStatus.ERROR -> "Verbindung fehlgeschlagen" to MaterialTheme.colorScheme.error
                                ObsWebSocketStatus.DISCONNECTED -> "Nicht verbunden" to MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Text(statusLabel, style = MaterialTheme.typography.bodyMedium, color = statusColor)
                            if (obsWsStatus == ObsWebSocketStatus.ERROR && obsWsErrorDetail != null) {
                                Text(
                                    obsWsErrorDetail!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearHistoryConfirm) {
        ConfirmDialog(
            title = s.settingsClearHistoryConfirmTitle,
            message = s.settingsClearHistoryConfirmMessage,
            confirmText = s.settingsClearHistoryButton,
            onConfirm = {
                showClearHistoryConfirm = false
                scope.launch {
                    app.matchRepository.clearHistory()
                    app.tournamentRepository.clearHistory()
                    app.trainingRecordRepository.clearHistory()
                }
            },
            onDismiss = { showClearHistoryConfirm = false },
        )
    }

    if (showClubPicker) {
        AlertDialog(
            onDismissRequest = { showClubPicker = false },
            title = { HideStatusBarInDialog(); Text(s.settingsDefaultClub) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    item {
                        ClubPickerRow(
                            name = s.settingsAllClubs,
                            selected = defaultClubId == null,
                            onClick = { settings.setDefaultClubId(null); showClubPicker = false },
                        )
                    }
                    items(clubs) { club ->
                        ClubPickerRow(
                            name = club.name,
                            selected = defaultClubId == club.id,
                            onClick = { settings.setDefaultClubId(club.id); showClubPicker = false },
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showClubPicker = false }) { Text(s.close) } },
        )
    }
}

@Composable
private fun ClubPickerRow(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 4.dp))
    }
}

/** "aabbccddeeff" / "aa-bb-.." / partial input -> "AA:BB:CC:DD:EE:FF", live as the user types. */
private fun formatMacInput(raw: String): String =
    raw.filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
        .uppercase()
        .take(12)
        .chunked(2)
        .joinToString(":")

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun RaceTargetRow(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        NumberStepper(label = "", value = value, onValueChange = onValueChange, min = 1)
    }
}
