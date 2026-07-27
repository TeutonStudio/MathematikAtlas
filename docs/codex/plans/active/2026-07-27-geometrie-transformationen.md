# Geometrietransformationen für v2.2.0 – ExecPlan

Status: geplant; Branch `feature/v2.2.0-geometrie-transformationen`.

## Ziel und Nutzerwirkung

Diese Branch implementiert die explizite Brücke zwischen Koordinatentupeln und linearer Algebra sowie lineare, affine und projektive Transformationen geometrischer Daten. Ein Punkt bleibt fachlich ein Punkt beziehungsweise analytisch ein Tupel; für Matrixoperationen wird er ausdrücklich in einen Spaltenvektor überführt und anschließend wieder in ein Tupel zurückverwandelt.

## Nicht-Ziele

- keine Definition der grundlegenden Geometrieobjekte, außer kleineren Integrationsanpassungen nach Merge der Kernbranch,
- keine UI oder Knotenvorlagen,
- keine Kameratransformationen des Renderers,
- keine stillschweigende Behandlung affiner Transformationen als lineare Transformationen,
- keine Aufhebung der Zeilen-/Spaltenvektor-Unterscheidung.

## Untersuchte Ist-Situation

- `Tupel` ist ein allgemeines geordnetes mathematisches Objekt.
- `SpaltenVektor` und `ZeilenVektor` sind getrennte Typen.
- `Matrix` unterstützt Matrixaddition, Matrix-Matrix-Produkt, Transposition und rationale Inversion.
- Matrix-mal-Spaltenvektor fehlt.
- Die Geometriekernbranch liefert nach ihrem Merge Räume, Koordinatenbilder, Zellstrukturen und geometrische Objekte.

## Fachliche und mathematische Semantik

### Tupel und Vektoren

Für ein Zahlentupel

```text
p = (x1, ..., xn)
```

ist die lineare Punktabbildung durch eine Matrix `A` definiert als

```text
tupel(A * spalte(p)).
```

Die Konvertierung ist Teil der Operation und kein Identitätsnachweis zwischen Punkt und Vektor.

### Lineare Transformation

Für `A` mit `m` Zeilen und `n` Spalten:

```text
A : K^n -> K^m
```

Der Eingabepunkt benötigt `n` Koordinaten, der Ausgabepunkt besitzt `m` Koordinaten.

### Affine Transformation

```text
T(x) = A*x + b
```

`b` ist ein Koordinatentupel der Zieldimension. Eine reine Translation ist affin, aber nicht linear.

### Homogene und projektive Transformation

Eine spätere beziehungsweise optionale v2.2.0-Komponente darf homogene Koordinaten verwenden. Sie muss jedoch einen eigenen Typ beziehungsweise eine eigene Funktion besitzen und darf die lineare Transformation nicht umdeuten.

### Transformation geometrischer Strukturen

- 0-Zellen werden über ihre Koordinatentupel transformiert.
- Inzidenz- und Randverweise bleiben strukturell erhalten.
- Höhere Zellen werden aus transformierten Randzellen rekonstruiert.
- Der konkrete geometrische Typ darf sich ändern: Ein Kreis kann unter allgemeiner linearer Abbildung eine Ellipse oder ein entarteter Kegelschnitt werden.

## Datenvertrag

Vorgesehene Kernfunktionen und Typen:

- `Tupel.alsZahlenTupel`,
- `Tupel.alsSpaltenVektor`,
- `SpaltenVektor.alsTupel`,
- `ZeilenVektor.alsTupel`,
- `Matrix.times(SpaltenVektor)`,
- `LinearePunktTransformation`,
- `AffinePunktTransformation`,
- `GeometrischeTransformation`,
- `TransformationsKomposition`,
- `TransformationsInverse`,
- `GeometrieProjektion`.

## Architekturentscheidungen

- Konvertierungen liegen im Rechenkern und sind vollständig testbar.
- Tupelvalidierung akzeptiert ausschließlich `ZahlAusdruck`-Komponenten.
- Transformationsobjekte speichern Quell- und Zielraum beziehungsweise deren Dimensionen.
- Isometrie, orthogonale Transformation und allgemeine affine Transformation werden als Eigenschaften oder spezialisierte Typen unterscheidbar.
- Renderkameras verwenden diese Typen nicht automatisch; fachliche Transformation und Ansichtstransformation bleiben getrennt.

## Betroffene Dateien und Symbole

Voraussichtlich:

- `LineareAlgebra.kt`,
- neue Dateien unter `kern/geometrie/transformation/`,
- Typen aus der Geometriekernbranch,
- neue Tests im Rechenkern.

