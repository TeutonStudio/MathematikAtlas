# Prüfbericht

## In dieser Erstellungsumgebung ausgeführt

- Kompilation des neutralen Graphmodells, der Graphprüfung, des vollständigen CAS-Kerns, des Kartenadapters und der nichtvisuellen Mathematikknoten mit `kotlinc`.
- Ausführung der eigenständigen Kernprüfung in `werkzeuge/Prüfung.kt`.
- Architekturprüfung mit `scripts/pruefe_architektur.py`.
- Prüfung aller Kotlin-Dateien auf Parserfehler; Android- und Compose-Symbole konnten ohne Android-SDK naturgemäß nicht aufgelöst werden.
- Prüfung des Wrapper-JAR-Inhalts und seiner Java-17-Kompatibilität.

## Ergebnis

Die Kernprüfung deckt unter anderem ab:

- normalisierte rationale Zahlen,
- variadische Addition,
- lineares Gleichungslösen,
- exakte Matrixinversion,
- hierarchische Anschlusskompatibilität,
- topologische Kartenauswertung und CAS-Adapter.

## Nicht in dieser Umgebung ausführbar

Ein vollständiger Android-Gradle-Build konnte hier nicht ausgeführt werden, da kein Android SDK installiert und der direkte externe Downloadzugriff der Laufzeit gesperrt war. Das Repository enthält deshalb zusätzlich den reproduzierbaren GitHub-Actions-Build. Auf einem normalen Entwicklungsrechner lautet die verbindliche Prüfung:

```bash
./gradlew --stacktrace test :app:assembleDebug
```
