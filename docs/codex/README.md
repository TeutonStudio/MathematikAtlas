# Codex-Struktur des Mathematik Atlas

Diese Dateien bilden das versionierte Projektgedächtnis und den Workflow für Codex.

## Zuständigkeiten

| Datei oder Verzeichnis | Zweck |
|---|---|
| `/AGENTS.md` | Kurze, automatisch geladene Repository-Regeln |
| `/.codex/config.toml` | Projektweite Multi-Agent-Konfiguration |
| `/.codex/agents/` | Tatsächliche Codex-Subagenten |
| `/.agents/skills/neuer-knoten/SKILL.md` | Wiederverwendbarer Ablauf für neue Knoten |
| `/docs/codex/PROJECT_CONTEXT.md` | Stabile Produkt- und Begriffsgrundlage |
| `/docs/codex/CURRENT_STATE.md` | Zuletzt verifizierter Istzustand |
| `/docs/codex/ARCHITECTURE.md` | Architekturgrenzen und Zielprinzipien |
| `/docs/codex/NODE_CONTRACT.md` | Vertrag für jeden Node-Typ |
| `/docs/codex/PLANS.md` | Anforderungen an ausführbare Pläne |
| `/docs/codex/CODE_REVIEW.md` | Prüfkriterien |
| `/docs/codex/TEST_STRATEGY.md` | Testebenen und Mindestabdeckung |
| `/docs/codex/plans/` | Aktive und abgeschlossene ExecPlans |
| `/docs/codex/decisions/` | Dauerhafte Architekturentscheidungen |
| `/docs/codex/templates/` | Vorlagen |

## Einfügen

Kopiere den Inhalt dieses Pakets in das Root-Verzeichnis des Mathematik-Atlas-Repositories. Versteckte Ordner wie `.codex` und `.agents` müssen mitkopiert werden.

Vorhandene Dateien nicht blind überschreiben:

- Eine bestehende `AGENTS.md` zusammenführen.
- Eine bestehende `.codex/config.toml` um den `[agents]`-Abschnitt ergänzen.
- Bestehende gleichnamige Agenten prüfen, bevor sie ersetzt werden.

## Erstinitialisierung

Starte Codex im Repository-Root und verwende zunächst:

```text
Untersuche dieses Repository vollständig genug, um docs/codex/CURRENT_STATE.md und die noch offenen projektspezifischen Abschnitte in docs/codex/PROJECT_CONTEXT.md zu aktualisieren. Ändere keinen Produktionscode. Trenne bestätigte Fakten von Annahmen und führe nur sichere Lese- und Diagnosebefehle aus.
```

Danach lässt sich die Konfiguration prüfen:

```text
Fasse die geladenen Repository-Anweisungen zusammen. Nenne die verfügbaren projektspezifischen Subagenten und Skills. Ändere keine Dateien.
```

## Neuen Knoten beauftragen

Explizit über den Skill:

```text
$neuer-knoten

Plane und implementiere einen Knoten für einen iterativen Summenoperator. Er erhält eine Indexmenge und einen parametrierten Ausdruck, bindet den Index und gibt den resultierenden Ausdruck aus. Verwende den vollständigen Planer-, Mathematikprüfer-, Implementierer- und Verifizierer-Ablauf.
```

Oder natürlichsprachlich:

```text
Verwende den Skill neuer-knoten und entwickle einen Knoten für die Lösungsmenge einer Gleichung.
```

## Pflegeprinzip

- `PROJECT_CONTEXT.md` enthält langfristig stabile Fakten.
- `CURRENT_STATE.md` enthält den nachweisbaren aktuellen Zustand.
- ExecPlans enthalten den Zustand einer konkreten größeren Aufgabe.
- ADRs enthalten dauerhafte Entscheidungen und ihre Begründung.
- Flüchtige Sitzungsnotizen gehören nicht in das Projektgedächtnis.

Damit bleibt der Kontext klein genug, um nützlich zu sein, statt sich in eine feierlich versionierte Gerüchtesammlung zu verwandeln.
