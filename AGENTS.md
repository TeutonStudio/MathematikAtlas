# Mathematik Atlas – Repository-Anweisungen für Codex

## Projektziel

Der Mathematik Atlas ist eine native Android-Anwendung, die mathematische Vorgänge als interaktive Knotenkarten darstellt. Mathematische Objekte, Operationen und Umformungen werden als Knoten verbunden, topologisch ausgewertet und als versionierte Karten gespeichert.

Projektbegriffe:

- **Knoten** sind persistierbare `KnotenDaten`-Instanzen.
- **Anschlüsse** sind typisierte `AnschlussDaten`-Instanzen.
- **Verbindungen** sind `VerbindungDaten` zwischen zwei `AnschlussVerweis`-Instanzen.
- **Karten** sind versionierte `KartenDaten` mit Knoten, Verbindungen, Ansicht und optionalen visuellen Gruppen.
- Mathematische Vorgänge umfassen unter anderem Terme, Gleichungen, Rechnungen, Funktionen, Mengen, Relationen, Graphen, Geometrie und iterative Operatoren.

Verwendete Technologien:

- Kotlin und Gradle mit Kotlin-DSL,
- Android und Jetpack Compose,
- Material 3,
- ein Android- und Compose-freier Kotlin-Rechenkern,
- ein nativer Compose-Renderer für den vom Rechenkern erzeugten LaTeX-Teilumfang,
- die bereits im Repository vorhandenen Abhängigkeiten.

Das Projekt verwendet ausdrücklich **nicht** Vite, React, React Flow, shadcn/ui oder KaTeX. Ermittle konkrete Versionen, Plugins, Tasks, Pakete und Verzeichnisnamen immer aus dem Repository.

## Verbindliche Lektüre

Lies vor Architektur- oder Implementierungsarbeit die für die Aufgabe relevanten Dateien:

1. `docs/codex/PROJECT_CONTEXT.md`
2. `docs/codex/CURRENT_STATE.md`
3. `docs/codex/ARCHITECTURE.md`
4. `docs/codex/NODE_CONTRACT.md`
5. bei neuen Knoten zusätzlich `docs/codex/NEW_NODE_WORKFLOW.md`
6. bei größeren Änderungen zusätzlich `docs/codex/PLANS.md`
7. bei Reviews zusätzlich `docs/codex/CODE_REVIEW.md`
8. bei Tests zusätzlich `docs/codex/TEST_STRATEGY.md`

Dokumente sind Orientierung, nicht Ersatz für Codeprüfung. Wenn Dokumentation und Code einander widersprechen, behandle den Code als aktuellen Istzustand, weise auf den Widerspruch hin und aktualisiere die betroffene Dokumentation im Rahmen der Aufgabe. Ein älterer Verifikationsstand in `CURRENT_STATE.md` darf nicht als Beleg für neuere Commits ausgegeben werden.

## Modulgrenzen

- `MathematikRechenSystem` enthält die mathematische Domäne und bleibt frei von Android, Compose und Karteneditor-Abhängigkeiten.
- `KnotenKartenVerwalter` enthält den fachneutralen Karteneditor, Graphdaten, Interaktion und Undo/Redo. Es darf keine Mathematikregeln oder knotenspezifischen Parameterkonventionen kennen.
- `MathematikKartenAdapter` verbindet Graph und Rechenkern, führt Karten topologisch aus und verwaltet Auswertungsergebnisse und Cache.
- `MathematikKnoten` enthält mathematische Vorlagen, Anschlussarten, Auswerter und spezialisierte Compose-Renderer.
- `app` koordiniert Kartenbibliothek, Navigation, Inspector, Persistenz, Import/Export und anwendungsspezifische Dialoge.

Abhängigkeiten dürfen nur in der durch die Gradle-Module vorgegebenen Richtung erweitert werden. Eine bequeme Abkürzung ist keine neue Architektur, sondern meist nur technische Schuld mit optimistischem Namen.

## Architekturregeln

- Trenne mathematische Semantik, Graphzustand, Anwendungskoordination, Darstellung und Persistenz.
- Implementiere mathematische Regeln im Rechenkern oder in dafür vorgesehenen Auswertern, nicht ausschließlich in Composables oder Pointer-Handlern.
- Ein Knoten visualisiert und konfiguriert ein fachliches Modell; er ist nicht alleinige Quelle der mathematischen Wahrheit.
- Verwende stabile, eindeutige IDs für Karten, Knoten, Anschlüsse, Verbindungen und visuelle Gruppen.
- Definiere für jeden Anschluss Richtung, fachlichen Datentyp, Kardinalität, Reihenfolge und Kompatibilitätsregeln.
- Speichere keine Composables, Funktionen, `State`-Objekte oder andere Laufzeitobjekte in persistierten Daten.
- Registriere mathematische Knotenvorlagen über den vorhandenen Vorlagenkatalog und Auswerter über das bestehende Auswerterregister. Erzeuge kein paralleles Ersatzregister.
- Berücksichtige Laden, Speichern, Kopieren, Löschen, Undo/Redo und Migrationen, soweit die Änderung diese Pfade betrifft.
- Der Inspector verändert validierte `KnotenDaten` über die vorgesehenen Kartenaktionen und keine unabhängigen UI-Schattenzustände.
- LaTeX wird aus fachlichen Objekten oder Ausdrücken erzeugt. Eine alternative Darstellung darf die mathematische Semantik nicht heimlich ersetzen.
- Der neutrale Karteneditor darf keine Namen wie `festeEingänge`, mathematische Knotenschlüssel oder sonstige Konventionen höherer Module voraussetzen.
- Bestehende Abstraktionen sind zu erweitern, solange sie die neue Semantik korrekt tragen. Eine neue Abstraktion braucht eine konkrete Begründung.
- Keine neue Produktionsabhängigkeit ohne nachgewiesene Notwendigkeit.
- Keine beiläufigen Refactorings außerhalb des Aufgabenumfangs.

