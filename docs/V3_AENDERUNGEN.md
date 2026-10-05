# v3 – Änderungsdokumentation

Diese Datei ist die fortlaufende Anforderungsliste für den Versionsraum **v3.y.x** des Mathematik Atlas. Sie konkretisiert die langfristige `ROADMAP.md`, ohne bereits eine bestimmte Unterversion oder einen Implementierungsbranch vorwegzunehmen.

Neue für v3 beschlossene Änderungen werden hier ergänzt, bevor sie in konkrete Releases und Implementierungspläne zerlegt werden.

## Übergeordneter v3-Rahmen

Der in der Roadmap festgelegte Schwerpunkt von v3 bleibt **Grafik, Auszeichnung und Dokumente** mit strukturierten Inhalten und Erzeugungspfaden für unter anderem SVG, TikZ, LaTeX, Mermaid und HTML.

Zusätzlich werden hier notwendige produktweite Änderungen festgehalten, die für die v3-Nutzung verbindlich sein sollen.

## Änderungen

### V3-001 – Geöffnete Karte über den App-Lebenszyklus erhalten

**Status:** geplant

Die aktuell geöffnete Karte muss Teil des dauerhaft wiederherstellbaren App-Zustands sein. Ein Wechsel des Android-Lebenszyklus darf den Nutzer nicht aus der Karte werfen oder stillschweigend eine andere Karte öffnen.

#### Anforderungen

- Beim Öffnen oder Wechseln einer Karte wird ihre stabile Karten-ID als aktuell geöffnete Karte gespeichert.
- Eine Bildschirmrotation beziehungsweise andere Activity-/Configuration-Neuerstellung lässt dieselbe Karte geöffnet.
- Wird die App minimiert und anschließend wieder in den Vordergrund geholt, bleibt dieselbe Karte geöffnet.
- Wird der App-Prozess während des Hintergrundzustands von Android verworfen und später neu erstellt, wird die zuletzt geöffnete Karte erneut geöffnet, sofern sie weiterhin existiert.
- Die Wiederherstellung darf keine neue Kopie der Karte erzeugen und die Karte nicht erneut als inhaltliche Änderung speichern.
- Ist die gespeicherte Karte nicht mehr vorhanden oder nicht lesbar, muss die App kontrolliert auf die Kartenübersicht zurückfallen, statt abzustürzen.

#### Abgrenzung

Diese Anforderung betrifft zunächst die **Identität der geöffneten Karte**. Flüchtige UI-Zustände innerhalb der Karte, etwa offene Dialoge, aktuelle Auswahl, Drag-Zustände oder Inspector-Fokus, gelten nicht automatisch als mitgespeichert und werden bei Bedarf als eigene V3-Anforderungen dokumentiert.

#### Abnahmekriterien

1. Karte A öffnen, Gerät drehen: Karte A bleibt geöffnet.
2. Karte A öffnen, App minimieren, App erneut öffnen: Karte A bleibt geöffnet.
3. Karte A öffnen, App in den Hintergrund schicken, Prozess neu erzeugen lassen: Karte A wird anhand ihrer stabilen ID wieder geöffnet.
4. Die Wiederherstellung erzeugt weder eine zweite Karteninstanz noch einen zusätzlichen inhaltlichen Undo-/Speicherschritt.
5. Ist Karte A zwischenzeitlich nicht mehr verfügbar, erscheint die Kartenübersicht ohne Absturz.


### V3-002 – Flüssige Kartenansicht bei großen Graphen

**Status:** analysiert, Umsetzung geplant

Verschieben und Zoomen der Kartenansicht müssen auch bei großen Karten flüssig bleiben. Eine reine Kameratransformation darf weder mathematische Auswertung noch strukturelle Kartenbereinigung auslösen und darf nicht in jedem Pointer-Schritt die vollständige Karte nach sichtbaren Elementen durchsuchen.

#### Ist-Analyse

Der aktuelle Renderpfad koppelt die Kamera zu eng an den fachlichen Kartenbestand:

- `KartenDaten` enthält neben Knoten, Verbindungen und visuellen Gruppen auch `ansicht`. Jeder Pan- oder Zoom-Schritt erzeugt damit eine neue vollständige Karteninstanz.
- `KnotenKartenEditor` berechnet aus jeder neuen Ansicht den sichtbaren Weltbereich und filtert anschließend erneut die vollständige Knoten- und Verbindungsliste.
- Das Knotenculling ist damit mindestens linear in der Knotenzahl. Das Verbindungsculling ist deutlich teurer: Für die Endpunkte einer Verbindung wird derzeit jeweils per `firstOrNull` in der vollständigen Knotenliste gesucht und anschließend die Anschlussposition erneut aus der Anschlussliste bestimmt. Bei großen Karten kann das in Richtung `O(E × N)` wachsen.
- Jede Kamerabewegung wird über `KartenEditorZustand.führeAus(KartenAktion.AnsichtÄndern(...))` geleitet. Dieser allgemeine Aktionspfad entfernt vor und nach der Aktion unverbundene dynamische Eingänge, bereinigt visuelle Gruppen und bereinigt anschließend die Auswahl. Diese Arbeiten sind für eine reine Kameraänderung fachlich unnötig.
- In der Android-App hängt `LaunchedEffect(zustand.editor.karte)` an der vollständigen Karte. Deshalb startet bereits während des Pans oder Zooms nach jedem Kartenwechsel erneut `aktualisiereAuswertung()`. Die mathematische Auswertung und die Synchronisierung mehrerer Knotensysteme laufen somit auch bei einer rein visuellen Kamerabewegung an.
- Derselbe Effekt plant nach 650 ms außerdem `speichereAktuell()`. Eine Viewportänderung kann dadurch wie eine Kartenänderung behandelt und persistiert werden, obwohl sich kein mathematischer Inhalt geändert hat.
- Die sichtbaren Verbindungsgeometrien werden mit `ansicht` als Cache-Schlüssel erzeugt. Schon eine reine Translation verwirft damit alle sichtbaren Bézier-Geometrien.
- Die MiniMap ermittelt ihre Inhaltsgrenzen durch einen vollständigen Durchlauf über alle Knoten und zeichnet anschließend alle Knoten. Da sie die vollständige Karte erhält, wird auch dieser Pfad durch Viewportänderungen invalidiert.
- Die Knotendarstellung berechnet Position und Skalierung je Knoten aus der aktuellen Ansicht. Zusätzlich wird `rendererFür(knoten)` während der Komposition aufgerufen; mehrere Rendererpfade erzeugen dabei neue Renderer- beziehungsweise Wrapperinstanzen.

Die Vermutung, dass das Viewport-Culling während des Drags zu häufig ausgeführt wird, ist damit bestätigt. Es ist allerdings nur ein Teil der Ursache.

#### Zielarchitektur

Viewportzustand und Karteninhalt werden als zwei unterschiedlich schnell veränderliche Zustände behandelt:

1. **Karteninhalt** umfasst Knoten, Anschlüsse, Verbindungen, Gruppen und fachliche Konfigurationen. Änderungen daran dürfen Auswertung, Normalisierung, Caches und Persistenz invalidieren.
2. **Ansichtszustand** umfasst mindestens Verschiebung und Zoom. Während einer laufenden Geste muss dieser Zustand über einen leichten Renderpfad aktualisiert werden können, ohne fachliche Kartenarbeit auszulösen.

Die persistierbare Ansicht darf weiterhin gespeichert werden. Sie darf während einer Interaktion jedoch nicht als inhaltliche Kartenänderung durch sämtliche fachlichen Beobachter laufen. Eine Viewportänderung allein darf insbesondere keine neue fachliche Kartenversion erzeugen.

