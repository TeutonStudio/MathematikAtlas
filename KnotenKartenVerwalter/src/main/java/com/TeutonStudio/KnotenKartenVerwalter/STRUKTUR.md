# Struktur: KnotenKartenVerwalter

Der `KnotenKartenVerwalter` ist ein Android-Library-Modul fuer interaktive Knoten-Karten in Jetpack Compose. Das Modul stellt ein kontrolliertes Graph-Editor-System bereit: Die App besitzt den fachlichen Zustand, das Modul rendert diesen Zustand und meldet Benutzeraktionen ueber Callbacks oder Commands zurueck.

Das Modul ist in zwei Hauptbereiche getrennt:

- `daten`: fachlicher Zustand, Auswahl, Commands, Copy/Paste, Layout, Verbindungsregeln, mathematische Typen und Pull-Cache.
- `schnittstelle`: Compose-UI, Rendering von Karte/Knoten/Anschluessen/Verbindungen, Pan/Zoom, Drag-Interaktion, Minimap, Kontrollleiste und Kontextmenue.

## Modulaufbau

```text
KnotenKartenVerwalter/
└── src/main/java/com/TeutonStudio/KnotenKartenVerwalter/
    ├── STRUKTUR.md
    ├── PLAN.md
    ├── daten/
    │   ├── GraphDaten.kt
    │   ├── KarteDaten.kt
    │   ├── KnotenDaten.kt
    │   ├── VerbindungDaten.kt
    │   ├── AnschlussDaten.kt
    │   ├── AuswahlDaten.kt
    │   ├── KartenCommands.kt
    │   ├── KartenBearbeitung.kt
    │   ├── KartenZwischenablage.kt
    │   ├── KartenLayoutAlgorithmus.kt
    │   ├── KartenPullCache.kt
    │   ├── VerbindungsRegeln.kt
    │   ├── MathematikTypen.kt
    │   ├── KartenGrenzen.kt
    │   ├── KartenOptionen.kt
    │   ├── KartenEreignisse.kt
    │   └── KartenKontextZiel.kt
    └── schnittstelle/
        ├── Karte.kt
        ├── Knoten.kt
        ├── Anschluss.kt
        ├── Verbindung.kt
        ├── Übersicht.kt
        ├── KontrollLeiste.kt
        ├── Koordinaten.kt
        ├── KartenInteraktion.kt
        ├── KartenControls.kt
        ├── KartenRenderer.kt
        ├── VerbindungGeometrie.kt
        ├── MiniMap.kt
        ├── Hintergrund.kt
        └── GraphObjekt.kt
```

## Zentrale Schichten

### Daten-Schicht

Die Daten-Schicht beschreibt den Zustand der Karte und alle reinen Operationen darauf. Sie ist der stabile Kern des Moduls.

`GraphDaten.kt` definiert die gemeinsame Minimalbasis fuer Graph-Elemente:

- `GraphDaten`
  - besitzt nur `id`.
  - wird von Karte, Knoten, Verbindungen und Anschluessen verwendet.

`KarteDaten.kt` definiert den gesamten Kartenzustand:

- `AnsichtsfensterDaten`
  - persistierter Viewport einer Karte.
  - enthaelt `verschiebung` und `zoom`.
  - `verschiebung` liegt in Bildschirmkoordinaten.
- `KarteZustand`
  - Laufzeit-Zustand der sichtbaren Karte.
  - enthaelt Viewport, UI-Flags und Auswahl.
  - Felder: `verschiebung`, `zoom`, `zeigeÜbersicht`, `zeigeKontrollLeiste`, `auswahl`.
- `KarteDaten`
  - fachlicher Zustand einer Knotenkarte.
  - enthaelt `id`, `name`, optionale `größe`, `knoten`, `verbindungen`, Initialdaten, registrierte Arten, `ansichtsfenster` und `cache`.
  - wird immutable behandelt: Aenderungen erzeugen neue Instanzen ueber `copy(...)`.

`KnotenDaten.kt` beschreibt einzelne Knoten:

- `KnotenDaten`
  - Felder: `id`, `name`, `position`, `fläche`, `art`, `ausgewaehlt`, `beweglich`, `data`.
  - `position` ist die linke obere Ecke in Weltkoordinaten.
  - `fläche` beschreibt Breite und Hoehe als `Offset`.
  - `art` steuert, welche Knotenklasse und Anschluesse verwendet werden.
  - `data` traegt fachliche Zusatzdaten, zum Beispiel mathematische Werte, Operatoren oder Formeln.
- Hilfsfunktionen rechnen Knotenpositionen in Bildschirmkoordinaten um.

`AnschlussDaten.kt` beschreibt Handles an Knoten:

- `AnschlussRichtung`
  - `Eingang`
  - `Ausgang`
- `AnschlussKante`
  - `Links`, `Rechts`, `Oben`, `Unten`
