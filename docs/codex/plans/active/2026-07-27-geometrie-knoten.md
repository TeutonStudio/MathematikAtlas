# Geometrieknoten für v2.2.0 – ExecPlan

Status: geplant; Branch `feature/v2.2.0-geometrie-knoten`.

## Ziel und Nutzerwirkung

Diese Branch macht den Geometriekern im Knotengraphen des Mathematik Atlas nutzbar. Sie ergänzt Anschlussarten, Knotenvorlagen, Auswerter, Inspectorverträge, Rendererformeln und Tests. Die fachlichen Ergebnisse werden ausschließlich aus dem Rechenkern abgeleitet; die Knotenkomponenten pflegen keine zweite Geometriesemantik.

## Nicht-Ziele

- keine Definition neuer mathematischer Grundtypen außerhalb notwendiger Integrationskorrekturen,
- kein eigener Visualisierungsrenderer,
- keine direkte Änderung der neutralen Graphlogik,
- keine dynamisch wechselnden Handle-Schemata für unterschiedliche Konstruktorvarianten,
- keine automatische Geometrie-Menge-Konvertierung durch Edge-Kompatibilität.

## Untersuchte Ist-Situation

- `MathematikAnschlussArten` definiert die Typ-Hierarchie.
- `MathematikKnotenVorlagen.alle` ist die statische Katalogquelle.
- `StandardMathematikAuswerter.erzeugeRegister` registriert Node-Auswerter.
- Ein Eingang akzeptiert höchstens eine Verbindung; dynamische Eingänge sind möglich.
- `BedingterWert` transportiert Annahmen und Sonderfallbedingungen.
- Inspectoränderungen werden über `KnotenEigenschaft` und `KartenAktion` persistierbar und undo-fähig geschrieben.

## Fachliche und mathematische Semantik

Geometrieknoten repräsentieren entweder:

- ein geometrisches Objekt,
- eine Relation beziehungsweise Aussage,
- eine Konstruktion,
- eine explizite Repräsentationsumwandlung,
- eine Transformation.

Knotenvarianten mit verschiedenen Eingängen erhalten verschiedene Node-Typen. Inspectorvarianten sind nur zulässig, wenn der Handle-Vertrag unverändert bleibt.

Beispiele:

- `Kreis aus Mittelpunkt und Punkt` und `Kreis aus Mittelpunkt und Radius` sind getrennte Node-Typen.
- Winkelmaß in Grad oder Radiant kann eine Inspectorvariante sein.
- lineare und affine Punkttransformation sind wegen unterschiedlicher Eingänge getrennte Node-Typen.

## Daten-, Node-, Handle- und Edge-Vertrag

### Anschlussarten

In einem neuen `MathematikGeometrieAnschlussArten`-Register:

- Raum,
- Koordinatensystem,
- Geometrieobjekt,
- Punkt,
- Gerade,
- Ebene,
- Strecke,
- Strahl,
- Winkel,
- Kurve,
- Kreislinie,
- Polygon,
- Flächengebiet,
- Körper,
- Geometriestruktur,
- Geometriegruppe,
- Transformation,
- Koordinatentupel.

Die Typen werden additiv in das allgemeine Anschlussregister aufgenommen. `Geometrieobjekt` ist kein Untertyp von `Menge`.

### Knotenfamilien

#### Räume und Koordinaten

- `mathematik.geometrie.raum`
- `mathematik.geometrie.standardKoordinatensystem`
- `mathematik.geometrie.punktFrei`
- `mathematik.geometrie.punktAusKoordinaten`
- `mathematik.geometrie.koordinatenEinesPunktes`

#### Konstruktionen

- `mathematik.geometrie.geradeDurchPunkte`
- `mathematik.geometrie.strecke`
- `mathematik.geometrie.strahl`
- `mathematik.geometrie.winkel`
- `mathematik.geometrie.kreisMittelpunktPunkt`
- `mathematik.geometrie.kreisMittelpunktRadius`
- `mathematik.geometrie.dreieck`
- `mathematik.geometrie.polygon`
- `mathematik.geometrie.gruppe`

#### Relationen

- Inzidenz,
- Zwischen,
- Kollinearität,
- Parallelität,
- Orthogonalität,
- geometrische Gleichheit,
- Streckenkongruenz,
- Winkelkongruenz.

#### Schnitt und Ableitungen

- Geradenschnitt,
- Gerade-Ebene-Schnitt,
- Ebenenschnitt,
- Mittelpunkt,
- Parallele durch Punkt,
- Senkrechte beziehungsweise Lot,
- Punktprojektion.

#### Repräsentationen

- Geometrie zu Struktur,
- Struktur zu Geometrie,
- Geometrie zu Trägermenge,
- Geometrie zu Koordinatenbild,
- Zellen einer Dimension.

#### Transformationen

- Spaltenvektor zu Tupel,
- Zeilenvektor zu Tupel,
- lineare Punkttransformation,
- affine Punkttransformation,
- Geometrie transformieren,
- Geometrie projizieren,
- Transformationen komponieren,
- Transformation invertieren.

### Bedingte Ausgänge

Sonderfälle werden als getrennte Aussageausgänge geführt. Beispiele:

- `punkteIdentisch`,
- `kollinear`,
- `parallel`,
- `identisch`,
- `windschief`,
- `radiusNull`,
- `radiusNegativ`,
- `dimensionPasst`,
- `invertierbar`.

## Architekturentscheidungen

