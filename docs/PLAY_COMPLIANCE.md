# Google Play – Compliance-Unterlagen

Arbeitsdokument für die Veröffentlichung. Keine Rechtsberatung – die Einschätzungen unten stützen
sich auf den Wortlaut der jeweils verlinkten Google-Richtlinien (Stand August 2026).

---

## 1. Data-Safety-Formular

### Empfohlene Antworten

| Frage im Play-Console-Formular | Antwort |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| Is all of the user data collected by your app encrypted in transit? | *(entfällt bei „No")* |
| Do you provide a way for users to request that their data is deleted? | *(entfällt bei „No")* |
| Privacy policy URL | Pflichtfeld – URL der gehosteten `DATENSCHUTZ.md` |

### Begründung – warum „No"

Google definiert *Collection* als „transmitting data from your app **off a user's device**" und nennt
diese Ausnahmen:

1. **Nur auf dem Gerät verarbeitet** – „User data accessed by your app that is only processed locally
   on the user's device and not sent off device does not need to be disclosed."
2. **Ende-zu-Ende verschlüsselt** – „User data that is sent off device, but that is unreadable by you
   or anyone other than the sender and recipient … does not need to be disclosed."
3. **Ephemer verarbeitet.**

Für *Sharing* gilt zusätzlich die Ausnahme: „Transferring user data to a third party based on a
**specific user-initiated action**, where the user reasonably expects the data to be shared."

Zuordnung der App-Funktionen:

| Funktion | Einordnung |
|---|---|
| Room-Datenbank (Spieler, Vereine, Partien, Trainingssessions) | Ausnahme 1 – verlässt das Gerät nie; zusätzlich per `backup_rules.xml` / `data_extraction_rules.xml` von Auto Backup **und** Geräte-zu-Geräte-Transfer ausgeschlossen |
| Spieler-Export/Import (Datei) | Die App selbst öffnet **keine** Netzwerkverbindung – sie schreibt eine Datei und ruft Androids Share-Sheet auf. Die eigentliche Übertragung übernimmt die vom Nutzer gewählte Ziel-App (Nearby Share, Bluetooth, Mail, …), nicht diese App. Zusätzlich greift ohnehin die Sharing-Ausnahme „specific user-initiated action" |
| Android Auto Backup | Die personenbezogene Datenbank ist ausgeschlossen (siehe oben); nur nicht-personenbezogene Voreinstellungen können gesichert werden – Plattformfunktion des Betriebssystems, landet im **eigenen** Google-Konto des Nutzers, ab Android 9 E2E-verschlüsselt (`minSdk = 28` stellt das sicher) |
| OBS WebSocket | siehe Abschnitt 1.1 – der einzige Punkt mit Auslegungsspielraum |

Entscheidend: Es existiert **kein Backend des Anbieters**. Es gibt keinen Ort, an den Daten fließen
könnten. Keine Analytics-, Crash-Reporting- oder Werbe-SDKs sind eingebunden (prüfbar in
`app/build.gradle.kts`).

### 1.1 Der eine Punkt mit Auslegungsspielraum: OBS WebSocket

**Ehrliche Einordnung:** Die App verbindet sich als Client mit OBS' eingebautem WebSocket-Server
und schreibt Spielernamen unverschlüsselt (obs-websocket unterstützt kein natives TLS) dorthin.
Damit greift Ausnahme 2 hier *nicht* sauber – anders als beim Sync. Zugriffsschutz besteht über
ein Passwort (Challenge-Response), das nie im Klartext übertragen wird – das ist Zugriffskontrolle,
keine Transportverschlüsselung.

Warum die Antwort trotzdem „No" lauten sollte:

- Der Anbieter empfängt nichts. Das Formular beschreibt ausdrücklich die Datenpraktiken *des
  Entwicklers*; ein „Ja" würde Nutzern fälschlich signalisieren, dass Daten beim Anbieter landen.
- Die Sharing-Ausnahme „specific user-initiated action" greift eindeutig: Die Funktion ist
  standardmäßig aus und wird bewusst mit vom Nutzer eingegebenen Zugangsdaten aktiviert.
- Die Daten verlassen das lokale Netzwerk nicht; die App selbst ist zudem nicht auffindbar/erreichbar
  (kein Server, keine Netzwerk-Ankündigung) – anders als bei der vorherigen HTTP-Server-Variante.

**Warum kein TLS für OBS WebSocket:** obs-websocket selbst unterstützt kein natives `wss://` –
das ist eine Einschränkung des OBS-Projekts, keine Entscheidung dieser App. Ein Reverse-Proxy/Tunnel
wäre für die Zielgruppe (Vereins-Setup) nicht praktikabel.

**Falls Google im Review nachfragt:** Verweis auf die Sharing-Ausnahme und darauf, dass kein Backend
existiert. Fallback wäre, die Datenarten „Name" und „Other info" als *collected, not shared* zu
deklarieren – schlechter für die Store-Darstellung, aber unstrittig.

---

## 2. Datenschutzerklärung

Entwurf liegt unter [`DATENSCHUTZ.md`](DATENSCHUTZ.md). Vor Veröffentlichung:

- [ ] Platzhalter für Name, Anschrift, Kontakt-E-Mail ausfüllen
- [ ] Unter einer stabilen, öffentlich erreichbaren URL hosten (z. B. auf `rbuck.info`)
- [ ] URL im Play-Console-Formular hinterlegen
- [ ] Erreichbarkeit ohne Login prüfen – Google verlangt eine frei zugängliche Seite

---

## 3. DSA-Händlerstatus (EU)

Seit Februar 2024 verlangt Google Play für die EU-Distribution eine Angabe zum Händlerstatus.

**Entscheidungspunkt:**

| | Trader | Non-Trader |
|---|---|---|
| Wann | App erzielt Einnahmen (Kaufpreis, In-App-Käufe, Werbung) | rein kostenlos, ohne Monetarisierung |
| Folge | **Name, Anschrift, Telefonnummer und E-Mail werden öffentlich auf der Store-Seite angezeigt** | keine öffentliche Anschrift |

> **Update (30.08.2026):** Die App soll kostenpflichtig werden (Preis noch offen) → damit ist
> **„Trader" gesetzt**, nicht mehr optional. Offen ist nur noch, **welche** Anschrift/Telefonnummer
> öffentlich erscheint:
>
> - **Privatanschrift** – kostenlos, aber Adresse/Telefonnummer sind für jeden Store-Besucher sichtbar.
> - **Impressumsdienst / virtuelle Geschäftsadresse** – Privatanschrift bleibt raus, laufende Kosten
>   (ca. 5–15 €/Monat je Anbieter, z. B. für ein Gewerbe/Freiberufler-Impressum).
>
> Entscheidung wurde bewusst vertagt – siehe Checkliste unten.

- [ ] **Offene Entscheidung:** private Anschrift vs. Impressumsdienst für den Händler-Eintrag
- [ ] Händlerstatus in der Play Console auf „Trader" festlegen, sobald obige Entscheidung steht

---

## 4. Testanforderung für neue Privatkonten

Für **Privatkonten, die nach dem 13.11.2023 angelegt wurden**: geschlossener Test mit mindestens
**12 Testern**, die **14 Tage durchgehend** angemeldet sind, bevor die Produktionsfreigabe beantragt
werden kann. Organisationskonten und ältere Privatkonten sind ausgenommen.

Wichtig: Die 14 Tage müssen zusammenhängend sein. Tester, die zwischendurch aussteigen, zählen nicht.

- [ ] Kontotyp und Anlagedatum prüfen
- [ ] Falls betroffen: Testerliste rechtzeitig aufbauen (Zeitpuffer einplanen)

---

## 5. Checkliste vor Einreichung

- [ ] `DATENSCHUTZ.md` ausgefüllt und gehostet
- [ ] Data-Safety-Formular ausgefüllt (Abschnitt 1)
- [ ] Händlerstatus + Anschriftfrage festgelegt (Abschnitt 3)
- [ ] Testanforderung geklärt (Abschnitt 4)
- [ ] Altersfreigabe-Fragebogen ausgefüllt
- [ ] `isMinifyEnabled = true` für den Release-Build erwägen
- [ ] `versionCode` / `versionName` auf einen Release-Stand gesetzt
- [ ] Prüfen, dass weiterhin keine Analytics-/Tracking-SDKs eingebunden sind
- [x] Release-Signing-Keystore erzeugt (siehe [`keystore/README.md`](../keystore/README.md)) - **den
      Keystore + `keystore.properties` sofort an einem zweiten Ort sichern**, sonst sind spätere
      App-Updates nicht mehr signierbar
- [ ] Preis festlegen (aktuell offen) und in der Play Console eintragen
- [ ] Store-Listing-Texte (Kurzbeschreibung, lange Beschreibung) verfassen
- [ ] Screenshots unter `screenshots/` aktualisieren - Stand vor dem YAPBS-Rebrand (Name/Logo/Vintage-Theme)
- [ ] `.aab` statt `.apk` für den Play-Console-Upload bauen (`./gradlew bundleRelease`)

---

## Quellen

- [Play Console – Data safety](https://support.google.com/googleplay/android-developer/answer/10787469)
- [Play Console – User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311)
- [Play Console – App testing requirements](https://support.google.com/googleplay/android-developer/answer/14151465)
- [Android – Auto Backup](https://developer.android.com/identity/data/autobackup)
