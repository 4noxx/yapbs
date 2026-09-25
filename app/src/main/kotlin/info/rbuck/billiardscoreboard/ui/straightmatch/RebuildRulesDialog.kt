package info.rbuck.billiardscoreboard.ui.straightmatch

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import info.rbuck.billiardscoreboard.i18n.AppLanguage
import info.rbuck.billiardscoreboard.i18n.Translations
import info.rbuck.billiardscoreboard.ui.bsApplication
import info.rbuck.billiardscoreboard.ui.components.HideStatusBarInDialog

/**
 * DBU rule 4.8 "Situationen beim Wiederaufbau" (Anlage 1 - Aufbaugrafiken): what happens to the
 * cue ball and the 15th object ball when they block rebuilding the rack. Shown from an Info button
 * on the 14.1 match scoreboard, since this is the one 14.1 rule complex enough that players
 * routinely need to look it up mid-match rather than recall from memory.
 *
 * Diagrams are the official DBU artwork, redrawn as SVG (assets/aufbau/Aufbau_<n>_<1|2>.svg) and
 * decoded at runtime via coil-svg (registered in BsApplication). English captions below are the
 * fallback text (and single source of truth for English); DE/ES/FR overrides live in
 * [Translations.rebuildRulesCaption], keyed by example number and part - DE is the original wording
 * transcribed verbatim from "Spielregeln Pool" (Stand 07/2016), Anlage 1, Beispiel 1-7.
 */
private data class RebuildExample(val number: Int, val caption1: String, val asset1: String, val caption2: String, val asset2: String)

private val REBUILD_EXAMPLES = listOf(
    RebuildExample(
        1,
        "The 14th and 15th balls have been pocketed.", "aufbau/Aufbau_1_1.svg",
        "The rack is rebuilt completely. The cue ball stays where it is.", "aufbau/Aufbau_1_2.svg",
    ),
    RebuildExample(
        2,
        "The 15th ball is in the way of the rack; the cue ball lies somewhere on the table.", "aufbau/Aufbau_2_1.svg",
        "The 15th ball is placed on the head spot. The cue ball stays where it is.", "aufbau/Aufbau_2_2.svg",
    ),
    RebuildExample(
        3,
        "The 15th ball is in the way of the rack; the cue ball blocks the head spot.", "aufbau/Aufbau_3_1.svg",
        "The 15th ball is placed on the center spot. The cue ball stays where it is.", "aufbau/Aufbau_3_2.svg",
    ),
    RebuildExample(
        4,
        "Both the cue ball and the 15th ball are in the way of the rack.", "aufbau/Aufbau_4_1.svg",
        "The rack is rebuilt completely. The cue ball is played from anywhere within the kitchen.", "aufbau/Aufbau_4_2.svg",
    ),
    RebuildExample(
        5,
        "The cue ball is in the way of the rack; the 15th ball is not within the kitchen.", "aufbau/Aufbau_5_1.svg",
        "The cue ball may be placed anywhere within the kitchen. The 15th ball stays where it is.", "aufbau/Aufbau_5_2.svg",
    ),
    RebuildExample(
        6,
        "The cue ball is in the way of the rack; the 15th ball lies within the kitchen.", "aufbau/Aufbau_6_1.svg",
        "The cue ball is placed on the head spot and may be played in any direction. The 15th ball stays where it is.", "aufbau/Aufbau_6_2.svg",
    ),
    RebuildExample(
        7,
        "The cue ball is in the way of the rack; the 15th ball blocks the head spot.", "aufbau/Aufbau_7_1.svg",
        "The cue ball is placed on the center spot. The 15th ball stays where it is.", "aufbau/Aufbau_7_2.svg",
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RebuildRulesDialog(onDismiss: () -> Unit) {
    val app = bsApplication()
    val language by app.settingsRepository.language.collectAsStateWithLifecycle()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        HideStatusBarInDialog()
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(Translations.rebuildRulesTitle(language) ?: "Rebuild situations") },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Filled.Close, contentDescription = "Close")
                            }
                        },
                    )
                },
            ) { padding ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    items(REBUILD_EXAMPLES) { example ->
                        Column {
                            Text(
                                "${Translations.rebuildRulesExampleLabel(language) ?: "Example"} ${example.number}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(8.dp))
                            RebuildExampleImages(example, language)
                            Spacer(Modifier.height(20.dp))
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RebuildExampleImages(example: RebuildExample, language: AppLanguage) {
    val caption1 = Translations.rebuildRulesCaption(example.number, 1, language) ?: example.caption1
    val caption2 = Translations.rebuildRulesCaption(example.number, 2, language) ?: example.caption2
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (isLandscape) {
        // Sized by height, not by the row's half-width: at the table diagram's tall portrait aspect
        // ratio, letting width drive the size (the old "fillMaxWidth then derive height" approach)
        // made each table taller than the screen on a landscape tablet, clipping the bottom pockets
        // out of view. Reserving a fixed budget for the chrome around it (top bar, title, captions,
        // spacing) and handing the rest to the images as a height cap keeps a full example - both
        // table halves - on screen at once without needing to scroll mid-diagram.
        val tableHeight = (LocalConfiguration.current.screenHeightDp.dp - 220.dp).coerceIn(200.dp, 700.dp)

        // Captions and images are two separate rows, not one Column per side: if one caption wraps
        // to more lines than the other, keeping them in the same Column would push that side's image
        // down relative to its neighbor. A shared caption row - stretched to the taller caption's
        // height via IntrinsicSize.Max - keeps both tables starting at the exact same height instead.
        Column {
            Row(modifier = Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    caption1,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    caption2,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    AufbauTable(example.asset1, caption1, fixedHeight = tableHeight)
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    AufbauTable(example.asset2, caption2, fixedHeight = tableHeight)
                }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AufbauImage(caption1, example.asset1, modifier = Modifier.fillMaxWidth())
            AufbauImage(caption2, example.asset2, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun AufbauImage(caption: String, assetPath: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(caption, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        AufbauTable(assetPath, caption, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun AufbauTable(assetPath: String, caption: String, modifier: Modifier = Modifier, fixedHeight: Dp? = null) {
    val sizedModifier = if (fixedHeight != null) {
        // matchHeightConstraintsFirst: derive width from the fixed height instead of the default
        // (derive height from available width) - that default is exactly what made the table too
        // tall for the screen in landscape (see the call site's comment).
        modifier.height(fixedHeight).aspectRatio(525.67f / 947.72f, matchHeightConstraintsFirst = true)
    } else {
        modifier.aspectRatio(525.67f / 947.72f)
    }
    AsyncImage(
        model = "file:///android_asset/$assetPath",
        contentDescription = caption,
        modifier = sizedModifier,
    )
}

