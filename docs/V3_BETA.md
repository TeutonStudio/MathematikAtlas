# Mathematik Atlas – v3 Beta

## Zweck

v3 Beta baut auf dem Alpha-Stand auf und schließt die noch offenen
Semantik-, Plattform- und Laufzeitverträge vor einer stabilen v3-Version. Diese
Datei beschreibt die geplante Reihenfolge; sie behauptet keine bereits
implementierten Funktionen.

## Geplante Änderungsschritte

### 1. Inhaltsgleichheit und Selbigkeit

- V3-004 aus `V3_AENDERUNGEN.md` als zentralen Vergleichsdienst umsetzen.
- Inhaltsgleichheit typübergreifend nur für ausdrücklich definierte Familien
  erlauben.
- Selbigkeit rekursiv typ-, orientierungs- und formtreu modellieren.
- Gleichheit, Ungleichheit, endliche Mengen und Definitionskarten gemeinsam
  migrieren und das Gesetz `selbig(a, b) => gleich(a, b)` testen.

### 2. Aussage-Standardwerte

- V3-005 mit persistierten Wahr/Falsch-Standardwerten für unverbundene
  Aussageeingänge umsetzen.
- Priorität Verbindung vor Standardwert vor sicherem Fallback gewährleisten.
- Inspector, dynamische Anschlüsse, Kopieren, Migration und Undo/Redo abdecken.

### 3. Einheitliche Adaptergeometrie

- V3-006 für Typ-, Orientierungs- und Repräsentationsadapter abschließen.
- Die fachneutrale Formklassifikation im Karteneditor halten und mathematische
  Zuordnung im Mathematikmodul registrieren.
- Trefferflächen, Auswahlrahmen, Anschlusspositionen und MiniMap prüfen.

### 4. Große Graphen vollständig entkoppeln

- Viewportzustand und persistierbaren Karteninhalt vollständig trennen.
- Räumlichen Index, stabile Weltgeometrie- und Anschlusscaches sowie
  MiniMap-Inhaltsgrenzen an Inhaltsrevisionen koppeln.
- Pan und Zoom ohne mathematische Auswertung oder neue fachliche Kartenversion
  ausführen.
- Eine reproduzierbare Karte mit mindestens 1.000 Knoten und 1.500 Verbindungen
  auf einem festgelegten Android-Referenzgerät messen.

### 5. Visualisierungs- und Renderpipeline optimieren

Die mathematische Visualisierung soll sich bei Pan, Zoom und R³-Rotation ähnlich
wie spezialisierte Plotter verhalten: Fachliche Berechnung erzeugt eine stabile
Geometrie, während Kamerabewegungen möglichst nur noch die Darstellung dieser
Geometrie verändern. Die bereits vorhandene adaptive Abtastung,
Hintergrundausführung auf `Dispatchers.Default`, Abbruchunterstützung und das
harte Auswertungsbudget bleiben Grundlage und werden nicht durch einen zweiten
Sampler ersetzt.

#### 5.1 Berechnung, Rendergeometrie und Kamera strikt trennen

- Die Pipeline ausdrücklich in
  `Mathematische Definition -> Sampling -> Rendergeometrie -> Darstellung`
  aufteilen.
- Eine reine Kameraänderung darf keine mathematische Normalisierung, keine
  Methodenabtastung und keine neue fachliche Geometrie erzeugen.
- Änderungen des mathematischen Inhalts, der Achsenbereiche oder der
  Samplingqualität dürfen die fachliche Geometrie invalidieren.
- Kamera, Bildschirmgröße und andere reine Darstellungsparameter invalidieren
  dagegen nur die tatsächlich davon abhängigen Renderdaten.
- Der Karteneditor-Viewport aus Schritt 4 und die lokale Kamera eines
  Visualisierungsknotens bleiben getrennte Zustände mit demselben Grundprinzip:
  Ansicht ist kein fachlicher Inhalt.

#### 5.2 Sampling-Ergebnis in indizierte Rendergeometrie überführen

- Zwischen `VisualisierungsErgebnis` und Compose-Renderer eine
  Compose-freie Rendergeometrie einführen.
- Gemeinsame Stützpunkte nur einmal speichern und Linien beziehungsweise
  Dreiecke über stabile Indizes referenzieren.
