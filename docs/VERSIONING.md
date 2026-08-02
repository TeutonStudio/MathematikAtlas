# Versionsschema

Der Mathematik Atlas verwendet das Schema `vM.y.x`. Die drei Stellen besitzen eine projektspezifische Bedeutung und sind nicht bloß klassische Major-, Minor- und Patchnummern.

## Bedeutung

| Stelle | Bezeichnung | Wann sie sich ändert |
|---|---|---|
| `M` | Versionsraum | nur durch eine ausdrücklich beschlossene größere fachliche oder technische Roadmap-Phase |
| `y` | Knoten-Version | wenn mindestens ein neuer, separat erzeugbarer und registrierter Knotentyp oder eine neue Knotenfamilie veröffentlicht wird |
| `x` | Änderungs-Version | wenn kein neuer Knotentyp entsteht, etwa bei Fehlerkorrekturen, UI-Änderungen, Dokumentation, Refactorings oder Erweiterungen vorhandener Knoten |

## Entscheidungsregeln

Ausgehend von `vM.y.x` gilt:

| Releaseumfang | Folgende Version |
|---|---|
| kein neuer Knotentyp | `vM.y.(x+1)` |
| mindestens ein neuer Knotentyp | `vM.(y+1).0` |
| neue Knoten und weitere Änderungen gemeinsam | `vM.(y+1).0` |
| ausdrücklich beschlossener neuer Versionsraum | die in der Roadmap festgelegte nächste `M`-Version |

Ein zusätzlicher Anschluss, ein Inspector-Feld, ein Renderer, ein Parameter oder ein neuer Auswertungsfall eines vorhandenen Knotens ist für sich allein **kein neuer Knotentyp**.

## Quellen der Wahrheit

| Information | Verbindliche Quelle |
|---|---|
| aktuelle Android-Version | `app/build.gradle.kts` |
| veröffentlichte und geplante Releases | `release/roadmap.toml` |
| langfristige Produktphasen | `ROADMAP.md` und `docs/assets/roadmap.svg` |
| finaler Veröffentlichungsstand | tatsächliche Commit-Historie auf `master` |
| Release- und Branchregeln | `docs/codex/RELEASE_WORKFLOW.md` |

Widersprechen sich diese Quellen, ist der Releasezustand inkonsistent. Eine neue Version wird erst reserviert, nachdem der Widerspruch geklärt wurde.

## Branch- und Veröffentlichungsmodell

- Releasebranch: `release/v<version>-<kurzname>`
- Subbranch: `agent/v<version>/<aufgabe>`
- kleiner eigenständiger Release: `agent/v<version>-<kurzname>`
- pro Pull Request gegen `master` genau eine Version
- Veröffentlichung per Squash-Merge
- finaler Commit-Titel auf `master`: exakt `v<version>`

Dokumentationsänderungen sind normalerweise eine `x`-Version, solange sie keinen neuen registrierten Knotentyp einführen.
