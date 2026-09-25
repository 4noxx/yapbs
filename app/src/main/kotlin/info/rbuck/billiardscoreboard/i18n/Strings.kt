package info.rbuck.billiardscoreboard.i18n

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * App-wide UI copy, one property per visible string. [En] is the base and single source of truth
 * for English; the other languages are subclasses that override only what's actually translated,
 * so any not-yet-translated string transparently falls back to English.
 *
 * Access from composables via [LocalStrings] (`val s = LocalStrings.current`). The active language
 * is driven by [info.rbuck.billiardscoreboard.data.AppSettingsRepository.language] and provided at
 * the top of the tree in MainActivity.
 *
 * This gradually replaces the scattered hardcoded English literals; [Translations] still holds the
 * larger templated tournament / training-rule / rebuild-rule copy.
 */
open class Strings {

    // Generic / shared
    open val close = "Close"
    open val cancel = "Cancel"
    open val delete = "Delete"
    open val save = "Save"
    open val done = "Done"
    open val back = "Back"
    open val change = "Change"
    open val add = "Add"

    // Start screen
    open val startNewMatch = "New match"
    open val startMore = "More"
    open val eightBall = "8-Ball"
    open val nineBall = "9-Ball"
    open val tenBall = "10-Ball"
    open val straightPool = "Straight Pool"
    open val tilePlayers = "Players"
    open val tileHistory = "History"
    open val tileSettings = "Settings"
    open val tileTraining = "Training"
    open val tileTournament = "Tournament"

    // Settings screen
    open val settingsTitle = "Settings"
    open val settingsSectionRaceTargets = "Default race to targets"
    open val settingsRace8Ball = "8-Ball Race"
    open val settingsRace9Ball = "9-Ball Race"
    open val settingsRace10Ball = "10-Ball Race"
    open val settingsRaceStraightPool = "14.1 Straight Pool Race"
    open val settingsSectionPlayerSelection = "Player selection"
    open val settingsDefaultClub = "Default club"
    open val settingsAllClubs = "All clubs"
    open val settingsSectionTournament = "Tournament"
    open val settingsTournamentMode = "Tournament mode"
    open val settingsTournamentModeDesc =
        "Adds a Tournament tile to Start, for running small tournaments with up to 8 players."
    open val settingsSectionTraining = "Training"
    open val settingsSaveTrainingToHistory = "Save training to history"
    open val settingsSaveTrainingToHistoryDesc =
        "Only fully played-through sessions are saved - all innings finished, or \"Finish\" on the " +
            "open-ended 14.1 HighRun. Cancelling or going back never saves."
    open val settingsObsPauseScene = "Pause scene (optional)"
    open val settingsObsPauseSceneDesc =
        "Scene name in OBS to switch to for \"Pause\" in the Regie screen - the stream itself " +
            "keeps running (obs-websocket has no native stream pause). Leave empty to hide the " +
            "Pause button there."

    open val settingsObsRegieTile = "Show \"Regie\" tile on Start"
    open val settingsObsWakeOnLanMac = "PC MAC address (Wake-on-LAN, optional)"
    open val settingsObsWakeOnLanMacDesc =
        "Lets Regie show a \"Wake PC\" button when OBS is unreachable. Needs Wake-on-LAN enabled " +
            "on the PC's network adapter (BIOS/UEFI and Windows) and only works if the tablet and " +
            "PC are on the same network - wired Ethernet is far more reliable than Wi-Fi for this."
    open val settingsObsCameraSource = "Camera source name (optional)"
    open val settingsObsCameraSourceDesc =
        "Name of the OBS media/camera source (e.g. an RTSP \"Kamera\" input). Adds a \"Reload " +
            "camera\" button to Regie and auto-restarts that source after the app reconnects from a " +
            "longer outage - OBS often leaves an RTSP camera frozen on the last frame after the PC wakes."
    open val settingsObsAutoHide = "Auto-hide when idle (optional)"
    open val settingsObsAutoHideDesc =
        "Comma-separated names of top-level scene items in OBS (usually Groups, e.g. \"Player1," +
            "Player2,Scoreboard\") to show only while a match is open and hide the rest of the " +
            "time - so crests and the score overlay don't sit visible-but-blank between matches."