- `AnschlussDaten`
  - gemeinsame Basis fuer Eingangs- und Ausgangsanschluesse.
  - enthaelt `id`, `label`, `richtung`, `kante` und optional `zahlenTyp`.
- `EingangDaten`
  - standardmaessig links.
- `AusgangDaten`
  - standardmaessig rechts.

`VerbindungDaten.kt` beschreibt Kanten zwischen Anschluessen:

- `VerbindungDaten`
  - referenziert Quelle und Ziel ueber Knoten-ID und Anschluss-ID.
  - enthaelt optional `label`, `art`, `zahlenTyp` und `fehler`.
  - `ausgewaehlt` kann fuer direkte Anzeige genutzt werden, die kontrollierte Auswahl liegt aber in `AuswahlDaten`.
- `mitErsetztemEingang(...)`
  - fuegt eine Verbindung hinzu und ersetzt bestehende Verbindungen auf demselben Ziel-Eingang.
  - dadurch bleiben Ausgaenge mehrfach nutzbar, Eingangsanschluesse erhalten aber nur eine eingehende Verbindung.

`AuswahlDaten.kt` modelliert Auswahl:

- `AuswahlDaten`
  - enthaelt `knotenIds` und `verbindungIds`.
  - bietet Hilfen fuer Einzelwahl, Hinzufuegen, Entfernen, Umschalten und Zusammenfuehren.
  - ist kontrollierter Zustand: Die UI meldet Auswahlwunsch, die App entscheidet ueber den neuen Zustand.

### Command-Schicht

`KartenCommands.kt` kapselt fachliche Aenderungen an `KarteDaten`.

Wichtige Typen:

- `KartenCommand`
  - besitzt `beschreibung`.
  - fuehrt `ausfuehren(karte, auswahl)` aus.
- `KartenCommandErgebnis`
  - enthaelt neue `karte`, optionale neue `auswahl` und `ausgefuehrt`.
- konkrete Commands:
  - `KnotenErstellen`
  - `KnotenAendern`
  - `KnotenVerschieben`
  - `VerbindungErstellen`
  - `VerbindungLoeschen`
  - `AuswahlEinfuegen`
  - `AuswahlLoeschen`
  - `KartenLayoutAnwenden`
  - `AppKartenCommand`

`KartenControllerZustand` ist ein optionaler Controller ueber der reinen Command-Schicht:

- haelt `karte` und `auswahl`.
- kann Undo/Redo verwalten.
- kann nach jedem Command den Pull-Cache aktualisieren.
- speichert History-Eintraege mit Vorher-/Nachher-Karte und Vorher-/Nachher-Auswahl.

Der Controller ist nicht zwingend fuer die UI. Eine App kann die Daten auch selbst halten und die Callbacks direkt in eigene Updates umsetzen.

### Bearbeitung, Copy/Paste und Layout

`KartenBearbeitung.kt` enthaelt reine Bearbeitungsfunktionen:

- `KartenBearbeitungsAktion`
  - beschreibt Absichten wie Loeschen, Kopieren, Einfuegen, Abbrechen, Rueckgaengig und Wiederholen.
- `auswahlImBereich(...)`
  - bestimmt Knoten innerhalb eines Weltbereichs.
  - nimmt Verbindungen mit, wenn Quelle und Ziel im Bereich liegen.
- `loescheAuswahl(...)`
  - entfernt ausgewaehlte Knoten.
  - entfernt auch Verbindungen, die direkt ausgewaehlt sind oder an geloeschten Knoten haengen.
- `dupliziereAuswahl(...)`
  - kopiert Auswahl, verschiebt sie und fuegt sie mit neuen IDs wieder ein.

`KartenZwischenablage.kt` beschreibt Copy/Paste:

- `KartenZwischenablage`
  - enthaelt kopierte Knoten, kopierte Verbindungen und Ursprungsauswahl.
- `kopiereAuswahl(...)`
  - kopiert ausgewaehlte Knoten.
  - kopiert explizit ausgewaehlte Verbindungen und interne Verbindungen zwischen kopierten Knoten.
- `fuegeEin(...)`
  - vergibt neue IDs fuer Knoten und Verbindungen.
  - positioniert kopierte Knoten relativ zur Zielposition.
  - gibt neue Karte und neue Auswahl zurueck.

`KartenLayoutAlgorithmus.kt` definiert automatische Layouts:

- `KartenLayoutAlgorithmus`
  - Fun-Interface fuer austauschbare Layoutberechnung.
- `StandardKartenLayout`
  - berechnet einfache Spalten anhand gerichteter Verbindungstiefe.
  - Knoten ohne erreichbare topologische Position werden danach angeordnet.
  - veraendert nur Knotenpositionen.

### Verbindungsregeln und Mathematik

`VerbindungsRegeln.kt` prueft, ob eine Verbindung fachlich erlaubt ist:

