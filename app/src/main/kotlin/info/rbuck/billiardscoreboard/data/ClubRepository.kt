package info.rbuck.billiardscoreboard.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class ClubRepository(
    private val dao: ClubDao,
    private val context: Context,
) {
    fun observeClubs(): Flow<List<Club>> = dao.observeClubs()

    suspend fun getById(id: String): Club? = dao.getById(id)

    suspend fun createOrUpdate(id: String? = null, name: String, crestPath: String? = null): Club {
        require(name.isNotBlank()) { "Club name must not be blank" }
        val club = Club(id = id ?: UUID.randomUUID().toString(), name = name.trim(), crestPath = crestPath)
        dao.upsert(club)
        return club
    }

    /** Copies the picked image into app-internal storage so it survives even if the source URI's access grant is revoked. */
    suspend fun saveCrestImage(clubId: String, sourceUri: Uri): String = withContext(Dispatchers.IO) {
        val crestsDir = File(context.filesDir, "club_crests").apply { mkdirs() }
        val destFile = File(crestsDir, "$clubId.jpg")
        context.contentResolver.openInputStream(sourceUri).use { input ->
            requireNotNull(input) { "Could not open image" }
            destFile.outputStream().use { output -> input.copyTo(output) }
        }
        destFile.absolutePath
    }

    suspend fun delete(club: Club) {
        club.crestPath?.let { File(it).delete() }
        dao.delete(club)
    }
}
