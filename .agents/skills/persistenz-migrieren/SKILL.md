---
name: persistenz-migrieren
description: Plant und prüft Migrationen an Karten-JSON, Knotenparametern, Anschluss-IDs, Gruppen und Bibliotheksdaten im Mathematik Atlas. Verwenden bei gespeicherten Datenformaten und Rückwärtskompatibilität.
---
# Persistenz und Migration

Lies `AGENTS.md`, `docs/codex/NODE_CONTRACT.md`, `docs/codex/TEST_STRATEGY.md` und reale JSON-Writer/Reader, Versionierung und Altkarten. Nicht jede Änderung benötigt eine Formatversion.

## Vorgehen
1. Alten und neuen Datenvertrag anhand realer Felder, Standardwerte, Typ-IDs und Anschlussreferenzen vergleichen.
2. Festlegen, ob Migration, kompatibler Default oder neue Formatversion erforderlich ist; bestehende IDs und Kanten erhalten.
3. Ladepfad, Serialisierung, Kopieren, Import/Export, Archiv, Undo/Redo und Gruppenkarten gemeinsam betrachten.
4. Unbekannte zukünftige Typen und beschädigte Daten defensiv behandeln. Keine stillen Löschungen.
5. Migration möglichst deterministisch, idempotent und testbar halten. Backup-/Rollbackpfad des Bestands berücksichtigen.

## Testmatrix
Altes JSON → neuer Reader; neuer Roundtrip; fehlende optionale Felder; dynamische Anschlüsse; duplizierte Karten; Referenzstabilität; ungültige Daten; alte Standardkarten. Vergleich vor/nach Migration dokumentieren. Nicht ausgeführte Gerätetests getrennt kennzeichnen.