- verhindert standardmaessig Selbstverbindungen.
- verhindert standardmaessig doppelte Verbindungen.
- erlaubt standardmaessig nur Ausgang nach Eingang.
- prueft optional mathematische Typkompatibilitaet.
- `mitTypPruefung(...)` uebernimmt den Quelltyp in die Verbindung und setzt bei Inkompatibilitaet einen Fehlertext.

`MathematikTypen.kt` modelliert Zahlenraeume und Anschluss-/Verbindungstypen:

- `Zahlenraum`
  - `Natuerlich`
  - `Ganz`
  - `Rational`
  - `Reell`
  - `Komplex`
  - `Eingeschraenkt`
  - `Produkt`
  - `Funktion`
- `ZahlenTyp`
  - kombiniert Zahlenraum mit optionalem Wert, Anzeigename oder Ausdruck.
  - liefert `kurzform` fuer die Anzeige.
- `istKompatibelMit(...)`
  - prueft, ob der Quellraum als Teilraum des Zielraums verwendbar ist.

### Pull-Cache

`KartenPullCache.kt` stellt ein einfaches Cache-System fuer Knotenauswertung bereit.

Wichtige Typen:

- `KnotenCacheEintrag`
  - `knotenId`, `signatur`, Ergebnisdaten, optionaler Fehler und Gueltigkeit.
- `KartenCacheDaten`
  - Cache-Map pro Knoten.
- `KnotenPullAuswertung`
  - austauschbare Auswertungsfunktion.
- `StandardKnotenPullAuswertung`
  - baut ein einfaches Ergebnis aus Knotendaten und Eingangs-Caches.
- `mitAktualisiertemPullCache(...)`
  - berechnet die Knoten in topologischer Reihenfolge.
  - nutzt alte Cache-Eintraege wieder, wenn die Signatur unveraendert ist.
  - verarbeitet Zyklen oder nicht besuchte Knoten am Ende in bestehender Reihenfolge.

Die Signatur eines Knotens setzt sich aus Name, Art, sortierten `data`-Eintraegen, eingehenden Verbindungen und Eingangs-Cache-Signaturen zusammen. Dadurch werden nur Knoten neu berechnet, deren lokale Daten oder Eingangsdaten sich geaendert haben.

## Schnittstellen-Schicht

Die Schnittstellen-Schicht rendert die Karte und behandelt Pointer-Interaktionen.

### GraphObjekt

`GraphObjekt.kt` ist die gemeinsame UI-Basis:

- Objekte koennen sich als Compose-Baustein rendern.
- Karte, Knoten, Anschluss und Verbindung implementieren diese Struktur.

### Karte

`Karte.kt` ist die zentrale UI-Datei.

Oeffentliche Einstiegspunkte:

- `KarteDaten.zuComposable(...)`
  - rendert eine Karte mit Minimal-API.
  - erwartet mindestens eine `aktualisierung` fuer Knotenbewegungen.
- ueberladene `KarteDaten.zuComposable(...)`
  - zusaetzlich mit `onVerbindungErstellen`, `onKontextAktion`, `onAuswahlÄndern`.
- `BasisKarte`
  - implementiert `Karte` und delegiert an `KartenOberfläche`.

`KartenOberfläche` ist der zentrale Compose-Baustein:

- misst die sichtbare Flaeche.
- initialisiert einen lokalen Viewport aus `daten.ansichtsfenster`.
- erzeugt sichtbare Knoten inklusive temporärer Drag-Positionen.
- markiert Knoten und Verbindungen anhand von `zustand.auswahl`.
- berechnet Anschlusspositionen fuer Rendering, Drag und Hit-Test.
- rendert Verbindungen hinter Knoten.
- rendert temporaere Verbindung waehrend Anschluss-Drag.
- rendert Knoten mit Drag-Modifiern.
- rendert optional Minimap und Kontrollleiste.
- rendert Kontextmenue ueber allem.

Wichtige interne Hilfsmodelle:

- `KartenTreffer`
  - Ergebnis eines Hit-Tests: Hintergrund, Knoten, Anschluss oder Verbindung.
- `KartenKontextAktion`
  - vom Kontextmenue gemeldete Aktion mit Ziel und Weltposition.
- `AnschlussReferenz`
  - aufgeloester Anschluss mit Bildschirmposition und Typ.
- `VerbindungsDrag`
  - temporaerer Zustand beim Ziehen einer neuen Verbindung.
- `KontextMenüZustand`
  - Position, Ziel und Weltposition des offenen Kontextmenues.

### Knoten

`Knoten.kt` definiert Knoten-Fabriken, Knotentypen und die Standarddarstellung.

Wichtige Typen:

- `KnotenFabrik`
  - erzeugt aus `KnotenDaten` ein konkretes `Knoten`-Objekt.
- `KnotenArten`
  - Registry nach `KnotenDaten.art`.
  - entspricht konzeptionell `nodeTypes`.
  - enthaelt Standardtypen und kann mit `mit(...)` erweitert werden.
