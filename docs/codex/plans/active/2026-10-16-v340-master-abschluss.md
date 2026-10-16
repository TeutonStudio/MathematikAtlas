# v3.4.0 nach master integrieren

Status: Releaseprüfung läuft. Nutzerauftrag vom 16.10.2026: neuen Master-Commit erstellen.

## Ziel und Nutzerwirkung

Den vorhandenen Produktstand als genau einen geprüften Squashcommit `v3.4.0` auf `master` veröffentlichen. Enthalten sind Methodenquelle/Identität, angeschlossene Variablengrundräume und die vorhandenen Visualisierungs-/Darstellungskorrekturen.

## Nicht-Ziele

Keine weitere Produktentwicklung. Die vorbereitete Prädikatindex-/Operatorplanung ist kein implementiertes Releasefeature. Persönliche IDE-Dateien und Python-Bytecode bleiben außerhalb des Commits.

## Untersuchter Istzustand

Arbeitsbranch `samai/v3.4.0/methodenknoten`; HEAD und live bestätigter Remote-master `c4b23c69c431370b600cd4bd810c8d5135c43ff7` (`v3.3.1`). Produktänderungen sind uncommittet. `release/knotentypen.json` ergänzt exakt `mathematik.methode`; v3.4.0 ist dafür reserviert. Frühere Teilaufgabenprüfungen und verbleibende Grenzen sind in den bestehenden Fachplänen dokumentiert.

## Fachliche Semantik und Datenverträge

Dieser Abschluss übernimmt die vorhandenen Modelle und Anschlussverträge. Keine zusätzlichen mathematischen Regeln, Knoten, Anschlüsse oder Wire-Formate. Die Identitäts- und Methodenquellenverträge sowie die Variablengrundraummigration und Visualisierungsansicht sind durch die jeweiligen Fachpläne/ADRs beschrieben.

## Architekturentscheidungen

Bestehenden Release-Workflow verwenden: geprüfter SamAI-Branch, genau eine Zielversion, Squash-PR gegen master. `master_verwalter` besitzt Release-Metadaten und Git-/PR-Integration; der Koordinator prüft Produktumfang und aktuelle Tests. Keine neue Architekturentscheidung erforderlich.

## Betroffene Dateien und Symbole

Vorhandener Produktdiff in Rechenkern, Kartenadapter, neutralem Editor, Mathematikknoten, Android und Desktop. Release-Metadaten in `release/roadmap.toml`, `app/build.gradle.kts` sowie versionierten Desktop-/CI-Paketangaben. Prüfnachweis in diesem Plan und `CURRENT_STATE.md`.

## Meilensteine und konkrete Schritte

- [x] Basis und Klassifikation prüfen: Remote-master entspricht v3.3.1; genau ein neuer Katalogtyp rechtfertigt y/3.4.0.
- [x] Metadaten auf 3.4.0/3004000 abgeglichen; Desktoppaket und CI-Artefaktname ebenfalls 3.4.0. Explizite Commitliste umfasst 123 Dateien.
- [x] Lokale Repository-, Release-, JVM-, Desktop-, Build- und Lintprüfungen erfolgreich. Android-Gerätelaufzeit wird wegen gesperrtem Tablet aktuell durch die unabhängige Pflicht-CI ergänzt.
- [ ] Ausgewählte Dateien über SamAI-Skript committen, Identität prüfen, pushen und Release-PR eröffnen.
- [ ] Pflicht-CI abwarten, Squash `v3.4.0` integrieren und Remote-master/Releaseprüfungen verifizieren.

## Tests und Validierung

`pruefe_repository.py`, `pruefe_standardkarten.py`, `pruefe_releaseplan.py`, `pruefe_versionsfolge.py` und alle 14 Guardregressionen erfolgreich. Finaler Lauf `./gradlew test :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :desktopApp:compileKotlin` nach Metadatenabgleich erfolgreich (59 s, 248 Tasks; `/tmp/atlas-v340-release-final.log`). XML-Berichte: 1.975 JVM-/Desktop-Testfälle, 0 Fehler, 1 vorhandene Opt-in-Samplingmessung übersprungen; 1.974 bestanden. Reguläre APK besitzt wieder die ursprüngliche App-ID. PR-Pflichtchecks müssen vor Merge grün sein.

Zwei lokale Android-Laufzeitversuche unter separater Test-App-ID scheiterten beim ersten Compose-UI-Zugriff: keine Compose-Hierarchie. ADB bestätigt `mWakefulness=Dozing` und `deviceLocked=1`; Aufwecken schließt die Gerätesperre nicht. Nutzer wurde um Entsperren gebeten. Diese Versuche sind kein erfolgreicher Laufzeitnachweis; vollständige Android-Emulator-CI bleibt verpflichtend. Logs: `/tmp/atlas-v340-android-laufzeit.log` und `/tmp/atlas-v340-android-laufzeit-wach.log`.

## Persistenz und Migration

Vorhandene Persistenz-/Migrationstests ausführen. Der Releaseabschluss selbst ändert keine gespeicherten Nutzerdaten und ersetzt keine installierte persönliche App.

## Risiken und Rückfallstrategie

Kein Merge bei roten Pflichtprüfungen, falscher Basis oder unbeabsichtigten Dateien. Gradle benötigt Schreibzugriff auf den vorhandenen Cache außerhalb der Sandbox; Standalone-`kotlinc` fehlt, Gradle-Kerntests dienen als verfügbarer Kernnachweis. Das dokumentierte 60-Hz-Ziel der Visualisierung bleibt offen. Änderungen werden nicht pauschal zurückgesetzt.

## Fortschritt und Entscheidungsprotokoll

16.10.2026: Skill release-verwalten gelesen und master_verwalter beauftragt. Nutzerauftrag umfasst Veröffentlichung des vorhandenen Arbeitsstands; bestehende Regeln verlangen Squash-PR und erfolgreiche Pflichtchecks. Persönliche `.idea`-Änderungen und `scripts/__pycache__` werden ausgeschlossen.

16.10.2026: Vollständiger lokaler Prüfpfad nach Release-Metadatenabgleich grün. Dateiliste auf Eindeutigkeit, Existenz und Ausschluss von `.idea/`, `.kotlin/` und `scripts/__pycache__/` geprüft; `git diff --check` erfolgreich. Vorgängerbasis unverändert. Android-Gerätesperre ist eine dokumentierte Umgebungsgrenze und wird nicht als Produktfehler oder erfolgreiche Abnahme ausgegeben.

## Abweichungen vom ursprünglichen Plan

Keine. Konkrete Umgebungsgrenzen werden getrennt vom Prüfergebnis ausgewiesen.

## Ergebnis und Verifikation

Lokaler Releasekandidat geprüft; Commit, PR-Pflicht-CI und Merge stehen aus. Die noch ausstehenden Online-Ergebnisse werden im Release-PR nachvollziehbar festgehalten.