- Für den Renderpfad kompakte Strukturen wie Vertex-, Linien- und
  Dreiecksindizes bevorzugen; bei messbarem Vorteil primitive Arrays statt
  großer Objektgraphen verwenden.
- Bereits im adaptiven Sampler gecachte Stützpunkte möglichst direkt mit einer
  stabilen Vertex-ID verbinden, statt gemeinsame Punkte anschließend per
  `flatMap`, `distinct` oder Hash-Mengen erneut zu entdecken.
- Exakte Punkte, Linien, Dreiecke, Intervalle und beweisbewusste Zellen müssen
  ihre bestehende mathematische Bedeutung und Diagnoseinformation behalten.

#### 5.3 Allokationen aus der Draw-Phase entfernen

Der aktuelle Renderer erzeugt beim Zeichnen unter anderem temporäre Listen,
Paare, `Triple`-Objekte, Hash-Mengen und einzelne `Path`-Objekte. Diese Arbeit
soll aus dem Frame-Pfad verschwinden.

- Aus dem Canvas-Zeichenblock keine vollständigen
  `flatMap`-/`distinct`-/`toHashSet`-Pipelines mehr aufbauen.
- Linien nicht pro Frame über `zipWithNext()` in neue Zwischenobjekte
  zerlegen.
- Unveränderte Pfade, Zellgeometrien, Vertexzuordnungen und andere
  kameraunabhängige Daten cachen.
- Projektionen gemeinsamer R³-Vertices pro Frame höchstens einmal berechnen und
  von allen referenzierenden Dreiecken wiederverwenden.
- Per-Frame-Allokationen mit Android Studio Profiler beziehungsweise
  vergleichbarer JVM-/Desktop-Messung erfassen und als eigene
  Regressionsgröße behandeln.

#### 5.4 R¹/R²/C über stabile Weltgeometrie transformieren

- Zweidimensionale Geometrie möglichst in Weltkoordinaten stabil halten.
- Pan und Zoom bevorzugt über eine gemeinsame Draw- beziehungsweise
  `graphicsLayer`-Transformation anwenden, statt jeden Weltpunkt bei jedem
  Frame auf Anwendungsebene neu zu materialisieren.
- `drawWithCache` beziehungsweise gleichwertige Compose-Caches für
  unveränderte Pfade und Zeichenobjekte prüfen.
- Achsenbeschriftungen, zoomunabhängige Griffgrößen und andere
  bildschirmabhängige Elemente dürfen als getrennte Overlay-Schicht verbleiben.
- Die Optimierung nur übernehmen, wenn Frame-Timing und CPU-Messung einen
  messbaren Vorteil gegenüber der bestehenden Projektion zeigen.

#### 5.5 R³ auf gebatchte Mesh-Darstellung vorbereiten

- R³-Dreiecke nicht langfristig als viele unabhängige
  `Path`-Zeichenoperationen behandeln.
- Einen Prototypen auf Basis der indizierten Rendergeometrie und einer
  gebatchten Dreiecksausgabe, beispielsweise Compose-`Vertices` /
  `drawVertices`, erstellen.
- Tiefensortierung nur neu berechnen, wenn Blickrichtung oder tatsächlich
  relevante Geometrie geändert wurden.
- Bei gemeinsamem Vertexbestand Projektion, Farbwert und weitere
  vertexbezogene Eigenschaften wiederverwenden.
- Transparenz, Zellenstatus, beweisbewusste Färbung und bestehende
  Diagnosekonventionen dürfen durch einen schnelleren Renderer nicht
  semantisch verändert werden.
- Eine spätere stärker GPU-nahe oder plattformspezifische 3D-Ausgabe bleibt
  möglich, darf aber nicht in den mathematischen Sampler oder Rechenkern
  einsickern.

#### 5.6 Interaktives LOD und progressive Verfeinerung

Wie bei spezialisierten Mathematikplottern soll Interaktivität Vorrang vor
unnötiger Detailtreue während einer laufenden Geste erhalten.

- Für Pan, Zoom und R³-Rotation einen Interaktionsmodus mit begrenzter
  Darstellungsarbeit vorsehen.
- Während der Geste darf ein bereits vorhandenes gröberes Mesh beziehungsweise
  eine reduzierte Darstellungsstufe verwendet werden.