Der `V2_2_MERGE_NOTE` in `LineareAlgebra.kt` ist verbindlich zu beachten und nach erfolgreicher Integration in dauerhafte KDoc oder eine klare Typstruktur zu überführen.

## Meilensteine

- [ ] sichere Tupel-Zahl-Konvertierung,
- [ ] Vektor-Tupel-Konvertierungen,
- [ ] Matrix-mal-Spaltenvektor,
- [ ] lineare Punkttransformation,
- [ ] affine Punkttransformation,
- [ ] geometrische Transformation von Zellstrukturen,
- [ ] Projektion höherer Dimensionen,
- [ ] Komposition und Inversion,
- [ ] vollständige Tests.

## Konkrete Umsetzungsschritte

1. Nach Merge der Kernbranch auf den aktuellen Integrationsstand wechseln.
2. Zahlenvalidierung für Tupel zentral implementieren.
3. explizite Konvertierungen zwischen Tupel und orientierten Vektoren ergänzen.
4. Matrix-mal-Spaltenvektor mit exakter symbolischer Multiplikation implementieren.
5. linearen Transformationsvertrag mit Quell- und Zieldimension modellieren.
6. affine Transformation mit Translationskomponente implementieren.
7. Eigenschaften `invertierbar`, `dimensionErhalten`, `orientierungErhalten`, `längenErhalten` und `winkelErhalten` soweit sicher entscheidbar modellieren.
8. Zellstruktur rekursiv beziehungsweise stufenweise transformieren.
9. Projektion nach R1, R2 oder R3 als explizite Transformation implementieren.
10. Komposition und Inversion mit Dimensionsprüfung ergänzen.

## Tests und Validierung

Mindestens prüfen:

- Tupel mit Nicht-Zahl-Komponente wird abgewiesen,
- `SpaltenVektor -> Tupel -> SpaltenVektor` erhält Reihenfolge und Werte,
- Zeilenvektor und Spaltenvektor bleiben unterschiedliche Typen,
- Matrix-mal-Spaltenvektor liefert korrekte Dimension,
- unpassende Dimensionen erzeugen einen fachlich klaren Fehler,
- Identitätsmatrix erhält das Tupel,
- allgemeine `m x n`-Matrix ändert die Dimension korrekt,
- affine Translation mit `A = I` verschiebt den Punkt um `b`,
- Kompositionsreihenfolge ist korrekt,
- Inversion funktioniert nur für nachweisbar invertierbare Transformationen,
- Zellverweise bleiben nach Transformation konsistent,
- Projektion von 4D nach 3D erzeugt ein 3D-Koordinatenbild und keine direkte 4D-Darstellung.

Auszuführen:

```text
./gradlew :MathematikRechenSystem:test
python3 scripts/pruefe_repository.py
```

## Persistenz und Migration

Keine direkte Kartenpersistenz. Transformationswerte werden später über Knotenkonfigurationen rekonstruiert. Es werden keine transformierten Rendernetze persistiert.

## Risiken und Rückfallstrategie

Risiken:

- Verwechslung fachlicher Punkttransformation mit Kameratransformation,
- falsche Multiplikationsreihenfolge,
- unklare Typänderung eines transformierten Objekts,
- unkontrollierte Verwendung homogener Koordinaten.

Gegenmaßnahmen:

- getrennte Typen,
- Tests mit nichtkommutierenden Matrizen,
- allgemeines Bildobjekt bei nicht erhaltener konkreter Objektart,
- homogene Transformation nur mit eigenem Vertrag.

Bei Fehlschlag bleibt die Branch unvermischt; die Kernbranch kann unabhängig integriert bleiben.

## Fortschritt

- 2026-07-27: Branch aus der Planungsintegration angelegt.
- 2026-07-27: ExecPlan erstellt.

## Entscheidungsprotokoll

Branchspezifische Entscheidung: Eine lineare Punkttransformation verwendet intern einen Spaltenvektor, gibt aber wieder ein Tupel beziehungsweise einen Punkt aus. Der interne Rechenschritt bestimmt nicht den fachlichen Typ.

## Abweichungen vom ursprünglichen Plan

Noch keine.

## Ergebnis und Verifikation

Noch keine Implementierung und keine für diese Branch ausgeführten Tests.

## Mergevertrag

Diese Branch darf erst nach der Kernbranch in `release/v2.2.0-geometrie` gemergt werden. Vor Beginn der Implementierung ist sie auf den integrierten Kernstand zu bringen. Vor Merge müssen Rechenkern-Tests, Dimensionsgrenzfälle und Kompositionsreihenfolge nachgewiesen sein.
