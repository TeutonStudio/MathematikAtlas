# v2.2.0 Geometrie – Integration und Verifikation

Status: geplant; Branch `feature/v2.2.0-geometrie-verifikation`.

## Ziel und Nutzerwirkung

Diese Branch führt keine neue Geometriesemantik ein. Sie übernimmt den vollständig integrierten Stand der Fachbranches, synchronisiert ihn mit dem letzten verfügbaren v2.1.x-Stand, prüft alle Merge-Grenzen, schließt Tests und Dokumentation ab und bereitet den finalen Commit `v2.2.0` vor.

## Nicht-Ziele

- keine eigenständige Neugestaltung des Geometriekerns,
- keine Reparaturen ohne dokumentiertes Finding,
- keine pauschale Konfliktauflösung mit „ours“ oder „theirs“,
- kein Überspringen der mathematischen oder unabhängigen Verifikation,
- kein finaler Commit, solange Tests, Persistenz oder Merge-Hinweise offen sind.

## Untersuchte Ist-Situation

Die Branch wurde vom gemeinsamen Planungsstand abgezweigt. Vor tatsächlicher Arbeit muss sie auf den vollständig integrierten Stand von `release/v2.2.0-geometrie` gebracht werden. Die Integrationsbranch basiert anfänglich auf `v2.1.15`, während die v2.1.x-Linie weiterläuft.

## Fachliche und mathematische Prüfpunkte

Die Verifikation muss insbesondere bestätigen:

- geometrische Objekte sind keine Mengen,
- Trägermenge, Koordinatenbild und Zellstruktur sind getrennte explizite Repräsentationen,
- Punktkoordinaten sind Tupel,
- Zeilen- und Spaltenvektoren bleiben getrennt,
- lineare und affine Transformationen sind nicht vermischt,
- geometrische Gleichheit ist nicht Kotlin-Strukturgleichheit,
- Räume und Dimensionen werden geprüft,
- R1/R2/R3-Darstellung verändert keine Mathematik,
- höhere Dimensionen benötigen eine explizite Projektion.

## Integrationsvertrag

### Eingehende Branches

In dieser Reihenfolge müssen bereits in `release/v2.2.0-geometrie` integriert sein:

1. `feature/v2.2.0-geometrie-kern`,
2. `feature/v2.2.0-geometrie-transformationen`,
3. `feature/v2.2.0-geometrie-knoten`,
4. `feature/v2.2.0-geometrie-visualisierung`.

Diese Verifikationsbranch wird erst danach auf den Release-Stand gebracht.

### Synchronisation mit v2.1.x

1. aktuellen Hauptstand und jüngsten v2.1.x-Versionscommit bestimmen,
2. den gewählten Stand dokumentieren,
3. in `release/v2.2.0-geometrie` zusammenführen,
4. Konflikte pro Datei semantisch auflösen,
5. danach diese Verifikationsbranch aktualisieren,
6. vollständige Prüfung auf dem synchronisierten Stand durchführen.

Änderungen aus v2.1.x werden nicht in jede Fachbranch zurückkopiert.

## Architekturentscheidungen

- Findings werden zuerst dokumentiert und danach auf der zuständigen Fachbranch oder einer eng begrenzten Integrationskorrektur behoben.
- Der Verifizierer verändert bei der ersten Prüfung keinen Code.
- Jede Korrektur benötigt einen erneuten vollständigen Prüflauf des betroffenen Bereichs.
- `V2_2_MERGE_NOTE` ist ein temporärer Marker: Jeder Treffer muss vor dem finalen Commit entfernt, in dauerhafte KDoc überführt oder ausdrücklich als weiter gültiger Kommentar begründet werden.

## Betroffene Dateien und Symbole

Gesamtes Repository, mit besonderem Schwerpunkt auf:

