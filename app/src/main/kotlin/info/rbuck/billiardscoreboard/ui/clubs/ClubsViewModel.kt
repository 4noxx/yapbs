package info.rbuck.billiardscoreboard.ui.clubs

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.rbuck.billiardscoreboard.data.Club
import info.rbuck.billiardscoreboard.data.ClubRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClubsViewModel(private val repository: ClubRepository) : ViewModel() {

    val clubs: StateFlow<List<Club>> = repository.observeClubs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createOrUpdate(id: String? = null, name: String, crestUri: Uri?, existingCrestPath: String?) {
        viewModelScope.launch {
            val club = repository.createOrUpdate(id, name, existingCrestPath)
            if (crestUri != null) {
                val path = repository.saveCrestImage(club.id, crestUri)
                repository.createOrUpdate(club.id, club.name, path)
            }
        }
    }

    fun delete(club: Club) {
        viewModelScope.launch { repository.delete(club) }
    }
}
