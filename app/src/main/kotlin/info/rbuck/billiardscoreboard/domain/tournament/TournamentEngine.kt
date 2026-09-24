package info.rbuck.billiardscoreboard.domain.tournament

/**
 * Pure functions over [TournamentState], in the same "fold the action log" style as
 * [info.rbuck.billiardscoreboard.domain.straight.StraightMatchEngine]: the list of
 * played [TournamentGameResult]s is the only stored state, and everything else (who's
 * at the table, the waiting queue, standings, the champion) is re-derived from it -
 * so undo is just "drop the last result".
 */
object TournamentEngine {

    private const val BYE = "__BYE__"

    /** The two players currently at the table, or null once the tournament is over
     * (or if there aren't at least 2 players to begin with). Null for PARTNER_ROTATION too -
     * that mode is 2v2, not 1v1, so use [currentTeams] instead. */
    fun currentTable(state: TournamentState): Pair<String, String>? {
        if (state.playerIds.size < 2) return null
        return when (state.settings.mode) {
            TournamentMode.LOSER_STAYS, TournamentMode.WINNER_STAYS -> deriveRotation(state).table
            TournamentMode.SUDDEN_DEATH -> deriveSuddenDeathRotation(state).table
            TournamentMode.ROUND_ROBIN -> nextRoundRobinPair(state)
            TournamentMode.SINGLE_ELIMINATION -> currentBracketMatch(state)?.let { it.player1!! to it.player2!! }
            TournamentMode.PARTNER_ROTATION -> null
        }
    }

    /** The two 2-player teams currently facing off, or null outside PARTNER_ROTATION (or once
     * that tournament is over, or with fewer than 4 players). With exactly 4 players, all of them
     * play every round and their pairing cycles through the 3 possible combinations every 3
     * rounds: (AB vs CD), (AC vs BD), (AD vs BC). With more, [partnerRotationActivePlayers] picks
     * this round's 4 active players (see there for how waiting rotates) and the same 3-way cycle
     * applies to *that* foursome, counted across however many earlier rounds it has appeared in -
     * so a recurring group of 4 still sees every partner combination, not just AB-CD each time. */
    fun currentTeams(state: TournamentState): Pair<List<String>, List<String>>? {
        if (state.settings.mode != TournamentMode.PARTNER_ROTATION) return null
        if (state.playerIds.size < 4 || isOver(state)) return null
        val round = state.results.size / 2
        val active = partnerRotationActivePlayers(state.playerIds, round)
        val (a, b, c, d) = active
        val sameActiveCount = (0 until round).count { partnerRotationActivePlayers(state.playerIds, it) == active }
        return when (sameActiveCount % 3) {
            0 -> listOf(a, b) to listOf(c, d)
            1 -> listOf(a, c) to listOf(b, d)
            else -> listOf(a, d) to listOf(b, c)
        }
    }

    /** PARTNER_ROTATION's active foursome for a given 0-based round, out of possibly more than 4
     * players - the rest sit out that round (see [partnerRotationWaitingPlayers]). A block of
     * `playerIds.size - 4` consecutive players (in the fixed, randomized [TournamentState.playerIds]
     * order, wrapping around) waits each round, and the block's start advances by its own size
     * every round - so with 5 players one waits per round and everyone waits equally often every 5
     * rounds; with 6, two wait per round, equally often every 3 rounds; and so on. With exactly 4
     * players nobody ever waits and this simply returns all of them, unchanged every round. */
    private fun partnerRotationActivePlayers(playerIds: List<String>, round: Int): List<String> {
        val waitCount = (playerIds.size - 4).coerceAtLeast(0)
        if (waitCount == 0) return playerIds
        val n = playerIds.size
        val offset = (round * waitCount) % n
        val waitingIndices = (0 until waitCount).map { (offset + it) % n }.toSet()
        return playerIds.indices.filterNot { it in waitingIndices }.map { playerIds[it] }
    }

