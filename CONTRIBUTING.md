# Zum Mathematik Atlas beitragen

Beiträge sind willkommen, sofern sie den fachlichen und technischen Rahmen des Projekts respektieren. Besonders hilfreich sind reproduzierbare Fehlermeldungen, mathematisch präzise Randfälle, Tests, Dokumentationsverbesserungen und klar abgegrenzte Implementierungen.

## Vor dem Start

1. Lies `README.md` und `docs/DEVELOPMENT.md`.
2. Suche nach bestehenden Issues und Pull Requests zum selben Thema.
3. Verwende für neue Vorschläge die passende Issue-Vorlage.
4. Besprich größere Architektur-, Persistenz- oder Knotenkonzepte vor der Implementierung.
5. Lies bei einem neuen Knotentyp zusätzlich `docs/codex/NODE_CONTRACT.md` und `docs/codex/NEW_NODE_WORKFLOW.md`.

## Fehler melden

Eine gute Fehlermeldung enthält:

- betroffene Version oder Commit
- konkrete Schritte zur Reproduktion
- erwartetes Verhalten
- tatsächliches Verhalten
- betroffene Karte und Knotentypen
- relevante Fehlermeldungen
- Screenshots oder minimales Karten-JSON, sofern ohne vertrauliche Daten möglich

Sicherheitsprobleme gehören **nicht** in ein öffentliches Issue. Verwende dafür `SECURITY.md`.

## Änderungen entwickeln

- Arbeite nicht direkt auf `master`.
- Halte den Umfang eines Branches und Pull Requests klar begrenzt.
- Vermeide beiläufige Refactorings außerhalb des eigentlichen Problems.
- Bewahre öffentliche APIs, sofern die Aufgabe keine bewusste Änderung verlangt.
- Ergänze Tests oder begründe im Pull Request, warum kein sinnvoller Test möglich ist.
- Aktualisiere betroffene Dokumentation gemeinsam mit dem Code.
- Berücksichtige Persistenz und Migrationen, wenn gespeicherte Daten betroffen sind.

## Branches und Versionen

Das verbindliche Schema steht in `docs/VERSIONING.md` und `docs/codex/RELEASE_WORKFLOW.md`. Jeder Pull Request gegen `master` gehört zu genau einer reservierten Version. Dokumentation und Fehlerkorrekturen sind gewöhnlich `x`-Änderungen; neue, separat erzeugbare Knotentypen lösen eine `y`-Version aus.

## Prüfungen

Führe die für deine Änderung relevanten Prüfungen aus:

```bash
python3 scripts/pruefe_repository.py
python3 scripts/pruefe_releaseplan.py
python3 scripts/pruefe_versionsfolge.py
python3 scripts/pruefe_kern.py
./gradlew test
./gradlew :app:assembleDebug
```

Dokumentiere auch nicht ausführbare Prüfungen mit ihrem konkreten Grund. Behaupte keinen Emulator- oder Gerätetest, wenn nur JVM-Tests oder ein Build ausgeführt wurden.

## Pull Requests

Ein Pull Request soll enthalten:

- Problem und Ziel
- umgesetztes Verhalten
- zentrale geänderte Dateien oder Module
- mathematische und technische Randfälle
- Persistenz- oder Migrationsauswirkungen
- ausgeführte Prüfungen mit Ergebnis
- verbleibende Risiken
- zugehörige Issue-Nummern

## Lizenz der Beiträge

Das Projekt steht unter der Apache License 2.0. Mit dem Einreichen eines Beitrags bestätigst du, dass du die notwendigen Rechte daran besitzt und ihn unter denselben Lizenzbedingungen zur Verfügung stellst. Das Urheberrecht an deinem eigenen Beitrag bleibt bei dir; du räumst die für die Projektlizenz erforderlichen Nutzungsrechte ein.
