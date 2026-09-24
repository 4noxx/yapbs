package info.rbuck.billiardscoreboard.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import info.rbuck.billiardscoreboard.domain.GameType
import info.rbuck.billiardscoreboard.domain.training.TrainingExercise
import info.rbuck.billiardscoreboard.ui.archive.ArchiveScreen
import info.rbuck.billiardscoreboard.ui.clubs.ClubsScreen
import info.rbuck.billiardscoreboard.ui.headtohead.HeadToHeadScreen
import info.rbuck.billiardscoreboard.ui.newmatch.NewSimpleMatchScreen
import info.rbuck.billiardscoreboard.ui.newmatch.NewStraightMatchScreen
import info.rbuck.billiardscoreboard.ui.newmatch.NewTrainingScreen
import info.rbuck.billiardscoreboard.ui.obs.ObsControlScreen
import info.rbuck.billiardscoreboard.ui.players.PlayersScreen
import info.rbuck.billiardscoreboard.ui.settings.ImportPlayersFileScreen
import info.rbuck.billiardscoreboard.ui.settings.SettingsScreen
import info.rbuck.billiardscoreboard.ui.simplematch.SimpleMatchScreen
import info.rbuck.billiardscoreboard.ui.start.StartScreen
import info.rbuck.billiardscoreboard.ui.straightmatch.StraightMatchScreen
import info.rbuck.billiardscoreboard.ui.tournament.NewTournamentScreen
import info.rbuck.billiardscoreboard.ui.tournament.TournamentHistoryScreen
import info.rbuck.billiardscoreboard.ui.tournament.TournamentScreen
import info.rbuck.billiardscoreboard.ui.training.TrainingScoreScreen

/** Switches the active new-match setup screen to a different discipline, replacing the current setup screen so Back still goes to Start. */
private fun NavController.navigateToNewMatch(gameType: GameType) {
    val route = if (gameType == GameType.STRAIGHT_POOL) {
        BsDestinations.newStraightMatch()
    } else {
        BsDestinations.newSimpleMatch(gameType.name)
    }
    navigate(route) {
        popUpTo(BsDestinations.START) { inclusive = false }
        launchSingleTop = true
    }
}