    /** The mirror of [partnerRotationActivePlayers]: who sits out a given round - empty with
     * exactly 4 players. */
    private fun partnerRotationWaitingPlayers(playerIds: List<String>, round: Int): List<String> {
        val waitCount = (playerIds.size - 4).coerceAtLeast(0)
        if (waitCount == 0) return emptyList()
        val n = playerIds.size
        val offset = (round * waitCount) % n
        return (0 until waitCount).map { playerIds[(offset + it) % n] }
    }

    /** The two sides of the current encounter, as player-id lists - `listOf(a) to listOf(b)` for
     * every 1v1 mode (from [currentTable]), or the two teams for PARTNER_ROTATION (from
     * [currentTeams]). Null once the tournament is over, or if there's nobody to play right now. */
    fun currentSides(state: TournamentState): Pair<List<String>, List<String>>? {
        if (state.settings.mode == TournamentMode.PARTNER_ROTATION) return currentTeams(state)
        val table = currentTable(state) ?: return null
        return listOf(table.first) to listOf(table.second)
    }

    /** (side-A games won, side-B games won) in the current, not-yet-decided encounter - null
     * once the tournament is over or there's no current encounter. Resets to (0, 0) every time
     * an encounter is decided and the next one begins. */
    fun currentEncounterScore(state: TournamentState): Pair<Int, Int>? {
        val (sideA, sideB) = currentSides(state) ?: return null
        val aWins = state.currentEncounterGames.count { it.winnerId in sideA }
        val bWins = state.currentEncounterGames.count { it.winnerId in sideB }
        return aWins to bWins
    }

    /** 1-based round number for PARTNER_ROTATION - null for every other mode, and once the
     * tournament is over. Unlike SINGLE_ELIMINATION's [currentRound] this has no "total": the
     * 3-pairing cycle just repeats until someone hits the target win count. */
    fun partnerRotationRound(state: TournamentState): Int? {
        if (state.settings.mode != TournamentMode.PARTNER_ROTATION || isOver(state)) return null
        return state.results.size / 2 + 1
    }

    /** Players not currently at the table, in a stable order. A real FIFO queue for
     * LOSER_STAYS/WINNER_STAYS/SUDDEN_DEATH; "everyone else still alive" for ROUND_ROBIN
     * (no queue concept there, just a fixed schedule) and SINGLE_ELIMINATION (no queue
     * either - the bracket alone decides who plays next). Empty for PARTNER_ROTATION with
     * exactly 4 players (everyone plays every round); with more, whoever [partnerRotationActivePlayers]
     * sat out this round (see there for the rotation rule). */
    fun waitingPlayers(state: TournamentState): List<String> {
        return when (state.settings.mode) {
            TournamentMode.ROUND_ROBIN -> {
                val table = currentTable(state)?.toList().orEmpty()
                state.playerIds.filter { it !in table }
            }
            TournamentMode.SUDDEN_DEATH -> deriveSuddenDeathRotation(state).queue
            TournamentMode.SINGLE_ELIMINATION -> {
                val table = currentTable(state)?.toList().orEmpty()
                val eliminated = state.results.map { it.loserId }.toSet()
                state.playerIds.filter { it !in table && it !in eliminated }
            }
            TournamentMode.LOSER_STAYS, TournamentMode.WINNER_STAYS -> deriveRotation(state).queue
            TournamentMode.PARTNER_ROTATION -> partnerRotationWaitingPlayers(state.playerIds, state.results.size / 2)
        }
    }

    fun winCounts(state: TournamentState): Map<String, Int> {
        val counts = state.playerIds.associateWith { 0 }.toMutableMap()
        for (result in state.results) {
            counts[result.winnerId] = (counts[result.winnerId] ?: 0) + 1
        }
        return counts
    }