- `Knoten`
  - besitzt `daten`, `eingänge`, `ausgänge`.
  - liefert geordnete Anschlussdaten.
  - rendert sich als Compose-Baustein.
- `BasisKnoten`
  - Standardknoten mit einem Eingang und einem Ausgang.

Vorhandene Knotentypen:

- `BasisKnoten`: generischer Knoten.
- `EingabeKnoten`: kein Eingang, Standardausgang.
- `AusgabeKnoten`: Standardeingang, kein Ausgang.
- `MathematikEingabeKnoten`: Ausgang `wert`.
- `UnbekannteKnoten`: Ausgang `variable`.
- `RechenKnoten`: Eingaenge `links` und `rechts`, Ausgang `ergebnis`.
- `FormelKnoten`: Eingang `in`, Ausgang `formel`.
- `AuswertungsKnoten`: Eingang `in`, Ausgang `ergebnis`.
- `FunktionKnoten`: Eingang `argument`, Ausgang `wert`.

Die Standarddarstellung `KnotenRahmen(...)`:

- zeichnet Rahmen und Hintergrund.
- faerbt den Rahmen blau, wenn der Knoten ausgewaehlt ist.
- zeigt Name und mathematische Kurzform oder Typ.
- legt Anschluesse an den konfigurierten Kanten an.
- skaliert Groessen anhand der aktuellen Zoomstufe.

### Anschluesse

`Anschluss.kt` rendert einzelne Handles und definiert die Anschluss-Objekte.

Wichtige Typen:

- `Anschluss`
  - besitzt `daten`, `besitzer` und optionalen `partner`.
  - prueft, ob eine Verbindung zu einem anderen Anschluss erlaubt ist.
- `BasisAnschluss`
  - verhindert Verbindungen am selben Knoten und zwischen gleicher Richtung.
- `BasisEingang`
- `BasisAusgang`

Rendering:

- Eingaenge werden blau dargestellt.
- Ausgaenge werden gruen dargestellt.
- Standardform ist ein kleiner Kreis.
- `anschlussModifierSkaliert(...)` passt Groesse und Padding an die Zoomstufe an.

### Verbindungen

`Verbindung.kt` rendert Kanten auf einer Canvas-Ebene.

Wichtige Typen:

- `Verbindung`
  - UI-Objekt fuer eine Verbindung.
- `BasisVerbindung`
  - Standardverbindung.
- `VerbindungFabrik`
- `VerbindungArten`
  - Registry nach `VerbindungDaten.art`.
  - kann mit `mit(...)` erweitert werden.

Rendering:

- Alle Verbindungen werden gemeinsam im Canvas hinter den Knoten gezeichnet.
- Fehlende Endpunkte werden uebersprungen.
- Eine Verbindung ist eine kubische Bezier-Kurve.
- Farbe:
  - rot bei `fehler`.
  - blau bei Auswahl.
  - grau im Normalzustand.

### Minimap und Kontrollleiste

`Übersicht.kt` enthaelt die aktuell aktive Minimap:

- berechnet Graphgrenzen und sichtbaren Weltbereich.
- vereinigt beide Bereiche fuer stabile Projektion.
- rendert Knoten als kleine Rechtecke.
- rendert den sichtbaren Viewport als blaues Rechteck.
- Drag auf der Minimap verschiebt den Hauptviewport.

`MiniMap.kt` ist ein vorbereiteter Zielort fuer die spaetere bereinigte Minimap-Schnittstelle. Die aktive Implementierung liegt noch in `Übersicht.kt`.

`KontrollLeiste.kt` rendert die schwebende Zoom-Leiste:

- Zoom raus.
- Zoom rein.
- Fit-to-View.
- Die Leiste mutiert nicht direkt, sondern ruft Callbacks auf.

`KartenControls.kt` sammelt reine Berechnungen fuer Kontrollen:

- `fitView(...)` berechnet Zoom und Verschiebung fuer einen Weltbereich.

### Koordinaten und Interaktion

`Koordinaten.kt` ist der zentrale Ort fuer Umrechnungen:

- Weltposition zu Bildschirmposition.
- Bildschirmposition zu Weltposition.
- Bildschirmdelta zu Weltdelta.

`KartenInteraktion.kt` enthaelt vorbereitete Daten und Logik fuer eine spaetere Trennung der Pointer-Interaktion:

- `KnotenDragZustand`
- `zoomeUmPunkt(...)`

Aktuell liegt die konkrete Gestenerkennung noch in `Karte.kt`.

## Ausfuehrlicher Ablauf

### 1. Initialisierung durch die App

Die App erstellt oder laedt eine `KarteDaten`:

