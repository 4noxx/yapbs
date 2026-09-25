package info.rbuck.billiardscoreboard.data

import kotlinx.coroutines.flow.Flow
import java.util.UUID

class PlayerRepository(
    private val dao: PlayerDao,
) {

    fun observeActivePlayers(): Flow<List<Player>> = dao.observeActivePlayers()

    suspend fun getById(id: String): Player? = dao.getById(id)

    suspend fun createOrUpdate(id: String? = null, name: String, clubId: String? = null): Player {
        require(name.isNotBlank()) { "Player name must not be blank" }
        val trimmedName = name.trim()
        val existing = id?.let { dao.getById(it) }
        val player = existing?.copy(name = trimmedName, clubId = clubId)
            ?: Player(id = id ?: UUID.randomUUID().toString(), name = trimmedName, clubId = clubId)
        dao.upsert(player)
        return player
    }

    suspend fun delete(player: Player) = dao.delete(player)
}
