package info.rbuck.billiardscoreboard.ui.headtohead

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import info.rbuck.billiardscoreboard.domain.GameType
import info.rbuck.billiardscoreboard.data.Player
import info.rbuck.billiardscoreboard.i18n.LocalStrings
import info.rbuck.billiardscoreboard.i18n.Strings
import info.rbuck.billiardscoreboard.ui.bsApplication
import info.rbuck.billiardscoreboard.ui.components.ChoiceChip
import info.rbuck.billiardscoreboard.ui.components.SettingsCard
import java.text.DateFormat
import java.util.Date
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class Timeframe { ALL, LAST_12M }

private const val TWELVE_MONTHS_MS = 365L * 24 * 60 * 60 * 1000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeadToHeadScreen(
    onBack: () -> Unit,
    initialAId: String? = null,
    initialBId: String? = null,
) {
    val app = bsApplication()
    val s = LocalStrings.current
    val viewModel: HeadToHeadViewModel = viewModel(
        factory = viewModelFactory {
            initializer { HeadToHeadViewModel(app.playerRepository, app.matchRepository) }
        },
    )
    val players by viewModel.players.collectAsStateWithLifecycle()
    val playerAId by viewModel.playerAId.collectAsStateWithLifecycle()
    val playerBId by viewModel.playerBId.collectAsStateWithLifecycle()
    val allGames by viewModel.games.collectAsStateWithLifecycle()
    val straightDetail by viewModel.straightDetail.collectAsStateWithLifecycle()

    LaunchedEffect(initialAId) { initialAId?.let(viewModel::selectA) }
    LaunchedEffect(initialBId) { initialBId?.let(viewModel::selectB) }

    val nameA = players.firstOrNull { it.id == playerAId }?.name
    val nameB = players.firstOrNull { it.id == playerBId }?.name

    var disciplineFilter by remember { mutableStateOf<GameType?>(null) }
    var timeframe by remember { mutableStateOf(Timeframe.ALL) }

    val filteredGames = remember(allGames, disciplineFilter, timeframe) {
        val cutoff = if (timeframe == Timeframe.LAST_12M) System.currentTimeMillis() - TWELVE_MONTHS_MS else 0L
        allGames.orEmpty().filter { g ->
            (disciplineFilter == null || g.gameType == disciplineFilter) && g.createdAt >= cutoff
        }
    }
    val h2h = remember(filteredGames) { aggregateHeadToHead(filteredGames) }

    val straightIds = remember(filteredGames) {
        filteredGames.filter { it.gameType?.isStraightPool == true }.map { it.matchId }
    }
    LaunchedEffect(straightIds, playerAId, playerBId) {
        val a = playerAId; val b = playerBId
        if (a != null && b != null) viewModel.loadStraightDetail(straightIds, a, b)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.h2hTitle) },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                PlayerPicker(s.h2hPlayerA, nameA, players, Modifier.weight(1f), viewModel::selectA)
                PlayerPicker(s.h2hPlayerB, nameB, players, Modifier.weight(1f), viewModel::selectB)
            }

            if (playerAId != null && playerBId != null && !allGames.isNullOrEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                ) {
                    ChoiceChip(disciplineFilter == null, { disciplineFilter = null }, s.h2hFilterAll)
                    GameType.entries.forEach { gt ->
                        ChoiceChip(disciplineFilter == gt, { disciplineFilter = gt }, gt.displayName)
                    }
                    Spacer(Modifier.width(8.dp))
                    ChoiceChip(timeframe == Timeframe.ALL, { timeframe = Timeframe.ALL }, s.h2hFilterAllTime)
                    ChoiceChip(timeframe == Timeframe.LAST_12M, { timeframe = Timeframe.LAST_12M }, s.h2hFilterLast12)
                }
            }

            when {
                playerAId == null || playerBId == null ->
                    Text(s.h2hPickPrompt, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                allGames.isNullOrEmpty() ->
                    Text(s.h2hNoMeetings, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                h2h.total == 0 ->
                    Text(s.h2hNoMeetings, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> H2HBody(h2h, straightDetail, nameA ?: s.h2hPlayerA, nameB ?: s.h2hPlayerB, s)
            }
        }
    }
}

