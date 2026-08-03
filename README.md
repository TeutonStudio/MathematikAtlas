# Mathematik Atlas

> **Eine knotenorientierte Mathematiklandschaft**

Mathematik Atlas ist eine experimentelle native Android-Anwendung, in der mathematische Objekte, Operatoren und Umformungen als verbundene Knotenkarten dargestellt werden. Die sichtbare Graphstruktur macht Abhängigkeiten und Operatorreihenfolgen nachvollziehbar, während ein modularer Rechenkern die Karten topologisch auswertet und wiederverwendbare Zwischenergebnisse zwischenspeichert.

> [!IMPORTANT]
> Das Projekt befindet sich in aktiver Entwicklung. Es gibt derzeit keine professionell veröffentlichte Endnutzer-Version und keinen offiziellen APK-Download. Datenformate, Bedienkonzepte und mathematische Knoten können sich noch verändern.

## Worum geht es?

Mathematische Ausdrücke werden in gewöhnlicher Schreibweise häufig stark verschachtelt. Der Mathematik Atlas zerlegt sie stattdessen in sichtbare Bestandteile:

- **Knoten** repräsentieren mathematische Objekte, Operatoren oder Verarbeitungsschritte.
- **Anschlüsse** legen fest, welche Werte ein Knoten benötigt oder bereitstellt.
- **Verbindungen** transportieren mathematische Objekte zwischen kompatiblen Anschlüssen.
- **Karten** fassen einen vollständigen mathematischen Vorgang zusammen.
- **Definitionskarten** zeigen, wie ein Objekt oder eine Operation aufgebaut ist beziehungsweise funktioniert.

Langfristig soll daraus zugleich eine visuelle Mathematikumgebung, ein knotenorientiertes Computeralgebrasystem, ein Werkzeug zum Lernen und Erkunden sowie eine Plattform für formale Prozesse entstehen. Die [Roadmap](ROADMAP.md) beschreibt diese Vision getrennt vom aktuellen Funktionsstand.

## Grundkonzepte

| Begriff | Bedeutung |
|---|---|
| **Knoten** | Persistierbares mathematisches Objekt, Operator oder Verarbeitungsschritt innerhalb einer Karte |
| **Anschluss** | Typisierter Ein-, Aus- oder Neutralanschluss eines Knotens |
| **Verbindung** | Gerichtete Beziehung zwischen zwei kompatiblen Anschlüssen |
| **Karte** | Versionierter Graph aus Knoten, Verbindungen und Ansichtsdaten |
| **Inspector** | Eigenschaftenbereich zur Konfiguration des ausgewählten Knotens |
| **Auswertung** | Topologische Verarbeitung der Karte durch den mathematischen Rechenkern |
| **Cache** | Wiederverwendung bereits berechneter Knotenergebnisse, solange ihre Voraussetzungen unverändert bleiben |
| **Gruppenknoten** | Wiederverwendbare Referenz auf eine feste Version einer anderen Karte |

## Was macht den Atlas besonders?

### Definition und Funktionsweise

Mathematische Objekte sollen nicht nur als fertige Symbole erscheinen. Definitions- und Konzeptkarten machen ihren inneren Aufbau untersuchbar und können als bearbeitbare Kopien geöffnet werden. Der Bestand wächst schrittweise; die langfristige Vision ist eine passende Erklärung für jedes unterstützte Objekt.

### Extreme Modularität

Knoten lassen sich zu Karten zusammensetzen. Karten können wiederum als versionierte Gruppenknoten oder Methoden in anderen Karten verwendet werden. Komplexe Vorgänge entstehen dadurch aus kleineren, überprüfbaren Bausteinen.

### Sichtbare Operatorreihenfolge

Die Graphstruktur zeigt unmittelbar, welches Objekt in welchen Operator eingeht und in welcher Reihenfolge Ergebnisse weiterverwendet werden. Abhängigkeiten müssen nicht allein aus Klammern und impliziten Schreibkonventionen rekonstruiert werden.

