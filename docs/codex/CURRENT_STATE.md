# Aktueller verifizierter Projektzustand

> Stand: 2026-10-12, veröffentlichte V3 Alpha. Beide Releases und ihre PR-/Master-Push-Prüfungen sind abgeschlossen; lokales und GitHub-master wurden auf den veröffentlichten3.1.0-Commit synchronisiert. Vollständige Anforderungsmatrix: [V3_ALPHA_ABNAHME.md](../V3_ALPHA_ABNAHME.md).

## Metadaten

- Veröffentlichter Vorgänger: `v3.0.1`, GitHub-master-Commit `2842a03e18797d9689ebaf5fc3e389e1bb60ffc9`, PR#444; sämtliche PR- und Master-Push-Prüfungen grün.
- Veröffentlichte Alpha: `v3.1.0`, GitHub-master-Commit `811955f8c0bdd5f2bd59292cd9dea5ba634f1c51`, [PR#445](https://github.com/TeutonStudio/MathematikAtlas/pull/445); sämtliche PR- und Master-Push-Prüfungen grün.
- Produktstand: `3.1.0`, Android-Code `3001000`, Desktop `3.1.0`; Branch `samai/v3.1.0/alpha-erweiterungen`, Releasebranch `release/v3.1.0-alpha-erweiterungen`. Versionsachse `y`, exakt zwei neue Typen.
- Originalmaster per Fast-forward auf `811955f8c0bdd5f2bd59292cd9dea5ba634f1c51` synchronisiert; Produktarbeitsbaum sauber. Zehn persönliche IDE-Dateien bytegleich erhalten. Dauerhafter lokaler Nachweis: `Original/.gradle/alpha-konsolidierung/backup-20261011-094918/final-master-sync.json`. Persönliche IDE-Dateien gehören nicht zum Release.
- Karten-JSON-Writer: `KartenDatenJson.FORMAT_VERSION = 9`; neue Knoten benötigen keine Formaterhöhung.
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


## Mengenoperatorgrundlage – lokaler Arbeitsstand 2026-10-12

- Die zehn bestehenden Mengenquellen besitzen `MengenKonstante`-Definitionen im
  Kern mit stabilen Auswahlkennungen und nullstelligen `OperatorSignatur`-Verträgen.
  Mengenknoten und Mengenrechner nutzen dieselbe App-Dialogvorlage; mathematische
  Definitionen werden im Dialog mit `LatexText` angezeigt. Eigene versionsfeste
  Mengenkarten bleiben erhalten; unbekannte Quellen liefern keinen Ersatzwert.
- Mitgliedschaft, Inklusion, Gleichheit, Disjunktheit und sichere Mengenalgebra
  berücksichtigen N0, Primzahlen und gaußsche Mengen. Exakte Primzahltests haben
  eine Grenze von 10000 ungeraden Divisorkandidaten; danach bleibt die Aussage
  unbekannt. Kardinalitäts- und explizite Topologiepfade nutzen dieselbe Grundlage.
- Vollständiger Gradle-Testlauf, Android-Debug-Build, Desktop-Kompilierung sowie
  Repository-/Releaseplan-/Versionsfolgeprüfungen erfolgreich. Aktuelle Berichte:
  1447 Tests ohne Fehler/Überspringungen. Unabhängige statische Abnahme ohne Blocker.
- Keine neue Veröffentlichung; Änderungen uncommittet auf dem bestehenden
  Arbeitsbranch. Keine Dialog-Laufzeitprüfung mangels verbundenem Android-Gerät
  oder eingerichtetem AVD. Eigenständige Kern-Skriptprüfung benötigt fehlendes
  `kotlinc`; Gradle-Kernprüfung erfolgreich. Der DesktopInspektor besitzt weiterhin
  keinen Mengen-Auswahldialog. Details im abgeschlossenen
  [ExecPlan](plans/completed/2026-10-12-mengenoperatorgrundlage.md).


## Visuelle Gruppen – lokaler Implementierungsstand 2026-10-12

- Gemeinsamer Android-/Desktopeditor unterstützt verschachtelte Gruppen, Inspector-Faltung, abgeleitete Grenzanschlüsse und separat navigierbare Gruppenansichten auf demselben kanonischen Graphen. Auflösen erhält Inhalte; Ausschneiden entfernt den gewählten Teilbaum atomar. Undo/Redo, Kopieren, JSON/.matlas und eigene Gruppenkameras sind berücksichtigt.
- JSON-Format 9 ergänzt Faltzustand, Elternreferenz und Gruppenkamera; alte flache Gruppen werden beim Laden räumlich migriert. Keine neue registrierte Knotenart oder Produktionsabhängigkeit.
- Projektion wird zwischengespeichert, bei großen Karten im Hintergrund berechnet und bei überholtem Zustand verworfen. Eingeklappte Inhalte werden nach dem Übergang nicht weiter komponiert; Anschlüsse und Verbindungen verwenden Viewport-Culling.
- Desktop-Smoke-Test bestätigte Gruppenbaum, Inspector, Faltung, Gruppenansicht und Auswahl des Originalknotens über einen abgeleiteten Ausgang. JVM-Skalierungstest deckt 100/500/1000 Knoten und bis knapp 2000 Verbindungen ab. Android-Framezeiten sind mangels angeschlossenem Gerät/Emulator noch nicht gemessen.
- Architekturentscheidung: [ADR 0046](decisions/0046-visuelle-gruppenprojektion.md). Prüfdetails und Grenzen: [ExecPlan](plans/active/2026-10-12-visuelle-gruppen-faltung.md). Lokaler Arbeitsstand, kein Release oder neuer Commit.


## Einheitliches Dialogsystem im Arbeitsstand (nicht veröffentlicht)

- Gemeinsamer fachneutraler Compose-Dialograhmen mit festen Abständen für Kopf,
  Untertitel, Inhalt, Abschnitte, Einträge und umbrechende Aktionen. Android,
  Desktop und gemeinsame Formeleditoren verwenden dieselben Bausteine.
- Alle gefundenen direkten Compose-Dialogaufrufe liegen jetzt im gemeinsamen
  Rahmen; native Dateidialoge bleiben unverändert. Titel und Aktionen konkurrieren
  nicht um die Textbreite. Kopf und Fuß sind bei Überlänge begrenzt scrollbar.
- Gesamtprüfung aus JVM-Tests, Android-Debug-Build/Lint und Desktop-Kompilierung
  erfolgreich. Grafischer Desktoptest prüft zwölf Größen-/Schrift-/Themevarianten,
  Scrollen bis zum letzten Eintrag und aktiviertes/deaktiviertes Außenklickverhalten.
- Keine Android-Laufzeitabnahme: ADB meldet kein Gerät und es ist keine AVD installiert.
  Einzelheiten und Abnahmegrenzen stehen im
  [ExecPlan](plans/active/2026-10-12-einheitliches-dialogsystem.md) und in ADR 0047.


## Releasekandidat v3.1.1 – Abschlussprüfung 2026-10-12

- Der vollständige Releaseumfang umfasst Rechnerkonsolidierung und Operandenreihenfolge, Mengenoperatorgrundlage und Prädikatsauswertung, Gruppenprojektion mit Kartenformat 9 sowie gemeinsame Dialoge. Der Erstellenkatalog reduziert sich von 138 auf 128 Typen; es entsteht kein neuer Knotentyp (Versionsachse x).
- Android-Metadaten sind 3.1.1/3001001; Desktop-Paketmetadaten und Workflow-Artefaktname stimmen überein. Bestätigte GitHub-master-Basis ist e8f1c9d6 nach dem finalen v3.1.0-Commit 811955f8.
- Der Rechenweg-Desktoptest verwendet Compose-Semantik statt nativer AWT-Window-/Robotannahmen und prüft Öffnen, Zurück sowie das Verwerfen veralteter Details nach Ergebniswechsel. Der gezielte Test und der anschließende vollständige Gradle-Testlauf samt Android-Debug-Build sind erfolgreich. Die zusätzliche Compose-Testabhängigkeit ist auf den Desktop-Testclasspath beschränkt.
- Repository-, Architektur-, Methodenmodell-, Releaseplan-, Versionsfolgeprüfung und 14 Guard-Regressionen erfolgreich. Android-Release-Build erfolgreich, bisher ohne konfigurierte Produktionssignatur. Keine Android-Geräte-/Emulatorabnahme. Die separate RPM-Paketierung scheitert lokal an jpackage (unsupported type rpm); sie ist kein APK-Artefakt.
- Dies ist ein vorbereiteter Release-PR-Stand. Veröffentlichung und finale Commit-SHA sind ausschließlich anhand von GitHub-master und GitHub Release zu bestimmen.


## Rechner-Auswertungsschemata im lokalen Arbeitsstand

- Android-Rechnerinspektoren, dynamische Konzeptreiter und Desktop-Inspector öffnen gemeinsame Schema-Inhalte. Spezialdefinitionen von Formelmodi, Quadrat und Kubik bleiben erhalten.
- Matrixketten zeigen jedes Zwischenprodukt einmal. Ergebniszellen wählen Zeile/Spalte; auswählbare geordnete Einzelprodukte markieren die zugehörigen Faktoren. Ergebniswechsel setzt die atomare Auswahl zurück.
- Strukturwerte bieten achsenweise Komponentenauswahl und farblich zugeordnete Eingabe-/Ergebniskomponenten; Addition zeigt deren tatsächliche Summe. Methoden zeigen vorhandene Vorschriften, gemeinsame Parameter, Definitionsraum und Ergebnisannahmen. Methodenwertige Matrixprodukte verwenden den Methodenpfad.
- Registry-Abdeckung, Unbekannt-Diagnose, Tensorindizes, Zahlenformelparameter und Methodenrouting sind durch Modelltests geprüft; Compose prüft Auswahlwechsel auf P2 und Ergebniswechsel. Vollständiger Gradle-Testlauf, Android-Debug-Build/Lint, Desktop-Kompilierung und Repositoryprüfung erfolgreich. Unabhängige Diffprüfung ohne verbleibende Blocker.
- Keine Android-Laufzeitabnahme: ADB meldet keine Geräte. Keine visuelle Abnahme großer Schrift oder Themevarianten. Zusätzlicher direkter Kern-Smoke-Test mangels kotlinc/java im PATH nicht ausführbar; Gradle-Kerntests erfolgreich. Kein Release oder Commit. Details: [ExecPlan](plans/active/2026-10-12-rechner-auswertungsschemata.md), [ADR 0048](decisions/0048-rechner-auswertungsschemata.md).


## Polarkoordinaten im Arbeitsstand (2026-10-12)

- Bestehender Übersetzer unterstützt kartesisch↔polar für reelle Tupel; der Vektorrechner bietet Radius, Winkeltupel/Vorzeichen und kartesische Koordinaten aus Radius plus Winkeln.
- Gradkonvention: Azimut zuerst in [0°,360°), weitere Polarwinkel zu x₃,…,xₙ in [0°,180°]. R¹ verwendet Betrag und skalares Vorzeichen; flache Übersetzertupel benötigen für die R¹-Rückrichtung den gespeicherten Schalter.
- Numerische Umrechnung liegt gemeinsam im Rechenkern. Graphdimensionen, Formelpfad, stabile Übersetzeranschlüsse, JSON und Undo/Redo sind getestet; keine neue Knotenart, Produktionsabhängigkeit oder Formatversion.
- Vollständiger Gradle-Testlauf, Android-Debug-Build, Desktopkompilierung und Repositoryprüfung erfolgreich; unabhängige Verifikation ohne Blocker. Keine manuelle Geräteabnahme: ADB meldet kein verbundenes Gerät. Zusätzliches Kernskript benötigt ein fehlendes separates kotlinc; Gradle-Kerntests bestehen.
- Details und Prüfgrenzen: [ExecPlan Polarkoordinaten](plans/completed/2026-10-12-polarkoordinaten.md), ADR 0049. Keine Commits oder Veröffentlichung durch diese Aufgabe.


## Gradmenge und Definitionskomposition – lokaler Arbeitsstand 2026-10-12

- Die separate Gradmenge mit Winkeltyp identifiziert 360° und 0°. R↔Grad im bestehenden Übersetzer verwendet Bogenmaß und wählbare Hauptvertreter; der Inspector zeigt die bestehende Mengenintervallnotation. Allgemeine Parameter können explizit Winkel sein. Polarwinkel sind nun Winkelwerte, historische numerische Gradrepräsentanten bleiben rückwärtskompatibel.
- Der bestehende Zahlenrechner besitzt eine Grad-Sinus-Signatur aus dem gemeinsamen Register. Die reelle Sinusdefinition ist der ausführbare Graph q→Grad-Sinus→Kartenausgang; die geometrische Grunddefinition vermeidet einen Definitionszyklus. Die bisherigen numerischen/komplexen Auswertungsmodi bleiben erhalten.
- Tupel behalten gelieferte Komponentenobermengen und deren Vereinigung als gemeinsame Grundmenge. Strukturübersetzungen, Abflachen und leere Tupel berücksichtigen diese Metadaten; Tupel, Zeile und Spalte bleiben getrennte Strukturtypen.
- [Definitionsanalyse](DEFINITIONSKARTEN_ANALYSE.md): 465 konfigurierte Einträge und 131 statische Definitionsassets, davon 113 mit Regelknoten und 18 ohne. Weitere Zerlegungen sind priorisierte Vorschläge, noch nicht umgesetzt.
- 1525 Tests ohne Fehler/Überspringen, Android-Debug-Build und Desktopkompilierung erfolgreich. Repository-, Releaseplan- und Versionsfolgeprüfung erfolgreich. Unabhängige Produktcodefreigabe; keine manuelle Geräte-/UI-Abnahme. Separates Kernskript benötigt fehlende Werkzeuge, Gradle-Kerntests bestehen. Keine neue Version, Knotenart, Abhängigkeit oder JSON-Formatversion; kein Commit/Release.
- Nachweise: [ExecPlan](plans/completed/2026-10-12-gradmenge-definitionen.md), [ADR 0050](decisions/0050-gradmenge-und-definitionskomposition.md).

## Vektorpotenz, Unterraum und Geometrie – lokaler Arbeitsstand 2026-10-12

- Bestehender Vektorrechner besitzt Vektorpotenz mit nichtnegativem ganzzahligen Exponenten, ausgewähltem Skalarprodukt und paritätsabhängigem Zahl-/Vektorergebnis. p=0/1 berechnen kein Selbstprodukt. Tupel werden ausdrücklich als Spalten interpretiert.
- Bestehender Mengenrechner bildet linearen/affinen Spann als argumentabhängige Kernmenge. Rationale Rang-/Mitgliedschaftsprüfungen sind exakt; symbolische Pivots bleiben unbestimmt.
- Bestehender Übersetzer bietet Geometriekoordinatenmengen, Objektpunkte, Mittelpunkte und erkannte geometrische Rückrichtungen. Reellheit, Raum-/Dimensionsverträglichkeit, Ankerzugehörigkeit und symbolische Grenzen werden berücksichtigt.
- Skalarproduktprüfung akzeptiert bloße Referenztexte nicht mehr als Beweis. Nicht nachgewiesene eigene Produkte führen eine ausdrückliche Annahme weiter; widerlegte Produkte werden abgewiesen. Android und Desktop bieten Produktauswahl und Potenzvorgabe.
- Vollständiger Gradle-Testlauf erfolgreich: 319 Testsuiten, 1536 Tests ohne Fehler. Android-Debug-APK und Desktopkompilierung sowie Android-Lint erfolgreich; unabhängige Prüfung ohne verbleibende Blocker. Separates Kernskript benötigt fehlendes kotlinc, Gradle-Kerntests bestehen. Keine Geräteabnahme, kein Commit oder Release. Reservierte x-Version 3.1.2, Produktversion unverändert 3.1.1. Details: [ExecPlan](plans/completed/2026-10-12-vektorpotenz-unterraum-geometrie.md), [ADR 0051](decisions/0051-vektorpotenz-unterraum-geometrie.md).


## Zertifizierte Polynomlösungen im Arbeitsstand (2026-10-12)

- Der gemeinsame Langdruckdialog bietet analytische und numerische Tabs für erkannte Polynomgleichungen und Eigenwertanfragen. n=0..100 bezeichnet die ersten Dezimalziffern ohne Rundung; Konfiguration wird über vorhandene Kartenaktionen gespeichert.
- Reelle rationale Polynome verwenden Sturm-Isolation, komplex-rationale Polynome lokale Rouché-Scheiben mit unabhängig prüfbarer Einschließung. Existenz, Vollständigkeit, Dimension, Vielfachheit und Ziffernnachweis werden getrennt behandelt; fehlende Nachweise bleiben offen.
- Vorhandene Variablenknoten können quadratische Matrixunbekannte als geordnete Eintragsvariablen erzeugen. Matrix-/Vektorgleichheiten werden gemeinsam komponentenweise gelöst. Lineare Systeme liefern exakte Familien samt numerisch auswählbaren Mitgliedern; nichtlineare Systeme liefern exakt geprüfte Mitglieder oder lokale Krawczyk-Nachweise.
- Analytische Darstellungen bis Grad vier enthalten Zweigbedingungen. Eigenwerte rational-komplexer Matrizen bis Dimension 8 verwenden Faddeev–LeVerrier und denselben Wurzellöser. Verfahrensgrenzen und Architekturentscheidung: ADR 0052; vollständiger Nachweis im abgeschlossenen ExecPlan 2026-10-12-zertifizierte-polynomloesungen.
- Finaler Gesamtprüflauf: 1557 Tests ohne Fehler oder Überspringen, Android-Debug-Build erfolgreich. Zwei neue Desktop-Compose-Laufzeittests prüfen Tabs, Genauigkeit und Familienparameter. Repository-/Architekturprüfung, Releaseplan und Versionsfolge erfolgreich; unabhängige Abschlussprüfung ohne offene Findings.
- Prüfgrenzen: kein Android-Gerät/Emulator verbunden, daher keine Android-Gestenprüfung. Das zusätzliche Kernskript benötigt kotlinc, das im PATH fehlt; Kern-Gradletests bestehen. Keine neue Knotenart, Abhängigkeit oder Kartenformatversion; keine Veröffentlichung oder neuer Commit.

## Integrierter Stand v3.2.0 (Releaseprüfung, 2026-10-13)

TODO 22b56645 und master 7e11b711 sind im isolierten Arbeitsbereich dreiseitig integriert. Strukturierte Winkelparameter erhalten gemeinsame Herkunft, Wn-Domäne und öffentliche Argumentpackung bis zur Bildmenge. Polarkonvention wird explizit gespeichert und historisch migriert. Produktgitter beziehungsweise deterministische Halton-Abtastung berücksichtigen alle Winkelachsen und diskreten Vorzeichen; Ausgabekoordinaten werden erst nach vollständiger Auswertung projiziert. Das Indexdefinitionsinventar ist leer; symbolische Zerlegungsrezepte liefern ausführbare Kandidatenbedingungen mit ausgewiesenen Materialisierungsgrenzen.

Vollständige lokale Gradle-Tests, Android-Debug-Build, Lint, Desktop-Kompilierung und acht gezielte Desktop-Compose-Regressionen erfolgreich. Android-Instrumentierung im isolierten API35-CI-Emulator erfolgreich (PR #449, Lauf 38400142819); sämtliche PR-Prüfungen am Produkt-HEAD ae1669fc grün. Lokale isolierte Emulatorstarts scheitern im nativen Emulatorprozess; kein physisches Gerät verbunden. Der Releaseabschluss erfolgt als geprüfter Squash v3.2.0.

## Variablengrundraum und Tupelinspector (Arbeitsstand 2026-10-13)

- Variablen besitzen einen optionalen Mengeneingang `grundraum`; verbunden ersetzt er den Inspectorbereich einschließlich Intervall. Ohne Verbindung gilt der bisherige Bereich. Strukturierte Variablen verwenden ihn als Komponentenbereich; Winkelstrukturen behalten ihren festen Winkelraumvertrag.
- Der gemeinsame mathematische Codec ergänzt den Eingang beim Laden/Import und Schreiben idempotent, ohne Ausgangsreferenzen zu ändern.
- Android bietet für Tupel rationale Standardwerte und die vorhandene Operandenliste mit Ziehgriff und Auf-/Ab-Schaltflächen. Umordnung und Längenänderung erhalten bestehende Anschlussidentitäten und Reihenfolge und sind rückgängig machbar.
- Vollständiger Gradle-Testlauf und Android-Debug-Build erfolgreich; unabhängige Diffprüfung ohne verbliebene konkrete Findings. Keine lokale Geräteprüfung dieser Änderungen. v3.2.1 wird als Teilumfang auf dem SamAI-Branch vorbereitet; bis der finale Versionscommit auf `master` liegt, gilt sie nicht als veröffentlicht.

## Grad-Richtungsraum und Bildmenge (Arbeitsstand 2026-10-13)

- Gradmenge ohne `dim` bleibt die Winkelgruppe G. Mit `dim=d` (1–65) liefert sie den signierten Richtungsraum W_(d−1): ein Vorzeichen und d−1 Halbkreiswinkel. Diese Korrektur ersetzt die unveröffentlichte G^d-Erweiterung.
- Ein verbundener kanonischer Richtungsraum bestimmt die Winkelvariablendimension. Nachgewiesene endliche Richtungsprodukte der Definitionskarte werden beim strukturierten Verbraucher gepackt; allgemeine Funktionsprodukte bleiben unverändert.
- Die Grad-Definitionskarte verwendet die Fallunterscheidungsgruppe des Nutzers mit endlichen Indizes {0,…,d−1} und [0,180)-Winkelvertretern. Der Definitionsresolver übernimmt eine konkret angeschlossene Dimension.
- Der historische Tupel-zu-Vektor-Adapter erhält Variablenquellen und Bereichsmetadaten; dadurch kann Term→Methode die strukturierte Winkelquelle nach einem Matrixprodukt binden.
- Regressionen prüfen Dimensionen 1–4 sowie eine vollständige gespeicherte Definitions-/Diagonalmatrix-/Bildmengenkarte bis zum Sampler. Tensorstufe bleibt Tensorgrad; die Koordinatenanzahl muss ausdrücklich als Dimension verwendet werden.
- Gesamtprüflauf `./gradlew test :app:assembleDebug` erfolgreich; damalige Gradle-XML-Berichte: 1664 Tests ohne Fehler oder Überspringungen. Repository-/Releaseplan-/Versionsfolgeprüfung erfolgreich; unabhängige Codeabnahme ohne Blocker. Keine Geräteprüfung. Anschließend in Branchcommit `8465f6c9` integriert; nicht auf `master` veröffentlicht. Details im abgeschlossenen Plan `2026-10-13-grad-bildmenge.md`.


## Methoden-, Tupel- und Formelvertrag – implementierter Teilstand 2026-10-13

- Neue kanonische Methodenaufrufe liefern Tupelergebnis plus offene Voraussetzungen. Explizite Bindungs-IDs, gespeicherte Komponentenidentitäten, reduzierte partielle Methoden und mehrdimensionale Tupelkomposition sind ergänzt. Aufrufbedingungen bleiben über verschachtelte Vorschriften, Voraussetzungen und Methodenkopien erhalten.
- Formelgraph überträgt Bedingungen und optionalen Bindungskontext. Strukturcodec v3 verwendet typisierte Literale; Strukturformel-, Formelkontext-, Wert- und Wertpaketcodec akzeptieren ausschließlich ihre aktuelle Wire-Version und weisen ältere Payloads ab. Der Zwischenablagevertrag für übertragene Werte lautet `ATLAS-WERT-2`. Kontextcodec bewahrt Quellen, vollständige Typen, Methodensignaturen und Annahmen. Deklarierte Methodenvariablen lassen sich im Grundparser aufrufen. Nicht registrierte Wertvarianten werden ausdrücklich abgewiesen.
- Tupelkonkatenation unterstützt null/einen Operanden und leere Ergänzung. Mehrkomponentige Kartengrenzen erhalten gespeicherte Reihenfolgen. Neue Kernoperatoren ergänzen rational-affine Inversion, partielle Ableitung, Polynomintegration und begrenzte numerische Jacobi-Matrizen, lokale Inversion und bestimmte Quadratur; numerische Ergebnisse sind Approximationen.
- Finaler damaliger Gesamtprüflauf `./gradlew test :app:assembleDebug :app:lintDebug :desktopApp:compileKotlin` erfolgreich; 1689 Tests laut XML-Berichten ohne Fehler oder Überspringungen. Repository-, Releaseplan-, Versionsfolge- und Diffprüfung erfolgreich. Separates Kernskript wegen fehlendem standalone kotlinc nicht ausführbar. Keine UI-/Gerätelaufzeitabnahme. Anschließend in Branchcommit `8465f6c9` integriert; nicht auf `master` veröffentlicht.
- **Der Gesamtvertrag ist nicht abgeschlossen.** Die gemeinsame UI-Integration ist nachfolgend dokumentiert; alle Wertvarianten, sämtliche Binder/Operatorfamilien und die vollständige Ablösung der Legacy-Pfade bleiben offen. Einzelmatrix und Nachweise: [aktiver ExecPlan](plans/active/2026-10-13-methoden-tupel-formelvertrag.md); Architekturentscheidung: [Bindungen und Codecs](decisions/2026-10-13-methodenbindungen-und-formelcodecs.md).

## v3.2.1 Releasekandidat (Arbeitsstand 2026-10-14)

- Reservierter Änderungsumfang auf Basis v3.2.0 (`dc072189`); es kommen keine neuen registrierten Knotentypen hinzu. Der Gesamtvertrag mit 77 Prüfpunkten bleibt Folgearbeit.
- Formel-/Methoden-/Tupelteilumfang enthält deklarationsvorrangige Methodenaufrufe, verschachtelte methodenwertige Ergebnisse, nullstellige und mehrwertige Bildmengen sowie aktuelle-only Mathematikcodecs ohne Rückwärtskompatibilität.
- Android-Metadaten und Roadmap auf 3.2.1/3002001 gestellt. Lokale Repositoryprüfungen und vollständiger Gradle-Lauf sind nach den letzten Änderungen erfolgreich; PR-CI, Android-Emulator-/Desktop-Abnahme und PR-Freigabe stehen noch aus.


### Übersetzungszeugnisse (Arbeitsstand 13.10.2026)

- Gemeinsamer Kernvertrag `ÜbersetzungsAnfrage`/`ÜbersetzungsErgebnis`/`ÜbersetzungsZeugnis`: unabhängige Abbildungseigenschaften, Informationserhaltung und Rechengenauigkeit; strukturierte Nichtdefiniertheit und konservative offene Spezialfälle.
- Alle 14 bestehenden Übersetzerziele, Abflachen, frühe Index-/Definitionsbausteinrückgaben und historische Struktur-/Termübersetzer werden zentral bezeugt. Zielträger und symbolischer exakter Bildraum bleiben getrennt. Heterogene Komponentenrestriktionen und stabile Quellen bleiben erhalten; Abflachungspläne rekonstruieren auch leere Untertupel ausdrücklich.
- Formel-Literale, Graphkonvertierung, mathematische Methoden und selbsttragende Exportwerte führen Zeugnisse/Quellen/Bedingungen mit. Nachfolgende Berechnungen unterscheiden aktuelle Zeugnisse von Herkunft. Unbekannte Regelversionen bleiben mit ungeprüften Behauptungen erhalten und gelten nicht als bestätigte Nachweise.
- Gemeinsames Auswertungsschema zeigt die vollständigen Zeugnisdetails; Desktop-Compose-Prüfung umfasst schmale/niedrige Fenster, helle/dunkle Darstellung und 1x/2x Schrift. Android-Gerätetest ist ergänzt und kompiliert, aber mangels verbundenem Gerät und AVD noch nicht ausgeführt.
- Gesamttests, Android-Debug-Build, Test-APK, Lint und Desktop-Kompilierung wurden erfolgreich ausgeführt. Repository-, Releaseplan- und Versionsfolgeprüfungen sind grün. Die zusätzliche Standalone-Kernprüfung ist wegen fehlendem `kotlinc` nicht ausführbar; Gradle-Kerntests sind grün.
- Unabhängige statische Prüfung und gezielte Nachprüfung durch `node_verifier`: gefundene Rückführungs-, Bildraumpfad-, Literalbedingungs- und Quellenfehler korrigiert; keine weiteren Blocker in der gezielten Nachprüfung. Dies ist keine Freigabe sämtlicher 77 Punkte des übergeordneten Vertrags.
- Nachweisübersicht: [ExecPlan Übersetzungszeugnisse](plans/active/2026-10-13-uebersetzungszeugnisse.md), [ADR](decisions/2026-10-13-uebersetzungszeugnisse.md). Kein neuer Knotentyp, keine Produktionsabhängigkeit oder Veröffentlichung.


### Gemeinsame UI-Vertragsintegration (Arbeitsstand 13.10.2026)

- Android und Desktop verwenden denselben gebundenen Formelbauer mit Symbolrollen, vollständigen Typen, Methodensignaturen, strukturierten Bereichen und Voraussetzungen. Noch nicht übernommene Symbol-/Komponentennamen und Typparameter sperren die globale Übernahme. Fehlerhafte Imports und beschädigte gespeicherte Kontexte überschreiben den Ausgangszustand nicht.
- Stabile fachliche Anschlusszuordnungen überleben Kopieren, Laden, Umbenennen und Reihenfolgeänderungen. Formel↔Karte und Methodenbildung verwenden vorhandene Knoten und atomare Kartenaktionen. Partielle Methodenbindung verwendet Komponentenidentitäten. Eigene und angeschlossene freie Quellen einschließlich Annahmen bleiben im aktuellen Ergebnissnapshot erhalten.
- Gemeinsame Ergebnisdetails zeigen Struktur, Signatur, Voraussetzungen, Bereiche, aktuelles Zeugnis und getrennte Herkunft. Selbsttragende Formel-/Wertpakete sind über Zwischenablage austauschbar; Wertcodec v2 ergänzt einen optionalen Formelkontext; alle Wertpaketversionen außer der aktuellen werden abgewiesen. Historische Werte mit nicht rekonstruierbaren Bindungen bleiben als Wert exportierbar, ihr Formeleditorimport wird diagnostiziert. Dateiwege verwenden weiterhin vollständige Karten.
- Rückführung ist ein Modus des bestehenden Übersetzers; frühere Indexparameter beeinflussen andere aktive Übersetzerziele nicht. Visualisierung erlaubt explizite Koordinaten und feste Parameter; verschachtelte Tupel benötigen eine ausdrücklich gewählte Blattprojektion.
- Q mit deklarierter Methodenquelle durchläuft Formel, Karte, Kartencodec, Methodenerzeugung und Anwendung: f(t)=t²,x=3,h=2 ergibt 8, h=0 wird zurückgewiesen. Eine allgemeine automatische symbolische Quotientenkürzung zu 2x+h ist keine neu implementierte Fähigkeit. Nicht unterstützte Binder und Graphknoten liefern Diagnosen.
- Desktop-Compose-Bedienungstests bestehen. Entsprechende Android-Gerätetests sind ergänzt; Android-Gerät/AVD ist nicht verfügbar, daher keine Android-Laufzeitfreigabe. Unabhängige statische Prüfung durch node_verifier meldet nach Korrektur der belegten Identitäts-/Metadatenfehler keine weiteren konkreten Blocker. Keine Gesamtfreigabe aller 77 Vertragsprüfpunkte.
- Nachweise und Grenzen: [UI-ExecPlan](plans/active/2026-10-13-ui-vertragsintegration.md), [ADR](decisions/2026-10-13-ui-formelkontext.md). Kein neuer Knotentyp, keine Produktionsabhängigkeit, kein Commit oder Release.

- Finaler UI-Gesamtprüflauf: 1721 Tests ohne Fehler/Überspringungen; Android-Debug-APK, Test-APK, Lint und Desktop-Kompilierung erfolgreich. Repository-, Releaseplan-, Versionsfolge- und Diffprüfungen grün. Zusätzliche Standalone-Kernprüfung mangels kotlinc nicht ausführbar; Gradle-Kerntests grün.
