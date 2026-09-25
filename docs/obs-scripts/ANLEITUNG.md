# OBS-Einrichtung – Anleitung

Zwei unabhängige Schritte: das **Wappen-Umschalt-Script** (Lua) und **obs-websocket**
(Push-Anbindung der App an OBS). Beides kann unabhängig voneinander eingerichtet werden.

---

## Teil 1: Wappen-Umschalt-Script (`club_crest_switcher.lua`)

Schaltet automatisch ein Vereinswappen-Bild um, sobald sich der angezeigte Vereinsname
ändert – ohne dass die App das Bild überträgt. Die Zuordnung läuft rein über den Namen.

### Voraussetzungen in der OBS-Szene

Du brauchst bereits eingerichtet:
- Zwei **Textquellen**, die die Vereinsnamen von Spieler 1 / Spieler 2 anzeigen (z. B. über
  die Browser-Quelle der App, oder eigene Textquellen).
- Zwei **Bildquellen** (Image Source), eine pro Spieler, die das Wappen zeigen sollen.

### Schritt für Schritt

1. **Ordner für Wappen anlegen**, z. B. `C:\OBS\wappen`.
2. Für jeden Verein eine PNG-Datei dort ablegen, benannt nach dem **Vereinsnamen in
   „Slug"-Form**: klein geschrieben, Leerzeichen/Sonderzeichen durch `-` ersetzt.

   | Vereinsname in der App | Erwarteter Dateiname |
   |---|---|
   | `PBC Musterstadt` | `pbc-musterstadt.png` |
   | `PBC Bremen` | `pbc-bremen.png` |
   | `1. BC Hamburg` | `1-bc-hamburg.png` |

   Umlaute werden dabei in die gängige deutsche Schreibweise übertragen: `ä`→`ae`,
   `ö`→`oe`, `ü`→`ue`, `ß`→`ss`. Beispiel: `PBC Schönberg` → `pbc-schoenberg.png`.
3. Optional: eine `no-crest.png` (oder `default.png`) im selben Ordner ablegen – wird
   gezeigt, wenn kein Verein passt (z. B. Spieler ohne Verein).
4. In OBS: **Tools → Scripts → „+"** → `club_crest_switcher.lua` auswählen.
5. Im erscheinenden Einstellungsbereich des Scripts ausfüllen:
   - **Club 1 text source** – Name der Textquelle für Verein 1
   - **Club 2 text source** – Name der Textquelle für Verein 2
   - **Club 1 image source** – Name der Bildquelle für Verein 1
   - **Club 2 image source** – Name der Bildquelle für Verein 2
   - **Crest folder** – der Ordner aus Schritt 1
6. Fertig. Sobald sich ein Vereinsname ändert (neue Partie, neuer Gegner), schaltet
   das Script automatisch das passende Wappen um – alle 750ms wird geprüft.

### Neuen Verein hinzufügen (auch bei vielen Vereinen, z. B. 50)

Das Konfigurationsfenster des Scripts (Textquellen, Bildquellen, Ordner) füllst du **nur
einmal** aus – unabhängig davon, ob du 2 oder 50 Vereine hast. Ein weiterer Verein bedeutet
**nur**: eine weitere PNG-Datei mit passendem Namen in denselben Ordner legen. Kein erneutes
Öffnen des Script-Panels, kein Neustart von OBS nötig.

### Fehlersuche

- **Wappen fehlt oder falsch?** Öffne **Tools → Scripts → Script Log** in OBS. Sobald ein
  Vereinsname ankommt, für den keine passende Datei gefunden wurde, erscheint dort eine
  Warnzeile mit dem **exakt erwarteten Dateinamen** – praktisch beim Einpflegen vieler
  Vereine, um Tippfehler zwischen App-Vereinsname und Dateiname schnell zu finden.
- **Testen ohne die App/ein laufendes Match:** Öffne die Eigenschaften der `club1`- bzw.
  `club2`-Textquelle direkt in OBS und tippe testweise einen Vereinsnamen ein. Das Script
  reagiert auf jede Änderung der Textquelle, egal ob sie von der App oder manuell kommt –
  so lässt sich die ganze Vereinsliste durchklicken, ohne ein Match starten zu müssen.
- Quellnamen exakt wie in der OBS-Szenenliste eintragen (Groß-/Kleinschreibung zählt).
- Nach einer Änderung an der `.lua`-Datei selbst: in Tools → Scripts das Script markieren
  und auf das Reload-Symbol (kreisförmiger Pfeil) klicken, damit die Änderung greift.

---