1. Karte bekommt `id` und `name`.
2. Knoten werden als `KnotenDaten` mit Position, Flaeche, Art und optionalen Fachwerten angelegt.
3. Verbindungen werden als `VerbindungDaten` mit Quell- und Zielreferenzen angelegt.
4. Optional werden `AnsichtsfensterDaten`, `KartenCacheDaten`, Knotenarten oder Verbindungsarten uebergeben.

Danach ruft die App `karte.zuComposable(...)` auf und uebergibt:

- `zustand`: Viewport-Flags und aktuelle Auswahl.
- `aktualisierung`: Callback fuer Knotenpositionen.
- `onVerbindungErstellen`: Callback fuer neue Verbindungen.
- `onKontextAktion`: Callback fuer Kontextmenue-Aktionen.
- `onAuswahlÄndern`: Callback fuer Auswahlwechsel.

Die App kann die Rueckmeldungen direkt selbst behandeln oder ueber `KartenControllerZustand.fuehreAus(...)` in Commands umsetzen.

### 2. Aufbau der sichtbaren Karte

`BasisKarte` ruft `KartenOberfläche` auf. Dort passiert der eigentliche UI-Aufbau:

1. Die sichtbare Container-Groesse wird mit `onSizeChanged` gespeichert.
2. Der lokale Viewport `ansicht` wird anhand der Karten-ID aus `daten.ansichtsfenster` initialisiert.
3. Temporäre UI-Zustaende werden angelegt:
   - gezogene Knotenpositionen.
   - aktueller Verbindungs-Drag.
   - offenes Kontextmenue.
   - Sperrflags fuer Hintergrundgesten.
4. Aus `daten.knoten` entstehen `sichtbareKnotenDaten`.
   - Falls ein Knoten gerade gezogen wird, wird seine temporäre Position verwendet.
   - Auswahl wird aus `zustand.auswahl` in `ausgewaehlt` gespiegelt.
5. `KnotenArten` wandelt `KnotenDaten` in konkrete `Knoten`-Objekte.
6. Verbindungen werden ebenfalls mit Auswahlstatus gespiegelt.
7. Zu jedem Knoten werden Anschlussreferenzen mit Bildschirmposition berechnet.

Die UI arbeitet damit auf einer sichtbaren Momentaufnahme, waehrend der fachliche Zustand weiterhin beim Aufrufer liegt.

### 3. Viewport, Weltkoordinaten und Bildschirmkoordinaten

Knotenpositionen und Knotengroessen liegen in Weltkoordinaten. Der Viewport liegt in `KarteZustand`:

- `zoom`: Skalierung zwischen Welt und Bildschirm.
- `verschiebung`: Bildschirmverschiebung des Weltkoordinatensystems.

Umrechnung:

```text
bildschirm = welt * zoom + verschiebung
welt       = (bildschirm - verschiebung) / zoom
```

Diese Umrechnung wird genutzt fuer:

- Knotenpositionen.
- Anschlusspositionen.
- Hit-Tests.
- Minimap-Viewport.
- Drag-Deltas.
- Fit-to-View.

Beim Zoomen um einen Punkt bleibt der Weltpunkt unter dem Zeiger stabil. Dafuer berechnet `transformiereUm(...)` zuerst das Weltzentrum unter dem Zeiger und setzt danach Zoom und Verschiebung so, dass dieses Weltzentrum weiterhin unter derselben Bildschirmposition liegt.

### 4. Rendering-Reihenfolge

Die Karte rendert in dieser Reihenfolge:

1. Hintergrund der Kartenflaeche.
2. Persistierte Verbindungen auf Canvas.
3. Temporäre Verbindung waehrend Anschluss-Drag.
4. Knoten inklusive Anschluessen.
5. Optionale Minimap.
6. Optionale Kontrollleiste.
7. Kontextmenue.

Diese Reihenfolge sorgt dafuer, dass Verbindungen unter Knoten liegen, Drag-Vorschauen sichtbar sind und Menues immer ueber dem Karteninhalt erscheinen.

### 5. Knoten-Rendering

Fuer jeden sichtbaren Knoten:

1. `KnotenArten.erstelle(...)` sucht anhand von `KnotenDaten.art` die passende Fabrik.
2. Der konkrete Knotentyp liefert seine Eingangs- und Ausgangsanschluesse.
3. `KnotenRahmen(...)` rendert den sichtbaren Knoten.
4. Name und mathematische Kurzform werden angezeigt.
5. Anschluesse werden an linker, rechter, oberer oder unterer Kante verteilt.
6. Die ganze Knotenbox wird per `Modifier.offset` an die Bildschirmposition gesetzt.
7. Die Knotenbox wird anhand von `fläche * zoom` skaliert.

Die fachliche Bedeutung eines Knotens entsteht aus `art` und `data`. Zum Beispiel kann ein Mathematik-Eingabeknoten aus `data["wert"]` und `data["zahlenTyp"]` eine Anzeige wie `4 in N` bilden.

### 6. Verbindungs-Rendering

Fuer jede Verbindung:

