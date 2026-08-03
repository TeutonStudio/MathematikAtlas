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
| Autor-, Committer- und SamAI-Branchregeln | `docs/codex/GIT_IDENTITY.md` |

Widersprechen sich diese Quellen, ist der Releasezustand inkonsistent. Eine neue Version wird erst reserviert, nachdem der Widerspruch geklärt wurde.

## Branch- und Veröffentlichungsmodell

- Releasebranch: `release/v<version>-<kurzname>`
- durch SamAI erzeugter Subbranch: `samai/v<version>/<aufgabe>`
- kleiner eigenständiger SamAI-Release: `samai/v<version>-<kurzname>`
- Reparaturbranch: `repair/v<version>/<aufgabe>`
- pro Pull Request gegen `master` genau eine Version
- Veröffentlichung per Squash-Merge
- finaler Commit-Titel auf `master`: exakt `v<version>`

Historische `agent/`-Branches bleiben erhalten, werden von SamAI aber nicht mehr neu erzeugt. Ein Branch besitzt technisch keinen Autor; der Präfix `samai/` kennzeichnet deshalb seine Herkunft.

## SamAI-Commits

Durch SamAI lokal erzeugte Commits werden über `bash scripts/samai-git.sh` erstellt und besitzen identische Autor- und Committerdaten:

```text
SamAI <46108494+TeutonStudio@users.noreply.github.com>
```

Vor dem Push muss `bash scripts/samai-git.sh verify HEAD` erfolgreich sein. Connector- oder GitHub-Merge-Commits können technisch andere Committer wie `web-flow` besitzen und dürfen nicht als lokal signierte SamAI-Commits bezeichnet werden.

Dokumentations- und Governance-Änderungen sind normalerweise eine `x`-Version, solange sie keinen neuen registrierten Knotentyp einführen.