    fun isOver(state: TournamentState): Boolean = when (state.settings.mode) {
        TournamentMode.LOSER_STAYS, TournamentMode.WINNER_STAYS, TournamentMode.PARTNER_ROTATION -> {
            val target = state.settings.targetWins
            target != null && winCounts(state).values.any { it >= target }
        }
        TournamentMode.SUDDEN_DEATH -> state.playerIds.size >= 2 && deriveSuddenDeathRotation(state).table == null
        TournamentMode.ROUND_ROBIN -> state.playerIds.size >= 2 && nextRoundRobinPair(state) == null
        TournamentMode.SINGLE_ELIMINATION -> state.playerIds.size >= 2 && bracket(state).championId != null
    }

    /** The champion, or null if the tournament isn't over yet, or (round robin only)
     * it ended in a tie - check [isDraw] to tell the two apart. For PARTNER_ROTATION, "champion"
     * is whichever of the (exactly one, since [isOver] requires someone to have hit the target)
     * player(s) reached the target first - both members of the winning team hit it on the same
     * game, so pick either deterministically. */
    fun championId(state: TournamentState): String? {
        if (!isOver(state)) return null
        return when (state.settings.mode) {
            TournamentMode.LOSER_STAYS, TournamentMode.WINNER_STAYS, TournamentMode.PARTNER_ROTATION -> {
                val target = state.settings.targetWins ?: return null
                winCounts(state).entries.firstOrNull { it.value >= target }?.key
            }
            TournamentMode.SUDDEN_DEATH -> deriveSuddenDeathRotation(state).resident
            TournamentMode.ROUND_ROBIN -> {
                val counts = winCounts(state)
                val max = counts.values.maxOrNull() ?: return null
                val leaders = counts.filterValues { it == max }.keys
                leaders.singleOrNull()
            }
            TournamentMode.SINGLE_ELIMINATION -> bracket(state).championId
        }
    }

    /** (current round, total rounds) for SINGLE_ELIMINATION - null for every other mode, and
     * once the tournament is over. 1-based, e.g. (1, 3) for a first-round match in an 8-player
     * bracket, (3, 3) for the final. */
    fun currentRound(state: TournamentState): Pair<Int, Int>? {
        if (state.settings.mode != TournamentMode.SINGLE_ELIMINATION || isOver(state)) return null
        val b = bracket(state)
        return (b.rounds.size) to b.totalRounds
    }

    /** Both members of the winning team, for the rare case a PARTNER_ROTATION tournament ends
     * with both of the last round's partners having reached the target win count together -
     * null otherwise (including the much more common case of a single player reaching it alone,
     * since partners rotate every round and win counts are individual - see [championId]) or for
     * every other mode, or before it's over. */
    fun championTeam(state: TournamentState): List<String>? {
        if (state.settings.mode != TournamentMode.PARTNER_ROTATION || !isOver(state)) return null
        val target = state.settings.targetWins ?: return null
        val winners = winCounts(state).filterValues { it >= target }.keys.toList()
        return winners.takeIf { it.size >= 2 }
    }

    fun isDraw(state: TournamentState): Boolean =
        state.settings.mode == TournamentMode.ROUND_ROBIN && isOver(state) && championId(state) == null

    fun recordResult(state: TournamentState, winnerId: String, loserId: String): TournamentState {
        if (isOver(state)) return state
        return state.copy(results = state.results + TournamentGameResult(winnerId, loserId))
    }

    /** PARTNER_ROTATION only: records a doubles game as two [TournamentGameResult]s (one per
     * winner-loser pairing across the teams), so both winners' win counts go up together and
     * the existing single-winner [winCounts]/[undo] machinery keeps working unchanged. */
    fun recordTeamResult(state: TournamentState, winningTeam: List<String>, losingTeam: List<String>): TournamentState {
        if (isOver(state)) return state
        val added = listOf(
            TournamentGameResult(winningTeam[0], losingTeam[0]),
            TournamentGameResult(winningTeam[1], losingTeam[1]),
        )
        return state.copy(results = state.results + added)
    }

