package info.rbuck.billiardscoreboard.i18n

import info.rbuck.billiardscoreboard.domain.tournament.TournamentMode
import info.rbuck.billiardscoreboard.domain.training.TrainingExercise

/** Every fixed (non-templated) string on the tournament screens - see [Translations.tournamentText]. */
enum class TournamentTextKey {
    NEW_TOURNAMENT_TITLE, ADD_PLAYER, MODE, TARGET_WINS, START,
    TOURNAMENT_TITLE, TABLE_NOW, STAYING_LOST_LAST, STAYING_WON_LAST, CHALLENGER,
    WAITING, DRAW, WINS_TOURNAMENT, TOURNAMENT_FINISHED,
    SAVE_AND_FINISH, SAVE_AND_REMATCH, STANDINGS,
    TOURNAMENTS_TITLE, NO_TOURNAMENTS_YET, CHAMPION, IN_PROGRESS,
    DELETE_TOURNAMENT_TITLE, DELETE_TOURNAMENT_MESSAGE, DELETE,
    ROUND, ROUND_NUMBER, TEAM_WINS_TOURNAMENT, GAMES_PER_ENCOUNTER,
    LEAVE_TOURNAMENT_TITLE, LEAVE_TOURNAMENT_MESSAGE, LEAVE,
    STARTING_ORDER, ORDER_AS_LISTED, ORDER_RANDOM, STARTING_ORDER_HINT,
}

/** Every fixed (non-templated, or "%s"/"%d"-templated) string on the 14.1 straight-pool scoreboard
 * screen - see [Translations.straightMatchText]. */
enum class StraightMatchTextKey {
    BREAK_FOUL_TITLE, BREAK_FOUL_QUESTION, YES, NO_NORMAL_FOUL,
    REBREAK_TITLE, REBREAK_EXPLANATION, REQUIRE_REBREAK, OPPONENT_TAKES_TABLE,
    THIRD_FOUL_TITLE, THIRD_FOUL_EXPLANATION, THIRD_FOUL_CONFIRM, NORMAL_FOUL,
    RERACK_TITLE, BALLS_REMAINING_QUESTION, RERACK_ACTION,
    BALLS_ON_TABLE_TITLE, SET_ACTION,
    MATCH_HISTORY_TITLE, CLOSE,
    DISCARD_MATCH_TITLE, DISCARD_MATCH_MESSAGE, DISCARD,
    RACE_TO, RACE_TO_INNINGS_LIMIT,
    MATCH_WON_BY, MATCH_DRAW, MATCH_FINISHED,
    WINS_SHORT, DRAW_SHORT, FINISHED_SHORT,
    SAVE_AND_FINISH, SAVE_AND_REMATCH, REMATCH,
    /** Single-letter abbreviation for "Inning" used in stat lines and table headers (e.g. "I:5",
     * the history table's "I" column) - German uses "A" for "Aufnahme", not "I". */
    INNING_ABBREV,
}

/** Every fixed (non-templated, or "%d"/"%s"-templated) string on the "New Training" setup screen
 * (exercise picker) - see [Translations.trainingSetupText]. Exercise names themselves ("14.1
 * HighRun", "Equal Offense Level N") stay untranslated - only the surrounding UI and each card's
 * detail line are translated. */
enum class TrainingSetupTextKey {
    PLAYER, OPEN_ENDED, INNINGS_TARGET, REF_SUFFIX,
}

/**
 * Translated copy for the app's translated UI text (Training's per-exercise rules, the OBS WebSocket
 * consent notice, the 14.1 rebuild-rules info dialog, and the Tournament screens) - the only UI text
 * the app translates.
 * [AppLanguage.EN] always falls back to the English text that already lives at the call site (e.g.
 * [TrainingExercise.ruleDescription]), so there's a single source of truth for English and no
 * duplicate copy to keep in sync.
 */
object Translations {

    fun trainingRule(exercise: TrainingExercise, language: AppLanguage): String =
        trainingRules[language]?.get(exercise) ?: exercise.ruleDescription

    /** Consent reminder shown under the OBS WebSocket toggle - this feature publishes player names to the
     * LAN, normally for streaming. Null for English - callers fall back to the hardcoded English text. */
    fun obsWebSocketPrivacyNotice(language: AppLanguage): String? = obsWebSocketPrivacyNotices[language]

    /** Explanation of the source-prefix field, shown under it in Settings. Null for English - callers
     * fall back to the hardcoded English text. */
    fun obsWebSocketSourcePrefixHint(language: AppLanguage): String? = obsWebSocketSourcePrefixHints[language]

    /** Title of the 14.1 "rebuild situations" info dialog. Null for English - caller falls back to
     * the hardcoded English text. */
    fun rebuildRulesTitle(language: AppLanguage): String? = rebuildRulesTitles[language]

    /** "Example" label prefixed to each rebuild-rules example's number (e.g. "Beispiel 3"). Null for
     * English - caller falls back to the hardcoded "Example" text. */
    fun rebuildRulesExampleLabel(language: AppLanguage): String? = rebuildRulesExampleLabels[language]

    /** "of" in "Example N of 7", shown next to the rebuild-rules pagination controls. Null for
     * English - caller falls back to the hardcoded "of" text. */
    fun rebuildRulesOfCount(language: AppLanguage): String? = rebuildRulesOfCounts[language]

    /** One rebuild-rules example's caption, keyed by example number (1-7) and part (1 or 2 - the
     * "before"/"after" diagram). Null for English - caller falls back to the hardcoded English text. */
    fun rebuildRulesCaption(exampleNumber: Int, part: Int, language: AppLanguage): String? =
        rebuildRulesCaptions[language]?.get(exampleNumber to part)

    /** A tournament mode's short chip/label text (e.g. "Loser stays"). Null for English - caller
     * falls back to the hardcoded English text. */
    fun tournamentModeLabel(mode: TournamentMode, language: AppLanguage): String? =
        tournamentModeLabels[language]?.get(mode)

    /** A tournament mode's one-line rule explanation, shown under the mode picker. Null for
     * English - caller falls back to the hardcoded English text. */
    fun tournamentModeDescription(mode: TournamentMode, language: AppLanguage): String? =
        tournamentModeDescriptions[language]?.get(mode)

    /** A fixed (non-templated) tournament-screen string. Null for English - caller falls back to
     * the hardcoded English text. */
    fun tournamentText(key: TournamentTextKey, language: AppLanguage): String? =
        tournamentTexts[language]?.get(key)

    /** A fixed (non-templated, or "%s"/"%d"-templated) 14.1 scoreboard string. Null for English -
     * caller falls back to the hardcoded English text. */
    fun straightMatchText(key: StraightMatchTextKey, language: AppLanguage): String? =
        straightMatchTexts[language]?.get(key)

    /** A fixed (non-templated, or "%d"/"%s"-templated) "New Training" setup-screen string. Null for
     * English - caller falls back to the hardcoded English text. */
    fun trainingSetupText(key: TrainingSetupTextKey, language: AppLanguage): String? =
        trainingSetupTexts[language]?.get(key)

