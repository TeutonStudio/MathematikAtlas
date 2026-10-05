# Aktueller verifizierter Projektzustand

> Stand: 2026-10-05. Ältere Zustandsangaben zu `v2.1.9`, Gradle 8.13 oder Kartenformat 2 sind überholt. Der ausführliche Android-Bestandsaudit liegt unter `plans/completed/2026-09-30-android-bestandsabsicherung.md`.

## Metadaten

- Veröffentlichte Basis vor diesem Abschluss: `v2.34.0` auf Commit `f4be87a7`.
- Releaseabschluss: `samai/v3.0.0-releaseabschluss`; App-Version 3.0.0 (`versionCode` 3000000).
- Zielplattform dieser Prüfung: Android; gemeinsame Kotlin-Module wurden über ihre Android-Verwendung geprüft.
- Der Arbeitsbaum enthielt bereits lokale Änderungen an Android-Gradle-Plugin, Gradle-Wrapper und Android-Studio-Dateien. Diese Änderungen wurden erhalten und beim Build mitgeprüft.
- Aktueller Karten-JSON-Writer: `KartenDatenJson.FORMAT_VERSION = 8`.

## Verifizierte Prüfungen

| Prüfung | Ergebnis |
|---|---|
| `python3 scripts/pruefe_repository.py` | erfolgreich; Architektur, XML, Wrapper und Desktop-Shadowmodule geprüft |
| `python3 scripts/pruefe_releaseplan.py` | erwartungsgemäß noch nicht erfolgreich; die reservierte 2.34.0 ist bis zur Releaseintegration aktiv |
| `python3 scripts/pruefe_versionsfolge.py` | erfolgreich |
| `python3 scripts/pruefe_standardkarten.py` | erfolgreich; 15 Karten geprüft |
| `python3 scripts/pruefe_methodenmodell.py` | erfolgreich |
| `python3 scripts/pruefe_kern.py` | nicht ausführbar; das Skript fand in seiner Umgebung `kotlinc` nicht |
| `./gradlew test :app:assembleDebug` | erfolgreich mit den bereits lokal geänderten AGP-/Gradle-Versionen; vollständige JVM-Tests und Debug-APK gebaut |
| Katalog-zu-Auswerter-Regressionstest | erfolgreich; jede sichtbare mathematische Knotenart besitzt einen registrierten oder zentral behandelten Auswertungspfad |
| Android-Emulator | nicht ausführbar; der vorhandene AVD `Medium_Tablet` endete vor abgeschlossenem Boot mit Exitcode 139, ein erneuter ADB-Start ist in der Sandbox durch Socket-/Netlink-Rechte gesperrt |

## Bestätigter Aufbau

- Android-Anwendung: `app`; Einstieg über `MainActivity` und `MathematikAtlasApp`.
- Fachneutraler Graph und Editor: `KnotenKartenVerwalter`.
- Mathematischer Rechenkern: `MathematikRechenSystem`.
- Graph-/Rechenkernadapter: `MathematikKartenAdapter`.
- Knotenvorlagen, Auswerter, Renderer und mathematische Kartenmigration: `MathematikKnoten`.
- Der Android-Speicher liegt unter dem app-internen Pfad `MathematikAtlas/karten/<karten-id>/v<version>.json`; vorhandene Dateien werden vor dem Überschreiben zusätzlich unter `MathematikAtlas/sicherungen/` gesichert.
- Es gibt 244 Kotlin-Testdateien in den untersuchten Android- und gemeinsamen Modulen. Android-Instrumentierungstestquellen fehlen trotz vorhandener Testabhängigkeiten.

## Bestätigte Stärken

- Repository-, Architektur- und Versionsfolgeprüfungen bestehen; der Releaseplan bleibt bis zum Abschluss der reservierten 2.34.0 absichtlich aktiv.
- Der vollständige JVM-Testlauf und der Android-Debug-Build bestehen.
- Die 15 ausgelieferten Standardkarten werden strukturell geprüft und durch den echten Auswerter ohne gemeldete Fehler ausgewertet.
- Repräsentative Tests decken Zahlenrechnung, Mengen, Aussagenlogik, Methoden/Analysis, Vektoren und Matrizen einschließlich fachlicher Randfälle ab.
- Editor-Unit-Tests decken Verbindungskompatibilität, belegte Eingänge, dynamische Anschlüsse, Kopieren, Löschen, Mehrfachaktionen sowie Undo/Redo ab.
- Der neue Querschnittstest verhindert sichtbare mathematische Knotenarten ohne Auswertungspfad.

## Mengenvisualisierung und Orbitmengen in Abschlussprüfung

- Die Mengenvisualisierung trennt exakte R1-Algebra, rationale Zellnachweise und
  numerische Punktvorschau. Enthaltene, gemischte und unbekannte Bereiche besitzen
  getrennte Status; unbekannte Bereiche bleiben in R1, R2, R3 und C sichtbar.
