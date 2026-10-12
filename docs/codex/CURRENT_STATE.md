# Aktueller verifizierter Projektzustand

> Stand: 2026-10-12, veröffentlichte V3 Alpha. Beide Releases und ihre PR-/Master-Push-Prüfungen sind abgeschlossen; lokales und GitHub-master wurden auf den veröffentlichten3.1.0-Commit synchronisiert. Vollständige Anforderungsmatrix: [V3_ALPHA_ABNAHME.md](../V3_ALPHA_ABNAHME.md).

## Metadaten

- Veröffentlichter Vorgänger: `v3.0.1`, GitHub-master-Commit `2842a03e18797d9689ebaf5fc3e389e1bb60ffc9`, PR#444; sämtliche PR- und Master-Push-Prüfungen grün.
- Veröffentlichte Alpha: `v3.1.0`, GitHub-master-Commit `811955f8c0bdd5f2bd59292cd9dea5ba634f1c51`, [PR#445](https://github.com/TeutonStudio/MathematikAtlas/pull/445); sämtliche PR- und Master-Push-Prüfungen grün.
- Produktstand: `3.1.0`, Android-Code `3001000`, Desktop `3.1.0`; Branch `samai/v3.1.0/alpha-erweiterungen`, Releasebranch `release/v3.1.0-alpha-erweiterungen`. Versionsachse `y`, exakt zwei neue Typen.
- Originalmaster per Fast-forward auf `811955f8c0bdd5f2bd59292cd9dea5ba634f1c51` synchronisiert; Produktarbeitsbaum sauber. Zehn persönliche IDE-Dateien bytegleich erhalten. Dauerhafter lokaler Nachweis: `Original/.gradle/alpha-konsolidierung/backup-20261011-094918/final-master-sync.json`. Persönliche IDE-Dateien gehören nicht zum Release.
- Karten-JSON-Writer: `KartenDatenJson.FORMAT_VERSION = 8`; neue Knoten benötigen keine Formaterhöhung.
- Statischer Erstellenkatalog: 138 Typen; unveränderte veröffentlichte3.0.0-Baseline136 plus `atlas.uebersetzer` und `mathematik.tupelAbflachen`.

## Verifizierte Prüfungen

| Prüfung | Ergebnis |
|---|---|
| Repository, Architektur, Methodenmodell, Desktop-Shadowmodule, Standardkarten15 | erfolgreich im finalen3.1.0-Stand |
| Releaseplan, Versionsfolge, 14 Guardregressionen | erfolgreich; PR-Ereignis gegen exakte3.0.1-Masterbasis geprüft; neue Typen exakt abgeglichen |
| Vollständige JVM-/Desktoptests | 1419 ohne Fehler oder Überspringen: TypSystem10, Kern433, Graph93, Adapter49, Knoten573, App258, Desktop3 |
| Android-Debug-Build und Lint | assembleDebug/lintDebug erfolgreich mit3.1.0-Metadaten; bestehende Lintwarnungen bleiben dokumentiert |
| Unabhängige Knotenverifikation | finale Codefreigabe ohne blockierende Befunde; mathematische Quellen-/Tupel-/Matrixverträge geprüft |
| Android API36 | SAF-JSON/.matlas für neue Knoten, Abflachenergebnis, Übersetzerzielwechsel mit Undo/Redo und Prozessneustart geprüft |
| Gemeinsamer Formeleditor | Desktop Tastatur/Stift/Foto-Modi; Footer bei1440×840 erreichbar. Android Zeichnen/Radieren, atomare Formelübernahme/Undo/Redo und gespeicherte Originalgesten geprüft;1,5facheSchrift/dunklesTheme |
| Persistenz und Import | beide neuen verbundenen Knoten über echte JSON/.matlas-Codecs mit erwarteter Auswertung geprüft; Zeichnungs-/AST-/Kontext-/Copy-/Undo-/Fehlerregressionen grün |
| Veröffentlichung3.1.0 | PR#445 und Master-Push-CI erfolgreich: Android38186386146, Linux38186386113, Mathematikkern38186386161, Release-Guard38186386173 |
| 3.0.1-Laufzeitnachweise | Importjournalwiederherstellung aller18Dateien byteidentisch; Operatorverlustbestätigung/Undo/Redo; Kapseltouch, Navigation, aktive Unterkarte und Prozessneustart |

Geräte mit physischem Stift, automatische Android-Instrumentierung, Frame-Timing und lokale Desktop-Installationspakete sind durch diese Tests nicht abgedeckt. Emulatorgesten und echte Desktop-Compose-Oberfläche ersetzen keine Hardwaremessung.

## Alpha-Erweiterungen in3.1.0

- Übersetzer mit gespeichertem Ziel, stabilen Anschlüssen und stark gerundeter Kapsel; historische Spezialknoten bleiben ladbar. Definitionen folgen dem gespeicherten Ziel.
- Heterogene Tupel, rekursives Abflachen und Term→Methode mit expliziter Quellenbindung, erhaltenen Außenquellen/Annahmen und kanonischem Tupelvertrag.
- Gemeinsame streng linksassoziative Matrixkette für beide Produktpfade; feste/innere Faktorverluste diagnostiziert, nur unverbundene Schlussreserve ausgelassen. Zusammenhängendes Falk-Schema auf Android/Desktop.
- Gemeinsamer Compose-Formeleditor und Konfigurationsplan; versionierte Originalzeichen-/Radiergesten bleiben vom AST getrennt. Automatische Handschrift-/Fotoerkennung ist sichtbar Beta; keine Fotopersistenz in Alpha.

## Bestätigter Aufbau

- Android-Anwendung: `app`; Einstieg über `MainActivity` und `MathematikAtlasApp`.
- Fachneutraler Graph und Editor: `KnotenKartenVerwalter`.
- Mathematischer Rechenkern: `MathematikRechenSystem`.
- Graph-/Rechenkernadapter: `MathematikKartenAdapter`.
- Knotenvorlagen, Auswerter, Renderer und mathematische Kartenmigration: `MathematikKnoten`.
- Der Android-Speicher liegt unter dem app-internen Pfad `MathematikAtlas/karten/<karten-id>/v<version>.json`; vorhandene Dateien werden vor dem Überschreiben zusätzlich unter `MathematikAtlas/sicherungen/` gesichert.
- Kotlin-JVM-Tests sind vorhanden. Android-Instrumentierungstestquellen fehlen trotz vorhandener Testabhängigkeiten; manuelle Emulatornachweise ersetzen keine automatisierte Gestenabdeckung.

## Bestätigte Stärken

- Repository-, Architektur- und Versionsfolgeprüfungen bestehen; 3.0.1 verwendet die vorhandene Reservierung. 3.1.0 wurde erst nach Veröffentlichung und grüner Master-CI des Vorgängers reserviert.
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
  Die unabhängige Knotenverifikation ist abgeschlossen; Veröffentlichung und grüne PR-/Master-Prüfungen sind durch PR#444 und PR#445 nachgewiesen.
- Ein isolierter API36-Emulator ist verfügbar. Erste Touch-/Inspector- und Lebenszyklusnachweise liegen vor; die hier dokumentierten Laufzeitnachweise ersetzen keine physische Geräteabnahme.

## Offene, priorisierte Android-Befunde

1. **Mittel – Start und Speichern können weiterhin umfangreiche Arbeit auf dem UI-Thread ausführen.** Importvalidierung, Transaktion und Listenaktualisierung laufen inzwischen auf Dispatchers.IO; automatische Speicherung wird während des Imports zurückgehalten. Verzögerungen sind noch nicht vollständig vermessen.
2. **Prüfgrenze – Navigation und alle Lebenszyklusübergänge.** Die aktive Karte wird aus gespeichertem Verweis wiederhergestellt und überlebt im Emulator einen Prozessneustart. Bibliothekswechsel, Unterkarte per Doppel-Tap, Brotkrumenrückkehr, Hintergrundwechsel und Neustart der aktiven Unterkarte sind im Emulator geprüft. Automatisierte Instrumentierungsabdeckung besteht weiterhin nicht.
3. **Prüfgrenze – Touch-Gesten.** Manuelle Emulatornachweise bestehen, Android-Instrumentierungstests fehlen weiterhin.
4. **Releaseintegration.** Operatordefinitionen, Signaturaliase, gemeinsamer Konfigurator, Verbrauchervereinheitlichung und explizite Fachauswerterkomposition sind implementiert und unabhängig ohne weitere Blocker geprüft. Der3.1.0-Umfang ist implementiert, unabhängig geprüft und als PR#445 veröffentlicht. Die grünen Release-PRs und der dokumentierte Git-Gleichstand schließen die Alpha-Konsolidierung ab.

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

## Operandenreihenfolge im Arbeitsstand (2026-10-12)

- Redundante Matrix- und Aussagenoperationen werden beim Laden/Import über den gemeinsamen mathematischen Codec in vorhandene Rechner überführt; bisherige Zahlen-, Mengen-, Vektor- und Tensormigrationen bleiben eingebunden.
- Assoziative Operatoren beschreiben variadische Rollen ausdrücklich. Gespeicherte Anschlussreihenfolge steuert Auswertung, Formel und Falk-Schema.
- Android und Desktop verwenden dieselbe vertikale Operandenliste mit Ziehgriff und Auf-/Ab-Schaltflächen. Umordnung bewahrt IDs, Verbindungen und freie Schlussreserve und bildet einen Undo-Schritt.
- Keine neue Knotenart, Produktionsabhängigkeit, JSON-Formatversion oder veröffentlichte Produktversion. Siehe ADR 0044 und den ExecPlan vom 2026-10-12.

## Dokumentationszustand

- Die Dateien unter `docs/codex/plans/active/` sind keine verlässliche Liste laufender Arbeiten: Mehrere Einträge bezeichnen bereits veröffentlichte Versionen oder nennen sich selbst abgeschlossen.
- Architektur- und Releaseentscheidungen müssen weiterhin gegen Code, `release/roadmap.toml` und Git geprüft werden.
- Der abgeschlossene Audit enthält umsetzungsreife Reparaturpakete und klare Abnahmekriterien; er verändert noch kein Produktionsverhalten und keine Persistenzversion.

## Nachprüfung des separaten Umkehrmethoden-PR

[PR#436: Review auf3.1.0](https://github.com/TeutonStudio/MathematikAtlas/pull/436#issuecomment-6115383718). Der isolierte Zwei-Dateien-Patch ist konfliktfrei anwendbar. Die ursprünglichen fünf Tests scheitern am Variablennamen `null`; nach temporärer Umbenennung in `nullWert` sind sie grün. Die Einbindung des neuen Inversetyps in den aktuellen Methodenaufruf benötigt gesonderte Arbeit. Keine Umschreibung des alten Branches und keine Übernahme der Umkehrmethoden in Alpha.