    private val trainingRules: Map<AppLanguage, Map<TrainingExercise, String>> = mapOf(
        AppLanguage.DE to mapOf(
            TrainingExercise.HIGH_RUN to "Spiele durchgehend 14.1: Rack für Rack, ohne Aufnahmenlimit und ohne Leben. " +
                "Versenke so viele Bälle in Folge wie möglich - das Ziel ist einfach deine längste Serie. Deine " +
                "aktuelle Serie und die Höchstserie der Sitzung werden unten auf der Kachel angezeigt.",
            TrainingExercise.EQUAL_OFFENSE_1 to "Aufbau: Die 15 Bälle werden als geschlossenes Dreieck aufgebaut, " +
                "dann wird aus dem Kopffeld gebreakt - der Break zählt nicht als normaler Stoß, versenkte Bälle " +
                "werden auf der Fußlinie wieder aufgebaut, sodass nach dem Break immer alle 15 Bälle auf dem Tisch " +
                "liegen. Du beginnst mit Weiß in der Hand auf dem ganzen Tisch. Equal Offense ist ein Ansagespiel " +
                "(Ball und Loch ansagen, außer bei offensichtlichen Kugeln).\n\n" +
                "10 Aufnahmen, jeweils ein Rack mit 15 Bällen. Du hast 2 Leben pro Aufnahme - sie endet erst beim " +
                "dritten Fehler oder Foul. Nach einem Fehler oder Foul spielst du von der aktuellen Position der " +
                "Weißen weiter (außer sie fällt oder verlässt den Tisch - dann wieder Weiß in der Hand auf dem " +
                "ganzen Tisch). Ziel ist ein Sitzungstotal von 120 Punkten oder mehr - ein Schnitt von 4,0 pro " +
                "Aufnahme ist der Referenzwert.",
            TrainingExercise.EQUAL_OFFENSE_2 to "Aufbau: Die 15 Bälle werden als geschlossenes Dreieck aufgebaut, " +
                "dann wird aus dem Kopffeld gebreakt - der Break zählt nicht als normaler Stoß, versenkte Bälle " +
                "werden auf der Fußlinie wieder aufgebaut, sodass nach dem Break immer alle 15 Bälle auf dem Tisch " +
                "liegen. Du beginnst mit Weiß in der Hand auf dem ganzen Tisch. Equal Offense ist ein Ansagespiel " +
                "(Ball und Loch ansagen, außer bei offensichtlichen Kugeln).\n\n" +
                "10 Aufnahmen, jeweils ein Rack mit 15 Bällen. Du hast 1 Leben pro Aufnahme - sie endet nach dem " +
                "zweiten Fehler oder Foul. Nach einem Fehler oder Foul spielst du von der aktuellen Position der " +
                "Weißen weiter (außer sie fällt oder verlässt den Tisch - dann wieder Weiß in der Hand auf dem " +
                "ganzen Tisch). Ziel ist ein Sitzungstotal von 120 Punkten oder mehr - ein Schnitt von 6,0 pro " +
                "Aufnahme ist der Referenzwert.",
            TrainingExercise.EQUAL_OFFENSE_3 to "Aufbau: Die 15 Bälle werden als geschlossenes Dreieck aufgebaut, " +
                "dann wird aus dem Kopffeld gebreakt - der Break zählt nicht als normaler Stoß, versenkte Bälle " +
                "werden auf der Fußlinie wieder aufgebaut, sodass nach dem Break immer alle 15 Bälle auf dem Tisch " +
                "liegen. Du beginnst mit Weiß in der Hand auf dem ganzen Tisch. Equal Offense ist ein Ansagespiel " +
                "(Ball und Loch ansagen, außer bei offensichtlichen Kugeln).\n\n" +
                "10 Aufnahmen, jeweils ein Rack mit 15 Bällen. Keine Leben - ein einziger Fehler oder Foul beendet " +
                "die Aufnahme sofort. Nach einem Fehler oder Foul spielst du von der aktuellen Position der " +
                "Weißen weiter (außer sie fällt oder verlässt den Tisch - dann wieder Weiß in der Hand auf dem " +
                "ganzen Tisch). Ziel ist ein Sitzungstotal von 120 Punkten oder mehr - ein Schnitt von 12,0 pro " +
                "Aufnahme ist der Referenzwert.",
            TrainingExercise.EQUAL_OFFENSE_4 to "Aufbau: Hier wird echtes 14.1 gespielt - nach dem Break gibt es " +
                "Weiß in der Hand nur im Kopffeld, kein automatischer Wiederaufbau der versenkten Bälle. Equal " +
                "Offense bleibt ein Ansagespiel (Ball und Loch ansagen, außer bei offensichtlichen Kugeln).\n\n" +
                "10 Aufnahmen, keine Leben - ein einziger Fehler oder Foul beendet die Aufnahme sofort. Sobald du " +
                "14 Bälle erreichst, rackt der Rack-Button den verbleibenden Ball als Anspielball, statt alle 15 " +
                "neu aufzubauen, und deine Zählung läuft über 14 hinaus weiter (\"14+1\", \"14+2\", ...) bis zu " +
                "20 Bällen pro Aufnahme. Ziel ist ein Sitzungstotal von 170 Punkten oder mehr - ein Schnitt von " +
                "17,0 pro Aufnahme ist der Referenzwert.",
        ),
        AppLanguage.ES to mapOf(
            TrainingExercise.HIGH_RUN to "Juega al 14.1 de forma continua: rack tras rack, sin límite de entradas " +
                "y sin vidas. Entroniza tantas bolas seguidas como puedas - el objetivo es simplemente tu tacada " +
                "más larga. Tu tacada actual y la mejor tacada de la sesión se muestran en la parte inferior de " +
                "la tarjeta.",
            TrainingExercise.EQUAL_OFFENSE_1 to "Preparación: se colocan las 15 bolas en triángulo cerrado y se " +
                "rompe desde la zona de salida - el golpe de rotura no cuenta como tiro normal, y las bolas que " +
                "entronice se vuelven a colocar en el punto de pie, de modo que tras la rotura siempre quedan " +
                "las 15 bolas en la mesa. Empiezas tu intento con bola en mano en toda la mesa. Equal Offense es " +
                "un juego de bola cantada (cantar bola y tronera, salvo en tiros evidentes).\n\n" +
                "10 entradas, un rack de 15 bolas cada una. Tienes 2 vidas por entrada - solo termina antes si " +
                "fallas 3 veces (2 vidas perdidas + el fallo que la termina). Tras un fallo o falta, sigues " +
                "jugando desde la posición actual de la bola blanca (salvo que se embolse o salga de la mesa - " +
                "entonces bola en mano de nuevo en toda la mesa). El objetivo es un total de sesión de 120 " +
                "puntos o más - una media de 4.0 por entrada es la referencia a superar.",
            TrainingExercise.EQUAL_OFFENSE_2 to "Preparación: se colocan las 15 bolas en triángulo cerrado y se " +
                "rompe desde la zona de salida - el golpe de rotura no cuenta como tiro normal, y las bolas que " +
                "entronice se vuelven a colocar en el punto de pie, de modo que tras la rotura siempre quedan " +
                "las 15 bolas en la mesa. Empiezas tu intento con bola en mano en toda la mesa. Equal Offense es " +
                "un juego de bola cantada (cantar bola y tronera, salvo en tiros evidentes).\n\n" +
                "10 entradas, un rack de 15 bolas cada una. Tienes 1 vida por entrada - termina tras tu segundo " +
                "fallo o falta. Tras un fallo o falta, sigues jugando desde la posición actual de la bola blanca " +
                "(salvo que se embolse o salga de la mesa - entonces bola en mano de nuevo en toda la mesa). El " +
                "objetivo es un total de sesión de 120 puntos o más - una media de 6.0 por entrada es la " +
                "referencia a superar.",
            TrainingExercise.EQUAL_OFFENSE_3 to "Preparación: se colocan las 15 bolas en triángulo cerrado y se " +
                "rompe desde la zona de salida - el golpe de rotura no cuenta como tiro normal, y las bolas que " +
                "entronice se vuelven a colocar en el punto de pie, de modo que tras la rotura siempre quedan " +
                "las 15 bolas en la mesa. Empiezas tu intento con bola en mano en toda la mesa. Equal Offense es " +
                "un juego de bola cantada (cantar bola y tronera, salvo en tiros evidentes).\n\n" +
                "10 entradas, un rack de 15 bolas cada una. Sin vidas - un solo fallo o falta termina la entrada " +
                "de inmediato. Tras un fallo o falta, sigues jugando desde la posición actual de la bola blanca " +
                "(salvo que se embolse o salga de la mesa - entonces bola en mano de nuevo en toda la mesa). El " +
                "objetivo es un total de sesión de 120 puntos o más - una media de 12.0 por entrada es la " +
                "referencia a superar.",
            TrainingExercise.EQUAL_OFFENSE_4 to "Preparación: este nivel se juega como 14.1 real - tras la " +
                "rotura, la bola en mano es solo dentro de la zona de salida, sin recolocación automática de las " +
                "bolas embolsadas. Equal Offense sigue siendo un juego de bola cantada (cantar bola y tronera, " +
                "salvo en tiros evidentes).\n\n" +
                "10 entradas, sin vidas - un solo fallo o falta termina la entrada de inmediato. Al llegar a 14 " +
                "bolas, el botón de rack coloca la bola restante como bola de rompida en lugar de volver a " +
                "colocar las 15, y tu conteo continúa más allá de 14 (\"14+1\", \"14+2\", ...) hasta 20 bolas " +
                "por entrada. El objetivo es un total de sesión de 170 puntos o más - una media de 17.0 por " +
                "entrada es la referencia a superar.",
        ),
        AppLanguage.FR to mapOf(
            TrainingExercise.HIGH_RUN to "Jouez au 14.1 en continu : rack après rack, sans limite de reprises et " +
                "sans vies. Rentrez autant de billes d'affilée que possible - l'objectif est simplement votre " +
                "plus longue série. Votre série en cours et la meilleure série de la session s'affichent en bas " +
                "de la carte.",
            TrainingExercise.EQUAL_OFFENSE_1 to "Préparation : les 15 billes sont montées en triangle fermé, " +
                "puis on casse depuis la zone de tête - le coup de casse ne compte pas comme un coup normal, et " +
                "les billes qu'il rentre sont replacées sur le point de pied, si bien qu'après la casse les 15 " +
                "billes sont toujours sur la table. Vous commencez votre tentative avec la bille en main sur " +
                "toute la table. Equal Offense est un jeu annoncé (annoncer bille et poche, sauf évidence).\n\n" +
                "10 reprises, un rack de 15 billes chacune. Vous avez 2 vies par reprise - elle ne se termine " +
                "qu'après 3 fautes. Après une faute, vous continuez depuis la position actuelle de la bille " +
                "blanche (sauf si elle est rentrée ou sort de la table - alors de nouveau bille en main sur " +
                "toute la table). Visez un total de session de 120 points ou plus - une moyenne de 4,0 par " +
                "reprise est la référence à battre.",
            TrainingExercise.EQUAL_OFFENSE_2 to "Préparation : les 15 billes sont montées en triangle fermé, " +
                "puis on casse depuis la zone de tête - le coup de casse ne compte pas comme un coup normal, et " +
                "les billes qu'il rentre sont replacées sur le point de pied, si bien qu'après la casse les 15 " +
                "billes sont toujours sur la table. Vous commencez votre tentative avec la bille en main sur " +
                "toute la table. Equal Offense est un jeu annoncé (annoncer bille et poche, sauf évidence).\n\n" +
                "10 reprises, un rack de 15 billes chacune. Vous avez 1 vie par reprise - elle se termine après " +
                "votre deuxième faute. Après une faute, vous continuez depuis la position actuelle de la bille " +
                "blanche (sauf si elle est rentrée ou sort de la table - alors de nouveau bille en main sur " +
                "toute la table). Visez un total de session de 120 points ou plus - une moyenne de 6,0 par " +
                "reprise est la référence à battre.",
            TrainingExercise.EQUAL_OFFENSE_3 to "Préparation : les 15 billes sont montées en triangle fermé, " +
                "puis on casse depuis la zone de tête - le coup de casse ne compte pas comme un coup normal, et " +
                "les billes qu'il rentre sont replacées sur le point de pied, si bien qu'après la casse les 15 " +
                "billes sont toujours sur la table. Vous commencez votre tentative avec la bille en main sur " +
                "toute la table. Equal Offense est un jeu annoncé (annoncer bille et poche, sauf évidence).\n\n" +
                "10 reprises, un rack de 15 billes chacune. Aucune vie - une seule faute termine immédiatement " +
                "la reprise. Après une faute, vous continuez depuis la position actuelle de la bille blanche " +
                "(sauf si elle est rentrée ou sort de la table - alors de nouveau bille en main sur toute la " +
                "table). Visez un total de session de 120 points ou plus - une moyenne de 12,0 par reprise est " +
                "la référence à battre.",
            TrainingExercise.EQUAL_OFFENSE_4 to "Préparation : ce niveau se joue en 14.1 réel - après la casse, " +
                "la bille en main n'est valable que dans la zone de tête, sans replacement automatique des " +
                "billes rentrées. Equal Offense reste un jeu annoncé (annoncer bille et poche, sauf évidence).\n\n" +
                "10 reprises, aucune vie - une seule faute termine immédiatement la reprise. Une fois 14 billes " +
                "atteintes, le bouton de rack replace la bille restante comme bille de casse au lieu de reformer " +
                "les 15, et votre compte continue au-delà de 14 (\"14+1\", \"14+2\", ...) jusqu'à 20 billes par " +
                "reprise. Visez un total de session de 170 points ou plus - une moyenne de 17,0 par reprise est " +
                "la référence à battre.",
        ),
    )

