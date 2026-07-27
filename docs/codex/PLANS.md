# Ausführbare Pläne für Codex

## Begriff

Ein **ExecPlan** ist ein eigenständiges, fortlaufend aktualisiertes Dokument, mit dem ein Agent eine größere Änderung vom untersuchten Istzustand bis zur verifizierten Fertigstellung durchführen kann.

Ein Leser muss den Plan mit dem aktuellen Repository verstehen können, ohne frühere Chats oder nicht versionierte Erinnerungen zu benötigen.

## Wann ein ExecPlan erforderlich ist

- neuer Node-Typ,
- Änderung eines Node-, Handle- oder Edge-Vertrags,
- neues mathematisches Datenmodell,
- Persistenz- oder Schemamigration,
- größere Refaktorierung über mehrere Module,
- Änderung mit mehreren voneinander abhängigen Meilensteinen,
- Aufgabe mit fachlichen Unsicherheiten oder einem Prototyp.

Für eine lokale, offensichtliche Fehlerkorrektur ist kein vollständiger ExecPlan nötig.

## Speicherort

Aktive Pläne:

```text
docs/codex/plans/active/
```

Abgenommene Pläne:

```text
docs/codex/plans/completed/
```

Dateiname:

```text
YYYY-MM-DD-kurzer-beschreibender-name.md
```

## Anforderungen

Ein ExecPlan ist:

- selbstständig verständlich,
- konkret auf reale Dateien und Symbole bezogen,
- nachprüfbar,
- lebendig und während der Arbeit aktualisiert,
- frei von nicht erklärten Abkürzungen,
- klar zwischen Fakten, Annahmen und Entscheidungen getrennt.

## Pflichtabschnitte

1. Titel und Status
2. Ziel und Nutzerwirkung
3. Nicht-Ziele
4. untersuchter Istzustand
5. fachliche und mathematische Semantik
6. Daten-, Node-, Handle- und Edge-Vertrag
7. Architekturentscheidungen
8. betroffene Dateien und Symbole
9. Meilensteine
10. konkrete Umsetzungsschritte
11. Tests und Validierung
12. Persistenz und Migration
13. Risiken und Rückfallstrategie
14. Fortschritt
15. Entscheidungsprotokoll
16. Abweichungen vom ursprünglichen Plan
17. Ergebnis und Verifikation

## Fortschritt

Jeder Meilenstein besitzt einen Zustand:

- `[ ]` offen
- `[-]` begonnen
- `[x]` abgeschlossen
- `[!]` blockiert

Ein abgeschlossener Punkt nennt kurz die nachgewiesene Evidenz.

## Entscheidungen

Jede wesentliche Entscheidung enthält:

- Datum,
- Entscheidung,
- Alternativen,
- Begründung,
- Konsequenzen.

Dauerhafte Entscheidungen werden zusätzlich als ADR festgehalten.

## Prototypen

Bei unklarer Machbarkeit darf ein begrenzter Prototyp vorgesehen werden. Der Plan beschreibt:

- zu prüfende Hypothese,
- maximale Änderung,
- Erfolgskriterium,
- ob und wie Prototypcode verworfen wird.

## Abschluss

Ein ExecPlan ist erst abgeschlossen, wenn:

- alle Abnahmekriterien geprüft sind,
- relevante Befehle mit Ergebnis dokumentiert sind,
- offene Punkte ausdrücklich als nicht blockierend begründet sind,
- `node_verifier` die Änderung abgenommen hat,
- der tatsächliche Endzustand beschrieben ist.
