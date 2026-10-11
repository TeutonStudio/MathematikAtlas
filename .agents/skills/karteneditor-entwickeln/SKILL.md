---
name: karteneditor-entwickeln
description: Entwickelt und prüft Karteninteraktionen, Graphzustand, Knotenverschiebung, Zoom, Gruppierung, Anschlüsse und Undo/Redo im Mathematik Atlas. Verwenden bei Editor- und Gestenänderungen.
---
# Karteneditor entwickeln

Lies `AGENTS.md`, `docs/codex/ARCHITECTURE.md`, `docs/codex/TEST_STRATEGY.md` und die betroffenen tatsächlichen Graph-/Compose-Pfade. `KnotenKartenVerwalter` bleibt mathematisch fachneutral.

## Vorgehen
1. Nutzerablauf präzise reproduzieren: Eingabegerät (Maus, Touch, Stift), Interaktionsmodus, Zoom, Verschiebung, Auswahl und Gruppenzustand.
2. Bildschirm-, Canvas- und Kartenkoordinaten getrennt untersuchen. Zoom-Invarianten und Treffertoleranz bei mehreren Skalierungen prüfen.
3. Zustandsänderung als Kartenaktion spezifizieren; keine dauerhaften Mutationen allein im Pointer-Handler oder Composable.
4. Anschlussreferenzen, Kanten, Gruppenmitgliedschaft, Selektion, Kopieren/Löschen, atomare Undo-/Redo-Einträge und Persistenz einbeziehen.
5. Bei großer Knotenzahl nur betroffene Subgraphen aktualisieren; Performanceänderungen mit `performance-optimieren` messen.
6. Einheitliche Zustandsübergänge für Drop-in/Drop-out einer Gruppe definieren; weder doppelte Mitgliedschaft noch verlorene Links akzeptieren.

## Prüfen
Graph-Unit-Tests für Aktionen und Invarianten; Compose-/Laufzeittests für tatsächliche Gesten, wenn verfügbar. Mindestens kleine und große Karte, unterschiedliche Zoomstufen, Abbruch der Geste, Save/Reload, Undo/Redo. JVM-Erfolg nicht als bestätigte Gerätegeste ausgeben.

## Abgrenzung
Mathematische Regeln nach `MathematikRechenSystem` bzw. `MathematikKnoten`; fachneutrale Graphlogik nur im Kartenverwalter. Kein unverbundenes Schattenmodell des Kartenstatus.
