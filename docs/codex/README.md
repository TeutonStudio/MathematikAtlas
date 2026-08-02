# Codex-Struktur des Mathematik Atlas

Diese Dateien bilden das versionierte Projektgedächtnis und den Arbeitsablauf für Codex und andere Projektagenten. Sie sind **nicht** der allgemeine Einstieg für Nutzer oder menschliche Mitwirkende.

Öffentliche Einstiege:

- `/README.md`: Produkt und Grundkonzepte
- `/ROADMAP.md`: langfristige Vision
- `/CONTRIBUTING.md`: Beitragsablauf
- `/docs/DEVELOPMENT.md`: Technik und Architektur
- `/docs/VERSIONING.md`: verständliches Versionsschema
- `/docs/TESTKARTEN.md`: eingebaute Beispielkarten

## Zuständigkeiten

| Datei oder Verzeichnis | Zweck |
|---|---|
| `/AGENTS.md` | automatisch geladene Repository-Regeln |
| `/.codex/config.toml` | projektweite Multi-Agent-Konfiguration, sofern lokal benötigt |
| `/.codex/agents/` | Codex-Subagenten einschließlich `master_verwalter` |
| `/.codex/agents/issue-verwalter.toml` | klärt Anforderungen, erstellt Issues und pflegt den kanonischen technischen Lösungsplan |
| `/.agents/skills/neuer-knoten/SKILL.md` | wiederverwendbarer Ablauf für neue Knoten |
| `/.agents/skills/release-verwalten/SKILL.md` | verbindlicher Ablauf für Versionen, Branches und `master`-Integration |
| `/release/roadmap.toml` | maschinenlesbarer Versions- und Releasezustand |
| `/docs/codex/RELEASE_WORKFLOW.md` | Branchmodell, Versionsregeln und Integrationsablauf |
| `/docs/codex/PROJECT_CONTEXT.md` | stabile Produkt- und Begriffsgrundlage |
| `/docs/codex/CURRENT_STATE.md` | zuletzt verifizierter Istzustand |
| `/docs/codex/ARCHITECTURE.md` | Architekturgrenzen und Zielprinzipien |
| `/docs/codex/NODE_CONTRACT.md` | Vertrag für jeden Knotentyp |
| `/docs/codex/PLANS.md` | Anforderungen an ausführbare Pläne |
| `/docs/codex/CODE_REVIEW.md` | Prüfkriterien |
| `/docs/codex/TEST_STRATEGY.md` | Testebenen und Mindestabdeckung |
| `/docs/codex/plans/` | aktive und abgeschlossene ExecPlans |
| `/docs/codex/decisions/` | dauerhafte Architekturentscheidungen |
| `/docs/codex/templates/` | Vorlagen |

## Dokumentationsgrenzen

- Öffentliche Dokumentation erklärt Produkt, Nutzung und Mitwirkung ohne Agentenvorkenntnisse.
- `PROJECT_CONTEXT.md` enthält langfristig stabile technische Fakten.
- `CURRENT_STATE.md` enthält ausschließlich nachweisbaren, datierten Verifikationsstand.
- `release/roadmap.toml` beschreibt den maschinenlesbaren Releasezustand.
- ExecPlans dokumentieren konkrete größere Aufgaben.
- ADRs dokumentieren dauerhafte Entscheidungen und ihre Begründung.
- Flüchtige Sitzungsnotizen gehören nicht in das Projektgedächtnis.

Wenn eine öffentliche Datei und `docs/codex/` denselben Sachverhalt beschreiben, müssen beide auf dieselbe Quelle der Wahrheit verweisen, statt voneinander kopierte Versionszahlen dauerhaft auseinanderlaufen zu lassen.

## Erstinitialisierung

Starte Codex im Repository-Root und verwende zunächst:

```text
Untersuche dieses Repository vollständig genug, um docs/codex/CURRENT_STATE.md und die noch offenen projektspezifischen Abschnitte in docs/codex/PROJECT_CONTEXT.md zu aktualisieren. Ändere keinen Produktionscode. Trenne bestätigte Fakten von Annahmen und führe nur sichere Lese- und Diagnosebefehle aus.
```

Danach lässt sich die Konfiguration prüfen:

```text
Fasse die geladenen Repository-Anweisungen zusammen. Nenne die verfügbaren projektspezifischen Subagenten und Skills. Ändere keine Dateien.
```

## Release prüfen oder beginnen

```text
$release-verwalten

Prüfe master, offene Pull Requests und release/roadmap.toml. Nenne die nächste zulässige Version, ihren Vorgängerstand und den korrekten Releasebranch. Ändere noch keine Produktionsdateien.
```

Der `master_verwalter` muss vor jeder Versionsreservierung und vor jedem Merge nach `master` eingesetzt werden. Ein Branchname oder eine Commitnachricht ist kein Beleg dafür, dass eine Version veröffentlicht wurde.

## Neuen Knoten beauftragen

```text
$neuer-knoten

Plane und implementiere einen Knoten für einen iterativen Summenoperator. Er erhält eine Indexmenge und einen parametrierten Ausdruck, bindet den Index und gibt den resultierenden Ausdruck aus. Verwende den vollständigen Planer-, Mathematikprüfer-, Implementierer- und Verifizierer-Ablauf.
```

Damit bleibt das Agentengedächtnis präzise, während die öffentliche README lesbar bleibt. Offenbar benötigt selbst Dokumentation inzwischen eine Gewaltenteilung.
