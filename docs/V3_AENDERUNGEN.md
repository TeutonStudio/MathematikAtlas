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