    private val obsWebSocketPrivacyNotices: Map<AppLanguage, String> = mapOf(
        AppLanguage.DE to "Solange aktiv, werden Spielernamen und Vereine im lokalen Netzwerk bereitgestellt - " +
            "meist zur Übertragung in einem Stream. Stelle sicher, dass die gezeigten Spieler damit " +
            "einverstanden sind.",
        AppLanguage.ES to "Mientras esté activo, los nombres de los jugadores y sus clubes se publican en la red " +
            "local, normalmente para retransmitirlos. Asegúrate de que los jugadores mostrados estén de acuerdo.",
        AppLanguage.FR to "Tant que cette option est active, les noms des joueurs et leurs clubs sont diffusés sur " +
            "le réseau local, généralement pour une retransmission. Assurez-vous que les joueurs affichés y consentent.",
    )

    private val obsWebSocketSourcePrefixHints: Map<AppLanguage, String> = mapOf(
        AppLanguage.DE to "Nur nötig, wenn mehrere Tablets in dieselbe OBS-Instanz schreiben (z. B. mehrere Tische " +
            "in einer Szene). Jedes Tablet braucht dann einen eigenen Wert (z. B. »tisch1«, »tisch2«) und in OBS " +
            "einen eigenen Satz Textquellen mit diesem Präfix (z. B. »tisch1_name1« statt »name1«). Leer lassen " +
            "bei nur einem Tisch.",
        AppLanguage.ES to "Solo necesario si varias tablets escriben en la misma instancia de OBS (por ejemplo, " +
            "varias mesas en una misma escena). Cada tablet necesita entonces su propio valor (por ejemplo, " +
            "»mesa1«, »mesa2«) y en OBS un conjunto propio de fuentes de texto con ese prefijo (por ejemplo, " +
            "»mesa1_name1« en lugar de »name1«). Déjalo vacío si solo hay una mesa.",
        AppLanguage.FR to "Nécessaire uniquement si plusieurs tablettes écrivent dans la même instance OBS (par " +
            "exemple plusieurs tables dans une même scène). Chaque tablette a alors besoin de sa propre valeur " +
            "(par exemple »table1«, »table2«) et OBS d'un jeu de sources de texte propre avec ce préfixe (par " +
            "exemple »table1_name1« au lieu de »name1«). Laissez vide s'il n'y a qu'une seule table.",
    )

