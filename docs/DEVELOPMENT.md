# Entwicklung und Architektur

Dieses Dokument ist der öffentliche technische Einstieg für Mitwirkende. Agentenspezifische Regeln und das versionierte Projektgedächtnis liegen getrennt unter `docs/codex/` und in `AGENTS.md`.

## Technischer Rahmen

- Kotlin und Gradle mit Kotlin-DSL
- Android und Jetpack Compose
- Material 3
- JVM-Toolchain 17
- Android SDK 36
- Android- und Compose-freier mathematischer Rechenkern
- nativer Compose-Renderer für den unterstützten LaTeX-Teilumfang

Das Repository ist keine Vite-, React-, React-Flow-, shadcn/ui- oder KaTeX-Anwendung.

## Module

| Modul | Verantwortung |
|---|---|
| `MathematikRechenSystem` | mathematische Domäne, exakte Objekte, Aussagen, Mengen, Methoden, Algebra, Geometrie und Umformungen |
| `KnotenKartenVerwalter` | neutraler Grapheditor, persistierbare Graphdaten, Verbindungsprüfung, Interaktion und Historie |
| `MathematikKartenAdapter` | topologische Auswertung, Auswerterregister, Fehleraggregation und Ergebnis-Cache |
| `MathematikKnoten` | mathematische Vorlagen, Anschlussarten, Auswerter und spezialisierte Compose-Renderer |
| `app` | Anwendung, Kartenbibliothek, Inspector, Navigation, Persistenz, Migrationen sowie Import und Export |

## Architekturprinzipien

1. Mathematische Semantik gehört in den Rechenkern oder in dafür vorgesehene Auswerter, nicht ausschließlich in Composables.
2. Der neutrale Karteneditor kennt keine mathematischen Knotenschlüssel oder Parameterkonventionen.
3. Knoten visualisieren und konfigurieren fachliche Modelle; sie sind nicht alleinige Quelle mathematischer Wahrheit.
4. Anschlüsse besitzen stabile IDs, Richtung, Typ, Kardinalität und Reihenfolge.
5. Laufzeitobjekte wie Compose-`State`, Funktionen oder Renderer-Caches werden nicht persistiert.
6. Darstellung leitet sich aus Daten und Auswertungsergebnissen ab.
7. Karten, Knoten, Anschlüsse, Verbindungen und visuelle Gruppen verwenden stabile IDs.
8. Änderungen berücksichtigen Laden, Speichern, Kopieren, Undo/Redo und Migrationen, soweit diese Pfade betroffen sind.

## Lokaler Start

```bash
./gradlew :app:assembleDebug
```

Die Debug-APK wird unter `app/build/outputs/apk/debug/app-debug.apk` erzeugt. Zum tatsächlichen Start ist ein Emulator oder Android-Gerät erforderlich.

## Prüfungen

```bash
# Struktur- und Architekturprüfung
python3 scripts/pruefe_repository.py

# Releaseplan
python3 scripts/pruefe_releaseplan.py

# Versionsfolge; benötigt einen Git-Checkout
python3 scripts/pruefe_versionsfolge.py

# Zusätzliche Kernprüfung; benötigt kotlinc
python3 scripts/pruefe_kern.py

# JVM-Tests
./gradlew test

# Android-Debug-Build
./gradlew :app:assembleDebug
```

Ein nicht ausführbarer Befehl wird mit konkretem Grund dokumentiert. Ein erfolgreicher Build ersetzt keine Bedienprüfung auf Emulator oder Gerät.

## Karten und Persistenz

Karten werden im App-internen Dateienbereich gespeichert:

```text
MathematikAtlas/karten/<karten-id>/v<version>.json
```

Der aktuelle Schreibpfad verwendet `formatVersion` 5. Ältere Karten werden über bestehende Lese- und Migrationspfade normalisiert. Änderungen am Format benötigen Tests für Laden, Speichern, Rückwärtskompatibilität und stabile IDs.

## Neue Knoten

Ein neuer Knotentyp benötigt mindestens:

- eindeutigen stabilen Typ-Schlüssel
- fachliche Beschreibung und mathematische Semantik
- definierte Ein- und Ausgänge samt Anschlussarten
- Vorlagenregistrierung und Auswerterregistrierung
- Darstellung und Inspector-Verhalten
- Persistenz- und gegebenenfalls Migrationsbetrachtung
- Tests für Normalfall, Randfälle und Fehlerzustände
- passende Versionsklassifikation nach `docs/VERSIONING.md`

Der vollständige interne Ablauf steht in `docs/codex/NEW_NODE_WORKFLOW.md` und `.agents/skills/neuer-knoten/SKILL.md`.

## Dokumentationsrollen

- `README.md`: verständlicher Produkteinstieg
- `ROADMAP.md`: langfristige Vision
- `CONTRIBUTING.md`: öffentlicher Beitragsablauf
- `docs/DEVELOPMENT.md`: technischer Einstieg
- `docs/VERSIONING.md`: öffentliches Versionsschema
- `docs/codex/`: Agenten-Runbook, Architekturdetails, Pläne und verifizierter Projektzustand

Wenn Dokumentation und Code einander widersprechen, gilt der Code als aktueller Istzustand. Die widersprechende Dokumentation soll im selben Änderungsvorhaben korrigiert werden.