    open val settingsObsStraightOnly = "Show only for 14.1 (optional)"
    open val settingsObsStraightOnlyDesc =
        "Same idea, but shown only while the open match is 14.1 Straight Pool specifically (e.g. " +
            "\"Statistik\" for an innings/highest-break group that doesn't apply to 8/9/10-Ball)."

    open val settingsSectionDataPrivacy = "Data & privacy"
    open val settingsRetention = "Keep history for"
    open val settingsRetentionDesc =
        "Finished matches, tournaments and training sessions older than this are removed " +
            "automatically when the app starts. Set to 0 to keep them forever."
    open val settingsRetentionForever = "Forever"
    open fun settingsRetentionDays(days: Int) = "$days days"
    open val settingsClearHistory = "Delete all history now"
    open val settingsClearHistoryButton = "Delete all"
    open val settingsClearHistoryConfirmTitle = "Delete all history?"
    open val settingsClearHistoryConfirmMessage =
        "This permanently deletes every saved match, tournament and training session from this " +
            "device. The player list itself is kept."
    open val settingsPrivacyNote =
        "Player names and results are stored only on this device. Nothing is sent to the app's " +
            "developer, and the history is excluded from cloud backup and device-to-device transfer."

    open val settingsSectionDisplay = "Display"
    open val settingsTheme = "Theme"
    open val settingsKeepScreenOn = "Keep screen on"
    open val settingsSectionLanguage = "Language"
    open val settingsLanguageRow = "App language"
    open val settingsSectionExportImport = "Export / Import players"
    open val settingsExportPlayers = "Export players"
    open val settingsExport = "Export"
    open val settingsImportPlayers = "Import players from file"
    open val settingsImport = "Import"
    open val settingsSharePlayers = "Share players"

    // Regie (OBS record/stream control) screen
    open val tileRegie = "Regie"
    open val obsRegieTitle = "Regie"
    open val obsRegieRecordingSection = "Recording"
    open val obsRegieStreamingSection = "Streaming"
    open val obsStateIdle = "Off"
    open val obsStateRecordingActive = "Recording"
    open val obsStateStreamingActive = "Live"
    open val obsStatePaused = "Paused"
    open val obsStartRecording = "Start recording"
    open val obsStopRecording = "Stop recording"
    open val obsPauseAction = "Pause"
    open val obsResumeAction = "Resume"
    open val obsStartStream = "Go live"
    open val obsStopStream = "End stream"
    open val obsStopRecordingConfirmTitle = "Stop recording?"
    open val obsStopRecordingConfirmMessage = "This stops and finalizes the current recording in OBS."
    open val obsStopStreamConfirmTitle = "End stream?"
    open val obsStopStreamConfirmMessage = "This ends the live stream in OBS / YouTube."
    open val obsNotConnected = "Not connected to OBS - check Settings → OBS WebSocket."
    open val obsWakePc = "Wake PC (Wake-on-LAN)"
    open val obsWakePcSent = "Magic packet sent - waiting for OBS to reconnect..."
    open val obsWakePcInvalidMac = "Invalid MAC address - check Settings → OBS WebSocket."
    open val obsReloadCamera = "Reload camera"
    open val obsPauseSceneMissing = "Set a pause scene in Settings → OBS WebSocket to enable stream pause."

    // Head-to-head (player vs player stats)
    open val h2hTitle = "Head-to-head"
    open val h2hPlayerA = "Player A"
    open val h2hPlayerB = "Player B"
    open val h2hPickPrompt = "Pick two players to see their past results."
    open val h2hNoMeetings = "These two have no finished match against each other yet."
    open val h2hRecordSection = "Record"
    open val h2hByDisciplineSection = "By discipline"
    open val h2hFormSection = "Form (old → new)"
    open val h2hLastMeeting = "Last meeting"
    open val h2hDraw = "Draw"
    open val h2hDrawsShort = "D"
    open val h2hRacks = "Racks (8/9/10-Ball)"
    open fun h2hStreak(name: String, count: Int) = "$name — $count wins in a row"
    open fun h2hMatchCount(n: Int) = if (n == 1) "1 match" else "$n matches"
    open val h2hMomentumSection = "Momentum"
    open val h2hMomentumHint = "Cumulative win difference over time"
    open val h2hStraightSection = "14.1 detail"
    open val h2hAvg = "Ø / inning"
    open val h2hHighestRun = "Highest run"
    open val h2hFouls = "Fouls"
    open val h2hFilterAll = "All"
    open val h2hFilterAllTime = "All time"
    open val h2hFilterLast12 = "12 months"