- Vorlagen und Auswerter werden in fachlich getrennte Geometriedateien ausgelagert.
- Das bestehende zentrale Register bindet diese Dateien additiv ein.
- Stabile technische IDs verwenden ausschließlich ASCII-Komponenten, zum Beispiel `mathematik.geometrie.geradeDurchPunkte`.
- Handle-IDs werden aus stabilen technischen Namen erzeugt, nicht aus übersetzten Labels.
- Renderer zeigen LaTeX aus dem Auswertungsergebnis und berechnen keine Geometrie selbst.
- Ein Geometrieobjekt kann nicht direkt mit einem Mengeneingang verbunden werden.

## Betroffene Dateien und Symbole

Neue Dateien voraussichtlich:

```text
MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/geometrie/
├── MathematikGeometrieAnschlussArten.kt
├── MathematikGeometrieVorlagen.kt
├── GeometrieAuswerter.kt
├── GeometrieKnotenRenderer.kt
└── GeometrieKonfiguration.kt
```

Additiv betroffen:

- `MathematikAnschlussArten.kt`,
- `MathematikKnotenVorlagen.kt`,
- `MathematikAuswerter.kt`,
- `AtlasZustand.kt`,
- `KnotenInspektoren.kt`.

## Meilensteine

- [ ] Anschlussartenregister.
- [ ] Raum- und Koordinatenknoten.
- [ ] Grundkonstruktionen.
- [ ] Relations- und Schnittknoten.
- [ ] Struktur- und Mengenkonvertierung.
- [ ] Transformationsknoten.
- [ ] Inspector und Renderer.
- [ ] Registry-, Handle- und Persistenztests.

## Konkrete Umsetzungsschritte

1. Branch nach Kern- und Transformationsmerge auf den aktuellen Integrationsstand bringen.
2. Geometrieanschlussarten hierarchisch definieren.
3. Vorlagen pro Fachfamilie erstellen.
4. Auswerter in `GeometrieAuswerter` registrieren.
5. Sonderfälle als `BedingterWert` und Aussageausgänge weitergeben.
6. Katalog und allgemeines Register additiv erweitern.
7. Inspectoroptionen nur für handle-stabile Varianten ergänzen.
8. LaTeX- und Kurzrenderer ergänzen.
9. dynamische Polygon- und Gruppenanschlüsse testen.
10. JSON-Roundtrip vorhandener und neuer Knotentypen prüfen.

## Tests und Validierung

Mindestens prüfen:

- jede Vorlage erzeugt stabile Node-Art und fachlich korrekte Anschlüsse,
- Geometrieanschlüsse sind untereinander hierarchisch kompatibel,
- Geometrie ist nicht direkt mit Menge kompatibel,
- explizite Konvertierung liefert einen Mengenausgang,
- Punktkoordinaten akzeptieren Koordinatentupel und keine beliebigen Tupel,
- Konstruktionen geben Bedingungen und Sonderfälle korrekt aus,
- Polygon- und Gruppeneingänge behalten Reihenfolge,
- Inspectoränderungen überleben Undo/Redo und JSON-Roundtrip,
- Registry enthält jeden Node genau einmal,
- bestehende v2.1.x-Knoten bleiben unverändert auswertbar.

Auszuführen:

```text
./gradlew :MathematikKnoten:test
./gradlew :MathematikKartenAdapter:test
./gradlew :app:test
python3 scripts/pruefe_repository.py
```

## Persistenz und Migration

- neue Node-Arten verwenden vorhandenes Kartenformat 2, sofern kein allgemeiner neuer Datentyp nötig ist,
- Eigenschaften bleiben rekursiv serialisierbare `KnotenEigenschaft`-Werte,
- bestehende Karten erhalten keine Geometrieanschlüsse automatisch,
- keine Migration der Mengenvisualisierung,
- unbekannte Geometrieknoten werden wie andere unbekannte Node-Typen behandelt.

## Risiken und Rückfallstrategie

Risiken:

- Konflikte in den großen zentralen Listen,
- instabile Handle-IDs durch Varianten,
- doppelte Semantik in Renderer und Auswerter,
- zu allgemeine Anschlussarten.

Gegenmaßnahmen:

- fachlich getrennte Registerdateien,
- additive Einbindung,
- getrennte Node-Typen bei verschiedenen Handles,
- Renderer ausschließlich aus Ergebnisdaten,
- konkrete Untertypen und Verbindungstests.

Die Branch bleibt bis zur vollständigen Registry- und Persistenzprüfung unvermischt.

## Fortschritt

- 2026-07-27: Branch aus der Planungsintegration angelegt.
- 2026-07-27: ExecPlan erstellt.

## Entscheidungsprotokoll

Branchspezifisch gilt: Ein Wechsel der Konstruktorart darf niemals bestehende Handles heimlich austauschen. Unterschiedliche Konstruktoren sind unterschiedliche Node-Typen.

## Abweichungen vom ursprünglichen Plan

Noch keine.

## Ergebnis und Verifikation

Noch keine Implementierung und keine für diese Branch ausgeführten Tests.

## Mergevertrag

Diese Branch wird nach Kern und Transformationen in `release/v2.2.0-geometrie` gemergt. Konflikte in `MathematikAnschlussArten.kt`, `MathematikKnotenVorlagen.kt` und `MathematikAuswerter.kt` müssen additiv gelöst werden. Kein v2.1.x-Knoten darf durch eine ältere Gesamtliste verschwinden.
