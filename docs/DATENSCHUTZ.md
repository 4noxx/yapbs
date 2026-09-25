# Datenschutzerklärung – YAPBS

**Stand:** 3. September 2026

## Kurzfassung

YAPBS ist eine reine Offline-App. **Es gibt keinen Server des Anbieters.** Alle Daten, die du eingibst, bleiben auf deinem Gerät. Es findet keine Analyse, kein Tracking und keine Werbung statt, und es sind keine Drittanbieter-SDKs eingebunden, die Daten übermitteln.

---

## 1. Verantwortlicher für diese App

<!-- TODO vor Veröffentlichung ausfüllen: -->
    [Name]
    [Anschrift]
    E-Mail: [Kontakt-E-Mail]

Diese Angaben betreffen die Bereitstellung der App selbst. Für die Daten, die *in* der App erfasst werden (siehe Abschnitt 3), ist die Person oder der Verein verantwortlich, die die App nutzt – nicht der Anbieter der App.

## 2. Welche Daten die App verarbeitet

Die App speichert ausschließlich lokal auf deinem Gerät:

| Datenart | Zweck |
|---|---|
| Spielernamen | Zuordnung von Partien und Statistiken |
| Vereinsnamen und Vereinswappen | optionale Zuordnung von Spielern zu Vereinen |
| Spielverläufe und Ergebnisse | Spielstandsanzeige, Archiv, Statistiken |
| Trainingssessions (Übungsergebnisse, Aufnahmen) | nur wenn in den Einstellungen aktiviert (Standard: aus); Speicherung im Verlauf, Statistiken |
| App-Einstellungen | Speicherung deiner Voreinstellungen |

Die App erhebt **keine** E-Mail-Adressen, Telefonnummern, Geburtsdaten, Standortdaten, Kontakte, Gerätekennungen oder Nutzungsstatistiken.

## 3. Wer ist datenschutzrechtlich verantwortlich?

Da keinerlei Daten an den Anbieter der App übermittelt werden, ist der Anbieter für die in der App erfassten Inhalte **weder Verantwortlicher noch Auftragsverarbeiter** im Sinne der DSGVO.

Wenn du Namen anderer Personen in die App einträgst – etwa Mitspieler oder Vereinsmitglieder – bist **du** bzw. dein Verein der Verantwortliche für diese Verarbeitung. Das umfasst insbesondere die Pflicht, die betroffenen Personen zu informieren und ihre Rechte (Auskunft, Berichtigung, Löschung) zu erfüllen. Die App unterstützt dich dabei: Spieler, Vereine und archivierte Partien lassen sich jederzeit vollständig in der App löschen.

## 4. Datenübertragung

### 4.1 Spieler-Export/Import (Datei)

Über „Einstellungen → Export players" lässt sich die lokale Spielerliste (Name + Vereinsname) als Datei exportieren. Die App übergibt diese Datei an das **Freigabemenü des Betriebssystems** (Android Share Sheet) – wie die Datei anschließend tatsächlich auf ein anderes Gerät gelangt (Nearby Share, Bluetooth, E-Mail, Kabel, Cloud-Speicher, …), entscheidet der Nutzer in diesem Moment selbst und liegt außerhalb der App.

- **Die App selbst öffnet zu keinem Zeitpunkt eine Netzwerkverbindung** für diese Funktion – weder als Server noch als Client. Sie schreibt lediglich eine Datei und ruft den System-Freigabedialog auf.
- Der Import erfolgt ebenso über den System-Dateiauswähler; die App liest nur die ausgewählte Datei, sucht nie selbstständig nach Geräten oder Daten.
- Beide Aktionen erfordern eine ausdrückliche Nutzeraktion (Antippen von „Export" bzw. „Import" und Auswahl im jeweiligen Systemdialog).
- **Zu beachten:** Die Exportdatei enthält Klarnamen. Wohin sie gelangt, bestimmst du mit der Wahl der Ziel-App – wählst du dort einen Cloud-Dienst oder E-Mail-Versand, verlassen die Namen dein Gerät und gelangen zu diesem Anbieter. Für diese Weitergabe bist du als Verantwortlicher zuständig (siehe Abschnitt 3); eine Übertragung per Direktverbindung (z. B. Nearby Share, Bluetooth, Kabel) vermeidet das.

### 4.2 OBS WebSocket

Verbindet sich – nach ausdrücklicher Aktivierung und Eingabe von IP-Adresse und Passwort in den Einstellungen – mit dem in OBS Studio eingebauten WebSocket-Server und schreibt den aktuellen Spielstand samt Spieler- und Vereinsnamen dort in vom Nutzer angelegte Textquellen, damit eine Streaming-Software (OBS Studio) sie einblenden kann. Hier ist die App der Client, OBS der Server – die App selbst ist im Netzwerk nicht erreichbar oder auffindbar.