- Nach Ende der Geste kann die feinere Darstellung wieder aktiviert oder im
  Hintergrund verfeinert werden.
- Kleine Kameraänderungen dürfen kein Resampling auslösen.
- Bildschirmabhängige Detailstufen über Pixelabweichungen beziehungsweise
  Fehlerschwellen ergänzen, ohne die persistierte mathematische Basisgeometrie
  bei jeder Zoomstufe zu verwerfen.
- Mindestens eine Basisstufe, eine normale Qualitätsstufe und optional eine
  Detailstufe bei starkem Zoom vorsehen, sofern Messungen den zusätzlichen
  Cacheaufwand rechtfertigen.
- Ein ausgeschöpftes Rechen- oder Renderbudget muss weiterhin sichtbar
  diagnostiziert werden, statt die Oberfläche bis zur vollständigen
  Berechnung zu blockieren.

#### 5.7 Numerischen Visualisierungsplan für häufige Methodenauswertung prüfen

Die allgemeine mathematische Objektsemantik bleibt verbindlich. Für tausende
numerische Stützstellen soll jedoch ein vorbereiteter Ausführungspfad möglich
werden.

- Für geeignete Methoden einen optionalen
  `NumerischerVisualisierungsPlan` oder gleichwertigen internen Vertrag
  untersuchen.
- Konstante Unterausdrücke und unveränderliche Teilberechnungen vor dem Sampling
  vorberechnen.
- Argumente in einem schlanken numerischen Speicherlayout verarbeiten, statt
  für jeden Stützpunkt erneut allgemeine `Map`-/`List`-Umgebungen aufzubauen.
- Der Plan ist ausschließlich eine optimierte Ausführungsrepräsentation; er
  ersetzt weder `Methode`, `MathematischesObjekt` noch den allgemeinen
  Rechenkern.
- Nicht unterstützte Ausdrücke müssen jederzeit auf die bestehende allgemeine
  Auswertung zurückfallen können.
- Erst nach Benchmarks entscheiden, welche Operatoren und Ausdrucksfamilien
  tatsächlich von diesem Pfad profitieren.

#### 5.8 Messung, Budgets und Abnahmekriterien

- Reproduzierbare Visualisierungs-Benchmarks für mindestens eine lange
  R²-Kurve, eine stark gekrümmte parametrische Kurve, eine implizite Region,
  eine dichte R³-Fläche und eine R³-Zell-/Volumendarstellung anlegen.
- Samplingzeit, Erzeugung der Rendergeometrie, Draw-CPU-Zeit,
  Frame-P95/P99, Allokationen pro Frame und Speicherbedarf getrennt messen.
- Für Android ein festgelegtes Referenzgerät oder einen festgelegten Emulator
  verwenden; Desktop dient zusätzlich zur reproduzierbaren JVM-Messung.
- Während reiner Kameraänderungen darf die Anzahl mathematischer
  Sampleauswertungen unverändert bleiben.
- Bei unveränderter Geometrie dürfen keine vollständigen Vertex-, Linien- oder
  Dreieckslisten pro Frame neu aufgebaut werden.
- R²-Pan/Zoom und R³-Rotation müssen gegenüber dem Ausgangsstand eine deutliche
  Reduktion von Jank, CPU-Arbeit und kurzlebigen Allokationen zeigen.
- Die mathematische Darstellung darf durch Performance-LOD keine falschen
  Verbindungen über Singularitäten, keine falschen Flächenfüllungen und keine
  verlorenen Nachweiszustände erzeugen.
- Die Implementierung wird stufenweise vorgenommen: zuerst indizierte
  Rendergeometrie und allokationsarme Draw-Phase, danach
  Transformations-/Mesh-Prototypen, anschließend LOD und erst zuletzt der
  optionale numerische Ausführungsplan.

### 6. Abstraktionsregeln erweitern

- Bereits methodengehobene Rechnergraphen unter vollständigem Vergleich von
  Signatur, Argumentreihenfolge, Wertevorrat, Zielmenge und Ausgangsprojektion
  unterstützen.
- Weitere sichere Regeln für Potenzen, Skalar- und Matrixprodukte,
  Tupelkonstruktionen und wiederkehrende Teilgraphen ergänzen.