    /**
     * The single entry point every screen should call for "this side won a game" - replaces
     * direct [recordResult]/[recordTeamResult] calls, which are now purely internal (only called
     * from here, once an encounter is actually decided). [winnerId] only needs to be *a* member of
     * the winning side (its own team for PARTNER_ROTATION) - membership, not identity, is what
     * [currentEncounterScore] tallies against.
     *
     * Appends to [TournamentState.currentEncounterGames]; once one side's tally reaches
     * [TournamentSettings.gamesPerEncounter], folds the encounter into [TournamentState.results]
     * (exactly the one/two-result shape [recordResult]/[recordTeamResult] always produced) and
     * resets the sub-tally for the next encounter. With the default `gamesPerEncounter = 1` this
     * happens on the very first call, same as the old immediate-decide behaviour.
     */
    fun recordEncounterGame(state: TournamentState, winnerId: String, loserId: String): TournamentState {
        if (isOver(state)) return state
        val (sideA, sideB) = currentSides(state) ?: return state
        val target = state.settings.gamesPerEncounter.coerceAtLeast(1)
        val updatedGames = state.currentEncounterGames + TournamentGameResult(winnerId, loserId)
        val aWins = updatedGames.count { it.winnerId in sideA }
        val bWins = updatedGames.count { it.winnerId in sideB }
        // Fold the just-played game in before possibly completing the encounter - otherwise
        // completeEncounter would tally currentEncounterGames from *before* this deciding game and
        // undercount it by exactly one (e.g. a 2:1 encounter recorded as a 1:1 tally).
        val stateWithGame = state.copy(currentEncounterGames = updatedGames)
        return when {
            aWins >= target -> completeEncounter(stateWithGame, winningSide = sideA, losingSide = sideB)
            bWins >= target -> completeEncounter(stateWithGame, winningSide = sideB, losingSide = sideA)
            else -> stateWithGame
        }
    }

    private fun completeEncounter(state: TournamentState, winningSide: List<String>, losingSide: List<String>): TournamentState {
        val winnerGames = state.currentEncounterGames.count { it.winnerId in winningSide }
        val loserGames = state.currentEncounterGames.count { it.winnerId in losingSide }
        val decided = if (state.settings.mode == TournamentMode.PARTNER_ROTATION) {
            recordTeamResult(state, winningSide, losingSide)
        } else {
            recordResult(state, winningSide[0], losingSide[0])
        }
        // recordResult/recordTeamResult append 1 or 2 entries to results (2 for PARTNER_ROTATION,
        // one per winning teammate) - mirror that here so encounterGameTallies stays index-aligned.
        val addedCount = decided.results.size - state.results.size
        return decided.copy(
            currentEncounterGames = emptyList(),
            encounterGameTallies = state.encounterGameTallies + List(addedCount) { winnerGames to loserGames },
        )
    }

    /** Cumulative individual-game score (games won, games lost) per player across the whole
     * tournament - distinct from [winCounts], which counts *encounters* won (worth exactly one
     * tournament point regardless of [TournamentSettings.gamesPerEncounter]). Only meaningful when
     * gamesPerEncounter > 1; the caller decides whether it's worth showing. Includes the current,
     * not-yet-decided encounter's sub-tally too, so it updates live as games are recorded. */
    fun gameTally(state: TournamentState): Map<String, Pair<Int, Int>> {
        val tally = state.playerIds.associateWith { 0 to 0 }.toMutableMap()
        fun add(id: String, won: Int, lost: Int) {
            val (w, l) = tally[id] ?: (0 to 0)
            tally[id] = (w + won) to (l + lost)
        }
        state.results.zip(state.encounterGameTallies).forEach { (result, gamesTally) ->
            val (winnerGames, loserGames) = gamesTally
            add(result.winnerId, winnerGames, loserGames)
            add(result.loserId, loserGames, winnerGames)
        }
        if (state.currentEncounterGames.isNotEmpty()) {
            val (sideA, sideB) = currentSides(state) ?: (emptyList<String>() to emptyList())
            val aWins = state.currentEncounterGames.count { it.winnerId in sideA }
            val bWins = state.currentEncounterGames.count { it.winnerId in sideB }
            sideA.forEach { add(it, aWins, bWins) }
            sideB.forEach { add(it, bWins, aWins) }
        }
        return tally
    }