#### Culling während Pan und Zoom

Das Culling soll nicht einfach ausschließlich auf das Ende eines Drags verschoben werden. Ein Pan kann prinzipiell beliebig weit gehen; es existiert daher keine sinnvolle endliche „maximal mögliche Verschiebung“, die einmalig als Puffer angesetzt werden könnte.

Stattdessen wird ein **stabiles Renderfenster mit Overscan und Hysterese** verwendet:

- Beim erstmaligen Anzeigen, nach einer strukturellen Geometrieänderung oder wenn der bisherige Bereich verlassen wird, wird ein Renderfenster bestimmt, das deutlich größer als der sichtbare Viewport ist.
- Knoten und Verbindungen innerhalb dieses Renderfensters bilden eine stabile Renderauswahl.
- Kleine Pan- und Zoom-Schritte verändern nur die Kameratransformation. Die Renderauswahl bleibt unverändert.
- Um das sichtbare Fenster liegt eine Sicherheitszone. Erst wenn der Viewport diese Zone erreicht oder verlässt, wird ein neues Renderfenster ermittelt.
- Bei langen Drags darf deshalb gelegentlich neu gecullt werden, aber nicht bei jedem Pointer- beziehungsweise Frame-Ereignis.
- Für Zoom wird ebenfalls mit Hysterese beziehungsweise Zoomstufen gearbeitet. Ein minimaler Zoomunterschied darf nicht sofort die vollständige Sichtbarkeitsberechnung wiederholen.
- Am Ende der Geste wird das Renderfenster auf einen passenden stabilen Bereich normalisiert.

Damit werden neu in den Bildschirm kommende Elemente rechtzeitig bereitgestellt, ohne für jede wenige Pixel große Bewegung die gesamte Karte neu zu filtern.

#### Räumliche Indizes und Geometriecaches

Für große Karten genügt auch ein seltener vollständiger `O(N + E)`-Scan irgendwann nicht mehr. Deshalb soll der Renderpfad zusätzlich vorbereitete räumliche Daten verwenden:

- Ein räumlicher Index ordnet Knoten anhand ihrer Welt-Rechtecke räumlichen Zellen oder einer vergleichbaren Indexstruktur zu. Sichtbarkeitsabfragen betrachten nur Kandidaten aus überlappenden Bereichen.
- Knoten werden zusätzlich über `KnotenId` direkt indiziert, sodass Verbindungsendpunkte nicht für jede Edge erneut durch die vollständige Knotenliste gesucht werden.
- Anschlusspositionen beziehungsweise die daraus abgeleiteten Welt-Endpunkte von Verbindungen werden gecacht und nur invalidiert, wenn sich der betreffende Knoten, seine Größe oder seine Anschlüsse ändern.
- Die Welt-Geometrie und Umhüllung einer Verbindung werden unabhängig von der Kamera gecacht. Pan allein darf diese Geometrie nicht neu berechnen.
- Inhaltsgrenzen der MiniMap werden aus einem Cache beziehungsweise einer Inhaltsrevision bezogen. Pan und Zoom verändern nur das eingezeichnete Viewport-Rechteck.
- Rendererinstanzen sollen soweit möglich stabil wiederverwendet werden, statt bei Viewport-Rekompositionen für jeden sichtbaren Knoten neu erzeugt zu werden.

Ein einfacher räumlicher Gitterindex ist zunächst ausreichend und leichter inkrementell zu pflegen als eine komplexere Baumstruktur. Eine Quadtree-/R-Tree-ähnliche Struktur ist erst nötig, wenn Messungen einen tatsächlichen Vorteil zeigen.

#### Rendertransformation

Nach Entkopplung von Inhalt und Ansicht soll geprüft werden, ob die stabile Renderauswahl als gemeinsame Welt-Ebene transformiert werden kann.

Bevorzugt wird eine einzige GPU-nahe `graphicsLayer`-Transformation für Translation und Skalierung der Welt-Ebene, sodass ein Viewport-Frame nicht Position und Skalierung jedes Knotens und jeder Edge einzeln neu erzeugen muss. Weltpositionen und Edge-Geometrien bleiben dabei stabil.

Anschlüsse und andere Bedienelemente, deren sichtbare beziehungsweise anklickbare Bildschirmgröße absichtlich zoomunabhängig ist, benötigen dabei entweder eine inverse Skalierung oder eine getrennte Interaktionsebene. Diese Optimierung wird deshalb erst nach den funktional risikofreieren Entkopplungs- und Culling-Schritten umgesetzt und anhand von Messungen bewertet.

#### Umsetzungsreihenfolge

1. Viewportänderungen erhalten einen leichten Zustands-/Aktionspfad, der keine dynamischen Anschlüsse, Gruppen oder Auswahl bereinigt.
2. Fachliche Kartenänderungen und reine Ansichtszustandsänderungen erhalten getrennte Änderungs- beziehungsweise Revisionssignale. Mathematische Auswertung, Synchronisierung und andere inhaltsabhängige `LaunchedEffect`-/`remember`-Pfade dürfen nicht mehr auf reine Viewportänderungen reagieren.
3. Autosave und Kartenversionierung werden so getrennt, dass Viewportänderungen gespeichert werden können, ohne als neue fachliche Version behandelt zu werden.
4. Das Viewport-Culling erhält Renderfenster, Overscan und Hysterese.
5. Knoten-, Anschluss- und Edge-Lookups erhalten vorbereitete Indizes und Welt-Geometriecaches.
6. MiniMap-Inhaltsgrenzen und andere nur vom Karteninhalt abhängige Berechnungen werden an eine Inhaltsrevision statt an die vollständige Karte gebunden.
7. Rendererinstanzen werden stabilisiert.
8. Eine gemeinsame transformierte Welt-Ebene wird prototypisch gemessen und nur übernommen, wenn sie gegenüber der dann bereits optimierten Einzelplatzierung einen messbaren Vorteil besitzt.

#### Abnahmekriterien

1. Während ausschließlich verschoben oder gezoomt wird, wird keine mathematische Kartenauswertung gestartet.
2. `AnsichtÄndern` beziehungsweise der Nachfolger führt pro Pointer-Ereignis keine Bereinigung dynamischer Eingänge, keine Gruppenbereinigung und keine vollständige Auswahlbereinigung aus.
3. Ein normaler Pan über wenige Pixel führt nicht zu einem vollständigen erneuten Durchlauf über alle Knoten und Verbindungen.
4. Bei einem langen Pan erscheinen neu eintretende Knoten und Verbindungen rechtzeitig; es entstehen keine leeren Streifen am Rand. Beliebig lange Kartenbewegungen bleiben möglich.
5. Eine reine Translation invalidiert keine Welt-Geometrie unveränderter Verbindungen.
6. Die Inhaltsgrenzen der MiniMap werden bei reinem Pan oder Zoom nicht erneut über alle Knoten berechnet.
7. Eine Viewportänderung erzeugt keine neue fachliche Kartenversion.
8. Für die Umsetzung wird eine reproduzierbare große Testkarte mit mindestens 1.000 Knoten und 1.500 Verbindungen bereitgestellt. Pan und Zoom werden auf einem festgelegten Android-Referenzgerät beziehungsweise Emulator mit Frame-Timing gemessen; die Messung muss gegenüber dem Ausgangsstand eine deutliche Reduktion von Jank und CPU-Arbeit nachweisen.
9. Auswahl, Knoten-Drag, Größenänderung, Verbindungstreffer, Anschluss-Drag, visuelle Gruppen, MiniMap und Undo/Redo bleiben funktional unverändert.


### V3-003 – Einheitliche Innenabstände für Dialoge und Karten

