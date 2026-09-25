package info.rbuck.billiardscoreboard.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * One player as written to / read from a player-export JSON file (Settings > "Export players" /
 * "Import players from file"). Deliberately just name + club name, matching what
 * [info.rbuck.billiardscoreboard.ui.settings.ImportPlayersFileViewModel] needs to de-duplicate
 * and re-create players/clubs locally - no ids, since ids are meaningless across devices.
 */
@Serializable
data class PlayerExportEntry(val name: String, val club: String? = null)

/** Case-insensitive, trimmed identity key for "same player" matching between an imported entry and a local player. */
fun playerExportMatchKey(name: String, club: String?): String =
    name.trim().lowercase() + "|" + (club?.trim()?.lowercase() ?: "")

private val exportJson = Json { ignoreUnknownKeys = true }

/**
 * Writes [entries] to a file in the app's cache dir and returns a `content://` URI for it (via
 * [FileProvider] - a plain `file://` URI would be rejected by the system Share sheet on modern
 * Android). No network involved: how the resulting file actually reaches another device (Nearby
 * Share, Bluetooth, email, cable, cloud drive, ...) is entirely up to whatever the user picks in
 * the share sheet this URI gets handed to - the app itself never opens a network connection.
 */
fun writePlayerExportFile(context: Context, entries: List<PlayerExportEntry>): Uri {
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(dir, "pb_scoreboard_players.json")
    file.writeText(exportJson.encodeToString(ListSerializer(PlayerExportEntry.serializer()), entries))
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/** Reads and parses a player-export file the user picked via the system file/document picker. */
fun readPlayerExportFile(context: Context, uri: Uri): List<PlayerExportEntry> {
    val stream = context.contentResolver.openInputStream(uri) ?: return emptyList()
    val text = stream.bufferedReader().use { it.readText() }
    return exportJson.decodeFromString(ListSerializer(PlayerExportEntry.serializer()), text)
}