    /** DBU rule 4.8 "Situationen beim Wiederaufbau" - the DE text is transcribed verbatim from
     * "Spielregeln Pool" (Stand 07/2016), Anlage 1, Beispiel 1-7; ES/FR are translations of that
     * original, not of the English fallback, to stay as close as possible to the official wording. */
    private val rebuildRulesTitles: Map<AppLanguage, String> = mapOf(
        AppLanguage.DE to "Situationen beim Wiederaufbau",
        AppLanguage.ES to "Situaciones al reconstruir el triángulo",
        AppLanguage.FR to "Situations de remontage du triangle",
    )

    private val rebuildRulesExampleLabels: Map<AppLanguage, String> = mapOf(
        AppLanguage.DE to "Beispiel",
        AppLanguage.ES to "Ejemplo",
        AppLanguage.FR to "Exemple",
    )

    private val rebuildRulesOfCounts: Map<AppLanguage, String> = mapOf(
        AppLanguage.DE to "von",
        AppLanguage.ES to "de",
        AppLanguage.FR to "sur",
    )

    private val rebuildRulesCaptions: Map<AppLanguage, Map<Pair<Int, Int>, String>> = mapOf(
        AppLanguage.DE to mapOf(
            (1 to 1) to "Die 14. und 15. Kugel wurden versenkt.",
            (1 to 2) to "Das Dreieck wird komplett neu aufgebaut. Die Weiße bleibt liegen.",
            (2 to 1) to "Die 15. Kugel behindert den Aufbau; die Weiße liegt irgendwo auf dem Tisch.",
            (2 to 2) to "Die 15. Kugel kommt auf den Kopfpunkt. Die Weiße bleibt liegen.",
            (3 to 1) to "Die 15. Kugel behindert den Aufbau; die Weiße blockiert den Kopfpunkt.",
            (3 to 2) to "Die 15. Kugel kommt auf den Mittelpunkt. Die Weiße bleibt liegen.",
            (4 to 1) to "Die Weiße und die 15. Kugel behindern beide den Aufbau.",
            (4 to 2) to "Das Dreieck wird komplett neu aufgebaut. Die Weiße wird irgendwo aus dem Kopffeld gespielt.",
            (5 to 1) to "Die Weiße behindert den Aufbau; die 15. Kugel liegt nicht im Kopffeld.",
            (5 to 2) to "Die Weiße kann irgendwo im Kopffeld plaziert werden. Die 15. Kugel bleibt liegen.",
            (6 to 1) to "Die Weiße behindert den Aufbau; die 15. Kugel liegt im Kopffeld.",
            (6 to 2) to "Die Weiße wird auf den Kopfpunkt gesetzt und darf in jede beliebige Richtung gespielt " +
                "werden. Die 15. Kugel bleibt liegen.",
            (7 to 1) to "Die Weiße behindert den Aufbau; die 15. Kugel blockiert den Kopfpunkt.",
            (7 to 2) to "Die Weiße wird auf den Mittelpunkt gesetzt. Die 15. Kugel bleibt liegen.",
        ),
        AppLanguage.ES to mapOf(
            (1 to 1) to "Las bolas 14 y 15 han sido embocadas.",
            (1 to 2) to "El triángulo se reconstruye por completo. La bola blanca se queda donde está.",
            (2 to 1) to "La bola 15 estorba el armado; la bola blanca está en algún lugar de la mesa.",
            (2 to 2) to "La bola 15 se coloca en el punto de cabeza. La bola blanca se queda donde está.",
            (3 to 1) to "La bola 15 estorba el armado; la bola blanca bloquea el punto de cabeza.",
            (3 to 2) to "La bola 15 se coloca en el punto central. La bola blanca se queda donde está.",
            (4 to 1) to "Tanto la bola blanca como la bola 15 estorban el armado.",
            (4 to 2) to "El triángulo se reconstruye por completo. La bola blanca se juega desde cualquier " +
                "punto de la cocina.",
            (5 to 1) to "La bola blanca estorba el armado; la bola 15 no está en la cocina.",
            (5 to 2) to "La bola blanca puede colocarse en cualquier punto de la cocina. La bola 15 se queda " +
                "donde está.",
            (6 to 1) to "La bola blanca estorba el armado; la bola 15 está en la cocina.",
            (6 to 2) to "La bola blanca se coloca en el punto de cabeza y puede jugarse en cualquier dirección. " +
                "La bola 15 se queda donde está.",
            (7 to 1) to "La bola blanca estorba el armado; la bola 15 bloquea el punto de cabeza.",
            (7 to 2) to "La bola blanca se coloca en el punto central. La bola 15 se queda donde está.",
        ),
        AppLanguage.FR to mapOf(
            (1 to 1) to "Les billes 14 et 15 ont été empochées.",
            (1 to 2) to "Le triangle est entièrement reconstruit. La bille blanche reste en place.",
            (2 to 1) to "La bille 15 gêne le remontage ; la bille blanche se trouve quelque part sur la table.",
            (2 to 2) to "La bille 15 est placée sur le point de tête. La bille blanche reste en place.",
            (3 to 1) to "La bille 15 gêne le remontage ; la bille blanche bloque le point de tête.",
            (3 to 2) to "La bille 15 est placée sur le point central. La bille blanche reste en place.",
            (4 to 1) to "La bille blanche et la bille 15 gênent toutes deux le remontage.",
            (4 to 2) to "Le triangle est entièrement reconstruit. La bille blanche est jouée depuis n'importe " +
                "où dans la zone de tête.",
            (5 to 1) to "La bille blanche gêne le remontage ; la bille 15 n'est pas dans la zone de tête.",
            (5 to 2) to "La bille blanche peut être placée n'importe où dans la zone de tête. La bille 15 " +
                "reste en place.",
            (6 to 1) to "La bille blanche gêne le remontage ; la bille 15 se trouve dans la zone de tête.",
            (6 to 2) to "La bille blanche est placée sur le point de tête et peut être jouée dans n'importe " +
                "quelle direction. La bille 15 reste en place.",
            (7 to 1) to "La bille blanche gêne le remontage ; la bille 15 bloque le point de tête.",
            (7 to 2) to "La bille blanche est placée sur le point central. La bille 15 reste en place.",
        ),
    )