- `MathematischesObjekt.kt`,
- `Mengen.kt`,
- `LineareAlgebra.kt`,
- neue Geometriepakete,
- `MathematikAnschlussArten.kt`,
- `MathematikKnotenVorlagen.kt`,
- `MathematikAuswerter.kt`,
- Geometrievisualisierung,
- `AtlasZustand.kt`,
- `KnotenInspektoren.kt`,
- `KartenJson.kt`,
- alle Geometrie- und Persistenztests,
- `CURRENT_STATE.md`, Architektur-, Node- und ADR-Dokumentation.

## Meilensteine

- [ ] alle Fachbranches in der Integrationsbranch vorhanden,
- [ ] letzter v2.1.x-Stand bestimmt und dokumentiert,
- [ ] v2.1.x synchronisiert,
- [ ] Merge-Konflikte semantisch geprüft,
- [ ] alle `V2_2_MERGE_NOTE`-Marker aufgelöst,
- [ ] Rechenkern- und Knotentests bestanden,
- [ ] Persistenz- und Migrationstests bestanden,
- [ ] Debug-Build bestanden,
- [ ] mathematische Verifikation bestanden,
- [ ] unabhängige Node-/Architekturverifikation bestanden,
- [ ] Dokumentation aktualisiert,
- [ ] finaler Commit `v2.2.0` vorbereitet.

## Konkrete Umsetzungsschritte

1. Commitgraph und Diff jeder Fachbranch gegen die Integrationsbranch prüfen.
2. sicherstellen, dass jede Branch nur ihren freigegebenen Umfang verändert hat.
3. letzten v2.1.x-Commit bestimmen und dessen Änderungen seit dem Ausgangscommit analysieren.
4. v2.1.x in die Integrationsbranch mergen.
5. Konfliktdateien einzeln gegen beide Semantiken prüfen.
6. mit Repositorysuche alle `V2_2_MERGE_NOTE`-Marker erfassen.
7. jeden Marker mit Entscheidung und betroffener Datei dokumentieren.
8. vollständige Testmatrix ausführen.
9. gezielte manuelle beziehungsweise strukturelle Prüfungen der Visualisierung durchführen.
10. Persistenzroundtrips alter v2.1.x- und neuer v2.2.0-Karten prüfen.
11. `CURRENT_STATE.md`, `ARCHITEKTUR.md`, Node-Vertrag und abgeschlossene ExecPlans aktualisieren.
12. unabhängige Abschlussverifikation durchführen.
13. nach Freigabe den finalen Integrationscommit mit Nachricht `v2.2.0` erstellen.

## Konfliktprüfmatrix

### Ausdruckshierarchie

Prüfen, dass alle neuen v2.1.x-Ausdruckstypen erhalten bleiben und `GeometrischerAusdruck` als eigener Zweig ergänzt ist.

### Lineare Algebra

Prüfen, dass neue v2.1.x-Matrixfunktionen erhalten bleiben, Matrix-mal-Spaltenvektor additiv ergänzt ist und die Vektororientierung nicht verwischt wurde.

### Anschlussregister

Prüfen, dass die Gesamtliste sämtliche v2.1.x- und v2.2.0-Anschlussarten genau einmal enthält.

### Vorlagen und Auswerter

Prüfen, dass keine neuere v2.1.x-Vorlage oder Registrierung durch eine ältere Liste verloren ging. Geometrieregister sollen fachlich getrennt und additiv eingebunden sein.

### Visualisierung

Prüfen, dass `mathematik.visualisierung` und sein R2/R3-Schema unverändert bleiben und `mathematik.geometrie.visualisierung` ein eigenes R1/R2/R3-Schema besitzt.

## Tests und Validierung

### Automatisierte Prüfungen

```text
python3 scripts/pruefe_repository.py
JAVA_HOME=/home/alex/.gradle/jdks/eclipse_adoptium-17-amd64-linux.2 ./gradlew test
JAVA_HOME=/home/alex/.gradle/jdks/eclipse_adoptium-17-amd64-linux.2 ./gradlew :app:assembleDebug
```