- Überlappende Vorschläge weiter einzeln anwenden; eine globale Optimierung erst
  nach einem nachgewiesenen konfliktfreien Auswahlverfahren anbieten.

### 7. Zustands- und I/O-Härtung

- Persistierenden Import- und Speicheranteil vollständig aus dem UI-Thread
  verlagern.
- Navigation und aktive Karte über einen lifecycle-festen Zustandsbesitzer mit
  `SavedState` wiederherstellen.
- Beschädigte Versionen und vorhandene Sicherungen in der Oberfläche sichtbar
  diagnostizieren und kontrolliert wiederherstellen.

### 8. Plattformmodernisierung

- Issue #395 erst umsetzen, wenn Kotlin und Android Gradle Plugin einen
  offiziell unterstützten gemeinsamen KMP-Pfad besitzen.
- Dann Desktop-Shadowmodule durch echte gemeinsame Quellsets ersetzen und die
  Whitelist schrittweise auf null reduzieren.
- Kein verstecktes AGP-Downgrade und keine nicht unterstützte
  Kotlin-/AGP-Kombination verwenden.

### 9. Strukturierte SVG- und TeX-Erzeugung

v3 Beta soll SVG-Grafiken und TeX-Dokumente als reguläre, typisierte
Atlas-Ergebnisse erzeugen können. Beide Ausgabewege folgen demselben Grundsatz:
Knoten bauen strukturierte Laufzeitwerte auf; ein Übersetzer projiziert
bestehende Atlas-Werte in das Zielformat und ein Serializer erzeugt erst am Ende
den eigentlichen Quelltext. Bestehende mathematische Knoten werden dafür nicht
auf SVG- oder TeX-Strings umgestellt.

#### 9.1 Gemeinsamer Architekturvertrag

- Mathematische Knoten behalten ihre bisherigen fachlichen Ausgänge wie Zahl,
  Tupel, Vektor, Matrix, Menge, Aussage oder Methode.
- SVG und TeX erhalten eigene strukturierte AST- beziehungsweise Dokumentwerte.
- Übersetzer arbeiten auf ausgewerteten Atlas-Werten und nicht auf
  `KnotenDaten`, damit dieselben mathematischen Ergebnisse unabhängig von ihrer
  Herkunft in verschiedene Ausgabeformate projiziert werden können.
- Übersetzer und Serializer bleiben getrennte Schichten:
  `Atlas-Wert -> Übersetzer -> strukturierter Zielwert -> Serializer -> Quelltext`.
- Diese Trennung soll später auch für TikZ, Mermaid, HTML und weitere
  v3-Ausgabeformate wiederverwendbar sein.

#### 9.2 Bestehendes SVG-Fundament weiterverwenden

- Den vorhandenen unveränderlichen `SvgGrafik`-AST, `SvgSerializer`, die
  Anschlussarten `grafik`, `grafik.svg`, `grafik.svg.stil` sowie
  `grafik.svg.knoten` und `grafik.svg.stil` nicht durch parallele
  Ersatzsysteme duplizieren.
- Der vorhandene SVG-Knoten bleibt der allgemeine SVG-OperatorKnoten; die
  konkrete Operation wird weiterhin über eine stabile Operator-ID gewählt.
- Bestehende Operator-IDs und Anschlussrollen bleiben stabil, damit vorhandene
  Karten ohne unnötige Migration oder verlorene Verbindungen weiter funktionieren.
- Beim Operatorwechsel werden bestehende Anschluss-IDs für gleichbleibende
  Rollen weiterverwendet.
- Den `OperatorKnotenSuchindex` um SVG-Operatoren erweitern, sodass Suchen wie
  „Kreis“, „Pfad“ oder „Funktionsgraph“ direkt einen passend vorkonfigurierten
  SVG-Knoten liefern.

#### 9.3 SVG-Operatoren und mathematische Übersetzung vervollständigen

- Die vorhandenen Operatoren für Dokument, Linie, Rechteck, Kreis, Ellipse,
  Polygon, Linienzug, Pfad, Text, Gruppierung und Kombination beibehalten.
- Höherwertige Operatoren für mathematische Grafiken ergänzen, insbesondere:
  Punkt, Punktmenge, Vektor/Pfeil, Achsen, Koordinatensystem, Raster,
  Funktionsgraph, parametrische Kurve, Mengen-/Gebietsdarstellung,
  mathematische Beschriftung, Verschieben, Skalieren, Drehen,
  Matrixtransformation, Clipping sowie Symboldefinition und Symbolverwendung.