1. `startOffset(...)` sucht den Quellknoten.
2. Im Quellknoten wird der passende Ausgangsanschluss anhand `quellAnschlussId` gesucht.
3. Die Anschlussposition wird in Bildschirmkoordinaten berechnet.
4. `endeOffset(...)` macht dasselbe fuer Zielknoten und Ziel-Eingang.
5. Wenn beide Endpunkte existieren, erzeugt `VerbindungArten` ein Verbindungsobjekt.
6. Die Verbindung wird als Bezier-Kurve auf Canvas gezeichnet.

Verbindungen sind dadurch robust gegen Knotenbewegungen: Die Verbindung speichert nur IDs, die eigentliche Geometrie wird pro Frame aus den aktuellen Knotenpositionen berechnet.

### 7. Pan und Zoom

Pan und Zoom werden in `KartenOberfläche` ueber `detectTransformGestures` behandelt:

1. Benutzer bewegt den Hintergrund oder nutzt Pinch-Zoom.
2. Wenn keine blockierende Knoten- oder Anschlussinteraktion aktiv ist, wird das Kontextmenue geschlossen.
3. `transformiereUm(...)` berechnet den neuen Viewport.
4. Der lokale Viewport `ansicht` wird aktualisiert.
5. Knoten, Verbindungen, Anschluesse, Minimap und Kontrollleiste werden mit dem neuen Viewport neu gerendert.

Der aktuelle Code haelt den Viewport lokal in der UI. `KartenEreignisse.onAnsichtAendern` und `KartenOptionen` bereiten eine staerker kontrollierte Variante vor.

### 8. Auswahl per Klick

Ein Tap auf die Karte fuehrt einen Hit-Test aus:

1. Zuerst wird geprueft, ob ein Anschluss in der Naehe liegt.
2. Danach wird geprueft, ob der Punkt in einem Knotenrechteck liegt.
3. Danach wird geprueft, ob der Punkt nahe an einer Bezier-Verbindung liegt.
4. Wenn nichts passt, ist der Treffer der Hintergrund.

Der Treffer wird in `AuswahlDaten` umgewandelt:

- Hintergrund: leere Auswahl.
- Knoten: Auswahl mit dieser Knoten-ID.
- Anschluss: Auswahl des zugehoerigen Knotens.
- Verbindung: Auswahl mit dieser Verbindungs-ID.

Die UI ruft danach `onAuswahlÄndern(...)` auf. Erst der aufrufende Code setzt die Auswahl in `KarteZustand`. Dadurch bleibt die Auswahl kontrollierbar.

### 9. Knoten ziehen

Beim Ziehen eines Knotens:

1. `detectDragGestures` startet auf dem Knoten.
2. Hintergrundgesten werden blockiert.
3. Das Kontextmenue wird geschlossen.
4. Der gezogene Knoten wird als Auswahl gemeldet.
5. Die Startposition wird in Weltkoordinaten gespeichert.
6. Jedes Bildschirmdelta wird durch den aktuellen Zoom geteilt.
7. Die neue Weltposition wird in `gezogeneKnoten` gespeichert, damit die UI sofort reagiert.
8. Gleichzeitig ruft die UI `aktualisierung(knotenId, neuePosition)` auf.
9. Die App kann daraus zum Beispiel `KnotenVerschieben` ausfuehren.
10. Nach Drag-Ende werden Hintergrundgesten wieder freigegeben.

Die temporäre Map `gezogeneKnoten` verhindert sichtbares Nachlaufen, falls die App den neuen `KarteDaten`-Zustand erst im naechsten Compose-Zyklus zurueckliefert.

### 10. Verbindung erstellen

Beim Ziehen von einem Anschluss:

1. Der Anschluss-Modifier startet eine Pointer-Geste.
2. `ziehtAnschluss` und `blockiereHintergrundGesten` werden gesetzt.
3. Ein `VerbindungsDrag` speichert Startanschluss, Startposition und aktuelle Pointerposition.
4. Beim Ziehen wird die aktuelle Position fortlaufend aktualisiert.
5. Die Karte rendert eine temporaere Verbindung.
6. Beim Loslassen sucht `nächsterAnschluss(...)` einen Zielanschluss im Maximalabstand.
7. `istKompatibelMit(...)` prueft:
   - nicht derselbe Knoten.
   - nicht dieselbe Anschlussrichtung.
8. `zuVerbindung(...)` legt Quelle und Ziel richtig herum fest.
   - Start an Eingang wird automatisch gedreht.
   - Quelle ist immer Ausgang.
   - Ziel ist immer Eingang.
9. `mitTypPruefung(...)` uebernimmt den Quelltyp und setzt bei Typfehlern `fehler`.
10. Die UI ruft `onVerbindungErstellen(...)` auf.
11. Die App fuehrt typischerweise `VerbindungErstellen` als Command aus.

