# Release- und Master-Workflow

## Ziel

`master` enthält ausschließlich vollständig geprüfte, geordnete Veröffentlichungsstände. Eine Versionsnummer bezeichnet genau einen finalen Releasecommit auf `master` und ist keine unverbindliche Beschriftung eines Arbeitsbranches.

## Quellen der Wahrheit

1. Der tatsächliche Git-Zustand entscheidet, was veröffentlicht ist.
2. `release/roadmap.toml` beschreibt Reihenfolge, Status, Roadmap-Zuordnung und Branch einer Version.
3. `app/build.gradle.kts` enthält die zur aktuellen Version passenden Android-Metadaten.
4. GitHub Actions prüft Plan, Metadaten und Pull-Request-Kontext.
5. `docs/codex/GIT_IDENTITY.md` definiert Git-Identitäten und die Herkunft von SamAI-Branches.

Widersprechen sich diese Quellen, ist der Releasezustand inkonsistent. Neue Versionsarbeit bleibt gesperrt, bis der Widerspruch behoben ist.

## Rollen

### `master_verwalter`

Der Master-Verwalter verwaltet Release-Metadaten, Branches, PR-Basen und Integrationsreihenfolgen. Er klassifiziert jeden Release als Versionsraum-, Knoten- oder Änderungs-Version, prüft den Abschlussdiff und veröffentlicht nach bestandenen Prüfungen. Er implementiert nicht beiläufig fachliche Produktfunktionen.

### Implementierungsagent

Ein Implementierungsagent arbeitet ausschließlich innerhalb einer reservierten Version und eines zugewiesenen Release- oder Subbranches. Er vergibt keine Versionsnummer und merged nicht nach `master`. Erkennt er eine falsche Versionsklassifikation, stoppt er und meldet die notwendige Neuklassifikation an den `master_verwalter`.

### Verifizierer

Der Verifizierer prüft Diff, Tests, Build, Migrationen, unbeabsichtigte Änderungen und die Übereinstimmung zwischen Versionsklassifikation und tatsächlichem Umfang unabhängig. Ein positives Ergebnis ersetzt nicht die Release-Prüfungen.

## Branchmodell

- Releasebranch: `release/v<version>-<kurzname>`
- SamAI-Subbranch: `samai/v<version>/<aufgabe>`
- kleiner eigenständiger SamAI-Release: `samai/v<version>-<kurzname>`
- Reparatursubbranch: `repair/v<version>/<aufgabe>`

Historische `agent/v...`-Branches bleiben gültige Historie, werden von SamAI aber nicht mehr neu erzeugt.

Ein Releasebranch entsteht vom in `previous_release` festgelegten veröffentlichten Stand. Subbranches entstehen vom zugehörigen Releasebranch und werden nur dorthin integriert. Ein versionsübergreifender Branchstapel ist unzulässig.

Für kleine, in sich geschlossene Releases darf ein einzelner `samai/v<version>-<kurzname>` direkt gegen `master` arbeiten. Auch dann gelten sämtliche Basis-, Plan-, Klassifikations- und Prüfregeln.

## Git-Identität

SamAI erzeugt lokale Branches und Commits über `bash scripts/samai-git.sh`.

Verbindliche SamAI-Identität:

```text
Author:    SamAI <46108494+TeutonStudio@users.noreply.github.com>
Committer: SamAI <46108494+TeutonStudio@users.noreply.github.com>
```

Vor dem Push muss `bash scripts/samai-git.sh verify HEAD` erfolgreich sein. Das Skript darf nicht durch eine dauerhafte Änderung von `git config user.name` oder `git config user.email` ersetzt werden, weil dadurch spätere manuelle Commits falsch zugeordnet würden.

Der GitHub-Connector stellt derzeit keine Autor- und Committerfelder für erzeugte Commits bereit. Connector-Commits gelten deshalb nicht als korrekt signierte SamAI-Commits und müssen im Abschlussbericht als solche benannt werden. Der Connector bleibt sinnvoll für Issues, Reviews, PR-Metadaten und das Eröffnen bereits lokal erzeugter und gepushter Branches.

## Versionsschema `vM.y.x`

- **`M` – Versionsraum:** ausdrücklich durch die Roadmap festgelegter größerer fachlicher oder technischer Abschnitt.
- **`y` – Knoten-Version:** Release mit mindestens einem neuen, eigenständig registrierten und separat erzeugbaren Knotentyp oder einer neuen Knotenfamilie.
- **`x` – Änderungs-Version:** Release ohne neue Knotentypen, etwa Fehlerkorrektur, UI, Refactoring, Dokumentation oder Erweiterung vorhandener Knoten.

### Entscheidung der nächsten Version

| Vollständiger Releaseumfang | Nächste Version |
|---|---|
| kein neuer Knotentyp | `vM.y.(x+1)` |
| mindestens ein neuer Knotentyp | `vM.(y+1).0` |
| neue Knotentypen und sonstige Änderungen | `vM.(y+1).0` |
| ausdrücklich beschlossener neuer Versionsraum | die in der Roadmap festgelegte nächste `M`-Version |