**Status:** analysiert, Umsetzung geplant

Dialoge, Karten und vergleichbare hervorgehobene Inhaltsflächen müssen im gesamten Atlas konsistente Innenabstände verwenden. Texte, Formeln, Eingabefelder und Aktionszeilen dürfen nicht unmittelbar oder optisch zu dicht an Karten- beziehungsweise Dialogrändern liegen.

#### Ist-Analyse

Die Compose-Oberfläche verwendet aktuell kein gemeinsames Spacing- oder Padding-System. Abstände werden lokal mit zahlreichen Einzelwerten wie 10, 12, 14, 16, 20 oder 24 dp festgelegt.

Mehrere neuere Komponenten zeigen bereits das gewünschte Muster, beispielsweise:

- `OperatorKachel` im Rechner-Operator-Auswahldialog mit 12 dp Innenabstand,
- Detailflächen im selben Dialog mit 16 dp,
- Karten im Papierkorb mit 16 dp,
- Karten im Inspektor der endlichen Menge mit 10 beziehungsweise 12 dp,
- verschiedene Formel- und Strukturvorschauen mit 12 beziehungsweise 16 dp.

Andere Dialog- und Surface-Strukturen setzen ihre Abstände dagegen unabhängig voneinander zusammen. Insbesondere bei selbst aufgebauten `Dialog` + `Surface`-Oberflächen existiert kein zentraler Vertrag dafür, welcher Abstand zwischen Rahmen, Textinhalt, Listen, Eingabefeldern und Aktionsbereichen einzuhalten ist.

Material-`AlertDialog` besitzt bereits eigene Layoutabstände und soll nicht pauschal zusätzlich doppelt gepolstert werden. Die Korrektur betrifft vor allem selbst aufgebaute Dialogflächen sowie `Card`, `OutlinedCard` und als Karte verwendete `Surface`-Container.

#### Anforderungen

- Für den Atlas wird ein kleiner gemeinsamer Satz semantischer Abstände definiert, statt neue rohe dp-Werte in jedem Dialog einzeln zu verteilen.
- Mindestens folgende Rollen werden unterschieden:
  - kompakter Innenabstand für kleine Chips, Hinweise und sehr dichte Hilfsflächen,
  - Standard-Innenabstand für Cards und OutlinedCards mit Textinhalt,
  - größerer Inhaltsabstand für Hauptbereiche eigener Dialoge,
  - Abstand zwischen logisch getrennten Elementen innerhalb einer Karte,
  - Abstand zwischen Dialogrand und äußerem Dialoginhalt.
- Eine normale texttragende Card erhält standardmäßig einen wahrnehmbaren Innenabstand. Als Ausgangswert sind etwa 12 bis 16 dp angemessen; die konkrete Festlegung erfolgt einmal zentral.
- Überschrift, Fließtext, LaTeX-Inhalt und Eingabeelemente dürfen bei normalen Karten nicht ohne bewusste Full-Bleed-Ausnahme unmittelbar am Kartenrand beginnen.
- Selbst aufgebaute Dialoge mit `Dialog` und `Surface` erhalten eine konsistente Struktur aus Kopf, Inhaltsbereich und Aktionsbereich. Diese Bereiche verwenden gemeinsame horizontale Grundabstände.
- Scrollbare Inhalte müssen ihre Innenabstände auch am ersten und letzten Element behalten. Das Padding darf nicht dadurch verloren gehen, dass nur die äußere `LazyColumn` oder der Scrollcontainer dimensioniert wird.
- Karten innerhalb von `LazyColumn`, `LazyVerticalGrid` oder anderen Listen behalten ihren eigenen Innenabstand unabhängig vom Abstand zwischen den Listenelementen.
- Bestehende Material-Komponenten mit bereits korrektem internem Padding, insbesondere `AlertDialog`, `ListItem`, `Button`, `TextField` und ähnliche Komponenten, werden nicht zusätzlich blind doppelt gepolstert.
- Full-Bleed-Inhalte wie Canvas, Diagramme, Bildflächen, Editoren oder bewusst randfüllende Vorschauen dürfen weiterhin bis an den Container reichen. Text- oder Steuerelement-Overlays innerhalb solcher Flächen benötigen jedoch wieder einen eigenen sicheren Abstand.
- Die Knotenkarte selbst wird von dieser Anforderung nicht pauschal vergrößert. Knotendarstellungen besitzen eigene Größen- und Zoomanforderungen und werden separat beurteilt.

#### Technische Richtung

Die Abstände sollen über eine zentrale, fachneutrale UI-Konvention bereitgestellt werden, beispielsweise durch semantisch benannte Konstanten oder kleine Layout-Helfer. Entscheidend ist nicht der konkrete Name, sondern dass neue Dialoge und Karten nicht erneut freie Einzelwerte erfinden.

Mögliche Rollen sind beispielsweise:

- `Kompakt`
- `KartenInhalt`
- `DialogInhalt`
- `DialogAußen`
- `ElementAbstandKlein`
- `ElementAbstandNormal`

Dabei sollen keine unnötig komplexen Wrapper-Composables entstehen. Wo `Modifier.padding(...)` und `Arrangement.spacedBy(...)` mit zentralen Werten ausreichen, bleiben diese die bevorzugte Lösung.

#### Prüfbereich

Bei der Umsetzung werden mindestens folgende UI-Gruppen vollständig durchgesehen:

- Knotenauswahl und Konzeptbibliothek,
- Rechner- und Operatorauswahldialoge,
- Formel- und Strukturformelbauer,
- Einstellungen und Profilverwaltung,
- Kartenexport, Karten-JSON und Kartenverwaltung,
- Wahrheits- und Umformungstabellen,
- Inspektor-Unterdialoge und Bestätigungsdialoge,
- mathematische Konzeptdialoge,
- alle `Card`, `OutlinedCard` und als Informationskarte verwendeten `Surface`-Container im `app`-Modul.

Dabei werden bereits korrekt gepolsterte Komponenten nicht nur deshalb verändert, um einen Diff zu erzeugen. Ziel ist visuelle Konsistenz, nicht dekorative Codebewegung.

#### Abnahmekriterien

1. Kein normaler Textblock innerhalb einer Card oder cardartigen Surface liegt ohne bewusste Full-Bleed-Ausnahme unmittelbar am Rand.
2. Karten mit vergleichbarer Funktion verwenden denselben oder semantisch gleichwertigen Innenabstand.
3. Eigene große Dialoge besitzen konsistente horizontale Grundabstände zwischen Kopf, Inhalt und Aktionsbereich.
4. Scrollbare Dialoginhalte behalten oben, unten und seitlich ausreichende Abstände.
5. Material-`AlertDialog` und andere Komponenten mit eigenem korrektem Content-Padding erhalten kein unnötiges zusätzliches Doppel-Padding.
6. Die Korrektur funktioniert auf schmalen Android-Displays ebenso wie auf großen beziehungsweise Desktop-Fenstern, ohne unnötig nutzbare Inhaltsfläche zu verlieren.
7. Für mindestens einen schmalen und einen breiten Layoutzustand werden Compose-Previews oder UI-Screenshots der wichtigsten Dialogtypen verglichen.
8. Neue UI-Komponenten können die gemeinsamen Spacing-Rollen wiederverwenden, ohne erneut frei gewählte Paddingwerte einzuführen.


### V3-004 – Gleichheit und Selbigkeit als getrennte Relationen

**Status:** analysiert, Umsetzung geplant

Der Mathematik Atlas trennt künftig zwei bislang vermischte Begriffe:

- **Gleichheit** `=` vergleicht den mathematischen Inhalt.
- **Selbigkeit** vergleicht zusätzlich den mathematischen Datentyp beziehungsweise die Strukturart auf jeder Verschachtelungsebene.

