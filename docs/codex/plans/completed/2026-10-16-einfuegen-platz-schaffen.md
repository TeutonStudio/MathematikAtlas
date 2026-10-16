# ExecPlan: Platz schaffen beim Ablegen auf einer Verbindung

## Status
2026-10-16, implementiert und unabhängig abgenommen mit Restpunkt Android-Laufzeit.
Gemeinsamer Android-/Desktop-Editor; Umsetzung durch SamAI.

## Ziel und Nutzerwirkung
Ein unverbundener Knoten teilt beim Ablegen eine kompatible Verbindung. Er bleibt
stehen, Quelle und Ziel rücken bei Bedarf horizontal auseinander; kollidierende
Nachbarn folgen lokal. Gesamte Geste einschließlich Verdrahtung ist ein Undo-Schritt.

## Nicht-Ziele
Keine Änderung des Einfügedialogs, mathematischer Semantik, Version oder Branch.
Kein globales Aufräumen bereits vorhandener Überlappungen.

## Untersuchter Istzustand
`VerbindungsPlan.planeKnotenInVerbindung` erzeugt geprüfte Graphvorschläge.
`KartenEditorZustand.beendeKnotenZiehen` übernimmt sie unmittelbar oder über eine
Anschlussauswahl. `beginneInteraktion`/`beendeInteraktion` bündeln die Historie.
`darstellungsKarte` enthält Inhaltsminima und Gruppenprojektion; Anhänge sind
flüchtige Geometrie. Vergleich: `VisuelleGruppeVerschieben`, `GruppenÜbergang`.
Bestehende lokale Anhang-/LaTeX-Änderungen bleiben erhalten. Baseline der drei
mitbearbeiteten Dateien liegt unter `/tmp/atlas-einfuege-layout-baseline`.

## Fachliche und mathematische Semantik
Nur Layout, keine Mathematikänderung. Abstand 32 Graph-Einheiten. Quelle links,
Ziel rechts; alle Y-Positionen bleiben unverändert. Nur ausgelöste Kollisionen
werden monoton nach außen weitergegeben, deterministisch nach Geometrie und ID.

## Daten-, Node-, Handle- und Edge-Vertrag
Stabile IDs und bestehende Kompatibilitätsprüfung bleiben maßgeblich. Keine
Schemaänderung. Interner Layoutplan liefert Positionsdeltas; Animation wird
nicht persistiert. Mehrdeutige Anschlussauswahl verschiebt erst nach Bestätigung.

## Architekturentscheidungen
Reine Layoutfunktion im fachneutralen Editor. Persistenter Endstand atomar über
`KartenInhaltErsetzen`; gemeinsame flüchtige Projektionsinterpolation für Knoten,
Anschlüsse, Verbindungen, Rahmen und Minimap (200 ms). Neue Bearbeitung beendet sie.
Gruppen als Blöcke; aktive Container um die Einfügung werden intern erweitert.

## Betroffene Dateien und Symbole
Neuer Layoutplan unter `logik/`, Integration in `KartenEditorZustand` und
Animationsantrieb in `KnotenKartenEditor`. Editor-Unit-, Desktop- und Gerätetests.

## Meilensteine
- [x] M1: Reine Layoutberechnung; 17 Layouttests erfolgreich.
- [x] M2: Atomare Integration, Anschlussauswahl, Animation, Historie/Persistenz.
- [x] M3: Desktop-Bedienprüfung, Gesamtchecks und unabhängige Abnahme erfolgreich.
  Android-Bedientest gebaut, Laufzeit mangels Gerät/AVD offen (siehe Prüfgrenze).

## Konkrete Umsetzungsschritte
1. Layoutbereiche, Gruppenblöcke, effektive Rechtecke und monotone Kollisionen berechnen.
2. Drag-Vorschläge um Layout ergänzen; erst nach validierter Bestätigung übernehmen.
3. Endstand speichern und gemeinsame Projektion transient interpolieren.
4. Fachneutrale Randfälle, echte Gesten und Unterbrechung prüfen.

## Tests und Validierung
Gezielt `:KnotenKartenVerwalter:testDebugUnitTest` und
`:desktopApp:test --tests '*KnotenVerbindungsDragDesktopTest*'`.
Danach `./gradlew test :app:assembleDebug :app:assembleDebugAndroidTest`,
`python3 scripts/pruefe_repository.py`, Android-Editorinstrumentierung.
Prüfungen und Umgebungsgrenzen werden unten tatsächlich protokolliert.