    private val tournamentModeLabels: Map<AppLanguage, Map<TournamentMode, String>> = mapOf(
        AppLanguage.DE to mapOf(
            TournamentMode.LOSER_STAYS to "Verlierer bleibt",
            TournamentMode.WINNER_STAYS to "Gewinner bleibt",
            TournamentMode.ROUND_ROBIN to "Jeder gegen Jeden",
            TournamentMode.SUDDEN_DEATH to "Sudden Death",
            TournamentMode.SINGLE_ELIMINATION to "Einzel-K.O.",
            TournamentMode.PARTNER_ROTATION to "Wechselnde Doppel",
        ),
        AppLanguage.ES to mapOf(
            TournamentMode.LOSER_STAYS to "El perdedor se queda",
            TournamentMode.WINNER_STAYS to "El ganador se queda",
            TournamentMode.ROUND_ROBIN to "Todos contra todos",
            TournamentMode.SUDDEN_DEATH to "Muerte súbita",
            TournamentMode.SINGLE_ELIMINATION to "Eliminación directa",
            TournamentMode.PARTNER_ROTATION to "Dobles rotativos",
        ),
        AppLanguage.FR to mapOf(
            TournamentMode.LOSER_STAYS to "Le perdant reste",
            TournamentMode.WINNER_STAYS to "Le gagnant reste",
            TournamentMode.ROUND_ROBIN to "Tournoi toutes rondes",
            TournamentMode.SUDDEN_DEATH to "Mort subite",
            TournamentMode.SINGLE_ELIMINATION to "Élimination directe",
            TournamentMode.PARTNER_ROTATION to "Doubles tournants",
        ),
    )

    private val tournamentModeDescriptions: Map<AppLanguage, Map<TournamentMode, String>> = mapOf(
        AppLanguage.DE to mapOf(
            TournamentMode.LOSER_STAYS to "Der Verlierer bleibt am Tisch und stößt an. Der Gewinner geht ans " +
                "Ende der Warteschlange.",
            TournamentMode.WINNER_STAYS to "Der Gewinner bleibt am Tisch und stößt an. Der Verlierer geht ans " +
                "Ende der Warteschlange.",
            TournamentMode.ROUND_ROBIN to "Jeder spielt einmal gegen jeden. Die meisten Siege gewinnen das " +
                "Turnier.",
            TournamentMode.SUDDEN_DEATH to "Wie Gewinner bleibt, aber wer verliert scheidet komplett aus. " +
                "Letzter verbleibender Spieler gewinnt.",
            TournamentMode.SINGLE_ELIMINATION to "Klassischer K.O.-Turnierbaum. Eine Niederlage bedeutet " +
                "Ausscheiden, bei ungerader Spielerzahl gibt es Freilose.",
            TournamentMode.PARTNER_ROTATION to "Doppel, mindestens 4 Spieler. Bei genau 4 wechseln die Partner " +
                "durch alle 3 möglichen Paarungen (AB-CD, AC-BD, AD-BC) im Kreis. Bei mehr spielen jede Runde 4, " +
                "der Rest setzt reihum aus, sodass alle etwa gleich oft warten. Beide Sieger erhalten je einen Punkt.",
        ),
        AppLanguage.ES to mapOf(
            TournamentMode.LOSER_STAYS to "El perdedor se queda en la mesa y rompe a continuación. El ganador " +
                "va al final de la cola.",
            TournamentMode.WINNER_STAYS to "El ganador se queda en la mesa y rompe a continuación. El perdedor " +
                "va al final de la cola.",
            TournamentMode.ROUND_ROBIN to "Todos juegan una vez contra todos. Gana el torneo quien tenga más " +
                "victorias.",
            TournamentMode.SUDDEN_DEATH to "Como \"el ganador se queda\", pero quien pierde queda eliminado " +
                "por completo. Gana el último jugador en pie.",
            TournamentMode.SINGLE_ELIMINATION to "Cuadro de eliminación directa clásico. Una derrota supone la " +
                "eliminación; con número impar de jugadores hay descansos.",
            TournamentMode.PARTNER_ROTATION to "Dobles, mínimo 4 jugadores. Con exactamente 4, las parejas rotan " +
                "entre las 3 combinaciones posibles (AB-CD, AC-BD, AD-BC) en cada ciclo. Con más, juegan 4 cada " +
                "ronda y el resto espera por turnos, de forma que todos descansan aproximadamente por igual. " +
                "Ambos ganadores suman un punto.",
        ),
        AppLanguage.FR to mapOf(
            TournamentMode.LOSER_STAYS to "Le perdant reste à la table et casse ensuite. Le gagnant retourne en " +
                "fin de file d'attente.",
            TournamentMode.WINNER_STAYS to "Le gagnant reste à la table et casse ensuite. Le perdant retourne " +
                "en fin de file d'attente.",
            TournamentMode.ROUND_ROBIN to "Chaque joueur affronte tous les autres une fois. Le plus grand " +
                "nombre de victoires remporte le tournoi.",
            TournamentMode.SUDDEN_DEATH to "Comme \"le gagnant reste\", mais le perdant est complètement " +
                "éliminé. Le dernier joueur restant gagne.",
            TournamentMode.SINGLE_ELIMINATION to "Tableau à élimination directe classique. Une défaite élimine " +
                "le joueur ; avec un nombre impair de joueurs, certains sont exemptés d'un tour.",
            TournamentMode.PARTNER_ROTATION to "Double, 4 joueurs minimum. Avec exactement 4, les partenaires " +
                "tournent parmi les 3 associations possibles (AB-CD, AC-BD, AD-BC) à chaque cycle. Au-delà, 4 " +
                "joueurs jouent chaque manche et les autres attendent à tour de rôle, pour un temps d'attente " +
                "à peu près égal pour tous. Les deux gagnants marquent un point chacun.",
        ),
    )