Damit ist Selbigkeit strenger als Gleichheit. Zwei Objekte können gleich sein, ohne selbig zu sein. Sind zwei Objekte selbig, müssen sie dagegen immer auch gleich sein.

#### Begriffsdefinition

**Gleichheit** ist eine rekursive Inhaltsrelation. Zwei Werte sind gleich, wenn sie derselben explizit definierten Inhaltsfamilie angehören und ihre mathematisch relevanten Inhalte rekursiv gleich sind. Repräsentations- beziehungsweise Strukturtypen dürfen dabei ignoriert werden, wenn für diese Typen ausdrücklich dieselbe Inhaltsfamilie definiert ist.

**Selbigkeit** ist eine rekursive typisierte Strukturrelation. Zwei Werte sind nur dann selbig, wenn:

1. ihr mathematischer Struktur- beziehungsweise Datentyp derselbe ist,
2. ihre Struktur dieselbe Form besitzt,
3. alle enthaltenen Bestandteile paarweise wiederum selbig sind.

Selbigkeit bedeutet ausdrücklich **nicht Kotlin-Referenzidentität** und nicht „dieselbe Instanz im Arbeitsspeicher“. Zwei getrennt erzeugte Spaltenvektoren können selbig sein, wenn sie denselben Strukturtyp besitzen und ihre Komponenten rekursiv selbig sind.

Der Vergleich erfolgt auf mathematisch ausgewerteten beziehungsweise normalisierten Werten, nicht auf der exakten Quelltext- oder AST-Schreibweise. Beispielsweise darf ein zu derselben rationalen Zahl vereinfachter Zahlterm denselben mathematischen Zahlenwert repräsentieren. Der Strukturtyp nach der mathematischen Normalisierung bleibt für Selbigkeit jedoch relevant.

#### Grundgesetz

Für alle unterstützten mathematischen Objekte gilt:

```
selbig(a, b) => gleich(a, b)
```

Die Umkehrung gilt ausdrücklich nicht.

Soweit eine Relation entscheidbar ist, sollen sowohl Gleichheit als auch Selbigkeit reflexiv, symmetrisch und transitiv sein und damit jeweils eine Äquivalenzrelation bilden.

#### Verbindliche Beispiele

| Links | Rechts | Gleich | Selbig |
|---|---|---:|---:|
| `Tupel(1, 2)` | `Tupel(1, 2)` | ja | ja |
| `Tupel(1, 2)` | `SpaltenVektor(1, 2)` | ja | nein |
| `Tupel(1, 2)` | `ZeilenVektor(1, 2)` | ja | nein |
| `SpaltenVektor(1, 2)` | `ZeilenVektor(1, 2)` | ja | nein |
| `SpaltenVektor(1, 2)` | `SpaltenVektor(1, 2)` | ja | ja |
| `ZeilenVektor(1, 2)` | `ZeilenVektor(1, 2)` | ja | ja |
| `SpaltenVektor(1, 2)` | `SpaltenVektor(1, 3)` | nein | nein |
| `Tupel(1, 2)` | `Tupel(1, 2, 3)` | nein | nein |

Die Typprüfung der Selbigkeit gilt **rekursiv auf allen Ebenen**. Deshalb gilt beispielsweise:

- `SpaltenVektor(Tupel(1, 2))` und `SpaltenVektor(SpaltenVektor(1, 2))` können inhaltlich gleich sein, sind aber nicht selbig.
- Zwei Spaltenvektoren sind nur dann selbig, wenn ihre korrespondierenden Komponenten nicht nur gleich, sondern jeweils wiederum selbig sind.
- Dasselbe gilt für Zeilenvektoren und andere zusammengesetzte mathematische Objekte.

Die bereits vorhandene Gleichheit zwischen einer rationalen Zahl `r` und einer komplexen Zahl `r + 0i` ist ein weiteres Beispiel für die Trennung: inhaltlich können sie gleich sein; selbig sind sie wegen des unterschiedlichen Zahltyps nicht.

#### Inhaltsfamilien

Die Gleichheit darf Typen nicht beliebig miteinander vergleichen. Cross-Type-Gleichheit wird nur über explizite Inhaltsfamilien zugelassen.

Für den ersten verbindlichen Umfang gilt:

- `Tupel`, `SpaltenVektor` und `ZeilenVektor` gehören bezüglich der Gleichheit zur Familie **geordnete Komponentenfolge**.
- Ihre äußere Repräsentationsart und Vektororientierung werden für `=` ignoriert.
- Anzahl und Reihenfolge der Komponenten bleiben Teil des Inhalts.
- Für Selbigkeit bleiben `Tupel`, `SpaltenVektor` und `ZeilenVektor` drei verschiedene Strukturtypen.
- Eine `Matrix` behält ihre zweidimensionale Form als Inhaltsbestandteil. Matrizen werden nicht lediglich flach als Komponentenfolge verglichen. Ob eine `1×n`- oder `n×1`-Matrix künftig zusätzlich in eine gemeinsame Inhaltsfamilie mit orientierten Vektoren aufgenommen wird, muss ausdrücklich definiert werden und geschieht nicht implizit.
- Mengen verwenden eine ungeordnete Inhaltssemantik. Bei Gleichheit werden Elemente über Gleichheit verglichen; bei Selbigkeit über Selbigkeit.
- Für bisher nicht ausdrücklich cross-type-fähige Objektarten gilt konservativ: unterschiedliche Strukturtypen beweisen keine Gleichheit. Das Ergebnis bleibt unbekannt, sofern ihre Ungleichheit nicht fachlich nachgewiesen werden kann.

#### Ist-Analyse

Die aktuelle Implementierung vermischt mathematische und technische Gleichheit:

- `Gleichheit.entscheide()` beginnt nach einer Zahlvereinfachung mit `l == r`. Damit wird Kotlin-`equals` als mathematischer Wahrheitsbeweis verwendet.
- Danach existieren einzelne Sonderfälle für `Tupel`, `KomplexeZahl` und rationale Zahlen.
- Tupel werden bereits komponentenweise rekursiv über `Gleichheit` verglichen.
- `SpaltenVektor`, `ZeilenVektor` und `Matrix` besitzen derzeit keine entsprechende allgemeine Gleichheitssemantik. Unterschiedliche Vektor-/Tupeltypen mit gleichem Inhalt werden deshalb nicht nach dem neuen Begriff behandelt.
- `relation.gleichheit` ist als allgemeiner Prädikatoperator registriert und als Äquivalenzrelation markiert.
- `Ungleichheit` ist direkt als Negation der bestehenden `Gleichheit` implementiert.
- `<=` und `>=` verwenden in ihrer Definitionsformel die Gleichheit. Das soll weiterhin die Inhaltsgleichheit sein.
- Gleichungen, Lösungsverfahren, Fallunterscheidungen und mathematische Bedingungen verwenden `Gleichheit` als Wertgleichheit. Diese Verwendung bleibt grundsätzlich richtig.
- `ElementBeziehung` prüft die Mitgliedschaft in endlichen Mengen derzeit über `Gleichheit`.
- `eindeutigeEndlicheElemente` entfernt beziehungsweise identifiziert Elemente ebenfalls über `Gleichheit`.
- Die aktuelle kanonische Definitionsformel von Gleichheit lautet sinngemäß: Zwei Objekte sind gleich, wenn sie in exakt denselben Mengen enthalten sind. Diese Definition beschreibt nach der neuen Begriffstrennung nicht mehr die lockere Inhaltsgleichheit.

#### Auswirkungen auf Mengen

Die neue Trennung muss auch in der Mengenlogik sichtbar sein.

