# Aktueller verifizierter Projektzustand

> Stand: 2026-10-03. Ältere Zustandsangaben zu `v2.1.9`, Gradle 8.13 oder Kartenformat 2 sind überholt. Der ausführliche Android-Bestandsaudit liegt unter `plans/completed/2026-09-30-android-bestandsabsicherung.md`.

## Metadaten

- Veröffentlichte Basis: Commit `21ce83fe` auf `origin/master`, App-Version 2.33.1 (`versionCode` 2033001).
- Aktive Arbeit: `samai/v2.34.0/mengenvisualisierung-orbits`; 2.34.0 ist als y-Version für `mathematik.orbit` und `mathematik.orbitBeschraenktheit` reserviert.
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

1. **Blockierend – ungespeicherte Änderungen können beim schnellen Kartenwechsel oder Lebenszyklusende verloren gehen.** Der Autosave wartet 650 ms; Kartenwechsel ersetzt den Editorzustand ohne vorherigen Flush. Während einer offenen Verbindungsinteraktion kehrt der Speicherpfad außerdem still zurück.
2. **Hoch – `.matlas` ist derzeit ein reines Exportformat.** Die Oberfläche erzeugt einen ZIP-Container, der Import liest jedoch ausschließlich Text und besitzt keinen Container-Reader.
3. **Hoch – ungültige Importe können ungefangen aus dem Activity-Result-Callback werfen.** Eine sichtbare Importfehlerbehandlung fehlt.
4. **Hoch – eine beschädigte neueste Kartenversion lässt die Karte aus der Bibliothek verschwinden.** Ältere valide Versionen und Sicherungen werden nicht als Fallback geladen oder in der Oberfläche angeboten.
5. **Mittel – Start, Auswertung, Import und Speichern führen umfangreiche Arbeit auf dem UI-Thread aus.** Die tatsächliche Verzögerung auf einem Gerät ist mangels erfolgreichem Emulatorlauf noch nicht gemessen.
6. **Mittel – Activity-Neuerstellung stellt die zuletzt aktive Karte und Navigation nicht wieder her.** `AtlasZustand` wird mit `remember` neu aus dem alphabetisch ersten Speichereintrag aufgebaut.
7. **Prüfgrenze – Touch-Gesten und Lebenszyklusabläufe besitzen keine Android-Instrumentierungstests.** Der konfigurierte AVD konnte in dieser Umgebung nicht vollständig booten.

## Dokumentationszustand

- Die Dateien unter `docs/codex/plans/active/` sind keine verlässliche Liste laufender Arbeiten: Mehrere Einträge bezeichnen bereits veröffentlichte Versionen oder nennen sich selbst abgeschlossen.
- Architektur- und Releaseentscheidungen müssen weiterhin gegen Code, `release/roadmap.toml` und Git geprüft werden.
- Der abgeschlossene Audit enthält umsetzungsreife Reparaturpakete und klare Abnahmekriterien; er verändert noch kein Produktionsverhalten und keine Persistenzversion.
