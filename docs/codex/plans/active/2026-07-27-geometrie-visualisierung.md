# Geometrievisualisierung für v2.2.0 – ExecPlan

Status: geplant; Branch `feature/v2.2.0-geometrie-visualisierung`.

## Ziel und Nutzerwirkung

Diese Branch implementiert einen eigenständigen Geometrievisualisierer für euklidische Räume und geometrische Objekte in 1D, 2D und 3D. Der Knoten stellt intrinsische Geometrie, Geometriegruppen oder Zellstrukturen dar und reicht den fachlichen Eingang unverändert weiter. Höherdimensionale Geometrie wird nur nach einer expliziten Projektion akzeptiert.

## Nicht-Ziele

- keine Erweiterung oder Umdeutung des bestehenden Mengenvisualisierungsknotens,
- keine Persistenz von Rendernetzen, Punktwolken oder Samples,
- keine direkte 4D-/5D-Darstellung,
- keine mathematische Transformation durch Kamerabewegung,
- keine Geometrieberechnung im Compose-Renderer,
- keine vollständige CAD- oder Mesh-Editorfunktion.

## Untersuchte Ist-Situation

- Die vorhandene `mathematik.visualisierung` stellt Mengen in R2 oder R3 numerisch angenähert dar.
- `VisualisierungsKonfiguration` persistiert Dimension, Achsen, Bereiche, Farbe, Sampling und Kamera.
- Die bestehende Konfiguration besitzt `RaumDimension { R2, R3 }` und muss aus Kompatibilitätsgründen unverändert bleiben.
- Der vorhandene Visualisierungsknoten ist nur an der Kopfzeile ziehbar.
- Sampling und Kamera sind bereits getrennt, was für den neuen Renderer als Architekturprinzip übernommen werden soll.

## Fachliche und mathematische Semantik

### Eingaben

Der Visualisierer akzeptiert einen gemeinsamen Obertyp `GeometrieDarstellbar` beziehungsweise typkompatible Eingänge für:

- `EuklidischerRaum`,
- `GeometrischerAusdruck`,
- `GeometrieGruppe`,
- `GeometrieStruktur`.

Optional kann ein passendes Koordinatensystem verbunden werden. Ohne Eingabe wird die vorhandene analytische Standardrealisierung des Raums verwendet, sofern verfügbar.

### Ausgabe

Der fachliche Eingang wird unverändert ausgegeben. Kamera, Projektion, Farbe und Sichtbarkeit ändern niemals das mathematische Objekt.

### Dimension

Die Darstellungsdimension stammt aus dem Raum oder dem expliziten Projektionsresultat:

- R1: eindimensionale Achse,
- R2: ebene Darstellung,
- R3: räumliche Darstellung.

Ein Objekt mit Dimension oder Einbettungsraum größer als 3 wird ohne vorgeschalteten Projektionsknoten als nicht darstellbar gemeldet.

### Zellbasierte Darstellung

Renderreihenfolge:

1. gefüllte sichtbare Zellen höchster Dimension,
2. Flächen und deren Ränder,
3. Kanten,
4. Ecken,
5. Beschriftungen und Hilfskonstruktionen.

Exakte gekrümmte Objekte dürfen für das Rendering tesselliert werden. Die Tessellierung ist eine abgeleitete Renderstruktur und ersetzt nicht die exakte Geometrie.

## Daten-, Node-, Handle- und Edge-Vertrag

### Node

```text
ID: mathematik.geometrie.visualisierung
Eingang: inhalt : GeometrieDarstellbar
Eingang optional: koordinatensystem : Koordinatensystem
Ausgang: inhalt : GeometrieDarstellbar
```

Falls das bestehende Anschlusssystem keinen gemeinsamen dynamischen Ausgangstyp passend zum konkreten Eingang unterstützt, wird ein stabiler allgemeiner Geometrie-Darstellbar-Typ verwendet. Der Ausgabewert bleibt dieselbe Instanz beziehungsweise derselbe `BedingterWert`.

### Eigene Konfiguration

Vorgesehen:

- `GeometrieRaumDimension { R1, R2, R3 }`, wobei die Dimension abgeleitet und nicht frei widersprüchlich gesetzt wird,
- Achsen- und Rasterkonfiguration,
- sichtbare Zellgrade,
- Ecken-/Kanten-/Flächenstil,
- Transparenz,
- Labels und Hilfskonstruktionen,
- Kamera,
- Projektion `Orthografisch`, `Perspektivisch`, `Isometrisch`,
- Tessellierungsauflösung und Toleranz.

Die Konfiguration erhält eigene Eigenschaftsschlüssel und einen eigenen Parser.

## Architekturentscheidungen

- eigener Node-Typ und eigenes Konfigurationsmodell,
- gemeinsame interne Renderhilfen mit der Mengenvisualisierung nur bei klarer Abhängigkeitsrichtung,
- Renderer erhält fachlich vorbereitete Renderprimitive oder Zellstrukturen,
- Sampling/Tessellierung läuft entprellt außerhalb des UI-Threads,
- Kamera ist reiner UI-Zustand und Teil der persistierbaren Ansichtskonfiguration, aber nicht Teil des mathematischen Ergebnisses,
- R1 bekommt eine eigene reduzierte Interaktionslogik.

## Betroffene Dateien und Symbole

Neue Pfade voraussichtlich:

```text
MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/geometrie/visualisierung/
├── modell/GeometrieVisualisierungsKonfiguration.kt
├── modell/GeometrieRenderPrimitive.kt
├── sampling/GeometrieTessellierer.kt
├── projektion/GeometrieProjektion.kt
└── ui/GeometrieVisualisierungsRenderer.kt
```