- Exakte rationale Grenzen und offene Endpunkte werden erst nach dem
  Fensterbeschnitt in Zeichenkoordinaten umgewandelt. N beginnt bei 1;
  Zeichentoleranzen verändern weder Ganzzahligkeit noch Mengentopologie.
- Feste Schnitte binden nicht dargestellte Variablen ausdrücklich. Eine optionale
  Farbdimension kann mehrere Werte pro Bildpunkt erhalten und markiert diese Fälle.
- Der Sampler arbeitet auf `Dispatchers.Default`, prüft den Coroutine-Abbruch und
  begrenzt Raster, Zertifikate und Orbits über persistierte Budgets.
- `OrbitFamilie` und `OrbitBeschraenktheitsMenge` liegen im Compose-freien Kern.
  Allgemeine endliche Nichtflucht bleibt unbestimmt; exakte Zyklen beweisen
  Beschränktheit. Für die erkannte Familie `z²+c` mit Nullstart kommen sichere
  Kardioiden-, Periode-2- und Fluchtnachweise hinzu.
- Die neue Mandelbrot-Beispielkarte besteht aus regulären Variablen-, Potenz-,
  Additions-, Methoden-, Orbit-, Mengen- und Visualisierungsknoten. Historische
  Standardkartenmigrationen bleiben auf die bisherigen fünf Beispielkarten
  begrenzt; die neue Karte wird als Standardkarten-Asset im Ordner
  `Standardkarten/Dynamische Systeme/01 Iterationsmengen` installiert.
- Die 28 konkreten Zahltypen und 42 konkreten Mengentypen sind mit ihrem aktuellen
  Visualisierungsstatus in `plans/active/2026-10-01-cas-abdeckungsmatrix.md`
  dokumentiert. Nicht räumliche und nicht entscheidbare Fälle werden nicht als
  leere Mengen ausgegeben.
- Gezielte Kern-, Knoten-, Katalog-, Visualisierungs-, Beispielkarten- und
  JSON-Roundtriptests sowie der vollständige JVM-Testlauf und Debug-Build bestehen.
  Die unabhängige Knotenverifikation und Releaseintegration sind noch nicht abgeschlossen.
- Ein Gerät ist nicht verbunden. Touch, Inspectorbedienung und Lebenszyklus bleiben
  daher eine ausdrückliche Laufzeitprüfgrenze.

## Offene, priorisierte Android-Befunde

1. **Mittel – Start, Auswertung und der persistierende Anteil von Import und Speichern führen weiterhin umfangreiche Arbeit auf dem UI-Thread aus.** Dateistream-I/O für Import und Export läuft inzwischen auf `Dispatchers.IO`; die tatsächliche Verzögerung auf einem Gerät ist mangels erfolgreichem Emulatorlauf noch nicht gemessen.
2. **Mittel – Activity-Neuerstellung stellt die zuletzt aktive Karte und Navigation nicht vollständig wieder her.** Der Kartenstand wird bei `ON_STOP` gesichert, `AtlasZustand` wird aber weiterhin mit `remember` aufgebaut.
3. **Prüfgrenze – Touch-Gesten und Lebenszyklusabläufe besitzen keine Android-Instrumentierungstests.** Der konfigurierte AVD konnte in dieser Umgebung nicht vollständig booten.

## Abstraktionswerkzeug und Speicherstabilisierung

- Der fachneutrale Kartenkern besitzt eine gemeinsame Teilgraphanalyse sowie eine
  snapshot-validierte, atomare `TeilgraphErsetzen`-Aktion mit Undo/Redo.
- Das neue Abstraktionsregister erkennt beweisbar polynomiale Termgraphen und
  ersetzt sichere Kandidaten durch die vorhandenen Tupel- und Polynomknoten.
  Extern verwendete Zwischenknoten werden nicht gelöscht; nachgeschaltete
  `TermZuMethode`-Schnittstellen bleiben erhalten.
- Der App-Dialog sortiert positive Einsparungen deterministisch, kann Kandidaten
  im Graph markieren und analysiert nach jeder semantischen Kartenänderung neu.
- `.matlas` kann nun mit Größen-, Pfad-, Manifest- und Prüfsummenvalidierung
  importiert werden. Importfehler werden sichtbar behandelt; beschädigte neueste
  Kartenversionen fallen beim Listen und Laden auf die jüngste lesbare Version
  zurück.
- Ein Kartenwechsel und `ON_STOP` brechen nur die flüchtige Verbindungsvorschau
  ab und sichern danach den aktuellen Kartenstand.

## Dokumentationszustand

- Die Dateien unter `docs/codex/plans/active/` sind keine verlässliche Liste laufender Arbeiten: Mehrere Einträge bezeichnen bereits veröffentlichte Versionen oder nennen sich selbst abgeschlossen.
- Architektur- und Releaseentscheidungen müssen weiterhin gegen Code, `release/roadmap.toml` und Git geprüft werden.
- Der abgeschlossene Audit enthält umsetzungsreife Reparaturpakete und klare Abnahmekriterien; er verändert noch kein Produktionsverhalten und keine Persistenzversion.