    // WAITING, WINS_TOURNAMENT and CHAMPION carry a "%s" placeholder - format with the relevant name(s).
    private val tournamentTexts: Map<AppLanguage, Map<TournamentTextKey, String>> = mapOf(
        AppLanguage.DE to mapOf(
            TournamentTextKey.NEW_TOURNAMENT_TITLE to "Neues Turnier",
            TournamentTextKey.ADD_PLAYER to "+ Spieler hinzufügen",
            TournamentTextKey.MODE to "Modus",
            TournamentTextKey.TARGET_WINS to "Punkte für Turniersieg",
            TournamentTextKey.START to "Start",
            TournamentTextKey.TOURNAMENT_TITLE to "Turnier",
            TournamentTextKey.TABLE_NOW to "Am Tisch",
            TournamentTextKey.STAYING_LOST_LAST to "Bleibt (letztes verloren)",
            TournamentTextKey.STAYING_WON_LAST to "Bleibt (letztes gewonnen)",
            TournamentTextKey.CHALLENGER to "Herausforderer",
            TournamentTextKey.WAITING to "Wartend: %s",
            TournamentTextKey.DRAW to "Unentschieden!",
            TournamentTextKey.WINS_TOURNAMENT to "%s gewinnt das Turnier!",
            TournamentTextKey.TOURNAMENT_FINISHED to "Turnier beendet",
            TournamentTextKey.SAVE_AND_FINISH to "Speichern & beenden",
            TournamentTextKey.SAVE_AND_REMATCH to "Speichern & Revanche",
            TournamentTextKey.STANDINGS to "Tabelle",
            TournamentTextKey.TOURNAMENTS_TITLE to "Turniere",
            TournamentTextKey.NO_TOURNAMENTS_YET to "Noch keine Turniere",
            TournamentTextKey.CHAMPION to "Sieger: %s",
            TournamentTextKey.IN_PROGRESS to "Läuft",
            TournamentTextKey.DELETE_TOURNAMENT_TITLE to "Turnier löschen?",
            TournamentTextKey.DELETE_TOURNAMENT_MESSAGE to "Dieses Turnier wird dauerhaft gelöscht.",
            TournamentTextKey.DELETE to "Löschen",
            TournamentTextKey.ROUND to "Runde %d von %d",
            TournamentTextKey.ROUND_NUMBER to "Runde %d",
            TournamentTextKey.TEAM_WINS_TOURNAMENT to "%s gewinnen das Turnier!",
            TournamentTextKey.GAMES_PER_ENCOUNTER to "Gewinnspiele",
            TournamentTextKey.LEAVE_TOURNAMENT_TITLE to "Turnier verlassen?",
            TournamentTextKey.LEAVE_TOURNAMENT_MESSAGE to "Das Turnier ist noch nicht beendet. Der Stand ist gespeichert - du kannst später über Turniere fortsetzen.",
            TournamentTextKey.LEAVE to "Verlassen",
            TournamentTextKey.STARTING_ORDER to "Startreihenfolge",
            TournamentTextKey.ORDER_AS_LISTED to "Wie in der Liste",
            TournamentTextKey.ORDER_RANDOM to "Zufällig auslosen",
            TournamentTextKey.STARTING_ORDER_HINT to "Legt fest, wer beginnt und in welcher Reihenfolge die übrigen Spieler in die Warteschlange kommen.",
        ),
        AppLanguage.ES to mapOf(
            TournamentTextKey.NEW_TOURNAMENT_TITLE to "Nuevo torneo",
            TournamentTextKey.ADD_PLAYER to "+ Añadir jugador",
            TournamentTextKey.MODE to "Modo",
            TournamentTextKey.TARGET_WINS to "Victorias objetivo",
            TournamentTextKey.START to "Iniciar",
            TournamentTextKey.TOURNAMENT_TITLE to "Torneo",
            TournamentTextKey.TABLE_NOW to "En la mesa",
            TournamentTextKey.STAYING_LOST_LAST to "Se queda (perdió la última)",
            TournamentTextKey.STAYING_WON_LAST to "Se queda (ganó la última)",
            TournamentTextKey.CHALLENGER to "Retador",
            TournamentTextKey.WAITING to "Esperando: %s",
            TournamentTextKey.DRAW to "¡Empate!",
            TournamentTextKey.WINS_TOURNAMENT to "¡%s gana el torneo!",
            TournamentTextKey.TOURNAMENT_FINISHED to "Torneo finalizado",
            TournamentTextKey.SAVE_AND_FINISH to "Guardar y finalizar",
            TournamentTextKey.SAVE_AND_REMATCH to "Guardar y revancha",
            TournamentTextKey.STANDINGS to "Clasificación",
            TournamentTextKey.TOURNAMENTS_TITLE to "Torneos",
            TournamentTextKey.NO_TOURNAMENTS_YET to "Aún no hay torneos",
            TournamentTextKey.CHAMPION to "Campeón: %s",
            TournamentTextKey.IN_PROGRESS to "En curso",
            TournamentTextKey.DELETE_TOURNAMENT_TITLE to "¿Eliminar torneo?",
            TournamentTextKey.DELETE_TOURNAMENT_MESSAGE to "Este torneo se eliminará de forma permanente.",
            TournamentTextKey.DELETE to "Eliminar",
            TournamentTextKey.ROUND to "Ronda %d de %d",
            TournamentTextKey.ROUND_NUMBER to "Ronda %d",
            TournamentTextKey.TEAM_WINS_TOURNAMENT to "¡%s ganan el torneo!",
            TournamentTextKey.GAMES_PER_ENCOUNTER to "Partidas por enfrentamiento",
            TournamentTextKey.LEAVE_TOURNAMENT_TITLE to "¿Salir del torneo?",
            TournamentTextKey.LEAVE_TOURNAMENT_MESSAGE to "El torneo aún no ha terminado. El progreso está guardado - puedes continuarlo más tarde desde Torneos.",
            TournamentTextKey.LEAVE to "Salir",
            TournamentTextKey.STARTING_ORDER to "Orden de inicio",
            TournamentTextKey.ORDER_AS_LISTED to "Según la lista",
            TournamentTextKey.ORDER_RANDOM to "Sorteo aleatorio",
            TournamentTextKey.STARTING_ORDER_HINT to "Determina quién empieza y en qué orden entran los demás jugadores en la cola.",
        ),
        AppLanguage.FR to mapOf(
            TournamentTextKey.NEW_TOURNAMENT_TITLE to "Nouveau tournoi",
            TournamentTextKey.ADD_PLAYER to "+ Ajouter un joueur",
            TournamentTextKey.MODE to "Mode",
            TournamentTextKey.TARGET_WINS to "Victoires visées",
            TournamentTextKey.START to "Démarrer",
            TournamentTextKey.TOURNAMENT_TITLE to "Tournoi",
            TournamentTextKey.TABLE_NOW to "À la table",
            TournamentTextKey.STAYING_LOST_LAST to "Reste (a perdu la dernière)",
            TournamentTextKey.STAYING_WON_LAST to "Reste (a gagné la dernière)",
            TournamentTextKey.CHALLENGER to "Challenger",
            TournamentTextKey.WAITING to "En attente : %s",
            TournamentTextKey.DRAW to "Match nul !",
            TournamentTextKey.WINS_TOURNAMENT to "%s remporte le tournoi !",
            TournamentTextKey.TOURNAMENT_FINISHED to "Tournoi terminé",
            TournamentTextKey.SAVE_AND_FINISH to "Enregistrer et terminer",
            TournamentTextKey.SAVE_AND_REMATCH to "Enregistrer et revanche",
            TournamentTextKey.STANDINGS to "Classement",
            TournamentTextKey.TOURNAMENTS_TITLE to "Tournois",
            TournamentTextKey.NO_TOURNAMENTS_YET to "Aucun tournoi pour l'instant",
            TournamentTextKey.CHAMPION to "Champion : %s",
            TournamentTextKey.IN_PROGRESS to "En cours",
            TournamentTextKey.DELETE_TOURNAMENT_TITLE to "Supprimer le tournoi ?",
            TournamentTextKey.DELETE_TOURNAMENT_MESSAGE to "Ce tournoi sera supprimé définitivement.",
            TournamentTextKey.DELETE to "Supprimer",
            TournamentTextKey.ROUND to "Manche %d sur %d",
            TournamentTextKey.ROUND_NUMBER to "Manche %d",
            TournamentTextKey.TEAM_WINS_TOURNAMENT to "%s remportent le tournoi !",
            TournamentTextKey.GAMES_PER_ENCOUNTER to "Manches par rencontre",
            TournamentTextKey.LEAVE_TOURNAMENT_TITLE to "Quitter le tournoi ?",
            TournamentTextKey.LEAVE_TOURNAMENT_MESSAGE to "Le tournoi n'est pas encore terminé. La progression est enregistrée - vous pourrez le reprendre plus tard depuis Tournois.",
            TournamentTextKey.LEAVE to "Quitter",
            TournamentTextKey.STARTING_ORDER to "Ordre de départ",
            TournamentTextKey.ORDER_AS_LISTED to "Comme dans la liste",
            TournamentTextKey.ORDER_RANDOM to "Tirage au sort",
            TournamentTextKey.STARTING_ORDER_HINT to "Détermine qui commence et dans quel ordre les autres joueurs rejoignent la file d'attente.",
        ),
    )

