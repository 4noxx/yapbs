package info.rbuck.billiardscoreboard

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.svg.SvgDecoder
import info.rbuck.billiardscoreboard.data.AppDatabase
import info.rbuck.billiardscoreboard.data.AppSettingsRepository
import info.rbuck.billiardscoreboard.data.ClubRepository
import info.rbuck.billiardscoreboard.data.MatchRepository
import info.rbuck.billiardscoreboard.data.PlayerRepository
import info.rbuck.billiardscoreboard.data.TournamentRepository
import info.rbuck.billiardscoreboard.data.TrainingRecordRepository
import info.rbuck.billiardscoreboard.obs.LiveScoreRepository
import info.rbuck.billiardscoreboard.obs.ObsWebSocketClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BsApplication : Application(), SingletonImageLoader.Factory {
    lateinit var playerRepository: PlayerRepository
        private set
    lateinit var matchRepository: MatchRepository
        private set
    lateinit var clubRepository: ClubRepository
        private set
    lateinit var settingsRepository: AppSettingsRepository
        private set
    lateinit var tournamentRepository: TournamentRepository
        private set
    lateinit var trainingRecordRepository: TrainingRecordRepository
        private set
    lateinit var liveScoreRepository: LiveScoreRepository
        private set
    lateinit var obsWebSocketClient: ObsWebSocketClient
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.get(this)
        settingsRepository = AppSettingsRepository(this)
        playerRepository = PlayerRepository(db.playerDao())
        matchRepository = MatchRepository(db.matchDao())
        clubRepository = ClubRepository(db.clubDao(), this)
        tournamentRepository = TournamentRepository(db.tournamentDao())
        trainingRecordRepository = TrainingRecordRepository(db.trainingDao())
        liveScoreRepository = LiveScoreRepository()
        obsWebSocketClient = ObsWebSocketClient(liveScoreRepository, settingsRepository)

        pruneHistoryPerRetention()
    }

    /** Enforce Settings > Data & privacy > history retention: on each cold start, drop finished
     * matches / tournaments / training sessions older than the configured number of days.
     * 0 = keep forever. Runs off the main thread and is best-effort. */
    private fun pruneHistoryPerRetention() {
        val days = settingsRepository.historyRetentionDays.value
        if (days <= 0) return
        val cutoff = System.currentTimeMillis() - days.toLong() * 24L * 60L * 60L * 1000L
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                matchRepository.pruneOlderThan(cutoff)
                tournamentRepository.pruneOlderThan(cutoff)
                trainingRecordRepository.pruneOlderThan(cutoff)
            }
        }
    }

    /** Registers SVG decoding for AsyncImage app-wide - used by RebuildRulesDialog to render the
     * bundled 14.1 re-rack diagrams (SVG files under assets/aufbau). Raster images (club crests) are unaffected. */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .build()
}
