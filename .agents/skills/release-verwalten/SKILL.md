---
name: release-verwalten
description: Prüft und verwaltet Versionsfolge, Releasebranches, Subbranches und die Integration nach master.
---

# Release verwalten

Verwende diesen Skill vor jeder Arbeit, die eine neue Versionsnummer beansprucht, einen Releasebranch anlegt, einen Pull Request nach `master` richtet oder einen Versionscommit veröffentlicht.

## Verbindliche Quellen

1. `AGENTS.md`
2. `release/roadmap.toml`
3. `docs/codex/RELEASE_WORKFLOW.md`
4. tatsächlicher Git- und Pull-Request-Zustand
5. `.github/workflows/release-guard.yml`

## Versionsklassifikation

Das Projekt verwendet `vM.y.x` mit eigener Bedeutung:

- `M`: ausdrücklich geplanter fachlicher oder technischer Versionsraum,
- `y`: Release mit mindestens einem neuen, eigenständig registrierten und separat erzeugbaren Knotentyp oder einer neuen Knotenfamilie,
- `x`: Release ohne neue Knotentypen, etwa für Fehlerkorrekturen, UI, Refactoring, Dokumentation oder Erweiterungen vorhandener Knoten.

Bestimme die Versionsachse vor jeder Reservierung aus dem vollständigen geplanten Umfang:

1. Mindestens ein neuer Knotentyp: `y` um eins erhöhen und `x` auf `0` setzen.
2. Kein neuer Knotentyp: `M` und `y` beibehalten und nur `x` erhöhen.
3. Neue Knoten zusammen mit sonstigen Änderungen: insgesamt eine `y`-Version; die anderen Änderungen erzeugen keinen zusätzlichen `x`-Schritt.
4. Zusätzliche Anschlüsse, Parameter, Inspector-Felder, Renderer oder Sonderfälle vorhandener Knotentypen zählen allein als `x`-Änderung.
5. `M` darf nur durch eine ausdrückliche Roadmap-Entscheidung wechseln.
6. Historische Releases werden nicht nachträglich umnummeriert.

Prüfe diese Klassifikation vor dem Abschluss erneut gegen den tatsächlichen Diff. Ein als `x` reservierter Release darf keinen neuen registrierten Knotentyp enthalten.

## Ablauf

### 1. Zustand prüfen

- Ermittle `master`-HEAD und den letzten finalen Versionscommit.
- Lies offene Pull Requests mit Basis- und Headbranch.
- Vergleiche die Fakten mit `release/roadmap.toml`.
- Ermittle anhand von Auftrag, Plan und erwarteten Typ-Schlüsseln, ob `M`, `y` oder `x` betroffen ist.
- Führe `python3 scripts/pruefe_releaseplan.py` aus.
- Führe `python3 scripts/pruefe_versionsfolge.py` aus, sofern ein Git-Checkout vorliegt.

Bei Widersprüchen darf keine neue Version begonnen werden. Erstelle stattdessen einen Reparaturplan mit den betroffenen Versionen, Branches und PRs.

### 2. Version reservieren

- Verwende ausschließlich die nach der Versionsklassifikation nächste zulässige Version.
- Trage Titel, Roadmap-Phase, Vorgängerrelease, Branch und Status in `release/roadmap.toml` ein.
- Dokumentiere bei `y`-Versionen die geplanten neuen Typ-Schlüssel oder Knotenfamilien im Releaseplan oder zugehörigen ExecPlan.
- Lege `release/v<version>-<kurzname>` vom veröffentlichten Vorgänger an.
- Subbranches heißen `agent/v<version>/<aufgabe>` und zielen auf den Releasebranch.

### 3. Arbeit integrieren

- Integriere Subbranches nur in den zugehörigen Releasebranch.
- Aktualisiere den Releasebranch auf den aktuellen `master`, bevor der Abschluss geprüft wird.
- Prüfe vollständigen Diff, Migrationen, Tests, Build und Release-Metadaten.
- Prüfe erneut, ob der Diff neue registrierte Knotentypen enthält und zur reservierten `y`- oder `x`-Version passt.
- Der PR gegen `master` enthält genau eine Version.

### 4. Veröffentlichen

- Verwende Squash-Merge.
- Der finale Commit-Titel ist exakt `v<version>`.
- Der Commit-Text beschreibt Verhalten, zentrale Änderungen, Versionsklassifikation und Prüfungen.
- Aktualisiere Android-`versionName` und `versionCode` gemeinsam mit dem Release.
- Eine Version gilt erst als veröffentlicht, wenn der finale Commit auf `master` liegt und die Release-Prüfungen grün sind.

## Stopbedingungen

Stoppe den Merge bei:

- falscher oder veralteter Branchbasis,
- aktiver niedrigerer Version,
- mehreren Versionsnummern in einem Release-PR,
- einem neuen Knotentyp in einer reservierten `x`-Version,
- einer `y`-Version ohne den geplanten neuen Knotentyp,
- abweichender Android-Version,
- fehlenden oder roten Prüfungen,
- unbeabsichtigten Änderungen im Abschlussdiff,
- ungeklärten Konflikten zwischen Releaseplan und Git-Historie.