Wenn `Tupel(1,2) = SpaltenVektor(1,2)`, aber beide nicht selbig sind, darf eine Menge, die explizit den Tupelwert als Element enthält, nicht allein aufgrund der Inhaltsgleichheit so behandelt werden, als enthalte sie automatisch auch den Spaltenvektor als dasselbe Element.

Deshalb gilt für die elementare Mengenidentität:

- **Mitgliedschaft in einer konkret materialisierten endlichen Menge wird über Selbigkeit der Elemente bestimmt.**
- **Eindeutigkeit und Kardinalität materialisierter Mengenelemente verwenden Selbigkeit.**
- Zwei gleichinhaltige, aber nicht selbige Objekte dürfen somit als verschiedene konkrete Elemente repräsentiert werden.
- Die **Gleichheit zweier Mengen** darf weiterhin eine inhaltlich-extensionalere Aussage sein und Elemente über Gleichheit zuordnen.
- Die **Selbigkeit zweier Mengen** verlangt denselben Mengentyp beziehungsweise dieselbe Mengenstruktur und eine Zuordnung selbiger Elemente.

Diese Trennung verhindert, dass die Typinformation von Zeile, Spalte und Tupel durch die Mengenimplementierung wieder verloren geht.

#### Definitionskarten

Die bisherige Gleichheitsdefinition über Ununterscheidbarkeit durch Mengenmitgliedschaft wird neu eingeordnet.

Die bisherige Formel

```latex
a=b
\Longleftrightarrow
\forall M\left(
  \operatorname{Menge}(M)
  \Rightarrow
  (a\in M\Leftrightarrow b\in M)
\right)
```

passt nach der neuen Semantik zur **Selbigkeit**, sofern Mengenmitgliedschaft konkrete Elemente über Selbigkeit bestimmt.

Für Gleichheit wird stattdessen eine explizite Inhaltsdefinition benötigt. Konzeptionell:

```latex
a=b
\Longleftrightarrow
\operatorname{Inhalt}(a)
\;\mathrel{\stackrel{?}{=}}\;
\operatorname{Inhalt}(b)
```

`Inhalt` ist dabei keine pauschale Typauslöschung, sondern die je Inhaltsfamilie definierte mathematische Inhaltsprojektion. Für Tupel, Zeile und Spalte ist dies beispielsweise dieselbe geordnete Folge rekursiv projizierter Komponenten.

Die Definitionskarte muss die rekursive Regel und die zulässigen Cross-Type-Inhaltsfamilien sichtbar machen; sie darf nicht behaupten, beliebige Objekte mit zufällig ähnlicher Darstellung seien gleich.

#### Symbol der Selbigkeit

Gewünscht ist ein eigenes Gleichheitszeichen mit einem Fragezeichen am Gleichheitszeichen, vom Nutzer als `\substack{?}{=}` angegeben.

Der aktuelle native LaTeX-Renderer unterstützt `\stackrel`, aber bislang kein `\substack`. Für die Umsetzung bestehen daher zwei zulässige Wege:

1. `\substack` gezielt im Renderer ergänzen und die gewünschte Schreibweise direkt unterstützen, oder
2. die visuell entsprechende vorhandene Darstellung `\stackrel{?}{=}` als kanonisches Selbigkeitszeichen verwenden.

Die endgültige Darstellung muss projektweit einheitlich sein: Rechenkern, Prädikatauswahl, Definitionskarten, Renderer, Suchindex und Tests dürfen nicht verschiedene Symbole verwenden.

#### Zielarchitektur im Rechenkern

Mathematische Gleichheit und Selbigkeit werden ausdrücklich von Kotlin-`equals` und Referenzidentität getrennt.

Vorgesehen ist ein zentraler Vergleichspfad im `MathematikRechenSystem`, beispielsweise mit den beiden Operationen:

```
entscheideGleichheit(links, rechts, kontext)
entscheideSelbigkeit(links, rechts, kontext)
```

oder einem gemeinsamen Vergleichsdienst mit zwei Modi.

Der Vergleichspfad muss:

- Zahlen vor dem Wertvergleich mit den bereits vorhandenen mathematischen Regeln vereinfachen,
- Inhaltsfamilien explizit erkennen,
- zusammengesetzte Werte rekursiv vergleichen,
- für Selbigkeit auf jeder Rekursionsebene den exakten mathematischen Strukturtyp prüfen,
- `Bewiesen`, `Widerlegt`, `Unbekannt` und gegebenenfalls `NichtAuswertbar` beibehalten,
- inkompatible Typen nur dann als widerlegt behandeln, wenn die Semantik dies tatsächlich beweist,
- keinen mathematischen Vergleich durch Kotlin-`equals` ersetzen.

Kotlin-`equals/hashCode` der Datenklassen bleiben technische Strukturmechanismen für Collections, Caches und interne Datenhaltung und werden nicht auf die neue mathematische Gleichheit umdefiniert.

#### Prädikatoperatoren

- `relation.gleichheit` bleibt bestehen und erhält die neue Inhaltssemantik.
- Neu kommt `relation.selbigkeit` hinzu.
- `relation.selbigkeit` verwendet dieselben allgemeinen Objekteingänge `links` und `rechts`.
- Beide Relationen werden als Äquivalenzrelationen beschrieben.
- Für Selbigkeit werden Suchbegriffe wie „selbig“, „dasselbe“, „gleicher Typ und Inhalt“ und das gewählte Symbol registriert.
- `relation.ungleichheit` bleibt die Negation von **Gleichheit**, nicht die Negation von Selbigkeit.
- Eine Nicht-Selbigkeit kann bei Bedarf als `Negation(Selbigkeit(...))` dargestellt werden; ein eigener Operator ist durch diese Anforderung noch nicht vorgeschrieben.
- Da der generische Prädikatknoten bereits verschiedene Relationsoperatoren trägt, benötigt Selbigkeit keinen neuen fachlichen Knotentyp, sondern eine neue Relationsoperator-Variante.

#### Auswirkungen auf bestehende Systeme

Bei der Umsetzung müssen mindestens folgende Stellen geprüft und bewusst auf Gleichheit oder Selbigkeit festgelegt werden:

- `Gleichheit` und `Ungleichheit` in `Aussagen.kt`,
- `RelationsOperatoren`,
- Relations-Definitionsformeln und automatisch erzeugte Definitionskarten,
- endliche Mengen, Mitgliedschaft, Eindeutigkeit und Mächtigkeit,
- Vereinigung, Schnitt, Differenz und kartesische Produkte,
- Gleichungs- und Lösungsverfahren,
- Fallunterscheidungen und Bedingungen,
- Tupel- und Vektoroperationen,
- Matrix- und Tensorstrukturen,
- Methodenargumente und Methodenbildmengen,
- Visualisierungs- und Koordinatenadapter,
- Suchindex, Prädikatauswahl und Konzeptbibliothek,
- vorhandene Tests, die Kotlin-Strukturgleichheit implizit mit mathematischer Gleichheit gleichsetzen.

#### Abnahmekriterien

