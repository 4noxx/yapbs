package info.rbuck.billiardscoreboard.ui.start

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
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
import kotlin.math.hypot

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
    val isFelt = appTheme == AppTheme.FELT
    // Felt and Light both get the gradient/glow tile treatment - Dark and Vintage keep the plain
    // flat-filled tile. Layout differences that are specifically Felt's own (no section headers,
    // the wider title gap, the plain-text wordmark) stay gated on [isFelt] alone, further below.
    val useGradientTiles = isFelt || appTheme == AppTheme.LIGHT
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
            // Felt's own, more generous breathing room between the title and the two tile rows -
            // its rows have no "Neues Spiel"/"Mehr" label to separate them from the title anymore,
            // so the gap itself needs to read as the section break.
            val sectionGap = if (isFelt) 40.dp else if (wide) 8.dp else 28.dp
            val rowArrangement = if (wide) Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally) else Arrangement.spacedBy(10.dp)
            fun RowScope.tileModifier(maxSize: Dp): Modifier =
                if (wide) Modifier.weight(1f, fill = false).widthIn(max = maxSize * uiScale) else Modifier.weight(1f)

            Column(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (appTheme == AppTheme.FELT) {
                        // Felt has no bespoke logo artwork (the other 3 themes each recolor the same
                        // comic-style wordmark PNG) - a chunky arcade wordmark would clash with Felt's
                        // clean, modern design, so it gets a plain text wordmark instead.
                        FeltWordmark(uiScale)
                    } else {
                        val logoRes = when (appTheme) {
                            AppTheme.LIGHT -> R.drawable.logo_light
                            AppTheme.DARK -> R.drawable.logo_dark
                            AppTheme.VINTAGE -> R.drawable.logo_vintage
                            else -> R.drawable.logo_dark
                        }
                        val logoPainter = painterResource(logoRes)
                        Image(
                            painter = logoPainter,
                            contentDescription = "YAPBS",
                            modifier = Modifier
                                .height(40.dp * uiScale)
                                .aspectRatio(logoPainter.intrinsicSize.width / logoPainter.intrinsicSize.height),
                        )
                    }
                }
                Spacer(Modifier.height(sectionGap))

                if (!isFelt) {
                    Text(s.startNewMatch, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                }
                if (wide) {
                    Row(horizontalArrangement = rowArrangement, modifier = Modifier.fillMaxWidth()) {
                        GameTile("8", s.eightBall, tileModifier(WideGameTileMaxSize), useGradientTiles, twoToneAccent = isFelt) { onNewSimpleMatch(GameType.EIGHT_BALL) }
                        GameTile("9", s.nineBall, tileModifier(WideGameTileMaxSize), useGradientTiles, twoToneAccent = isFelt) { onNewSimpleMatch(GameType.NINE_BALL) }
                        GameTile("10", s.tenBall, tileModifier(WideGameTileMaxSize), useGradientTiles, twoToneAccent = isFelt) { onNewSimpleMatch(GameType.TEN_BALL) }
                        GameTile("14.1", s.straightPool, tileModifier(WideGameTileMaxSize), useGradientTiles, twoToneAccent = isFelt, copper = true, onClick = onNewStraightMatch)
                    }
                } else {
                    Row(horizontalArrangement = rowArrangement, modifier = Modifier.fillMaxWidth()) {
                        GameTile("8", s.eightBall, tileModifier(WideGameTileMaxSize), useGradientTiles, twoToneAccent = isFelt) { onNewSimpleMatch(GameType.EIGHT_BALL) }
                        GameTile("9", s.nineBall, tileModifier(WideGameTileMaxSize), useGradientTiles, twoToneAccent = isFelt) { onNewSimpleMatch(GameType.NINE_BALL) }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = rowArrangement, modifier = Modifier.fillMaxWidth()) {
                        GameTile("10", s.tenBall, tileModifier(WideGameTileMaxSize), useGradientTiles, twoToneAccent = isFelt) { onNewSimpleMatch(GameType.TEN_BALL) }
                        GameTile("14.1", s.straightPool, tileModifier(WideGameTileMaxSize), useGradientTiles, twoToneAccent = isFelt, copper = true, onClick = onNewStraightMatch)
                    }
                }

                Spacer(Modifier.height(sectionGap))
                if (!isFelt) {
                    Text(s.startMore, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                }
                if (wide) {
                    Row(horizontalArrangement = rowArrangement, modifier = Modifier.fillMaxWidth()) {
                        MoreTile(Icons.Filled.Groups, s.tilePlayers, tileModifier(WideMoreTileMaxSize), useGradientTiles, twoToneAccent = isFelt, onClick = onOpenPlayers)
                        MoreTile(Icons.Filled.History, s.tileHistory, tileModifier(WideMoreTileMaxSize), useGradientTiles, twoToneAccent = isFelt, onClick = onOpenArchive)
                        MoreTile(Icons.Filled.Settings, s.tileSettings, tileModifier(WideMoreTileMaxSize), useGradientTiles, twoToneAccent = isFelt, onClick = onOpenSettings)
                        MoreTile(Icons.Filled.FitnessCenter, s.tileTraining, tileModifier(WideMoreTileMaxSize), useGradientTiles, twoToneAccent = isFelt, copper = true, onClick = onOpenTraining)
                        if (tournamentEnabled) {
                            MoreTile(Icons.Filled.EmojiEvents, s.tileTournament, tileModifier(WideMoreTileMaxSize), useGradientTiles, twoToneAccent = isFelt, copper = true, onClick = onOpenTournaments)
                        }
                        if (showRegieTile) {
                            MoreTile(Icons.Filled.Videocam, s.tileRegie, tileModifier(WideMoreTileMaxSize), useGradientTiles, twoToneAccent = isFelt, copper = true, onClick = onOpenObsControl)
                        }
                    }
                } else {
                    Row(horizontalArrangement = rowArrangement, modifier = Modifier.fillMaxWidth()) {
                        MoreTile(Icons.Filled.Groups, s.tilePlayers, tileModifier(WideMoreTileMaxSize), useGradientTiles, twoToneAccent = isFelt, onClick = onOpenPlayers)
                        MoreTile(Icons.Filled.History, s.tileHistory, tileModifier(WideMoreTileMaxSize), useGradientTiles, twoToneAccent = isFelt, onClick = onOpenArchive)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = rowArrangement, modifier = Modifier.fillMaxWidth()) {
                        MoreTile(Icons.Filled.Settings, s.tileSettings, tileModifier(WideMoreTileMaxSize), useGradientTiles, twoToneAccent = isFelt, onClick = onOpenSettings)
                        MoreTile(Icons.Filled.FitnessCenter, s.tileTraining, tileModifier(WideMoreTileMaxSize), useGradientTiles, twoToneAccent = isFelt, copper = true, onClick = onOpenTraining)
                        if (tournamentEnabled) {
                            MoreTile(Icons.Filled.EmojiEvents, s.tileTournament, tileModifier(WideMoreTileMaxSize), useGradientTiles, twoToneAccent = isFelt, copper = true, onClick = onOpenTournaments)
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    if (showRegieTile) {
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = rowArrangement, modifier = Modifier.fillMaxWidth()) {
                            MoreTile(Icons.Filled.Videocam, s.tileRegie, tileModifier(WideMoreTileMaxSize), useGradientTiles, twoToneAccent = isFelt, copper = true, onClick = onOpenObsControl)
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

/** Wraps [NumberTile] with the gradient/glow accent treatment when [gradientStyle] (Felt and Light),
 * leaving Dark/Vintage's plain filled tile untouched. [copper] marks 14.1 as the flagship mode with
 * the secondary accent instead of the primary one every other game tile uses - but only under
 * [twoToneAccent] (Felt alone): Light stays a single blue, just with the gradient added. */
@Composable
private fun GameTile(
    number: String,
    label: String,
    modifier: Modifier = Modifier,
    gradientStyle: Boolean,
    twoToneAccent: Boolean = false,
    copper: Boolean = false,
    onClick: () -> Unit,
) {
    val accent = if (copper && twoToneAccent) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    // Same whole-tile treatment as the "Mehr" row: a light-center-to-dark-edge radial gradient
    // (grootstudio.dev's styled-button recipe), a lighter accent rim border, and a matching glow -
    // tried testweise here too instead of just the neutral card + accent bar.
    val tileGradient = Pair(lerp(accent, Color.White, 0.35f), lerp(accent, Color.Black, 0.35f))
    val tileBorderColor = lerp(accent, Color.White, 0.45f)
    NumberTile(
        number,
        label,
        modifier,
        shape = if (gradientStyle) RoundedCornerShape(24.dp) else RoundedCornerShape(20.dp),
        tileGradient = if (gradientStyle) tileGradient else null,
        tileContentColor = if (gradientStyle) Color.White else null,
        tileBorderColor = if (gradientStyle) tileBorderColor else null,
        glowColor = if (gradientStyle) accent.copy(alpha = 0.6f) else null,
        onClick = onClick,
    )
}

/** Wraps [IconTile] with the gradient/glow accent treatment when [gradientStyle] (Felt and Light),
 * leaving Dark/Vintage's plain filled tile untouched. [copper] groups Training/Turnier/Regie under
 * the secondary accent, separately from Spieler/Verlauf/Einstellungen's primary one - but only under
 * [twoToneAccent] (Felt alone): Light stays a single blue, just with the gradient added. */
@Composable
private fun MoreTile(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    gradientStyle: Boolean,
    twoToneAccent: Boolean = false,
    copper: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val accent = if (copper && twoToneAccent) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    // The whole tile gets the vivid treatment now, not just a small inner badge - a light-center-
    // to-dark-edge radial gradient (grootstudio.dev's styled-button recipe: e.g. blue-500 core
    // fading to blue-800 rim), a lighter accent rim border, and a matching glow.
    val tileGradient = Pair(lerp(accent, Color.White, 0.35f), lerp(accent, Color.Black, 0.35f))
    val tileBorderColor = lerp(accent, Color.White, 0.45f)
    IconTile(
        icon,
        label,
        modifier,
        enabled = enabled,
        shape = if (gradientStyle) RoundedCornerShape(18.dp) else RoundedCornerShape(20.dp),
        tileGradient = if (gradientStyle) tileGradient else null,
        tileContentColor = if (gradientStyle) Color.White else null,
        tileBorderColor = if (gradientStyle) tileBorderColor else null,
        tileGlowColor = if (gradientStyle) accent.copy(alpha = 0.6f) else null,
        onClick = onClick,
    )
}

@Composable
private fun Tile(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(20.dp),
    color: Color? = null,
    gradientColors: Pair<Color, Color>? = null,
    contentColor: Color? = null,
    glowColor: Color? = null,
    borderColor: Color? = null,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val glowModifier = if (glowColor != null) {
        Modifier.shadow(16.dp, shape, ambientColor = glowColor, spotColor = glowColor)
    } else {
        Modifier
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = if (gradientColors != null) Color.Transparent else color ?: if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = contentColor ?: if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        border = borderColor?.let { BorderStroke(1.5.dp, it) },
        modifier = modifier.aspectRatio(1f).then(glowModifier),
    ) {
        val gradientModifier = if (gradientColors != null) {
            // Farthest-corner radius (half the tile's diagonal), matching CSS radial-gradient's
            // default - Brush.radialGradient's own default radius is only half the tile's side,
            // which faded to the dark edge color well before the corners, drawing a small,
            // hard-edged circle instead of a gradient that spans the whole tile.
            Modifier.fillMaxSize().drawWithCache {
                val radius = hypot(size.width, size.height) / 2f
                val brush = Brush.radialGradient(
                    colors = listOf(gradientColors.first, gradientColors.second),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = radius,
                )
                onDrawBehind { drawRect(brush) }
            }
        } else {
            Modifier.fillMaxSize()
        }
        Box(modifier = gradientModifier) {
            Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center) {
                content()
            }
        }
    }
}

/** [accentColor] draws a short bar above the number - Felt's flat, neutral-card treatment (a
 * restrained accent mark instead of filling the whole tile in a saturated color) instead of the
 * other 3 themes' plain filled tile. Null for those, so their look is unchanged. */
@Composable
private fun NumberTile(
    number: String,
    label: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    tileColor: Color? = null,
    tileGradient: Pair<Color, Color>? = null,
    tileContentColor: Color? = null,
    tileBorderColor: Color? = null,
    accentBrush: Brush? = null,
    labelColor: Color? = null,
    glowColor: Color? = null,
    onClick: () -> Unit,
) {
    Tile(
        modifier = modifier,
        shape = shape,
        color = tileColor,
        gradientColors = tileGradient,
        contentColor = tileContentColor,
        borderColor = tileBorderColor,
        glowColor = glowColor,
        onClick = onClick,
    ) {
        val uiScale = LocalUiScale.current
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (accentBrush != null) {
                Box(
                    modifier = Modifier
                        .width(26.dp * uiScale)
                        .height(4.dp * uiScale)
                        .background(accentBrush, RoundedCornerShape(2.dp)),
                )
                Spacer(Modifier.height(10.dp * uiScale))
            }
            Text(number, fontSize = 36.sp * uiScale, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                color = labelColor ?: Color.Unspecified,
            )
        }
    }
}

/** [tileGradient]/[tileBorderColor]/[tileGlowColor] give the whole tile Felt's vivid gradient +
 * glow + light-rim-border treatment (the same recipe as grootstudio.dev's styled button: a
 * light-center-to-dark-edge radial gradient, a lighter accent ring, and a matching colored
 * shadow) instead of the plain flat-colored tile the other 3 themes use (null keeps their look
 * unchanged). */
@Composable
private fun IconTile(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(20.dp),
    tileColor: Color? = null,
    tileGradient: Pair<Color, Color>? = null,
    tileContentColor: Color? = null,
    tileBorderColor: Color? = null,
    tileGlowColor: Color? = null,
    onClick: () -> Unit,
) {
    Tile(
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        color = tileColor,
        gradientColors = tileGradient,
        contentColor = tileContentColor,
        borderColor = tileBorderColor,
        glowColor = tileGlowColor,
        onClick = onClick,
    ) {
        val uiScale = LocalUiScale.current
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(38.dp * uiScale))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

/** Felt's plain-text wordmark: a small primary/secondary accent square on each side of "YAPBS",
 * a tiny nod to the two accent colors used throughout the theme, in place of the other 3 themes'
 * comic-style logo artwork (see the call site). */
@Composable
private fun FeltWordmark(uiScale: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp * uiScale)) {
            Box(
                modifier = Modifier
                    .size(9.dp * uiScale)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp)),
            )
            Text(
                "YAPBS",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 26.sp * uiScale,
                letterSpacing = 3.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Box(
                modifier = Modifier
                    .size(9.dp * uiScale)
                    .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(3.dp)),
            )
        }
        Spacer(Modifier.height(6.dp * uiScale))
        Text(
            "YET ANOTHER POOL BILLARD SCOREBOARD",
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp * uiScale,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
