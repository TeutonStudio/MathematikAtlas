# Erweiterungsprogramm: kartesische Räume, typisierte Methoden und Geometrie

Status: teilweise übernommen; Geometrieprototyp durch veröffentlichte v3.3.0 abgelöst

Dieses ExecPlan bündelt die drei Erweiterungspläne. Die Umsetzung erfolgt in unabhängig prüfbaren Phasen: Rechenkern, Knotenintegration, Methodensignaturen und anschließend das Blender-kompatible Geometriemodell.

## Fortschritt

- [x] Repository, Projektkontext, Architektur und relevante Verträge geprüft.
- [x] Kartesischer Potenzraum und Mengen-Knoten-Integration; gezielte Tests bestehen.
- [x] Prädikatsklassifikation für Bool-Einertupel; Filterpfad akzeptiert Einertupel.
- [x] Geometry-Nodes durch die vollständige veröffentlichte v3.3.0-Familie abgedeckt; lokale Input-/Output-Stubs und No-op-Realisierung werden nicht registriert.
- [ ] Effektive typisierte Methodensignaturen.
- [x] Prozedurales Geometry Set und Feld-ASTs liegen seit v3.3.0 unter `kern/prozeduralegeometrie/`; kein paralleles Graph-/Auswertersystem.
- [ ] Versioniertes Austauschformat und Blender-Bridge.

## Entscheidungen

- Kartesische Räume bleiben echte Tupelräume; `M¹` wird nicht zur Grundmenge reduziert.
- Bestehende `Tupelraum`- und Mengenverträge werden erweitert, nicht dupliziert.
- Methodensignaturen werden kontextabhängig abgeleitet; es entstehen keine Anschlussarten pro Signatur.
- Blender-Kompatibilität wird strukturell für einen versionierten gemeinsamen Wertebereich definiert.

## Verifikation

Gezielte Rechenkern- und Knotentests werden vor dem vollständigen Gradle-Testlauf ausgeführt. Die Blender-Bridge folgt erst nach dem Atlas-Geometriekern.

## Abgleich vom 2026-10-15

Der ursprüngliche lokale Prototyp ist vollständig in Git stash und lokal unter `.gradle/local-sync-backup-20261015/local-files.tar.gz` gesichert. Die fehlenden Mengen-, Prädikats- und Bibliotheksänderungen werden auf Basis a789e606 als v3.3.1 übernommen. Effektive Methodensignaturen und Blender-Bridge bleiben unbeauftragte Folgearbeit. Verifikation und Entscheidungen zum Abgleich stehen im Plan `2026-10-15-lokale-aenderungen-abgleichen.md`.