1. `Tupel(1,2) = SpaltenVektor(1,2)` wird als wahr entschieden.
2. `Tupel(1,2)` und `SpaltenVektor(1,2)` sind nicht selbig.
3. `ZeilenVektor(1,2) = SpaltenVektor(1,2)` wird als wahr entschieden.
4. Ein Zeilen- und ein Spaltenvektor sind unabhängig von ihren Komponenten niemals selbig.
5. Zwei Spaltenvektoren sind genau dann selbig, wenn sie gleich lang sind und alle korrespondierenden Komponenten rekursiv selbig sind.
6. Zwei Zeilenvektoren erfüllen dieselbe rekursive Selbigkeitsregel.
7. Ein unterschiedlicher Datentyp auf einer beliebigen inneren Verschachtelungsebene kann Selbigkeit widerlegen, obwohl die äußeren Objekte inhaltlich gleich bleiben.
8. Selbigkeit impliziert in allen getesteten Fällen Gleichheit.
9. Gleichheit und Selbigkeit verwenden weder Kotlin-Referenzidentität noch eine globale Umdefinition von `equals/hashCode`.
10. Mitgliedschaft und Eindeutigkeit materialisierter endlicher Mengen unterscheiden gleichinhaltige, aber nicht selbige Elemente.
11. `Ungleichheit` bleibt exakt die logische Negation der neuen Inhaltsgleichheit.
12. Bestehende Gleichungslöser, `<=`/`>=` und mathematische Bedingungen verwenden weiterhin Gleichheit und werden nicht versehentlich auf Selbigkeit umgestellt.
13. `relation.selbigkeit` ist im Prädikatdialog auswählbar, auswertbar, suchbar und besitzt eine Definitionskarte.
14. Gleichheits- und Selbigkeitsdarstellung sind in Rechenkern, UI und Definitionskarten konsistent.
15. Tests decken mindestens rationale/komplexe Zahlen, Tupel, Zeilenvektoren, Spaltenvektoren, verschachtelte Strukturen, Matrizen und endliche Mengen ab.


### V3-005 – Wahr/Falsch-Schalter an unverbundenen Aussage-Eingängen

**Status:** analysiert, Umsetzung geplant

Jeder konkrete Eingang vom mathematischen Typ **Aussage** erhält direkt am Knoten einen binären Standardwert-Schalter, solange dieser Eingang nicht durch eine Edge belegt ist.

Der Schalter definiert den Wert, den der Eingang ohne Verbindung liefert:

- **aus** = `Falsch` beziehungsweise `WahrheitsKonstante(false)`
- **ein** = `Wahr` beziehungsweise `WahrheitsKonstante(true)`

Damit sind unverbundene Aussage-Eingänge unmittelbar benutzbar und benötigen keinen zusätzlichen Wahr-/Falsch-Knoten nur zur Belegung eines konstanten Eingangs.

#### Ist-Analyse

Der Atlas besitzt bereits ein allgemeines Parameter-Schema für Standardwerte mit Schlüsseln der Form

```
standardwert.<anschlussname>
```

Dieses wird aktuell jedoch nur für Zahl-Eingänge ausgewertet:

- `KnotenInspektorFenster.StandardwerteEditor` bietet nur für nicht dynamische Zahl-Eingänge ein Textfeld an.
- `KartenAuswerter.sammleEingänge()` erzeugt nur für Zahl-Eingänge aus einem gespeicherten `standardwert.*` einen `RationaleZahl`-Wert.
- Aussage-Eingänge ohne Verbindung besitzen deshalb keinen allgemeinen Fallback.
- Einzelne Aussagenoperatoren reagieren unterschiedlich auf fehlende Eingänge: manche brechen mit „Aussage fehlt“ ab, andere erzeugen für unverbundene dynamische Eingänge eine `UnentscheidbareAussage`. Diese Sonderfälle sollen durch einen einheitlichen Standardwertpfad ersetzt werden, soweit tatsächlich ein konkreter Aussage-Eingang vorhanden ist.
- Die eigentlichen Anschlussgriffe werden im domänenneutralen Modul `KnotenKartenVerwalter` gezeichnet. Dieses Modul kennt absichtlich keine `MathematikAnschlussArten` und darf deshalb keine Aussage-spezifische Logik erhalten.

#### UI-Verhalten

Für jeden Aussage-Eingang gilt:

1. **Unverbunden:** Neben dem Anschluss wird ein kompakter Wahr/Falsch-Schalter angezeigt.
2. **Verbunden:** Der Schalter wird ausgeblendet. Der über die Edge kommende Wert hat vollständig Vorrang.
3. **Verbindung getrennt:** Der Schalter erscheint wieder mit seinem zuvor gespeicherten Zustand.
4. Eine Verbindung verändert oder löscht den gespeicherten Standardwert nicht.
5. Das Umschalten ist eine normale undo-/redo-fähige Kartenänderung.
6. Der Zustand wird mit der Karte persistiert und nach erneutem Öffnen identisch wiederhergestellt.
7. Der Schalter muss sowohl per Touch als auch Maus zuverlässig bedienbar sein und eine zugängliche Semantik wie „Standardwert <Eingangsname>: Wahr/Falsch“ besitzen.
8. Das Betätigen des Schalters darf keinen Node-Drag und keinen Verbindungs-Drag auslösen.

Der Schalter sitzt visuell beim zugehörigen Eingang und nicht nur im Inspektor. Dadurch bleibt sichtbar, welcher Wahrheitswert tatsächlich verwendet wird, ohne zuerst den Knoten auswählen und einen separaten Dialog öffnen zu müssen.

#### Standardwert und Speicherung

Das bestehende Standardwert-Schema wird erweitert statt dupliziert.

Für einen Aussage-Eingang `a` wird beispielsweise gespeichert:

```
standardwert.a=true
```

beziehungsweise

```
standardwert.a=false
```

Fehlt bei einer älteren Karte der Parameter vollständig, wird der Eingang deterministisch mit **Falsch** initialisiert beziehungsweise interpretiert. Sobald der Benutzer den Schalter betätigt, wird der explizite Wert gespeichert.

Die Persistenz benutzt den stabilen Anschlussnamen wie das bestehende Zahl-Standardwertsystem. Bei Knotenmodi, die Anschlussnamen ersetzen, müssen Konfigurations- und Migrationspfade prüfen, ob der zugehörige Standardwert erhalten, umbenannt oder bewusst entfernt werden muss.

#### Auswertung

`KartenAuswerter.sammleEingänge()` wird so erweitert, dass nach den echten Edge-Verbindungen auch unverbundene Aussage-Eingänge materialisiert werden.

Für einen nicht verbundenen Aussage-Eingang entsteht:

```kotlin
BedingterWert(
    objekt = WahrheitsKonstante(standardwert),
    latexDarstellung = WahrheitsKonstante(standardwert).zuLatex(),
)
```

Dabei gilt strikt:

```
verbundener Wert > gespeicherter Standardwert > impliziter Standard Falsch
```

Ein verbundener Eingang darf also niemals zusätzlich den Standardwert in die Auswertung einmischen.

Die Fallback-Erzeugung gehört in den gemeinsamen Kartenadapter und nicht in jeden einzelnen Knotenauswerter. Aussagenknoten sollen ihre Eingänge anschließend genauso lesen können, als käme der Wert von einem normalen Wahr-/Falsch-Knoten.

#### Dynamische Aussage-Eingänge

Bei erweiterbaren Operatoren wie Konjunktion, Disjunktion oder Adjunktion muss zwischen **konkretem Eingang** und einem ausschließlich zur Erzeugung weiterer Eingänge dienenden Erweiterungsplatz unterschieden werden.

- Jeder zur aktuellen Operatorstelligkeit gehörende konkrete Aussage-Eingang besitzt den Schalter, wenn er unverbunden ist.
- Ein rein technischer Reserve-/Erweiterungshandle darf nicht allein durch seinen impliziten Standardwert als tatsächlich belegtes zusätzliches Argument zählen.
- Das Umschalten eines solchen Anschlusses macht ihn dagegen zu einem bewusst verwendeten Argument; falls die dynamische Eingangslogik einen neuen Reserveanschluss benötigt, muss sie danach denselben Mechanismus anwenden wie beim Verbinden einer Edge.
- Automatische Bereinigung darf einen bewusst gesetzten Standardwert nicht wie einen vollständig leeren dynamischen Eingang entfernen.

