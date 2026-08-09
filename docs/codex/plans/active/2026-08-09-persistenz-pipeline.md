# Gemeinsame Karten- und Migrationspipeline

## 1. Status und Basis

**Status:** in Umsetzung auf `samai/v2.32.1/persistenz-pipeline` als zweite Phase des noch unveröffentlichten Architekturrefactors `v2.32.1`.

**Basis:** vollständig verifizierter Releasebranch `release/v2.32.1-architektur-refactor`, Head `9dbe73c35504f251a94ad78e3127102fc3c87c2a`.

**Versionsklassifikation:** Teil von `v2.32.1`; keine zusätzliche Versionsreservierung und keine neuen Knotentypen.

## 2. Ziel

Android und Desktop sollen dieselben mathematischen Regeln zum Schreiben, Dekodieren, Laden und Importieren von Karten verwenden. Der fachneutrale `KartenDatenJson` bleibt unverändert im Graphmodul; mathematische Migrationen werden in `MathematikKnoten` gebündelt.

## 3. Ausgangslage

- Android `KartenJson` besaß eine eigene Kette aus Transpositions-, Tensor-, Hyperanalysis-, Differential- und Integralmigrationen.
- Android `KartenSpeicher` ergänzte beim Laden zusätzlich Methodenanschluss-, Zahlenrechner- und Divisionsmigrationen.
- Transpositions- und Methodenanschlussmigration lagen im `app`-Modul, obwohl sie ausschließlich mathematische Knotendaten transformieren.
- Desktop verwendete `KartenDatenJson` direkt und durchlief diese mathematischen Migrationen nicht vollständig.

## 4. Nicht-Ziele

- keine Änderung des JSON-Formats oder der `formatVersion`,
- keine Änderung stabiler IDs,
- keine Änderung der Reihenfolge bereits vorhandener Migrationen innerhalb ihrer bisherigen Phase,
- keine Zusammenlegung von Dateisystem-, Papierkorb-, Freigabe- oder Ordnerverwaltung,
- keine neue mathematische Semantik.

## 5. Architekturentscheidung

1. `KartenDatenJson` bleibt der reine Graph-Codec.
2. `MathematikKartenCodec` ist die gemeinsame mathematische Fassade für Android und Desktop.
3. Migrationen bleiben in zwei expliziten Phasen:
   - `lese`: schema-/knotennahe Migrationen wie bisher in `KartenJson`,
   - `lade/importiere`: zusätzliche historische Methoden-/Rechnerkonsolidierungen wie bisher im Android-Dateispeicher.
4. Mathematische Migrationen dürfen nicht im Plattformmodul definiert werden.

## 6. Umsetzung

- [x] Transpositionsmigration nach `MathematikKnoten/migration` verschieben.
- [x] Methodenanschlussmigration nach `MathematikKnoten/migration` verschieben.
- [x] zugehörige Tests in das Mathematikknoten-Modul verschieben.
- [x] `MathematikKartenCodec` und `MathematikKartenMigrationen` einführen.
- [x] Android-`KartenJson` auf dünne Fassade reduzieren.
- [x] Android-`KartenSpeicher` auf gemeinsame Lade-/Importpipeline umstellen.
- [x] `DesktopKartenSpeicher` auf denselben Codec umstellen.
- [x] Pipeline-Phasen und Roundtrip regressionsprüfen.
- [x] Architekturprüfung gegen neue app-lokale Migrationen härten.
- [x] Versionsmetadaten bleiben konsistent auf dem bereits reservierten `v2.32.1`.
- [ ] GitHub-Actions-Verifikation auswerten.

## 7. Persistenzvertrag

Die Reihenfolge wird aus dem bisherigen Android-Verhalten übernommen:

### Vor dem Schreiben

1. Tensoroperationen,
2. Hyperanalysis,
3. Differential,
4. Integral.

### Nach dem Dekodieren

1. historische Transpositionsknoten,
2. Tensoroperationen,
3. Hyperanalysis,
4. Differential,
5. Integral.

### Nach dem Laden oder beim Import

1. historische Methodenanschlüsse,
2. universeller Zahlenrechner,
3. strukturierte Division.

Die Phasen sind getrennt, damit ein bloßer JSON-Editor-Roundtrip nicht stillschweigend stärkere Importnormalisierungen erhält.

## 8. Tests

Erforderlich:

- Architekturprüfung,
- bestehende Transpositions- und Methodenanschlussmigrationstests im neuen Modul,
- neuer Test für getrennte `lese`-/`lade`-Phasen,
- kanonischer Roundtrip nach vollständiger Migration,
- vollständige JVM-Tests,
- Android-Debug-Build,
- Linux-Desktop-Tests und Paketierung,
- Release-Guard.

## 9. Risiken

Das größte Risiko ist eine unbeabsichtigte Änderung der Migrationsreihenfolge. Die neue Pipeline übernimmt daher bewusst die vorherigen Reihenfolgen und macht die stärkere Ladephase separat sichtbar.

## 10. Ergebnis

Wird nach CI-Abnahme ergänzt und anschließend in den bestehenden Releasebranch `release/v2.32.1-architektur-refactor` übernommen.