### Inkrementelle Auswertung

Der Kartenadapter wertet den azyklischen Graph topologisch aus und hält Knotenergebnisse in einem Cache. Änderungen sollen dadurch nur die tatsächlich betroffenen Teile einer Karte neu berechnen.

## Aktueller Funktionsumfang

- mathematische Knoten für Zahlen, Terme, Aussagen, Mengen, Methoden, Vektoren, Matrizen, Geometrie und iterative Operatoren
- typisierte Anschlüsse mit Richtungs-, Kardinalitäts-, Kompatibilitäts- und Zyklusprüfung
- topologische Auswertung mit fachlichen Ergebnis-, Fehler- und Entscheidungszuständen
- nativer Jetpack-Compose-Editor mit Auswahl, Verschieben, Skalieren, Zoom, Verbindungen sowie Undo und Redo
- Inspector für persistierte Knotenparameter, Anschlusskonfigurationen und weitere Eigenschaften
- mathematischer Kotlin-Rechenkern mit modellbasierter LaTeX-Ausgabe
- versionierte Karten, Import und Export sowie Wiederverwendung als Gruppenknoten oder Kartenmethode
- Konzeptkatalog mit Definitionskarten und bearbeitbaren Kopien

## Eingebaute Beispielkarten

Wenn der interne Kartenspeicher leer ist, legt die App automatisch fünf Beispielkarten an:

| Karte | Gezeigtes Konzept |
|---|---|
| **Doppeln** | öffentlicher Karteneingang, Addition `x + x` und öffentlicher Kartenausgang |
| **Rechnen** | Addition von `2` und `3`, Auswertung und anschließende Verwendung der Karte „Doppeln“ als Gruppenknoten |
| **Aussage** | symbolische Variable, Gleichheit und Auswertung einer Aussage |
| **Mengen** | Vereinigung zweier endlicher Mengen |
| **Zahl und Menge verbinden** | typisierte Verbindung allgemeiner mathematischer Objekte mit einer Gleichheitsrelation |

Eine genauere Einordnung steht unter [Beispielkarten und erste Erkundung](docs/TESTKARTEN.md).

## Bedienung in Kürze

1. Öffne eine vorhandene Karte oder erstelle eine neue.
2. Füge über die Knotenauswahl mathematische Objekte und Operatoren ein.
3. Verbinde kompatible Anschlüsse.
4. Konfiguriere den ausgewählten Knoten im Inspector.
5. Betrachte Auswertung, Darstellung und fachliche Fehler direkt an der Karte.
6. Speichere die Karte und verwende sie bei Bedarf in anderen Karten weiter.

Weitere Bedien- und Entwicklungsdetails stehen in [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md).

## Architektur

Das Repository ist ein Android-Gradle-Multimodulprojekt:

| Modul | Verantwortung |
|---|---|
| `MathematikRechenSystem` | Android- und Compose-freier Kotlin-Rechenkern für mathematische Objekte und Umformungen |
| `KnotenKartenVerwalter` | Fachneutraler Compose-Karteneditor, Graphmodell, Interaktion und Undo/Redo |
| `MathematikKartenAdapter` | Topologische Kartenauswertung, Ergebniszustände und Cache |
| `MathematikKnoten` | Mathematische Knotenvorlagen, Anschlussarten, Auswerter und spezialisierte Renderer |
| `app` | Android-Anwendung, Kartenbibliothek, Inspector, Navigation, Persistenz sowie Import und Export |

Die mathematische Semantik bleibt vom Editor und von der Android-Oberfläche getrennt. Technische Details und verbindliche Modulgrenzen sind in der [Entwicklungsdokumentation](docs/DEVELOPMENT.md) zusammengefasst.

## Voraussetzungen

- JDK 17 oder neuer
- Android SDK Platform 36
- Android Studio mit Unterstützung für das enthaltene Gradle-Projekt
- Internetzugriff beim ersten Gradle-Aufruf, damit Wrapper und Maven-Abhängigkeiten geladen werden können