## Arbeitsweise

- Untersuche vor Änderungen die tatsächlichen Aufrufpfade und mindestens einen vergleichbaren vorhandenen Knoten oder Editorpfad.
- Bestimme ausführbare Prüfungen aus Gradle-Konfiguration, Skripten und CI. Suche nicht nach `package.json`, wenn das Repository ein Android-Gradle-Projekt ist.
- Verwende keine geratenen Befehle als angeblich erfolgreiche Prüfung.
- Ändere nur Dateien, die für die Aufgabe erforderlich sind.
- Bewahre bestehende öffentliche APIs, sofern die Aufgabe keine bewusste Änderung verlangt.
- Dokumentiere Annahmen, Risiken und Abweichungen vom Plan.
- Bei längeren Aufgaben führe einen ExecPlan nach `docs/codex/PLANS.md`.
- Halte `docs/codex/CURRENT_STATE.md` nur mit nachweisbaren Fakten aktuell.
- Halte dauerhafte Architekturentscheidungen als ADR unter `docs/codex/decisions/` fest.

## Standardprüfungen

Leite die tatsächlich passenden Befehle aus dem Bestand ab. Der derzeit übliche Prüfpfad ist:

```bash
python3 scripts/pruefe_repository.py
python3 scripts/pruefe_kern.py
./gradlew test
./gradlew :app:assembleDebug
```

Ein nicht ausführbarer Befehl wird mit konkretem Grund dokumentiert. Ein erfolgreicher Build ersetzt keinen Laufzeittest auf Emulator oder Gerät.

## Neuer-Knoten-Workflow

Bei der Planung oder Implementierung eines neuen Knotentyps verwende den Skill `neuer-knoten`.

Die Rollen werden grundsätzlich in dieser Reihenfolge eingesetzt:

1. `node_planner` untersucht den Bestand und erstellt eine ausführbare Spezifikation.
2. `math_reviewer` prüft bei mathematisch nicht trivialen Knoten die fachliche Semantik.
3. `node_implementer` setzt den bestätigten Plan um.
4. `node_verifier` prüft unabhängig Diff, Verhalten, Architektur und Tests.
5. Blockierende Findings gehen zurück an `node_implementer`.
6. `node_verifier` führt anschließend die Abschlussprüfung durch.

Planer, Mathematikprüfer und Verifizierer dürfen keine Produktionsdateien verändern. Es arbeitet höchstens ein schreibender Implementierungsagent gleichzeitig.

## Wann der Mathematikprüfer erforderlich ist

Setze `math_reviewer` ein, wenn mindestens einer dieser Punkte zutrifft:

- Der Knoten repräsentiert eine mathematische Operation, Relation, Funktion, Menge oder Transformation.
- Definitionsbereich, Wertebereich, Bindungsvariablen oder Gültigkeitsbedingungen sind relevant.
- Assoziativität, Kommutativität, Distributivität, Neutralität, Inversen oder partielle Definitionen beeinflussen das Verhalten.
- Der Knoten aggregiert, iteriert, löst, differenziert, integriert, transformiert oder erzeugt eine Lösungsmenge.
- Mehrdeutige mathematische Notation könnte zu einer falschen Datenstruktur führen.

Für rein dekorative, organisatorische oder einfache Anzeige-Knoten ist die Rolle optional.

## Definition of Done

Eine Aufgabe ist nur abgeschlossen, wenn:

- das gewünschte Verhalten implementiert ist,
- fachliche und technische Randfälle behandelt sind,
- relevante Tests ergänzt oder begründet nicht ergänzt wurden,
- alle vorhandenen passenden Prüfungen ausgeführt wurden,
- Build- und Testfehler aus der Änderung behoben sind,
- Persistenz und Migration berücksichtigt wurden, falls Daten verändert wurden,
- keine unnötigen Duplikate oder parallelen Abstraktionen entstanden sind,
- Dokumentation und Code denselben Zustand beschreiben,
- der abschließende Diff auf unbeabsichtigte Änderungen geprüft wurde.

## Abschlussbericht

Berichte am Ende knapp und überprüfbar:

1. umgesetztes Verhalten,
2. geänderte zentrale Dateien,
3. ausgeführte Prüfungen mit Ergebnis,
4. verbleibende Risiken oder bewusst nicht bearbeitete Punkte,
5. bei neuen Knoten: Typ-Schlüssel, Anschlüsse, Semantik, Inspector und Persistenz.