    // History / archive screen
    open val historyTitle = "History"
    open val historyTabMatches = "Matches"
    open val historyTabTraining = "Training"
    open val historyNoMatches = "No matches saved"
    open val historyNoTraining = "No training sessions saved"
    open val historyNoResults = "Nothing matches this filter"
    open val historyFilterPlaceholder = "Filter by name, discipline, year…"
    open val historyFilterAllTypes = "All"
    open val historyRematch = "Rematch"
    open val historyDeleteMatchTitle = "Delete match?"
    open val historyDeleteMatchMessage = "This will permanently delete this match from the archive."
    open val historyDeleteTrainingTitle = "Delete training session?"
    open val historyDeleteTrainingMessage =
        "This will permanently delete this training session from the history."
    open val historyInning = "Inning"
    open val historyScore = "Score"
    open val historyHighRun = "High run"
    open val historyInnings = "Innings"
    open val historyTotal = "Total"
    open val historyAverage = "Average"
    open val historyReference = "reference"
    open val genericPlayer = "Player"
    open val genericPlayer1 = "Player 1"
    open val genericPlayer2 = "Player 2"

    companion object {
        fun of(language: AppLanguage): Strings = when (language) {
            AppLanguage.EN -> En
            AppLanguage.DE -> De
            AppLanguage.ES -> Es
            AppLanguage.FR -> Fr
        }
    }

    object En : Strings()

    object De : Strings() {
        override val close = "Schließen"
        override val cancel = "Abbrechen"
        override val delete = "Löschen"
        override val save = "Speichern"
        override val done = "Fertig"
        override val back = "Zurück"
        override val change = "Ändern"
        override val add = "Hinzufügen"

        override val startNewMatch = "Neues Spiel"
        override val startMore = "Mehr"
        override val straightPool = "14.1 endlos"
        override val tilePlayers = "Spieler"
        override val tileHistory = "Verlauf"
        override val tileSettings = "Einstellungen"
        override val tileTraining = "Training"
        override val tileTournament = "Turnier"

        override val settingsTitle = "Einstellungen"
        override val settingsSectionRaceTargets = "Standard-Ziele (Race to)"
        override val settingsRace8Ball = "8-Ball Race"
        override val settingsRace9Ball = "9-Ball Race"
        override val settingsRace10Ball = "10-Ball Race"
        override val settingsRaceStraightPool = "14.1 Endlos Race"
        override val settingsSectionPlayerSelection = "Spielerauswahl"
        override val settingsDefaultClub = "Standard-Verein"
        override val settingsAllClubs = "Alle Vereine"
        override val settingsSectionTournament = "Turnier"
        override val settingsTournamentMode = "Turniermodus"
        override val settingsTournamentModeDesc =
            "Fügt eine Turnier-Kachel zum Startbildschirm hinzu, für kleine Turniere mit bis zu 8 Spielern."
        override val settingsSectionTraining = "Training"
        override val settingsSaveTrainingToHistory = "Trainingsstände im Verlauf speichern"
        override val settingsSaveTrainingToHistoryDesc =
            "Es werden nur vollständig durchgespielte Sessions gespeichert - alle Aufnahmen beendet, " +
                "oder \"Finish\" beim offenen 14.1 HighRun. Abbrechen oder Zurückgehen speichert nie."
        override val settingsObsPauseScene = "Pausen-Szene (optional)"
        override val settingsObsPauseSceneDesc =
            "Szenenname in OBS, auf den „Pause\" im Regie-Screen umschaltet - der Stream selbst läuft " +
                "weiter (obs-websocket kennt kein echtes Stream-Pause). Leer lassen, um den " +
                "Pause-Button dort auszublenden."

