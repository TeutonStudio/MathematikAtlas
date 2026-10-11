# Aktueller verifizierter Projektzustand

> Stand: 2026-10-11, laufende Alpha-Konsolidierung. Nachweise gelten für den jeweiligen geprüften Arbeitsstand, nicht als Release-Abnahme. Vollständige Anforderungsmatrix: [V3_ALPHA_ABNAHME.md](../V3_ALPHA_ABNAHME.md).

## Metadaten

- Veröffentlichte Produktbasis: `v3.0.0`, Commit `df4f25dc6c41a31cb77c7627ef9897b827a056bb`; GitHub-master beim Live-Abgleich `6ac2b2b4`.
- Isolierter Integrationsbranch: `samai/v3.0.1/alpha-konsolidierung`; Android-Metadaten 3.0.1 / 3000001 und Desktop 3.0.1 für den Release vorbereitet. Keine neue Veröffentlichung erfolgt.
- Originalarbeitsbaum einschließlich IDE-Dateien vollständig gesichert und unverändert erhalten. Produktintegration im isolierten Arbeitsbaum.
- Karten-JSON-Writer: `KartenDatenJson.FORMAT_VERSION = 8`.
- Tatsächlicher statischer Erstellenkatalog: 136 Typen einschließlich Geometrie, Mengenräumen, Notizen und Kartengruppen; Baseline aus unverändertem v3.0.0 durch App-JVM-Test exportiert.

## Verifizierte Prüfungen

| Prüfung | Ergebnis |
|---|---|
| Repository, Architektur, Methodenmodell, Desktop-Shadowmodule | erfolgreich nach Registrymigration und Lint am 11.10.; finale Änderungen erneut prüfen |
| Releaseplan, Versionsfolge | erfolgreich für vorbereitete 3.0.1-Metadaten; finaler PR-/Pushereignisnachweis offen |
| Standardkartenprüfung | erfolgreich; 15 Karten |
| Release-Guardregressionen | 14 Tests erfolgreich, einschließlich echter Git-Events und unveränderlicher Vorgängerhistorie |
| Core-/Graph-/Adapter-/Knotentests | 429 / 93 / 49 / 552 grün nach Registrymigration; TypSystem zusätzlich 10 Tests grün |
| App-JVM-Tests und Android-Debug-Build | 253 Tests grün einschließlich Abbruch nach allen fünf Importzielen; assembleDebug und lintDebug (0 Fehler, 9 Warnungen) erfolgreich |
| Desktop-Katalogtest | vollständige Desktop-Tests erfolgreich (3 Tests); CLI-Persistenz-Smoke in zwei Prozessen schreibt/liest Version1 und2; lokale Paketierung/GUI-Abnahme offen |
| Android-Emulator API36 | isolierter AVD mit Hostrenderer bootet; JSON-SAF-Import, aktive Karte nach Prozessneustart, starke Kapselkontur, Aussagenstandard mit Undo/Redo und Persistenz geprüft |
| Importabbruch im Emulator | fehlendes/leeres Journal sichtbar gemeldet; unterbrochene Wiederherstellung wiederholt und alle 18 Bestandsdateien byteidentisch; Phasenregressionen grün |
| Schmale Androidansicht | Bibliothek zunächst geschlossen, als Overlay öffnungsfähig; Canvas nutzt verfügbare Breite; Eigenfilter enthält keine leeren Standardordner |
| Vollständige finale Plattformabnahme | Kernskript, 1389 JVM-/Desktoptests, lintDebug und APK grün; bestätigter Operatorwechsel/Undo/Redo, Kapseltouch und Hintergrundwechsel grün; Unterkartennavigation geprüft; Releaseintegration offen |

## Bestätigter Aufbau

- Android-Anwendung: `app`; Einstieg über `MainActivity` und `MathematikAtlasApp`.
- Fachneutraler Graph und Editor: `KnotenKartenVerwalter`.
- Mathematischer Rechenkern: `MathematikRechenSystem`.
- Graph-/Rechenkernadapter: `MathematikKartenAdapter`.
- Knotenvorlagen, Auswerter, Renderer und mathematische Kartenmigration: `MathematikKnoten`.
- Der Android-Speicher liegt unter dem app-internen Pfad `MathematikAtlas/karten/<karten-id>/v<version>.json`; vorhandene Dateien werden vor dem Überschreiben zusätzlich unter `MathematikAtlas/sicherungen/` gesichert.
- Kotlin-JVM-Tests sind vorhanden. Android-Instrumentierungstestquellen fehlen trotz vorhandener Testabhängigkeiten; manuelle Emulatornachweise ersetzen keine automatisierte Gestenabdeckung.