- Vorhandene Visualisierungs-, Sampling- und Mengenlogik wiederverwenden statt
  für SVG eine zweite mathematische Auswertungslogik einzuführen.
- Einen zentralen SVG-Übersetzer beziehungsweise eine Registry von
  Übersetzungsregeln vorsehen, zum Beispiel für Tupel als Punkte, Vektoren als
  Pfeile, Methoden als Funktionsgraphen und Mengen als visualisierte Gebiete.
- Einen allgemeinen Fallback für geeignete mathematische Objekte als
  mathematische Beschriftung vorsehen, ohne die ursprünglichen mathematischen
  Werte zu verändern.

#### 9.4 Mathematischen Text in SVG tatsächlich renderbar machen

- Das bestehende `SvgText.mathematikLatex`-Metadatum nicht als vollständige
  Darstellungslösung behandeln.
- Mathematische Beschriftungen müssen in ein in einer normalen SVG-Datei
  tatsächlich darstellbares Ergebnis übersetzt werden, beispielsweise durch
  strukturierte SVG-Geometrie beziehungsweise Glyphen/Pfade.
- Der ursprüngliche LaTeX-Ausdruck darf zusätzlich als Metadatum erhalten
  bleiben, soll aber nicht die einzige Darstellung des mathematischen Inhalts
  sein.

#### 9.5 Strukturierte TeX-Dokumentdomäne einführen

- TeX nicht als beliebigen String modellieren und
  `MathematischesObjekt.zuLatex()` nicht zum vollständigen Dokumentexport
  umdefinieren.
- Einen strukturierten `TexDokument`-Wert mit Dokumentklasse, Optionen,
  Paketen, Metadaten, Inhalt und Assets einführen.
- Dokumentinhalt als strukturierte Blöcke modellieren, mindestens für
  Abschnitt, Unterabschnitt, Absatz, Mathematikblock, Liste, Tabelle,
  Abbildung, Ausrichtung und Seitenumbruch.
- Inline-Inhalte getrennt modellieren, mindestens für Text, Mathematik,
  Hervorhebungen, Referenzen und Zeilenumbrüche.
- Bei Bedarf `TexFragment` als eigenständigen Zwischenwert vorsehen, damit
  mehrere Knoten Dokumentteile aufbauen können, ohne jeweils ein vollständiges
  Dokument erzeugen zu müssen.
- Passende stabile Typ- und Anschlussarten für Dokument, TeX-Dokument,
  TeX-Fragment und gegebenenfalls wiederverwendbare TeX-Stile ergänzen.

#### 9.6 Einheitlichen TeX-OperatorKnoten ergänzen

- Einen regulären TeX-Knotentyp nach dem bestehenden Rechner- und SVG-Prinzip
  einführen; einzelne TeX-Operationen werden nicht zu jeweils eigenen
  Knotentypen.
- Die konkrete Operation über eine stabile `texOperator`-ID speichern.
- Erste Operatoren mindestens für Dokument, Titel, Autor, Abschnitt,
  Unterabschnitt, Absatz, Text, Inline-Mathematik, Mathematikblock, Gleichung,
  ausgerichtete Gleichungen, Aufzählung, nummerierte Liste, Tabelle, Abbildung,
  SVG-Abbildung, Seitenumbruch und Kombination bereitstellen.
- Für Definition, Registry, Konfiguration, Auswerter und Inspector dieselben
  stabilen Muster wie bei `SvgOperatoren`, `konfiguriereSvgKnoten` und dem
  SVG-Inspector verwenden.
- Den Operator-Suchindex um TeX-Operatoren erweitern, sodass beispielsweise
  „Abschnitt“, „Tabelle“ oder „Gleichung“ direkt einen vorkonfigurierten
  TeX-Knoten erzeugen.

#### 9.7 TeX-Übersetzer für vorhandene mathematische Werte

- Vorhandene mathematische Objekte wie Zahl, Matrix, Vektor, Menge, Aussage,
  Methode, Tensor und Geometrie ohne Änderung ihrer Erzeugerknoten als
  TeX-Mathematikfragmente verwenden können.