Wenn `VerbindungErstellen` mit `ersetzeBestehendenEingang = true` ausgefuehrt wird, wird eine bestehende Verbindung auf demselben Ziel-Eingang ersetzt.

### 11. Kontextmenue

Das Kontextmenue wird ueber sekundäre Eingabe geoeffnet:

- Android-MotionEvent mit Secondary Button.
- Compose-Pointer-Event mit Secondary Press.

Ablauf:

1. Bildschirmposition wird in `öffneKontextMenü(...)` verarbeitet.
2. Hit-Test bestimmt Ziel: Hintergrund, Knoten, Anschluss oder Verbindung.
3. Bildschirmposition wird in Weltposition umgerechnet.
4. `KontextMenüZustand` wird gespeichert.
5. Das Menue zeigt zielabhaengige Aktionen:
   - Hintergrund: `Knoten erstellen`, `Ansicht zentrieren`.
   - Knoten: `Knoten auswaehlen`, `Knoten duplizieren`, `Knoten loeschen`.
   - Anschluss: `Verbindung starten`, `Anschluss auswaehlen`.
   - Verbindung: `Verbindung auswaehlen`, `Verbindung loeschen`.
6. Bei Klick auf einen Eintrag ruft die UI `onKontextAktion(...)` mit Ziel, Weltposition und Aktionsname auf.
7. Die App entscheidet, welche fachliche Aenderung daraus entsteht.

### 12. Commands und Controller-Ablauf

Wenn die App `KartenControllerZustand` nutzt, laeuft eine Aenderung so:

1. UI oder App erzeugt ein `KartenCommand`.
2. `controller.fuehreAus(command)` ruft `command.ausfuehren(karte, auswahl)` auf.
3. Das Command gibt `KartenCommandErgebnis` zurueck.
4. Wenn `ausgefuehrt = false`, bleibt der Controller unveraendert.
5. Andernfalls wird die neue Auswahl uebernommen, falls sie im Ergebnis gesetzt ist.
6. Wenn `pullCacheAktiv = true`, wird `mitAktualisiertemPullCache(...)` aufgerufen.
7. Wenn Undo/Redo deaktiviert ist, werden Karte und Auswahl direkt ersetzt.
8. Wenn Undo/Redo aktiv ist, wird ein `KartenHistoryEintrag` angelegt.
9. Der Eintrag landet auf dem Undo-Stack.
10. Der Redo-Stack wird geleert.

Undo:

1. Letzter Undo-Eintrag wird genommen.
2. Karte und Auswahl werden auf `vorher` gesetzt.
3. Eintrag wandert vom Undo-Stack auf den Redo-Stack.

Redo:

1. Letzter Redo-Eintrag wird genommen.
2. Karte und Auswahl werden auf `nachher` gesetzt.
3. Eintrag wandert zurueck auf den Undo-Stack.

### 13. Copy/Paste-Ablauf

Kopieren:

1. App ruft `karte.kopiereAuswahl(auswahl)` auf.
2. Alle ausgewaehlten Knoten werden gesammelt.
3. Verbindungen werden kopiert, wenn sie explizit ausgewaehlt sind oder vollstaendig zwischen kopierten Knoten liegen.
4. Ergebnis ist eine `KartenZwischenablage`.

Einfuegen:

1. App fuehrt `AuswahlEinfuegen(zwischenablage, zielPosition)` aus.
2. `fuegeEin(...)` ermittelt die minimale Ursprungposition der kopierten Knoten.
3. Fuer jeden kopierten Knoten wird eine neue ID erzeugt.
4. Knoten werden relativ zur Zielposition neu platziert.
5. Nur Verbindungen, deren Quelle und Ziel beide kopiert wurden, werden neu erzeugt.
6. Verbindungen erhalten ebenfalls neue IDs.
7. Die neuen Elemente werden zur Karte hinzugefuegt.
8. Die eingefuegten Elemente werden als neue Auswahl zurueckgegeben.

### 14. Layout-Ablauf

`KartenLayoutAnwenden` nutzt standardmaessig `StandardKartenLayout`:

1. Alle Knoten-IDs werden gesammelt.
2. Eingangsgrade und ausgehende Nachbarschaften werden aus den Verbindungen berechnet.
3. Knoten ohne eingehende Verbindung starten die topologische Verarbeitung.
4. Ausgehende Kanten erhoehen die Tiefe der Zielknoten.
5. Besuchte Knoten werden nach Tiefe und Originalindex sortiert.
6. Pro Tiefe entsteht eine Spalte.
7. Innerhalb einer Spalte werden Knoten zeilenweise untereinander platziert.
8. Nicht besuchte Knoten, zum Beispiel in Zyklen, werden danach in weiteren Spalten angeordnet.
9. Die Karte wird mit neuen Knotenpositionen zurueckgegeben.

Das Layout erhaelt IDs, Verbindungen und fachliche Daten. Es aendert im Normalfall nur `position`.

