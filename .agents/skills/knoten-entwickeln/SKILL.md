---
name: knoten-entwickeln
description: Entwickelt neue oder bestehende Mathematik-Atlas-Knoten und Operatoren, einschließlich Anschlüssen, Auswertung, Inspector und Rückwärtskompatibilität. Verwenden bei Knotentypen, Operatoren, Typkonvertierungen und Änderungen am Knotenvertrag.
---
# Knoten entwickeln

Lies `AGENTS.md` und den tatsächlichen betroffenen Code. Nutze `docs/codex/NODE_CONTRACT.md`, `docs/codex/NEW_NODE_WORKFLOW.md` und `docs/codex/TEST_STRATEGY.md` nur in den zutreffenden Abschnitten. Dieser Skill ergänzt den Bestand; er erzeugt keine zweite Registry.

## Aufgabe bestimmen
1. **Neuer separat erzeugbarer registrierter Typ:** vollständiger `neuer-knoten`-Workflow, ExecPlan, `math_reviewer` bei nichttrivialer Semantik und `node_verifier`; Versionswirkung `y`.
2. **Bestehender Knotentyp oder Operator:** Typ-Schlüssel und Registrierungsweg unverändert? Dann `x`; Vertrag, Inspector, Auswertung und Migration gezielt prüfen. ExecPlan bei Vertrags-/Schemaveränderung.
3. **Lokaler Fehler:** Reproduktion, kleinster kohärenter Fix, Regressionstest und gezielte Verifikation. Kein voller Plan bei reiner lokaler Korrektur.
4. **Refactoring:** Äquivalenz des beobachtbaren Verhaltens, Persistenz und IDs durch Tests nachweisen.

Die Versionsklassifikation ist ein Vorschlag; Reservierung, Branch und Integration bleiben bei `master_verwalter` und `release-verwalten`. Aktive Releases nicht eigenmächtig umnummerieren.

## Vor jeder Implementierung
- Bestehenden ähnlichen Knoten und tatsächlich genutzte Erzeugungs-, Registry-, Auswertungs- und Inspectorpfade finden.
- Trennen: Knotentyp, Operatorvariante, mathematisches Objekt, persistierte Instanz und dynamischer Anschluss.
- Eingangs-/Ausgangsart, Kardinalität, Reihenfolge, ID-Stabilität, Fehlerfälle, Typregeln und dynamische Anschlussänderungen dokumentieren.
- Prüfen, welche Verbraucher in `MathematikRechenSystem`, `KnotenKartenVerwalter`, `MathematikKartenAdapter`, `MathematikKnoten` und `app` betroffen sind.
- Datenfluss Inspector → Kartenaktion → Persistenz/Undo → Auswertung → Renderer/LaTeX nachvollziehen.
- Bei mathematischer Semantik `mathematik-validieren`, bei JSON-/ID-Änderungen `persistenz-migrieren`, bei Gesten `karteneditor-entwickeln` ergänzen.

## Umsetzung und Prüfgates
- Fachliche Semantik im Compose-freien Rechenkern bzw. vorgesehenen Auswerter belassen.
- Bestehende Vorlagen-, Auswerter-, Typ- und Rendererregister erweitern; keine parallele Infrastruktur.
- Tests für Normalfall, Gegenbeispiel, fehlende und falsche Eingaben; Registrierung und Anschlussvertrag prüfen.
- Bei vorhandenen Daten: JSON-Roundtrip, Altkarten, Kopie, Undo/Redo und Verbindungsreferenzen prüfen.
- Zuerst gezielte Modultests; anschließend relevante Repository-/Gradleprüfungen aus dem aktuellen Build ableiten. Ein Build ersetzt keine Emulatorprüfung.
- Nach Umsetzung: unabhängige Prüfung entsprechend Risiko; neue Typ-Schlüssel gegen Versionsklassifikation abgleichen.

## Abschluss
Änderungsvertrag, konkrete Symbole, neue oder unveränderte Typ-Schlüssel, Tests mit Ergebnissen, offene Laufzeitprüfungen, Migration und Versionswirkung berichten. Keine falsche Aussage über SamAI-Commitidentität bei GitHub-Connector-Commits.
