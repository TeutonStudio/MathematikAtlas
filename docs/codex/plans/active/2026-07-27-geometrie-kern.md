# Geometriekern für v2.2.0 – ExecPlan

Status: geplant; Branch `feature/v2.2.0-geometrie-kern`.

## Ziel und Nutzerwirkung

Diese Branch implementiert ausschließlich das plattformneutrale mathematische Geometriemodell im Modul `MathematikRechenSystem`. Nach Abschluss können geometrische Räume, Grundobjekte, Relationen, Zellstrukturen, Trägermengen und Koordinatenbilder strukturiert dargestellt und getestet werden. Es werden noch keine Knotenvorlagen, Compose-Renderer oder App-Inspectoren hinzugefügt.

## Nicht-Ziele

- keine UI,
- keine Knotenvorlagen oder Anschlussregister,
- keine vollständige Transformationsbibliothek,
- keine Renderapproximation,
- keine direkte Persistenzänderung,
- keine allgemeinen Beweisverfahren über alle Hilbert-Axiome.

## Untersuchte Ist-Situation

- `MathematischesObjekt.kt` trennt `Ausdruck`, `ZahlAusdruck` und `MengenAusdruck`.
- `Mengen.kt` besitzt `Tupel`, `DefinierteMenge` und Mengenrelationen.
- `LineareAlgebra.kt` trennt orientierte Vektoren und Matrizen.
- `Aussage` und `RechenKontext` tragen Entscheidungsstatus und Annahmen.
- Das Rechensystem ist Android- und Compose-frei und muss es bleiben.

## Fachliche und mathematische Semantik

### Typtrennung

```text
GeometrischerAusdruck : Ausdruck
GeometrischerAusdruck !: MengenAusdruck
```

Ein geometrisches Objekt kann durch explizite Operationen liefern:

- `GeometrischeTrägermenge`: Menge geometrischer Punkte,
- `KoordinatenBild`: Menge von Zahlentupeln bezüglich eines Koordinatensystems,
- `GeometrieStruktur`: Zellstufen `C0` bis `Cn` mit orientierten Randverweisen.

### Räume

`EuklidischerRaum` besitzt mindestens:

- stabile fachliche ID,
- Dimension größer oder gleich 1,
- Axiomsystemkennung `HilbertEuklidisch`.

Objekte verschiedener Räume dürfen nur durch ausdrückliche Raumabbildungen kombiniert werden.

### Grundobjekte

Vorgesehen:

- freie und koordinatisierte Punkte,
- freie und konstruierte Geraden,
- Ebenen,
- Strecken,
- Strahlen,
- Winkel,
- Kreislinien und Kreisscheiben,
- Dreiecke und Polygone,
- allgemeine Zellkomplexe.

### Relationen

Als `Aussage` modellieren:

- Inzidenz,
- Zwischenlage,
- Kollinearität,
- Koplanarität,
- Parallelität,
- Orthogonalität,
- Strecken- und Winkelkongruenz,
- geometrische Gleichheit.

Unentscheidbare symbolische Fälle behalten die Aussage und liefern keinen erfundenen Wahrheitswert.

### Zellstruktur

`GeometrieStruktur` enthält:

- Raum,
- Liste von `ZellStufe`, geordnet nach Dimension,
- eindeutige Zell-IDs,
- `GeometrischeZelle` mit Dimension und orientierten Randverweisen,
- optionale intrinsische Geometrie pro Zelle,
- Kennzeichnung `Exakt`, `Symbolisch` oder `PolygonalApproximation`.

Jede Zelle der Dimension `k > 0` darf im Rand ausschließlich Zellen der Dimension `k-1` referenzieren.

## Datenvertrag

Vorgesehene neue Dateien:

```text
MathematikRechenSystem/src/main/kotlin/de/TeutonStudio/MathematikRechenSystem/kern/geometrie/
├── GeometrischerAusdruck.kt
├── EuklidischerRaum.kt
├── Grundobjekte.kt
├── GeradlinigeObjekte.kt
├── KreiseUndGebiete.kt
├── Polygone.kt
├── Relationen.kt
├── Konstruktionen.kt
├── Zellstruktur.kt
├── GeometrischeMengen.kt
└── GeometrieNormalisierung.kt
```

Bestehende Dateien nur additiv ändern:

- `MathematischesObjekt.kt` für den neuen Ausdruckszweig,
- gegebenenfalls zentrale Funktionen für Variablen, Substitution und strukturelle Schlüssel.

## Architekturentscheidungen

- Geometrie bleibt in einem eigenen Unterpaket.
- Keine Abhängigkeit auf `MathematikKnoten`, Adapter, Compose oder Android.
- Geometrische Gleichheit wird nicht durch Kotlin-`equals` ersetzt.
- Koordinaten werden als Tupel von `ZahlAusdruck` behandelt.
- Zellstruktur ist ein explizites mathematisches Objekt und kein UI-Mesh.
- Exakte und approximierte Strukturen müssen unterscheidbar bleiben.

