# Mathematik Atlas

Mathematik Atlas ist eine native Android-Anwendung, die mathematische Vorgänge als interaktive, gerichtete Knotenkarten darstellt. Mathematische Objekte, Operationen und Umformungen werden als Knoten verbunden, topologisch ausgewertet und als versionierte Karten gespeichert. Gespeicherte Karten können wiederum als wiederverwendbare Gruppenknoten eingesetzt werden.

Die Anwendung ist auf Deutsch benannt und implementiert; sie basiert auf Kotlin, Jetpack Compose und Gradle – nicht auf einer Web- oder React-Architektur.

## Funktionen

- mathematische Knoten für Zahlen, Terme, Aussagen, Mengen, Abbildungen, Operatoren, Vektoren und Matrizen
- typisierte Anschlüsse mit Prüfung von Richtung, Kompatibilität, Eingangskardinalität und Zyklen
- inkrementelle, topologische Auswertung mit fachlichen Fehlerzuständen
- nativer Compose-Editor mit Auswahl, Verschieben, Skalieren, Zoom, Verbindungen sowie Undo/Redo
- Inspector zur Bearbeitung persistierter Knotenparameter und Anschlüsse
- LaTeX-Ausgabe aus dem mathematischen Modell mit nativem Compose-Renderer
- JSON-Persistenz mit versionierten Karten, Import/Export und Gruppenknoten

## Roadmap

![Roadmap des Mathematik Atlas von v2.y.x bis v8.y.x](docs/assets/roadmap.svg)

### Versionsschema

Die Roadmap verwendet das Schema `vM.y.x`:

- **`M` – Versionsraum:** ein größerer fachlicher oder technischer Entwicklungsabschnitt
- **`y` – Knoten-Version:** ergänzt neue Knoten oder vollständige Knotenfamilien innerhalb des Versionsraums
- **`x` – Änderungs-Version:** enthält sonstige Änderungen, Fehlerkorrekturen, Bedienungsverbesserungen und Ausbau bestehender Konzepte

Eine neue `y`-Version erweitert damit ausdrücklich das verfügbare Knotenvokabular. Eine neue `x`-Version verändert oder verbessert vorhandene Funktionen, ohne diesen Anspruch zu erheben.

### Versionsräume

- **v2.y.x – Mathematischer Kern**
  - **v2.0.x:** funktionsfähige Grundlage
  - **v2.1.x:** Ergänzung von Visualisierungskonzepten
  - **v2.2.x:** Hilbert-basierte Geometrie
  - **v2.3.x:** Definitionskarten für vordefinierte Knoten
- **v3.y.x – Grafik, Auszeichnung und Dokumente:** SVG, TikZ, LaTeX, Mermaid und HTML
- **v4.y.x – Animation:** Manim und geeignete Alternativen
- **v5.y.x – Web-Programmierung:** JavaScript und TypeScript
- **v6.y.x – JVM-Programmierung:** Kotlin und Java
- **v7.y.x – Godot-Grundintegration:** Godot-Szenen, GDScript und ein Orchestrator als Vorlage
- **v8.y.x – Godot-Erweiterungen:** NobodyWho, Voxel Tools und LimboAI

### Noch keinem Versionsraum zugeordnet

C#, C++, C, Rust, Python und weitere Sprachen sind geplant. Ihre Versionsräume werden erst festgelegt, wenn Umfang, gemeinsame Sprachkonzepte und Abhängigkeiten ausreichend geklärt sind.

## Voraussetzungen

- JDK 17 oder neuer
- Android SDK Platform 36
- Internetzugriff beim ersten Gradle-Aufruf, damit der Wrapper und Maven-Abhängigkeiten geladen werden können

Das Projekt verwendet den enthaltenen Gradle Wrapper 8.13. Android Studio kann den Repository-Ordner direkt als Gradle-Projekt öffnen.

## Schnellstart

Öffne das Projekt in Android Studio und starte das Modul `app` auf einem Emulator oder Gerät. Für einen Debug-Build in der Konsole:

```bash
./gradlew :app:assembleDebug
```