Zusätzlich alle neu eingeführten gezielten Testklassen einzeln ausführen, falls der vollständige Lauf ein Problem nicht ausreichend lokalisiert.

### Alte Karten

- v2.1.x-Karte ohne Geometrie laden,
- bestehende Mengenvisualisierung laden,
- dynamische Anschlüsse und Gruppen prüfen,
- speichern und erneut laden,
- keine Geometrie-Migration oder Umdeutung feststellen.

### Neue Karten

- Raum, Punkte, Gerade und Strecke speichern/laden,
- Koordinatentupel und Transformation speichern/laden,
- Geometriegruppe und Visualisierung speichern/laden,
- stabile Node- und Handle-Referenzen nachweisen.

### Mathematische Grenzfälle

- identische Punkte bei Geradenkonstruktion,
- kollineare Dreieckspunkte,
- parallele, identische und windschiefe Geraden,
- nicht invertierbare Matrix,
- affine Translation,
- 4D-Objekt ohne und mit Projektion,
- Kreis unter nicht-isometrischer Transformation.

### Darstellung

- R1, R2 und R3 je mit mindestens einem vollständigen Beispielgraphen,
- Standardansicht und Kamerasteuerung,
- kein ungewolltes Knotenziehen aus Rendererinteraktionen,
- unveränderte fachliche Ausgabe bei Kameraänderung.

## Persistenz und Migration

- Kartenformat bleibt nur dann Version 2, wenn die vorhandenen rekursiven Eigenschaften vollständig genügen.
- Falls eine Formaterhöhung erforderlich wurde, müssen Leseweg, Defaults, Roundtrip und alte Karten explizit getestet sein.
- bestehende Visualisierungsknoten behalten ihre Bedeutung.
- Renderdaten bleiben unpersistiert.

## Risiken und Rückfallstrategie

Risiken:

- v2.1.x ändert dieselben zentralen Registrierungsstellen,
- ein Fachmerge ist nur gemeinsam mit einem späteren Merge grün,
- alte Karten laden zwar, werden aber semantisch anders interpretiert,
- temporäre Merge-Kommentare verbleiben unbeachtet,
- zu großer finaler Korrekturdiff.

Gegenmaßnahmen:

- Merge und Test nach jeder Fachbranch,
- Vergleich gegen Ausgangscommit und letzten v2.1.x-Commit,
- alte Karten als feste Regressionstests,
- Repositorysuche nach Markern,
- Korrekturen an zuständige Branch zurückgeben.

Rückfallpunkt ist der letzte vollständig grüne Commit auf `release/v2.2.0-geometrie`. Der v2.1.x-Hauptstand wird niemals auf einen unbestätigten v2.2.0-Stand verschoben.

## Fortschritt

- 2026-07-27: Branch aus der Planungsintegration angelegt.
- 2026-07-27: ExecPlan erstellt.

## Entscheidungsprotokoll

Branchspezifisch gilt: Der finale Versionscommit ist kein Sammelplatz für ungeprüfte Reparaturen. Jede fachliche Korrektur muss einem Finding und einem erneut ausgeführten Prüfschritt zugeordnet sein.

## Abweichungen vom ursprünglichen Plan

Noch keine.

## Ergebnis und Verifikation

Noch keine Integration oder Testausführung.

## Abschlusskriterien

Der Commit `v2.2.0` darf erst erstellt werden, wenn:

- alle Fachpläne abgeschlossen und nach `plans/completed/` verschoben sind,
- der letzte v2.1.x-Stand integriert ist,
- keine ungeklärten `V2_2_MERGE_NOTE`-Marker existieren,
- vollständige Tests und Debug-Build erfolgreich sind,
- mathematische und unabhängige Verifikation freigegeben haben,
- der tatsächliche Endzustand dokumentiert ist.