## Bestätigte Stärken

- Repository-, Architektur- und Versionsfolgeprüfungen bestehen; 3.0.1 verwendet die vorhandene Reservierung. 3.1.0 wird erst nach Abschluss des Vorgängers reserviert.
- Android-Debug-Build, Lint und vollständiger JVM-/Desktoplauf bestehen nach Registrykonsolidierung. Weitere Korrekturen müssen gezielt erneut geprüft werden.
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
  begrenzt Raster, Zertifikate und Orbits über separate Laufzeitbudgets. Gespeicherte Qualitätswerte bleiben beim Laden erhalten.
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
- Ein isolierter API36-Emulator ist verfügbar. Erste Touch-/Inspector- und Lebenszyklusnachweise liegen vor; die vollständige finale Bedienungsabnahme bleibt offen.

## Offene, priorisierte Android-Befunde

1. **Mittel – Start und Speichern können weiterhin umfangreiche Arbeit auf dem UI-Thread ausführen.** Importvalidierung, Transaktion und Listenaktualisierung laufen inzwischen auf Dispatchers.IO; automatische Speicherung wird während des Imports zurückgehalten. Verzögerungen sind noch nicht vollständig vermessen.
2. **Prüfgrenze – Navigation und alle Lebenszyklusübergänge.** Die aktive Karte wird aus gespeichertem Verweis wiederhergestellt und überlebt im Emulator einen Prozessneustart. Bibliothekswechsel, Unterkarte per Doppel-Tap, Brotkrumenrückkehr, Hintergrundwechsel und Neustart der aktiven Unterkarte sind im Emulator geprüft. Automatisierte Instrumentierungsabdeckung besteht weiterhin nicht.
3. **Prüfgrenze – Touch-Gesten.** Manuelle Emulatornachweise bestehen, Android-Instrumentierungstests fehlen weiterhin.
4. **Releaseintegration offen.** Operatordefinitionen, Signaturaliase, gemeinsamer Konfigurator, Verbrauchervereinheitlichung und explizite Fachauswerterkomposition sind implementiert und unabhängig ohne weitere Blocker geprüft. Finale Veröffentlichung und der nachfolgende 3.1.0-Umfang stehen noch aus.

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

## Kartenherkunft und Rechneroperatoren in v3.0.1

- Die Kartenbibliothek löst Herkunft mit der Priorität Standardprovenienz,
  Freigabequelle und app-lokaler Fremdimport auf und filtert eigene,
  vordefinierte und Fremdnutzerkarten samt ihren relevanten Ordnern.
- Datei- und HTTPS-Import verwenden denselben JSON-/Freigabepaket-/`.matlas`-
  Erkennungspfad. HTTPS bleibt auf sichere Weiterleitungen, Zeitlimits und
  höchstens 16 MiB begrenzt; ein Importjournal stellt gemeinsam veränderte
  Bibliotheksdateien nach Fehler oder Prozessabbruch wieder her.
- `RechnerOperatorRegistry` beschreibt Zahlen-, Vektor-, Matrix- und
  Tensoroperatoren mit Signaturen, Ports, Parametern, Fähigkeiten und Status.
  Der Operator-Suchindex und die abschließenden Familien-Dispatcher verwenden
  diese Registry; unbekannte persistierte IDs werden nicht umgeschrieben.

## Dokumentationszustand

- Die Dateien unter `docs/codex/plans/active/` sind keine verlässliche Liste laufender Arbeiten: Mehrere Einträge bezeichnen bereits veröffentlichte Versionen oder nennen sich selbst abgeschlossen.
- Architektur- und Releaseentscheidungen müssen weiterhin gegen Code, `release/roadmap.toml` und Git geprüft werden.
- Der abgeschlossene Audit enthält umsetzungsreife Reparaturpakete und klare Abnahmekriterien; er verändert noch kein Produktionsverhalten und keine Persistenzversion.