Ein Knotentyp gilt als neu, wenn er einen neuen Typ-Schlüssel beziehungsweise eine neue Registry- oder Vorlagenregistrierung erhält und vom Nutzer als eigener Knoten erzeugt werden kann. Zusätzliche Anschlüsse, Parameter, Inspector-Felder, Renderer, Auswertungsfälle oder Sonderfälle eines vorhandenen Knotentyps lösen allein keine `y`-Version aus.

Die Klassifikation erfolgt zweimal:

1. vor der Reservierung anhand von Auftrag und ExecPlan,
2. vor der Veröffentlichung anhand des vollständigen Diffs.

Weicht der tatsächliche Diff ab, darf nicht unter der falschen Version veröffentlicht werden. Historische veröffentlichte, übersprungene oder als `superseded` markierte Versionen werden nicht nachträglich umnummeriert.

## Allgemeine Versionsregeln

- Jede veröffentlichte Version besitzt genau einen finalen Commit mit dem Titel `v<version>` auf `master`.
- Jeder PR gegen `master` beansprucht genau eine Version.
- Die nächste Version muss auf dem letzten veröffentlichten Stand basieren.
- Eine niedrigere aktive Version blockiert eine höhere Version.
- Übersprungene Nummern benötigen `skipped` oder `superseded` mit Begründung.
- Arbeitscommits innerhalb eines Branches erhalten keine zusätzlichen Releaseversionen.
- `versionName` entspricht `current_version`.
- `versionCode` wird als `major * 1_000_000 + minor * 1_000 + patch` gebildet.

## Ablauf

### 1. Audit

- `master`-HEAD und finale Versionscommits ermitteln.
- Offene PRs samt Basis- und Headbranch erfassen.
- `release/roadmap.toml` gegen Git vergleichen.
- Releaseumfang als `M`, `y` oder `x` klassifizieren.
- `python3 scripts/pruefe_releaseplan.py` ausführen.
- In einem Checkout zusätzlich `python3 scripts/pruefe_versionsfolge.py` ausführen.

### 2. Reservierung

- Nach Klassifikation nächste zulässige Version bestimmen.
- Releaseeintrag mit vollständiger Basis, Branch und Begründung anlegen.
- Bei einer `y`-Version die geplanten Typ-Schlüssel oder Knotenfamilien dokumentieren.
- Vorgängerrelease und Branch festlegen.
- Draft-PR eröffnen.

### 3. Implementierung

- SamAI-Branches mit `bash scripts/samai-git.sh branch ...` anlegen.
- Nur ausdrücklich ausgewählte Dateien stagen.
- SamAI-Commits mit `bash scripts/samai-git.sh commit ...` erzeugen.
- Subbranches auf die reservierte Version begrenzen.
- Keine fremden Versionsänderungen übernehmen.
- Keine neuen Knotentypen in eine als `x` klassifizierte Version einschleusen.

### 4. Abschluss

- Releasebranch auf aktuellen `master` aktualisieren.
- Vollständigen Diff prüfen.
- Versionsklassifikation gegen Registry- und Typänderungen prüfen.
- Repository-, Architektur-, Test- und Buildprüfungen ausführen.
- Android-Version und Releaseplan abgleichen.
- Für den letzten lokal durch SamAI erzeugten Commit `bash scripts/samai-git.sh verify HEAD` ausführen.
- PR als bereit markieren.

### 5. Veröffentlichung

- Squash-Merge verwenden.
- Finalen Commit `v<version>` nennen.
- Releaseeintrag auf `released` setzen.
- Abgeschlossene Subbranches löschen oder schließen.

Ein durch GitHub erzeugter Squash- oder Merge-Commit kann als Committer `web-flow` enthalten. Er ist technisch ein GitHub-Commit und wird nicht nachträglich als lokaler SamAI-Commit ausgegeben.

## Abbruchbedingungen

Kein Merge bei:

- falscher oder veralteter Branchbasis,
- fehlendem Vorgängerrelease,
- mehreren Releaseversionen im PR,
- neuem registrierten Knotentyp in einer `x`-Version,
- `y`-Version ohne angekündigten neuen Knotentyp,
- abweichender Android-Version,
- fehlerhaftem Releaseplan,
- roten oder fehlenden Pflichtprüfungen,
- ungeklärten Migrationen oder Datenverlustgefahr,
- unbeabsichtigten Änderungen im Abschlussdiff,
- einem als SamAI-Commit behaupteten Commit mit abweichender Autor- oder Committeridentität.

## Bestehende Historie

Historische Commits und `agent/`-Branches werden nicht umgeschrieben. Eine Änderung ihrer Metadaten würde Commit-SHAs, Branches und Pull Requests neu schreiben. Die SamAI-Regel gilt für neu erzeugte Arbeit ab `v2.21.1`.