@Composable
fun BsNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = BsDestinations.START) {
        composable(BsDestinations.START) {
            StartScreen(
                onNewSimpleMatch = { gameType ->
                    navController.navigate(BsDestinations.newSimpleMatch(gameType.name))
                },
                onNewStraightMatch = { navController.navigate(BsDestinations.newStraightMatch()) },
                onOpenPlayers = { navController.navigate(BsDestinations.PLAYERS) },
                onOpenArchive = { navController.navigate(BsDestinations.ARCHIVE) },
                onOpenSettings = { navController.navigate(BsDestinations.SETTINGS) },
                onOpenTraining = { navController.navigate(BsDestinations.NEW_TRAINING) },
                onOpenTournaments = { navController.navigate(BsDestinations.TOURNAMENT_HISTORY) },
                onOpenObsControl = { navController.navigate(BsDestinations.OBS_CONTROL) },
            )
        }

        composable(BsDestinations.OBS_CONTROL) {
            ObsControlScreen(onBack = { navController.popBackStack() })
        }

        composable(BsDestinations.PLAYERS) {
            PlayersScreen(
                onBack = { navController.popBackStack() },
                onOpenClubs = { navController.navigate(BsDestinations.CLUBS) },
                onOpenHeadToHead = { navController.navigate(BsDestinations.headToHead()) },
            )
        }

        composable(
            route = BsDestinations.HEAD_TO_HEAD,
            arguments = listOf(
                navArgument("a") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("b") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) { backStackEntry ->
            HeadToHeadScreen(
                onBack = { navController.popBackStack() },
                initialAId = backStackEntry.arguments?.getString("a")?.takeIf { it.isNotEmpty() },
                initialBId = backStackEntry.arguments?.getString("b")?.takeIf { it.isNotEmpty() },
            )
        }

        composable(BsDestinations.CLUBS) {
            ClubsScreen(onBack = { navController.popBackStack() })
        }

        composable(BsDestinations.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenImportPlayersFile = { uri ->
                    navController.navigate(BsDestinations.importPlayersFile(Uri.encode(uri.toString())))
                },
            )
        }

        composable(
            route = BsDestinations.IMPORT_PLAYERS_FILE,
            arguments = listOf(navArgument("uri") { type = NavType.StringType }),
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("uri") ?: return@composable
            ImportPlayersFileScreen(uri = Uri.parse(Uri.decode(encodedUri)), onBack = { navController.popBackStack() })
        }

        composable(BsDestinations.ARCHIVE) {
            ArchiveScreen(
                onBack = { navController.popBackStack() },
                onOpenSimpleMatch = { id -> navController.navigate(BsDestinations.simpleMatch(id)) },
                onOpenStraightMatch = { id -> navController.navigate(BsDestinations.straightMatch(id)) },
            )
        }

        composable(
            route = BsDestinations.NEW_SIMPLE_MATCH,
            arguments = listOf(
                navArgument("gameType") { type = NavType.StringType },
                navArgument("rematchOf") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) { backStackEntry ->
            val gameTypeName = backStackEntry.arguments?.getString("gameType") ?: GameType.EIGHT_BALL.name
            val gameType = GameType.entries.firstOrNull { it.name == gameTypeName } ?: GameType.EIGHT_BALL
            val rematchOf = backStackEntry.arguments?.getString("rematchOf")
            NewSimpleMatchScreen(
                gameType = gameType,
                rematchOfMatchId = rematchOf,
                onBack = { navController.popBackStack() },
                onMatchCreated = { id ->
                    navController.navigate(BsDestinations.simpleMatch(id)) {
                        popUpTo(BsDestinations.START)
                    }
                },
                onGameTypeSelected = { newType -> navController.navigateToNewMatch(newType) },
                onOpenHeadToHead = { a, b -> navController.navigate(BsDestinations.headToHead(a, b)) },
            )
        }

        composable(
            route = BsDestinations.NEW_STRAIGHT_MATCH,
            arguments = listOf(
                navArgument("rematchOf") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) { backStackEntry ->
            val rematchOf = backStackEntry.arguments?.getString("rematchOf")
            NewStraightMatchScreen(
                rematchOfMatchId = rematchOf,
                onBack = { navController.popBackStack() },
                onMatchCreated = { id ->
                    navController.navigate(BsDestinations.straightMatch(id)) {
                        popUpTo(BsDestinations.START)
                    }
                },
                onGameTypeSelected = { newType -> navController.navigateToNewMatch(newType) },
                onOpenHeadToHead = { a, b -> navController.navigate(BsDestinations.headToHead(a, b)) },
            )
        }

        composable(
            route = BsDestinations.SIMPLE_MATCH,
            arguments = listOf(navArgument("matchId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId") ?: return@composable
            SimpleMatchScreen(
                matchId = matchId,
                onBack = { navController.popBackStack(BsDestinations.START, inclusive = false) },
                onSaveAndRematch = { gameType ->
                    navController.navigate(BsDestinations.newSimpleMatch(gameType.name, rematchOf = matchId)) {
                        popUpTo(BsDestinations.START)
                    }
                },
            )
        }

        composable(
            route = BsDestinations.STRAIGHT_MATCH,
            arguments = listOf(navArgument("matchId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId") ?: return@composable
            StraightMatchScreen(
                matchId = matchId,
                onBack = { navController.popBackStack(BsDestinations.START, inclusive = false) },
                onSaveAndRematch = {
                    navController.navigate(BsDestinations.newStraightMatch(rematchOf = matchId)) {
                        popUpTo(BsDestinations.START)
                    }
                },
            )
        }

        composable(BsDestinations.NEW_TRAINING) {
            NewTrainingScreen(
                onBack = { navController.popBackStack() },
                onStart = { exercise, player ->
                    navController.navigate(BsDestinations.trainingSession(exercise.name, player.id)) {
                        popUpTo(BsDestinations.START)
                    }
                },
            )
        }

        composable(
            route = BsDestinations.TRAINING_SESSION,
            arguments = listOf(
                navArgument("exercise") { type = NavType.StringType },
                navArgument("playerId") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val exerciseName = backStackEntry.arguments?.getString("exercise") ?: return@composable
            val exercise = TrainingExercise.entries.firstOrNull { it.name == exerciseName } ?: return@composable
            val playerId = backStackEntry.arguments?.getString("playerId") ?: return@composable
            TrainingScoreScreen(
                exercise = exercise,
                playerId = playerId,
                onBack = { navController.popBackStack(BsDestinations.START, inclusive = false) },
            )
        }

        composable(BsDestinations.TOURNAMENT_HISTORY) {
            TournamentHistoryScreen(
                onBack = { navController.popBackStack() },
                onOpenTournament = { id -> navController.navigate(BsDestinations.tournament(id)) },
                onNewTournament = { navController.navigate(BsDestinations.newTournament()) },
            )
        }

        composable(
            route = BsDestinations.NEW_TOURNAMENT,
            arguments = listOf(
                navArgument("rematchOf") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) { backStackEntry ->
            val rematchOf = backStackEntry.arguments?.getString("rematchOf")
            NewTournamentScreen(
                rematchOfTournamentId = rematchOf,
                onBack = { navController.popBackStack() },
                onTournamentCreated = { id ->
                    navController.navigate(BsDestinations.tournament(id)) {
                        popUpTo(BsDestinations.TOURNAMENT_HISTORY)
                    }
                },
            )
        }

        composable(
            route = BsDestinations.TOURNAMENT,
            arguments = listOf(navArgument("tournamentId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val tournamentId = backStackEntry.arguments?.getString("tournamentId") ?: return@composable
            TournamentScreen(
                tournamentId = tournamentId,
                onBack = { navController.popBackStack(BsDestinations.TOURNAMENT_HISTORY, inclusive = false) },
                onSaveAndRematch = {
                    navController.navigate(BsDestinations.newTournament(rematchOf = tournamentId)) {
                        popUpTo(BsDestinations.TOURNAMENT_HISTORY)
                    }
                },
            )
        }
    }
}
