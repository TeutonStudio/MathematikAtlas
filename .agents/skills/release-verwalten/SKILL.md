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

## Ablauf

### 1. Zustand prüfen

- Ermittle `master`-HEAD und den letzten finalen Versionscommit.
- Lies offene Pull Requests mit Basis- und Headbranch.
- Vergleiche die Fakten mit `release/roadmap.toml`.
- Führe `python3 scripts/pruefe_releaseplan.py` aus.
- Führe `python3 scripts/pruefe_versionsfolge.py` aus, sofern ein Git-Checkout vorliegt.

Bei Widersprüchen darf keine neue Version begonnen werden. Erstelle stattdessen einen Reparaturplan mit den betroffenen Versionen, Branches und PRs.

### 2. Version reservieren

- Verwende ausschließlich die nächste zulässige SemVer-Version.
- Trage Titel, Roadmap-Phase, Vorgängerrelease, Branch und Status in `release/roadmap.toml` ein.
- Lege `release/v<version>-<kurzname>` vom veröffentlichten Vorgänger an.
- Subbranches heißen `agent/v<version>/<aufgabe>` und zielen auf den Releasebranch.

### 3. Arbeit integrieren

- Integriere Subbranches nur in den zugehörigen Releasebranch.
- Aktualisiere den Releasebranch auf den aktuellen `master`, bevor der Abschluss geprüft wird.
- Prüfe vollständigen Diff, Migrationen, Tests, Build und Release-Metadaten.
- Der PR gegen `master` enthält genau eine Version.

### 4. Veröffentlichen

- Verwende Squash-Merge.
- Der finale Commit-Titel ist exakt `v<version>`.
- Der Commit-Text beschreibt Verhalten, zentrale Änderungen und Prüfungen.
- Aktualisiere Android-`versionName` und `versionCode` gemeinsam mit dem Release.
- Eine Version gilt erst als veröffentlicht, wenn der finale Commit auf `master` liegt und die Release-Prüfungen grün sind.

## Stopbedingungen

Stoppe den Merge bei:

- falscher oder veralteter Branchbasis,
- aktiver niedrigerer Version,
- mehreren Versionsnummern in einem Release-PR,
- abweichender Android-Version,
- fehlenden oder roten Prüfungen,
- unbeabsichtigten Änderungen im Abschlussdiff,
- ungeklärten Konflikten zwischen Releaseplan und Git-Historie.
