---
name: performance-optimieren
description: Untersucht und optimiert Lags, Recomposition, Renderkosten, Graphauswertung, Caches und Speicherlast im Mathematik Atlas. Verwenden bei Ruckeln, langsamen Karten oder Visualisierungen.
---
# Performance messen und optimieren

Lies `AGENTS.md` und die aktuellen betroffenen Codepfade. Erst messen, dann ändern.

## Reproduktion
Definiere Baseline: Gerät/Emulator/Desktop, Release-/Debugmodus, Kartengröße und Kantenzahl, Zoom, Visualisierungstyp, Eingabeaktion, Frametimes/Latenz und Speicher. Vermeide Debug-vs.-Release-Vergleiche ohne Kennzeichnung.

## Ursachen trennen
- Compose-Recomposition und Canvas-Draw, Text-/LaTeX-Layout, Hit-Testing und Pointerereignisse.
- Graphmutationen, topologische Reihenfolge, Teilgraphinvalidierung, Cachekeys und Cachelebensdauer.
- Mathematischer Sampler, numerische Budgets, Coroutine-Abbruch und UI-Thread-Blockaden.
- JSON, Datei-I/O, Im-/Export und wiederholte komplette Kartenberechnung.

## Optimieren
Eine messbare Hypothese pro Änderung. Korrektheit und deterministische fachliche Ergebnisse vor Geschwindigkeit. Keine Stale-Caches, keine gelöschten Fehlerzustände und keine versteckten Semantikänderungen. Große und kleine Karten sowie geänderte/ungeänderte Teilgraphen vergleichen. Bei Messumgebungslimitierung Ergebnisse ausdrücklich als unbestätigt markieren.

## Abschluss
Vorher/Nachher mit gleicher Umgebung, geänderter Pfad, Profilingbelege, Regressionstests, bekannte Trade-offs. Keine Prozentverbesserungen ohne Messwerte.