## Teil 2: obs-websocket einrichten

`obs-websocket` ist ab OBS Studio 28 bereits **fest eingebaut** – keine separate
Installation nötig. Die App verbindet sich als Client damit (Einstellungen →
„OBS WebSocket (Push)").

### Schritt für Schritt

1. In OBS: **Tools → WebSocket Server Settings**.
2. Häkchen setzen bei **„Enable WebSocket server"**.
3. **Server Port** notieren (Standard: `4455`).
4. Häkchen setzen bei **„Enable Authentication"** (unbedingt empfohlen – ohne
   Passwort kann sich jeder im selben WLAN verbinden und OBS fernsteuern).
5. Über **„Generate Password"** ein Passwort erzeugen oder ein eigenes eintragen –
   dieses Passwort wird später in der App hinterlegt.
6. **„Show Connect Info"** anzeigen lassen – dort stehen Server-IP, Port und ein
   QR-Code für die Einrichtung.
7. Mit **OK** bestätigen.

### Was das bewirkt (zur Einordnung)

- OBS ist jetzt der **Server**, die App wird später der **Client** – umgekehrt zu
  heute, wo die App den Server stellt.
- Ohne das richtige Passwort kann niemand eine Verbindung aufbauen und Daten lesen
  oder OBS-Einstellungen ändern.
- **Wichtig zu wissen:** Die Verbindung selbst ist **nicht verschlüsselt**
  (`obs-websocket` unterstützt kein natives TLS/`wss://` – das Passwort schützt vor
  unautorisiertem Zugriff, aber nicht vor Mitlesen der Daten im selben WLAN). Das
  entspricht dem heutigen Stand, ist also keine Verschlechterung.

### In der App einrichten

In YAPBS: **Einstellungen → OBS WebSocket (Push)** → Schalter aktivieren, dann:
- **OBS-Rechner IP-Adresse** – die Adresse aus „Show Connect Info" (Schritt 6 oben)
- **Port** – Standard `4455`
- **Passwort** – das in Schritt 5 vergebene Passwort

Der Status darunter zeigt sofort, ob die Verbindung steht: „Verbunden" (grün/primärfarben),
„Verbinde…", „Falsches Passwort" oder „Verbindung fehlgeschlagen" (jeweils rot). Bei
falschem Passwort einfach das Feld korrigieren – die App verbindet sich automatisch neu.

### Textquellen in OBS anlegen

Die App schreibt in **Textquellen mit festen Namen**. Lege in deiner Szene Textquellen mit
genau diesen Namen an (nur die, die du tatsächlich brauchst):

| Quellname | Inhalt |
|---|---|
| `name1` / `name2` | Spielername |
| `club1` / `club2` | Vereinsname |
| `score1` / `score2` | Punktestand |
| `run1` / `run2` | Aktuelle Serie (nur 14.1) |
| `highestbreak1` / `highestbreak2` | Höchstserie (nur 14.1) |
| `innings1` / `innings2` | Aufnahmen (nur 14.1) |
| `raceto` | Race-to-Ziel |
| `discipline` | Aktuelle Disziplin |
| `turn1` / `turn2` | „●" wenn am Zug, sonst „○" |

**Wichtig:** Wappen werden über diesen Weg **nicht** übertragen – dafür Teil 1 (Lua-Script)
oben nutzen. `club1`/`club2` liefern hier nur den Vereinsnamen als Text, den das Lua-Script
dann zum Umschalten des passenden Bildes verwendet.

### Denselben Wert zweimal in einer Szene anzeigen (z. B. Name links UND rechts)

**„Einfügen (Verweis)" (Strg+V) funktioniert dafür nur zuverlässig, wenn die zweite
Platzierung in einer ANDEREN Szene liegt.** Fügst du sie in dieselbe Szene ein, in der die
Quelle schon existiert, kann OBS sie nicht wirklich als Referenz doppeln (Quellnamen müssen
global eindeutig sein) und legt stattdessen eine **abgekoppelte, unabhängige Kopie** an – die
App schreibt aber weiterhin nur auf die ursprüngliche Quelle, die Kopie bleibt dann auf altem
Stand stehen.

Zuverlässige Lösung: **`text_mirror.lua`** (liegt bereits unter `docs/obs-scripts/`). Es
kopiert den Text einer Quelle laufend (alle 250 ms) in eine zweite, echte, unabhängige
Textquelle – unabhängig von Positionierung, Ausrichtung oder Szene.

