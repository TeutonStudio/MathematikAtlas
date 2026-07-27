# ADR: Geometrierepräsentationen und v2.2.0-Branching

Datum: 2026-07-27

Status: angenommen für die Planung von v2.2.0

## Kontext

Die v2.1.x-Linie wird weiterentwickelt, während das Geometriesystem mehrere Module, neue mathematische Typen, neue Knoten und einen eigenen Visualisierer benötigt. Gleichzeitig unterscheidet der Rechenkern Zeilen- und Spaltenvektoren und besitzt bereits eine persistierte Mengenvisualisierung für R2 und R3.

## Entscheidungen

1. Geometrische Objekte sind `Ausdruck`, aber keine `MengenAusdruck`-Instanzen.
2. Ein geometrisches Objekt kann ausdrücklich in folgende eigenständige Repräsentationen überführt werden:
   - geometrische Trägermenge aus Punkten,
   - Koordinatenbild als Menge von Zahlentupeln,
   - `GeometrieStruktur` aus Zellstufen `C0` bis `Cn`.
3. Punktkoordinaten werden durch Tupel dargestellt. Spaltenvektoren werden nur durch explizite Konvertierung für Matrixoperationen verwendet.
4. Der neue Geometrievisualisierer unterstützt R1, R2 und R3 und erhält einen eigenen Node-Typ sowie ein eigenes Persistenzschema. Die vorhandene Mengenvisualisierung bleibt unverändert.
5. Höherdimensionale Geometrie bleibt im Modell zulässig, benötigt vor der Darstellung jedoch eine explizite Projektion nach R1, R2 oder R3.
6. Die Entwicklung erfolgt auf `release/v2.2.0-geometrie` mit fachlich getrennten Subbranches. Der letzte v2.1.x-Stand wird vor dem finalen Commit kontrolliert in die Integrationsbranch übernommen.

## Alternativen

### Geometrie als Menge von Tupeln

Verworfen, weil damit synthetische Objekte, Inzidenz und Konstruktionen auf eine analytische Darstellung reduziert würden.

### Punkte als Spaltenvektoren

Verworfen, weil Punkte keine algebraische Orientierung besitzen und der bestehende Kern Zeilen- und Spaltenvektoren ausdrücklich unterscheidet.

### Erweiterung des bestehenden Visualisierungsknotens um R1 und Geometrie

Verworfen, weil dadurch bestehende persistierte Karten und die Semantik der Mengenvisualisierung verändert würden.

### Eine einzige Geometriebranch

Verworfen, weil Kern, Knoten, Transformationen und Darstellung dann nur als schwer überprüfbarer Großmerge integrierbar wären.

## Konsequenzen

- Konvertierungen werden als sichtbare Knoten modelliert.
- Registryänderungen erfolgen additiv.
- Zellstruktur ist nicht automatisch die Trägermenge und nicht automatisch ein Rendernetz.
- Renderapproximationen bleiben abgeleitet und werden nicht persistiert.
- Die finale Integration benötigt einen bewussten Merge des letzten v2.1.x-Stands, die Auflösung aller `V2_2_MERGE_NOTE`-Hinweise und eine vollständige unabhängige Verifikation.