Damit entsteht bei variadischen Operatoren keine endlose Reihe falscher Standardargumente nur deshalb, weil ein neuer Reservehandle erzeugt wird.

#### Architektur der Anschlussdarstellung

Die Aussage-spezifische Entscheidung bleibt außerhalb des domänenneutralen `KnotenKartenVerwalter`.

Dafür soll der Karteneditor einen kleinen optionalen Erweiterungspunkt für Anschluss-UI erhalten, beispielsweise sinngemäß:

```kotlin
anschlussZusatz: @Composable (
    knoten: KnotenDaten,
    anschluss: AnschlussDaten,
    verbunden: Boolean,
) -> Unit
```

Der Mathematik-Atlas kann darüber für unverbundene Aussage-Eingänge den Schalter einsetzen.

Der generische Editor selbst kennt dabei weder `MathematikAnschlussArten.Aussage` noch `WahrheitsKonstante` oder die Bedeutung des Schalters. So bleibt die bereits etablierte Trennung zwischen allgemeinem Node-Editor und mathematischer Domäne erhalten.

Alternativ ist ein gleichwertiger generischer Anschluss-Decorator zulässig, sofern dieselbe Modulgrenze gewahrt bleibt.

#### Darstellung und Skalierung

Der Schalter gehört logisch zum Knoteninhalt, nicht zum zoomunabhängig groß gehaltenen Trefferbereich des Handles.

- Er bewegt und skaliert sich zusammen mit dem Knoten.
- Die Bedienfläche muss bei üblichen Zoomstufen noch sicher erreichbar sein.
- Bei sehr kleinem Zoom darf die Darstellung vereinfacht oder ausgeblendet werden, sofern der mathematische Standardwert bestehen bleibt.
- Der Schalter darf Handle, Anschlussfarbe und Verbindungsziel nicht verdecken.
- Bei mehreren Aussage-Eingängen muss eindeutig erkennbar bleiben, welcher Schalter zu welchem Anschluss gehört.

#### Verhältnis zum Inspektor

Der Inline-Schalter ist die primäre Bedienoberfläche für Aussage-Standardwerte.

Der bestehende Abschnitt „Standardwerte“ im Inspektor kann zusätzlich um Aussage-Eingänge erweitert werden, muss aber denselben gespeicherten Parameter bearbeiten. Es dürfen nicht zwei voneinander unabhängige Zustände entstehen.

Falls ein Aussage-Standardwert im Inspektor angeboten wird, verwendet er ebenfalls einen `Switch` und kein freies Textfeld.

#### Abnahmekriterien

1. Jeder unverbundene konkrete Eingang mit `MathematikAnschlussArten.Aussage` zeigt direkt am Knoten einen Wahr/Falsch-Schalter.
2. Ein neuer Aussage-Eingang ohne gespeicherten Wert liefert deterministisch `Falsch`.
3. Schalter auf „Wahr“ erzeugt bei der Kartenauswertung `WahrheitsKonstante(true)`.
4. Schalter auf „Falsch“ erzeugt `WahrheitsKonstante(false)`.
5. Wird eine Edge mit dem Eingang verbunden, verschwindet der Schalter und ausschließlich der verbundene Wert wird ausgewertet.
6. Wird die Edge wieder entfernt, erscheint der Schalter mit seinem vorherigen gespeicherten Zustand.
7. Umschalten unterstützt Undo und Redo und wird mit der Karte persistiert.
8. Negation, Implikation, Äquivalenz, Konjunktion, Disjunktion, Adjunktion und weitere Knoten mit Aussage-Eingängen können unverbundene Aussage-Eingänge über diesen gemeinsamen Mechanismus auswerten.
9. Dynamische Reserveeingänge zählen nicht allein wegen ihres impliziten Falsch-Werts als zusätzliches Operatorargument.
10. Ein explizit umgeschalteter dynamischer Eingang bleibt erhalten und wird als bewusst verwendetes Argument behandelt.
11. Touch auf den Schalter verschiebt den Knoten nicht und startet keine Handle-Verbindung.
12. Android- und Desktop-Oberfläche verwenden dieselbe gespeicherte Semantik.
13. `KnotenKartenVerwalter` erhält keine Abhängigkeit auf mathematische Anschlussarten oder Wahrheitswerte.
14. Bestehende Zahl-Standardwerte funktionieren unverändert weiter.


### V3-006 – Einheitliche Geometrie für Typ- und Repräsentationsadapter

**Status:** analysiert, Umsetzung geplant

Knoten, deren primäre Aufgabe darin besteht, einen vorhandenen mathematischen Inhalt in einen anderen Typ, Vertrag oder eine andere Repräsentationshülle zu überführen, sollen sich geometrisch klar von normalen Rechen- und Fachknoten unterscheiden.

Normale Knoten bleiben grundsätzlich abgerundete Rechtecke. Reine beziehungsweise überwiegende **Adapter-/Konverterknoten** erhalten dagegen eine eigene, einheitliche Form.

Verbindliche Beispiele sind zunächst:

- `Term zu Methode`,
- `Aussage zu Methode`,
- `Tupel zu Spaltenvektor`,
- `Tupel zu Zeilenvektor` beziehungsweise der konsolidierte `Tupel zu Vektor`-Knoten.

#### Semantische Abgrenzung

Ein Adapterknoten gehört zu dieser Darstellungsfamilie, wenn er im Wesentlichen

```
gleicher mathematischer Inhalt
→ anderer Typ / anderer Vertrag / andere Repräsentationsform
```

abbildet und keine eigenständige mathematische Operation auf dem Inhalt ausführt.

Typische Merkmale:

- normalerweise genau ein fachlicher Eingang und ein fachlicher Ausgang,
- der Ausgang entsteht hauptsächlich durch Umhüllen, Typisieren, Orientieren oder Binden vorhandener Information,
- der wesentliche Inhalt des Eingangswerts bleibt nachvollziehbar erhalten,
- der Knoten stellt eher eine Grenze zwischen zwei Darstellungs- oder Vertragsformen als einen Rechenschritt dar.

Nicht allein wegen eines unterschiedlichen Ein- und Ausgangstyps zu dieser Familie gehören beispielsweise:

- Betrag beziehungsweise Radius,
- Ableitung und Integration,
- Kreuzprodukt,
- Mächtigkeit,
- Real- oder Imaginärteil,
- Vektor zu Polynom, sofern dabei die Komponenten als Koeffizienten einer neuen mathematischen Struktur interpretiert werden,
- sonstige Operationen, die tatsächlich neue mathematische Information berechnen oder verwerfen.

Die Einordnung erfolgt daher explizit über eine UI-/Knotenklassifikation und nicht automatisch nach der Regel „Eingangstyp != Ausgangstyp“.

#### Geometrie

Als gemeinsame Adapterform wird ein **horizontal ausgerichtetes, an beiden Seiten abgeschrägtes Rechteck** verwendet, visuell also ein flaches Hexagon:

```
    ____________
   /            \
--<              >--
   \____________/
```

Die Form vermittelt einen gerichteten Übergang von links nach rechts:

- Eingänge liegen weiterhin an der linken Seite,
- Ausgänge weiterhin an der rechten Seite,
- die abgeschrägten Seiten markieren den Übergangscharakter,
- obere und untere Kante bleiben weitgehend horizontal, damit Text und Formelinhalt genügend Platz besitzen.

Die Form darf nicht wie eine Raute wirken, da eine Raute üblicherweise eine Entscheidung oder Verzweigung signalisiert. Ebenso soll sie nicht als bloße Pillenform umgesetzt werden, weil dies weiterhin wie ein stärker abgerundeter normaler Knoten wirken würde.

