# MathematikRechenSystem

## Zweck

Dieses Modul enthält die mathematische Domäne und die fachliche Semantik des Mathematik Atlas. Es soll ohne Android, Compose und Karteneditor verwendbar bleiben.

## Zuständig für

- mathematische Werte, Mengen, Relationen, Methoden und Strukturen,
- mathematische Definitionen und fachliche Invarianten,
- domänennahe Operationen und Auswertung ohne UI-Abhängigkeit.

## Nicht zuständig für

- Compose- oder Android-Darstellung,
- Karteneditor-Zustand und Pointer-Interaktion,
- Inspector-UI,
- App-Navigation und Dateiauswahl.

## Abhängigkeiten

Das Modul darf keine Abhängigkeit auf `KnotenKartenVerwalter`, `MathematikKartenAdapter`, `MathematikKnoten` oder `app` einführen.

## Öffentliche Verträge

Änderungen an mathematischen Wert-, Mengen-, Methoden-, Relations- oder Strukturverträgen sind als fachliche API-Änderungen zu behandeln. Bestehende stabile IDs und persistierte Bedeutungen dürfen nicht beiläufig umgedeutet werden.

## Zentrale Einstiegspunkte

- `src/main/`: Produktionscode des Rechenkerns
- `src/test/`: fachliche Regressionstests
- `../docs/codex/ARCHITECTURE.md`

## Änderungen an diesem Pfad

Prüfe abhängig vom Vertrag mindestens `MathematikKartenAdapter`, `MathematikKnoten`, Definitionskarten, Persistenz/Migration und bestehende Architekturwächter.

## Tests

Mindestens die betroffenen Modultests sowie die in `../AGENTS.md` und `../docs/codex/TEST_STRATEGY.md` genannten Kernprüfungen.

## Relevante Skills

Mathematisch nicht triviale Änderungen benötigen fachliche Konzept- und Schlüssigkeitsprüfung. Die dafür vorgesehenen gemeinsamen Skills werden im Agentensystem zentral registriert.

## Unterpfade

- `src/main/`: mathematische Implementierung
- `src/test/`: mathematische Tests
