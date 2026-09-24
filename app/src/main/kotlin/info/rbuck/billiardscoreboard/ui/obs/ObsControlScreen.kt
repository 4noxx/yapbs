package info.rbuck.billiardscoreboard.ui.obs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import info.rbuck.billiardscoreboard.i18n.LocalStrings
import info.rbuck.billiardscoreboard.obs.ObsOutputState
import info.rbuck.billiardscoreboard.obs.ObsOutputStatus
import info.rbuck.billiardscoreboard.obs.ObsWebSocketStatus
import info.rbuck.billiardscoreboard.obs.WakeOnLan
import info.rbuck.billiardscoreboard.ui.bsApplication
import info.rbuck.billiardscoreboard.ui.components.ConfirmDialog
import info.rbuck.billiardscoreboard.ui.components.SettingsCard
import kotlinx.coroutines.launch

/**
 * Start/stop/pause OBS's Record and Stream outputs from the tablet - the app-side counterpart to
 * the manual buttons in OBS's own "Steuerung" dock. Reuses the same [info.rbuck.billiardscoreboard.obs.ObsWebSocketClient]
 * connection the scoreboard push already uses; see Settings > OBS WebSocket for the connection and
 * pause-scene settings this screen depends on.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObsControlScreen(onBack: () -> Unit) {
    val app = bsApplication()
    val client = app.obsWebSocketClient
    val s = LocalStrings.current

    val connectionStatus by client.status.collectAsStateWithLifecycle()
    val recordStatus by client.recordStatus.collectAsStateWithLifecycle()
    val streamStatus by client.streamStatus.collectAsStateWithLifecycle()
    val streamPaused by client.streamPaused.collectAsStateWithLifecycle()
    val pauseSceneName by app.settingsRepository.obsPauseSceneName.collectAsStateWithLifecycle()
    val wakeOnLanMac by app.settingsRepository.obsWakeOnLanMac.collectAsStateWithLifecycle()
    val cameraSourceName by app.settingsRepository.obsCameraSourceName.collectAsStateWithLifecycle()

    val connected = connectionStatus == ObsWebSocketStatus.CONNECTED
    var pendingStopRecord by remember { mutableStateOf(false) }
    var pendingStopStream by remember { mutableStateOf(false) }
    var wakeOnLanMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.obsRegieTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back) }
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
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (!connected) {
                Text(s.obsNotConnected, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                if (wakeOnLanMac.isNotBlank()) {
                    OutlinedButton(onClick = {
                        coroutineScope.launch {
                            val sent = WakeOnLan.sendMagicPacket(wakeOnLanMac)
                            wakeOnLanMessage = if (sent) s.obsWakePcSent else s.obsWakePcInvalidMac
                        }
                    }) { Text(s.obsWakePc) }
                    wakeOnLanMessage?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (connected && cameraSourceName.isNotBlank()) {
                OutlinedButton(onClick = client::restartCameraSource, modifier = Modifier.fillMaxWidth()) {
                    Text(s.obsReloadCamera)
                }
            }

            SettingsCard {
                Text(s.obsRegieRecordingSection, style = MaterialTheme.typography.titleMedium)
                OutputStatusRow(status = recordStatus, activeLabel = s.obsStateRecordingActive, s = s)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    when (recordStatus.state) {
                        ObsOutputState.IDLE -> Button(
                            onClick = client::startRecord,
                            enabled = connected,
                            modifier = Modifier.weight(1f),
                        ) { Text(s.obsStartRecording) }

                        ObsOutputState.ACTIVE -> {
                            OutlinedButton(onClick = client::pauseRecord, enabled = connected, modifier = Modifier.weight(1f)) {
                                Text(s.obsPauseAction)
                            }
                            Button(
                                onClick = { pendingStopRecord = true },
                                enabled = connected,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f),
                            ) { Text(s.obsStopRecording) }
                        }

                        ObsOutputState.PAUSED -> {
                            OutlinedButton(onClick = client::resumeRecord, enabled = connected, modifier = Modifier.weight(1f)) {
                                Text(s.obsResumeAction)
                            }
                            Button(
                                onClick = { pendingStopRecord = true },
                                enabled = connected,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f),
                            ) { Text(s.obsStopRecording) }
                        }
                    }
                }
            }

            SettingsCard {
                Text(s.obsRegieStreamingSection, style = MaterialTheme.typography.titleMedium)
                OutputStatusRow(status = streamStatus, activeLabel = s.obsStateStreamingActive, streamPaused = streamPaused, s = s)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    if (streamStatus.state == ObsOutputState.IDLE) {
                        Button(onClick = client::startStream, enabled = connected, modifier = Modifier.weight(1f)) {
                            Text(s.obsStartStream)
                        }
                    } else {
                        if (pauseSceneName.isNotBlank()) {
                            OutlinedButton(
                                onClick = { if (streamPaused) client.resumeStream() else client.pauseStream(pauseSceneName) },
                                enabled = connected,
                                modifier = Modifier.weight(1f),
                            ) { Text(if (streamPaused) s.obsResumeAction else s.obsPauseAction) }
                        }
                        Button(
                            onClick = { pendingStopStream = true },
                            enabled = connected,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f),
                        ) { Text(s.obsStopStream) }
                    }
                }
                if (streamStatus.state != ObsOutputState.IDLE && pauseSceneName.isBlank()) {
                    Text(s.obsPauseSceneMissing, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (pendingStopRecord) {
        ConfirmDialog(
            title = s.obsStopRecordingConfirmTitle,
            message = s.obsStopRecordingConfirmMessage,
            confirmText = s.obsStopRecording,
            onConfirm = { pendingStopRecord = false; client.stopRecord() },
            onDismiss = { pendingStopRecord = false },
        )
    }

    if (pendingStopStream) {
        ConfirmDialog(
            title = s.obsStopStreamConfirmTitle,
            message = s.obsStopStreamConfirmMessage,
            confirmText = s.obsStopStream,
            onConfirm = { pendingStopStream = false; client.stopStream() },
            onDismiss = { pendingStopStream = false },
        )
    }
}

@Composable
private fun OutputStatusRow(
    status: ObsOutputStatus,
    activeLabel: String,
    s: info.rbuck.billiardscoreboard.i18n.Strings,
    streamPaused: Boolean = false,
) {
    val label = when {
        streamPaused && status.state == ObsOutputState.ACTIVE -> s.obsStatePaused
        status.state == ObsOutputState.PAUSED -> s.obsStatePaused
        status.state == ObsOutputState.ACTIVE -> activeLabel
        else -> s.obsStateIdle
    }
    val color = when {
        status.state == ObsOutputState.ACTIVE && !streamPaused -> MaterialTheme.colorScheme.error
        status.state == ObsOutputState.PAUSED || streamPaused -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = color)
        if (status.timecode != null && status.state != ObsOutputState.IDLE) {
            Spacer(Modifier.height(0.dp))
            Text(
                "  ${status.timecode.substringBefore('.')}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