    // RACE_TO carries one "%d" placeholder, RACE_TO_INNINGS_LIMIT two, MATCH_WON_BY and WINS_SHORT
    // one "%s" - format with the relevant number(s)/name.
    private val straightMatchTexts: Map<AppLanguage, Map<StraightMatchTextKey, String>> = mapOf(
        AppLanguage.DE to mapOf(
            StraightMatchTextKey.BREAK_FOUL_TITLE to "Anstoßfoul",
            StraightMatchTextKey.BREAK_FOUL_QUESTION to "War das ein Anstoßfoul (regelwidriger Eröffnungsstoß)?",
            StraightMatchTextKey.YES to "Ja",
            StraightMatchTextKey.NO_NORMAL_FOUL to "Nein, normales Foul",
            StraightMatchTextKey.REBREAK_TITLE to "Anstoßfoul  −2",
            StraightMatchTextKey.REBREAK_EXPLANATION to "Der eintretende Spieler kann den Tisch übernehmen oder " +
                "einen erneuten Anstoß verlangen. Bei einem erneuten Anstoß stößt derselbe Spieler wieder an, die " +
                "−2 bleiben bestehen, und es ist weiterhin die erste Aufnahme.",
            StraightMatchTextKey.REQUIRE_REBREAK to "Erneuten Anstoß verlangen",
            StraightMatchTextKey.OPPONENT_TAKES_TABLE to "Tisch übernehmen",
            StraightMatchTextKey.THIRD_FOUL_TITLE to "Drittes Foul in Folge",
            StraightMatchTextKey.THIRD_FOUL_EXPLANATION to "−1 wie gewohnt plus zusätzlich −15, alle 15 Bälle " +
                "werden neu aufgebaut, und derselbe Spieler muss erneut anstoßen. Nur zählen, wenn der Spieler " +
                "nach dem 2. Foul verwarnt wurde - sonst ist es nur ein normales Foul.",
            StraightMatchTextKey.THIRD_FOUL_CONFIRM to "Drittes Foul  −16",
            StraightMatchTextKey.NORMAL_FOUL to "Normales Foul",
            StraightMatchTextKey.RERACK_TITLE to "Neu aufbauen?",
            StraightMatchTextKey.BALLS_REMAINING_QUESTION to "Wie viele Bälle liegen noch auf dem Tisch?",
            StraightMatchTextKey.RERACK_ACTION to "Neu aufbauen",
            StraightMatchTextKey.BALLS_ON_TABLE_TITLE to "Bälle auf dem Tisch?",
            StraightMatchTextKey.SET_ACTION to "Setzen",
            StraightMatchTextKey.MATCH_HISTORY_TITLE to "Spielverlauf",
            StraightMatchTextKey.CLOSE to "Schließen",
            StraightMatchTextKey.DISCARD_MATCH_TITLE to "Spiel verwerfen?",
            StraightMatchTextKey.DISCARD_MATCH_MESSAGE to "Wenn du jetzt zurückgehst, wird der bisherige " +
                "Spielverlauf verworfen - er wurde nicht gespeichert. Nutze stattdessen das Speicher-Symbol, um " +
                "ihn zu behalten.",
            StraightMatchTextKey.DISCARD to "Verwerfen",
            StraightMatchTextKey.RACE_TO to "Bis %d Punkte",
            StraightMatchTextKey.RACE_TO_INNINGS_LIMIT to "Bis %d Punkte - Aufnahmenlimit %d",
            StraightMatchTextKey.MATCH_WON_BY to "%s hat gewonnen!",
            StraightMatchTextKey.MATCH_DRAW to "Unentschieden!",
            StraightMatchTextKey.MATCH_FINISHED to "Spiel beendet",
            StraightMatchTextKey.WINS_SHORT to "%s gewinnt!",
            StraightMatchTextKey.DRAW_SHORT to "Unentschieden!",
            StraightMatchTextKey.FINISHED_SHORT to "Beendet",
            StraightMatchTextKey.SAVE_AND_FINISH to "Speichern & beenden",
            StraightMatchTextKey.SAVE_AND_REMATCH to "Speichern & Revanche",
            StraightMatchTextKey.REMATCH to "Revanche",
            StraightMatchTextKey.INNING_ABBREV to "A",
        ),
        AppLanguage.ES to mapOf(
            StraightMatchTextKey.BREAK_FOUL_TITLE to "Falta de salida",
            StraightMatchTextKey.BREAK_FOUL_QUESTION to "¿Fue una falta de salida (rotura ilegal)?",
            StraightMatchTextKey.YES to "Sí",
            StraightMatchTextKey.NO_NORMAL_FOUL to "No, falta normal",
            StraightMatchTextKey.REBREAK_TITLE to "Falta de salida  −2",
            StraightMatchTextKey.REBREAK_EXPLANATION to "El jugador entrante puede aceptar la mesa o exigir una " +
                "nueva rotura. En una nueva rotura, el mismo jugador rompe de nuevo, los −2 se mantienen y sigue " +
                "siendo la primera entrada.",
            StraightMatchTextKey.REQUIRE_REBREAK to "Exigir nueva rotura",
            StraightMatchTextKey.OPPONENT_TAKES_TABLE to "Tomar la mesa",
            StraightMatchTextKey.THIRD_FOUL_TITLE to "Tercera falta consecutiva",
            StraightMatchTextKey.THIRD_FOUL_EXPLANATION to "−1 como siempre más −15 adicional, las 15 bolas se " +
                "vuelven a armar y el mismo jugador debe romper de nuevo. Solo cuenta si el jugador fue advertido " +
                "tras la 2ª falta - si no, es solo una falta normal.",
            StraightMatchTextKey.THIRD_FOUL_CONFIRM to "Tercera falta  −16",
            StraightMatchTextKey.NORMAL_FOUL to "Falta normal",
            StraightMatchTextKey.RERACK_TITLE to "¿Rearmar?",
            StraightMatchTextKey.BALLS_REMAINING_QUESTION to "¿Cuántas bolas quedan sobre la mesa?",
            StraightMatchTextKey.RERACK_ACTION to "Rearmar",
            StraightMatchTextKey.BALLS_ON_TABLE_TITLE to "¿Bolas sobre la mesa?",
            StraightMatchTextKey.SET_ACTION to "Fijar",
            StraightMatchTextKey.MATCH_HISTORY_TITLE to "Historial de la partida",
            StraightMatchTextKey.CLOSE to "Cerrar",
            StraightMatchTextKey.DISCARD_MATCH_TITLE to "¿Descartar partida?",
            StraightMatchTextKey.DISCARD_MATCH_MESSAGE to "Si vuelves ahora se descarta el progreso de esta " +
                "partida - no se ha guardado. Usa el icono de guardar para conservarla.",
            StraightMatchTextKey.DISCARD to "Descartar",
            StraightMatchTextKey.RACE_TO to "Hasta %d puntos",
            StraightMatchTextKey.RACE_TO_INNINGS_LIMIT to "Hasta %d puntos - límite de %d entradas",
            StraightMatchTextKey.MATCH_WON_BY to "¡%s ha ganado!",
            StraightMatchTextKey.MATCH_DRAW to "¡Empate!",
            StraightMatchTextKey.MATCH_FINISHED to "Partida finalizada",
            StraightMatchTextKey.WINS_SHORT to "¡%s gana!",
            StraightMatchTextKey.DRAW_SHORT to "¡Empate!",
            StraightMatchTextKey.FINISHED_SHORT to "Finalizada",
            StraightMatchTextKey.SAVE_AND_FINISH to "Guardar y finalizar",
            StraightMatchTextKey.SAVE_AND_REMATCH to "Guardar y revancha",
            StraightMatchTextKey.REMATCH to "Revancha",
            StraightMatchTextKey.INNING_ABBREV to "E",
        ),
        AppLanguage.FR to mapOf(
            StraightMatchTextKey.BREAK_FOUL_TITLE to "Faute de casse",
            StraightMatchTextKey.BREAK_FOUL_QUESTION to "Était-ce une faute de casse (casse irrégulière) ?",
            StraightMatchTextKey.YES to "Oui",
            StraightMatchTextKey.NO_NORMAL_FOUL to "Non, faute normale",
            StraightMatchTextKey.REBREAK_TITLE to "Faute de casse  −2",
            StraightMatchTextKey.REBREAK_EXPLANATION to "Le joueur entrant peut accepter la table ou exiger une " +
                "nouvelle casse. Lors d'une nouvelle casse, le même joueur casse à nouveau, les −2 restent " +
                "acquis, et c'est toujours la première reprise.",
            StraightMatchTextKey.REQUIRE_REBREAK to "Exiger une nouvelle casse",
            StraightMatchTextKey.OPPONENT_TAKES_TABLE to "Prendre la table",
            StraightMatchTextKey.THIRD_FOUL_TITLE to "Troisième faute consécutive",
            StraightMatchTextKey.THIRD_FOUL_EXPLANATION to "−1 comme d'habitude plus −15 supplémentaire, les 15 " +
                "billes sont remontées et le même joueur doit recasser. À compter uniquement si le joueur a été " +
                "averti après la 2e faute - sinon c'est une faute normale.",
            StraightMatchTextKey.THIRD_FOUL_CONFIRM to "Troisième faute  −16",
            StraightMatchTextKey.NORMAL_FOUL to "Faute normale",
            StraightMatchTextKey.RERACK_TITLE to "Remonter le triangle ?",
            StraightMatchTextKey.BALLS_REMAINING_QUESTION to "Combien de billes restent sur la table ?",
            StraightMatchTextKey.RERACK_ACTION to "Remonter",
            StraightMatchTextKey.BALLS_ON_TABLE_TITLE to "Billes sur la table ?",
            StraightMatchTextKey.SET_ACTION to "Valider",
            StraightMatchTextKey.MATCH_HISTORY_TITLE to "Historique de la partie",
            StraightMatchTextKey.CLOSE to "Fermer",
            StraightMatchTextKey.DISCARD_MATCH_TITLE to "Abandonner la partie ?",
            StraightMatchTextKey.DISCARD_MATCH_MESSAGE to "Revenir en arrière maintenant abandonne la " +
                "progression de cette partie - elle n'a pas été enregistrée. Utilisez plutôt l'icône " +
                "d'enregistrement pour la conserver.",
            StraightMatchTextKey.DISCARD to "Abandonner",
            StraightMatchTextKey.RACE_TO to "Jusqu'à %d points",
            StraightMatchTextKey.RACE_TO_INNINGS_LIMIT to "Jusqu'à %d points - limite de %d reprises",
            StraightMatchTextKey.MATCH_WON_BY to "%s a gagné !",
            StraightMatchTextKey.MATCH_DRAW to "Match nul !",
            StraightMatchTextKey.MATCH_FINISHED to "Partie terminée",
            StraightMatchTextKey.WINS_SHORT to "%s gagne !",
            StraightMatchTextKey.DRAW_SHORT to "Match nul !",
            StraightMatchTextKey.FINISHED_SHORT to "Terminée",
            StraightMatchTextKey.SAVE_AND_FINISH to "Enregistrer et terminer",
            StraightMatchTextKey.SAVE_AND_REMATCH to "Enregistrer et revanche",
            StraightMatchTextKey.REMATCH to "Revanche",
            StraightMatchTextKey.INNING_ABBREV to "R",
        ),
    )