## Persistenz und Migration
Nur vorhandene Knoten-/Gruppenpositionen ändern sich; JSON-Roundtrip prüfen.
Keine Formatversion, Migration oder neue Produktionsabhängigkeit.

## Risiken und Rückfallstrategie
Gruppenprojektion und Inhaltsmessungen dürfen keine gespeicherten Körpergrößen
ersetzen. Animation darf keine zusätzlichen Auswertungen/Undo-Schritte auslösen.
Neue Interaktion springt auf den bereits gespeicherten Endstand. Ungültige oder
veraltete Anschlussauswahl behält die bisherige Drag-Abbruchsemantik.

## Fortschritt
- Ausgangsstand und reale Aufrufpfade geprüft; Implementierung begonnen.
- Beidseitige Layoutberechnung mit Gruppenblöcken, effektiven Inhaltsminima und
  Anhängen umgesetzt. Gruppenrahmen folgen auch der Ablage ohne Nachbarverschiebung.
- Zwei unabhängige Findings behoben: frühere unbewegte Hindernisse bei versetzter
  Quelle und gemessene Größen eingeklappter Gruppen. Cross-Side-Kollision breiter
  Rechtecke zusätzlich abgesichert. Regression über 200 deterministische Karten.
- Desktop-Touch und -Maus in jeweils acht Zoom-/Schrift-/Theme-Kombinationen
  erfolgreich; eigene Animation-/Undo-Prüfung und bestehender Einfüge-/Herauszieh-
  Test erfolgreich. Screenshots hell/dunkel visuell geprüft.

## Entscheidungsprotokoll
2026-10-16: Nutzer bestätigt beidseitiges Verschieben beim Ablegen, lokale
Mitnahme auch unverbundener Hindernisse, Gruppen zusammenhalten, nur Drag-Pfad.
32 Graph-Einheiten und 200 ms aus bestätigtem Plan übernommen.

## Abweichungen vom ursprünglichen Plan
Keine Produktabweichung. Die geplante Android-Laufzeitprüfung ist nicht ausführbar:
ADB meldet nach Abfrage außerhalb der Sandbox keine angeschlossenen Geräte;
`emulator -list-avds` liefert keine AVDs. Die Android-Testmatrix für Schrift 1/2,
Zoom 0,5/1 und beide Themes ist implementiert und als Test-APK gebaut. Diese
Umgebungsgrenze ist als offener Abnahmepunkt dokumentiert; die gemeinsame
Produktinteraktion wurde auf Desktop tatsächlich ausgeführt.

## Ergebnis und Verifikation
Finaler Befehl erfolgreich:

```bash
./gradlew test :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug :KnotenKartenVerwalter:assembleDebugAndroidTest --console=plain
python3 scripts/pruefe_repository.py
git diff --check
```

XML-Gesamtstand: 2.006 Fälle, davon **2.005 bestanden, 0 Fehler und 1 vorhandene
übersprungene Opt-in-Messung**. Editor 159/159 und Desktop 55/55 erfolgreich.
Neue Layoutsuite 17/17, neue Desktopsuite 3/3 erfolgreich. Debug-App,
App-Test-APK und Editor-Test-APK gebaut; Android-Lint erfolgreich.
Keine Geräteausführung behauptet. Standalone-Kernskript nicht zusätzlich nötig:
keine Rechenkernänderung, vollständige Gradle-Kerntests bestanden.

`node_verifier`: nach Korrekturen fachlich/technisch abgenommen mit Restpunkt
Android-Laufzeit; eigene unabhängige Modellprüfung mit 10.000 deterministischen
Karten ohne ausgelöste Kollision. Abschlussdiff gegenüber Aufgabenbaseline und
Repositorydiff geprüft; bestehende Fremdänderungen erhalten.

Branch unverändert `release/v3.4.0-methoden-grundraeume-visualisierung`, HEAD
`04925fc1352ad551622c1916fbd8f36c9ab83105`. Keine Commits/Pushes, daher keine
neuen Autor-/Committermetadaten. Keine Releaseversion reserviert oder verändert.
Architekturentscheidung: `docs/codex/decisions/2026-10-16-einfuege-layout.md`.
