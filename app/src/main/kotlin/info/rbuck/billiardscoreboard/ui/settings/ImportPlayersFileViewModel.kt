package info.rbuck.billiardscoreboard.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.rbuck.billiardscoreboard.data.ClubRepository
import info.rbuck.billiardscoreboard.data.PlayerRepository
import info.rbuck.billiardscoreboard.data.playerExportMatchKey
import info.rbuck.billiardscoreboard.data.readPlayerExportFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One player from the picked file, and whether it already exists locally (name+club match). */
data class ImportCandidate(val name: String, val club: String?, val alreadyExists: Boolean)

class ImportPlayersFileViewModel(
    private val playerRepository: PlayerRepository,
    private val clubRepository: ClubRepository,
    context: Context,
    uri: Uri,
) : ViewModel() {

    /** Null while still loading, true if the picked file couldn't be read/parsed as a player export. */
    private val _loadFailed = MutableStateFlow(false)
    val loadFailed: StateFlow<Boolean> = _loadFailed

    private val entries = try {
        readPlayerExportFile(context, uri).distinctBy { playerExportMatchKey(it.name, it.club) }
    } catch (e: Exception) {
        _loadFailed.value = true
        emptyList()
    }

    val candidates: StateFlow<List<ImportCandidate>> = combine(
        playerRepository.observeActivePlayers(),
        clubRepository.observeClubs(),
    ) { players, clubs ->
        val clubById = clubs.associateBy { it.id }
        val localKeys = players.map { playerExportMatchKey(it.name, it.clubId?.let { id -> clubById[id]?.name }) }.toSet()
        entries.map { entry -> ImportCandidate(entry.name, entry.club, playerExportMatchKey(entry.name, entry.club) in localKeys) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selected = MutableStateFlow<Set<ImportCandidate>>(emptySet())
    val selected: StateFlow<Set<ImportCandidate>> = _selected

    fun toggleSelected(candidate: ImportCandidate) {
        if (candidate.alreadyExists) return
        _selected.update { current -> if (candidate in current) current - candidate else current + candidate }
    }

    fun selectAllNew() {
        _selected.value = candidates.value.filterNot { it.alreadyExists }.toSet()
    }

    fun importSelected() {
        val toImport = _selected.value
        viewModelScope.launch {
            // Keyed by lowercased club name, seeded from what's already in the DB and kept up to date as
            // this loop creates new clubs - otherwise two candidates sharing a brand-new club would each
            // create their own duplicate instead of the second one reusing the first one's.
            val clubCache = clubRepository.observeClubs().first()
                .associateBy { it.name.trim().lowercase() }
                .toMutableMap()
            for (candidate in toImport) {
                val clubId = candidate.club?.let { clubName ->
                    val key = clubName.trim().lowercase()
                    val existing = clubCache[key]
                    if (existing != null) {
                        existing.id
                    } else {
                        val created = clubRepository.createOrUpdate(name = clubName)
                        clubCache[key] = created
                        created.id
                    }
                }
                playerRepository.createOrUpdate(name = candidate.name, clubId = clubId)
            }
            _selected.value = emptySet()
        }
    }
}