    // INNINGS_TARGET carries two "%d" placeholders (attempt count, target points); REF_SUFFIX one
    // "%s" (the reference average, already formatted).
    private val trainingSetupTexts: Map<AppLanguage, Map<TrainingSetupTextKey, String>> = mapOf(
        AppLanguage.DE to mapOf(
            TrainingSetupTextKey.PLAYER to "Spieler",
            TrainingSetupTextKey.OPEN_ENDED to "Endlos · kein Aufnahmenlimit",
            TrainingSetupTextKey.INNINGS_TARGET to "%d Aufnahmen · Ziel %d Punkte",
            TrainingSetupTextKey.REF_SUFFIX to " · Ref Ø %s",
        ),
        AppLanguage.ES to mapOf(
            TrainingSetupTextKey.PLAYER to "Jugador",
            TrainingSetupTextKey.OPEN_ENDED to "Sin límite · sin límite de entradas",
            TrainingSetupTextKey.INNINGS_TARGET to "%d entradas · objetivo %d puntos",
            TrainingSetupTextKey.REF_SUFFIX to " · ref Ø %s",
        ),
        AppLanguage.FR to mapOf(
            TrainingSetupTextKey.PLAYER to "Joueur",
            TrainingSetupTextKey.OPEN_ENDED to "Illimité · sans limite de reprises",
            TrainingSetupTextKey.INNINGS_TARGET to "%d reprises · objectif %d points",
            TrainingSetupTextKey.REF_SUFFIX to " · réf Ø %s",
        ),
    )
}