- Für mathematische Ausdrücke die bestehende LaTeX-Projektion des Rechenkerns
  wiederverwenden.
- `FormelLatexCodec` und den neuen vollständigen Dokumentserializer getrennt
  halten: der Formelcodec bleibt für den kontrollierten CAS-Formel-Roundtrip
  zuständig, während `TexSerializer` vollständige strukturierte Dokumente
  serialisiert.

#### 9.8 SVG als TeX-Asset und formatübergreifende Komposition

- `TexDokument` soll strukturierte Assets referenzieren können, insbesondere
  eine bereits ausgewertete `SvgGrafik`.
- Eine TeX-Abbildung darf daher direkt aus einem SVG-Ausgang gespeist werden,
  ohne SVG zunächst in einen untypisierten Dateipfad oder Rohstring umzuwandeln.
- Der Export eines TeX-Dokuments darf daraus ein Projekt mit Hauptdatei und
  Assets erzeugen, beispielsweise `dokument.tex` plus
  `assets/graph-1.svg`.
- Dateinamen und Referenzen deterministisch und kollisionsfrei erzeugen.

#### 9.9 Export und Vorschau als letzte Schicht

- Graphauswertung erzeugt zunächst `SvgGrafik`, `TexFragment` oder
  `TexDokument`, nicht unmittelbar Dateien.
- Datei- und Projekt-Export bleibt eine getrennte App-/I/O-Schicht.
- SVG wird über den vorhandenen beziehungsweise erweiterten `SvgSerializer`
  exportiert.
- TeX wird über einen neuen deterministischen `TexSerializer` exportiert.
- Eine spätere TeX-zu-PDF-Kompilierung bleibt optional und darf nicht zur
  Voraussetzung der TeX-Knoten- oder Dokumentsemantik werden.
- Inspector und Vorschau zeigen strukturierte Zwischenergebnisse, ohne
  persistierten Laufzeitzustand in der Karte abzulegen.

#### 9.10 Kompatibilitäts- und Testanforderungen

- Keine bestehenden mathematischen Knotenarten oder Anschlussarten allein für
  SVG/TeX umbenennen oder ersetzen.
- `grafik.svg`, `grafik.svg.knoten`, `grafik.svg.stil` und bestehende
  SVG-Operator-IDs kompatibel halten.
- Alte Karten ohne neue V3-Dokumentwerte sollen im Idealfall keine Migration
  benötigen.
- Neue Knoten und Typen in den kanonischen Knotenkatalog, das
  Auswerterregister, die Anschlussartregistrierung, die Typzuordnung, die
  Inspector-Zuordnung und den Operator-Suchindex integrieren.
- Tests für AST-Unveränderlichkeit, deterministische Serialisierung, Escaping,
  Operatorwechsel mit stabilen Anschluss-IDs, Übersetzungsregeln,
  formatübergreifende SVG-zu-TeX-Komposition und Laden bestehender Karten
  ergänzen.
- Mindestens einen End-to-End-Test abdecken, der aus bestehenden mathematischen
  Knoten sowohl eine SVG-Grafik als auch ein TeX-Dokument erzeugt, ohne die
  ursprünglichen mathematischen Knoten zu verändern.

#### 9.11 Empfohlene Implementierungsreihenfolge

1. gemeinsames Ausgabe- und Übersetzerfundament sowie strukturierte TeX-Typen,
2. vorhandenes SVG-System um Übersetzer und mathematische Operatoren erweitern,
3. TeX-Knotenfamilie, TeX-Serializer und Inspector implementieren,
4. SVG-Assets in TeX, Exportprojekt und formatübergreifende Vorschau ergänzen.

### 10. Beta-Abnahme

- vollständige Repository-, Release-, Migrations-, JVM-, Desktop-, Lint- und
  APK-Prüfungen ausführen,
- Android-Instrumentierungs- und Geräteprüfungen für Touch, Lebenszyklus,
  Import und Abstraktionsdialog ergänzen,
- SVG-/TeX-Erzeugung, Serialisierung, Export und formatübergreifende
  Komposition in den Beta-Abnahmetests berücksichtigen,
- verbleibende Deprecation-Warnungen nach Risiko priorisieren,
- erst danach einen formellen v3-Release im Releaseplan reservieren.
