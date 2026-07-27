# Mathematik Atlas

Der Mathematik Atlas ist eine Android-Anwendung zur Darstellung mathematischer Vorgänge als gerichtete Knotenkarten. Das Repository ist als unabhängige, deutsch benannte Modularchitektur aufgebaut. Der allgemeine Karteneditor kennt keine Mathematik; das Rechensystem kennt weder Android noch Compose.

## Module

| Modul | Aufgabe |
|---|---|
| `KnotenKartenVerwalter` | Wiederverwendbarer Node-Editor für Jetpack Compose, neutrales Kartenmodell, Verbindungen, Auswahl, Zoom, Undo/Redo |
| `MathematikRechenSystem` | Reines Kotlin-CAS für exakte Zahlen, Terme, Aussagen, Mengen, Funktionen, Vektoren, Matrizen und Umformungen |
| `MathematikKartenAdapter` | Topologische, inkrementell gecachte Auswertung von Karten mit dem CAS |
| `MathematikKnoten` | Mathematische Knotenvorlagen, Auswerter, Anschlussarten und nativer LaTeX-Renderer |
| `app` | Material-3-App, Kartenbibliothek, Inspector, Breadcrumbs, Import/Export und versionierte Dateispeicherung |

## Voraussetzungen

- JDK 17 oder neuer
- Android SDK Platform 36
- Internetzugriff beim ersten Gradle-Aufruf, damit Gradle und Maven-Abhängigkeiten geladen werden können

Android Studio kann den Ordner unmittelbar als Gradle-Projekt öffnen. Alternativ:

```bash
./gradlew test :app:assembleDebug
```

Das Debug-APK liegt anschließend unter:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Der enthaltene Wrapper-Bootstrap lädt Gradle 8.13 ausschließlich von `services.gradle.org` und prüft die offizielle SHA-256-Prüfsumme, bevor er die Distribution startet.

## Bedienung

- Langes Drücken auf die Kartenfläche öffnet die Knotenauswahl.
- Ein Anschluss kann angeklickt oder auf die Kartenfläche gezogen werden.
- Beim Ablegen einer Verbindung auf freier Fläche werden nur kompatible Knoten angeboten.
- Knoten lassen sich verschieben, auswählen, duplizieren, löschen und über den Griff unten rechts skalieren.
- Verbindungen lassen sich auswählen und löschen.
- Gespeicherte Karten mit Karten-Ein- und -Ausgängen erscheinen als wiederverwendbare Gruppenknoten.
- Ein Doppelklick auf einen Gruppenknoten öffnet seine Karte; die Werkzeugleiste zeigt den Breadcrumb.
- Änderungen werden automatisch ausgewertet und verzögert gespeichert.

## Karten und Versionen

Jede Kartenversion wird einzeln gespeichert:

```text
MathematikAtlas/karten/<karten-id>/v<version>.json
```

Eine bestehende Version wird überschrieben, solange sie in keiner anderen Karte als Gruppenknoten verwendet wird. Sobald sie verwendet wird, erzeugt die nächste Änderung automatisch eine neue Version. Bestehende Gruppenknoten behalten ihren festen Versionsverweis.

## Prüfungen

```bash
python3 scripts/pruefe_architektur.py
python3 scripts/pruefe_kern.py   # benötigt lokales kotlinc
./gradlew test
```

Weitere Einzelheiten stehen in `docs/`.