    /** The (wins, game differential, game losses) triple standings are ordered and ranked by -
     * see [standingsOrder]/[standingsRanks]. Two players compare equal (and so share a rank) only
     * when all three match, i.e. their individual-game score is literally identical, not just its
     * differential. */
    private fun standingsKey(id: String, wins: Map<String, Int>, tally: Map<String, Pair<Int, Int>>): Triple<Int, Int, Int> {
        val (gamesWon, gamesLost) = tally[id] ?: (0 to 0)
        return Triple(wins[id] ?: 0, gamesWon - gamesLost, gamesLost)
    }

    /** Standings order: most encounter wins first (see [winCounts]) - ties broken by individual-
     * game differential (only meaningful when [TournamentSettings.gamesPerEncounter] > 1; always
     * zero otherwise, so this tiebreak is a no-op there), then by fewer games lost (e.g. a 3:4
     * record ranks above an equal-differential 4:5 one). Players tied on all three (see
     * [standingsKey]) keep their existing relative order (stable sort) - [standingsRanks] is what
     * gives them the same displayed rank number. */
    fun standingsOrder(state: TournamentState): List<String> {
        val wins = winCounts(state)
        val tally = gameTally(state)
        // Triple isn't Comparable, so sort via an explicit comparator chain instead of
        // sortedByDescending(::standingsKey) - same three criteria, same priority order.
        return state.playerIds.sortedWith(
            compareByDescending<String> { wins[it] ?: 0 }
                .thenByDescending { val (w, l) = tally[it] ?: (0 to 0); w - l }
                .thenBy { val (_, l) = tally[it] ?: (0 to 0); l },
        )
    }

    /** 1-based "competition ranking" (1-2-2-4, not 1-2-2-3) for every player in [standingsOrder] -
     * players whose [standingsKey] is identical share a rank, and the rank after a tied group
     * jumps to its size plus the previous rank, matching how sports standings usually number ties. */
    fun standingsRanks(state: TournamentState): Map<String, Int> {
        val order = standingsOrder(state)
        val wins = winCounts(state)
        val tally = gameTally(state)
        val ranks = mutableMapOf<String, Int>()
        var rank = 1
        order.forEachIndexed { index, id ->
            if (index > 0 && standingsKey(id, wins, tally) != standingsKey(order[index - 1], wins, tally)) {
                rank = index + 1
            }
            ranks[id] = rank
        }
        return ranks
    }

    /** Whether the given side (0 or 1, matching [currentSides]' pair order) can have their own
     * last sub-game in the current, in-progress encounter taken back - true only if that side won
     * it, same "undo only your own last action" rule [info.rbuck.billiardscoreboard.ui.simplematch.SimpleMatchScreen]'s
     * per-player remove button follows. */
    fun canRemoveLastEncounterGameFor(state: TournamentState, side: Int): Boolean {
        val last = state.currentEncounterGames.lastOrNull() ?: return false
        val (sideA, sideB) = currentSides(state) ?: return false
        return last.winnerId in (if (side == 0) sideA else sideB)
    }

    fun removeLastEncounterGameFor(state: TournamentState, side: Int): TournamentState {
        if (!canRemoveLastEncounterGameFor(state, side)) return state
        return state.copy(currentEncounterGames = state.currentEncounterGames.dropLast(1))
    }

