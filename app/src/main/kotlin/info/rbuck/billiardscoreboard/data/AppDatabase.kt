package info.rbuck.billiardscoreboard.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Player::class, MatchRecordEntity::class, Club::class, TournamentRecordEntity::class, TrainingRecordEntity::class],
    version = 4,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun matchDao(): MatchDao
    abstract fun clubDao(): ClubDao
    abstract fun tournamentDao(): TournamentDao
    abstract fun trainingDao(): TrainingDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "billard-scoreboard.db",
            )
                // Pre-release schema churn; no real user data to preserve yet.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build().also { instance = it }
        }
    }
}