## Betroffene Dateien und Symbole

Zusätzlich zu den neuen Dateien sind voraussichtlich betroffen:

- `MathematischesObjekt.kt`,
- `Mengen.kt` nur für notwendige Integrationen, nicht zur Einordnung von Geometrie als Menge,
- Hilfsfunktionen für freie Variablen und Substitution,
- neue Tests unter `MathematikRechenSystem/src/test/.../geometrie/`.

## Meilensteine

- [ ] Typ- und Raumfundament.
- [ ] Punkt, Gerade, Ebene und geradlinige Objekte.
- [ ] Hilbert-nahe Relationen und bedingte Konstruktionen.
- [ ] Zellstruktur und Validierung.
- [ ] Trägermengen und Koordinatenbilder.
- [ ] Normalisierung und begrenzte geometrische Gleichheit.
- [ ] vollständige Kerntests.
- [ ] mathematische Freigabe.

## Konkrete Umsetzungsschritte

1. `GeometrischerAusdruck` und `EuklidischerRaum` anlegen.
2. Raumgleichheit und gemeinsame-Raum-Prüfung zentral implementieren.
3. Grundobjekte als unveränderliche Datenklassen beziehungsweise versiegelte Interfaces modellieren.
4. Relationen als `Aussage` implementieren.
5. Konstruktionen wie Gerade durch Punkte und Strecke mit expliziten Voraussetzungen modellieren.
6. Zellstruktur, Randverweise und Validierungsfehler implementieren.
7. geometrische Trägermenge und Koordinatenbild als getrennte `MengenAusdruck`-Typen implementieren.
8. freie Variablen und Substitution für neue Typen ergänzen.
9. LaTeX-Ausgaben definieren.
10. Normalisierungen nur für mathematisch sichere Fälle ergänzen.

## Tests und Validierung

Mindestens prüfen:

- geometrische Objekte sind keine `MengenAusdruck`-Instanzen,
- Räume akzeptieren nur positive Dimensionen,
- Operationen über verschiedene Räume schlagen nachvollziehbar fehl,
- Gerade durch `A,B` enthält beide Punkte unter `A != B`,
- `GeradeDurch(A,B)` und `GeradeDurch(B,A)` sind geometrisch gleich,
- Strecke und Strahl behalten Reihenfolge beziehungsweise Orientierung korrekt,
- Zwischenlage impliziert Kollinearität,
- Zellstufen sind lückenlos oder ausdrücklich als partielle Struktur markiert,
- ungültige Randdimensionen werden abgewiesen,
- Trägermenge und Koordinatenbild bleiben verschieden,
- Koordinatenbild enthält Zahlentupel,
- Variablen, Substitution und LaTeX bleiben strukturerhaltend.

Auszuführen:

```text
./gradlew :MathematikRechenSystem:test
python3 scripts/pruefe_repository.py
```

## Persistenz und Migration

Keine direkte Persistenzänderung. Die hier implementierten Ausgabewerte werden zur Laufzeit erzeugt. Persistierbare Knoten folgen erst auf der Knotenbranch.

## Risiken und Rückfallstrategie

Risiken:

- zu breite Basistypen,
- Zirkularität zwischen geometrischen Objekten und Zellstruktur,
- implizite Koordinatenabhängigkeit,
- unvollständige Rekursion in Substitution und Variablenanalyse.

Gegenmaßnahmen:

- kleine Typen mit gerichteten Abhängigkeiten,
- Zellstruktur referenziert Objekte optional, Objekte speichern nicht pauschal ihre Struktur,
- Koordinaten nur über Realisierungstypen,
- rekursive Tests für alle neuen Ausdrucksfamilien.

Bei Scheitern bleibt die Branch unvermischt; die Integrationsbranch behält den Planungsstand.

## Fortschritt

- 2026-07-27: Branch aus `release/v2.2.0-geometrie` angelegt.
- 2026-07-27: ExecPlan erstellt.

## Entscheidungsprotokoll

Die übergreifenden Entscheidungen stehen in `docs/codex/decisions/2026-07-27-geometrie-repraesentationen-und-branching.md`.

Branchspezifisch gilt: Keine Knoten- oder UI-Abhängigkeit darf zur Bequemlichkeit in den Kern gelangen.

## Abweichungen vom ursprünglichen Plan

Noch keine.

## Ergebnis und Verifikation

Noch keine Implementierung und keine für diese Branch ausgeführten Tests.

## Mergevertrag

Diese Branch wird zuerst in `release/v2.2.0-geometrie` gemergt. Vor dem Merge:

- Plan aktualisieren,
- mathematische Freigabe einholen,
- Kerntests ausführen,
- Diff auf Android-/Compose-Importe prüfen.

Nach dem Merge müssen die Transformations-, Knoten- und Visualisierungsbranches ihren Stand auf die aktualisierte Integrationsbranch bringen.