- Muss in den Einstellungen ausdrücklich aktiviert und mit den Zugangsdaten des OBS-Rechners konfiguriert werden.
- Der Zugriff ist durch das in OBS vergebene Passwort geschützt (Challenge-Response-Verfahren, das Passwort selbst wird nie übertragen).
- Die Verbindung läuft unverschlüsselt (obs-websocket unterstützt kein natives TLS) – das Passwort verhindert unautorisierten Zugriff, schützt aber nicht vor Mitlesen durch andere Geräte im selben WLAN.
- **Wichtig:** Diese Funktion dient typischerweise dazu, Namen in einer öffentlichen Übertragung zu zeigen. Stelle sicher, dass die angezeigten Spieler damit einverstanden sind. Rechtsgrundlage hierfür ist regelmäßig deren Einwilligung; sie einzuholen liegt in deiner Verantwortung als Betreiber.

## 5. Sicherung durch das Betriebssystem

Die App **schließt ihre Datenbank mit Spielernamen, Spiel-, Turnier- und Trainingsergebnissen ausdrücklich von der Sicherung aus** – sowohl vom Android Auto Backup in dein Google-Konto als auch von der Geräte-zu-Geräte-Übertragung beim Einrichten eines neuen Geräts (`backup_rules.xml` / `data_extraction_rules.xml`). Diese personenbezogenen Daten verlassen dein Gerät also auch dann nicht, wenn du die Systemsicherung aktiviert hast; bei einem Gerätewechsel werden sie nicht mitgenommen.

Gesichert werden lediglich nicht-personenbezogene App-Voreinstellungen (z. B. gewähltes Design, Standard-Race-Ziele). Falls Android diese sichert, geschieht das ab Android 9 Ende-zu-Ende mit deiner Bildschirmsperre verschlüsselt und ist weder für Google noch für den Anbieter dieser App lesbar.

Du kannst die Systemsicherung jederzeit in den Android-Systemeinstellungen ganz deaktivieren.

## 5a. Automatische Löschung nach Aufbewahrungsfrist

Unter „Einstellungen → Daten & Datenschutz → Verlauf aufbewahren" legst du fest, wie lange beendete Spiele, Turniere und Trainingssessions im Verlauf bleiben. **Standard: 365 Tage.** Ältere Einträge werden beim App-Start automatisch gelöscht. Der Wert ist frei einstellbar; `0` bedeutet unbegrenzte Aufbewahrung. Zusätzlich löscht „Verlauf jetzt komplett löschen" den gesamten Verlauf sofort (die Spielerliste bleibt dabei erhalten).

## 6. Berechtigungen

Die App fordert **genau eine** Berechtigung an:

| Berechtigung | Wofür |
|---|---|
| Internet / Netzwerkzugriff | Ausschließlich für die ausgehende Verbindung zur OBS-WebSocket-Funktion (Abschnitt 4.2). Ist diese Funktion nicht aktiviert, baut die App zu keinem Zeitpunkt eine Netzwerkverbindung auf. |

Ausdrücklich **nicht** angefordert werden Standort, Kontakte, Kamera, Mikrofon, Telefonstatus oder Speicher-/Fotozugriff. Das Vereinswappen wird über die Android-Fotoauswahl (Photo Picker) gewählt – dabei erhält die App nur genau das eine ausgewählte Bild, aber keinen Zugriff auf die Fotogalerie.

Keine Berechtigung wird für Werbung, Tracking oder Standortermittlung genutzt.

## 7. Löschung

Alle Daten liegen ausschließlich auf deinem Gerät. Du löschst sie, indem du einzelne Einträge in der App entfernst, „Verlauf jetzt komplett löschen" nutzt, die Aufbewahrungsfrist ablaufen lässt (Abschnitt 5a) oder die App deinstallierst. Eine Anfrage beim Anbieter ist dafür nicht nötig und auch nicht möglich, da dem Anbieter keine Daten vorliegen.

**Löschen eines Spielers entfernt dessen Namen vollständig.** Archivierte Partien speichern keine Namen, sondern ausschließlich interne Kennnummern (IDs) der beteiligten Spieler; die Namen werden erst beim Anzeigen aus der Spielerliste nachgeschlagen. Wird ein Spieler gelöscht, verschwindet sein Name daher auch aus allen bereits archivierten Partien – zurück bleibt lediglich der reine Spielverlauf (Punkte, Aufnahmen) ohne Personenbezug. Ein separates Löschen des Archivs ist dafür nicht erforderlich, ist aber ebenfalls jederzeit möglich.

## 8. Kinder und Jugendliche

Die App richtet sich an alle Altersgruppen und erhebt selbst keine Daten. Werden Namen Minderjähriger eingetragen, gelten für dich als Verantwortlichen die besonderen Anforderungen an die Einwilligung – insbesondere im Zusammenhang mit der OBS-WebSocket-Funktion.

## 9. Änderungen

Wird diese Erklärung geändert, findest du die aktuelle Fassung stets unter dieser Adresse mit angepasstem Datum.

## 10. Kontakt

<!-- TODO vor Veröffentlichung ausfüllen: -->
    E-Mail: [Kontakt-E-Mail]
