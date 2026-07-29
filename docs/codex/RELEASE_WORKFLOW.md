# Release- und Master-Workflow

## Ziel

`master` enthält ausschließlich vollständig geprüfte, geordnete Veröffentlichungsstände. Eine Versionsnummer ist keine unverbindliche Beschriftung eines Arbeitsbranches, sondern bezeichnet genau einen finalen Releasecommit auf `master`.

## Quellen der Wahrheit

1. Der tatsächliche Git-Zustand entscheidet, was veröffentlicht ist.
2. `release/roadmap.toml` beschreibt Reihenfolge, Status, Roadmap-Zuordnung und Branch einer Version.
3. `app/build.gradle.kts` enthält die zur aktuellen Version passenden Android-Metadaten.
4. GitHub Actions prüft Plan, Metadaten und Pull-Request-Kontext.

Widersprechen sich diese Quellen, ist der Releasezustand inkonsistent. Neue Versionsarbeit bleibt gesperrt, bis der Widerspruch behoben ist.

## Rollen

### `master_verwalter`

Der Master-Verwalter darf Release-Metadaten, Branches, PR-Basen und Integrationsreihenfolgen verwalten. Er prüft den Abschlussdiff und veröffentlicht nach bestandenen Prüfungen. Er implementiert nicht beiläufig fachliche Produktfunktionen.

### Implementierungsagent

Ein Implementierungsagent arbeitet ausschließlich innerhalb einer reservierten Version und eines zugewiesenen Release- oder Subbranches. Er vergibt keine Versionsnummer und merged nicht nach `master`.

### Verifizierer

Der Verifizierer prüft Diff, Tests, Build, Migrationen und unbeabsichtigte Änderungen unabhängig. Ein positives Ergebnis ersetzt nicht die Release-Prüfungen.

## Branchmodell

- Releasebranch: `release/v<version>-<kurzname>`
- Subbranch: `agent/v<version>/<aufgabe>`
- Reparatursubbranch: `repair/v<version>/<aufgabe>`

Ein Releasebranch entsteht vom in `previous_release` festgelegten veröffentlichten Stand. Subbranches entstehen vom zugehörigen Releasebranch und werden nur dorthin integriert. Ein versionsübergreifender Branchstapel ist unzulässig.

Für kleine, in sich geschlossene Releases darf ein einzelner `agent/v<version>-<kurzname>` direkt gegen `master` arbeiten. Auch dann gelten sämtliche Basis-, Plan- und Prüfregeln.

## Versionsregeln

- Jede veröffentlichte Version besitzt genau einen finalen Commit mit dem Titel `v<version>` auf `master`.
- Jeder PR gegen `master` beansprucht genau eine Version.
- Die nächste Version muss auf dem letzten veröffentlichten Stand basieren.
- Eine niedrigere aktive Version blockiert eine höhere Version.
- Übersprungene Nummern benötigen den Status `skipped` oder `superseded` mit Begründung.
- Arbeitscommits innerhalb eines Branches erhalten keine zusätzlichen Releaseversionen.
- `versionName` entspricht `current_version`.
- `versionCode` wird als `major * 1_000_000 + minor * 1_000 + patch` gebildet.

## Ablauf

### 1. Audit

- `master`-HEAD und finale Versionscommits ermitteln.
- Offene PRs samt Basis- und Headbranch erfassen.
- `release/roadmap.toml` gegen Git vergleichen.
- `python3 scripts/pruefe_releaseplan.py` ausführen.
- In einem Checkout zusätzlich `python3 scripts/pruefe_versionsfolge.py` ausführen.

### 2. Reservierung

- Nächste zulässige Version bestimmen.
- Releaseeintrag mit `planned` oder `reserved` anlegen.
- Vorgängerrelease und Branch festlegen.
- Draft-PR eröffnen.

### 3. Implementierung

- Subbranches auf die reservierte Version begrenzen.
- Keine fremden Versionsänderungen übernehmen.
- Releaseplan bei geänderten Abhängigkeiten aktualisieren.

### 4. Abschluss

- Releasebranch auf aktuellen `master` aktualisieren.
- Vollständigen Diff prüfen.
- Repository-, Architektur-, Test- und Buildprüfungen ausführen.
- Android-Version und Releaseplan abgleichen.
- PR als bereit markieren.

### 5. Veröffentlichung

- Squash-Merge verwenden.
- Finalen Commit `v<version>` nennen.
- Releaseeintrag auf `released` setzen.
- Abgeschlossene Subbranches löschen oder schließen.

## Abbruchbedingungen

Kein Merge bei:

- falscher oder veralteter Branchbasis,
- fehlendem Vorgängerrelease,
- mehreren Releaseversionen im PR,
- abweichender Android-Version,
- fehlerhaftem Releaseplan,
- roten oder fehlenden Pflichtprüfungen,
- ungeklärten Migrationen oder Datenverlustgefahr,
- unbeabsichtigten Änderungen im Abschlussdiff.

## Historische v2.3.x-Abweichung

v2.3.2 bis v2.3.9 wurden auf gestapelten Entwicklungsbranches umgesetzt, jedoch nicht als einzelne Releases nach `master` veröffentlicht. Ihre Arbeit wurde später durch v2.3.11 integriert. Der Releaseplan kennzeichnet diese Nummern deshalb als `superseded`, statt eine nie vorhandene Veröffentlichung zu behaupten.
