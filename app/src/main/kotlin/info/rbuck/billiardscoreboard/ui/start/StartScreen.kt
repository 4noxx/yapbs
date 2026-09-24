package info.rbuck.billiardscoreboard.ui.start

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import info.rbuck.billiardscoreboard.R
import info.rbuck.billiardscoreboard.domain.GameType
import info.rbuck.billiardscoreboard.i18n.LocalStrings
import info.rbuck.billiardscoreboard.i18n.Strings
import info.rbuck.billiardscoreboard.obs.ObsOutputState
import info.rbuck.billiardscoreboard.ui.bsApplication
import info.rbuck.billiardscoreboard.ui.theme.AppTheme
import info.rbuck.billiardscoreboard.ui.theme.LocalUiScale

/** Above this width there's room for all 4 game tiles (or all 4 "More" tiles) in a single row - typical of landscape/tablet. */
private val WideLayoutBreakpoint = 560.dp

/** Caps tile growth in the wide layout so tiles stay a sensible touch-target size instead of stretching to fill a short landscape screen's height. */
private val WideGameTileMaxSize = 130.dp
private val WideMoreTileMaxSize = 100.dp

@Composable
fun StartScreen(
    onNewSimpleMatch: (GameType) -> Unit,
    onNewStraightMatch: () -> Unit,
    onOpenPlayers: () -> Unit,
    onOpenArchive: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTraining: () -> Unit,
    onOpenTournaments: () -> Unit,
    onOpenObsControl: () -> Unit,
) {
    val tournamentEnabled by bsApplication().settingsRepository.tournamentEnabled.collectAsStateWithLifecycle()
    val obsWsEnabled by bsApplication().settingsRepository.obsWebSocketEnabled.collectAsStateWithLifecycle()
    val obsRegieTileEnabled by bsApplication().settingsRepository.obsRegieTileEnabled.collectAsStateWithLifecycle()
    val showRegieTile = obsWsEnabled && obsRegieTileEnabled
    val appTheme by bsApplication().settingsRepository.appTheme.collectAsStateWithLifecycle()
    val recordStatus by bsApplication().obsWebSocketClient.recordStatus.collectAsStateWithLifecycle()
    val streamStatus by bsApplication().obsWebSocketClient.streamStatus.collectAsStateWithLifecycle()
    val s = LocalStrings.current

    Scaffold { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            val wide = maxWidth > WideLayoutBreakpoint
            val uiScale = LocalUiScale.current
            val sectionGap = if (wide) 8.dp else 28.dp
            val rowArrangement = if (wide) Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally) else Arrangement.spacedBy(10.dp)
            fun RowScope.tileModifier(maxSize: Dp): Modifier =
                if (wide) Modifier.weight(1f, fill = false).widthIn(max = maxSize * uiScale) else Modifier.weight(1f)

            Column(modifier = Modifier.fillMaxWidth()) {
                val logoRes = when (appTheme) {
                    AppTheme.LIGHT -> R.drawable.logo_light
                    AppTheme.DARK -> R.drawable.logo_dark
                    AppTheme.VINTAGE -> R.drawable.logo_vintage
                }
                val logoPainter = painterResource(logoRes)
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Image(
                        painter = logoPainter,
                        contentDescription = "YAPBS",
                        modifier = Modifier
                            .height(40.dp * uiScale)
                            .aspectRatio(logoPainter.intrinsicSize.width / logoPainter.intrinsicSize.height),
                    )
                }
                Spacer(Modifier.height(sectionGap))

                Text(s.startNewMatch, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                if (wide) {
                    Row(horizontalArrangement = rowArrangement, modifier = Modifier.fillMaxWidth()) {
                        NumberTile("8", s.eightBall, tileModifier(WideGameTileMaxSize)) { onNewSimpleMatch(GameType.EIGHT_BALL) }
                        NumberTile("9", s.nineBall, tileModifier(WideGameTileMaxSize)) { onNewSimpleMatch(GameType.NINE_BALL) }
                        NumberTile("10", s.tenBall, tileModifier(WideGameTileMaxSize)) { onNewSimpleMatch(GameType.TEN_BALL) }
                        NumberTile("14.1", s.straightPool, tileModifier(WideGameTileMaxSize), onNewStraightMatch)
                    }
                } else {
                    Row(horizontalArrangement = rowArrangement, modifier = Modifier.fillMaxWidth()) {
                        NumberTile("8", s.eightBall, tileModifier(WideGameTileMaxSize)) { onNewSimpleMatch(GameType.EIGHT_BALL) }
                        NumberTile("9", s.nineBall, tileModifier(WideGameTileMaxSize)) { onNewSimpleMatch(GameType.NINE_BALL) }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = rowArrangement, modifier = Modifier.fillMaxWidth()) {
                        NumberTile("10", s.tenBall, tileModifier(WideGameTileMaxSize)) { onNewSimpleMatch(GameType.TEN_BALL) }
                        NumberTile("14.1", s.straightPool, tileModifier(WideGameTileMaxSize), onNewStraightMatch)
                    }
                }

                Spacer(Modifier.height(sectionGap))
                Text(s.startMore, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                if (wide) {
                    Row(horizontalArrangement = rowArrangement, modifier = Modifier.fillMaxWidth()) {
                        IconTile(Icons.Filled.Groups, s.tilePlayers, tileModifier(WideMoreTileMaxSize), onClick = onOpenPlayers)
                        IconTile(Icons.Filled.History, s.tileHistory, tileModifier(WideMoreTileMaxSize), onClick = onOpenArchive)
                        IconTile(Icons.Filled.Settings, s.tileSettings, tileModifier(WideMoreTileMaxSize), onClick = onOpenSettings)
                        IconTile(Icons.Filled.FitnessCenter, s.tileTraining, tileModifier(WideMoreTileMaxSize), onClick = onOpenTraining)
                        if (tournamentEnabled) {
                            IconTile(Icons.Filled.EmojiEvents, s.tileTournament, tileModifier(WideMoreTileMaxSize), onClick = onOpenTournaments)
                        }
                        if (showRegieTile) {
                            IconTile(Icons.Filled.Videocam, s.tileRegie, tileModifier(WideMoreTileMaxSize), onClick = onOpenObsControl)
                        }
                    }
                } else {
                    Row(horizontalArrangement = rowArrangement, modifier = Modifier.fillMaxWidth()) {
                        IconTile(Icons.Filled.Groups, s.tilePlayers, tileModifier(WideMoreTileMaxSize), onClick = onOpenPlayers)
                        IconTile(Icons.Filled.History, s.tileHistory, tileModifier(WideMoreTileMaxSize), onClick = onOpenArchive)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = rowArrangement, modifier = Modifier.fillMaxWidth()) {
                        IconTile(Icons.Filled.Settings, s.tileSettings, tileModifier(WideMoreTileMaxSize), onClick = onOpenSettings)
                        IconTile(Icons.Filled.FitnessCenter, s.tileTraining, tileModifier(WideMoreTileMaxSize), onClick = onOpenTraining)
                        if (tournamentEnabled) {
                            IconTile(Icons.Filled.EmojiEvents, s.tileTournament, tileModifier(WideMoreTileMaxSize), onClick = onOpenTournaments)
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    if (showRegieTile) {
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = rowArrangement, modifier = Modifier.fillMaxWidth()) {
                            IconTile(Icons.Filled.Videocam, s.tileRegie, tileModifier(WideMoreTileMaxSize), onClick = onOpenObsControl)
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        if (obsWsEnabled && (recordStatus.state != ObsOutputState.IDLE || streamStatus.state != ObsOutputState.IDLE)) {
            LiveIndicator(
                recording = recordStatus.state != ObsOutputState.IDLE,
                streaming = streamStatus.state != ObsOutputState.IDLE,
                s = s,
                onClick = onOpenObsControl,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            )
        }
        }
    }
}

/** Blinking REC/LIVE pill, top-right of Start - the one place you glance at to know OBS is
 * actually recording/streaming before walking away from the tablet. Tapping it opens Regie. */
@Composable
private fun LiveIndicator(recording: Boolean, streaming: Boolean, s: Strings, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "liveIndicatorBlink")
    val blinkAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "liveIndicatorAlpha",
    )
    val label = when {
        streaming && recording -> "${s.obsStateStreamingActive} · ${s.obsRegieRecordingSection}"
        streaming -> s.obsStateStreamingActive
        else -> s.obsRegieRecordingSection
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onError,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Icon(
                Icons.Filled.FiberManualRecord,
                contentDescription = null,
                modifier = Modifier.height(12.dp).alpha(blinkAlpha),
            )
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Tile(modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.aspectRatio(1f),
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
private fun NumberTile(number: String, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Tile(modifier = modifier, onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(number, fontSize = 36.sp * LocalUiScale.current, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@Composable
private fun IconTile(icon: ImageVector, label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Tile(modifier = modifier, enabled = enabled, onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.height(28.dp * LocalUiScale.current))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}