#### Kompaktere Darstellung

Adapterknoten sollen zusätzlich einfacher und kompakter als normale Fachknoten sein.

Bevorzugte Darstellung:

- zentraler kurzer Titel oder eine kompakte Typabbildung,
- optional eine kleine Transformation wie `Tupel → Spalte` beziehungsweise `Term → Methode`,
- keine unnötige große Inhaltsfläche,
- keine wiederholte Langbeschreibung direkt im Knoten,
- detaillierte Konfiguration bleibt im Inspector,
- ein Adapter ohne zusätzliche Konfiguration darf deutlich niedriger und schmaler als ein normaler Knoten sein.

Wo die Anschlussarten bereits eindeutig sichtbar sind, darf die Knotenbeschriftung sehr knapp bleiben.

#### Ist-Analyse

Die äußere Knotenform ist derzeit nicht Teil des Renderer- oder Vorlagenvertrags.

`KnotenKartenEditor.KnotenDarstellung()` erzeugt für jeden Knoten zentral:

```kotlin
Card(
    Modifier.fillMaxSize()
        .border(..., MaterialTheme.shapes.medium),
    ...
)
```

und verwendet damit für alle normalen Knoten dieselbe abgerundete Rechteckform.

`KnotenRenderer` kann derzeit lediglich

- den Inhaltsbereich,
- den Interaktionsmodus und
- eine optionale Fußzeile

bereitstellen. Eine äußere Geometrie kann ein mathematischer Renderer aktuell nicht auswählen.

Dadurch sehen `mathematik.termZuMethode` und `mathematik.tupelZuSpalte` trotz ihres Adaptercharakters geometrisch genauso aus wie Addition, Kreuzprodukt oder andere eigentliche Operationen.

#### Zielarchitektur

Der fachneutrale Karteneditor erhält einen kleinen allgemeinen Vertrag für Knotengeometrien. Er darf dabei nicht wissen, was ein mathematischer Adapter ist.

Geeignet ist beispielsweise eine fachneutrale Klassifikation:

```kotlin
enum class KnotenForm {
    AbgerundetesRechteck,
    Adapter,
}
```

oder ein gleichwertiger allgemeiner Formvertrag.

Die tatsächliche Compose-`Shape` für `Adapter` wird zentral im Editor umgesetzt. Dadurch verwenden Android und Desktop dieselbe Geometrie.

Die mathematische Schicht entscheidet lediglich, welche Knotenart diese Form erhält.

Die Forminformation kann entweder

- vom `KnotenRenderer` bereitgestellt werden oder
- als allgemeine Darstellungsmetadaten der Knotenvorlage geführt werden.

Bevorzugt wird ein Renderer-/Darstellungsvertrag, solange die Form keine fachliche oder persistenzrelevante Eigenschaft des mathematischen Knotens ist. Eine reine UI-Geometrie soll nicht ohne Grund das gespeicherte mathematische Kartenmodell erweitern.

#### Gemeinsame Form für Card, Rahmen und Auswahl

Die gewählte Knotenform muss konsequent auf alle sichtbaren Schichten angewendet werden:

- Hintergrundfläche,
- Card beziehungsweise Surface,
- normaler Rahmen,
- Auswahlrahmen,
- Hover-/Fokusdarstellung,
- gegebenenfalls Schatten und Clip.

Es darf nicht lediglich ein Hexagon in ein weiterhin sichtbares abgerundetes Rechteck gezeichnet werden. Der ganze Knoten muss tatsächlich die neue Silhouette besitzen. Die Menschheit hat bereits genug UI-Elemente, bei denen drei verschiedene Rahmen übereinander so tun, als seien sie Absicht.

#### Interaktion und Trefferfläche

Die visuelle Form darf die Bedienbarkeit nicht verschlechtern.

- Handles bleiben an ihren semantischen linken beziehungsweise rechten Positionen.
- Die abgeschrägten Seiten müssen ausreichend Platz für Handle-Trefferflächen lassen.
- Knotenverschiebung, Auswahl, Kontextmenü und Inspector bleiben unverändert erreichbar.
- Eine rechteckige interne Pointer-Trefferbox ist als erste Implementierung zulässig, solange sie nicht sichtbar über die Form hinausragt und keine benachbarten Knoten störend überlappt.
- Langfristig kann die exakte Shape auch für Hit-Testing verwendet werden, falls dies messbar oder UX-seitig erforderlich ist.

#### Klassifikation der bestehenden Knoten

Für die erste Umsetzung werden mindestens alle ein-zu-eins-artigen Transformationsknoten geprüft und explizit einer der beiden Gruppen zugeordnet:

1. **Adapter / Typ- oder Repräsentationsänderung**
2. **echte mathematische Operation**

Explizit als Adapter vorgesehen:

- `mathematik.termZuMethode` in der Term- und Aussagevariante,
- `mathematik.tupelZuSpalte`,
- die historische beziehungsweise orientierte Zeilenvariante `mathematik.tupelZuZeile`,
- der daraus konsolidierte `Tupel zu Vektor`-Knoten.

Weitere Knoten werden nur aufgenommen, wenn ihre Semantik dieselbe Adaptereigenschaft erfüllt. Die Darstellung darf nicht allein anhand des Namens oder einer unterschiedlichen Anschlussart automatisch gewählt werden.

#### Verhältnis zu Gleichheit und Selbigkeit

Die neue Adapterdarstellung passt zur in V3-004 eingeführten Trennung von Gleichheit und Selbigkeit:

Ein Typadapter kann einen inhaltlich gleichen Wert erzeugen, der wegen seines anderen mathematischen Strukturtyps nicht selbig mit dem Eingang ist.

Beispielsweise:

```
Tupel(1,2)
    → [Tupel zu Spalte]
SpaltenVektor(1,2)
```

Der Adapter macht damit im Graphen sichtbar, dass an dieser Stelle bewusst eine Typ-/Strukturgrenze überschritten wird.

Die Geometrie selbst bestimmt jedoch keinerlei Gleichheitssemantik. Sie visualisiert lediglich die bereits fachlich definierte Transformation.

#### Abnahmekriterien

1. Normale Rechen- und Fachknoten bleiben abgerundete Rechtecke.
2. `Term zu Methode` und `Aussage zu Methode` verwenden die neue Adaptergeometrie.
3. `Tupel zu Vektor` beziehungsweise die Zeilen-/Spaltenvarianten verwenden dieselbe Adaptergeometrie.
4. Die Adapterform ist auf Android und Desktop identisch.
5. Hintergrund, Clip, Rahmen, Auswahlrahmen und Schatten folgen derselben Form.
6. Handles bleiben korrekt positioniert und vollständig bedienbar.
7. Adapterknoten sind kompakter als vergleichbare normale Fachknoten, sofern ihr Inhalt keine größere Fläche benötigt.
8. Ein Knoten wird nicht automatisch nur deshalb Adapter, weil Ein- und Ausgang unterschiedliche Typen besitzen.
9. Mathematische Operationen wie Ableitung, Betrag, Kreuzprodukt oder Mächtigkeit behalten die normale Knotengeometrie.
10. Die Formklassifikation bleibt fachneutral im Karteneditor; die Entscheidung, welche mathematische Knotenart Adapter ist, liegt in `MathematikKnoten`.
11. Die neue Geometrie verändert keine Auswertung, Persistenz oder mathematische Semantik.
12. Preview-/Screenshot-Tests vergleichen mindestens normalen Rechenknoten, `Term zu Methode` und `Tupel zu Vektor` bei hellem und dunklem Theme.