### 15. Pull-Cache-Ablauf

Nach einem Command kann der Controller den Pull-Cache aktualisieren:

1. Knoten-IDs werden gesammelt.
2. Verbindungen innerhalb der Karte werden nach Quelle und Ziel gruppiert.
3. Aus eingehenden Verbindungen wird ein Eingangsgrad pro Knoten berechnet.
4. Knoten mit Eingangsgrad `0` starten die Verarbeitung.
5. Eine topologische Reihenfolge wird aufgebaut.
6. Nicht besuchte Knoten werden am Ende angehaengt.
7. Fuer jeden Knoten werden Eingangscaches aus bereits berechneten Quellen oder altem Cache geholt.
8. Aus Knotendaten, eingehenden Verbindungen und Eingangscaches wird eine Signatur gebildet.
9. Wenn der alte Cache dieselbe Signatur hat, wird er wiederverwendet.
10. Sonst berechnet `KnotenPullAuswertung` einen neuen `KnotenCacheEintrag`.
11. Die Karte bekommt einen neuen `KartenCacheDaten`.

Damit kann eine spaetere mathematische Auswertung inkrementell arbeiten: Unveraenderte Teilgraphen behalten ihre Cachewerte.

### 16. Minimap-Ablauf

Wenn `zustand.zeigeÜbersicht = true`, rendert die Karte die Minimap:

1. Graphgrenzen werden aus allen Knoten berechnet.
2. Der aktuell sichtbare Weltbereich wird aus Hauptviewport und Container-Flaeche berechnet.
3. Beide Bereiche werden vereinigt.
4. Eine `MiniMapProjektion` bildet Weltkoordinaten in Minimap-Koordinaten ab.
5. Knoten werden als kleine Rechtecke gezeichnet.
6. Der sichtbare Bereich wird als blaues Rechteck gezeichnet.
7. Drag auf der Minimap wird in Weltposition umgerechnet.
8. Daraus wird eine neue Hauptansicht berechnet, bei der die gewaehlte Weltposition in der Mitte liegt.

### 17. Kontrollleisten-Ablauf

Wenn `zustand.zeigeKontrollLeiste = true`, rendert die Karte die Kontrollleiste:

1. `-` ruft `zoomUm(0.8f, fläche)` auf.
2. `+` ruft `zoomUm(1.25f, fläche)` auf.
3. `[]` ruft `zoomAufInhalt(...)` auf.
4. Die lokale Ansicht wird aktualisiert.

Die Kontrollleiste selbst kennt keine Persistenz. Sie meldet nur Viewport-Aenderungen innerhalb der Kartenoberflaeche.

## Verantwortungsgrenzen

Das Modul ist verantwortlich fuer:

- Datenmodell fuer Karten, Knoten, Anschluesse und Verbindungen.
- Compose-Rendering der Graph-Oberflaeche.
- Pan, Zoom, Knoten-Drag, Anschluss-Drag und Kontextmenue.
- Auswahlmodell und Auswahl-Hilfsfunktionen.
- generische Commands.
- Undo/Redo im optionalen Controller.
- Copy/Paste und einfaches Layout.
- mathematische Typinformationen an Anschluessen und Verbindungen.
- Pull-Cache als Vorbereitung fuer Auswertung.

Die App ist verantwortlich fuer:

- Persistenz und Versionierung.
- konkrete Fachaktionen aus Kontextmenues.
- eigene Knotentypen oder Renderer.
- Inspector-/Bearbeiter-Panels.
- fachliche Validierung, falls die Standardregeln nicht reichen.
- eigentliche mathematische Auswertung, sofern sie ueber die Standard-Pull-Auswertung hinausgeht.

## Aktuelle Uebergangsstellen

Einige Dateien sind bereits als Zielstruktur vorbereitet, waehrend die aktive Logik noch teilweise in `Karte.kt` liegt:

- `Koordinaten.kt` enthaelt zentrale Umrechnung, aber `Karte.kt` besitzt noch interne Hilfsfunktionen.
- `KartenInteraktion.kt` beschreibt kuenftige Trennung der Gestenlogik, die aktuelle Gestenerkennung liegt noch in `Karte.kt`.
- `KartenControls.kt` enthaelt reine FitView-Berechnung, die aktive Kontrollleiste nutzt aktuell noch interne Funktionen aus `Karte.kt`.
- `MiniMap.kt` markiert die zukuenftige Minimap-Schnittstelle, die aktive Minimap liegt in `Übersicht.kt`.
- `KartenEreignisse.kt` und `KartenOptionen.kt` beschreiben eine breitere kontrollierte API, sind aber noch nicht vollstaendig in `KartenOberfläche` integriert.

Diese Uebergangsstellen sind wichtig fuer weitere Entwicklung: Die vorhandene Funktionalitaet ist nutzbar, aber die Architektur ist noch in Richtung staerker getrennter, testbarer Bausteine unterwegs.
