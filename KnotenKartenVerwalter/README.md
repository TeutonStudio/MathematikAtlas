# KnotenKartenVerwalter

## Zweck

Dieses Modul stellt den fachneutralen Knoten- und Karteneditor bereit. Es verwaltet Graphdaten, Interaktion und Bearbeitung, ohne mathematische Regeln zu kennen.

## Zuständig für

- Karten-, Knoten-, Anschluss- und Verbindungsdaten,
- fachneutrale Editoraktionen und Undo/Redo,
- Auswahl, Verschieben, Verbinden und allgemeine Karteninteraktion.

## Nicht zuständig für

- mathematische Semantik,
- mathematische Anschlusskonventionen,
- konkrete mathematische Auswertung,
- anwendungsspezifische Navigation.

## Abhängigkeiten

Der Editor darf keine Mathematikregeln aus höheren Modulen voraussetzen.

## Öffentliche Verträge

Stabile IDs, Anschlussrichtung, Kardinalität, Verbindungsregeln und Undo/Redo-Verhalten sind zentrale Verträge dieses Moduls.

## Zentrale Einstiegspunkte

- `src/main/`
- `src/test/`
- `../docs/codex/NODE_CONTRACT.md`

## Änderungen an diesem Pfad

Prüfe bei Datenmodelländerungen insbesondere Persistenz, Kopieren, Löschen, Undo/Redo sowie Adapter- und Renderer-Verbraucher.

## Tests

Betroffene Modultests sowie die Repository-Prüfungen aus `../AGENTS.md`.
