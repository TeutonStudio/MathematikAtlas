# Zum Mathematik Atlas beitragen

Beiträge sind willkommen, sofern sie den fachlichen, technischen und lizenzrechtlichen Rahmen des Projekts respektieren. Besonders hilfreich sind reproduzierbare Fehlermeldungen, mathematisch präzise Randfälle, Tests, Dokumentationsverbesserungen und klar abgegrenzte Implementierungen.

## Vor dem Start

1. Lies `README.md`, `LICENSE`, `CLA.md` und `docs/DEVELOPMENT.md`.
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
- Veröffentliche aus deinem Beitrags-Fork keine APKs, Releases, Pakete oder sonstigen ausführbaren Artefakte.
- Nutze den Fork ausschließlich für private Entwicklung und die Vorbereitung eines Beitrags.

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
- ausdrückliche Zustimmung zum `CLA.md`

Füge dafür folgende Erklärung in die Beschreibung des Pull Requests ein:

```text
Ich habe CLA.md gelesen und stimme den Bedingungen für diesen Beitrag zu.
```

Pull Requests ohne diese ausdrückliche Zustimmung werden nicht in das offizielle Projekt übernommen.

## Lizenz des Projekts

Der aktuelle Mathematik Atlas steht unter der Mathematik Atlas Source-Available License 1.0. Die Lizenz erlaubt das Einsehen des Quellcodes, private Änderungen, private nichtkommerzielle Builds und Beitrags-Forks. Sie erlaubt keine öffentliche Distribution und keine kommerzielle Nutzung ohne ausdrückliche schriftliche Genehmigung.

Frühere Fassungen bis einschließlich Commit `90a85368942db1f0b8d06f0ca458e9c6970daf62` bleiben unter der Apache License 2.0 nutzbar. Näheres steht in `LICENSE_HISTORY.md`.

## Rechte an Beiträgen

Das Urheberrecht an einem eigenen Beitrag verbleibt bei der beitragenden Person. Damit Alexander Würfl den Mathematik Atlas einheitlich pflegen, veröffentlichen und kommerziell verwerten kann, werden Beiträge nur unter den zusätzlichen Bedingungen des `CLA.md` angenommen.

Das Einreichen einer allgemeinen Idee, einer Fehlermeldung oder einer mathematischen Beobachtung über ein Issue ist für sich allein keine Einräumung ausschließlicher Nutzungsrechte. Für Quellcode, Dokumentation, Grafiken und andere schutzfähige Beiträge gilt dagegen das `CLA.md`.