Das erzeugte APK liegt anschließend unter:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Bedienung

- Langes Drücken auf die Kartenfläche öffnet die Knotenauswahl.
- Anschlüsse lassen sich anklicken oder auf die Kartenfläche ziehen.
- Beim Ablegen auf freier Fläche werden nur kompatible Knoten angeboten.
- Knoten können verschoben, ausgewählt, dupliziert, gelöscht und über den Griff unten rechts skaliert werden.
- Verbindungen können ausgewählt und gelöscht werden.
- Ein Doppelklick auf einen Gruppenknoten öffnet dessen Karte; die Werkzeugleiste zeigt den Navigationspfad.
- Änderungen werden automatisch ausgewertet und verzögert gespeichert.

## Projektstruktur

| Modul | Verantwortung |
|---|---|
| `MathematikRechenSystem` | Reines Kotlin-CAS für exakte Zahlen, Terme, Aussagen, Mengen, Funktionen, Vektoren, Matrizen und Umformungen |
| `KnotenKartenVerwalter` | Neutraler Compose-Node-Editor, Graphmodell, Verbindungen, Auswahl, Zoom und Undo/Redo |
| `MathematikKartenAdapter` | Topologische Auswertung der Karten mit dem Rechenkern und Ergebnis-Cache |
| `MathematikKnoten` | Mathematische Knotenvorlagen, Auswerter, Anschlussarten und LaTeX-Renderer |
| `app` | Material-3-Anwendung, Kartenbibliothek, Inspector, Navigation sowie Import/Export und Dateispeicherung |

Die Abhängigkeiten verlaufen von der Darstellung über Graph- und Anwendungslogik zur mathematischen Domäne. Der Rechenkern bleibt unabhängig von Android und Compose.

## Karten und Persistenz

Karten werden im App-internen Dateienbereich gespeichert:

```text
MathematikAtlas/karten/<karten-id>/v<version>.json
```

Das JSON-Format wird aktuell als Formatversion 2 geschrieben und kann Karten der Formatversion 1 mit fehlenden Knoteneigenschaften rückwärtskompatibel lesen. Eine Kartenversion wird überschrieben, solange keine andere Karte sie als Gruppenknoten referenziert. Sobald sie verwendet wird, erzeugt die nächste Änderung automatisch eine neue Version; bestehende Gruppenknoten behalten ihren festen Versionsverweis.

## Prüfungen

```bash
# Repository- und Architekturprüfung
python3 scripts/pruefe_repository.py

# Zusätzliche Kernprüfung; benötigt ein lokal verfügbares kotlinc
python3 scripts/pruefe_kern.py

# JVM-Unit-Tests aller Module
./gradlew test

# Debug-Build der Anwendung
./gradlew :app:assembleDebug
```

Die zuletzt verifizierten Ergebnisse und bekannte Einschränkungen stehen in [docs/codex/CURRENT_STATE.md](docs/codex/CURRENT_STATE.md). Insbesondere wurde die App noch nicht auf Emulator oder Gerät verifiziert.

## Weiterführende Dokumentation

- [Projektkontext](docs/codex/PROJECT_CONTEXT.md): Begriffe, Technologie und zentrale Quellpfade
- [Architektur](docs/codex/ARCHITECTURE.md): Verantwortungsgrenzen und Abhängigkeitsrichtung
- [Node-Vertrag](docs/codex/NODE_CONTRACT.md): Anforderungen an Knotentypen und Anschlüsse
- [Teststrategie](docs/codex/TEST_STRATEGY.md): Testebenen und Prüfanforderungen
- [Dokumentationsübersicht](docs/codex/README.md): Pläne, Architekturentscheidungen und Arbeitsablauf

## Mitwirken

Vor Änderungen bitte die Repository-Anweisungen in [AGENTS.md](AGENTS.md) und die einschlägigen Dokumente unter `docs/codex/` lesen. Neue mathematische Knotentypen folgen dem dokumentierten Workflow mit Planung, fachlicher Prüfung, Implementierung und unabhängiger Verifikation.
