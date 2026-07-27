# Mathematik Atlas – Repository-Anweisungen für Codex

## Projektziel

Der Mathematik Atlas visualisiert mathematische Vorgänge als interaktiven Node-Graph.

Projektbegriffe:

- **Knoten** sind React-Flow-Nodes.
- **Anschlüsse** sind Handles.
- **Verbindungen** sind Edges.
- Mathematische Vorgänge umfassen unter anderem Terme, Gleichungen, Rechnungen, Funktionen, Mengen, Relationen, Graphen und iterative Operatoren.

Verwendete Technologien:

- Vite
- React
- TypeScript oder JavaScript entsprechend dem bestehenden Repository
- React Flow
- shadcn/ui
- KaTeX
- die bereits im Repository vorhandenen Abhängigkeiten

Ermittle konkrete Versionen, Paketnamen, Skripte und Verzeichnisnamen immer aus dem Repository. Erfinde keine Struktur, die dem vorhandenen Code widerspricht.

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

Dokumente sind Orientierung, nicht Ersatz für Codeprüfung. Wenn Dokumentation und Code einander widersprechen, behandle den Code als aktuellen Istzustand, weise auf den Widerspruch hin und aktualisiere die Dokumentation im Rahmen der Aufgabe.

## Architekturregeln

- Trenne mathematische Semantik, Anwendungslogik, Graphintegration, Darstellung und Persistenz.
- Implementiere mathematische Regeln nicht ausschließlich in React-Komponenten.
- Ein Node visualisiert und bearbeitet ein fachliches Modell; er ist nicht selbst die alleinige Quelle der mathematischen Wahrheit.
- Verwende stabile, eindeutige IDs für Nodes, Handles und Edges.
- Definiere für jeden Anschluss Richtung, fachlichen Datentyp, Kardinalität und Kompatibilitätsregeln.
- Speichere keine React-Komponenten, Funktionen oder nicht serialisierbaren Laufzeitobjekte in persistierten Node-Daten.
- Registriere neue Node-Typen über das bestehende zentrale Registrierungs- oder Fabriksystem. Erzeuge kein paralleles Ersatzregister.
- Berücksichtige Laden, Speichern, Kopieren, Löschen, Undo/Redo und Migrationen, soweit diese Funktionen im Projekt existieren.
- Der Inspector verändert validierte Node-Daten und keine versteckten UI-Schattenzustände.
- KaTeX-Ausgaben werden aus fachlichen Daten oder Ausdrücken erzeugt und nicht als zweite, unabhängige Semantik gepflegt.
- Bestehende Abstraktionen sind zu erweitern, solange sie die neue Semantik korrekt tragen. Eine neue Abstraktion braucht eine konkrete Begründung.
- Keine neue Produktionsabhängigkeit ohne nachgewiesene Notwendigkeit.
- Keine beiläufigen Refactorings außerhalb des Aufgabenumfangs.

## Arbeitsweise

- Untersuche vor Änderungen die tatsächlichen Aufrufpfade und mindestens einen vergleichbaren vorhandenen Knoten.
- Bestimme den Paketmanager über vorhandene Lockdateien.
- Bestimme ausführbare Prüfungen aus `package.json` und der vorhandenen Toolkonfiguration.
- Verwende keine geratenen Befehle als angeblich erfolgreiche Prüfung.
- Ändere nur Dateien, die für die Aufgabe erforderlich sind.
- Bewahre bestehende öffentliche APIs, sofern die Aufgabe keine bewusste Änderung verlangt.
- Dokumentiere Annahmen, Risiken und Abweichungen vom Plan.
- Bei längeren Aufgaben führe einen ExecPlan nach `docs/codex/PLANS.md`.
- Halte `docs/codex/CURRENT_STATE.md` nur mit nachweisbaren Fakten aktuell.
- Halte dauerhafte Architekturentscheidungen als ADR unter `docs/codex/decisions/` fest.

## Neuer-Knoten-Workflow

Bei der Planung oder Implementierung eines neuen Node-Typs verwende den Skill `neuer-knoten`.

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

Für rein dekorative, organisatorische oder einfache Anzeige-Nodes ist die Rolle optional.

## Definition of Done

Eine Aufgabe ist nur abgeschlossen, wenn:

- das gewünschte Verhalten implementiert ist,
- fachliche und technische Randfälle behandelt sind,
- relevante Tests ergänzt oder begründet nicht ergänzt wurden,
- alle vorhandenen passenden Prüfungen ausgeführt wurden,
- Build-, Typ-, Lint- und Testfehler aus der Änderung behoben sind,
- Persistenz und Migration berücksichtigt wurden, falls Node-Daten verändert wurden,
- keine unnötigen Duplikate oder parallelen Abstraktionen entstanden sind,
- Dokumentation und Code denselben Zustand beschreiben,
- der abschließende Diff auf unbeabsichtigte Änderungen geprüft wurde.

## Abschlussbericht

Berichte am Ende knapp und überprüfbar:

1. umgesetztes Verhalten,
2. geänderte zentrale Dateien,
3. ausgeführte Prüfungen mit Ergebnis,
4. verbleibende Risiken oder bewusst nicht bearbeitete Punkte,
5. bei neuen Knoten: Node-Typ, Anschlüsse, Semantik, Inspector und Persistenz.
