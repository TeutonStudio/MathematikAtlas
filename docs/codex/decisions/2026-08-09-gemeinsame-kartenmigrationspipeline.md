# ADR: Gemeinsame mathematische Kartenmigrationspipeline

## Status

Angenommen als zweite Architekturphase des noch unveröffentlichten `v2.32.1`-Refactors.

## Kontext

Der fachneutrale JSON-Codec liegt bereits in `KnotenKartenVerwalter`. Die mathematische Normalisierung war jedoch auf mehrere Plattformpfade verteilt: Androids `KartenJson` und `KartenSpeicher` führten zusätzliche Migrationen aus, während Desktop den neutralen Codec unmittelbar verwendete. Zwei rein mathematische Migrationen lagen zudem im Android-App-Modul.

## Entscheidung

- `KartenDatenJson` bleibt fachneutral und kennt keine Mathematikknoten.
- `MathematikKartenCodec` in `MathematikKnoten` ist die kanonische mathematische Serialisierungsfassade.
- Android und Desktop verwenden diese Fassade.
- Mathematische Migrationen werden in `MathematikKnoten/migration` definiert.
- Die bisherige Zweiphasigkeit bleibt erhalten: `lese` führt nur dekodiernahe Migrationen aus; `lade` und `importiere` ergänzen historische Methoden-/Rechnerkonsolidierungen.
- Plattformmodule dürfen Dateisystem, Backups, Ordner, Papierkorb und Dateidialoge besitzen, aber keine eigene mathematische Migrationskette.

## Konsequenzen

Android und Desktop interpretieren dasselbe Karten-JSON nach denselben mathematischen Regeln. Migrationsreihenfolgen sind zentral sichtbar und testbar. Der neutrale Graphcodec bleibt weiterhin ohne Mathematikabhängigkeit.

## Nicht entschieden

Die physische Vereinheitlichung der Android- und Desktop-Dateispeicher selbst ist nicht Bestandteil dieser Entscheidung. Dateisystem- und Bibliotheksfunktionen bleiben plattformspezifisch, solange ihre Verträge noch unterschiedlich sind.