## Schnellstart

Öffne den Repository-Ordner in Android Studio und starte das Modul `app` auf einem Emulator oder Android-Gerät.

Ein Debug-Build lässt sich auch in der Konsole erzeugen:

```bash
./gradlew :app:assembleDebug
```

Die APK liegt anschließend unter:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Die wichtigsten Prüfungen sind:

```bash
python3 scripts/pruefe_repository.py
python3 scripts/pruefe_releaseplan.py
python3 scripts/pruefe_versionsfolge.py
python3 scripts/pruefe_kern.py
./gradlew test
./gradlew :app:assembleDebug
```

Nicht jeder Befehl ist in jeder Umgebung ausführbar. Insbesondere benötigt `scripts/pruefe_kern.py` ein lokal verfügbares `kotlinc`. Ein erfolgreicher JVM-Test oder APK-Build ersetzt keinen Laufzeittest auf Emulator oder Gerät.

## Roadmap

![Roadmap des Mathematik Atlas von v2.y.x bis v8.y.x](docs/assets/roadmap.svg)

Die Grafik zeigt langfristige Entwicklungsräume. Sie ist keine Zusage für Termine oder bereits implementierte Funktionen. Die vollständige textuelle Fassung steht in [ROADMAP.md](ROADMAP.md); das projektspezifische Versionsschema wird unter [docs/VERSIONING.md](docs/VERSIONING.md) erklärt.

## Dokumentation

- [Roadmap](ROADMAP.md)
- [Beispielkarten](docs/TESTKARTEN.md)
- [Entwicklung und Architektur](docs/DEVELOPMENT.md)
- [Versionsschema](docs/VERSIONING.md)
- [Beitragen](CONTRIBUTING.md)
- [Contributor License Agreement](CLA.md)
- [Lizenzhistorie](LICENSE_HISTORY.md)
- [Sicherheitsrichtlinie](SECURITY.md)
- [Verhaltenskodex](CODE_OF_CONDUCT.md)
- [Danksagungen](ACKNOWLEDGEMENTS.md)
- [Agenten- und Projektgedächtnis](docs/codex/README.md)

## Mitwirken

Fehlermeldungen, mathematische Hinweise, Dokumentationsverbesserungen und gut abgegrenzte Beiträge sind willkommen. Vor größeren Änderungen bitte [CONTRIBUTING.md](CONTRIBUTING.md) und die für den Bereich relevante technische Dokumentation lesen.

Die Issue-Vorlagen unterscheiden zwischen Fehlern und Funktionsvorschlägen. Neue mathematische Semantik benötigt eine fachliche Beschreibung, nachvollziehbare Randfälle und geeignete Tests. Direkte Produktionscommits auf `master` sind nicht vorgesehen. Codebeiträge werden nur angenommen, wenn die beitragende Person dem [Contributor License Agreement](CLA.md) ausdrücklich zustimmt.

## Lizenz

Copyright © 2026 Alexander Würfl (TeutonStudio).

Der Mathematik Atlas wird unter der [Mathematik Atlas Source-Available License 1.0](LICENSE) bereitgestellt. Dies ist **keine Open-Source-Lizenz**. Erlaubt sind insbesondere das Einsehen des Quellcodes, Beitrags-Forks, private Änderungen sowie das Erstellen und private nichtkommerzielle Nutzen einer eigenen APK. Öffentliche Distribution und jede direkte oder indirekte kommerzielle Nutzung bedürfen einer ausdrücklichen schriftlichen Genehmigung des Lizenzgebers.

Frühere Repository-Fassungen bis einschließlich Commit `90a85368942db1f0b8d06f0ca458e9c6970daf62` bleiben unter der Apache License 2.0 nutzbar. Einzelheiten und Fremdlizenzen sind in der [Lizenzhistorie](LICENSE_HISTORY.md) beschrieben.
