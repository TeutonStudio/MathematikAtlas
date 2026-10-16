---
name: release-verwalten
description: Prüft und verwaltet Versionsfolge, Releasebranches, SamAI-Branches und die Integration nach master.
---

# Release verwalten

Verwende diesen Skill vor jeder Arbeit, die eine neue Versionsnummer beansprucht, einen Release- oder SamAI-Branch anlegt, einen Pull Request nach `master` richtet oder einen Versionscommit veröffentlicht.

## Verbindliche Quellen

1. `AGENTS.md`
2. `release/roadmap.toml`
3. `docs/codex/RELEASE_WORKFLOW.md`
4. `docs/codex/GIT_IDENTITY.md`
5. tatsächlicher Git- und Pull-Request-Zustand
6. `.github/workflows/release-guard.yml`

## Versionsklassifikation

Das Projekt verwendet `vM.y.x` mit eigener Bedeutung:

- `M`: ausdrücklich geplanter fachlicher oder technischer Versionsraum,
- `y`: Release mit mindestens einem neuen, eigenständig registrierten und separat erzeugbaren Knotentyp oder einer neuen Knotenfamilie,
- `x`: Release ohne neue Knotentypen, etwa Fehlerkorrekturen, UI, Refactoring, Dokumentation oder Erweiterungen vorhandener Knoten.

Bestimme die Versionsachse vor jeder Reservierung aus dem vollständigen geplanten Umfang:

1. Mindestens ein neuer Knotentyp: `y` erhöhen und `x` auf `0` setzen.
2. Kein neuer Knotentyp: `M` und `y` beibehalten und nur `x` erhöhen.
3. Neue Knoten zusammen mit sonstigen Änderungen: insgesamt eine `y`-Version.
4. Zusätzliche Anschlüsse, Parameter, Inspector-Felder, Renderer oder Sonderfälle vorhandener Knotentypen zählen allein als `x`-Änderung.
5. `M` darf nur durch eine ausdrückliche Roadmap-Entscheidung wechseln.
6. Historische Releases werden nicht nachträglich umnummeriert.

Prüfe die Klassifikation vor dem Abschluss erneut gegen den tatsächlichen Diff.

## Katalog- und Vertragsgate bei Konsolidierung

- Prüfe `release/knotentypen.json` gegen den tatsächlichen kanonischen Erstellen-Katalog und die Android-/Desktop-Katalogtests. Geänderte Labels, Operatoren, Übersetzerziele und entfernte sichtbare Duplikate allein erzeugen keine `y`-Version. Neue eigenständig erzeugbare Typen dürfen nicht als bloße Aliasbereinigung versteckt werden.
- Bei entfernten/ersetzten Typen oder APIs verlange die Verbraucherinventur und Migrationsnachweise aus [Katalog-, Migrations- und Vertragsregeln](../neuer-knoten/references/katalog-und-vertraege.md). Weniger Katalogeinträge beweisen weder tote Auswerter noch verlustfreie Migration. Lade-Kompatibilität und die bewusste Ablehnung alter Mathematik-Wire-Versionen sind getrennte Verträge.
- Ordne Tests und CI dem geprüften Head-SHA zu. `pruefe_repository.py` umfasst Architektur-, Methodenmodell- und Desktop-Shadowprüfungen; `pruefe_releaseplan.py`, `pruefe_versionsfolge.py` im tatsächlichen PR-Kontext und `test_release_guards.py` sichern die Releaseintegration. Bestehende Android-/Desktop-/Kern-Workflows bleiben maßgeblich; keine Gates oder Whitelists zur Bereinigung abschwächen.
- Eine Skill- oder Dokumentationsänderung schließt keinen Produktvertrag. Ein Gesamtabschluss von Methoden-, Tupel- und Formelverträgen benötigt aktualisierte Anforderungsevidenz einschließlich noch offener Legacy-/UI-Pfade. Teilnachweise und historische grüne Läufe sind keine Gesamtfreigabe.
- Prüfe die Dokumentationsausnahme am aktuellen `pruefe_versionsfolge.py`: derzeit nur Markdown unter `docs/` und Root-`README*.md`. Änderungen an `.agents/skills` benötigen daher eine eigene `x`-Version und kohärente Release-/Android-/Desktop-Metadaten; den Guard nicht als Nebenwirkung umgehen.

## Git-Identität

SamAI verwendet für lokale Branches und Commits ausschließlich `bash scripts/samai-git.sh`.

```text
Author:    SamAI <46108494+TeutonStudio@users.noreply.github.com>
Committer: SamAI <46108494+TeutonStudio@users.noreply.github.com>
```

- SamAI-Branches beginnen mit `samai/`.
- Direkte Commits auf `master` sind verboten.
- Vor dem Push muss `bash scripts/samai-git.sh verify HEAD` erfolgreich sein.
- Der GitHub-Connector kann die Identität nicht auf SamAI setzen. Connector-Commits müssen als solche ausgewiesen werden.

## Ablauf

### 1. Zustand prüfen

- Ermittle `master`-HEAD und letzten finalen Versionscommit.
- Lies offene Pull Requests mit Basis- und Headbranch.
- Vergleiche die Fakten mit `release/roadmap.toml`.
- Ermittle anhand von Auftrag, Plan und erwarteten Typ-Schlüsseln die Versionsachse.
- Führe `python3 scripts/pruefe_releaseplan.py` aus.
- Führe `python3 scripts/pruefe_versionsfolge.py` aus, sofern ein Git-Checkout vorliegt.

Bei Widersprüchen darf keine neue Version begonnen werden. Erstelle stattdessen einen Reparaturplan.

### 2. Version reservieren

- Verwende ausschließlich die nach der Klassifikation nächste zulässige Version.
- Trage Titel, Roadmap-Phase, Vorgängerrelease, Branch und Status in `release/roadmap.toml` ein.
- Dokumentiere bei `y`-Versionen die geplanten neuen Typ-Schlüssel oder Knotenfamilien.
- Lege Integrationsbranches als `release/v<version>-<kurzname>` an.
- Lege SamAI-Arbeitsbranches mit `bash scripts/samai-git.sh branch v<version>/<aufgabe> <basis>` an.
- Für einen kleinen eigenständigen Release ist `samai/v<version>-<kurzname>` zulässig.

### 3. Arbeit integrieren

- Integriere Subbranches nur in den zugehörigen Releasebranch.
- Aktualisiere den Releasebranch auf den aktuellen `master`, bevor der Abschluss geprüft wird.
- Prüfe vollständigen Diff, Migrationen, Tests, Build und Release-Metadaten.
- Prüfe erneut Versionsachse und neue Registry-Einträge.
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
- neuem Knotentyp in einer reservierten `x`-Version,
- `y`-Version ohne geplanten neuen Knotentyp,
- abweichender Android-Version,
- fehlenden oder roten Prüfungen,
- unbeabsichtigten Änderungen im Abschlussdiff,
- ungeklärten Konflikten zwischen Releaseplan und Git-Historie,
- einer als SamAI ausgegebenen Änderung mit abweichender Autor- oder Committeridentität.