    /** Steps back one game at a time: first empties any in-progress encounter's sub-tally (back
     * to 0:0), then - once there's nothing left to step back within the current encounter - drops
     * the last *completed* encounter from [TournamentState.results], un-rotating the table/bracket/
     * schedule right along with it. */
    fun undo(state: TournamentState): TournamentState {
        if (state.currentEncounterGames.isNotEmpty()) {
            return state.copy(currentEncounterGames = state.currentEncounterGames.dropLast(1))
        }
        if (state.results.isEmpty()) return state
        val dropCount = if (state.settings.mode == TournamentMode.PARTNER_ROTATION) 2 else 1
        val n = dropCount.coerceAtMost(state.results.size)
        return state.copy(
            results = state.results.dropLast(n),
            encounterGameTallies = state.encounterGameTallies.dropLast(n),
        )
    }

    fun canUndo(state: TournamentState): Boolean = state.currentEncounterGames.isNotEmpty() || state.results.isNotEmpty()

    /**
     * "Circle method" round-robin schedule: fix the first player, rotate the rest one
     * step each round. Pads with a bye if the count is odd (dropped from the returned
     * pairs). Purely a function of [playerIds] - never persisted, always recomputed.
     */
    fun roundRobinSchedule(playerIds: List<String>): List<Pair<String, String>> {
        if (playerIds.size < 2) return emptyList()
        val ids = if (playerIds.size % 2 == 1) playerIds + BYE else playerIds
        val fixed = ids[0]
        val rotating = ids.drop(1).toMutableList()
        val schedule = mutableListOf<Pair<String, String>>()
        repeat(ids.size - 1) {
            val roundPairs = mutableListOf(fixed to rotating[0])
            var i = 1
            var j = rotating.size - 1
            while (i < j) {
                roundPairs += rotating[i] to rotating[j]
                i++
                j--
            }
            schedule += roundPairs.filterNot { (a, b) -> a == BYE || b == BYE }
            rotating.add(0, rotating.removeAt(rotating.size - 1))
        }
        return schedule
    }

    private fun nextRoundRobinPair(state: TournamentState): Pair<String, String>? {
        val played = state.results.map { setOf(it.winnerId, it.loserId) }.toSet()
        return roundRobinSchedule(state.playerIds).firstOrNull { (a, b) -> setOf(a, b) !in played }
    }

    /**
     * [table] is always `resident to challenger`: [resident] is whoever is "staying"
     * under the tournament's rule (the defending loser in LOSER_STAYS, the reigning
     * winner in WINNER_STAYS) - so the same player always renders in the same slot
     * across turns instead of flipping between "table position 1/2" depending on
     * where they happened to be sitting.
     */
    private data class Rotation(val table: Pair<String, String>?, val queue: List<String>)

    private fun deriveRotation(state: TournamentState): Rotation {
        val ids = state.playerIds
        // Nobody has "stayed" yet for the very first game - arbitrarily anchor resident
        // to playerIds[0] (the randomized order already makes this a fair pick).
        var resident = ids[0]
        var challenger = ids[1]
        val queue = ArrayDeque(ids.drop(2))

        for (result in state.results) {
            val staying = if (state.settings.mode == TournamentMode.LOSER_STAYS) result.loserId else result.winnerId
            val leaving = if (staying == result.winnerId) result.loserId else result.winnerId
            queue.addLast(leaving)
            resident = staying
            challenger = queue.removeFirstOrNull() ?: return Rotation(null, queue.toList())
        }
        return Rotation(resident to challenger, queue.toList())
    }

    /** Same shape as [Rotation], but for SUDDEN_DEATH: [resident] is always the last game's
     * winner (there's no "loser stays" variant of this mode), and a loser is simply dropped -
     * never re-added to [queue] - since one loss eliminates a player for good. */
    private data class SuddenDeathRotation(val table: Pair<String, String>?, val queue: List<String>, val resident: String)