1. In OBS: **Tools → Scripts → „+"** → `text_mirror.lua` auswählen.
2. Lege in der Szene eine zweite, eigenständige Textquelle an (z. B. `table1_name1_right`,
   über Quelle hinzufügen → Text, **nicht** per Kopieren/Einfügen).
3. Im Konfigurationsbereich des Scripts ein Paar ausfüllen:
   - **Source text source 1** → `table1_name1` (die von der App beschriebene Original-Quelle)
   - **Target text source 1** → `table1_name1_right` (die neue, unabhängige Quelle)
4. Fertig – `table1_name1_right` folgt jetzt automatisch dem Text von `table1_name1`, lässt
   sich aber völlig unabhängig positionieren, skalieren und ausrichten (z. B. rechtsbündig).
5. **Ein Script-Aufruf reicht für bis zu 12 solcher Paare** – du musst also nicht wie beim
   Wappen-Script mehrere Kopien der Datei anlegen. Trage für jeden weiteren gewünschten
   Doppel-Wert (z. B. Tisch 2, 3, 4, oder auch `name2`) einfach das nächste Paar
   „Source text source 2" / „Target text source 2" usw. ein.

### Mehrere Tische in einer Szene (mehrere Tablets → ein OBS)

Verbinden sich mehrere Tablets mit **derselben** OBS-Instanz (z. B. 4 Tische, 1 Regie-PC),
würden sie ohne weitere Einstellung alle auf dieselben Textquellen (`name1`, `score1`, …)
schreiben und sich gegenseitig überschreiben. Dafür gibt es das Feld **„Tisch-Präfix"** in
den App-Einstellungen, direkt unter dem Passwort-Feld:

1. Trage auf **jedem** Tablet einen eigenen, kurzen Wert ein, z. B. `tisch1`, `tisch2`,
   `tisch3`, `tisch4`.
2. Die App schreibt dann nicht mehr auf `name1`, sondern auf `tisch1_name1` (bzw. `tisch2_name1`
   usw.) – nach dem Muster `<präfix>_<quellname>`.
3. Lege in der OBS-Szene entsprechend **einen vollständigen Satz Textquellen pro Tisch** an,
   also z. B. `tisch1_name1`, `tisch1_score1`, `tisch1_club1`, … sowie dieselbe Liste noch
   einmal mit `tisch2_`, `tisch3_`, `tisch4_`.
4. Ordne die vier Quellsätze in der Szene so an, wie du die vier Tische zeigen möchtest
   (z. B. Vierfach-Split, oder vier separate Kästen).

Bei nur einem Tisch bleibt das Feld einfach **leer** – dann gelten weiterhin die unpräfixten
Namen aus der Tabelle oben, ohne dass du etwas ändern musst.

**Lua-Wappen-Script pro Tisch – als eigene Datei, nicht als zweiter Listeneintrag:**
Für jeden Tisch brauchst du eine eigene Instanz von `club_crest_switcher.lua` (Teil 1), damit
sich die Wappen der Tische nicht gegenseitig überschreiben. OBS verknüpft die Einstellungen
eines Scripts aber über den **Dateipfad** – lädst du dieselbe `.lua`-Datei zweimal über „+",
landen beide Einträge auf derselben Konfiguration statt zwei getrennten. Deshalb:

1. Kopiere `club_crest_switcher.lua` einmal pro Tisch, z. B. zu `club_crest_switcher_tisch1.lua`,
   `club_crest_switcher_tisch2.lua`, `club_crest_switcher_tisch3.lua`,
   `club_crest_switcher_tisch4.lua` (liegen bereits fertig unter `docs/obs-scripts/`).
2. In OBS: **Tools → Scripts → „+"** und **jede** dieser Kopien einzeln hinzufügen – du bekommst
   vier unabhängige Einträge in der Liste.
3. Jede Kopie separat konfigurieren, mit den Textquellen des jeweiligen Tisches:
   - `club_crest_switcher_tisch1.lua` → Club 1/2 text source: `tisch1_club1` / `tisch1_club2`
   - `club_crest_switcher_tisch2.lua` → `tisch2_club1` / `tisch2_club2`
   - usw.
   - Crest folder kann bei allen vier derselbe Ordner sein – die Zuordnung läuft ja über den
     Vereinsnamen, nicht über den Tisch.
4. Nach einer Änderung am Script-**Code** selbst (z. B. einem Update dieser Datei) müssen alle
   vier Kopien einzeln aktualisiert werden – am einfachsten, indem du die Ursprungsdatei erneut
   auf alle vier Kopien überträgst und in OBS bei jedem Eintrag auf das Reload-Symbol klickst.