@Composable
private fun H2HBody(
    h2h: HeadToHead,
    straight: StraightH2HDetail?,
    nameA: String,
    nameB: String,
    s: Strings,
) {
    val colorA = MaterialTheme.colorScheme.primary
    val colorB = MaterialTheme.colorScheme.onSurface
    val colorDraw = MaterialTheme.colorScheme.surfaceVariant

    SettingsCard {
        Text(s.h2hRecordSection, style = MaterialTheme.typography.titleMedium)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(nameA, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text("${h2h.winsA} : ${h2h.winsB}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(nameB, modifier = Modifier.weight(1f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
        }
        BalanceBar(h2h.winsA, h2h.draws, h2h.winsB, colorA, colorDraw, colorB)
        val range = h2h.first?.let {
            val df = DateFormat.getDateInstance(DateFormat.SHORT)
            "${df.format(Date(it.createdAt))} – ${df.format(Date(h2h.last!!.createdAt))}"
        }
        Text(
            buildString {
                append(s.h2hMatchCount(h2h.total))
                if (h2h.draws > 0) append("  ·  ${h2h.draws} ${s.h2hDraw}")
                if (range != null) append("  ·  $range")
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (h2h.streakPlayer != -1 && h2h.streakLength >= 2) {
            Text(
                s.h2hStreak(if (h2h.streakPlayer == 0) nameA else nameB, h2h.streakLength),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (h2h.racksA + h2h.racksB > 0) {
            Text(
                "${s.h2hRacks}:  ${h2h.racksA} : ${h2h.racksB}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (h2h.total >= 2) {
        SettingsCard {
            Text(s.h2hMomentumSection, style = MaterialTheme.typography.titleMedium)
            Text(s.h2hMomentumHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            MomentumChart(h2h.momentum, colorA, colorB, MaterialTheme.colorScheme.outline)
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(nameA, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = colorA)
                Text(nameB, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (straight != null) {
        SettingsCard {
            Text("${s.h2hStraightSection}  (${s.h2hMatchCount(straight.matchCount)})", style = MaterialTheme.typography.titleMedium)
            StatRow(s.h2hAvg, round1(straight.avgA).toString(), round1(straight.avgB).toString())
            StatRow(s.h2hHighestRun, straight.highestBreakA.toString(), straight.highestBreakB.toString())
            StatRow(s.h2hFouls, straight.foulsA.toString(), straight.foulsB.toString())
        }
    }

    if (h2h.byDiscipline.size > 1) {
        SettingsCard {
            Text(s.h2hByDisciplineSection, style = MaterialTheme.typography.titleMedium)
            h2h.byDiscipline.forEach { (gt, rec) ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(gt.displayName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (rec.draws > 0) "${rec.winsA} : ${rec.winsB}  (${rec.draws} ${s.h2hDrawsShort})" else "${rec.winsA} : ${rec.winsB}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }

    SettingsCard {
        Text(s.h2hFormSection, style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            h2h.games.takeLast(20).forEach { g ->
                val (bg, fg, label) = when (g.winner) {
                    0 -> Triple(colorA, MaterialTheme.colorScheme.onPrimary, "A")
                    1 -> Triple(colorB, MaterialTheme.colorScheme.surface, "B")
                    else -> Triple(colorDraw, MaterialTheme.colorScheme.onSurfaceVariant, s.h2hDrawsShort)
                }
                Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(bg), contentAlignment = Alignment.Center) {
                    Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    h2h.last?.let { last ->
        SettingsCard {
            Text(s.h2hLastMeeting, style = MaterialTheme.typography.titleMedium)
            val date = DateFormat.getDateInstance().format(Date(last.createdAt))
            val winnerName = when (last.winner) {
                0 -> nameA
                1 -> nameB
                else -> s.h2hDraw
            }
            Text("${last.gameType?.displayName ?: ""} · $date", style = MaterialTheme.typography.bodyMedium)
            Text("${last.scoreA} : ${last.scoreB}   →   $winnerName", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatRow(label: String, valueA: String, valueB: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(valueA, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valueB, modifier = Modifier.weight(1f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MomentumChart(series: List<Int>, colorA: Color, colorB: Color, zeroColor: Color) {
    if (series.isEmpty()) return
    val maxAbs = (series.maxOf { abs(it) }).coerceAtLeast(1)
    Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
        val w = size.width
        val h = size.height
        val midY = h / 2f
        drawLine(zeroColor, Offset(0f, midY), Offset(w, midY), strokeWidth = 1.5f)
        val n = series.size
        fun pointAt(i: Int): Offset {
            val x = if (n == 1) w / 2f else w * i / (n - 1)
            val y = midY - (series[i].toFloat() / maxAbs) * (midY - 6f)
            return Offset(x, y)
        }
        // zero start -> first point, then between points
        var prev = Offset(if (n == 1) w / 2f else 0f, midY)
        for (i in series.indices) {
            val cur = pointAt(i)
            val up = series[i] >= (if (i == 0) 0 else series[i - 1])
            drawLine(if (up) colorA else colorB, prev, cur, strokeWidth = 4f, cap = StrokeCap.Round)
            prev = cur
        }
        val end = pointAt(n - 1)
        drawCircle(if (series.last() >= 0) colorA else colorB, radius = 5f, center = end)
    }
}

@Composable
private fun BalanceBar(winsA: Int, draws: Int, winsB: Int, colorA: Color, colorDraw: Color, colorB: Color) {
    val total = (winsA + draws + winsB).coerceAtLeast(1)
    Row(modifier = Modifier.fillMaxWidth().height(20.dp).clip(RoundedCornerShape(6.dp))) {
        if (winsA > 0) Box(Modifier.weight(winsA.toFloat() / total).fillMaxSize().background(colorA))
        if (draws > 0) Box(Modifier.weight(draws.toFloat() / total).fillMaxSize().background(colorDraw))
        if (winsB > 0) Box(Modifier.weight(winsB.toFloat() / total).fillMaxSize().background(colorB))
    }
}

@Composable
private fun PlayerPicker(
    label: String,
    selectedName: String?,
    players: List<Player>,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selectedName ?: "—", maxLines = 1)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = false),
                modifier = Modifier.heightIn(max = 360.dp),
            ) {
                players.forEach { p ->
                    DropdownMenuItem(text = { Text(p.name) }, onClick = { onSelect(p.id); expanded = false })
                }
            }
        }
    }
}

private fun round1(v: Float): Float = (v * 10).roundToInt() / 10f


/**
 * Compact head-to-head line for the "New match" screens: shows the record between the two chosen
 * players (once both are chosen and they have met), tappable to open the full screen. No-op row
 * when they have never met, so it never adds noise for first-time pairings.
 */
@Composable
fun HeadToHeadInline(
    playerAId: String,
    playerBId: String,
    playerAName: String,
    playerBName: String,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (playerAId == playerBId) return
    val s = LocalStrings.current
    val repo = bsApplication().matchRepository
    val rows by remember(playerAId, playerBId) { repo.observeFinishedBetween(playerAId, playerBId) }
        .collectAsStateWithLifecycle(emptyList())
    val h2h = remember(rows) { computeHeadToHead(rows, playerAId, playerBId) }
    if (h2h.total == 0) return

    val last = h2h.last
    val leader = when {
        h2h.winsA > h2h.winsB -> playerAName
        h2h.winsB > h2h.winsA -> playerBName
        else -> null
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onOpen)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Column {
            Text(
                "${s.h2hTitle}:  ${h2h.winsA} : ${h2h.winsB}" + (leader?.let { "  ($it)" } ?: ""),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            if (last != null) {
                val df = DateFormat.getDateInstance(DateFormat.SHORT)
                Text(
                    "${last.gameType?.displayName ?: ""} ${last.scoreA}:${last.scoreB} · ${df.format(Date(last.createdAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