        override val settingsObsRegieTile = "„Regie\"-Kachel auf Start anzeigen"
        override val settingsObsWakeOnLanMac = "MAC-Adresse des PCs (Wake-on-LAN, optional)"
        override val settingsObsWakeOnLanMacDesc =
            "Zeigt in Regie einen „PC aufwecken\"-Button, wenn OBS nicht erreichbar ist. Erfordert " +
                "Wake-on-LAN auf dem Netzwerkadapter des PCs (BIOS/UEFI und Windows) und funktioniert " +
                "nur, wenn Tablet und PC im selben Netzwerk sind - kabelgebundenes Ethernet ist dafür " +
                "deutlich zuverlässiger als WLAN."
        override val settingsObsCameraSource = "Name der Kamera-Quelle (optional)"
        override val settingsObsCameraSourceDesc =
            "Name der OBS-Medien-/Kamera-Quelle (z. B. eine RTSP-\"Kamera\"). Fügt Regie einen " +
                "„Kamera neu laden\"-Button hinzu und startet die Quelle nach einem längeren " +
                "Verbindungsausfall automatisch neu - OBS lässt eine RTSP-Kamera nach dem Aufwachen " +
                "des PCs oft auf dem letzten Standbild einfrieren."
        override val settingsObsAutoHide = "Automatisch ausblenden (optional)"
        override val settingsObsAutoHideDesc =
            "Namen von Top-Level-Quellen in OBS (meist Gruppen, z. B. \"Player1,Player2,Scoreboard\"), " +
                "Komma-getrennt - werden nur eingeblendet, solange eine Partie läuft, sonst " +
                "ausgeblendet. So stehen Wappen und Score-Overlay zwischen den Partien nicht " +
                "sichtbar-aber-leer in der Szene."

        override val settingsObsStraightOnly = "Nur bei 14.1 einblenden (optional)"
        override val settingsObsStraightOnlyDesc =
            "Wie oben, aber nur eingeblendet, solange die laufende Partie 14.1 Endlos ist (z. B. " +
                "\"Statistik\" für eine Aufnahmen-/Höchstserie-Gruppe, die bei 8/9/10-Ball nicht gilt)."

        override val settingsSectionDataPrivacy = "Daten & Datenschutz"
        override val settingsRetention = "Verlauf aufbewahren"
        override val settingsRetentionDesc =
            "Beendete Spiele, Turniere und Trainings-Sessions, die älter sind, werden beim " +
                "App-Start automatisch entfernt. 0 = unbegrenzt aufbewahren."
        override val settingsRetentionForever = "Unbegrenzt"
        override fun settingsRetentionDays(days: Int) = "$days Tage"
        override val settingsClearHistory = "Verlauf jetzt komplett löschen"
        override val settingsClearHistoryButton = "Alles löschen"
        override val settingsClearHistoryConfirmTitle = "Gesamten Verlauf löschen?"
        override val settingsClearHistoryConfirmMessage =
            "Damit werden alle gespeicherten Spiele, Turniere und Trainings-Sessions dauerhaft von " +
                "diesem Gerät gelöscht. Die Spielerliste selbst bleibt erhalten."
        override val settingsPrivacyNote =
            "Spielernamen und Ergebnisse werden ausschließlich auf diesem Gerät gespeichert. Nichts " +
                "wird an den App-Entwickler gesendet, und der Verlauf ist von Cloud-Backup und " +
                "Geräte-zu-Geräte-Übertragung ausgeschlossen."

        override val settingsSectionDisplay = "Anzeige"
        override val settingsTheme = "Design"
        override val settingsKeepScreenOn = "Bildschirm anlassen"
        override val settingsSectionLanguage = "Sprache"
        override val settingsLanguageRow = "App-Sprache"
        override val settingsSectionExportImport = "Spieler exportieren / importieren"
        override val settingsExportPlayers = "Spieler exportieren"
        override val settingsExport = "Exportieren"
        override val settingsImportPlayers = "Spieler aus Datei importieren"
        override val settingsImport = "Importieren"
        override val settingsSharePlayers = "Spieler teilen"

