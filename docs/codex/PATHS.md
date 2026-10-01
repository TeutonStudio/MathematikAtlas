# Pfadverantwortungen

Diese Datei ist der Index für lokale Verantwortungs-READMEs. Die READMEs erklären den jeweiligen Pfad; diese Datei kopiert ihre Inhalte nicht.

## Grundsatz

Für architektonisch relevante Hauptpfade soll eine `README.md` mindestens Zweck, Zuständigkeit, Nicht-Zuständigkeit, Abhängigkeiten, öffentliche Verträge, Einstiegspunkte, Folgeprüfungen und Tests beschreiben.

Pfad-READMEs sind Orientierung. Bei Widerspruch gilt der nachweisbare Code- und Buildzustand; die Dokumentation ist anschließend zu korrigieren.

## Architekturpfade

- `TypSystem/`: fachneutrale Typ- und Signaturgrundlagen
- `MathematikRechenSystem/`: mathematische Domäne und Semantik
- `KnotenKartenVerwalter/`: fachneutraler Karten- und Grapheditor
- `MathematikKartenAdapter/`: Brücke zwischen Graph und Rechenkern
- `MathematikKnoten/`: mathematische Knotentypen, Anschlüsse, Auswerter und Renderer
- `app/`: Android-App, Inspector, Persistenz und Import/Export

## Agenten- und Betriebsstruktur

- `.agents/`: gemeinsame Skills und Workflowbausteine
- `.codex/`: Agentenrollen und Codex-Konfiguration
- `docs/codex/`: langlebiger technischer Kontext, Pläne und Entscheidungen
- `release/`: maschinenlesbarer Releasezustand
- `scripts/`: Repository-, Release- und Strukturprüfungen
- `.github/`: GitHub-Automation und CI

Lokale Verantwortungs-READMEs für `.agents/` und `.codex/` sind weiterhin ein eigener Strukturbaustein und werden getrennt von einzelnen neuen Agenten oder Skills gepflegt.
