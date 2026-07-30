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

Der Master-Verwalter darf Release-Metadaten, Branches, PR-Basen und Integrationsreihenfolgen verwalten. Er klassifiziert jeden Release als Versionsraum-, Knoten- oder Änderungs-Version, prüft den Abschlussdiff und veröffentlicht nach bestandenen Prüfungen. Er implementiert nicht beiläufig fachliche Produktfunktionen.

### Implementierungsagent

Ein Implementierungsagent arbeitet ausschließlich innerhalb einer reservierten Version und eines zugewiesenen Release- oder Subbranches. Er vergibt keine Versionsnummer und merged nicht nach `master`. Stellt er fest, dass der Diff entgegen der Reservierung einen neuen Knotentyp einführt oder der geplante neue Knotentyp entfällt, stoppt er und meldet die notwendige Neuklassifikation an den `master_verwalter`.

### Verifizierer

Der Verifizierer prüft Diff, Tests, Build, Migrationen, unbeabsichtigte Änderungen und die Übereinstimmung zwischen Versionsklassifikation und tatsächlichem Umfang unabhängig. Ein positives Ergebnis ersetzt nicht die Release-Prüfungen.

## Branchmodell

- Releasebranch: `release/v<version>-<kurzname>`
- Subbranch: `agent/v<version>/<aufgabe>`
- Reparatursubbranch: `repair/v<version>/<aufgabe>`

Ein Releasebranch entsteht vom in `previous_release` festgelegten veröffentlichten Stand. Subbranches entstehen vom zugehörigen Releasebranch und werden nur dorthin integriert. Ein versionsübergreifender Branchstapel ist unzulässig.

Für kleine, in sich geschlossene Releases darf ein einzelner `agent/v<version>-<kurzname>` direkt gegen `master` arbeiten. Auch dann gelten sämtliche Basis-, Plan-, Klassifikations- und Prüfregeln.

## Versionsschema `vM.y.x`

Die Stellen der Versionsnummer besitzen im Mathematik Atlas eine feste Bedeutung:

- **`M` – Versionsraum:** ein größerer fachlicher oder technischer Abschnitt, der ausdrücklich durch die Roadmap festgelegt wird.
- **`y` – Knoten-Version:** ein Release mit mindestens einem neuen, eigenständig registrierten und separat erzeugbaren Knotentyp oder einer neuen Knotenfamilie.
- **`x` – Änderungs-Version:** ein Release ohne neue Knotentypen, etwa für Fehlerkorrekturen, UI, Refactoring, Dokumentation oder Erweiterungen bestehender Knoten.

### Entscheidung der nächsten Version

| Vollständiger Releaseumfang | Nächste Version |
|---|---|
| kein neuer Knotentyp | `vM.y.(x+1)` |
| mindestens ein neuer Knotentyp | `vM.(y+1).0` |
| neue Knotentypen und sonstige Änderungen | `vM.(y+1).0` |
| ausdrücklich beschlossener neuer Versionsraum | `v(M+1).0.0` oder die in der Roadmap festgelegte Startversion |

Ein Knotentyp gilt als neu, wenn er einen neuen Typ-Schlüssel beziehungsweise eine neue Registry- oder Vorlagenregistrierung erhält und vom Nutzer als eigener Knoten erzeugt werden kann. Zusätzliche Anschlüsse, Parameter, Inspector-Felder, Renderer, Auswertungsfälle oder Sonderfälle eines vorhandenen Knotentyps lösen allein keine `y`-Version aus.

Die Klassifikation erfolgt zweimal:

1. vor der Reservierung anhand von Auftrag und ExecPlan,
2. vor der Veröffentlichung anhand des vollständigen Diffs.

Weicht der tatsächliche Diff ab, darf nicht unter der falschen Version veröffentlicht werden. Der `master_verwalter` passt Reservierung, Branch und Releaseplan an oder entfernt den abweichenden Umfang.

Die Regel gilt für künftige Reservierungen. Historische veröffentlichte, übersprungene oder als `superseded` markierte Versionen werden nicht nachträglich umnummeriert.

## Allgemeine Versionsregeln

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
- Releaseumfang als `M`, `y` oder `x` klassifizieren.
- `python3 scripts/pruefe_releaseplan.py` ausführen.
- In einem Checkout zusätzlich `python3 scripts/pruefe_versionsfolge.py` ausführen.

### 2. Reservierung

- Nach Klassifikation nächste zulässige Version bestimmen.
- Releaseeintrag mit `planned` oder `reserved` anlegen.
- Bei einer `y`-Version die geplanten Typ-Schlüssel oder Knotenfamilien im ExecPlan dokumentieren.
- Vorgängerrelease und Branch festlegen.
- Draft-PR eröffnen.

### 3. Implementierung

- Subbranches auf die reservierte Version begrenzen.
- Keine fremden Versionsänderungen übernehmen.
- Keine neuen Knotentypen in eine als `x` klassifizierte Version einschleusen.
- Releaseplan bei geänderten Abhängigkeiten oder geänderter Versionsklassifikation aktualisieren.

### 4. Abschluss

- Releasebranch auf aktuellen `master` aktualisieren.
- Vollständigen Diff prüfen.
- Neue Typ-Schlüssel und Registry-Einträge gegen die reservierte Versionsachse prüfen.
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
- einem neuen registrierten Knotentyp in einer `x`-Version,
- einer `y`-Version ohne den angekündigten neuen Knotentyp,
- abweichender Android-Version,
- fehlerhaftem Releaseplan,
- roten oder fehlenden Pflichtprüfungen,
- ungeklärten Migrationen oder Datenverlustgefahr,
- unbeabsichtigten Änderungen im Abschlussdiff.

## Historische v2.3.x-Abweichung

v2.3.2 bis v2.3.9 wurden auf gestapelten Entwicklungsbranches umgesetzt, jedoch nicht als einzelne Releases nach `master` veröffentlicht. Ihre Arbeit wurde später durch v2.3.11 integriert. Der Releaseplan kennzeichnet diese Nummern deshalb als `superseded`, statt eine nie vorhandene Veröffentlichung zu behaupten.

Auch weitere bereits veröffentlichte v2.3.x-Versionen werden durch die neu präzisierte Bedeutung von `y` und `x` nicht rückwirkend umnummeriert. Die Klassifikationsregel gilt ab ihrer Einführung für neue Reservierungen.