        override val tileRegie = "Regie"
        override val obsRegieTitle = "Regie"
        override val obsRegieRecordingSection = "Aufnahme"
        override val obsRegieStreamingSection = "Stream"
        override val obsStateIdle = "Aus"
        override val obsStateRecordingActive = "Nimmt auf"
        override val obsStateStreamingActive = "Live"
        override val obsStatePaused = "Pausiert"
        override val obsStartRecording = "Aufnahme starten"
        override val obsStopRecording = "Aufnahme stoppen"
        override val obsPauseAction = "Pause"
        override val obsResumeAction = "Fortsetzen"
        override val obsStartStream = "Live gehen"
        override val obsStopStream = "Stream beenden"
        override val obsStopRecordingConfirmTitle = "Aufnahme stoppen?"
        override val obsStopRecordingConfirmMessage = "Beendet die laufende Aufnahme in OBS und schließt die Datei ab."
        override val obsStopStreamConfirmTitle = "Stream beenden?"
        override val obsStopStreamConfirmMessage = "Beendet den Live-Stream in OBS / YouTube."
        override val obsNotConnected = "Keine Verbindung zu OBS - prüfe Einstellungen → OBS WebSocket."
        override val obsWakePc = "PC aufwecken (Wake-on-LAN)"
        override val obsWakePcSent = "Magic Packet gesendet - warte auf Verbindung zu OBS..."
        override val obsWakePcInvalidMac = "Ungültige MAC-Adresse - prüfe Einstellungen → OBS WebSocket."
        override val obsReloadCamera = "Kamera neu laden"
        override val obsPauseSceneMissing = "Lege in den Einstellungen → OBS WebSocket eine Pausen-Szene fest, um Stream-Pause zu nutzen."

        override val h2hTitle = "Direktvergleich"
        override val h2hPlayerA = "Spieler A"
        override val h2hPlayerB = "Spieler B"
        override val h2hPickPrompt = "Zwei Spieler wählen, um ihre bisherigen Ergebnisse zu sehen."
        override val h2hNoMeetings = "Diese beiden haben noch kein beendetes Spiel gegeneinander."
        override val h2hRecordSection = "Bilanz"
        override val h2hByDisciplineSection = "Nach Disziplin"
        override val h2hFormSection = "Form (alt → neu)"
        override val h2hLastMeeting = "Letzte Begegnung"
        override val h2hDraw = "Remis"
        override val h2hDrawsShort = "U"
        override val h2hRacks = "Racks (8/9/10-Ball)"
        override fun h2hStreak(name: String, count: Int) = "$name — $count Siege in Folge"
        override fun h2hMatchCount(n: Int) = if (n == 1) "1 Spiel" else "$n Spiele"
        override val h2hMomentumSection = "Momentum"
        override val h2hMomentumHint = "Kumulierte Siegdifferenz über die Zeit"
        override val h2hStraightSection = "14.1 im Detail"
        override val h2hAvg = "Ø / Aufnahme"
        override val h2hHighestRun = "Höchste Serie"
        override val h2hFouls = "Fouls"
        override val h2hFilterAll = "Alle"
        override val h2hFilterAllTime = "Gesamt"
        override val h2hFilterLast12 = "12 Monate"

        override val historyTitle = "Verlauf"
        override val historyTabMatches = "Spiele"
        override val historyTabTraining = "Training"
        override val historyNoMatches = "Keine Spiele gespeichert"
        override val historyNoTraining = "Keine Trainings-Sessions gespeichert"
        override val historyNoResults = "Kein Treffer für diesen Filter"
        override val historyFilterPlaceholder = "Nach Name, Disziplin, Jahr filtern…"
        override val historyFilterAllTypes = "Alle"
        override val historyRematch = "Revanche"
        override val historyDeleteMatchTitle = "Spiel löschen?"
        override val historyDeleteMatchMessage = "Dieses Spiel wird dauerhaft aus dem Verlauf gelöscht."
        override val historyDeleteTrainingTitle = "Trainings-Session löschen?"
        override val historyDeleteTrainingMessage =
            "Diese Trainings-Session wird dauerhaft aus dem Verlauf gelöscht."
        override val historyInning = "Aufnahme"
        override val historyScore = "Punkte"
        override val historyHighRun = "Höchstserie"
        override val historyInnings = "Aufnahmen"
        override val historyTotal = "Gesamt"
        override val historyAverage = "Schnitt"
        override val historyReference = "Referenz"
        override val genericPlayer = "Spieler"
        override val genericPlayer1 = "Spieler 1"
        override val genericPlayer2 = "Spieler 2"
    }

    object Es : Strings()

    object Fr : Strings()
}

val LocalStrings = staticCompositionLocalOf<Strings> { Strings.En }