    private fun deriveSuddenDeathRotation(state: TournamentState): SuddenDeathRotation {
        val ids = state.playerIds
        var resident = ids[0]
        val queue = ArrayDeque(ids.drop(1))
        var challenger = queue.removeFirstOrNull()
        for (result in state.results) {
            resident = result.winnerId
            challenger = queue.removeFirstOrNull()
        }
        val table = challenger?.let { resident to it }
        return SuddenDeathRotation(table, queue.toList(), resident)
    }

    /** One bracket match: the two seeded/advanced players (null = "not decided yet", e.g. still
     * waiting on an earlier round), and the winner once played (auto-filled for a bye). */
    private data class BracketMatch(val player1: String?, val player2: String?, val winner: String?) {
        val isBye: Boolean get() = player1 == null || player2 == null
        val isPending: Boolean get() = player1 != null && player2 != null && winner == null
    }

    private class BracketState(val rounds: List<List<BracketMatch>>, val totalRounds: Int) {
        val championId: String? = rounds.lastOrNull()?.singleOrNull()?.winner
    }

    /**
     * Seeds [playerIds] into a single-elimination bracket: padded to the next power of two with
     * byes, byes assigned one per first-round match (never bye-vs-bye - always possible since a
     * mode with N players and P = next power of two >= N always has fewer byes (P-N) than
     * first-round matches (P/2), because N > P/2 by definition of "next power of two"). Purely a
     * function of [playerIds] (already randomized once at tournament creation) - never persisted,
     * always recomputed, same approach as [roundRobinSchedule].
     */
    private fun singleEliminationBracket(playerIds: List<String>): List<Pair<String?, String?>> {
        if (playerIds.size < 2) return emptyList()
        var size = 1
        while (size < playerIds.size) size *= 2
        val byes = size - playerIds.size
        val slots = arrayOfNulls<String>(size)
        var nextPlayer = 0
        var byesLeft = byes
        var i = 0
        while (i < size) {
            slots[i] = playerIds[nextPlayer++]
            slots[i + 1] = if (byesLeft > 0) { byesLeft--; null } else playerIds[nextPlayer++]
            i += 2
        }
        return slots.toList().chunked(2).map { it[0] to it[1] }
    }

    /** Replays [state.results] through the bracket seeded from [state.playerIds], round by
     * round, stopping at the first round with any match still undecided (the "frontier" -
     * that round is always the last entry in [BracketState.rounds]). */
    private fun bracket(state: TournamentState): BracketState {
        var totalSlots = 1
        while (totalSlots < state.playerIds.size) totalSlots *= 2
        var totalRounds = 0
        var n = totalSlots
        while (n > 1) { n /= 2; totalRounds++ }

        val rounds = mutableListOf<List<BracketMatch>>()
        var resultIdx = 0
        var currentSlots: List<String?> = singleEliminationBracket(state.playerIds).flatMap { listOf(it.first, it.second) }

        while (currentSlots.size >= 2) {
            val matches = currentSlots.chunked(2).map { (a, b) ->
                val winner = when {
                    a == null -> b
                    b == null -> a
                    resultIdx < state.results.size && setOf(state.results[resultIdx].winnerId, state.results[resultIdx].loserId) == setOf(a, b) ->
                        state.results[resultIdx++].winnerId
                    else -> null
                }
                BracketMatch(a, b, winner)
            }
            rounds.add(matches)
            if (matches.any { it.winner == null }) break
            currentSlots = matches.map { it.winner }
        }
        return BracketState(rounds, totalRounds)
    }

    /** The next real (non-bye) match to play, or null if none is pending right now (either the
     * tournament is over, or - impossible in practice since byes auto-resolve - a round is
     * fully byes). */
    private fun currentBracketMatch(state: TournamentState): BracketMatch? =
        bracket(state).rounds.lastOrNull()?.firstOrNull { it.isPending }
}