Additiv betroffen:

- Geometrieknotenvorlagen und -auswerter,
- `AtlasZustand.kt` für Rendererwahl,
- `KnotenInspektoren.kt` für den neuen Inspector.

Die bestehende Datei `visualisierung/modell/VisualisierungsKonfiguration.kt` wird nicht fachlich erweitert. Ihr `V2_2_MERGE_NOTE` ist beim finalen Merge zu prüfen.

## Meilensteine

- [ ] eigenes Konfigurationsmodell und Eigenschaftsroundtrip,
- [ ] gemeinsame Renderprimitiv-Schnittstelle,
- [ ] R1-Renderer,
- [ ] R2-Renderer,
- [ ] R3-Renderer,
- [ ] Tessellierung gekrümmter Objekte,
- [ ] Kamera- und Projektionsinteraktion,
- [ ] Inspector,
- [ ] Renderer- und Persistenztests.

## Konkrete Umsetzungsschritte

1. Branch nach Integration von Kern und Knoten auf den aktuellen Release-Stand bringen.
2. neuen Node- und Konfigurationsvertrag konkretisieren.
3. fachliche Geometrie in dimensionsunabhängige Renderprimitive übersetzen.
4. R1-Achse mit Punkt-, Strecken-, Strahl- und Geradendarstellung implementieren.
5. R2-Projektion und Darstellung für Punkte, Geraden, Kreise, Polygone und Gebiete implementieren.
6. R3-Kamera und orthografische Projektion als erste stabile Variante implementieren.
7. perspektivische und isometrische Projektion ergänzen, falls die Grundvariante verifiziert ist.
8. Zellgrade, Transparenz und Labels konfigurierbar machen.
9. Tessellierung für Kreislinien, Kugeloberflächen und symbolische Zellen implementieren.
10. Inspector und Standardansicht je Dimension umsetzen.

## Dimensionsabhängige Interaktion

### R1

- Translation entlang x,
- Zoom,
- Standardansicht.

### R2

- Translation x/y,
- Ansichtsrotation optional,
- Zoom,
- Standardansicht.

### R3

- Rotation x/y/z,
- Translation x/y/z,
- Zoom,
- Standardansicht,
- Projektion umschalten.

Die Steuerflächen dürfen keine Knotenziehbewegung auslösen. Der Knoten bleibt nur an der Kopfzeile ziehbar.

## Tests und Validierung

Mindestens prüfen:

- bestehende Mengenvisualisierung lädt und verhält sich unverändert,
- Geometrievisualisierung erhält eigene Eigenschaften,
- R1 zeichnet Punkt, Strecke, Strahl und Gerade,
- R2 zeichnet Polygonrand und -gebiet getrennt,
- R3 zeichnet Zellflächen vor Kanten und Ecken,
- 4D-Eingabe ohne Projektion erzeugt einen verständlichen Fehler,
- projizierte 4D-Geometrie nach R3 ist darstellbar,
- Kameraänderungen verändern nicht den Ausgabewert,
- Tessellierungsänderungen verändern nicht das geometrische Objekt,
- Standardansicht ist je Dimension korrekt,
- Interaktion verschiebt den Knoten nicht versehentlich,
- Eigenschaftsroundtrip erhält Kamera und Darstellungseinstellungen.

Auszuführen:

```text
./gradlew :MathematikKnoten:test
./gradlew :app:test
./gradlew :app:assembleDebug
python3 scripts/pruefe_repository.py
```

## Persistenz und Migration

- eigener Node-Typ `mathematik.geometrie.visualisierung`,
- eigene rekursive Eigenschaftsmap,
- keine Migration vorhandener `mathematik.visualisierung`-Knoten,
- keine persistierten Renderprimitive oder Tessellierungen,
- gespeicherte Kamera- und Stilwerte erhalten sichere Defaults für fehlende Felder.

## Risiken und Rückfallstrategie

Risiken:

- zu starke Kopplung an den bestehenden Mengensampler,
- UI-Blockierung durch Tessellierung,
- unklare Tiefensortierung in R3,
- widersprüchliche Dimensionseinstellung,
- versehentliche fachliche Transformation durch die Kamera.

Gegenmaßnahmen:

- eigene Pipeline mit kleinen wiederverwendbaren Hilfen,
- Berechnung auf `Dispatchers.Default`,
- deterministische Sortierung beziehungsweise einfache Tiefenstrategie für v2.2.0,
- Dimension ausschließlich aus dem Eingang ableiten,
- Ausgabe unverändert durchreichen.

Bei Problemen kann R3 zunächst auf orthografische Projektion und polygonale Zellstrukturen begrenzt werden, ohne R1/R2 oder den Geometriekern zurückzunehmen.

## Fortschritt

- 2026-07-27: Branch aus der Planungsintegration angelegt.
- 2026-07-27: ExecPlan erstellt.

## Entscheidungsprotokoll

Branchspezifische Entscheidung: Der Geometrievisualisierer ist ein fachlicher Durchreicher mit eigener Darstellungskonfiguration. Er ersetzt weder das Geometrieobjekt noch die vorhandene Mengenvisualisierung.

## Abweichungen vom ursprünglichen Plan

Noch keine.

## Ergebnis und Verifikation

Noch keine Implementierung und keine für diese Branch ausgeführten Tests.

## Mergevertrag

Diese Branch wird nach Kern und Geometrieknoten in `release/v2.2.0-geometrie` gemergt. Vor Merge muss nachgewiesen sein, dass bestehende Mengenvisualisierungskarten unverändert geladen werden und keine neue Dimension in deren altes Enum oder Eigenschaftsschema eingeführt wurde.
